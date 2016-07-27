/*
 * NoCaseComparator.java
 */

package com.lifepics.neuron.meta;

import java.util.Comparator;

/**
 * A comparator that compares strings without regard to case ...
 * except that, as a last resort, it does use case,
 * because it doesn't make sense to consider different strings equal.
 * This now uses {@link NumberStringComparator} as the base order.
 */

public class NoCaseComparator implements Comparator {

// --- implementation of Comparator ---

   public int compare(Object o1, Object o2) {

      // make nulls and non-strings count as infinitely negative;
      // note that (null instanceof String) returns false

      if (o1 instanceof String) {
         if (o2 instanceof String) { // normal case
            return NumberStringComparator.compareIgnoreCase((String) o1,(String) o2);
         } else { // o2 not valid
            return 1;
         }
      } else { // o1 not valid
         if (o2 instanceof String) {
            return -1;
         } else { // o2 not valid
            return 0;
         }
      }
   }

   public boolean equals(Object o) {
      return (o instanceof NoCaseComparator);
   }

}

