/*
 * DP2Mapping.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.*;

/**
 * An object that holds SKU mapping information for the DP2 format.
 */

public class DP2Mapping extends Structure implements Mapping {

// --- fields ---

   public PSKU getPSKU() { return psku; }
   public void setPSKU(PSKU psku) { this.psku = psku; }

   public PSKU psku;
   public String product;
   public Integer surface;   // this and the rest are nullable
   public String paperWidth; // probably integer, possibly floating-point
   public String autoCrop;

   // here the surface really is just an integer, not an enumeration,
   // since the DP2 definitions are so flexible.

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      DP2Mapping.class,
      // no version
      new AbstractField[] {

         new PSKUField("psku","A","SKU","Rule"),
         new StringField("product","Product"),
         new NullableIntegerField("surface","Surface"),
         new NullableStringField("paperWidth","PaperWidth"),
         new NullableStringField("autoCrop","AutoCrop")
      });

   static { sd.setAttributed(); }
   protected StructureDefinition sd() { return sd; }

// --- validation ---

   private static void notilde(String s, String key) throws ValidationException {
      if (s.indexOf('~') != -1) throw new ValidationException(Text.get(DP2Mapping.class,key));
   }

   public void validate() throws ValidationException {

      if (product.length() == 0) throw new ValidationException(Text.get(this,"e13"));
      notilde(product,"e11");
      if (paperWidth != null) notilde(paperWidth,"e16");
      if (autoCrop   != null) notilde(autoCrop,  "e17");
   }

// --- migration ---

   private static String emptyToNull(String s) { return (s == null || s.length() == 0) ? null : s; }

   public void migrate(Channel c) throws ValidationException {
      final char DELIMITER = ':';

      // we don't require any delimiters, so loop is simpler than in AgfaConfig

      int start = 0;
      String[] s = new String[4];

      String channel = c.channel + DELIMITER; // easier this way

      for (int i=0; i<s.length; i++) {
         int delim = channel.indexOf(DELIMITER,start);
         if (delim == -1) { s[i] = ""; continue; }
         s[i] = channel.substring(start,delim);
         start = delim+1;
      }

      if (start != channel.length()) throw new ValidationException(Text.get(this,"e12",new Object[] { channel, String.valueOf(DELIMITER) }));

      psku = new OldSKU(c.sku);
      product = s[0];
      surface = Convert.toNullableInt(emptyToNull(s[1]));
      paperWidth = emptyToNull(s[2]);
      autoCrop = emptyToNull(s[3]);
   }

}

