/*
 * Base64EncoderOutputStream.java
 */

package com.lifepics.neuron.misc;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A minimal base-64 encoder.  It emits the current block whenever flush is called,
 * whether or not it's full, so in theory it can write a final block in the middle
 * of the stream as well as at the end.  It does not include any line breaks.
 */

public class Base64EncoderOutputStream extends FilterOutputStream {

   private int n;
   private int accum;
   private byte[] r;

   public Base64EncoderOutputStream(OutputStream out) {
      super(out);
      n = 0;
      // no need to initialize accumulator
      r = new byte[4];
   }

   // the code assumes that we don't try to recover from IOExceptions.
   // for example ... if we write one byte and flush, and it fails,
   // then a second flush would think it had three bytes.  even worse,
   // a write afterward would push the byte out of the accumulator.

   // parent class implementation of write(byte[],int,int) is fine

   public void write(int b) throws IOException {
      pack(b & 0xFF); // high bits may not be zero, we must ignore
      if (n == 3) emit(); // takes n back to zero
   }

   public void flush() throws IOException {
      if (n != 0) emit();
      out.flush(); // same as FilterOutputStream
   }

   private void pack(int b) {
      // pack into accumulator in eights
      n++;
      accum = (accum << 8) + b;
   }

   private void emit() throws IOException {

      // fill the accumulator but save the count
      int count = n;
      while (n < 3) pack(0);

      // unpack from accumulator in sixes
      for (int i=3; i>=0; i--) {
         r[i] = encodeTable[accum & 0x3F];
         accum >>= 6;
      }

      // make adjustments for final block
      if (count < 3) r[3] = 61; // equals sign
      if (count < 2) r[2] = 61;

      out.write(r);

      n = 0;
      // no need to reset accumulator
   }

   private static byte[] encodeTable = {
       65,  66,  67,  68,  69,  70,  71,  72,  73,  74,  75,  76,  77,  78,  79,  80,
       81,  82,  83,  84,  85,  86,  87,  88,  89,  90,  97,  98,  99, 100, 101, 102,
      103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118,
      119, 120, 121, 122,  48,  49,  50,  51,  52,  53,  54,  55,  56,  57,  43,  47
   };
   // ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/

}

