/*
 * XeroxMapping.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.*;

/**
 * An object that holds SKU mapping information for the Xerox format.
 */

public class XeroxMapping extends Structure implements Mapping {

// --- fields ---

   public PSKU getPSKU() { return psku; }
   public void setPSKU(PSKU psku) { this.psku = psku; }

   public PSKU psku;
   public String productID; // normally five digits, but we may want other things
   public String description;

// --- constants ---

   private static final int LENGTH_DESCRIPTION_MAX = 50;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      XeroxMapping.class,
      // no version
      new AbstractField[] {

         new PSKUField("psku","A","SKU","Rule"),
         new StringField("productID","ProductID"),
         new StringField("description","Description")
      });

   static { sd.setAttributed(); }
   protected StructureDefinition sd() { return sd; }

// --- validation ---

   public void validate() throws ValidationException {

      if (    XeroxConfig.ruleLoose.matcher(productID  ).find()
           || XeroxConfig.ruleLoose.matcher(description).find() ) throw new ValidationException(Text.get(this,"e2"));

      if (description.length() > LENGTH_DESCRIPTION_MAX) throw new ValidationException(Text.get(this,"e1",new Object[] { Convert.fromInt(LENGTH_DESCRIPTION_MAX) }));
   }

// --- migration ---

   public void migrate(Channel c) throws ValidationException {}
      // n/a, this mapping never existed as a channel

}

