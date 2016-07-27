/*
 * OrderEnum.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.EnumeratedType;

/**
 * A utility class containing conversion functions for enumerated types.
 */

public class OrderEnum {

// --- local utilities ---

   private static String get(String key) {
      return Text.get(OrderEnum.class,key);
   }

   private static int find(String[] table, String s) {
      for (int i=0; i<table.length; i++) {
         if (s.equals(table[i])) return i;
      }
      return -1;
   }

// --- conversion functions (cf. Convert) ---

   // these depend upon the *actual numerical values* of the following enumerations
   //
   //    Order.STATUS_ORDER_X
   //    Order.HOLD_X
   //    Order.DUE_STATUS_X
   //    Order.FORMAT_X
   //    Order.STATUS_ITEM_X
   //    Backprint.TYPE_X
   //    COS media surface

// --- format ---

   private static String[] formatTable = {
         get("fo0"),
         get("fo1"),
         get("fo2"),
         get("fo3"),
         get("fo4"),
         get("fo5"),
         get("fo6"),
         get("fo7"),
         get("fo8"),
         get("fo9"),
         get("fo10"),
         get("fo11"),
         get("fo12"),
         get("fo13"),
         get("fo14"),
         get("fo15"),
         get("fo16"),
         get("fo17"),
         get("fo18"),
         get("fo19"),
         get("fo20"),
         get("fo21"),
         get("fo22"),
         get("fo23"),
         get("fo24")
      };
   private static String special7 = get("fx7");
   private static String special10 = get("fx10");
   private static String special14a = get("fx14a");
   private static String special14b = get("fx14b");
   private static String special19 = get("fx19");
   private static String special21 = get("fx21");
   private static String special22 = get("fx22");

   public static int toFormat(String s) throws ValidationException {
      int i = find(formatTable,s);
      if (i == -1) {
         if      (s.equals(special7)) i = 7;
         else if (s.equals(special10)) i = 10;
         else if (s.equals(special14a)) i = 14;
         else if (s.equals(special14b)) i = 14;
         else if (s.equals(special19)) i = 19;
         else if (s.equals(special21)) i = 21;
         else if (s.equals(special22)) i = 22;
         else throw new ValidationException(Text.get(OrderEnum.class,"e1",new Object[] { s }));
      }
      return i;
   }

   public static String fromFormat(int format) {
      return formatTable[format];
   }

   public static EnumeratedType formatType = new EnumeratedType() {
      public int toIntForm(String s) throws ValidationException { return toFormat(s); }
      public String fromIntForm(int i) { return fromFormat(i); }
   };

// --- order status ---

   private static String[] orderStatusTable = {
         get("os0"),
         get("os1"),
         get("os2"),
         get("os3"),
         get("os4"),
         get("os5"),
         get("os5a"),
         get("os5b"),
         get("os6"),
         get("os7"),
         get("os8"),
         get("os9"),
         get("os10"),
         get("os11")
      };

   public static int toOrderStatus(String s) throws ValidationException {
      int i = find(orderStatusTable,s);
      if (i == -1) throw new ValidationException(Text.get(OrderEnum.class,"e2",new Object[] { s }));
      return i;
   }

   public static String fromOrderStatus(int status) {
      return orderStatusTable[status];
   }

   public static EnumeratedType orderStatusType = new EnumeratedType() {
      public int toIntForm(String s) throws ValidationException { return toOrderStatus(s); }
      public String fromIntForm(int i) { return fromOrderStatus(i); }
   };

// --- hold ---

   private static String[] holdTable = {
         get("ho0"),
         get("ho1"),
         get("ho2"),
         get("ho3"),
         get("ho4")
      };

   public static int toHold(String s) throws ValidationException {
      int i = find(holdTable,s);
      if (i == -1) throw new ValidationException(Text.get(OrderEnum.class,"e3",new Object[] { s }));
      return i;
   }

   public static String fromHold(int hold) {
      return holdTable[hold];
   }

   public static EnumeratedType holdType = new EnumeratedType() {
      public int toIntForm(String s) throws ValidationException { return toHold(s); }
      public String fromIntForm(int i) { return fromHold(i); }
   };

// --- due-status ---

   private static String[] dueStatusTable = {
         get("ds0"),
         get("ds1"),
         get("ds2")
      };

   public static int toDueStatus(String s) throws ValidationException {
      int i = find(dueStatusTable,s);
      if (i == -1) throw new ValidationException(Text.get(OrderEnum.class,"e8",new Object[] { s }));
      return i;
   }

   public static String fromDueStatus(int dueStatus) {
      return dueStatusTable[dueStatus];
   }

