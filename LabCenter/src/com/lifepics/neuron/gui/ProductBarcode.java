/*
 * ProductBarcode.java
 */

package com.lifepics.neuron.gui;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.EnumeratedType;

/**
 * An enumeration of the different product barcode types we support.
 */

public class ProductBarcode {

// --- local utilities ---

   private static String get(String key) {
      return Text.get(ProductBarcode.class,key);
   }

   private static int find(String[] table, String s) {
      for (int i=0; i<table.length; i++) {
         if (s.equals(table[i])) return i;
      }
      return -1;
   }

// --- enumeration ---

   private static final int TYPE_MIN = 0;

   public static final int TYPE_CODE128_STRING = 0;
   public static final int TYPE_CODE128_DIGITS = 1;
   public static final int TYPE_EAN13_20_5_5   = 2;

   private static final int TYPE_MAX = 2;

   private static String[] typeTable = {
         get("ty0"),
         get("ty1"),
         get("ty2")
      };

   public static int toType(String s) throws ValidationException {
      int i = find(typeTable,s);
      if (i == -1) throw new ValidationException(Text.get(ProductBarcode.class,"e1",new Object[] { s }));
      return i;
   }

   public static String fromType(int type) {
      return typeTable[type];
   }

   public static EnumeratedType typeType = new EnumeratedType() {
      public int toIntForm(String s) throws ValidationException { return toType(s); }
      public String fromIntForm(int i) { return fromType(i); }
   };

   public static void validateType(Integer type) throws ValidationException {
      if (type != null) validateType(type.intValue());
   }

   public static void validateType(int type) throws ValidationException {
      if (type < TYPE_MIN || type > TYPE_MAX) {
         throw new ValidationException(Text.get(ProductBarcode.class,"e2",new Object[] { Convert.fromInt(type) }));
      }
   }

}

