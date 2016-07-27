/*
 * Nullable.java
 */

package com.lifepics.neuron.core;

import java.io.File;
import java.util.Date;
import java.util.LinkedList;

/**
 * A utility class for comparison functions that handle null.
 */

public class Nullable {

   public static boolean equals(String s1, String s2) {
      return (s1 != null) ? s1.equals(s2) : (s2 == null);
   }

   public static boolean equals(Boolean b1, Boolean b2) {
      return (b1 != null) ? b1.equals(b2) : (b2 == null);
   }

   public static boolean equals(Integer i1, Integer i2) {
      return (i1 != null) ? i1.equals(i2) : (i2 == null);
   }

   public static boolean equals(Long l1, Long l2) {
      return (l1 != null) ? l1.equals(l2) : (l2 == null);
   }

   public static boolean equals(Double d1, Double d2) {
      return (d1 != null) ? d1.equals(d2) : (d2 == null);
   }

   public static boolean equals(Date d1, Date d2) {
      return (d1 != null) ? d1.equals(d2) : (d2 == null);
   }

   public static boolean equals(File f1, File f2) {
      return (f1 != null) ? f1.equals(f2) : (f2 == null);
   }

   public static boolean equals(LinkedList l1, LinkedList l2) {
      return (l1 != null) ? l1.equals(l2) : (l2 == null);
   }

   public static boolean equalsObject(Object o1, Object o2) {
      return (o1 != null) ? o1.equals(o2) : (o2 == null);
   }
   // we could actually use this to replace all the others,
   // but I'd rather save it for rare cases ... that's why
   // I'm giving it a different name.  don't use it on
   // structures unless you mean to go through Object.equals!

   public static boolean nbToB(Boolean b) {
      return (b != null) ? b.booleanValue() : false;
   }

   public static Boolean bToNb(boolean b) {
      return (b ? Boolean.TRUE : null);
   }

}

