/*
 * TableException.java
 */

package com.lifepics.neuron.table;

/**
 * An exception class used to report failure in a {@link Table}.
 */

public class TableException extends Exception {

   public TableException(String message) {
      super(message);
   }

   public TableException(String message, Throwable cause) {
      super(message,cause);
   }

}

