/*
 * KonicaMapping.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.*;

/**
 * An object that holds SKU mapping information for the Konica format.
 */

public class KonicaMapping extends Structure implements Mapping {

// --- fields ---

   public PSKU getPSKU() { return psku; }
   public void setPSKU(PSKU psku) { this.psku = psku; }

   public PSKU psku;
   public String channel;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      KonicaMapping.class,
      // no version
      new AbstractField[] {

         new PSKUField("psku","A","SKU","Rule"),
         new StringField("channel","Channel")
      });

   static { sd.setAttributed(); }
   protected StructureDefinition sd() { return sd; }

// --- validation ---

   public void validate() throws ValidationException {

      if (channel.length() == 0) throw new ValidationException(Text.get(this,"e3"));
   }

// --- migration ---

   public void migrate(Channel c) throws ValidationException {

      psku = new OldSKU(c.sku);
      channel = c.channel;
   }

}

