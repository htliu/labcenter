/*
 * ThumbnailCache.java
 */

package com.lifepics.neuron.gui;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.swing.Timer;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * An object that holds a cache of thumbnail images.
 */

public class ThumbnailCache implements ActionListener {

// --- overview ---

   // this is a very complex object, but I couldn't see how to
   // break it down into simpler pieces.

   // life cycle of an entry:
   //
   // description  done  image  queueCount  in queue  in map
   // -----------  ----  -----  ----------  --------  ------
   // nonexistent                           no        no
   // request #1   no    null   1           yes       yes
   // request #2   no    null   2           yes       yes
   //
   //    failed    yes   null   0           no        yes
   // OR loaded    yes   valid  0           no        yes
   // request #3   yes   any    0           no        yes
   // purged                                no        no
   //
   // cancel #1    no    null   1           yes       yes
   // cancel #2                             no        no

   // in other words, allowed states:
   //
   // description  done  image  queueCount  in queue  in map
   // -----------  ----  -----  ----------  --------  ------
   // nonexistent                           no        no
   // queued       no    null   N           yes       yes
   // completed    yes   any    0           no        yes

   // some points to notice:
   //
   // * the desired result: every request for an entry that's not done
   //   will produce a notification iff not canceled.
   //
   // * an entry may exist in multiple queues.
   // * an entry is prevented from existing more than once in the same queue,
   //   but the queue element may contain multiple notifications.
   // * an entry that's in a queue is always in the map as well.
   // * entries are removed from all queues when they become done,
   //   so done (or queueCount) can be used as a substitute for checking all the queues.
   //
   // * there are different queues so that we can cancel loading
   //   of a set of images without shutting down the whole cache.
   // * notifications must be stored in the queues, not on the entries,
   //   so that we can know which ones to remove during a cancel.
   // * if an entry is fully canceled, it must be removed from the map.
   // * the queueCount field exists only as an optimization for canceling;
   //   it's used in garbage collection but could be replaced by done.

// --- map entry ---

   private class Entry implements ThumbnailUtil.Result, Runnable { // not static because of loaderDone

   // --- fields ---

      public File file; // this is the map key, but it's useful to have a copy

      public boolean done;
      public Image image;

      public int queueCount; // number of queues the entry is in (a ref count)

   // --- construction ---

      public Entry(File file) {
         this.file = file;
         done = false;
         image = null;
         queueCount = 0; // this is not an allowed state, but it gets incremented
      }

   // --- implementation of ThumbnailUtil.Result ---

      public boolean isDone() { return done; }
      public Image getImage() { return image; }

   // --- implementation of Runnable ---

      public void run() { loaderDone(this); }
   }

// --- queue element ---

   private static class Element {

      public Entry entry;
      public LinkedList notifications; // list of Runnable

      public Element(Entry entry) {
         this.entry = entry;
         notifications = new LinkedList();
      }

      public void sendNotifications() {
         Iterator i = notifications.iterator();
         while (i.hasNext()) {
            ((Runnable) i.next()).run();
         }
         notifications = null; // won't need that any more
      }
   }

// --- fields ---

   private int thumbWidth;
   private int thumbHeight;

   private int[] requests; // requests since last cancel

   /**
    * The cache capacity.  This is multi-dimensional so that loading
    * a large set of files into one pane doesn't flush the cache for
    * the other.  Actually, it still may, depending on which was more
    * recently used, but it will also stabilize afterward.
    */
   private int[] capacity;
   private double scale;
   private int totalCapacity;

   private LinkedHashMap map; // map of File to Entry, ordered by LRU
   private LinkedHashMap[] queue; // map of Entry to Element, ordered by insertion

   // only one of these can be non-null at a time
   private Timer timer;
   private ThumbnailLoader loader; // null if not loading

// --- construction ---

   public ThumbnailCache(int thumbWidth, int thumbHeight,
                         int sources, int initialCapacity, double scale) {

      this.thumbWidth = thumbWidth;
      this.thumbHeight = thumbHeight;

      requests = new int[sources];
      for (int si=0; si<sources; si++) requests[si] = 0;

      capacity = new int[sources];
      for (int si=0; si<sources; si++) capacity[si] = initialCapacity;
      this.scale = scale;
      recalculateTotalCapacity();

      // note, 0.75 is the default load factor
      //
      map = new LinkedHashMap(totalCapacity,(float) 0.75,/* accessOrder = */ true) {
         protected boolean removeEldestEntry(Map.Entry eldest) {
            Entry entry = (Entry) eldest.getValue();
            return (size() > totalCapacity && entry.queueCount == 0);
            // don't remove entries if still queued.
            // because unresolved requests increase the total capacity,
            // it's hard to construct a realistic scenario where
            // the LRU entry is still queued, but in theory it's possible.
         }
      };

      queue = new LinkedHashMap[sources];
      for (int si=0; si<sources; si++) queue[si] = new LinkedHashMap();
      // queues never remove eldest entry automatically

      // timer and loader start out null
   }

