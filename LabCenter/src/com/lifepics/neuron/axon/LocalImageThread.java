/*
 * LocalImageThread.java
 */

package com.lifepics.neuron.axon;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.table.Index;
import com.lifepics.neuron.thread.NotConfiguredException;
import com.lifepics.neuron.thread.StoppableThread;
import com.lifepics.neuron.thread.ThreadStatus;

import java.io.File;
import java.io.FileFilter;
import java.util.Date;
import java.util.regex.Pattern;

/**
 * A thread that creates upload objects for local images.
 */

public class LocalImageThread extends StoppableThread {

// --- fields ---

   private File localShareDir;
   private LocalImageConfig config;
   private RollManager rollManager;
   private ThreadStatus threadStatus;

   private Index index;
   private FileFilter fileFilter;

// --- construction ---

   public LocalImageThread(File localShareDir, LocalImageConfig config, RollManager rollManager, ThreadStatus threadStatus) {
      super(Text.get(LocalImageThread.class,"s1"));

      this.localShareDir = localShareDir;
      this.config = config;
      this.rollManager = rollManager;
      this.threadStatus = threadStatus;

      index = rollManager.getTable().createIndex(RollUtil.localImageID);
      // this is what we have in place of a view

      fileFilter = new LocalFileFilter();
   }

// --- interface for thread fields ---

   protected void doInit() throws Exception {
   }

   protected void doRun() throws Exception {
      try {
         while ( ! isStopping() ) {
            poll();
            sleepNice(config.pollInterval);
         }
      } catch (Exception e) {
         if ( ! isStopping() ) threadStatus.fatal(e); // StoppableThread will log
         throw e;
      }
   }

   protected void doExit() {
   }

   protected void doStop() {
   }

// --- helpers ---

   private static final String FILTER_REGEX = "[0-9]+";

   // yes, specifically JPEG only, since these are supposed to be
   // server-ready images that we're just storing locally for now

   private static class LocalFileFilter implements FileFilter {

      private Pattern pattern;
      public LocalFileFilter() { pattern = Pattern.compile(FILTER_REGEX); }

      public boolean accept(File file) {
         if (file.isDirectory()) return false;

         String name = file.getName();
         int i = name.lastIndexOf('.');
         if (i == -1) return false;
         String prefix = name.substring(0,i);
         String suffix = name.substring(i+1).toLowerCase();

         if ( ! pattern.matcher(prefix).matches() ) return false;
         if ( ! (suffix.equals("jpg") || suffix.equals("jpeg")) ) return false;

         return true;
      }
   }

// --- notes ---

   // Q: why not remove the need for an index by doing some renaming
   // like all the other hot folders?
   // A: because we store the real paths to the images on the server,
   // and the browser needs them to stay in the same places.  it's a
   // pain, yes, but we just have to deal with it.

   // Q: why a complicated index object, why not just hash the table
   // every poll cycle?
   // A: because we poll very frequently, and the table may be large
   // and doesn't change that often relative to polling.

   // Q: why does ScanThread use the memory file instead of an index?
   // A: partly because I didn't think of it, but there are reasons.
   // with local images, we create an upload for every image and then
   // remove the image when we purge the upload.  with DLS, we don't
   // create an upload for every image, and we don't purge the images
   // out of the DLS when we purge the upload.  so, we really do need
   // a separate memory of what in the DLS we've seen.

   // Q: why a new subsystem, why not just put this in PollThread?
   // A: it was hard to decide, but I think this way is correct.
   // to put it in PollThread was going to require a lot of code that
   // looked like "if (localShareDir != null)", and that's a big hint
   // you ought to have a subclass or something.  and, if you look at
   // it, there's really not that much that they have in common.
   // files don't get renamed, the new-files logic is different, etc.
   // another factor is, since the LC for kiosk is just the dealer's
   // normal LC, in theory they could set up an XML poller and block
   // the entire thread with throttle.

   // about synchronization ...
   //
   // there are two sets here, the set of files in the directory
   // and the set of uploads in LC, and they both change with time,
   // not always exactly in sync.  on the one hand, the external
   // process can put a new image in the directory, which is the case
   // we're looking for here; on the other hand, a roll can be purged
   // either automatically or manually.
   //
   // unfortunately, when a roll is purged, it's removed from LC
   // before it's removed from the file system, so for an instant
   // it looks just like a new image.  so, we need a custom sync
   // with the main purge function to keep us out of that instant.
   //
   // one way to do it would be to suspend the index and then use
   // the purge lock to call listFiles.  that would be solid,
   // but it seems like more overhead and a long time in the lock.
   // the way below does one lock per new roll, but no big deal.

// --- main functions ---

   private void poll() throws Exception {

      if (localShareDir == null) throw new NotConfiguredException(Text.get(this,"e3")).setHint(Text.get(this,"h3"));
      // see explanation in LocalThread

      if ( ! localShareDir.exists() ) throw new Exception(Text.get(this,"e1",new Object[] { Convert.fromFile(localShareDir) }));

      File[] file = localShareDir.listFiles(fileFilter);
      if (file == null) throw new Exception(Text.get(this,"e2",new Object[] { Convert.fromFile(localShareDir) }));

      for (int i=0; i<file.length && ! isStopping(); i++) {
         String name = file[i].getName();
         String localImageID = name.substring(0,name.lastIndexOf('.'));

         if (index.lookup(localImageID) == null) {

            // ok, we've got a candidate, now make sure it's the real thing
            boolean create;
            synchronized (rollManager.getPurgeLock()) { create = file[i].exists(); }

            if (create) createRoll(file[i],name,localImageID);
         }
      }
   }

   private void createRoll(File file, String name, String localImageID) throws Exception {

      Roll roll = new Roll();

      roll.source = Roll.SOURCE_LOCAL;
      roll.email = "";

      roll.rollDir = localShareDir;

      // I don't see any advantage in making the date the same for the entire poll loop,
      // let's just do this
      roll.receivedDate = new Date();

      Roll.Item item = ItemUtil.makeItemScanned(file); // see RollManager.createInPlace
      item.imageID = localImageID;
      roll.items.add(item);

      // clean up these files too.  the kiosk software generates them because MediaClip
      // layout software can't handle high-res images even if they're on the local disk.
      roll.extraFiles.add(new File("thumbnail",name).getPath());
      roll.extraFiles.add(new File("MedHigh",  name).getPath());

      rollManager.createInPlaceHaveItems(roll);
   }

}

