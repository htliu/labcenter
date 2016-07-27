/*
 * PixelMapping.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.*;

/**
 * An object that holds SKU mapping information for the Pixel Magic format.
 */

public class PixelMapping extends Structure implements Mapping {

// --- fields ---

   public PSKU getPSKU() { return psku; }
   public void setPSKU(PSKU psku) { this.psku = psku; }

   public PSKU psku;
   public String product;
   public int service;
   public int finishType;

   // I'm not mapping the service and finish type enumerations
   // to an external form, I'm just storing them exactly as is.

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      PixelMapping.class,
      // no version
      new AbstractField[] {

         new PSKUField("psku","A","SKU","Rule"),
         new StringField("product","Product"),
         new IntegerField("service","Service"),
         new IntegerField("finishType","FinishType")
      });

   static { sd.setAttributed(); }
   protected StructureDefinition sd() { return sd; }

// --- validation ---

   public void validate() throws ValidationException {

      if (product.length() == 0) throw new ValidationException(Text.get(this,"e3"));
   }

// --- migration ---

   public void migrate(Channel c) throws ValidationException {
      final char DELIMITER = ':';

      String channel = c.channel;

      // don't bother with the fancy "too many/few delimiters" messages,
      // just fail when the middle thing fails to convert to an integer.

      int i = channel.indexOf(DELIMITER);
      int j = channel.lastIndexOf(DELIMITER);

      if (i == -1 || j == i) throw new ValidationException(Text.get(this,"e4",new Object[] { channel }));
      // note, i != -1 implies j != -1

      psku = new OldSKU(c.sku);
      product = channel.substring(0,i);
      service = Convert.toInt(channel.substring(i+1,j));
      finishType = Convert.toInt(channel.substring(j+1));
   }

}

