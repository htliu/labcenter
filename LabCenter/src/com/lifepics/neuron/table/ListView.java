/*
 * ListView.java
 */

package com.lifepics.neuron.table;

import com.lifepics.neuron.meta.SortUtil;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * A class that presents an existing {@link List} as a {@link View}
 * and provides notification-producing functions to add and remove elements.
 */

public class ListView implements View {

// --- fields ---

   private List objects;
   private Comparator comparator;

   private LinkedList listeners;

// --- construction ---

   /**
    * @param objects The list of objects to be viewed.
    * @param comparator The comparator to be used, <i>if</i> new elements are added.
    *                   The order of existing elements is not modified.
    */
   public ListView(List objects, Comparator comparator) {
      this.objects = objects;
      this.comparator = comparator;

      listeners = new LinkedList();
   }

// --- listeners ---

   /**
    * Add a view listener.
    */
   public void addListener(ViewListener a_listener) {
      listeners.add(a_listener);
   }

   /**
    * Remove a view listener.
    */
   public void removeListener(ViewListener a_listener) {

      // we could just use LinkedList.remove, but I want to remove all instances,
      // basically just to be consistent with Table and TableView

      ListIterator li = listeners.listIterator();
      while (li.hasNext()) {
         ViewListener listener = (ViewListener) li.next();
         if (listener == a_listener) {
            li.remove();
         }
      }
   }

   private void reportInsert(int j, Object o) {
      ListIterator li = listeners.listIterator();
      while (li.hasNext()) {
         ViewListener listener = (ViewListener) li.next();
         listener.reportInsert(j,o);
      }
   }

   private void reportUpdate(int i, int j, Object o) {
      ListIterator li = listeners.listIterator();
      while (li.hasNext()) {
         ViewListener listener = (ViewListener) li.next();
         listener.reportUpdate(i,j,o);
      }
   }

   private void reportDelete(int i) {
      ListIterator li = listeners.listIterator();
      while (li.hasNext()) {
         ViewListener listener = (ViewListener) li.next();
         listener.reportDelete(i);
      }
   }

   private void reportChange() {
      ListIterator li = listeners.listIterator();
      while (li.hasNext()) {
         ViewListener listener = (ViewListener) li.next();
         listener.reportChange();
      }
   }

// --- public methods ---

   /**
    * Get the length of the view array.
    */
   public int size() {
      return objects.size();
   }

   /**
    * Get the i-th element of the view array.
    */
   public Object get(int i) {
      return objects.get(i);
   }

   /**
    * Sort the view array.
    */
   public void sort(Comparator comparator) {
      this.comparator = comparator;
      Collections.sort(objects,comparator);
      reportChange();
   }

   /**
    * Suspend and resume are not implemented,
    * because ListViews are not dynamically updated.
    */
   public void suspend() {
   }

   /**
    * Suspend and resume are not implemented,
    * because ListViews are not dynamically updated.
    */
   public void resume() {
   }

// --- notification-producing functions ---

   public void add(Object o) {
      int j = SortUtil.addInSortedOrder(objects,o,comparator);
      reportInsert(j,o);
   }

   /**
    * This has the same effect on the list as calling remove and then add,
    * but it produces only a single notification, and hence less flicker.
    */
   public void edit(Object o) {
      int i = objects.indexOf(o);
      if (i == -1) return; // shouldn't happen

      objects.remove(i); // ignore result

      int j = SortUtil.addInSortedOrder(objects,o,comparator);
      reportUpdate(i,j,o);
   }

   /**
    * Same as edit, but doesn't adjust the object's position in the list.
    */
   public void editNoSort(Object o) {
      int i = objects.indexOf(o);
      if (i == -1) return; // shouldn't happen

      reportUpdate(i,i,o);
   }

   public void remove(Object o) {
      int i = objects.indexOf(o);
      if (i == -1) return; // shouldn't happen

      objects.remove(i); // ignore result
      reportDelete(i);
   }

   public void userChange() {
      reportChange();
   }

   public void userChange(Comparator comparator) {
      this.comparator = comparator;
      reportChange();
   }

   public void repoint(List objects) {
      this.objects = objects;
      reportChange();
   }

}

