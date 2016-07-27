/*
 * ImageProcess.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Log;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.misc.StreamLogger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.util.logging.Level;

/**
 * A class that manages communication with the ImageLib process.
 * It's not designed for use from multiple threads, you'll have
 * to add that if you want it.  Be sure to call stop at the end!
 */

public class ImageProcess {

// --- constants ---

   public static String TOKEN_GENERATE = "generate";
   public static String TOKEN_GC = "gc";

// --- fields ---

   private Process process;
   private int sequence;
   private OutputStream os;
   private InputStream is;
   private BufferedWriter writer;
   private BufferedReader reader;

   // no constructor, everything starts out null

// --- methods ---

   /**
    * The start function that starts the external process.
    * You don't need to call this, it's run automatically.
    */
   private void start() throws Exception { // from exec, the rest is fine
      if (process != null) return; // just a formality, caller checks too

      try {
         process = Runtime.getRuntime().exec(new String[] { "ImageLib.exe" });
         // don't need to specify a directory, ImageLib sits in the current dir,
         // just like all the DLLs we use.
         // the rest can't fail, go ahead and assign to the process variable.
      } catch (IOException e) {
         throw new Exception(Text.get(this,"e8"),e);
      }
      // because "CreateProcess: ImageLib.exe error=2" isn't good for users

      sequence = 1;

      os = process.getOutputStream();
      is = process.getInputStream();

      writer = new BufferedWriter(new OutputStreamWriter(os));
      reader = new BufferedReader(new InputStreamReader(is));

      new StreamLogger(this,"i1",process.getErrorStream()).start();
      // we don't care about the error stream,
      // but we need to consume it so the process won't jam up
   }

   /**
    * The stop function that shuts down the external process.
    * It's very important to call this at some point,
    * otherwise neither the StreamLogger nor Java will exit.
    * Actually I fixed that with setDaemon, but you'd still
    * have the external process running.
    */
   public void stop() {
      if (process == null) return; // need to check since we can be called from outside

      try { os.close(); } catch (Exception e) {} // make it stop
      // this should really be very reliable -- the only point
      // where ImageLib waits is in ReadLine calls, so closing the stream
      // will be noticed right away.
      // the other option would be to do a destroy, but that seems to me
      // more likely to cause trouble, not letting the process terminate
      // in an orderly way.

      new StreamLogger(this,"i2",is).start();
      // consume any leftover stuff in the process output stream

      process = null;
      os = null;
      is = null;
      writer = null;
      reader = null;
      // if there's data in the buffers, discard it
   }

   /**
    * Call a function in the image library.
    */
   public String[] call(String token, String[] data) throws Exception {
      if (process == null) start();

      Block response = null;
      boolean error = false;
      try {
         Block.write(writer,Block.createRequest(sequence,token,data));

         response = Block.read(reader);
         if (response == null) throw new Exception(Text.get(this,"e1"));

         // basic features of all responses
         if (response.isRequest) throw new Exception(Text.get(this,"e2"));
         if (response.sequence != sequence) throw new Exception(Text.get(this,"e3"));

         if      (response.token.equals(Block.TOKEN_OK   )) error = false;
         else if (response.token.equals(Block.TOKEN_ERROR)) error = true;
         else throw new Exception(Text.get(this,"e4"));

      } catch (Exception e) { // can't recover from these
         stop();
         throw new Exception(Text.get(this,"e5"),e);
      }

      sequence++;
      if ( ! error) return response.data; // ignore message, should be null

   // error, log the stack trace (if any, but we should always get one)

      StringBuffer b = new StringBuffer();
      // no need to include message, ImageLib does that itself
      for (int i=0; i<response.data.length; i++) {
         b.append(response.data[i]);
         b.append("\n");
      }
      Log.log(Level.FINE,this,"i3",new Object[] { b.toString() });

   // report the error

      // not a communication failure so we can keep going in spite of error
      String key = (response.message != null) ? "e6" : "e7";
      throw new Exception(Text.get(this,key,new Object[] { response.message }));
   }

}

