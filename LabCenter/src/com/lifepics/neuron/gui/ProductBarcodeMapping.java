/*
 * ProductBarcodeMapping.java
 */

package com.lifepics.neuron.gui;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.meta.Accessor;
import com.lifepics.neuron.meta.FieldComparator;
import com.lifepics.neuron.meta.NoCaseComparator;
import com.lifepics.neuron.struct.*;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Config settings that let us determine the product code part
 * of a product barcode from the base SKU in the order summary.
 * Hopefully this is temporary and will go away.
 */

public class ProductBarcodeMapping extends Structure {

// --- fields ---

   public String baseSKU;
   public String productCode;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      ProductBarcodeMapping.class,
      // no version
      new AbstractField[] {

         new StringField("baseSKU","BaseSKU"),
         new StringField("productCode","ProductCode")
      });

   static { sd.setAttributed(); }
   protected StructureDefinition sd() { return sd; }

// --- sort functions ---

   public static Accessor baseSKUAccessor = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return ((ProductBarcodeMapping) o).baseSKU; }
   };

   public static Comparator baseSKUComparator = new FieldComparator(baseSKUAccessor,new NoCaseComparator());

// --- validation ---

   public static void validate(LinkedList mappings) throws ValidationException {
      HashSet set = new HashSet();

      Iterator i = mappings.iterator();
      while (i.hasNext()) {
         ProductBarcodeMapping m = (ProductBarcodeMapping) i.next();

         m.validate();

         if ( ! set.add(m.baseSKU) ) throw new ValidationException(Text.get(ProductBarcodeMapping.class,"e2",new Object[] { m.baseSKU }));
      }
   }

   public void validate() throws ValidationException {

      if (productCode.length() == 0) throw new ValidationException(Text.get(this,"e1",new Object[] { baseSKU }));
      // let end user check in more detail
   }

// --- other functions ---

   /**
    * Build a map from baseSKU to productCode.
    */
   public static HashMap buildProductBarcodeMap(LinkedList mappings) {
      HashMap map = new HashMap();

      Iterator i = mappings.iterator();
      while (i.hasNext()) {
         ProductBarcodeMapping m = (ProductBarcodeMapping) i.next();

         map.put(m.baseSKU,m.productCode);
      }

      return map;
   }

}

