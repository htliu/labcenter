/*
 * PatternUtil.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.struct.Pattern;
import com.lifepics.neuron.struct.PSKU;
import com.lifepics.neuron.struct.SKU;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * A class to hold pattern and SKU utility functions.
 */

public class PatternUtil {

   public static void gather(QueueList queueList, HashSet skus) {

      gather(queueList.mappings,skus);

      Iterator i = queueList.queues.iterator();
      while (i.hasNext()) {
         Queue q = (Queue) i.next();
         if (q.formatConfig instanceof MappingConfig) {
            MappingConfig mc = (MappingConfig) q.formatConfig;

            gather(mc.getMappings(),skus);
         }
      }
   }

   public static void gather(LinkedList mappings, HashSet skus) {

      Iterator i = mappings.iterator();
      while (i.hasNext()) {
         Mapping m = (Mapping) i.next();
         skus.add(m.getPSKU());
      }
   }

   public static LinkedList getPatterns(Collection skus) {
      LinkedList result = new LinkedList();
      Iterator i = skus.iterator();
      while (i.hasNext()) {
         PSKU psku = (PSKU) i.next();
         if (psku instanceof Pattern) result.add(psku);
      }
      return result;
   }

   public static LinkedList getProductRecords(String product, ProductConfig pc) {
      LinkedList result = new LinkedList();
      Iterator i = pc.productsNew.iterator();
      while (i.hasNext()) {
         NewProduct p = (NewProduct) i.next();
         if (p.productCode.equals(product)) result.add(p);
      }
      return result;
   }

   // isActive is true iff getProductRecords returns any records

   public static boolean isActive(String product, ProductConfig pc) {
      Iterator i = pc.productsNew.iterator();
      while (i.hasNext()) {
         NewProduct p = (NewProduct) i.next();
         if (p.productCode.equals(product)) return true;
      }
      return false;
      // there can be multiple product entries with the same product code,
      // but all we need here is any one match
   }

   public static LinkedList getActive(LinkedList patterns, ProductConfig pc) {
      LinkedList result = new LinkedList();
      Iterator i = patterns.iterator();
      while (i.hasNext()) {
         Pattern pattern = (Pattern) i.next();
         if (isActive(pattern.getProduct(),pc)) result.add(pattern);
      }
      return result;
   }

   public static Pattern getMatch(Collection patterns, SKU sku) {
      Iterator i = patterns.iterator();
      while (i.hasNext()) {
         Pattern pattern = (Pattern) i.next();
         if (pattern.matches(sku)) return pattern;
      }
      return null;
   }

   public static boolean matchesAny(Collection patterns, PSKU psku) {
      if ( ! (psku instanceof SKU) ) return false; // not a definite SKU
      return (getMatch(patterns,(SKU) psku) != null);
   }

   public static LinkedList getCovered(Collection patterns, Collection skus) {
      LinkedList result = new LinkedList();
      Iterator i = skus.iterator();
      while (i.hasNext()) {
         PSKU psku = (PSKU) i.next();
         if (matchesAny(patterns,psku)) result.add(psku);
      }
      return result;
   }

   public static LinkedList sortedList(Collection skus) {
      Object[] s = skus.toArray();
      Arrays.sort(s,SKUComparator.displayOrder);
      return new LinkedList(Arrays.asList(s));
   }

}

