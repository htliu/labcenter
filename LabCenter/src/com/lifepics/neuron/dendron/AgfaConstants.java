/*
 * AgfaConstants.java
 */

package com.lifepics.neuron.dendron;

/**
 * Some non-simple constants, moved out of class {@link Agfa}
 * to prevent link errors and exceptions at unexpected times.
 */

public class AgfaConstants {

// --- more constants ---

   // these are constants that cause the class to load if you refer to them.
   // since Agfa can't load if it can't find Agfa.jar and ConnectionKit.dll,
   // you could get in all kinds of trouble just by referring to a constant.
   // but, now it's fixed.

   // print width (also length)
   //
   // although other values are possible, the standard values are an enumeration.
   // however, since we want the enumeration to be easy to manipulate in code,
   // don't define one constants per enumerated value, instead define a pair of arrays.
   //
   // actually, I added 3" for completeness, since 3x5 prints are fairly standard.
   // the conversion to millimeters is tricky, because the rounding isn't consistent.
   // everything in [0,0.4] rounds down, and everything in [0.8,0.99] rounds up,
   // but in between we have 3.25" (0.55) and 4.75" (0.65) rounding down.  but,
   // it's not just a weird threshold, because 4" (0.6) and 8.25" (0.55) round up.
   // fortunately, 3" is 76.2 mm, which is not in the ambiguous range.
   //
   // 5.25, 5.33, and 5.5 added for use as lengths only; the first two are in the
   // round-down range, the third is ambiguous, 139.7, and I chose to round it up.
   // also I added 18 later on.
   //
   // *** any new entries with nonstandard rounding must be added to EditSKUDialog ***
   //
   public static final String[] PRINT_WIDTH_NAMES  = { "3", "3.25", "3.5", "3.75", "4", "4.75", "5", "5.25", "5.33", "5.5", "6", "6.5", "7", "8", "8.25", "8.5", "10", "11", "12", "18" };
   public static final int[]    PRINT_WIDTH_VALUES = { 76,  82,     89,    95,    102, 120,    127, 133,    135,    140,   152, 165,   178, 203, 210,    216,    254,  279,  305,  457  };

   // rotation
   public static final String[] ROTATION_NAMES  = { "0", "90", "180", "270" };
   public static final int[]    ROTATION_VALUES = {  0,   900,  1800,  2700 };

}

