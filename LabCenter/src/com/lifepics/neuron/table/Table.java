/*
 * Table.java
 */

package com.lifepics.neuron.table;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.meta.Accessor;
import com.lifepics.neuron.meta.Selector;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;

/**
 * A class representing a simple database table.
 * It can contain any kind (or kinds) of object,
 * as long as it's described using a {@link TableAdapter},
 * and it can be based on any kind of {@link Storage}.
 * The entire table is loaded into memory at construction.<p>
 *
 * The class is thread-safe, and makes copies of the objects as necessary
 * so that modifications in one thread will never affect another.
 */

public class Table {

// --- fields ---

   private TableAdapter adapter;
   private Storage storage;
   private AutoNumber autoNumber;

   private HashMap objects;
   private HashMap locks;
   private LinkedList listeners;

// --- construction ---

   /**
    * @param autoNumber An auto-numbering object, or null for no auto-numbering.
    */
   public Table(TableAdapter adapter, Storage storage, AutoNumber autoNumber) throws TableException {
      this.adapter = adapter;
      this.storage = storage;
      this.autoNumber = autoNumber;

      objects = new HashMap();
      locks = new HashMap();
      listeners = new LinkedList();

      try {
         load();
      } catch (TableException e) {
         throw e; // subclass already wrapped the exception
      } catch (Exception e) {
         throw new TableException(Text.get(this,"e16"),e);
      }
   }

// --- initialization ---

   private void load() throws IOException, ValidationException, TableException {
      String[] keys = storage.list();
      for (int i=0; i<keys.length; i++) {

         Object o;
         try {
            o = storage.load(keys[i],adapter);
         } catch (Exception e) {
            throw new TableException(Text.get(this,"e20",new Object[] { keys[i] }),e);
         }

         // important consistency check
         String key = adapter.getKey(o);
         if ( ! key.equals(keys[i]) ) {
            throw new ValidationException(Text.get(this,"e1",new Object[] { key, keys[i] }));
         }

         // the rest is like insert, but simpler

         if (objects.containsKey(key)) {
            throw new ValidationException(Text.get(this,"e2",new Object[] { key }));
         }
         // this can only happen if keys contains duplicates

         objects.put(key,o); // ignore result
      }
   }

// --- listeners ---

   // the listeners are stored as weak references
   // so that views don't need to hold back-pointers to the table.
   // the views can just be released and finalized,
   // and the listener will be removed from the list later.

   /**
    * Add a table listener.
    */
   public synchronized void addListener(TableListener a_listener) {
      listeners.add(new WeakReference(a_listener));
   }

   /**
    * Remove a table listener.
    * Only call this function if the listener is <i>not</i> being destroyed,
    * but you want it to stop receiving events anyway.
    * If the listener is being destroyed, you don't need to unregister it,
    * because the table only holds a weak reference to it.
    */
   public synchronized void removeListener(TableListener a_listener) {
      ListIterator li = listeners.listIterator();
      while (li.hasNext()) {
         WeakReference ref = (WeakReference) li.next();
         TableListener listener = (TableListener) ref.get();
         if (listener == null || listener == a_listener) {
            li.remove();
         }
      }
   }

   // you should think of reportInsert and reportUpdate as like get.
   // they dispense an object out of the table,
   // and so they have to make a copy to avoid sharing and back-writing.

   private void reportInsert(Object o) {
      ListIterator li = listeners.listIterator();
      while (li.hasNext()) {
         WeakReference ref = (WeakReference) li.next();
         TableListener listener = (TableListener) ref.get();
         if (listener == null) {
            li.remove();
         } else {
            listener.reportInsert(adapter.copy(o));
         }
      }
   }

   private void reportUpdate(Object o) {
      ListIterator li = listeners.listIterator();
      while (li.hasNext()) {
         WeakReference ref = (WeakReference) li.next();
         TableListener listener = (TableListener) ref.get();
         if (listener == null) {
            li.remove();
         } else {
            listener.reportUpdate(adapter.copy(o));
         }
      }
   }

   private void reportDelete(String key) {
      ListIterator li = listeners.listIterator();
      while (li.hasNext()) {
         WeakReference ref = (WeakReference) li.next();
         TableListener listener = (TableListener) ref.get();
         if (listener == null) {
            li.remove();
         } else {
            listener.reportDelete(key);
         }
      }
   }

   private void reportRefresh() {
      ListIterator li = listeners.listIterator();
      while (li.hasNext()) {
         WeakReference ref = (WeakReference) li.next();
         TableListener listener = (TableListener) ref.get();
         if (listener == null) {
            li.remove();
         } else {
            listener.reportRefresh();
         }
      }
   }

// --- helpers ---

   private void fail(String textKey, String objectKey) throws TableException {
      throw new TableException(Text.get(this,textKey,new Object[] { objectKey }));
   }

