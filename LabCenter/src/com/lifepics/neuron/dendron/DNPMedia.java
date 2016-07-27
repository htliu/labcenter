/*
 * DNPMedia.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Nullable;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;

/**
 * A helper class for dealing with DNP media.
 */

public class DNPMedia {

   public int media;
   public String scode; // short code
   public double width;
   public double height;
   public int imageWidth;
   public int imageHeight;
   public int multi2;
   public int multi3;

   public DNPMedia(int media, String scode, double width, double height, int imageWidth, int imageHeight, int multi2, int multi3) {
      this.media = media;
      this.scode = scode;
      this.width = width;
      this.height = height;
      this.imageWidth = imageWidth;
      this.imageHeight = imageHeight;
      this.multi2 = multi2;
      this.multi3 = multi3;
   }

   public String describe() {
      return Convert.fromDouble(width) + " x " + Convert.fromDouble(height);
      // if you change this, be sure to make findMediaByDescription backward compatible
      // since it's used in DNPConfig serialization when printerIDType is ID_TYPE_MEDIA
   }

   public static final DNPMedia[] table = new DNPMedia[] {

      new DNPMedia(DNP.MEDIA_5x35, "20", 5,  3.5, 1548, 1088, 0, 0),
      new DNPMedia(DNP.MEDIA_5x7,  "21", 5,  7,   1548, 2138, 0, 0),
      new DNPMedia(DNP.MEDIA_6x4,  "30", 6,  4,   1844, 1240, DNP.MEDIA_6x4x2, 0),
      new DNPMedia(DNP.MEDIA_6x8,  "31", 6,  8,   1844, 2436, 0, 0),
      new DNPMedia(DNP.MEDIA_6x9,  "40", 6,  9,   1844, 2740, 0, 0),
      new DNPMedia(DNP.MEDIA_8x4,  null, 8,  4,   2448, 1236, DNP.MEDIA_8x4x2, DNP.MEDIA_8x4x3),
      new DNPMedia(DNP.MEDIA_8x5,  null, 8,  5,   2448, 1536, DNP.MEDIA_8x5x2, 0),
      new DNPMedia(DNP.MEDIA_8x6,  null, 8,  6,   2448, 1836, DNP.MEDIA_8x6x2, 0),
      new DNPMedia(DNP.MEDIA_8x8,  null, 8,  8,   2448, 2436, 0, 0),
      new DNPMedia(DNP.MEDIA_8x10, "50", 8, 10,   2448, 3036, 0, 0),
      new DNPMedia(DNP.MEDIA_8x12, "51", 8, 12,   2448, 3636, 0, 0)
   };

   public static DNPMedia findMedia(int media) throws ValidationException {
      for (int i=0; i<table.length; i++) {
         if (table[i].media == media) return table[i];
      }
      throw new ValidationException(Text.get(DNPMedia.class,"e1",new Object[] { Convert.fromInt(media) }));
   }

   public static String getShortCode(String code) throws ValidationException {
      if (code.length() != 5) throw new ValidationException(Text.get(DNPMedia.class,"e2",new Object[] { code }));
      return code.substring(2,4);
   }

   public static DNPMedia findMedia(String code) throws ValidationException {
      String s = getShortCode(code);
      for (int i=0; i<table.length; i++) {
         if (Nullable.equals(table[i].scode,s)) return table[i];
      }
      throw new ValidationException(Text.get(DNPMedia.class,"e3",new Object[] { code }));
   }

   public static DNPMedia findMediaByDescription(String description) throws ValidationException {
      for (int i=0; i<table.length; i++) {
         if (table[i].describe().equals(description)) return table[i];
      }
      throw new ValidationException(Text.get(DNPMedia.class,"e4",new Object[] { description }));
   }

}

