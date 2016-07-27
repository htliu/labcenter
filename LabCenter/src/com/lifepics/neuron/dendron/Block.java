/*
 * Block.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

/**
 * A block of data for simple communication (with the ImageLib process) using streams.
 */

public class Block {

// --- fields ---

   // here's what we're looking at in practice:
   //
   // request <sequence> 3 Generate
   // three argument lines
   //
   // response <sequence> 0 ok
   // (or)
   // response <sequence> N error Exception message here.
   // N stack trace lines

   public boolean isRequest;
   public int sequence; // starting with 1
   public String token; // function name in request, ok / error in response
   public String message; // can be null
   public String[] data; // token, message, and data must not contain CR or LF

// --- constants ---

   private static String TYPE_REQUEST = "request";
   private static String TYPE_RESPONSE = "response";

   public static String TOKEN_OK = "ok";
   public static String TOKEN_ERROR = "error";

// --- construction ---

   private Block() {
      // just to stop construction from outside
   }

   public static Block createRequest(int sequence, String token, String[] data) {
      Block b = new Block();
      b.isRequest = true;
      b.sequence = sequence;
      b.token = token;
      b.message = null;
      b.data = data;
      return b;
   }

   // don't bother with response blocks, exception formatting is a pain

// --- I/O ---

   public static void write(BufferedWriter w, Block b) throws IOException {
      String header =   (b.isRequest ? TYPE_REQUEST : TYPE_RESPONSE)
                      + " "
                      + Convert.fromInt(b.sequence)
                      + " "
                      + Convert.fromInt(b.data.length)
                      + " "
                      + b.token;
      if (b.message != null) header = header + " " + b.message;
      w.write(header); w.newLine();
      for (int i=0; i<b.data.length; i++) { w.write(b.data[i]); w.newLine(); }
      w.flush();
   }

   public static Block read(BufferedReader r) throws IOException, ValidationException {
      String header = r.readLine();
      if (header == null) return null; // EOF

      String[] s = header.split(" ",5); // not the usual -1 here
      if (s.length < 4 || s.length > 5) throw new ValidationException(Text.get(Block.class,"e1"));

      Block b = new Block();

      if      (s[0].equals(TYPE_REQUEST )) b.isRequest = true;
      else if (s[0].equals(TYPE_RESPONSE)) b.isRequest = false;
      else throw new ValidationException(Text.get(Block.class,"e2"));

      b.sequence = Convert.toInt(s[1]);
      int count  = Convert.toInt(s[2]);
      b.token = s[3];
      b.message = (s.length > 4) ? s[4] : null;

      b.data = new String[count];
      for (int i=0; i<b.data.length; i++) {
         String line = r.readLine();
         if (line == null) throw new ValidationException(Text.get(Block.class,"e3"));
         b.data[i] = line;
      }

      return b;
   }

}

