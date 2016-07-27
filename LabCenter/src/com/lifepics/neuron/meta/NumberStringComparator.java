/*
 * NumberStringComparator.java
 */

package com.lifepics.neuron.meta;

import java.util.Comparator;

/**
 * A comparator that produces an intuitive ordering
 * for strings that contain variable-length numbers,
 * so that for example "a9b" comes before "a10b".<p>
 *
 * Unlike some other string comparators, this one does not handle nulls
 * or non-string objects.
 */

public class NumberStringComparator {

// --- explanation ---

   // the key to the whole thing is, we think of each string as a sequence
   // of tokens.  a token can be a non-numeric character, or a whole group
   // of digits.  digit tokens should compare to non-numeric tokens as '0',
   // to keep normal string order; the only question then is how the digit
   // tokens compare to one another.  we classify them by length and value,
   // and that's completely specific -- if two tokens have the same length
   // and same value, they must be the exact same strings, and are equal.
   // we also definitely want ascending order on value, which leaves the
   // question of what order we want on length, and how it interacts with value.
   //
   // three plausible rules and what they produce:
   //
   // val asc, len asc   1 01 001 2 02 002 3 03 003 10 010 20 020 30 030
   // val asc, len desc  001 01 1 002 02 2 003 03 3 010 10 020 20 030 30
   // len asc, val asc   1 2 3 01 02 03 10 20 30 001 002 003 010 020 030
   //
   // all three rules work fine if the numbers to be sorted all have the same
   // length, or all have no leading zeros.  so, the question is, what's the
   // most likely cause of something like above?  did the user mis-rename and
   // get a length wrong, or are there two series of images, one numbered
   // with one length, one with another?  I have to go with the "wrong length"
   // theory here, which means we want one of the first two rules.
   //
   // then, should "1" come before "01", because it's shorter, or should it
   // come after, because that's how it works in normal string order?
   // it's debatable, but I'm going to pick the second case, and second rule,
   // because that's what XP does.

   // other notes:

   // we don't allow minus signs ... we could, but it would be a pain, and would
   // do the wrong thing with dashes used as separators before numbers, and with
   // date strings with dash delimiters.

   // we can't actually convert the strings into streams of token objects,
   // because we'd have to store the numbers somehow, and eventually some
   // user would overflow the storage.  I've seen names with 17 digits.
   // but, we can get the same effect with in-place comparison, not too hard.

// --- main function ---

   private static int compareImpl(String s1, String s2, boolean ignoreCase) {

      int i1 = 0;
      int i2 = 0;

      int len1 = s1.length();
      int len2 = s2.length();

      int d;

      // in fact, i1 is always equal to i2, but it works as is, don't mess with it
      while (i1 < len1 && i2 < len2) {

         char c1 = s1.charAt(i1++);
         char c2 = s2.charAt(i2++);

         if (isDigit(c1) && isDigit(c2)) { // both digits, special compare

            int j1 = skipZeros(s1,--i1,len1);
            int j2 = skipZeros(s2,--i2,len2);

            int k1 = skipDigits(s1,j1,len1);
            int k2 = skipDigits(s2,j2,len2);

            // compare lengths first -- that seems to be the opposite of what
            // the explanation above says, but these are the lengths of the
            // nonzero parts, so this is really just an initial value compare.
            d = (k1-j1) - (k2-j2);
            if (d != 0) return d;

            // precompute this before we destroy i1 and i2
            int dLeading = (j1-i1) - (j2-i2);

            // ok, equal-length nonzero parts, compare them
            i1 = j1;
            i2 = j2;
            while (i1 < k1) { // && i2 < k2

               c1 = s1.charAt(i1++);
               c2 = s2.charAt(i2++);

               // we know the chars are digits, just extract and compare
               d = c1 - c2;
               if (d != 0) return d;
            }

            // equal digits, so it's down to leading zeros; negate so shorter is larger
            d = -dLeading;
            if (d != 0) return d;

            // if we get here, i1 and i2 are equal to k1 and k2, correct final state

         } else { // any non-digit, normal compare

            if (ignoreCase) {
               c1 = Character.toLowerCase(c1);
               c2 = Character.toLowerCase(c2);
            }
            // you could also just use String.toLowerCase in the caller,
            // but that allocates memory.
            // we don't need to do this in the digit case, so skip it.

            // char converts to positive int before math, so we get right order here
            d = c1 - c2;
            if (d != 0) return d;

            // in the model where we're thinking of things as tokens, it seems like
            // we ought to have some code where we skip over the rest of the number
            // token, but actually we don't need that, because if we're comparing
            // a number to a non-number, it's guaranteed to terminate and not match.
         }
      }

      if (i1 == len1) {
         if (i2 == len2) return 0; // equal !
         return -1; // i1 is shorter / smaller
      } else {
         return  1; // i2 is shorter / smaller
      }
   }

   private static boolean isDigit(char c) { return (c >= '0' && c <= '9'); }

   private static int skipZeros(String s, int i, int len) {
      while (i < len && s.charAt(i) == '0') i++;
      return i;
   }

   private static int skipDigits(String s, int i, int len) {
      while (i < len && isDigit(s.charAt(i))) i++;
      return i;
   }

// --- compare functions ---

   public static int compareWithCase(String s1, String s2) {
      return compareImpl(s1,s2,/* ignoreCase = */ false);
   }

   public static int compareIgnoreCase(String s1, String s2) {
      int result = compareImpl(s1,s2,/* ignoreCase = */ true);
      if (result != 0) return result;
      return compareImpl(s1,s2,/* ignoreCase = */ false);
   }

// --- comparator objects ---

   private static class WithCase implements Comparator {
      public int compare(Object o1, Object o2) {
         return compareWithCase((String) o1,(String) o2);
      }
      public boolean equals(Object o) {
         return (o instanceof WithCase);
      }
   };

   private static class IgnoreCase implements Comparator {
      public int compare(Object o1, Object o2) {
         return compareIgnoreCase((String) o1,(String) o2);
      }
      public boolean equals(Object o) {
         return (o instanceof IgnoreCase);
      }
   };

   public static Comparator withCase   = new WithCase();
   public static Comparator ignoreCase = new IgnoreCase();

}

