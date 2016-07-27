/*
 * HTTPStopAction.java
 */

package com.lifepics.neuron.net;

import com.lifepics.neuron.thread.StoppableThread;

import HTTPClient.HTTPConnection;

import java.io.IOException;
import java.net.SocketException;

/**
 * A small helper class for interrupting HTTP transfer.
 */

public class HTTPStopAction implements Runnable {

   private HTTPConnection http;
   private boolean executed;

   public HTTPStopAction(HTTPConnection http) {
      this.http = http;
      executed = false;
   }

   public void run() {
      http.stop();
      executed = true;
   }

   /**
    * @return The current stopping value, as a convenience.
    *         If it's true, the stop action won't be run,
    *         so you should avoid doing whatever's going to block.
    */
   public boolean set() {
      return StoppableThread.setStopActionIfStoppable(this);
   }

   public void clear() {
      StoppableThread.clearStopActionIfStoppable();
   }

   public boolean caused(Exception e) {
      return (executed && isStopException(e));
   }

   public static boolean isStopException(Exception e) {
      return (    (e instanceof IOException     && e.getMessage().equals("Request aborted by user"))
               || (e instanceof SocketException && e.getMessage().equals("Socket closed"          )) );
      // you get the first kind if you stop while reading, the second kind if while writing
   }

}

