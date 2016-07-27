/*
 * ProMode.java
 */

package com.lifepics.neuron.axon;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.EnumeratedType;

/**
 * A utility class that holds the global pro mode flag.
 * This is a bit ugly, but it really is a global,
 * and passing it around everywhere is pretty ugly too.
 */

public class ProMode {

// --- enum values ---

   private static final int PRO_MODE_MIN = 0;

   public static final int NORMAL  = 0; // NORMAL
   public static final int PRO_OLD = 1; // PRO
   public static final int PRO_NEW = 2; // STUDIO
   public static final int MERGED  = 3; // MERGED

   private static final int PRO_MODE_MAX = 3;

// --- static functions used by app ---

   // these plus their negations cover all possible permutations
   // (of the three main types)

   public static boolean isPro   (int proMode) { return (proMode != NORMAL && proMode != MERGED); }
   public static boolean isProOld(int proMode) { return (proMode == PRO_OLD); }
   public static boolean isProNew(int proMode) { return (proMode == PRO_NEW); }

   // merged behaves just like normal except in special cases where we test for it explicitly
   public static boolean isMerged(int proMode) { return (proMode == MERGED); }

// --- static variable used by axon ---

   private static Integer proModeStatic; // start out null to guarantee no early misuse
   public static void setProMode(int proMode) { proModeStatic = new Integer(proMode); }

   public static boolean isPro()    { return isPro   (proModeStatic.intValue()); }
   public static boolean isProOld() { return isProOld(proModeStatic.intValue()); }
   public static boolean isProNew() { return isProNew(proModeStatic.intValue()); }

   public static boolean isMerged() { return isMerged(proModeStatic.intValue()); }

// --- enum implementation ---

   private static String[] proModeTable = {
         Text.get(ProMode.class,"pm0"),
         Text.get(ProMode.class,"pm1"),
         Text.get(ProMode.class,"pm2"),
         Text.get(ProMode.class,"pm3")
      };

   private static int toProMode(String s) throws ValidationException {
      for (int i=0; i<proModeTable.length; i++) {
         if (s.equals(proModeTable[i])) return i;
      }
      throw new ValidationException(Text.get(ProMode.class,"e1",new Object[] { s }));
   }

   private static String fromProMode(int proMode) {
      return proModeTable[proMode];
   }

   public static EnumeratedType proModeType = new EnumeratedType() {
      public int toIntForm(String s) throws ValidationException { return toProMode(s); }
      public String fromIntForm(int i) { return fromProMode(i); }
   };

   public static void validateProMode(int proMode) throws ValidationException {
      if (proMode < PRO_MODE_MIN || proMode > PRO_MODE_MAX) throw new ValidationException(Text.get(ProMode.class,"e2",new Object[] { Convert.fromInt(proMode) }));
   }

}

