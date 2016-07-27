/*
 * ValidationException.java
 */

package com.lifepics.neuron.core;

/**
 * An exception class used to report validation errors
 * and errors that occur while parsing external input,
 * such as input from the user interface or from a file.
 */

public class ValidationException extends Exception {

   public ValidationException(String message) {
      super(message);
   }

   public ValidationException(String message, Throwable cause) {
      super(message,cause);
   }

}

