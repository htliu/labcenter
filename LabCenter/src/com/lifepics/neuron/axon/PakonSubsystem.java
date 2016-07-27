/*
 * PakonSubsystem.java
 */

package com.lifepics.neuron.axon;

import com.lifepics.neuron.net.ServerThread;
import com.lifepics.neuron.thread.StoppableGroup;

import java.net.Socket;

/**
 * The Pakon subsystem of the Axon application.
 */

public class PakonSubsystem {

// --- fields ---

   private PakonThread.Callback pakonCallback;
   private StoppableGroup handlers;
   private ServerThread.Callback serverCallback;

   private ServerThread server;
   //
   // the current values of enabled and port are encoded here.
   // if the server isn't set to null, the subsystem is enabled,
   // with port equals to server.getPort()

// --- construction ---

   public PakonSubsystem(boolean enabled, int port, PakonThread.Callback callback) {

      pakonCallback = callback;
      handlers = new StoppableGroup();
      serverCallback = new ServerThread.Callback() {
         public void accepted(Socket socket) {
            new PakonThread(handlers,socket,pakonCallback).start();
         }
      };

      // server may stay null, depending on below

      init(enabled,port);
   }

// --- helpers ---

   // these are only called when the subsystem is in an appropriate state

   private void start(int port) {
      server = new ServerThread(port,serverCallback);
      server.start();
   }

   private void stop() {
      server.stopNice();
      server = null;

      handlers.stopNice();
   }

// --- methods ---

   private void init(boolean enabled, int port) {
      if (enabled) start(port);
   }

   public void reinit(boolean enabled, int port) {
      boolean wasEnabled = (server != null);

      if (enabled && wasEnabled && port == server.getPort()) {
         // running, no change
      } else if ( ! enabled && ! wasEnabled ) {
         // not running, no change
      } else {
         // change
         if (wasEnabled) stop();
         if (enabled) start(port);
      }
   }

   public void exit() {
      boolean wasEnabled = (server != null);

      if (wasEnabled) stop();
   }

}

