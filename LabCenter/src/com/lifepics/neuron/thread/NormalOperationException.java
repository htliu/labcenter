/*
 * NormalOperationException.java
 */

package com.lifepics.neuron.thread;

/**
 * An exception wrapper that indicates that the exception
 * is something that happens in normal operation, that is,
 * not really exceptional, and so shouldn't get logged.
 */

public class NormalOperationException extends Exception {

   public NormalOperationException() {
      super();
   }

   public NormalOperationException(Throwable cause) {
      super(null,cause);
      // see comment in LocalException
   }

}

