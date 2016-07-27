/*
 * RollEnum.java
 */

package com.lifepics.neuron.axon;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.EnumeratedType;

/**
 * A utility class containing conversion functions for enumerated types.
 */

public class RollEnum {

// --- local utilities ---

   private static String get(String key) {
      return Text.get(RollEnum.class,key);
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
   //    Roll.SOURCE_X
   //    Roll.OLDSHC_X
   //    Roll.STATUS_ROLL_X
   //    Roll.HOLD_ROLL_X
   //    Roll.STATUS_FILE_X

// --- source ---

   private static String[] sourceTable = {
         get("so0"),
         get("so1"),
         get("so2"),
         get("so3"),
         get("so4"),
         get("so5"),
         get("so6"),
         get("so7"),
         get("so8"),
         get("so9")
      };
   private static String special9 = get("sx9");

   public static int toSource(String s) throws ValidationException {
      int i = find(sourceTable,s);
      if (i == -1) {
         if (s.equals(special9)) i = 9;
         else throw new ValidationException(Text.get(RollEnum.class,"e1",new Object[] { s }));
      }
      return i;
   }

   public static String fromSource(int source) {
      return sourceTable[source];
   }

   public static EnumeratedType sourceType = new EnumeratedType() {
      public int toIntForm(String s) throws ValidationException { return toSource(s); }
      public String fromIntForm(int i) { return fromSource(i); }
   };

// --- old status-hold code ---

   private static String[] oldshcTable = {
         get("os0"),
         get("os1"),
         get("os2"),
         get("os3"),
         get("os4"),
         get("os5"),
         get("os6"),
         get("os7")
      };

   public static int toOldSHC(String s) throws ValidationException {
      int i = find(oldshcTable,s);
      if (i == -1) throw new ValidationException(Text.get(RollEnum.class,"e2",new Object[] { s }));
      return i;
   }

// --- roll status ---

   private static String[] rollStatusTable = {
         get("rs0"),
         get("rs1"),
         get("rs2"),
         get("rs3"),
         get("rs4"),
         get("rs5")
      };

   public static int toRollStatus(String s) throws ValidationException {
      int i = find(rollStatusTable,s);
      if (i == -1) throw new ValidationException(Text.get(RollEnum.class,"e4",new Object[] { s }));
      return i;
   }

   public static String fromRollStatus(int status) {
      return rollStatusTable[status];
   }

   public static EnumeratedType rollStatusType = new EnumeratedType() {
      public int toIntForm(String s) throws ValidationException { return toRollStatus(s); }
      public String fromIntForm(int i) { return fromRollStatus(i); }
   };

// --- roll hold ---

   private static String[] rollHoldTable = {
         get("rh0"),
         get("rh1"),
         get("rh2"),
         get("rh3"),
         get("rh4"),
         get("rh5"),
         get("rh6")
      };

   public static int toRollHold(String s) throws ValidationException {
      int i = find(rollHoldTable,s);
      if (i == -1) throw new ValidationException(Text.get(RollEnum.class,"e5",new Object[] { s }));
      return i;
   }

   public static String fromRollHold(int hold) {
      return rollHoldTable[hold];
   }

   public static EnumeratedType rollHoldType = new EnumeratedType() {
      public int toIntForm(String s) throws ValidationException { return toRollHold(s); }
      public String fromIntForm(int i) { return fromRollHold(i); }
   };

// --- transform type ---

   private static String[] transformTypeTable = {
         get("tt0"),
         get("tt1"),
         get("tt2")
      };

   public static int toTransformType(String s) throws ValidationException {
      int i = find(transformTypeTable,s);
      if (i == -1) throw new ValidationException(Text.get(RollEnum.class,"e6",new Object[] { s }));
      return i;
   }

   public static String fromTransformType(int transformType) {
      return transformTypeTable[transformType];
   }

   public static EnumeratedType transformTypeType = new EnumeratedType() {
      public int toIntForm(String s) throws ValidationException { return toTransformType(s); }
      public String fromIntForm(int i) { return fromTransformType(i); }
   };

// --- file status ---

   private static String[] fileStatusTable = {
         get("fs0"),
         get("fs1"),
         get("fs2")
      };

   public static int toFileStatus(String s) throws ValidationException {
      int i = find(fileStatusTable,s);
      if (i == -1) throw new ValidationException(Text.get(RollEnum.class,"e3",new Object[] { s }));
      return i;
   }

   public static String fromFileStatus(int status) {
      return fileStatusTable[status];
   }

   public static EnumeratedType fileStatusType = new EnumeratedType() {
      public int toIntForm(String s) throws ValidationException { return toFileStatus(s); }
      public String fromIntForm(int i) { return fromFileStatus(i); }
   };

}

