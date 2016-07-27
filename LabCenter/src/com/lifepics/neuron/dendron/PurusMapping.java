/*
 * PurusMapping.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.*;

/**
 * An object that holds SKU mapping information for the Purus format.
 */

public class PurusMapping extends Structure implements Mapping {

// --- fields ---

   public PSKU getPSKU() { return psku; }
   public void setPSKU(PSKU psku) { this.psku = psku; }

   public PSKU psku;
   public String productID;
   public String productName;

   // late additions must be nullable
   public String mercuryMain;
   public String surfaceMain;
   public Integer pageLimit;
   public String mercuryAdditional;
   public String surfaceAdditional;

   // the "mercury" fields go into spots named "skucode",
   // but Canon calls them Mercury codes, a better name.

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      PurusMapping.class,
      // no version
      new AbstractField[] {

         new PSKUField("psku","A","SKU","Rule"),
         new StringField("productID","ProductID"),
         new StringField("productName","ProductName"),

         new NullableStringField("mercuryMain","MercuryMain"),
         new NullableStringField("surfaceMain","SurfaceMain"),
         new NullableIntegerField("pageLimit","PageLimit"),
         new NullableStringField("mercuryAdditional","MercuryAdditional"),
         new NullableStringField("surfaceAdditional","SurfaceAdditional")
      });

   static { sd.setAttributed(); }
   protected StructureDefinition sd() { return sd; }

// --- validation ---

   public void validate() throws ValidationException {

      if (productID.indexOf(' ') != -1) throw new ValidationException(Text.get(this,"e1"));
   }

// --- migration ---

   public void migrate(Channel c) throws ValidationException {}
      // n/a, this mapping never existed as a channel

}

