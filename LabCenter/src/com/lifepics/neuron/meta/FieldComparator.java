/*
 * FieldComparator.java
 */

package com.lifepics.neuron.meta;

import java.util.Comparator;

/**
 * A comparator that compares objects based on an individual field.
 */

public class FieldComparator implements Comparator {

// --- fields ---

   private Accessor accessor;
   private Comparator comparator;

// --- construction ---

   public FieldComparator(Accessor accessor, Comparator comparator) {
      this.accessor = accessor;
      this.comparator = comparator;
   }

// --- implementation of Comparator ---

   public int compare(Object o1, Object o2) {
      return comparator.compare(accessor.get(o1),accessor.get(o2));
   }

   public boolean equals(Object o) {
      return (    o instanceof FieldComparator
               && accessor == ((FieldComparator) o).accessor
               && comparator.equals( ((FieldComparator) o).comparator ) );
   }

}

