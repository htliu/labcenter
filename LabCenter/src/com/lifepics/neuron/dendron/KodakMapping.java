/*
 * KodakMapping.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.*;

/**
 * An object that holds SKU mapping information for the Kodak kiosk format.
 */

public class KodakMapping extends Structure implements Mapping {

// --- fields ---

   public PSKU getPSKU() { return psku; }
   public void setPSKU(PSKU psku) { this.psku = psku; }

   public PSKU psku;
   public String product;
   public Integer surface;
   public Boolean border;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      KodakMapping.class,
      // no version
      new AbstractField[] {

         new PSKUField("psku","A","SKU","Rule"),
         new StringField("product","Product"),
         new NullableEnumeratedField("surface","Surface",OrderEnum.surfaceInternalType),
         new NullableBooleanField("border","Border")
      });

   static { sd.setAttributed(); }
   protected StructureDefinition sd() { return sd; }

// --- validation ---

   public void validate() throws ValidationException {

      if (product.length() == 0) throw new ValidationException(Text.get(this,"e3"));

      if (surface != null) OrderEnum.validateSurface(surface.intValue(),/* allowRaw = */ true);

      // the product and surface codes are actually even more restricted than that,
      // but I don't enforce that here, it's just a result of how the UI is set up.
   }

// --- migration ---

   public void migrate(Channel c) throws ValidationException {
      final char DELIMITER = ':';

      String channel = c.channel;

      int i = channel.indexOf(DELIMITER);
      if (i == -1) throw new ValidationException(Text.get(this,"e4",new Object[] { channel, String.valueOf(DELIMITER) }));

      int j = channel.indexOf(DELIMITER,i+1);
      if (j != -1) throw new ValidationException(Text.get(this,"e5",new Object[] { channel, String.valueOf(DELIMITER) }));

      psku = new OldSKU(c.sku);
      product = channel.substring(0,i);
      surface = (i == channel.length()-1) ? null : new Integer(OrderEnum.toSurfaceInternal(channel.substring(i+1)));
      // leave border null, meaning none
   }

}

