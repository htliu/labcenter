/*
 * StringSelector.java
 */

package com.lifepics.neuron.meta;

import com.lifepics.neuron.core.Text;

import java.util.Stack;

/**
 * A set of selector classes and utilities to use in making a regex-like matcher.
 * Why not just use regex?  This is easier to read and doesn't require the parts
 * to match in order.
 */

public class StringSelector {

// --- readme ---

   // it's a tiny postfix language that embeds in XML without &amp; substitution.
   // lowercase conversion for case-insensitive matching isn't included, do that
   // outside if you want it.  the pattern strings can be lowercased too.
   //
   // | : quote for string literals
   //
   // s : string -> StringSelector.StartsWith
   // e : string -> StringSelector.EndsWith
   // c : string -> StringSelector.Contains
   //
   // a : sel1 sel2 -> AndSelector
   // o : sel1 sel2 -> OrSelector
   // n : sel1 sel2 -> NotSelector

// --- selector classes ---

   public static class StartsWith implements Selector {
      private String s;
      public StartsWith(String s) { this.s = s; }
      public boolean select(Object o) {
         return (o instanceof String) && ((String) o).startsWith(s);
      }
   }

   public static class EndsWith implements Selector {
      private String s;
      public EndsWith(String s) { this.s = s; }
      public boolean select(Object o) {
         return (o instanceof String) && ((String) o).endsWith(s);
      }
   }

   public static class Contains implements Selector {
      private String s;
      public Contains(String s) { this.s = s; }
      public boolean select(Object o) {
         return (o instanceof String) && (((String) o).indexOf(s) != -1);
      }
   }

   public static class Equals implements Selector { // not useful in patterns but useful for other things
      private String s;
      public Equals(String s) { this.s = s; }
      public boolean select(Object o) {
         return (o instanceof String) && ((String) o).equals(s);
      }
   }

// --- construction ---

   // EmptyStackException and ClassCastException don't have good messages,
   // so add my own.  probably not needed since this isn't end-user stuff,
   // but you never know.

   private static Object pop(Stack stack) throws Exception {
      if (stack.isEmpty()) throw new Exception(Text.get(StringSelector.class,"e1"));
      return stack.pop();
   }

   private static String popString(Stack stack) throws Exception {
      Object o = pop(stack);
      if ( ! (o instanceof String) ) throw new Exception(Text.get(StringSelector.class,"e2"));
      return (String) o;
   }

   private static Selector popSelector(Stack stack) throws Exception {
      Object o = pop(stack);
      if ( ! (o instanceof Selector) ) throw new Exception(Text.get(StringSelector.class,"e3"));
      return (Selector) o;
   }

   public static Selector construct(String pattern) throws Exception {
      Stack stack = new Stack();

      int i = 0;
      int len = pattern.length();
      while (i < len) {
         char c = pattern.charAt(i);
         if (c == '|') {
            int end = pattern.indexOf('|',i+1);
            if (end == -1) throw new Exception(Text.get(StringSelector.class,"e4"));
            stack.push(pattern.substring(i+1,end));
            i = end+1;
         } else {
            Selector sel1, sel2;
            switch (c) {
            case 's':
               stack.push(new StartsWith(popString(stack)));
               break;
            case 'e':
               stack.push(new EndsWith(popString(stack)));
               break;
            case 'c':
               stack.push(new Contains(popString(stack)));
               break;
            case 'a':
               sel2 = popSelector(stack);
               sel1 = popSelector(stack);
               stack.push(new AndSelector(sel1,sel2));
               break;
            case 'o':
               sel2 = popSelector(stack);
               sel1 = popSelector(stack);
               stack.push(new OrSelector(sel1,sel2));
               break;
            case 'n':
               stack.push(new NotSelector(popSelector(stack)));
               break;
            default:
               throw new Exception(Text.get(StringSelector.class,"e5"));
            }
            i++;
         }
      }

      if (stack.size() > 1) throw new Exception(Text.get(StringSelector.class,"e6"));
      // size 0 caught by pop

      return popSelector(stack);
   }

}

