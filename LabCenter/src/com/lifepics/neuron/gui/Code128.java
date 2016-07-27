/*
 * Code128.java
 */

package com.lifepics.neuron.gui;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;

/**
 * A utility class for making Code 128 barcodes.
 * It's in this package only because Barcode is.
 */

public class Code128 {

// --- constants ---

   private static final int CHAR_MIN =  32;
   private static final int CHAR_MAX = 126;

   private static final int CODE_CHAR_MIN =  0;
   private static final int CODE_CHAR_MAX = 94;
   // codes from 95 to 102 not used here
   private static final int CODE_MODULUS = 103;
   private static final int CODE_START_A = 103;
   private static final int CODE_START_B = 104;
   private static final int CODE_START_C = 105;
   private static final int CODE_STOP    = 106;

   private static final int ENCODED_SIZE = 11;
   private static final int ENCODED_STOP = 13;

   private static final String[] encoded = new String[] {
      "11011001100",
      "11001101100",
      "11001100110",
      "10010011000",
      "10010001100",
      "10001001100",
      "10011001000",
      "10011000100",
      "10001100100",
      "11001001000",
      "11001000100",
      "11000100100",
      "10110011100",
      "10011011100",
      "10011001110",
      "10111001100",
      "10011101100",
      "10011100110",
      "11001110010",
      "11001011100",
      "11001001110",
      "11011100100",
      "11001110100",
      "11101101110",
      "11101001100",
      "11100101100",
      "11100100110",
      "11101100100",
      "11100110100",
      "11100110010",
      "11011011000",
      "11011000110",
      "11000110110",
      "10100011000",
      "10001011000",
      "10001000110",
      "10110001000",
      "10001101000",
      "10001100010",
      "11010001000",
      "11000101000",
      "11000100010",
      "10110111000",
      "10110001110",
      "10001101110",
      "10111011000",
      "10111000110",
      "10001110110",
      "11101110110",
      "11010001110",
      "11000101110",
      "11011101000",
      "11011100010",
      "11011101110",
      "11101011000",
      "11101000110",
      "11100010110",
      "11101101000",
      "11101100010",
      "11100011010",
      "11101111010",
      "11001000010",
      "11110001010",
      "10100110000",
      "10100001100",
      "10010110000",
      "10010000110",
      "10000101100",
      "10000100110",
      "10110010000",
      "10110000100",
      "10011010000",
      "10011000010",
      "10000110100",
      "10000110010",
      "11000010010",
      "11001010000",
      "11110111010",
      "11000010100",
      "10001111010",
      "10100111100",
      "10010111100",
      "10010011110",
      "10111100100",
      "10011110100",
      "10011110010",
      "11110100100",
      "11110010100",
      "11110010010",
      "11011011110",
      "11011110110",
      "11110110110",
      "10101111000",
      "10100011110",
      "10001011110",
      "10111101000",
      "10111100010",
      "11110101000",
      "11110100010",
      "10111011110",
      "10111101110",
      "11101011110",
      "11110101110",
      "11010000100",
      "11010010000",
      "11010011100",
      "1100011101011"
   };

// --- methods ---

   /**
    * Convert a string into a code array.
    */
   private static int[] toCodeArray(String s) throws ValidationException {
      char[] c = s.toCharArray();

      int[] code = new int[c.length];
      for (int i=0; i<c.length; i++) {
         if (c[i] < CHAR_MIN || c[i] > CHAR_MAX) throw new ValidationException(Text.get(Code128.class,"e1",new Object[] { Convert.fromInt(c[i]) }));
         code[i] = (c[i] - CHAR_MIN + CODE_CHAR_MIN);
      }

      return code;
   }

   /**
    * Compute the checksum of a code array.
    * @param start The start code to be used, not included in the array.
    */
   private static int computeChecksum(int start, int[] code) {

      // basically start plus sum of (i+1) * code[i], mod 103, but I'm adding
      // some other stuff to prevent wraparound (would cause an incorrect mod)

      int sum = start;
      int mul = 1;
      for (int i=0; i<code.length; i++) {
         sum += mul * code[i];
         sum %= CODE_MODULUS;
         mul++;
         if (mul == CODE_MODULUS) mul = 0; // yes, apparently the 103rd char really isn't checked
      }

      return sum;
   }

   /**
    * Convert a string into a Code 128 bit string.
    */
   public static String toCode128(String s) throws ValidationException {

      int[] code = toCodeArray(s);
      int start = CODE_START_B;
      int check = computeChecksum(start,code);

      StringBuffer buf = new StringBuffer(ENCODED_SIZE*(code.length+2) + ENCODED_STOP);

      buf.append(encoded[start]);
      for (int i=0; i<code.length; i++) buf.append(encoded[code[i]]);
      buf.append(encoded[check]);
      buf.append(encoded[CODE_STOP]);

      return buf.toString();
   }

}

