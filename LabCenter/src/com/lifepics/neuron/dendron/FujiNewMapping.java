/*
 * FujiNewMapping.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.*;

/**
 * An object that holds SKU mapping information for the Fuji PIC 2.6 format.
 */

public class FujiNewMapping extends Structure implements Mapping {

// --- fields ---

   public PSKU getPSKU() { return psku; }
   public void setPSKU(PSKU psku) { this.psku = psku; }

   public PSKU psku;
   public String printCode;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      FujiNewMapping.class,
      // no version
      new AbstractField[] {

         new PSKUField("psku","A","SKU","Rule"),
         new StringField("printCode","PrintCode")
      });

   static { sd.setAttributed(); }
   protected StructureDefinition sd() { return sd; }

// --- validation ---

   public void validate() throws ValidationException {

      if (printCode.length() == 0) throw new ValidationException(Text.get(this,"e3"));
   }

// --- migration ---

   public void migrate(Channel c) throws ValidationException {

      psku = new OldSKU(c.sku);
      printCode = c.channel;
   }

}

