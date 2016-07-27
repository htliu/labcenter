/*
 * BurnMapping.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.*;

/**
 * An object that holds SKU mapping information for the media burn format.
 */

public class BurnMapping extends Structure implements Mapping {

// --- fields ---

   public PSKU getPSKU() { return psku; }
   public void setPSKU(PSKU psku) { this.psku = psku; }

   public PSKU psku;
   public String productType;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      BurnMapping.class,
      // no version
      new AbstractField[] {

         new PSKUField("psku","A","SKU","Rule"),
         new StringField("productType","ProductType")
      });

   static { sd.setAttributed(); }
   protected StructureDefinition sd() { return sd; }

// --- validation ---

   public void validate() throws ValidationException {

      if (productType.length() == 0) throw new ValidationException(Text.get(this,"e1"));
   }

// --- migration ---

   public void migrate(Channel c) throws ValidationException {}
      // n/a, this mapping never existed as a channel

}