   private void fail(String textKey, String objectKey, Throwable t) throws TableException {
      throw new TableException(Text.get(this,textKey,new Object[] { objectKey }),t);
   }

   private void failIfStopping(String objectKey) throws TableException {
      Thread thread = Thread.currentThread();
      if ( thread instanceof Stoppable && ((Stoppable) thread).isStopping() ) failBecauseStopping(objectKey);
   }

   private void failBecauseStopping(String objectKey) throws TableException {
      Thread thread = Thread.currentThread();
      throw new TableException(Text.get(this,"e21",new Object[] { objectKey, describe(thread) }));
   }

   private static String describe(Thread thread) {
      if (thread instanceof Stoppable) {
         return thread.getName();
      } else {
         return Text.get(Table.class,"s1");
      }
   }

   private static class Lock {
      public Thread thread;
      public Lock() { thread = Thread.currentThread(); }
   }
   // wrap the thread in a lock object so that we don't lose
   // the original behavior of the table, which was to use
   // a unique object for every lock.  it's not super-important,
   // but it does block some obscure but vaguely possible bugs.

// --- insert and delete ---

   // in all the functions that modify the table,
   // there are three things that need to be kept consistent:
   // the storage, the map of objects, and the map of locks

   /**
    * Insert an object into the table.
    * If auto-numbering is active, the object's key will be changed.
    *
    * @return A lock on the newly-inserted object.
    */
   public synchronized Object insert(Object o) throws TableException {

      if (autoNumber != null) {
         String key = autoNumber.getKey();
         if ( ! adapter.setKey(o,key) ) {
            fail("e15",key);
         }
      }

      String key = adapter.getKey(o);
      if (objects.containsKey(key)) fail("e3",key);

      try {
         storage.store(key,o,adapter); // do this first, it can fail (but is atomic)
      } catch (IOException e) {
         fail("e17",key,e);
      }
      Object copy = adapter.copy(o);
      objects.put(key,copy); // ignore result
      // copy the object so that nobody can hold a reference to it

      // set the lock before telling anyone the object exists, just in case.
      // in general, though, notifications shouldn't feed back and alter the table
      Object lock = new Lock();
      locks.put(key,lock); // ignore result

      if (autoNumber != null) autoNumber.advance();
      // in theory we ought to roll back if the auto-number fails to advance,
      // but that is very unlikely ... easier to just let later inserts fail.

      reportInsert(copy);
      // whether we use "o" or "copy" here is almost a technicality,
      // since reportInsert makes more copies of it.
      // but, "copy" is better, because it is theoretically possible
      // that one of the listeners could modify "o"
      // and as a result mess up a later notification.

      return lock;
   }

   /**
    * Delete an object from the table.
    * (The object must be locked first.)
    */
   public synchronized void delete(Object o, Object a_lock) throws TableException {
      delete(adapter.getKey(o),a_lock);
   }

   /**
    * Delete an object from the table.
    * (The object must be locked first.)
    */
   public synchronized void delete(String key, Object a_lock) throws TableException {
      if ( ! objects.containsKey(key) ) fail("e4",key);

      Object lock = locks.get(key);
      if (lock == null) fail("e5a",key); // two tests, because messages are different
      if (lock != a_lock) fail("e5b",key);

      try {
         storage.delete(key); // do this first, it can fail
      } catch (IOException e) {
         fail("e18",key,e);
      }
      objects.remove(key); // ignore result
      locks.remove(key); // ignore result

      reportDelete(key);
   }

// --- update ---

   /**
    * Lock an object in the table.
    */
   public synchronized Object lock(Object o) throws TableException {
      return lock(adapter.getKey(o));
   }

   /**
    * Lock an object in the table.
    */
   public synchronized Object lock(String key) throws TableException {
      if ( ! objects.containsKey(key) ) fail("e6",key);

      final int RETRY_COUNT = 3;    // initial try at t=0, so, three seconds max wait
      final int RETRY_DELAY = 1000; // one second
      // semi-arbitrary constants for lock retrying.  the important thing
      // to remember here is, even a single retry is very rare,
      // as rare as the e7 error messages we were getting before this.

      int retryNumber = 0;
      while (true) {

         if ( ! locks.containsKey(key) ) break; // success!

         if (retryNumber++ == RETRY_COUNT) { // fail("e7",key)

            Thread tOld = ((Lock) locks.get(key)).thread;
            Thread tNew = Thread.currentThread();
            throw new LockException(tOld,Text.get(this,"e7",new Object[] { key, describe(tOld), describe(tNew) }));
            //
            // there are other places where we say the object is locked,
            // but they don't need special messages because they happen
            // only when there's programmer error.
         }

         // not decided; wait a sec and try again.  it seems a bit weird
         // to do this in the UI thread, but even the UI thread can do
         // things that lock an order and then try to lock all related jobs,
         // so I think it's correct.

         try {

            // there's probably some way to unify all these failure cases,
            // but I'm not seeing it.  also, failing seems kind of harsh,
            // but I don't know what else I can do ... we don't have the lock,
            // and we don't have any way to signal that but an exception.

            failIfStopping(key);

            // must wait rather than sleep so that whoever has the lock on the object
            // can get into the synchronization block to release it.  relatedly,
            // must wait on the Table object, not the Thread object as sleepNice does.
            // that means we might have a one-second delay in thread stopping.
            //
            wait(RETRY_DELAY);

            failIfStopping(key);

         } catch (InterruptedException e) {
            failBecauseStopping(key);
            // shouldn't happen, but handle it, as usual
         }
      }

      Object lock = new Lock();
      locks.put(key,lock); // ignore result

      return lock;
   }

