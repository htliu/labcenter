/*
 * DKS3Mapping.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.*;

/**
 * An object that holds SKU mapping information for the DKS3 format.
 */

public class DKS3Mapping extends Structure implements Mapping {

// --- fields ---

   public PSKU getPSKU() { return psku; }
   public void setPSKU(PSKU psku) { this.psku = psku; }

   public PSKU psku;
   public int surface; // enum
   public int width;   // enum
   public int advance; // mm
   public int crop;    // enum
   public int border;  // mm

   // the border field is optional, so normally we'd want it to be nullable,
   // but not having a border shouldn't be the default behavior.

// --- constants ---

   public static final int SURFACE_MIN    = 0;
   public static final int SURFACE_GLOSSY = 0;
   public static final int SURFACE_OTHER  = 1;
   public static final int SURFACE_MAX    = 1;

   public static final int WIDTH_MIN     =  0;
   public static final int WIDTH__89_3_5 =  0;
   public static final int WIDTH_102_4   =  1;
   public static final int WIDTH_114_4_5 =  2;
   public static final int WIDTH_127_5   =  3;
   public static final int WIDTH_152_6   =  4;
   public static final int WIDTH_178_7   =  5;
   public static final int WIDTH_203_8   =  6;
   public static final int WIDTH_210_8_5 =  7;
   public static final int WIDTH_240_9_5 =  8;
   public static final int WIDTH_250_10  =  9;
   public static final int WIDTH_305_12  = 10;
   public static final int WIDTH_MAX     = 10;

   public static final int ADVANCE_MIN = 102; // mm, not enum
   public static final int ADVANCE_MAX = 457;
   // careful if you change these, they're in e6 in inch form

   public static final int CROP_MIN          = 0;
   public static final int CROP_FULL_PAPER   = 0;
   public static final int CROP_FULL_IMAGE   = 1;
   public static final int CROP_AUTO_ADVANCE = 2;
   public static final int CROP_MAX          = 2;

   public static final int BORDER_MIN =  5; // mm, not enum ; zero is also allowed
   public static final int BORDER_MAX = 10;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      DKS3Mapping.class,
      // no version
      new AbstractField[] {

         new PSKUField("psku","A","SKU","Rule"),
         new IntegerField("surface","Surface"),
         new IntegerField("width","Width"),
         new IntegerField("advance","Advance"),
         new IntegerField("crop","Crop"),
         new IntegerField("border","Border")
      });

   static { sd.setAttributed(); }
   protected StructureDefinition sd() { return sd; }

// --- validation ---

   public void validate() throws ValidationException {

      if (surface < SURFACE_MIN || surface > SURFACE_MAX) {
         throw new ValidationException(Text.get(this,"e4",new Object[] { Convert.fromInt(surface) }));
      }
      if (width < WIDTH_MIN || width > WIDTH_MAX) {
         throw new ValidationException(Text.get(this,"e5",new Object[] { Convert.fromInt(width) }));
      }
      if (advance < ADVANCE_MIN || advance > ADVANCE_MAX) {
         throw new ValidationException(Text.get(this,"e6"));
      }
      if (crop < CROP_MIN || crop > CROP_MAX) {
         throw new ValidationException(Text.get(this,"e7",new Object[] { Convert.fromInt(crop) }));
      }
      if (border != 0 && (border < BORDER_MIN || border > BORDER_MAX)) {
         throw new ValidationException(Text.get(this,"e8",new Object[] { Convert.fromInt(BORDER_MIN), Convert.fromInt(BORDER_MAX) }));
      }
   }

// --- migration ---

   public void migrate(Channel c) throws ValidationException {
      final char DELIMITER = ':';

      int start = 0;
      String[] s = new String[5];

      String channel = c.channel + DELIMITER; // easier this way

      for (int i=0; i<s.length; i++) {
         int delim = channel.indexOf(DELIMITER,start);
         if (delim == -1) {
            throw new ValidationException(Text.get(this,"e2",new Object[] { channel, String.valueOf(DELIMITER) }));
         }
         s[i] = channel.substring(start,delim);
         start = delim+1;
      }

      if (start != channel.length()) throw new ValidationException(Text.get(this,"e3",new Object[] { channel, String.valueOf(DELIMITER) }));

      psku = new OldSKU(c.sku);
      surface = Convert.toInt(s[0]);
      width = Convert.toInt(s[1]);
      advance = Convert.toInt(s[2]);
      crop = Convert.toInt(s[3]);
      border = Convert.toInt(s[4]);
   }

}

