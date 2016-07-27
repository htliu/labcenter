/*
 * ThreadStopException.java
 */

package com.lifepics.neuron.thread;

/**
 * An exception that terminates the main loop of a thread.
 */

public class ThreadStopException extends Exception {

   public ThreadStopException(String message) {
      super(message);
   }

   public ThreadStopException(String message, Throwable cause) {
      super(message,cause);
   }

// --- hint system ---

   // the point is to be able to move big blocks of help text
   // up into the UI where the user can see them, without
   // breaking the rule that error messages have no linefeeds.

   private String hint;

   public ThreadStopException setHint(String hint) {
      this.hint = hint;
      return this; // convenience
   }

   public String getHint() {
      return hint;
   }

}

