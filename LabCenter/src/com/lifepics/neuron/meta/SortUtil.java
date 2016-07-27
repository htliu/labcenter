/*
 * SortUtil.java
 */

package com.lifepics.neuron.meta;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A utility class for keeping things in sorted order.
 */

public class SortUtil {

   /**
    * @return The position where the object was added.
    */
   public static int addInSortedOrder(List objects, Object o, Comparator comparator) {

      int j = Collections.binarySearch(objects,o,comparator);
      if (j < 0) j = -(j+1); // convert j into insertion point

      // binarySearch returns one of two things
      //
      // (a) a positive number which is the index of an element
      //     that is equal to "o" according to the comparator.
      //     in this case, that is as good a place as any to insert to object
      //
      // (b) a negative number equal to minus the insertion point, minus one.

      objects.add(j,o);
      return j;
   }

}

