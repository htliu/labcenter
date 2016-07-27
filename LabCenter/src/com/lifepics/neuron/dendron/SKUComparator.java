/*
 * SKUComparator.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.meta.NoCaseComparator;
import com.lifepics.neuron.struct.NewSKU;
import com.lifepics.neuron.struct.OldSKU;
import com.lifepics.neuron.struct.Pattern;

import java.util.Comparator;

/**
 * A utility class that defines two sort orders for SKUs.
 * Actually for PSKUs now, but I think I won't rename it.
 */

public class SKUComparator {

// --- compare functions ---

   // note, these are both complete orderings of SKU values ...
   // as the javadocs say, they're consistent with equals.
   // callers are responsible for making sure the args have
   // the right classes (any kind of PSKU).
   // actually, the args can also be null, which counts as
   // infinitely negative .. just as with NoCaseComparator.

   // I thought I'd use storage order in the config file
   // so that the old and new entries wouldn't get mixed together,
   // but the order mostly comes from EditSKUDialog,
   // not the structure with-clause, and in the dialog we need to
   // use display order.  and, on second thought, if the entries
   // aren't in display order in the config, it'll confuse people.
   // so, compareForStorage is now just an expensive tie-breaker.

   // what about patterns?  there are two things going with them.
   //
   // one, we want them to sort to the top of their product codes,
   // so display order alphabetizes them by code alone and leaves
   // the tie-breaking to storage order (which then must be sure
   // to put patterns above new SKUs with no attributes filled in)
   //
   // two, we want them to sort from more precise to less precise
   // so that for example the general rule will fire last.
   // storage order does that.  they all still fire before NewSKU.

   // the static functions could take SKU arguments, but since
   // most callers have an Object it would just be inconvenient.

   private static final NoCaseComparator noCaseComparator = new NoCaseComparator();

   private static String getSortString(Object sku) {
      if (sku == null) return null;
      if (sku instanceof Pattern) return ((Pattern) sku).getProduct();
      return sku.toString();
   }

   public static int compareForDisplay(Object sku1, Object sku2) {

      // here the order is alphabetical by string form,
      // since the user may not know about old vs. new.
      // if there are ties, storage order breaks them.
      // exception, patterns sort to the top of their product code

      int result = noCaseComparator.compare(getSortString(sku1),getSortString(sku2));
      if (result != 0) return result;
      return compareForStorage(sku1,sku2);
   }

   private static final int CLASS_NULL = 0;
   private static final int CLASS_OLD = 1;
   private static final int CLASS_PATTERN = 2;
   private static final int CLASS_NEW = 3;

   private static int getClassCode(Object sku) {

      // check in rough probability order

      if (sku instanceof NewSKU ) return CLASS_NEW;
      if (sku instanceof Pattern) return CLASS_PATTERN;
      if (sku instanceof OldSKU ) return CLASS_OLD;
      return CLASS_NULL; // or we don't know what it is
   }

   public static int compareForStorage(Object sku1, Object sku2) {

      // here the order is, old SKUs before new SKUs,
      // with old SKUs ordered as strings, as before,
      // and new SKUs ordered by internal encoding
      //
      // the second part isn't so great, but there's
      // not any obvious way to order the attributes,
      // and at least this way, when you look in the
      // config file, you see the encodings in order.
      //
      // patterns come before any related new SKUs
      // and are ordered first by attribute count
      // (descending) and then by internal encoding

      int c1 = getClassCode(sku1);
      int c2 = getClassCode(sku2);

      int d = c1 - c2;
      if (d != 0) return d;

      String s1;
      String s2;
      //
      switch (c1) { // we know c2 is same
      case CLASS_NEW:
         s1 = NewSKU.encode((NewSKU) sku1);
         s2 = NewSKU.encode((NewSKU) sku2);
         break;
      case CLASS_PATTERN:

         int n1 = ((Pattern) sku1).getAttributeCount();
         int n2 = ((Pattern) sku2).getAttributeCount();
         d = n2 - n1; // reverse order for descending
         if (d != 0) return d;

         s1 = Pattern.encode((Pattern) sku1);
         s2 = Pattern.encode((Pattern) sku2);
         break;
      case CLASS_OLD:
         s1 = OldSKU.encode((OldSKU) sku1);
         s2 = OldSKU.encode((OldSKU) sku2);
         break;
      default: // CLASS_NULL
         return 0; // null = null
      }

      return noCaseComparator.compare(s1,s2);
   }

// --- comparator objects ---

   private static class DisplayOrder implements Comparator {
      public int compare(Object o1, Object o2) {
         return compareForDisplay(o1,o2);
      }
      public boolean equals(Object o) {
         return (o instanceof DisplayOrder);
      }
   };

   private static class StorageOrder implements Comparator {
      public int compare(Object o1, Object o2) {
         return compareForStorage(o1,o2);
      }
      public boolean equals(Object o) {
         return (o instanceof StorageOrder);
      }
   };

   public static Comparator displayOrder = new DisplayOrder();
   public static Comparator storageOrder = new StorageOrder();

}

