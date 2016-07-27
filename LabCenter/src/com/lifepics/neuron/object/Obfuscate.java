/*
 * Obfuscate.java
 */

package com.lifepics.neuron.object;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Random;

/**
 * A utility class for obfuscating strings for storage,
 * so that it is not obvious to casual inspection what they are.
 */

public class Obfuscate {

// --- hex encoding ---

   // here, the Convert to-from convention is reversed

   private static final String hex = "0123456789ABCDEF";

   public static String toHex(byte[] b) {
      int len = b.length;

      char[] c = new char[len*2];
      int j = 0;

      for (int i=0; i<len; i++) {
         c[j++] = hex.charAt( (b[i] >> 4) & 0xF );
         c[j++] = hex.charAt(  b[i]       & 0xF );
      }

      return new String(c);
   }

   public static byte[] fromHex(String s) throws ValidationException {

      char[] c = s.toCharArray();
      int j = 0;

      if ((c.length & 1) == 1) throw new ValidationException(Text.get(Obfuscate.class,"e1"));
      int len = c.length / 2;

      byte[] b = new byte[len];

      for (int i=0; i<len; i++) {

         int nu = hex.indexOf(c[j++]);
         int nl = hex.indexOf(c[j++]);
         if (nu == -1 || nl == -1) throw new ValidationException(Text.get(Obfuscate.class,"e2"));

         b[i] = (byte) ((nu << 4) + nl);
      }

      return b;
   }

// --- hex encoding of type int ---

   // here, the Convert to-from convention is not reversed

   public static String fromChecksum(int i) {
      int len = 8;

      char[] c = new char[len];

      for (int j=len-1; j>=0; j--) {
         c[j] = hex.charAt( i & 0xF );
         i >>= 4;
      }

      return new String(c);
   }

   public static int toChecksum(String s) throws ValidationException {
      int len = 8;

      char[] c = s.toCharArray();
      if (c.length != len) throw new ValidationException(Text.get(Obfuscate.class,"e5"));

      int i = 0;

      for (int j=0; j<len; j++) {

         int n = hex.indexOf(c[j]);
         if (n == -1) throw new ValidationException(Text.get(Obfuscate.class,"e6"));

         i = (i << 4) + n;
      }

      return i;
   }

// --- random number generators ---

   // the protocol is {createNew|read} {write|next}* ...
   // in other words, createNew and read are constructors.
   // also note that write always writes the initial seed,
   // not the current seed.
   //
   private interface RandomByteStream {
      void createNew(String s);
      void read (DataInputStream  ds, int classifier) throws IOException;
      void write(DataOutputStream ds, int classifier) throws IOException;
      byte next();
      int getSize(); // number of bytes in a data stream
   }

   private static class NativeRandomByteStream implements RandomByteStream {

      private Random r;
      private long seed;

      public void createNew(String s) {

         // we're given a number "A" to work with, currently the hash code.
         // we used to use the current time, but then the obfuscation
         // would change every time you saved the file, which was annoying.
         //
         // we want to produce a number "B" to use as the obfuscation seed.
         // B = A doesn't work, because A, whether hash code or time stamp,
         // has some recognizable structure.  what if we use A as the seed
         // of an intermediate PRNG, and take B as the first value?
         // that works pretty well, but some structure still comes through,
         // especially for the hash code which is only an int, not a long.
         // for example:
         //
         // string   hash code   first value        second value
         // ------   ---------   ----------------   ----------------
         // a        0x61        B8E73C9BED74F8D4   AFB42A66500148EE
         // b        0x62        B8F8D9631ED715F4   2D7E6185E8C3AE73
         // c        0x63        B8F2FA7663B66194   58E5A47AB5D83747
         //
         // the second value is sufficiently randomized, though, so use that.

         r = new Random(s.hashCode());
         r.nextLong();
         seed = r.nextLong();
         r.setSeed(seed);
      }

      public void read(DataInputStream ds, int classifier) throws IOException {
         seed = ds.readLong()-classifier;
         r = new Random(seed);
      }

      public void write(DataOutputStream ds, int classifier) throws IOException {
         ds.writeLong(seed+classifier);
      }

      public byte next() { return (byte) r.nextInt(); }
      public int getSize() { return 8; }
   }

   // I thought I was going to need to replace NativeRandomByteStream
   // with a custom one, but it turned out that wasn't necessary.
   // the key is, you can implement the native one using 32-bit ints.

// --- XOR function ---

   private static void xor(byte[] b, RandomByteStream rbs) {

      // note that we have to start at getSize
      // so we don't mess up the seed value

      for (int i=rbs.getSize(); i<b.length; i++) {
         b[i] = (byte) (b[i] ^ rbs.next());
      }
   }

// --- main functions ---

   // as it says in PasswordField, the classifier is used to vary the obfuscation slightly
   // so that you can't just copy and paste between different kinds of password fields.
   // the detailed effect is, the value stored at the start of the hex string is no longer
   // the seed, it's the seed plus the classifier.  changing the seed by 1 doesn't totally
   // change the byte stream (the second byte has a noticeable pattern), but it changes it
   // more than enough to mess up the length value encoded at the start of the UTF string.

   /**
    * Produce an obfuscated string from a string.
    */
   public static String hide(String s, int classifier) {
      try {

         RandomByteStream rbs = new NativeRandomByteStream();
         rbs.createNew(s);

         ByteArrayOutputStream bs = new ByteArrayOutputStream();
         DataOutputStream ds = new DataOutputStream(bs);

         rbs.write(ds,classifier);
         ds.writeUTF(s);

         byte[] b = bs.toByteArray();
         xor(b,rbs);
         return toHex(b);

      } catch (IOException e) {
         return "";
         // can't happen
      }
   }

   /**
    * Recover a string from an obfuscated string.
    */
   public static String recover(String s, int classifier) throws ValidationException {
      try {

         byte[] b = fromHex(s);
         ByteArrayInputStream bs = new ByteArrayInputStream(b);
         DataInputStream ds = new DataInputStream(bs);

         RandomByteStream rbs = new NativeRandomByteStream();
         rbs.read(ds,classifier);

         xor(b,rbs); // ok to modify buffer while ds is reading

         String result = ds.readUTF();

         if (ds.available() > 0) throw new ValidationException(Text.get(Obfuscate.class,"e3"));
         // the UTF length acts like a checksum ... not every hex string is valid

         return result;

      } catch (IOException e) {
         throw new ValidationException(Text.get(Obfuscate.class,"e4"),e);
      }
   }

// --- alternate ---

   private static byte[] table = { 4, 10, 3, 8, 2, 5, 1, 7, 9, 6 };

   public static byte[] hideAlternate(String s) {

      s += "\t000000\t000000";

      byte[] b = s.getBytes();
      int len = b.length;
      // we know that tab and zero encode as single bytes,
      // so len-14 is the length of the original data

      int sum = 0;
      int xor = 0;

      for (int i=0; i<len-14; i++) { // length of original data
         sum += b[i];
         xor ^= b[i];
      }

      for (int i=0; i<6; i++) {
         b[len-8-i] += sum % 10;
         sum /= 10;
         b[len-1-i] += xor % 10;
         xor /= 10;
      }
      // if the sum hits 10^6, this produces a different result than the original,
      // but then what the original did in that case was stupid.

      for (int i=0; i<len; i++) { // total length of byte array
         b[i] += table[i%table.length];
      }

      return b; // not new String(b), because that mixes up some bytes
      // that are greater than 0x80 ... see Query.encode for details.
   }

}

