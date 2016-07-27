/*
 * BooleanComparator.java
 */

package com.lifepics.neuron.meta;

import java.util.Comparator;

/**
 * A comparator that compares booleans.  (Natural order doesn't work
 * for {@link Boolean} since it doesn't implement {@link Comparable}.)
 */

public class BooleanComparator implements Comparator {

// --- implementation of Comparator ---

   public int compare(Object o1, Object o2) {

      // make nulls count as infinitely negative

      if (o1 != null) {
         if (o2 != null) { // normal case

            boolean b1 = ((Boolean) o1).booleanValue();
            boolean b2 = ((Boolean) o2).booleanValue();
            int i1 = (b1 ? 1 : 0);
            int i2 = (b2 ? 1 : 0);
            return (i1 - i2);

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
      return (o instanceof BooleanComparator);
   }

}

