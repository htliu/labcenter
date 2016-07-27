/*
 * AppVersion.java
 */

package com.lifepics.neuron.admin;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;

/**
 * Some helper functions related to the app version.
 */

public class AppVersion {

   // basically, these are conversion functions like in Convert,
   // except that there's an intermediate int[3] representation

// --- constants ---

   private static String prefix = Text.get(AppVersion.class,"s1");
   private static String suffix = Text.get(AppVersion.class,"s2");

   private static String DOT_NORMAL = ".";
   private static String DOT_REGEX = "\\.";
   private static String UNDERSCORE = "_";

// --- int <-> jarfile ---

   public static int jarfileToInt(String s) throws ValidationException {
      if ( ! s.startsWith(prefix) || ! s.endsWith(suffix) ) {
         throw new ValidationException(Text.get(AppVersion.class,"e3",new Object[] { s }));
      }
      s = s.substring(prefix.length(),s.length()-suffix.length());
      return pack(toIntArraySplit(s,UNDERSCORE));
   }

   public static String jarfileFromInt(int t) {
      return prefix + join(fromIntArray(unpack(t)),UNDERSCORE) + suffix;
   }

// --- int <-> version ---

   public static int versionToInt(String s) throws ValidationException {
      return pack(toIntArraySplit(s,DOT_REGEX));
   }

   public static String versionFromInt(int t) {
      return join(fromIntArray(unpack(t)),DOT_NORMAL);
   }

// --- int[] <-> String (useful combo) ---

   public static int[] toIntArraySplit(String arg, String delim) throws ValidationException {
      String[] s = arg.split(delim,-1);
      if (s.length != 3) throw new ValidationException(Text.get(AppVersion.class,"e1",new Object[] { arg }));
      int[] v = toIntArray(s);
      if (v[0] < 1 || v[1] < 0 || v[1] > 9 || v[2] < 0 || v[2] > 9) {
         throw new ValidationException(Text.get(AppVersion.class,"e2",new Object[] { arg }));
      }
      return v;
   }

// --- int <-> int[] ---

   public static int pack(int[] v) {
      return v[0] * 100 + v[1] * 10 + v[2];
   }

   public static int[] unpack(int t) {
      int[] v = new int[3];
      v[2] = t % 10;
      t /= 10;
      v[1] = t % 10;
      t /= 10;
      v[0] = t;
      return v;
   }

// --- int[] <-> String[] ---

   public static int[] toIntArray(String[] s) throws ValidationException {
      int[] v = new int[s.length];
      for (int i=0; i<s.length; i++) v[i] = Convert.toInt(s[i]);
      return v;
   }

   public static String[] fromIntArray(int[] v) {
      String[] s = new String[v.length];
      for (int i=0; i<v.length; i++) s[i] = Convert.fromInt(v[i]);
      return s;
   }

// --- String[] <-> String ---

   // use String.split(delim,-1) for the first side

   public static String join(String[] s, String delim) {
      StringBuffer b = new StringBuffer();
      for (int i=0; i<s.length; i++) {
         if (i != 0) b.append(delim);
         b.append(s[i]);
      }
      return b.toString();
   }
   // these aren't inverses unless you exclude String[0] as a value

}

