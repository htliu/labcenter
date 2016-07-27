/*
 * DNPMapping.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.*;

/**
 * An object that holds SKU mapping information for the DNP direct format.
 */

public class DNPMapping extends Structure implements Mapping {

// --- constants ---

   public static final int MARGIN_L = 0;
   public static final int MARGIN_R = 1;
   public static final int MARGIN_T = 2;
   public static final int MARGIN_B = 3;
   public static final int MARGIN_N = 4;

   public static final int TILES_X = 0;
   public static final int TILES_Y = 1;
   public static final int TILES_N = 2;

// --- fields ---

   public PSKU getPSKU() { return psku; }
   public void setPSKU(PSKU psku) { this.psku = psku; }

   public PSKU psku;
   public int media;  // enum MEDIA_X
   public int finish; // enum FINISH_X
   public String margin; // pixels
   public String tiles;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      DNPMapping.class,
      // no version
      new AbstractField[] {

         new PSKUField("psku","A","SKU","Rule"),
         new IntegerField("media","Media"),
         new IntegerField("finish","Finish"),
         new NullableStringField("margin","Margin"),
         new NullableStringField("tiles","Tiles")
      });

   static { sd.setAttributed(); }
   protected StructureDefinition sd() { return sd; }

// --- validation ---

   public void validate() throws ValidationException {

      DNPMedia m = DNPMedia.findMedia(media); // semi-validation

      if (margin != null) {
         int[] marginArray = Convert.toFlexArray(margin,MARGIN_N);
         for (int i=0; i<MARGIN_N; i++) {
            if (marginArray[i] < 0) {
               throw new ValidationException(Text.get(this,"e1"));
            }
         }
         if (    marginArray[MARGIN_L] + marginArray[MARGIN_R] > m.imageWidth
              || marginArray[MARGIN_T] + marginArray[MARGIN_B] > m.imageHeight ) {
            throw new ValidationException(Text.get(this,"e2"));
         }
      }

      if (tiles != null) {
         int[] tilesArray = Convert.toIntArray(tiles,TILES_N); // not flex, it's not natural to say "2" for "2x2"
         for (int i=0; i<TILES_N; i++) {
            if (tilesArray[i] < 1) {
               throw new ValidationException(Text.get(this,"e3"));
            }
         }
      }
   }

// --- migration ---

   public void migrate(Channel c) throws ValidationException {}
      // n/a, this mapping never existed as a channel

}

