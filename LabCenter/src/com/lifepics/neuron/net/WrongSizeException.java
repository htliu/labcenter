/*
 * WrongSizeException.java
 */

package com.lifepics.neuron.net;

/**
 * An exception class used to report a retryable wrong-size condition.
 */

public class WrongSizeException extends Exception {

   public WrongSizeException(String message) {
      super(message);
   }

}

