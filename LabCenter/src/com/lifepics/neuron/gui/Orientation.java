/*
 * Orientation.java
 */

package com.lifepics.neuron.gui;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.EnumeratedType;

import java.awt.print.PageFormat;
import javax.print.attribute.standard.OrientationRequested;

/**
 * A utility class factored out of PrintConfig because it was
 * causing complicated issues with class initialization order.
 * See PrintConfig version control history for details.
 */

public class Orientation {

   // I can't control whether the enumeration values are consecutive,
   // so handle this differently than the other enumerations.

   private static String orientL = Text.get(Orientation.class,"or0");
   private static String orientP = Text.get(Orientation.class,"or1");
   private static String orientR = Text.get(Orientation.class,"or2");

   public static int toOrientation(String s) throws ValidationException {
      if (s.equals(orientL)) return PageFormat.LANDSCAPE;
      if (s.equals(orientP)) return PageFormat.PORTRAIT;
      if (s.equals(orientR)) return PageFormat.REVERSE_LANDSCAPE;
      throw new ValidationException(Text.get(Orientation.class,"e2",new Object[] { s }));
   }

   public static String fromOrientation(int orientation) {
      if (orientation == PageFormat.LANDSCAPE) return orientL;
      if (orientation == PageFormat.PORTRAIT ) return orientP;
      if (orientation == PageFormat.REVERSE_LANDSCAPE) return orientR;
      throw new Error(Text.get(Orientation.class,"e3",new Object[] { Convert.fromInt(orientation) }));
   }

   public static OrientationRequested attributeOrientation(int orientation) {
      if (orientation == PageFormat.LANDSCAPE) return OrientationRequested.LANDSCAPE;
      if (orientation == PageFormat.PORTRAIT ) return OrientationRequested.PORTRAIT;
      if (orientation == PageFormat.REVERSE_LANDSCAPE) return OrientationRequested.REVERSE_LANDSCAPE;
      throw new Error(Text.get(Orientation.class,"e15",new Object[] { Convert.fromInt(orientation) }));
   }
   // we don't have anything that maps to OrientationRequested.REVERSE_PORTRAIT ...
   // which is just as well, since the printers don't seem to support it either

   public static void validateOrientation(Integer orientation) throws ValidationException {
      if (orientation != null) validateOrientation(orientation.intValue());
   }

   public static void validateOrientation(int orientation) throws ValidationException {
      if ( ! (    orientation == PageFormat.LANDSCAPE
               || orientation == PageFormat.PORTRAIT
               || orientation == PageFormat.REVERSE_LANDSCAPE ) ) {
         throw new ValidationException(Text.get(Orientation.class,"e4",new Object[] { Convert.fromInt(orientation) }));
      }
   }

   public static EnumeratedType orientationType = new EnumeratedType() {
      public int toIntForm(String s) throws ValidationException { return toOrientation(s); }
      public String fromIntForm(int i) { return fromOrientation(i); }
   };

}

