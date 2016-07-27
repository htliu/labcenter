/*
 * Barcode.java
 */

package com.lifepics.neuron.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;

/**
 * A utility class for drawing barcodes and doing other related things.
 */

public class Barcode {

// --- constants ---

   private static final int upcModuleCount = 95;

   private static final String   upcStart  = "101";
   private static final String   upcMiddle = "01010";
   private static final String   upcEnd    = "101";

   private static final String[] upcDigitL = new String[] { "0001101",
                                                            "0011001",
                                                            "0010011",
                                                            "0111101",
                                                            "0100011",
                                                            "0110001",
                                                            "0101111",
                                                            "0111011",
                                                            "0110111",
                                                            "0001011"  };

   private static final String[] upcDigitG = new String[] { "0100111", // L inverted and reversed
                                                            "0110011",
                                                            "0011011",
                                                            "0100001",
                                                            "0011101",
                                                            "0111001",
                                                            "0000101",
                                                            "0010001",
                                                            "0001001",
                                                            "0010111"  };

   private static final String[] upcDigitR = new String[] { "1110010", // L inverted
                                                            "1100110",
                                                            "1101100",
                                                            "1000010",
                                                            "1011100",
                                                            "1001110",
                                                            "1010000",
                                                            "1000100",
                                                            "1001000",
                                                            "1110100"  };

   private static final String[] lgTable = new String[] { "LLLLLL", // switch between L and G in EAN-13
                                                          "LLGLGG",
                                                          "LLGGLG",
                                                          "LLGGGL",
                                                          "LGLLGG",
                                                          "LGGLLG",
                                                          "LGGGLL",
                                                          "LGLGLG",
                                                          "LGLGGL",
                                                          "LGGLGL"  };

   private static final int[] pcTable1 = new int[] { 0, 6, 2, 8, 4, 7, 3, 9, 5, 1 };
   private static final int[] pcTable2 = new int[] { 0, 9, 8, 7, 6, 5, 4, 3, 2, 1 };
   private static final int[] pcTable3 = new int[] { 0, 5, 7, 2, 4, 9, 1, 6, 8, 3 };

// --- utilities ---

   private static class CharRef { public char c; }

   public static class Pair {
      public Pair(String display, String encoded) { this.display = display; this.encoded = encoded; }
      public String display;
      public String encoded; // consists of 0 and 1
   }

   /**
    * Convert a digit string to a digit array.
    */
   private static int[] toDigitArray(String s, int extra) {
      char[] c = s.toCharArray();

      int[] digit = new int[c.length+extra];
      for (int i=0; i<c.length; i++) {
         digit[i] = (c[i] - '0');
      }

      return digit;
   }

   /**
    * Compute the price check digit (for UPCs that start with 2).
    */
   private static void computePriceCheck(int[] digit, CharRef cr1, boolean barcodePrefixReplace) {

      // the spec, found at this URL, uses different tables and then multiplies by 3,
      // but I went ahead and premultiplied.  see "Price Check Digit" part near end.
      // http://barcodes.gs1us.org/GS1%20US%20BarCodes%20and%20eCom%20-%20The%20Global%20Language%20of%20Business.htm

      if (digit[0] == 2 && barcodePrefixReplace) {
         int sum = 0;
         sum += pcTable1[digit[7]];
         sum += pcTable1[digit[8]];
         sum += pcTable2[digit[9]];
         sum += pcTable3[digit[10]];
         digit[6] = sum % 10;
      }

      cr1.c = (char) ('0' + digit[6]);
   }

   /**
    * Compute the UPC check digit.
    */
   private static void computeCheckDigit(int[] digit, CharRef cr2, int eanDigit) {

      int sum = 0;
      for (int i=0; i<11; i+=2) sum += digit[i];
      sum *= 3;
      sum += eanDigit;
      for (int i=1; i<11; i+=2) sum += digit[i];

      // we want the last digit to make the sum zero mod 10, so ...

      int check = sum % 10;
      check = 10 - check;
      if (check == 10) check = 0;

      digit[11] = check;
      cr2.c = (char) ('0' + check); // we do need to go back to char form after all
   }

   /**
    * Convert an 11-digit digit string to a 95-bit UPC bit string.
    * @param eanDigit The extra front digit for EAN-13.
    *                 If this is zero you get a standard UPC code.
    */
   private static String toUPC(String s, CharRef cr1, CharRef cr2, int eanDigit, boolean barcodePrefixReplace) {

      int[] digit = toDigitArray(s,/* extra = */ 1);
      computePriceCheck(digit,cr1,barcodePrefixReplace);
      computeCheckDigit(digit,cr2,eanDigit);

      StringBuffer buf = new StringBuffer(upcModuleCount);
      String lg = lgTable[eanDigit];

      buf.append(upcStart);
      for (int i=0; i< 6; i++) buf.append((lg.charAt(i) == 'G') ? upcDigitG[digit[i]] : upcDigitL[digit[i]]);
      buf.append(upcMiddle);
      for (int i=6; i<12; i++) buf.append(upcDigitR[digit[i]]);
      buf.append(upcEnd);

      return buf.toString();
   }

