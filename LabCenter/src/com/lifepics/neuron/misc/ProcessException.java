/*
 * ProcessException.java
 */

package com.lifepics.neuron.misc;

/**
 * An exception class used to report failure in a high-level process.
 */

public class ProcessException extends Exception {

   public ProcessException(String message) {
      super(message);
   }

   public ProcessException(String message, Throwable cause) {
      super(message,cause);
   }

   /**
    * No-message constructor for MultipleInstanceException.
    */
   protected ProcessException() {
   }

}

