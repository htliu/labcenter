/*
 * AgfaMapping.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.*;

/**
 * An object that holds SKU mapping information for the Agfa format.
 */

public class AgfaMapping extends Structure implements Mapping {

// --- fields ---

   public PSKU getPSKU() { return psku; }
   public void setPSKU(PSKU psku) { this.psku = psku; }

   public PSKU psku;
   public int surface;
   public int printWidth;      // mm
   public Integer printLength; // mm, null to omit
   public Integer rotation;    //     null to omit
   public Integer imageFill;   //     null to omit
   public Integer borderWidth; // mm, null to omit

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      AgfaMapping.class,
      // no version
      new AbstractField[] {

         new PSKUField("psku","A","SKU","Rule"),
         new EnumeratedField("surface","Surface",OrderEnum.surfaceInternalType),
         new IntegerField("printWidth","PrintWidth"),
         new NullableIntegerField("printLength","PrintLength"),
         new NullableIntegerField("rotation","Rotation"),
         new NullableIntegerField("imageFill","ImageFill"),
         new NullableIntegerField("borderWidth","BorderWidth")
      });

   static { sd.setAttributed(); }
   protected StructureDefinition sd() { return sd; }

// --- validation ---

   public void validate() throws ValidationException {

      OrderEnum.validateSurface(surface,/* allowRaw = */ false);

      if (printWidth < 1) throw new ValidationException(Text.get(this,"e5"));
      if (printLength != null) {
         int len = printLength.intValue();
         if (len < 1) throw new ValidationException(Text.get(this,"e6"));
      }
      if (rotation != null) {
         int rot = rotation.intValue();
         if (rot < 0 || rot > 3599) throw new ValidationException(Text.get(this,"e7"));
      }
      if (imageFill != null) {
         int fill = imageFill.intValue();
         if (    fill < Agfa.IMAGE_FILL_MIN
              || fill > Agfa.IMAGE_FILL_MAX ) throw new ValidationException(Text.get(this,"e9",new Object[] { Convert.fromInt(fill) }));
      }
      if (borderWidth != null) {
         int wid = borderWidth.intValue();
         if (wid < 0) throw new ValidationException(Text.get(this,"e10"));
      }
   }

// --- migration ---

   private static String emptyToNull(String s) { return (s == null || s.length() == 0) ? null : s; }

   public void migrate(Channel c) throws ValidationException {
      final char DELIMITER = ':';

      int start = 0;
      String[] s = new String[6];

      String channel = c.channel + DELIMITER; // easier this way

      for (int i=0; i<s.length; i++) {
         int delim = channel.indexOf(DELIMITER,start);
         if (delim == -1) {

            // for backward compatibility, allow no delimiter on last two
            // (the two before that can be null, but must still be delimited)
            if (i >= 4) { s[i] = ""; continue; }

            throw new ValidationException(Text.get(this,"e3",new Object[] { channel, String.valueOf(DELIMITER) }));
         }
         s[i] = channel.substring(start,delim);
         start = delim+1;
      }

      if (start != channel.length()) throw new ValidationException(Text.get(this,"e4",new Object[] { channel, String.valueOf(DELIMITER) }));

      psku = new OldSKU(c.sku);
      surface = OrderEnum.toSurfaceInternal(s[0]);
      printWidth = Convert.toInt(s[1]);
      printLength = Convert.toNullableInt(emptyToNull(s[2]));
      rotation = Convert.toNullableInt(emptyToNull(s[3]));
      imageFill = Convert.toNullableInt(emptyToNull(s[4]));
      borderWidth = Convert.toNullableInt(emptyToNull(s[5]));
   }

}

