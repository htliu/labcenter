/*
 * TableView.java
 */

package com.lifepics.neuron.table;

import com.lifepics.neuron.meta.Selector;
import com.lifepics.neuron.meta.SortUtil;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Vector;

/**
 * A class representing a view of a {@link Table}.
 * A view is a subset which is defined by a {@link Selector}
 * and ordered by a {@link Comparator}.
 * To construct a view, call {@link Table#select(Selector,Comparator,boolean,boolean) Table.select}.
 */

public class TableView implements View, TableListener {

// --- fields ---

   private Selector selector;
   private Comparator comparator;
   private TableAdapter adapter;

   private Vector objects;
   private LinkedList listeners;

   private int suspendCount;
   private TableListenerUtil.Accumulator accumulator;
   private TableListenerUtil.Transferrer transferrer;

// --- construction ---

   /**
    * A package-private constructor for use by {@link Table}.
    *
    * @param adapter The table adapter, used only to get keys for objects,
    *                and to copy them during initialization.
    * @param tableObjects The objects in the table, presented as a collection
    *                     which we trust the view not to modify or hold.
    */
   TableView(Selector selector, Comparator comparator, TableAdapter adapter, Collection tableObjects) {
      this.selector = selector;
      this.comparator = comparator;
      this.adapter = adapter;

      objects = new Vector();
      listeners = new LinkedList();

      suspendCount = 0;
      accumulator = new TableListenerUtil.Accumulator(this);
      // transferrer is set in transferrer(), if at all

      load(tableObjects);
   }

// --- initialization ---

   private void load(Collection tableObjects) {
      Iterator i = tableObjects.iterator();
      while (i.hasNext()) {
         Object o = i.next();

         // let the user-supplied selector look at the master objects,
         // this is a small but mostly harmless breach of security
         // that saves us from making and discarding a lot of copies.

         if (selector.select(o)) {
            objects.add(adapter.copy(o)); // ignore result, always true
         }
      }

      Collections.sort(objects,comparator);
   }

   TableListener transferrer() {
      transferrer = new TableListenerUtil.Transferrer(this);
      return transferrer;

      // it would be simpler to just say, in the table,
      //
      //    addListener(new TableListenerTransferrer(view))
      //
      // the problem is, because listener references are weak,
      // it would be cleaned up right away.  so, as a workaround,
      // the view holds a real reference to the transferrer.
   }

// --- listeners ---

   // as with Table, the listeners are stored as weak references

   /**
    * Add a view listener.
    */
   public synchronized void addListener(ViewListener a_listener) {
      listeners.add(new WeakReference(a_listener));
   }

   /**
    * Remove a view listener.
    * Only call this function if the listener is <i>not</i> being destroyed,
    * but you want it to stop receiving events anyway.
    * If the listener is being destroyed, you don't need to unregister it,
    * because the view only holds a weak reference to it.
    */
   public synchronized void removeListener(ViewListener a_listener) {
      ListIterator li = listeners.listIterator();
      while (li.hasNext()) {
         WeakReference ref = (WeakReference) li.next();
         ViewListener listener = (ViewListener) ref.get();
         if (listener == null || listener == a_listener) {
            li.remove();
         }
      }
   }

   private void reportInsert(int j, Object o) {
      ListIterator li = listeners.listIterator();
      while (li.hasNext()) {
         WeakReference ref = (WeakReference) li.next();
         ViewListener listener = (ViewListener) ref.get();
         if (listener == null) {
            li.remove();
         } else {
            listener.reportInsert(j,o);
         }
      }
   }

   private void reportUpdate(int i, int j, Object o) {
      ListIterator li = listeners.listIterator();
      while (li.hasNext()) {
         WeakReference ref = (WeakReference) li.next();
         ViewListener listener = (ViewListener) ref.get();
         if (listener == null) {
            li.remove();
         } else {
            listener.reportUpdate(i,j,o);
         }
      }
   }

   private void reportDelete(int i) {
      ListIterator li = listeners.listIterator();
      while (li.hasNext()) {
         WeakReference ref = (WeakReference) li.next();
         ViewListener listener = (ViewListener) ref.get();
         if (listener == null) {
            li.remove();
         } else {
            listener.reportDelete(i);
         }
      }
   }

   private void reportChange() {
      ListIterator li = listeners.listIterator();
      while (li.hasNext()) {
         WeakReference ref = (WeakReference) li.next();
         ViewListener listener = (ViewListener) ref.get();
         if (listener == null) {
            li.remove();
         } else {
            listener.reportChange();
         }
      }
   }

// --- public methods ---

   /**
    * Get the length of the view array.
    */
   public synchronized int size() {
      return objects.size();
   }

   /**
    * Get the i-th element of the view array.
    */
   public synchronized Object get(int i) {
      return objects.get(i);
   }

   /**
    * Sort the view array.
    */
   public synchronized void sort(Comparator comparator) {
      this.comparator = comparator;
      Collections.sort(objects,comparator);
      reportChange();
   }

   /**
    * Suspend dynamic updates of the view.
    * This should only be done for short intervals,
    * because all the updates are saved in a list.
    */
   public synchronized void suspend() {
      suspendCount++;
   }

   /**
    * Resume dynamic updates of the view.
    */
   public synchronized void resume() {
      if (suspendCount == 0) return;
      if (--suspendCount == 0) { accumulator.run(); }
   }

   // note about suspend and resume:
   // you might think you'd want to have all notifications flow through
   // the accumulator, and put the "if (suspendCount > 0)" test there.
   // the problem is, the notifications flowing downward would interact badly
   // with the suspend command flowing upward -- either notifications
   // would be able to slip through after suspension, or you'd get deadlocks.
   // what I have here is not beautiful, but it works.

// --- implementation of TableListener ---

   private int remove(String key) {
      for (int i=0; i<objects.size(); i++) {
         if (adapter.getKey(objects.get(i)).equals(key)) {
            objects.removeElementAt(i);
            return i;
         }
      }
      return -1;
   }

   private int add(Object o) {
      if ( ! selector.select(o) ) return -1;
      return SortUtil.addInSortedOrder(objects,o,comparator);
   }

   /**
    * Report that an object has been inserted.
    */
   public synchronized void reportInsert(Object o) {
      if (suspendCount > 0) { accumulator.reportInsert(o); return; }

      int j = add(o);
      if (j != -1) reportInsert(j,o);
   }

   /**
    * Report that an object has been updated.
    */
   public synchronized void reportUpdate(Object o) {
      if (suspendCount > 0) { accumulator.reportUpdate(o); return; }

      // we could optimize this to avoid shifting the array
      // in the special case where the object doesn't move,
      // but that's just asking for bugs.
      // also it is a premature optimization.

      int i = remove(adapter.getKey(o));
      int j = add(o);
      if (i != -1) {
         if (j != -1) reportUpdate(i,j,o);
         else         reportDelete(i);
      } else {
         if (j != -1) reportInsert(j,o);
         // else nothing happened
      }
   }

   /**
    * Report that an object has been deleted.
    */
   public synchronized void reportDelete(String key) {
      if (suspendCount > 0) { accumulator.reportDelete(key); return; }

      int i = remove(key);
      if (i != -1) reportDelete(i);
   }

   /**
    * Report that a computed field has changed.
    */
   public synchronized void reportRefresh() {
      reportChange();
   }

}

