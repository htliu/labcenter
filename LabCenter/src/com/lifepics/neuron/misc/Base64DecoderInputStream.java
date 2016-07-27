/*
 * Base64DecoderInputStream.java
 */

package com.lifepics.neuron.misc;

import com.lifepics.neuron.core.Text;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A minimal base-64 decoder.  It accepts final blocks in midstream,
 * but not any line breaks.
 */

public class Base64DecoderInputStream extends FilterInputStream {

   private int n;
   private int accum;
   private byte[] r;

   public Base64DecoderInputStream(InputStream in) {
      super(in);
      n = 0;
      // no need to initialize accumulator
      r = new byte[4];
   }

   // the code assumes that we don't try to recover from IOExceptions,
   // same as with the encoder.

   public int read(byte[] b, int off, int len) throws IOException {

      // adapted from FilterOutputStream.write.
      // I figure this should be the default behavior, but oh well.
      // unfortunately, can't call InputStream.read from here;
      // that has different exception behavior, probably better.

      if ((off | len | (b.length - (off + len)) | (off + len)) < 0) throw new IndexOutOfBoundsException();

      int i;
      for (i=0; i<len; i++) {
         int next = read();
         if (next == -1) break;
         b[off+i] = (byte) next;
      }

      return (i == 0) ? -1 : i;
   }

   public int read() throws IOException {

      if (n == 0) { // need next block?
         if ( ! more() ) return -1; // at EOF
      }

      // now we have data in the accumulator
      n--;
      int result = (accum & 0xFF0000) >> 16;
      accum <<= 8;
      return result;
   }

   /**
    * @return False if there's not more data.
    */
   private boolean more() throws IOException {

   // read four bytes

      for (int i=0; i<4; i++) {
         int b = in.read();
         if (b == -1) {
            if (i != 0) throw new IOException(Text.get(this,"e1")); // incomplete block
            return false; // at EOF
         }
         r[i] = (byte) b;
      }

   // convert equals signs to zero and adjust count
   // equals signs in other places will not be seen
   // and will produce errors.

      n = 3;

      if (r[3] == 61) {
         n = 2;
         r[3] = 65;

         if (r[2] == 61) {
            n = 1;
            r[2] = 65;
         }
      }

   // load bytes into accumulator

      for (int i=0; i<4; i++) {
         accum = (accum << 6) + decode(r[i]);
      }

   // validate there are no extra bits

      if (    (n == 1 && (accum & 0x00FFFF) != 0)
           || (n == 2 && (accum & 0x0000FF) != 0) ) throw new IOException(Text.get(this,"e2"));

      return true;
   }

   private static int decode(byte b) throws IOException {
      b -= 32;
      if (b >= 0 && b < decodeTable.length) {
         int i = decodeTable[b];
         if (i != -1) return i;
      }
      throw new IOException(Text.get(Base64DecoderInputStream.class,"e3"));
   }

   private static int[] decodeTable = {
       -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 62, -1, -1, -1, 63,
       52, 53, 54, 55, 56, 57, 58, 59, 60, 61, -1, -1, -1, -1, -1, -1,
       -1,  0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14,
       15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -1, -1, -1, -1,
       -1, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40,
       41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, -1, -1, -1, -1, -1,
   };

   // these functions would be troublesome to implement, so don't bother.
   // for the first two functions, I'm not really following the
   // InputStream contract, but it should be OK, we won't ever call them.

   public int available() { return 0; }
   public long skip(long n) throws IOException { throw new IOException(); }

   public boolean markSupported() { return false; }
   public void mark(int readlimit) {}
   public void reset() throws IOException { throw new IOException(); }

}

