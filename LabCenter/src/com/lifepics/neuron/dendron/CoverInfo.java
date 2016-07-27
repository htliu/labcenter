/*
 * CoverInfo.java
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

/**
 * If a book has printed cover(s), often the cover will need to be
 * sent to the printer with a different SKU than the regular pages.
 * This structure holds information about how to do that.
 */

public class CoverInfo extends Structure {

// --- fields ---

   public SKU bookSKU;
   public SKU frontSKU;
   public SKU backSKU;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      CoverInfo.class,
      // no version
      new AbstractField[] {

         new SKUField("bookSKU","BookSku","BookSKU"),
         new NullableSKUField("frontSKU","FrontSku","FrontSKU"),
         new NullableSKUField("backSKU", "BackSku", "BackSKU" )
      });

   static { sd.setAttributed(); }
   protected StructureDefinition sd() { return sd; }

// --- sort functions ---

   public static Accessor bookSKUAccessor = new Accessor() {
      public Class getFieldClass() { return SKU.class; }
      public Object get(Object o) { return ((CoverInfo) o).bookSKU; }
   };

   public static Comparator bookSKUComparator = new FieldComparator(bookSKUAccessor,SKUComparator.displayOrder);

// --- validation ---

   public static void validate(LinkedList coverInfos) throws ValidationException {

      HashSet bookSet = new HashSet();

      Iterator i = coverInfos.iterator();
      while (i.hasNext()) {
         CoverInfo c = (CoverInfo) i.next();

         c.validate();

         if ( ! bookSet.add(c.bookSKU) ) throw new ValidationException(Text.get(CoverInfo.class,"e3",new Object[] { c.bookSKU.toString() }));
         // different books can have the same kind of cover, so no validation on frontSKU / backSKU
      }
   }

   public void validate() throws ValidationException {

      if ( ! (bookSKU instanceof NewSKU) ) throw new ValidationException(Text.get(this,"e1",new Object[] { bookSKU.toString() }));
      // nothing wrong with having bookSKU be an old SKU, but any order that has
      // a multi-image product must be a version 2 order, with only new SKUs,
      // so we might as well validate for that.  I figure it's remotely possible
      // that we might want to print covers using old SKUs, so do allow that.

      if (frontSKU == null && backSKU == null) throw new ValidationException(Text.get(this,"e2",new Object[] { bookSKU.toString() }));
   }

// --- other functions ---

   // we don't iterate over these in the same way that we do products
   // because we don't have description and due-interval information,
   // but it's still useful to be able to dump the skus into a HashSet.

   // we also can't include these in the standard iteration because they'd usually be duplicates!

   public static void addAll(LinkedList coverInfos, HashSet skus) {

      Iterator i = coverInfos.iterator();
      while (i.hasNext()) {
         CoverInfo c = (CoverInfo) i.next();

         skus.add(c.bookSKU);
         if (c.frontSKU != null) skus.add(c.frontSKU);
         if (c.backSKU  != null) skus.add(c.backSKU );
      }
   }

   /**
    * Build a lookup table from book SKUs to cover records.
    */
   public static HashMap buildBookMap(LinkedList coverInfos) {
      HashMap map = new HashMap();

      Iterator i = coverInfos.iterator();
      while (i.hasNext()) {
         CoverInfo c = (CoverInfo) i.next();

         map.put(c.bookSKU,c); // share c
      }

      return map;
   }

}

