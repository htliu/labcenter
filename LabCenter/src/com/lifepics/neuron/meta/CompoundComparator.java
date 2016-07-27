/*
 * CompoundComparator.java
 */

package com.lifepics.neuron.meta;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.ListIterator;

/**
 * A comparator that contains a list of other comparators,
 * to allow specification of an arbitrary "order by" clause for a {@link com.lifepics.neuron.table.View}.
 */

public class CompoundComparator implements Comparator {

// --- fields ---

   private LinkedList comparators;

// --- construction ---

   public CompoundComparator() {
      comparators = new LinkedList();
   }

   public void add(Comparator comparator) {
      comparators.add(comparator);
   }

   /**
    * A shorthand for construction plus addition.
    */
   public CompoundComparator(Comparator comparator) {
      this();
      add(comparator);
   }

   /**
    * A shorthand for construction plus addition.
    */
   public CompoundComparator(Comparator comparator1, Comparator comparator2) {
      this();
      add(comparator1);
      add(comparator2);
   }

// --- implementation of Comparator ---

   public int compare(Object o1, Object o2) {
      ListIterator li = comparators.listIterator();
      while (li.hasNext()) {
         Comparator comparator = (Comparator) li.next();
         int result = comparator.compare(o1,o2);
         if (result != 0) return result;
      }
      return 0;
   }

   public boolean equals(Object o) {
      if ( ! (o instanceof CompoundComparator) ) return false;

      LinkedList list1 = comparators; // just for clarity
      LinkedList list2 = ((CompoundComparator) o).comparators;
      if (list1.size() != list2.size()) return false;

      ListIterator li1 = list1.listIterator();
      ListIterator li2 = list2.listIterator();
      while (li1.hasNext() && li2.hasNext()) {

         if ( ! li1.next().equals(li2.next()) ) return false;

         // no need to cast to Comparator,
         // Comparator.equals is just an override of Object.equals
      }

      return true;
   }

}

