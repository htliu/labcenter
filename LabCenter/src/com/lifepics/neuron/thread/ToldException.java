/*
 * ToldException.java
 */

package com.lifepics.neuron.thread;

/**
 * An exception wrapper that indicates that the user has already been told,
 * (through {@link com.lifepics.neuron.gui.User#tell(int,String) User.tell}).
 */

public class ToldException extends Exception {

   public ToldException() {
      super();
   }

   public ToldException(Throwable cause) {
      super(null,cause);
      // see comment in LocalException
   }

}

