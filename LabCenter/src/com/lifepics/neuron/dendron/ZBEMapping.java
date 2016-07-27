/*
 * ZBEMapping.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.*;

/**
 * An object that holds SKU mapping information for the ZBE format.
 */

public class ZBEMapping extends Structure implements Mapping {

// --- fields ---

   public PSKU getPSKU() { return psku; }
   public void setPSKU(PSKU psku) { this.psku = psku; }

   public PSKU psku;
   public double width;
   public double height;
   public Boolean colorProfile;
   public String productPath; // nullable

   // productRoot points to the product directory in WorkStream;
   // productPath gives the relative path from there.
   // here's a sample productPath from ZBE: "Std Sizes\8x10.etag"

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      ZBEMapping.class,
      // no version
      new AbstractField[] {

         new PSKUField("psku","A","SKU","Rule"),
         new DoubleField("width","Width"),
         new DoubleField("height","Height"),
         new NullableBooleanField("colorProfile","ColorProfile"),
         new NullableStringField("productPath","ProductPath")
      });

   static { sd.setAttributed(); }
   protected StructureDefinition sd() { return sd; }

// --- validation ---

   public void validate() throws ValidationException {

      if (width  < 0) throw new ValidationException(Text.get(this,"e3"));
      if (height < 0) throw new ValidationException(Text.get(this,"e4"));
   }

// --- migration ---

   public void migrate(Channel c) throws ValidationException {}
      // n/a, this mapping never existed as a channel

}