   private void recalculateTotalCapacity() {
      totalCapacity = 0;
      for (int si=0; si<capacity.length; si++) totalCapacity += capacity[si];
      totalCapacity *= scale;
      // scale so that we cache a useful amount above currently visible
   }

// --- implementation of ThumbnailUtil.Source ---

   public ThumbnailUtil.Source getSource(final int si) {
      return new ThumbnailUtil.Source() {
         public ThumbnailUtil.Result get(File file, Runnable r) { return ThumbnailCache.this.get(si,file,r); }
         public void cancel() { ThumbnailCache.this.cancel(si); }
      };
   }

   private ThumbnailUtil.Result get(int si, File file, Runnable r) {

      // deal with capacity first, since the map put depends on it
      if (++requests[si] > capacity[si]) {
         capacity[si] = requests[si];
         recalculateTotalCapacity();
      }

      Entry entry = (Entry) map.get(file);
      if (entry == null) {
         entry = new Entry(file);
         map.put(file,entry);
      }

      if ( ! entry.done ) { // includes new entry

         Element element = (Element) queue[si].get(entry);
         if (element == null) {
            element = new Element(entry);
            queue[si].put(entry,element);
            entry.queueCount++;
         }

         element.notifications.add(r);

         loaderActivate(); // may already be active
      }

      return entry;
   }

   private void cancel(int si) {

      // deal with capacity first
      requests[si] = 0;

      Iterator i = queue[si].values().iterator();
      while (i.hasNext()) {
         Element element = (Element) i.next();
         Entry entry = element.entry;
         if (--entry.queueCount == 0) {
            map.remove(entry.file);
         }
         // because the entry is in a queue, it's not done,
         // so we aren't removing any completed entries.
      }

      queue[si].clear();

      // ok if loader is working on something from this queue,
      // see note in loaderDone
   }

// --- methods ---

   // everything here (and above) runs in the UI thread,
   // so no need for synchronization

   // all modification of timer and loader variables is here or below.
   // there are three states, let's give them names.
   //
   // state    timer  loader
   // -------  -----  ------
   // idle     null   null
   // waiting  valid  null
   // loading  null   valid
   //
   // every function must make sure it only operates on the correct states.
   //
   // loaderActivate   idle    -> waiting
   // actionPerformed  waiting -> loading or idle
   // loaderDone       loading -> idle (but then calls loaderActivate)
   // stop             loading or waiting -> idle

   private void loaderActivate() {

      if (timer != null || loader != null) return; // already have one

      timer = new Timer(50,this);
      timer.setRepeats(false);
      timer.start();
      // the delay is empirical ... shorter and the GUI doesn't have much
      // time to repaint, longer and the images take too long to appear.
   }

   public void actionPerformed(ActionEvent e) {

      if (timer == null || loader != null) return; // stopped, ignore

      timer = null;

      for (int si=0; si<queue.length; si++) {
         if (queue[si].size() > 0) { // avoid getting iterator unnecessarily
            Element element = (Element) queue[si].values().iterator().next();
            Entry entry = element.entry;
            loader = new ThumbnailLoader(entry.file,thumbWidth,thumbHeight,entry);
            return;
         }
      }

      // else no entries, stop loading
   }

   private void loaderDone(Entry entry) {

      if (timer != null || loader == null) return; // stopped, ignore

      // note, if the entry is fully canceled, we'll mark it done,
      // notify nobody, and move on to the next entry, as desired.

   // update entry

      entry.done = true;

      if (loader.isUsable()) entry.image = loader.getScaledImage();
      // else leave null to show failure

      loader = null;
      //
      // set to null so that loaderActivate will create next one.

   // notify

      for (int si=0; si<queue.length; si++) {
         Element element = (Element) queue[si].remove(entry);
         if (element != null) element.sendNotifications();
      }

      entry.queueCount = 0;

   // continue loading

      loaderActivate();
   }

   public void stop() {

      if (timer != null) {
         timer.stop();
         timer = null;
      }

      if (loader != null) {
         loader.stop();
         loader = null;
      }

      // this stops the loader without changing any of the data structures.
      // a notification might already be in the works,
      // but we catch and prevent that at the start of loaderDone.
   }

}

