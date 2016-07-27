/*
 * Conversion.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.meta.Accessor;
import com.lifepics.neuron.meta.FieldComparator;
import com.lifepics.neuron.struct.*;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

/**
 * An object that tells how to convert between an old SKU and a new SKU.
 * Normally, conversions will only be used during the upgrade to LC 5.0,
 * but for a few dealers we may continue to use them to back-convert
 * to the old SKUs (since they need numeric codes at the cash register).
 */

public class Conversion extends Structure {

// --- fields ---

   public SKU oldSKU;
   public SKU newSKU;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      Conversion.class,
      // no version
      new AbstractField[] {

         new SKUField("oldSKU","OldSku","OldSKU"),
         new SKUField("newSKU","NewSku","NewSKU")
      });

   static { sd.setAttributed(); }
   protected StructureDefinition sd() { return sd; }

// --- sort functions ---

   public static Accessor newSKUAccessor = new Accessor() {
      public Class getFieldClass() { return SKU.class; }
      public Object get(Object o) { return ((Conversion) o).newSKU; }
   };

   public static Comparator newSKUComparator = new FieldComparator(newSKUAccessor,SKUComparator.displayOrder);

// --- validation ---

   /**
    * Remove conversions that are exact duplicates, with both
    * old and new SKU the same.  We don't know why the server
    * sends these duplicates, but this is the simplest fix.
    */
   public static void removeDuplicates(LinkedList conversions) {

      // use a hash on new SKU to speed things up a little.
      // a hash on the entire conversion would be slightly
      // more efficient, maybe, but a lot more work to code.

      HashSet newSet = new HashSet();

      ListIterator li = conversions.listIterator();
      while (li.hasNext()) {
         Conversion c = (Conversion) li.next();

         if ( ! newSet.add(c.newSKU) ) {

            // a duplicate new SKU, so do linear search for exact match.
            // if we find one, remove this entry;
            // if not, keep this entry so that we fail validation later.
            // note, previousIndex does handle it when we remove things.

            if (conversions.indexOf(c) < li.previousIndex()) li.remove();
         }
      }
   }

   public static void validate(LinkedList conversions) throws ValidationException {

      HashSet newSet = new HashSet();

      Iterator i = conversions.iterator();
      while (i.hasNext()) {
         Conversion c = (Conversion) i.next();

         c.validate();

         if ( ! newSet.add(c.newSKU) ) throw new ValidationException(Text.get(Conversion.class,"e3",new Object[] { c.newSKU.toString() }));
      }
      // I'm going to allow duplicate old SKUs ... the point of conversions
      // is to fill in info for the new SKUs, and maybe we'll want to fill in
      // more than one new SKU from the same old SKU.
      // the same rule works for back-conversion too ... we absolutely need
      // a single old SKU for each new SKU.  duplicates aren't ideal, since
      // they'll show as identical lines on the invoice, but close enough.
   }

   public void validate() throws ValidationException {
      if ( ! (oldSKU instanceof OldSKU) ) throw new ValidationException(Text.get(this,"e1",new Object[] { oldSKU.toString() }));
      if ( ! (newSKU instanceof NewSKU) ) throw new ValidationException(Text.get(this,"e2",new Object[] { newSKU.toString() }));
   }

// --- other functions ---

   // we don't iterate over these in the same way that we do products
   // because we don't have description and due-interval information,
   // but it's still useful to be able to dump the skus into a HashSet.

   // we also can't include these in the standard iteration because they'd usually be duplicates!

   public static void addAll(LinkedList conversions, HashSet skus) {

      Iterator i = conversions.iterator();
      while (i.hasNext()) {
         Conversion c = (Conversion) i.next();

         skus.add(c.oldSKU);
         skus.add(c.newSKU);
      }
   }

   /**
    * Build a back-conversion map, that takes new SKUs (NewSKU) to old SKUs (OldSKU).
    * The new SKUs are validated to be unique, so there's no ambiguity in the result.
    */
   public static HashMap buildConversionMap(LinkedList conversions) {
      HashMap map = new HashMap();

      Iterator i = conversions.iterator();
      while (i.hasNext()) {
         Conversion c = (Conversion) i.next();

         map.put(c.newSKU,c.oldSKU);
      }

      return map;
   }

}

