/*
 * DueUtil.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.gui.User;
import com.lifepics.neuron.struct.SKU;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Some utility functions for due-interval calculations.
 */

public class DueUtil {

   /**
    * Build a map from SKUs to intervals, with entries only for non-null intervals.
    */
   public static HashMap buildMap(ProductConfig productConfig, final int skuDue, final LinkedList skus) {
      final HashMap dueMap = new HashMap();

      // fill in dueMap with correct information
      switch (skuDue) {
      case User.SKU_DUE_NONE:
         break;
      case User.SKU_DUE_SOME:
      case User.SKU_DUE_ALL:
         productConfig.iterate(new ProductCallback() { public void f(SKU sku, String description, Long dueInterval) {

            if (dueInterval == null) return;
            if (skuDue == User.SKU_DUE_SOME && ! skus.contains(sku)) return;

            dueMap.put(sku,dueInterval);
         } });
         break;
      }

      return dueMap;
   }

   /**
    * Compute the due interval for an order using a prebuilt map.
    */
   public static Long getDueInterval(Order order, HashMap dueMap) {
      Long result = null;

      Iterator i = order.items.iterator();
      while (i.hasNext()) {
         Order.Item item = (Order.Item) i.next();

         Long interval = (Long) dueMap.get(item.sku);
         if (interval != null) {
            if (result == null || interval.longValue() < result.longValue()) {
               result = interval;
            }
         }
         // you could use a hash set to prevent multiple map look-ups,
         // but then you'd still have one *set* hash look-up per item.
      }

      return result;
   }

}

