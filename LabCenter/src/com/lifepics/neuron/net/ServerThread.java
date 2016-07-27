/*
 * ServerThread.java
 */

package com.lifepics.neuron.net;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.thread.StoppableThread;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * A thread that waits for and accepts incoming socket connections.
 */

public class ServerThread extends StoppableThread {

// --- callback interface ---

   public interface Callback {
      void accepted(Socket socket);
   }

// --- fields ---

   private int port;
   private Callback callback;

   // thread fields
   private ServerSocket socket;

// --- construction ---

   public ServerThread(int port, Callback callback) {
      super(Text.get(ServerThread.class,"s1"));

      this.port = port;
      this.callback = callback;

      // socket starts out null
   }

   public boolean bind() throws IOException {
      try {
         doInit();
         return true;
      } catch (BindException e) {
         return false;
      }
      // other exceptions pass through
   }

// --- accessors ---

   public int getPort() { return port; }

// --- interface for thread fields ---

   /**
    * Construct the thread fields.
    */
   protected void doInit() throws IOException {
      if (socket == null) socket = new ServerSocket(port);
   }

   /**
    * Run the thread.
    * This won't be called if doInit throws an exception.
    */
   protected void doRun() throws Exception {
      while (true) {
         callback.accepted(socket.accept());
      }
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

}

