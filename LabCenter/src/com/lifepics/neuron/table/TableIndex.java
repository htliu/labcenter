/*
 * TableIndex.java
 */

package com.lifepics.neuron.table;

import com.lifepics.neuron.core.Nullable;
import com.lifepics.neuron.meta.Accessor;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

/**
 * A class representing an index on a {@link Table}.
 */

public class TableIndex implements Index, TableListener {

// --- fields ---

   private Accessor accessor;
   private TableAdapter adapter;

   private HashMap indexToKey;
   private HashMap keyToIndex;

   private int suspendCount;
   private TableListenerUtil.Accumulator accumulator;

// --- construction ---

   /**
    * A package-private constructor for use by {@link Table}.
    *
    * @param accessor The accessor that retrieves the index value.
    *                 Null values are not indexed.
    *                 If the values aren't unique, only the most recent entry will be kept (not recommended).
    * @param adapter The table adapter, used only to get keys for objects.
    * @param tableObjects The objects in the table, presented as a collection
    *                     which we trust the index not to modify or hold.
    */
   TableIndex(Accessor accessor, TableAdapter adapter, Collection tableObjects) {
      this.accessor = accessor;
      this.adapter = adapter;

      indexToKey = new HashMap();
      keyToIndex = new HashMap();

      suspendCount = 0;
      accumulator = new TableListenerUtil.Accumulator(this);

      Iterator i = tableObjects.iterator();
      while (i.hasNext()) add(i.next());
   }

// --- public methods ---

   /**
    * Look up the primary key from an index value.
    */
   public synchronized String lookup(Object index) {
      return (String) indexToKey.get(index);
   }

   /**
    * Suspend dynamic updates of the index.
    * This should only be done for short intervals,
    * because all the updates are saved in a list.
    */
   public synchronized void suspend() {
      suspendCount++;
   }

   /**
    * Resume dynamic updates of the index.
    */
   public synchronized void resume() {
      if (suspendCount == 0) return;
      if (--suspendCount == 0) { accumulator.run(); }
   }

   // see note about suspend-resume in TableView

// --- implementation of TableListener ---

   private void add(Object o) {
      Object index = accessor.get(o);
      if (index != null) add(index,adapter.getKey(o));
   }

   /**
    * Add a bond, breaking any existing ones on either side.
    */
   private void add(Object index, String key) {
      Object keyOld = indexToKey.put(index,key);
      if (keyOld != null) keyToIndex.remove(keyOld);
      Object indexOld = keyToIndex.put(key,index);
      if (indexOld != null) indexToKey.remove(indexOld);
   }

   /**
    * Remove the bond with the given key, if any.
    */
   private void remove(String key) {
      Object index = keyToIndex.remove(key);
      if (index != null) indexToKey.remove(index);
      // else the index value was null,
      // or we had a collision of index values
   }

   public synchronized void reportInsert(Object o) {
      if (suspendCount > 0) { accumulator.reportInsert(o); return; }

      add(o);
   }

   public synchronized void reportUpdate(Object o) {
      if (suspendCount > 0) { accumulator.reportUpdate(o); return; }

      String key = adapter.getKey(o);
      Object indexOld = keyToIndex.get(key);
      Object indexNew = accessor.get(o);
      if (Nullable.equalsObject(indexOld,indexNew)) return;

      if (indexNew != null) {
         add(indexNew,key); // also breaks the old bond, if any
      } else {
         remove(key);
      }
      // academic since we don't change index values, but let's do it right
   }

   public synchronized void reportDelete(String key) {
      if (suspendCount > 0) { accumulator.reportDelete(key); return; }

      remove(key);
   }

   public void reportRefresh() {
      // indexes that depend on external variables not supported
   }

}

