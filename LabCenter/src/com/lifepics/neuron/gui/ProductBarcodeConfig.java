/*
 * ProductBarcodeConfig.java
 */

package com.lifepics.neuron.gui;

import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.*;

import java.util.LinkedList;

/**
 * Config settings that let us determine the product code part
 * of a product barcode from the base SKU in the order summary.
 * Hopefully this is temporary and will go away.
 */

public class ProductBarcodeConfig extends Structure {

// --- fields ---

   public boolean enableMap;
   public boolean forceMap;
   public LinkedList mappings;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      ProductBarcodeConfig.class,
      0,0,
      new AbstractField[] {

         new BooleanField("enableMap","EnableMap"),
         new BooleanField("forceMap","ForceMap"),
         new StructureListField("mappings","Mapping",ProductBarcodeMapping.sd,Merge.IDENTITY).with(ProductBarcodeMapping.baseSKUAccessor,ProductBarcodeMapping.baseSKUComparator)
      });

   protected StructureDefinition sd() { return sd; }

// --- validation ---

   public void validate() throws ValidationException {
      ProductBarcodeMapping.validate(mappings);
   }

}

