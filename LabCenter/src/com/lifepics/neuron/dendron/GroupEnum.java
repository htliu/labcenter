/*
 * GroupEnum.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Text;

/**
 * A utility class containing conversion functions for enumerated types.
 */

public class GroupEnum {

// --- local utilities ---

   private static String get(String key) {
      return Text.get(GroupEnum.class,key);
   }

// --- conversion functions (cf. Convert) ---

   // these depend upon the *actual numerical values* of the following enumerations
   //
   //    Group.STATUS_GROUP_X

// --- group status ---

   private static String[] groupStatusTable = {
         get("gs-2"),
         get("gs-1"),
         get("gs0"),
         get("gs1")
      };

   // there is no toGroupStatus, because this is not a complete enumeration.
   // in other words, there are string status values that can't be converted
   // into an integer from the enumeration, for example, "0 / 2 / 1".
   // these mixed statuses have numeric values, but no defined constants.

   public static String fromGroupStatus(int status) {
      return groupStatusTable[status+2];
   }

}

