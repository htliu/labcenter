/*
 * NoritsuMapping.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.*;

/**
 * An object that holds SKU mapping information for the Noritsu format.
 */

public class NoritsuMapping extends Structure implements Mapping {

// --- fields ---

   public PSKU getPSKU() { return psku; }
   public void setPSKU(PSKU psku) { this.psku = psku; }

   public PSKU psku;
   public String channel; // could store as an int, but too much conversion

// --- n-digit values ---

   // public static final int NDIGIT_CHANNEL = 3; // encoded in isValid and normalize now

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      NoritsuMapping.class,
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

      if ( ! isValid(channel) ) throw new ValidationException(Text.get(this,"e4",new Object[] { channel }));
   }

   // the deal here is that we want to allow an alphabetic character
   // after the digits without totally removing the validation rules.

   public static boolean isValid(String channel) {
      switch (classify(channel)) {
      case 30:
      case 31:
         return true;
      default:
         return false;
      }
   }

   /**
    * @return A normalized (valid) channel value, if we can figure one out,
    *         otherwise just the original string.
    */
   public static String normalize(String channel) {
      switch (classify(channel)) {
      case 10:
      case 11:
         return "00" + channel;
      case 20:
      case 21:
         return "0" + channel;
      default:
         return channel;
      }
   }

   private static int classify(String channel) {
      int i = 0;
      int len = channel.length();
      while (i < len && isDigit(channel.charAt(i))) i++;
      int nd = i;
      while (i < len && isAlpha(channel.charAt(i))) i++;
      int na = i-nd;
      if (i == len && nd < 10 && na < 10) {
         return 10*nd + na;
      } else {
         return -1;
      }
   }

   private static boolean isDigit(char c) { return (c >= '0' && c <= '9'); }
   private static boolean isAlpha(char c) { return (c >= 'A' && c <= 'Z')
                                                || (c >= 'a' && c <= 'z'); }

// --- migration ---

   public void migrate(Channel c) throws ValidationException {

      psku = new OldSKU(c.sku);
      channel = c.channel;
   }

}

