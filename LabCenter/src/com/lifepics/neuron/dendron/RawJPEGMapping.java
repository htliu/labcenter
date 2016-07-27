/*
 * RawJPEGMapping.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.*;

/**
 * An object that holds SKU mapping information for the direct-print JPEG format.
 */

public class RawJPEGMapping extends Structure implements Mapping {

// --- fields ---

   public PSKU getPSKU() { return psku; }
   public void setPSKU(PSKU psku) { this.psku = psku; }

   public PSKU psku;
   public double width;
   public double height;
   public String margin;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      RawJPEGMapping.class,
      // no version
      new AbstractField[] {

         new PSKUField("psku","A","SKU","Rule"),
         new DoubleField("width","Width"),
         new DoubleField("height","Height"),
         new NullableStringField("margin","Margin")
      });

   static { sd.setAttributed(); }
   protected StructureDefinition sd() { return sd; }

// --- validation ---

   public void validate() throws ValidationException {

      if (width  <= 0) throw new ValidationException(Text.get(this,"e1"));
      if (height <= 0) throw new ValidationException(Text.get(this,"e2"));

      if (margin != null) {
         Margin m = parse(margin); // has side effect of doing most of the validation
         double[] a = m.getMargins();
         if (a != null) {
            for (int i=0; i<MARGIN_N; i++) { // MARGIN_N == a.length, not sure what's nicer
               if (a[i] < 0) {
                  throw new ValidationException(Text.get(this,"e4"));
               }
            }
         }
      }
   }

// --- migration ---

   public void migrate(Channel c) throws ValidationException {}
      // n/a, this mapping never existed as a channel

// --- margin enum ---

   // the deal here is, there are five margin enum values, three with margins
   // and two borderless.  of the three with margins, two auto-apply margins
   // based on printer capabilities, so it's only in the other case that you actually
   // specify margin widths.  so, the margin field here is either one of four enum
   // values or a set of margin widths (in which case the fifth enum value is implied).
   // the fifth enum value is defined here so that we can use it in the UI as a hint
   // that you should fill in some numbers instead.

   // the protocol of the Margin class follows from the comments above.  first you
   // call isBorderless.  if it's true, getBorderlessMethod is available.  if not,
   // getMarginLayout and getMargins are available, but getMargins will return null
   // in two of the three cases, meaning don't send margin widths.

   public static class Margin {

      public  int index; // used for sorting in GUI
      public  String enumText;
      private String disp;

      private boolean b;
      private String s1;
      private String s2;
      private double[] a;

      public Margin(int index, boolean b, String s1, String s2) {
         String key = Convert.fromInt(index);

         this.index = index;
         enumText = Text.get(RawJPEGMapping.class,"ma" + key);
         disp = Text.get(RawJPEGMapping.class,"md" + key);

         this.b = b;
         this.s1 = s1;
         this.s2 = s2;
         this.a = null;
      }

      public Margin(Margin m, double[] a) {

         // index, enum, and disp should not be used here!

         this.b = m.b;
         this.s1 = m.s1;
         this.s2 = m.s2;
         this.a = a;
      }

      public String toString() { return disp; }

      public boolean isBorderless() { return b; }
      public String getBorderlessMethod() { return s1; }
      public String getMarginLayout() { return s2; }
      public double[] getMargins() { return a; }
   }

   // the string constants here are immutable, defined by HP
   public static final Margin MARGIN_STANDARD   = new Margin(0,false, null,   "STANDARD");
   public static final Margin MARGIN_OVERSIZE   = new Margin(1,false, null,   "OVERSIZE");
   public static final Margin MARGIN_CLIPINSIDE = new Margin(2,false, null,   "CLIPINSIDE");
   public static final Margin MARGIN_BL_AUTOFIT = new Margin(3,true, "AUTOFIT",null);
   public static final Margin MARGIN_BL_CROP    = new Margin(4,true, "CROP",   null);

   public static final int MARGIN_L = 0; // same as in DNPMapping
   public static final int MARGIN_R = 1;
   public static final int MARGIN_T = 2;
   public static final int MARGIN_B = 3;
   public static final int MARGIN_N = 4;

   private static Margin parse1(String margin) throws ValidationException {
      if      (margin.equals(MARGIN_STANDARD  .enumText)) throw new ValidationException(Text.get(RawJPEGMapping.class,"e3"));
      else if (margin.equals(MARGIN_OVERSIZE  .enumText)) return MARGIN_OVERSIZE;
      else if (margin.equals(MARGIN_CLIPINSIDE.enumText)) return MARGIN_CLIPINSIDE;
      else if (margin.equals(MARGIN_BL_AUTOFIT.enumText)) return MARGIN_BL_AUTOFIT;
      else if (margin.equals(MARGIN_BL_CROP   .enumText)) return MARGIN_BL_CROP;
      else return null;
   }

   private static Margin parse2(String margin) throws ValidationException {
      return new Margin(MARGIN_STANDARD,Convert.toFlexArrayDouble(margin,MARGIN_N));
   }

   // also serves to validate
   public static Margin parse(String margin) throws ValidationException {
      Margin m = parse1(margin);
      return (m != null) ? m : parse2(margin);
   }

   // helper for combo in GUI
   public static Object combo(String margin) throws ValidationException {
      Margin m = parse1(margin);
      return (m != null) ? m : (Object) margin;
   }

}

