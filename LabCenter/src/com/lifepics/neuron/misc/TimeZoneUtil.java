/*
 * TimeZoneUtil.java
 */

package com.lifepics.neuron.misc;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.TimeZone;

/**
 * A utility class for understanding the structure of time zone names.
 * This should really only be used in the config dialog -- everywhere
 * else we just want to *use* the names.
 *
 * The goal is to build something here that will be fairly robust
 * when the list of time zones defined in Java inevitably changes.
 */

public class TimeZoneUtil {

// --- split and join ---

   /**
    * Check whether a name is an abbreviation, i.e., all uppercase.
    * Most but not all abbreviations are three letters.
    * Some country names are all UC, exclude them individually.
    */
   private static boolean isAbbrev(String name) {

      if ( ! name.equals(name.toUpperCase()) ) return false; // not all UC

      if (    name.equals("GB")
           || name.equals("NZ")
           || name.equals("NZ-CHAT") // no idea here either
           || name.equals("PRC")
           || name.equals("ROK")
           || name.equals("W-SU") ) return false; // exception to the rule
           // no idea what W-SU is -- maybe western Soviet Union?

      return true;
   }

   public static class Split {
      public String first; // usually a country but not always
      public String second; // blank, or zone within a country
   }

   private static char DELIM = '/';
   private static String ABBREV = "Abbreviation";

   public static Split split(String name) {
      return split(name,new Split());
   }
   public static Split split(String name, Split sp) {
      int i = name.indexOf(DELIM);
      if (i != -1) { // slash, split
         sp.first  = name.substring(0,i);
         sp.second = name.substring(i+1);
      } else if (isAbbrev(name)) { // abbrev, fake split
         sp.first  = ABBREV;
         sp.second = name;
      } else { // not abbrev, no split
         sp.first  = name;
         sp.second = "";
      }
      // note, in every case we update both fields
      return sp;
   }
   // note, there really can be more than one slash,
   // for example America/Indiana/Indianapolis

   public static String join(Split sp) {
      return join(sp.first,sp.second);
   }
   public static String join(String first, String second) {
      if (first.equals(ABBREV)) return second;
      if (second.length() == 0) return first;
      if (first .length() == 0) return second; // not a case we produce, but handle it from UI
      return first + DELIM + second;
   }

   // there are many cases where split and join don't invert.
   //
   // join(split(.)) : file -> file produces different value.
   //
   //  * Abbreviation/x -> x
   //  * x/ -> x
   //  * /x -> x
   //  * Abbreviation -> empty string
   //
   // split(join(.)) : UI -> UI produces different value.
   //
   //  * Abbreviation,x/y -> x,y
   //  * Abbreviation,Xy -> Xy,E
   //  * x/y,E -> x,y
   //  * E,x/y -> x,y
   //  * XY,E -> Abbreviation,XY
   //  * E,XY -> Abbreviation,XY
   //  * E,x -> x,E
   //  * x/y,z -> x,y/z
   //
   // fortunately these aren't likely to come up in practice!

// --- object arrays ---

   /**
    * Hard-coded knowledge of which codes aren't country names.
    */
   private static boolean isMisc(String first) {

      return (    first.equals(ABBREV)
               || first.equals("Etc")
               || first.equals("Greenwich")
               || first.equals("SystemV")
               || first.equals("Universal")
               || first.equals("Zulu") ); // meaning Z, not Africans
   }

   private static Object[] arrayEmpty = new Object[] { "" };
   private static Object[] arrayFirst;
   private static HashMap  arraySecondMap; // from String to Object[]

   public static Object[] getArrayFirst() { return arrayFirst; }
   public static Object[] getArraySecond(String first) {
      Object[] arraySecond = (Object[]) arraySecondMap.get(first);
      return (arraySecond != null) ? arraySecond : arrayEmpty;
   }
   // (*) use arrayEmpty instead of an empty array because if there are
   // no items, the JComboBox popup shows a big blank unselectable area.

   static {

      LinkedList first = new LinkedList();
      LinkedList firstMisc = new LinkedList();

      HashMap secondMap = new HashMap();

      Split sp = new Split(); // save a little on memory allocation

   // distribute data

      String[] names = TimeZone.getAvailableIDs();
      for (int i=0; i<names.length; i++) {
         split(names[i],sp);

         LinkedList second = (LinkedList) secondMap.get(sp.first);
         if (second == null) {
            (isMisc(sp.first) ? firstMisc : first).add(sp.first);
            second = new LinkedList();
            secondMap.put(sp.first,second);
         }

         second.add(sp.second); // assume no duplicates in the list
      }

      // note, we create a map entry even if the only second part
      // is the empty string.  it makes the coding easier here,
      // and maybe it'll make things easier for the UI coding too.
      // see also (*) above.

   // build first array

      // Arrays.sort is probably faster, but it's not convenient here
      Collections.sort(first);
      Collections.sort(firstMisc);

      first.addAll(firstMisc);

      arrayFirst = first.toArray();

   // build second arrays

      Iterator i = secondMap.entrySet().iterator();
      while (i.hasNext()) {
         Map.Entry entry = (Map.Entry) i.next();

         LinkedList second = (LinkedList) entry.getValue();
         Object[] arraySecond = second.toArray();
         Arrays.sort(arraySecond);
         entry.setValue(arraySecond); // overwrite list
      }

      arraySecondMap = secondMap;
   }

}

