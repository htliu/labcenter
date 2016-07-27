/*
 * Compress.java
 */

package com.lifepics.neuron.misc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * A utility class that combines compression and Base64 encoding
 * in the right way for exchanging config files with the server.
 * You just wrap up the input or output stream that connects to it.
 */

public class Compress {

   // old data is uncompressed and has no marker, new data is compressed

   private static final byte MARKER = '@'; // could be anything except XML start character '<'

   public static OutputStream wrapOutput(OutputStream out) throws IOException {
      out.write(MARKER);
      return new DeflaterOutputStream(new Base64EncoderOutputStream(out));
      // you'd think the wrap order would be reversed, but it's not
   }

   public static InputStream wrapInput(InputStream in) throws IOException {
      int b = in.read();
      if (b == -1) {
         return in; // pushback is bad, decompress is pointless
      } else if (b == MARKER) {
         return new InflaterInputStream(new Base64DecoderInputStream(in));
      } else {
         PushbackInputStream pushback = new PushbackInputStream(in);
         pushback.unread(b); // this works, pushback doesn't track
         return pushback;
      }
   }

}