   public static EnumeratedType dueStatusType = new EnumeratedType() {
      public int toIntForm(String s) throws ValidationException { return toDueStatus(s); }
      public String fromIntForm(int i) { return fromDueStatus(i); }
   };

// --- item status ---

   private static String[] itemStatusTable = {
         get("is0"),
         get("is1"),
         get("is2"),
         get("is3"),
         get("is4")
      };

   public static int toItemStatus(String s) throws ValidationException {
      int i = find(itemStatusTable,s);
      if (i == -1) throw new ValidationException(Text.get(OrderEnum.class,"e4",new Object[] { s }));
      return i;
   }

   public static String fromItemStatus(int status) {
      return itemStatusTable[status];
   }

   public static EnumeratedType itemStatusType = new EnumeratedType() {
      public int toIntForm(String s) throws ValidationException { return toItemStatus(s); }
      public String fromIntForm(int i) { return fromItemStatus(i); }
   };

// --- backprint type ---

   // this isn't a property of orders, but the code is similar

   private static String[] backprintTypeTable = {
         get("bp0"),
         get("bp1"),
         get("bp2"),
         get("bp3"),
         get("bp4"),
         get("bp5"),
         get("bp6"),
         get("bp7"),
         get("bp8"),
         get("bp9"),
         get("bp10"),
         get("bp11"),
         get("bp12"),
         get("bp13")
      };

   public static int toBackprintType(String s) throws ValidationException {
      int i = find(backprintTypeTable,s);
      if (i == -1) throw new ValidationException(Text.get(OrderEnum.class,"e5",new Object[] { s }));
      return i;
   }

   public static String fromBackprintType(int type) {
      return backprintTypeTable[type];
   }

   public static EnumeratedType backprintTypeType = new EnumeratedType() {
      public int toIntForm(String s) throws ValidationException { return toBackprintType(s); }
      public String fromIntForm(int i) { return fromBackprintType(i); }
   };

// --- media surface (internal) ---

   // this isn't a property of orders, either
   //
   // note special handling to allow negative codes.
   // this could have been done by array offset,
   // but since the codes are systematic I did it this way.

   private static String codeInternal = get("sun") + ' ';
   private static final int CODE_MAX = 4;

   private static String[] surfaceTableInternal = {
         get("su0"),
         get("su1"),
         get("su2"),
         get("su3"),
         get("su4"),
         get("su5"),
         get("su6"),
         get("su7"),
         get("su8"),
         get("su9"),
         get("su10"),
         get("su11"),
         get("su12")
      };

   public static int toSurfaceInternal(String s) throws ValidationException {
      if (s.startsWith(codeInternal)) {
         int i = Convert.toInt(s.substring(codeInternal.length()));
         if (i < 1 || i > CODE_MAX) throw new ValidationException(Text.get(OrderEnum.class,"e7",new Object[] { Convert.fromInt(i) }));
         return -i;
      } else {
         int i = find(surfaceTableInternal,s);
         if (i == -1) throw new ValidationException(Text.get(OrderEnum.class,"e6",new Object[] { s }));
         return i;
      }
   }

   public static String fromSurfaceInternal(int surface) {
      if (surface < 0) {
         return codeInternal + Convert.fromInt(-surface);
      } else {
         return surfaceTableInternal[surface];
      }
   }

   public static EnumeratedType surfaceInternalType = new EnumeratedType() {
      public int toIntForm(String s) throws ValidationException { return toSurfaceInternal(s); }
      public String fromIntForm(int i) { return fromSurfaceInternal(i); }
   };

   public static void validateSurface(int surface, boolean allowRaw) throws ValidationException {
      int min = allowRaw ? -CODE_MAX : 0;
      int max = surfaceTableInternal.length - 1;
      if (surface < min || surface > max) {
         throw new ValidationException(Text.get(OrderEnum.class,"e9",new Object[] { Convert.fromInt(surface) }));
      }
   }

// --- media surface (external) ---

   private static String codeExternal = get("sxn") + ' ';

   private static String[] surfaceTableExternal = {
         get("sx0"),
         get("sx1"),
         get("sx2"),
         get("sx3"),
         get("sx4"),
         get("sx5"),
         get("sx6"),
         get("sx7"),
         get("sx8"),
         get("sx9"),
         get("sx10"),
         get("sx11"),
         get("sx12")
      };

   public static String fromSurfaceExternal(int surface) {
      if (surface < 0) {
         return codeExternal + Convert.fromInt(-surface);
      } else {
         return surfaceTableExternal[surface];
      }
   }

}

