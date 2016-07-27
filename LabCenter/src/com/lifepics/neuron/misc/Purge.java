/*
 * Purge.java
 */

package com.lifepics.neuron.misc;

/**
 * A utility class containing enumerations for purge functions.
 */

public class Purge {

// --- reason enumeration ---

   public static final int REASON_MANUAL    = 0;
   public static final int REASON_AUTOMATIC = 1;
   public static final int REASON_REBUILD   = 2; // for orders only

   public static String getReasonSuffix(int reason) {
      switch (reason) {
      case REASON_MANUAL:     return "a";
      case REASON_AUTOMATIC:  return "b";
      case REASON_REBUILD:    return "c";
      default:                return "x";
      }
   }

// --- result enumeration ---

   public static final int RESULT_FAILED     = 0; // couldn't delete record
   public static final int RESULT_INCOMPLETE = 1; // deleted record but not all files
   public static final int RESULT_COMPLETE   = 2;
   public static final int RESULT_STOPPING   = 3;

}

