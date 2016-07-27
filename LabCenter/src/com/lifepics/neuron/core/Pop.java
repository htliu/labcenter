/*
 * Pop.java
 */

package com.lifepics.neuron.core;

import java.util.logging.Level;

import java.awt.Component;
import javax.swing.JOptionPane;

/**
 * A utility class for popping up simple messages with JOptionPane.
 * We used to call JOptionPane directly everywhere, but now that
 * we want to put line breaks in long lines, we really need to have
 * a standard set of wrapper functions.
 *
 * You'd think this would go in the gui package, but it turns out
 * we need to call it from misc/AppUtil, and anyway it's a fairly
 * low-level function, much like logging.
 */

public class Pop {

// --- JOptionPane wrappers ---

   public static void error(Component c, String message, String title) {
      JOptionPane.showMessageDialog(c,breakAll(message),title,JOptionPane.ERROR_MESSAGE);
   }

   public static void error(Component c, Throwable t, String title) {
      JOptionPane.showMessageDialog(c,breakAll(ChainedException.format(t)),title,JOptionPane.ERROR_MESSAGE);
   }

   public static void warning(Component c, String message, String title) {
      JOptionPane.showMessageDialog(c,breakAll(message),title,JOptionPane.WARNING_MESSAGE);
   }

   public static void warningVariant(Component c, Object message, String title) {
      JOptionPane.showMessageDialog(c,breakObject(message),title,JOptionPane.WARNING_MESSAGE);
   }
   // when the message is a string, warningVariant is identical to warning,
   // so why not replace that with this?  because I like the type checking.

   public static void info(Component c, String message, String title) {
      JOptionPane.showMessageDialog(c,breakAll(message),title,JOptionPane.INFORMATION_MESSAGE);
   }

   public static boolean confirm(Component c, String message, String title) {
      int result = JOptionPane.showConfirmDialog(c,breakAll(message),title,JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE);
      return (result == JOptionPane.YES_OPTION);
   }

   public static boolean confirmVariant(Component c, Object message, String title) {
      int result = JOptionPane.showConfirmDialog(c,breakObject(message),title,JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE);
      return (result == JOptionPane.YES_OPTION);
   }

   /**
    * @return Null if canceled.
    */
   public static String inputString(Component c, String message, String title) {
      return JOptionPane.showInputDialog(c,breakAll(message),title,JOptionPane.QUESTION_MESSAGE);
   }

   /**
    * @return Null if canceled.
    */
   public static Object inputSelect(Component c, String message, String title, Object[] array) {
      return JOptionPane.showInputDialog(c,breakAll(message),title,JOptionPane.QUESTION_MESSAGE,/* icon = */ null,array,array[0]);
   }

// --- diagnostics ---

   public static void diagnostics(Component c, String description, Throwable t) {

      Log.log(Level.SEVERE,Pop.class,"e1",new Object[] { description },t);

      String line1 = ChainedException.format(t);
      String line2 = t.getClass().getName();
      StackTraceElement[] ste = t.getStackTrace();
      String line3 = (ste.length > 0) ? ste[0].toString() : Text.get(Pop.class,"s2");
      Pop.error(c,Text.get(Pop.class,"e2",new Object[] { description, line1, line2, line3 }),Text.get(Pop.class,"s1"));
   }

// --- line-break functions ---

   // aka. line wrap, but there's already another kind of wrapping in here

   // the main source of too-long lines is ChainedException.  if you trace
   // it out, some of those go to non-UI destinations, some go to gui/Blob,
   // and the rest come here.  so, all of them that need to be line-broken
   // will get line-broken.

   private static final int WIDTH_STANDARD = Text.getInt(Pop.class,"w1");

   /**
    * Line-break a string consisting of exactly one line.
    */
   private static String breakOne(String message) {
      return breakOne(message,WIDTH_STANDARD);
   }
   private static String breakOne(String message, int width) {

      // very common case, optimize it
      int len = message.length();
      if (len <= width) return message;

      // make sure loop will terminate
      if (width < 1) throw new IllegalArgumentException();

      StringBuffer b = new StringBuffer();
      int base = 0;

      while (base+width < len) { // another break needed

      // find break point

         // we don't generate anything with tabs or CRs, and LF
         // should be handled by breakAll, so space is the only
         // kind of whitespace that can occur

         // base+width is in range because the loop didn't exit

         int i = message.lastIndexOf(' ',base+width); // allow i=base+width
         if (i < base) i = base+width; // hard break, includes i=-1

      // widen to remove spaces

         // an uncommon case, but let's handle it gracefully

         int left = i;
         while (left > base && message.charAt(left-1) == ' ') left--;

         int right = i;
         while (right < len && message.charAt(right) == ' ') right++; // rechecks the found space, if any

      // make appropriate changes

         if (left > base) {
            if (b.length() > 0) b.append('\n');
            b.append(message.substring(base,left));
         }
         // else that segment was all spaces!

         base = right;
      }

      if (len > base) {
         if (b.length() > 0) b.append('\n');
         b.append(message.substring(base,len));
      }
      // else that segment was all spaces!

      // note that (b.length() > 0) is a good test because we never try
      // to append an empty string as a line.

      // kind of arbitrary to remove spaces around breaks but not remove
      // spaces at the front and end of the string, but that's my choice.
      // if there's indentation, it's nice to leave it on the first line.

      return b.toString();
   }

   /**
    * Line-break a string consisting of one or more lines
    * by breaking each line independently.
    */
   private static String breakAll(String message) {
      return breakAll(message,WIDTH_STANDARD);
   }
   private static String breakAll(String message, int width) {

      // very common case, optimize it
      int len = message.length();
      if (len <= width) return message;

      StringBuffer b = new StringBuffer();

      String[] s = message.split("\n",-1); // -1 to stop weird default behavior
      for (int i=0; i<s.length; i++) {
         if (i != 0) b.append('\n');
         b.append(breakOne(s[i],width));
      }
      // note that we use (i != 0) as the condition, not (b.length() > 0).
      // the result is that all original line breaks are preserved,
      // even if there are lines at the start that are empty strings.
      // it's possible for breakOne to return an empty string if the
      // line is a large number of spaces, but that should be rare.

      return b.toString();
   }

   private static Object[] breakArray(Object[] message) {
      return breakArray(message,WIDTH_STANDARD);
   }
   private static Object[] breakArray(Object[] message, int width) {
      for (int i=0; i<message.length; i++) {
         message[i] = breakObject(message[i],width);
      }
      return message; // modify array in place is OK here
   }

   private static Object breakObject(Object message) {
      return breakObject(message,WIDTH_STANDARD);
   }
   private static Object breakObject(Object message, int width) {
      if      (message instanceof String  ) return breakAll  ((String)   message,width);
      else if (message instanceof Object[]) return breakArray((Object[]) message,width);
      else                                  return message; // component, pass through
   }

}