   /**
    * Try to lock an object in the table.
    *
    * @return The lock, or null if the object could not be locked.
    */
   public synchronized Object lockTry(Object o) {
      return lockTry(adapter.getKey(o));
   }

   /**
    * Try to lock an object in the table.
    *
    * @return The lock, or null if the object could not be locked.
    */
   public synchronized Object lockTry(String key) {
      if ( ! objects.containsKey(key) ) return null;
      if (locks.containsKey(key)) return null;

      Object lock = new Lock();
      locks.put(key,lock); // ignore result

      return lock;
   }

   /**
    * Update an object in the table.
    * (This does not release the lock.)
    */
   public synchronized void update(Object o, Object a_lock) throws TableException {
      String key = adapter.getKey(o);
      if ( ! objects.containsKey(key) ) fail("e8",key);

      Object lock = locks.get(key);
      if (lock == null) fail("e9",key); // two tests, because messages are different
      if (lock != a_lock) fail("e10",key);

      try {
         storage.store(key,o,adapter); // do this first, it can fail
      } catch (IOException e) {
         fail("e19",key,e);
      }
      Object copy = adapter.copy(o);
      objects.put(key,copy); // ignore result
      // copy the object so that nobody can hold a reference to it

      reportUpdate(copy);
      // see note in insert about using "o" vs. "copy"
   }

   /**
    * Release a lock on an object.
    */
   public synchronized void release(Object o, Object a_lock) throws TableException {
      release(adapter.getKey(o),a_lock);
   }

   /**
    * Release a lock on an object.
    */
   public synchronized void release(String key, Object a_lock) throws TableException {
      if ( ! objects.containsKey(key) ) fail("e11",key); // redundant, but useful message

      Object lock = locks.get(key);
      if (lock == null) fail("e12",key); // two tests, because messages are different
      if (lock != a_lock) fail("e13",key);

      locks.remove(key); // ignore result
   }

// --- get and select ---

   /**
    * Get the current number of objects in the table.
    */
   public synchronized int count() {
      return objects.size();
   }

   /**
    * Test whether an object exists in the table.
    */
   public synchronized boolean exists(Object o) {
      return exists(adapter.getKey(o));
   }

   /**
    * Test whether an object exists in the table.
    */
   public synchronized boolean exists(String key) {
      return (objects.get(key) != null);
   }

   /**
    * Get (a copy of) an object in the table.
    */
   public synchronized Object get(Object o) throws TableException {
      return get(adapter.getKey(o));
   }

   /**
    * Get (a copy of) an object in the table.
    */
   public synchronized Object get(String key) throws TableException {
      Object o = objects.get(key);
      if (o == null) fail("e14",key);

      // doesn't matter if the object is locked,
      // you can still get the current version

      return adapter.copy(o);
      // copy the object so that nobody can hold a reference to it
   }

   /**
    * Select a subset of the objects in the table.
    *
    * @param dynamic Whether the view should dynamically update,
    *                i.e., whether it should listen for changes in the table.
    *                Calling table.addListener(view) after the fact
    *                does <i>not</i> produce the same result,
    *                because the view may miss one or more events.
    * @param transfer Whether the view notifications should be transferred
    *                 to the UI dispatch thread.  The point of doing that
    *                 is that if the notifications are transferred,
    *                 the UI can perform multi-step operations on the view
    *                 (like re-reading after a sort) without worrying
    *                 that the view contents will change partway through.
    */
   public synchronized View select(Selector selector, Comparator comparator, boolean dynamic, boolean transfer) {
      TableView view = new TableView(selector,comparator,adapter,objects.values());
      if (dynamic) addListener( transfer ? view.transferrer() : view );
      return view;
   }

   public synchronized DerivedTable derive(Derivation derivation) {
      DerivedTable derivedTable = new DerivedTable(derivation,adapter,objects.values());
      addListener(derivedTable); // always dynamic, never transferred
      return derivedTable;
   }

   public synchronized Index createIndex(Accessor accessor) {
      TableIndex index = new TableIndex(accessor,adapter,objects.values());
      addListener(index); // always dynamic, never transferred
      return index;
   }

// --- refresh ---

   public synchronized void refresh() {
      reportRefresh();
   }

}

