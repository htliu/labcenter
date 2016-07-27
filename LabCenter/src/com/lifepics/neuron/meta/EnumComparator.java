/*
 * EnumComparator.java
 */

package com.lifepics.neuron.meta;

import java.util.Arrays;
import java.util.Comparator;

/**
 * A comparator that compares objects in an enumeration based on an arbitrary order.
 */

public class EnumComparator implements Comparator {

// --- fields ---

   private int base;
   private int[] order;

// --- construction ---

   public EnumComparator(int base, int[] order) {
      this.order = order;
   }

// --- implementation of Comparator ---

   private int getOrder(Object o) {

      if ( ! (o instanceof Integer) ) return -1;
      int i = ((Integer) o).intValue();

      int index = i-base;
      if (index < 0 || index >= order.length) return -1;

      return order[index];
   }

   public int compare(Object o1, Object o2) {
      return (getOrder(o1) - getOrder(o2));
   }

   public boolean equals(Object o) {
      return (    o instanceof EnumComparator
               && base == ((EnumComparator) o).base
               && Arrays.equals(order,((EnumComparator) o).order) );
   }

}