   public static Pair toEAN(int eanDigit, String s) {

      CharRef cr1 = new CharRef();
      CharRef cr2 = new CharRef();
      String upc = toUPC(s,cr1,cr2,eanDigit,false);

      String space = "  ";
      String expanded =           (char) ('0' + eanDigit)
                        + space + s.substring(0,6)
                        + space + s.substring(6,11) + cr2.c;

      return new Pair(expanded,upc);
   }

// --- graphics ---

   /**
    * Draw a barcode using parameters set by a BarcodeConfig.
    * The coordinates specify the lower left corner position.
    *
    * @param s An 11-digit digit string.
    */

   public static void drawBarcode(Graphics g, BarcodeConfig bc, int x, int y, String s, boolean barcodePrefixReplace) {
      drawBackground(g,bc,x,y);

   // set up

      x += bc.marginPixelH.intValue();

      int y2 = y  - bc.marginPixelV.intValue();
      int y1 = y2 - bc.textPixels  .intValue()
                  - bc.heightPixels.intValue();

      int w = bc.modulePixels.intValue() * upcModuleCount;
      int h = bc.heightPixels.intValue();

      int module = bc.modulePixels.intValue();

      CharRef cr1 = new CharRef();
      CharRef cr2 = new CharRef();
      String upc = toUPC(s,cr1,cr2,0,barcodePrefixReplace) + "0"; // extra zero to flush last bar

   // draw text

      String space = "  ";
      String expanded =           s.substring(0, 1)
                        + space + s.substring(1, 6)
                        + space + cr1.c + s.substring(7,11) // i.e., the rest
                        + space + cr2.c;

      // font already includes plenty of top margin space,
      // no need to reduce the text pixels by anything.

      g.setFont(new Font("SansSerif",Font.PLAIN,bc.textPixels.intValue()));

      FontMetrics fm = g.getFontMetrics();
      int sw = fm.stringWidth(expanded);
      // assume no descent, numbers don't have any

      g.drawString(expanded,x+(w-sw)/2,y2); // centered

   // draw bars

      char[] c = upc.toCharArray();

      Integer start = null; // no bar started yet; can't use -1 for null because it's a coordinate
      for (int i=0; i<c.length; i++) {

         if (c[i] != '0') {
            if (start == null) start = new Integer(x);
         } else {
            if (start != null) {
               g.fillRect(start.intValue(),y1,x-start.intValue(),h);
               start = null;
            }
         }
         x += module;
      }
      // draw adjacent bars in one call so that we do the best we can
      // even if we're not really drawing to exact pixels.
   }

   /**
    * Draw an X'd out box in the area a barcode would occupy.
    * The coordinates specify the lower left corner position.
    */
   public static void drawBox(Graphics g, BarcodeConfig bc, int x, int y) {
      drawBackground(g,bc,x,y);

      int x1 = x  + bc.marginPixelH.intValue();
      int x2 = x1 + bc.modulePixels.intValue() * upcModuleCount;

      int y2 = y  - bc.marginPixelV.intValue();
      int y1 = y2 - bc.textPixels  .intValue()
                  - bc.heightPixels.intValue();

      x2--; // account for one-pixel line width
      y2--;

      g.drawLine(x1,y1,x2,y1);
      g.drawLine(x1,y2,x2,y2);
      g.drawLine(x1,y1,x1,y2);
      g.drawLine(x2,y1,x2,y2);
      g.drawLine(x1,y1,x2,y2);
      g.drawLine(x1,y2,x2,y1);
   }

   /**
    * Blank out the background area for a barcode or box.
    * This should be blank already, but if the invoice
    * extends into the overlap region, the barcode wins.
    */
   private static void drawBackground(Graphics g, BarcodeConfig bc, int x, int y) {

      int w = getTotalWidth (bc);
      int h = getTotalHeight(bc);

      g.setColor(Color.white);
      g.fillRect(x,y-h,w,h);
      g.setColor(Color.black);
   }

   public static int getTotalWidth(BarcodeConfig bc) {
      return   bc.modulePixels.intValue() * upcModuleCount
             + bc.marginPixelH.intValue() * 2;
   }

   public static int getTotalHeight(BarcodeConfig bc) {
      return   bc.heightPixels.intValue()
             + bc.textPixels  .intValue()
             + bc.marginPixelV.intValue() * 2;
   }

}

