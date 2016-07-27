/*
 * NaturalComparator.java
 */

package com.lifepics.neuron.meta;

import java.util.Comparator;

/**
 * A comparator that compares objects based on their natural order.
 * (Natural order is only defined for objects that implement {@link Comparable}.)
 */

public class NaturalComparator implements Comparator {

// --- implementation of Comparator ---

   public int compare(Object o1, Object o2) {

      // make nulls count as infinitely negative

      if (o1 != null) {
         if (o2 != null) { // normal case
            return ((Comparable) o1).compareTo(o2);
         } else { // o2 == null
            return 1;
         }
      } else { // o1 == null
         if (o2 != null) {
            return -1;
         } else { // o2 == null
            return 0;
         }
      }
   }

   public boolean equals(Object o) {
      return (o instanceof NaturalComparator);
   }

}

