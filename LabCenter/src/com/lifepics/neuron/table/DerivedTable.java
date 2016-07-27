/*
 * DerivedTable.java
 */

package com.lifepics.neuron.table;

import com.lifepics.neuron.meta.Selector;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

/**
 * An object similar to a table (but not related to one by inheritance)
 * that uses a {@link Derivation} to construct the objects it contains.
 * As far as construction is concerned, DerivedTable is similar to TableView.
 */

public class DerivedTable implements TableListener {

// --- fields ---

   private Derivation derivation;
   private TableAdapter adapter;        // for the original objects
   private TableAdapter derivedAdapter; // for the derived objects

   private HashMap objects; // map from original key to collection of derived objects
   private LinkedList listeners;

// --- derived adapter ---

   private static class DerivedAdapter implements TableAdapter {

      private Derivation derivation;
      public DerivedAdapter(Derivation derivation) { this.derivation = derivation; }

      public String getKey(Object d) { return derivation.getKey(d); }

      public Object copy(Object d) { return d; }
      //
      // the point of having the copy function in TableAdapter was to make the table bulletproof:
      // unless there's a bug in the table code, the only way to modify the objects in the table
      // is through the approved channels, so there's no way to corrupt the table data on disk.
      //
      // a DerivedTable, however, is more like a view ... it can pass objects without copying,
      // because it's no big deal if they get corrupted.  it would mess up the current instance
      // of the program, but it's just a nuisance, it can't affect the underlying data.

      public boolean setKey(Object d, String key) { throw new UnsupportedOperationException(); }
      public Object load(InputStream inputStream) { throw new UnsupportedOperationException(); }
      public void store(OutputStream outputStream, Object d) { throw new UnsupportedOperationException(); }
   }

// --- construction ---

   /**
    * A package-private constructor for use by {@link Table}.
    *
    * @param adapter The table adapter, used only to get keys for objects,
    *                and to copy them during initialization.
    * @param tableObjects The objects in the table, presented as a collection
    *                     which we trust this class not to modify or hold.
    */
   DerivedTable(Derivation derivation, TableAdapter adapter, Collection tableObjects) {
      this.derivation = derivation;
      this.adapter = adapter;
      this.derivedAdapter = new DerivedAdapter(derivation);

      objects = new HashMap();
      listeners = new LinkedList();

      load(tableObjects);
   }

// --- initialization ---

   private void load(Collection tableObjects) {
      Iterator i = tableObjects.iterator();
      while (i.hasNext()) {
         Object o = i.next();
         objects.put(adapter.getKey(o),derivation.derive(adapter.copy(o)));
      }
      // note we have to derive off a copy, since (as it happens)
      // the derived objects are going to hold a pointer to it.
   }

// --- utility function ---

   private boolean remove(Collection c, String key) {
      Iterator i = c.iterator();
      while (i.hasNext()) {
         if (derivation.getKey(i.next()).equals(key)) { i.remove(); return true; }
      }
      return false; // nothing removed
   }

// --- implementation of TableListener ---

   public synchronized void reportInsert(Object o) {

      Collection c = derivation.derive(o);
      objects.put(adapter.getKey(o),c);

      Iterator i = c.iterator();
      while (i.hasNext()) notifyInsert(i.next());
   }

   public synchronized void reportUpdate(Object o) {

      Collection cNew = derivation.derive(o);
      Collection cOld = (Collection) objects.put(adapter.getKey(o),cNew);

      // ok to modify cOld, it's not in the object map any more

      Iterator i = cNew.iterator();
      while (i.hasNext()) {
         Object d = i.next();
         if (remove(cOld,derivation.getKey(d))) notifyUpdate(d);
         else notifyInsert(d);
      }

      // you might think you'd want to compare the derived objects,
      // to avoid sending updates in some cases.  the problem is,
      // the derived objects are going to hold a pointer to the original,
      // and display fields from it; and we know the original changed.

      i = cOld.iterator();
      while (i.hasNext()) notifyDelete(derivation.getKey(i.next()));
   }

   public synchronized void reportDelete(String key) {

      Collection c = (Collection) objects.remove(key);

      Iterator i = c.iterator();
      while (i.hasNext()) notifyDelete(derivation.getKey(i.next()));
   }

   public synchronized void reportRefresh() {
      notifyRefresh();
   }

// --- listeners ---

   // the code is cloned off the listener code in Table, see comments there.
   // here are the differences:
   //  * the names had to be changed, since this implements TableListener
   //  * addListener is private and unsynchronized, since only select uses it
   //  * removeListener is gone
   //  * the objects aren't copied, see comment in DerivedAdapter
   //  * I renamed "o" to "d", to fit the naming convention here

   private void addListener(TableListener a_listener) {
      listeners.add(new WeakReference(a_listener));
   }

   private void notifyInsert(Object d) {
      ListIterator li = listeners.listIterator();
      while (li.hasNext()) {
         WeakReference ref = (WeakReference) li.next();
         TableListener listener = (TableListener) ref.get();
         if (listener == null) {
            li.remove();
         } else {
            listener.reportInsert(d);
         }
      }
   }

   private void notifyUpdate(Object d) {
      ListIterator li = listeners.listIterator();
      while (li.hasNext()) {
         WeakReference ref = (WeakReference) li.next();
         TableListener listener = (TableListener) ref.get();
         if (listener == null) {
            li.remove();
         } else {
            listener.reportUpdate(d);
         }
      }
   }

   private void notifyDelete(String key) {
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

   private void notifyRefresh() {
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

// --- select ---

   /**
    * Select a subset of the objects in the table.
    */
   public synchronized View select(Selector selector, Comparator comparator, boolean dynamic, boolean transfer) {

   // make a single list of all objects

      Collection c = new LinkedList();

      Iterator i = objects.values().iterator();
      while (i.hasNext()) {
         c.addAll((Collection) i.next());
      }

   // construct the view

      TableView view = new TableView(selector,comparator,derivedAdapter,c);
      if (dynamic) addListener( transfer ? view.transferrer() : view );
      return view;
   }

}

