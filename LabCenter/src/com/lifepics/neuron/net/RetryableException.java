/*
 * RetryableException.java
 */

package com.lifepics.neuron.net;

/**
 * An exception class used to report a finitely retryable condition.
 */

public class RetryableException extends Exception {

   public RetryableException(String message) {
      super(message);
   }

   public RetryableException(String message, Throwable cause) {
      super(message,cause);
   }

}

