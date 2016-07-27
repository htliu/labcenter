/*
 * DirectEnum.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.EnumeratedType;

import javax.print.attribute.standard.Fidelity;
import javax.print.attribute.standard.SheetCollate;
import javax.print.attribute.standard.Sides;

/**
 * A utility class containing conversion functions for direct-print enumerated types.
 */

public class DirectEnum {

   // put these enums in their own class to allow reuse, and to avoid
   // the weird problem I ran into with PrintConfig.orientationType (now Orientation.orientationType)
   //
   // like orientation, these enums match up to existing Java enums,
   // but unlike it, the enums are object-valued, not int-valued.
   // I don't want to make the fields object-valued, since that wouldn't
   // go well with my structure framework, so I have to use ints ...
   // but then there's no convenient way to go from an int to an object,
   // so there's no reason my ints need to be the same as theirs.
   // and, if I use my own ints, they can be consecutive, which makes
   // the code here simpler.  QED!

   // the attribute conversions aren't used right now, but maybe they
   // will be again in the future.  same with the fidelity attribute.

// --- local utilities ---

   private static String get(String key) {
      return Text.get(DirectEnum.class,key);
   }

   private static int find(String[] table, String s) {
      for (int i=0; i<table.length; i++) {
         if (s.equals(table[i])) return i;
      }
      return -1;
   }

// --- sides ---

   private static final int SIDES_MIN = 0;

   public static final int SIDES_SINGLE = 0;
   public static final int SIDES_DUPLEX = 1;
   public static final int SIDES_TUMBLE = 2;

   private static final int SIDES_MAX = 2;

   private static String[] sidesTable = {
         get("si0"),
         get("si1"),
         get("si2")
      };

   private static Sides[] sidesAttribute = {
         Sides.ONE_SIDED,
         Sides.DUPLEX,
         Sides.TUMBLE
      };

   public static int toSides(String s) throws ValidationException {
      int i = find(sidesTable,s);
      if (i == -1) throw new ValidationException(Text.get(DirectEnum.class,"e1",new Object[] { s }));
      return i;
   }

   public static String fromSides(int sides) {
      return sidesTable[sides];
   }

   public static Sides attributeSides(int sides) {
      return sidesAttribute[sides];
   }

   public static EnumeratedType sidesType = new EnumeratedType() {
      public int toIntForm(String s) throws ValidationException { return toSides(s); }
      public String fromIntForm(int i) { return fromSides(i); }
   };

   public static void validateSides(Integer sides) throws ValidationException {
      if (sides != null) validateSides(sides.intValue());
   }

   public static void validateSides(int sides) throws ValidationException {
      if (sides < SIDES_MIN || sides > SIDES_MAX) {
         throw new ValidationException(Text.get(DirectEnum.class,"e2",new Object[] { Convert.fromInt(sides) }));
      }
   }

// --- collate ---

   // the reason I'm using this instead of a boolean is,
   // we already have combo box utilities for int enums.

   private static final int COLLATE_MIN = 0;

   public static final int COLLATE_NO = 0;
   public static final int COLLATE_YES = 1;

   private static final int COLLATE_MAX = 1;

   private static String[] collateTable = {
         get("co0"),
         get("co1")
      };

   private static SheetCollate[] collateAttribute = {
         SheetCollate.UNCOLLATED,
         SheetCollate.COLLATED
      };

   public static int toCollate(String s) throws ValidationException {
      int i = find(collateTable,s);
      if (i == -1) throw new ValidationException(Text.get(DirectEnum.class,"e5",new Object[] { s }));
      return i;
   }

   public static String fromCollate(int collate) {
      return collateTable[collate];
   }

   public static SheetCollate attributeCollate(int collate) {
      return collateAttribute[collate];
   }

   public static EnumeratedType collateType = new EnumeratedType() {
      public int toIntForm(String s) throws ValidationException { return toCollate(s); }
      public String fromIntForm(int i) { return fromCollate(i); }
   };

   public static void validateCollate(Integer collate) throws ValidationException {
      if (collate != null) validateCollate(collate.intValue());
   }

   public static void validateCollate(int collate) throws ValidationException {
      if (collate < COLLATE_MIN || collate > COLLATE_MAX) {
         throw new ValidationException(Text.get(DirectEnum.class,"e6",new Object[] { Convert.fromInt(collate) }));
      }
   }

// --- fidelity ---

   // the reason I'm using this instead of a boolean is,
   // we already have combo box utilities for int enums.

   private static final int FIDELITY_MIN = 0;

   public static final int FIDELITY_NO = 0;
   public static final int FIDELITY_YES = 1;

   private static final int FIDELITY_MAX = 1;

   private static String[] fidelityTable = {
         get("fi0"),
         get("fi1")
      };

   private static Fidelity[] fidelityAttribute = {
         Fidelity.FIDELITY_FALSE,
         Fidelity.FIDELITY_TRUE
      };

   public static int toFidelity(String s) throws ValidationException {
      int i = find(fidelityTable,s);
      if (i == -1) throw new ValidationException(Text.get(DirectEnum.class,"e7",new Object[] { s }));
      return i;
   }

   public static String fromFidelity(int fidelity) {
      return fidelityTable[fidelity];
   }

   public static Fidelity attributeFidelity(int fidelity) {
      return fidelityAttribute[fidelity];
   }

   public static EnumeratedType fidelityType = new EnumeratedType() {
      public int toIntForm(String s) throws ValidationException { return toFidelity(s); }
      public String fromIntForm(int i) { return fromFidelity(i); }
   };

   public static void validateFidelity(Integer fidelity) throws ValidationException {
      if (fidelity != null) validateFidelity(fidelity.intValue());
   }

   public static void validateFidelity(int fidelity) throws ValidationException {
      if (fidelity < FIDELITY_MIN || fidelity > FIDELITY_MAX) {
         throw new ValidationException(Text.get(DirectEnum.class,"e8",new Object[] { Convert.fromInt(fidelity) }));
      }
   }

}

