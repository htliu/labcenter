/*
 * LocalException.java
 */

package com.lifepics.neuron.dendron;

/**
 * An exception wrapper that carries a local status code override.
 */

public class LocalException extends Exception {

   private int code;

   public LocalException(int code) {
      super();
      this.code = code;
   }

   public LocalException(Throwable cause, int code) {
      super(null,cause);
      // when you construct an exception with a cause but no message,
      // the Throwable constructor calls toString on the cause and
      // uses that as the message.  ugh!!  explicit null avoids that.

      this.code = code;
   }

   public int getCode() { return code; }

}

