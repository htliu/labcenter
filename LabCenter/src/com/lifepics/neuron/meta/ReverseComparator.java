/*
 * ReverseComparator.java
 */

package com.lifepics.neuron.meta;

import java.util.Comparator;

/**
 * A comparator that reverses the order provided by another comparator.
 */

public class ReverseComparator implements Comparator {

// --- fields ---

   private Comparator comparator;

// --- construction ---

   public ReverseComparator(Comparator comparator) {
      this.comparator = comparator;
   }

// --- implementation of Comparator ---

   public int compare(Object o1, Object o2) {
      return -comparator.compare(o1,o2);
   }

   public boolean equals(Object o) {
      return (    o instanceof ReverseComparator
               && comparator.equals( ((ReverseComparator) o).comparator ) );
   }

}

