/*
 * PakonThread.java
 */

package com.lifepics.neuron.axon;

import com.lifepics.neuron.core.ChainedException;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.thread.StoppableThread;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;

/**
 * A thread that communicates with the Pakon scanner software via a socket.
 */

public class PakonThread extends StoppableThread {

// --- callback interface ---

   public interface Callback {
      void received(String uploadDirectory, String bagID, int totalBytesInDirectory, String email) throws Exception;
   }

// --- fields ---

   // thread fields
   private Socket socket;

   // non-thread fields
   private Callback callback;
   private DataInputStream dataInput;
   private DataOutputStream dataOutput;

// --- construction ---

   public PakonThread(ThreadGroup group, Socket socket, Callback callback) {
      super(group,Text.get(PakonThread.class,"s1"));

      this.socket = socket;

      this.callback = callback;
      // streams start out null
   }

// --- interface for thread fields ---

   /**
    * Construct the thread fields.
    */
   protected void doInit() throws Exception {
      dataInput = new DataInputStream(socket.getInputStream());
      dataOutput = new DataOutputStream(socket.getOutputStream());
   }

   /**
    * Run the thread.
    * This won't be called if doInit throws an exception.
    */
   protected void doRun() throws Exception {
      while (doMessage()) ;
   }

   /**
    * Destroy the thread fields.
    * This will be called even if doInit throws an exception.
    */
   protected void doExit() {
      try {
         if (socket != null) socket.close();
      } catch (IOException e) {
         // ignore
      }
   }

   /**
    * Alter the thread fields to make the thread stop,
    * even if it's blocked in the middle of some operation.
    * This won't be called if doInit throws an exception,
    * or if doExit has already been called.
    */
   protected void doStop() {
      try {
         socket.close(); // known non-null
      } catch (IOException e) {
         // ignore
      }
   }

// --- constants ---

   private static final int CODE_START = 0x55426949; // some random ASCII
   private static final int CODE_END   = 0x70637152;

   private static final int CODE_SETUP_REQUEST   = 1; // setup messages may go away later, or be ignored
   private static final int CODE_SETUP_RESPONSE  = 2;
   private static final int CODE_UPLOAD_REQUEST  = 3;
   private static final int CODE_UPLOAD_RESPONSE = 4;
   private static final int CODE_UPLOAD_STATUS   = 5;
   private static final int CODE_UPLOAD_CANCEL   = 6;

// --- main function ---

   private boolean doMessage() throws IOException {

   // read a message

      String uploadDirectory = null;
      String bagID = null;
      int totalBytesInDirectory = 0;
      String email = null;

      try {
         readConstant(CODE_START);
      } catch (EOFException e) { // other IOExceptions pass through
         return false;
      }
      // this is a bit lax in that we will report a clean exit
      // even if we read 1-3 bytes of extra data, but it is good enough

      int code = dataInput.readInt();
      switch(code) {

      case CODE_SETUP_REQUEST:
         break;

      case CODE_UPLOAD_REQUEST:
         uploadDirectory = readStringAsChars();
         bagID = readStringAsChars();
         totalBytesInDirectory = dataInput.readInt();
         email = readStringAsChars();
         break;

      case CODE_UPLOAD_CANCEL:
         break;

      default:
         throw new IOException(Text.get(this,"e2"));
      }

      readConstant(CODE_END);

   // respond to the message, now that we read the whole thing successfully

      switch (code) {

      case CODE_SETUP_REQUEST:

         // don't do anything, but pretend we did

         dataOutput.writeInt(CODE_START);
         dataOutput.writeInt(CODE_SETUP_RESPONSE);
         writeBooleanAsInt(true); // ok
         dataOutput.writeInt(CODE_END);

         break;

      case CODE_UPLOAD_REQUEST:

         boolean ok = true;
         String error = "";

         try {
            callback.received(uploadDirectory,bagID,totalBytesInDirectory,email);
         } catch (Exception e) {
            ok = false;
            error = ChainedException.format(e);
         }

         if (ok) { // pretend we did everything already

            dataOutput.writeInt(CODE_START);
            dataOutput.writeInt(CODE_UPLOAD_STATUS);
            dataOutput.writeInt(totalBytesInDirectory);
            dataOutput.writeInt(CODE_END);
         }

         dataOutput.writeInt(CODE_START);
         dataOutput.writeInt(CODE_UPLOAD_RESPONSE);
         writeBooleanAsInt(ok);
         writeStringAsChars(error);
         dataOutput.writeInt(CODE_END);

         break;

      case CODE_UPLOAD_CANCEL:

         // don't do anything, we already sent back the response

         break;
      }

      return true;
   }

// --- helpers ---

   private void readConstant(int i) throws IOException {
      if (dataInput.readInt() != i) throw new IOException(Text.get(this,"e1"));
   }

   private boolean readBooleanAsInt() throws IOException {
      return (dataInput.readInt() != 0);
   }

   private void writeBooleanAsInt(boolean b) throws IOException {
      dataOutput.writeInt(b ? 1 : 0);
   }

   private String readStringAsChars() throws IOException {
      int len = dataInput.readInt();
      char[] c = new char[len];
      for (int i=0; i<len; i++) c[i] = dataInput.readChar();
      return new String(c);
   }

   private void writeStringAsChars(String s) throws IOException {
      dataOutput.writeInt(s.length());
      dataOutput.writeChars(s);
   }

}

