/*
 * CategorizedException.java
 */

package com.lifepics.neuron.thread;

/**
 * An exception that is categorized (for error list maintenance).
 */

public class CategorizedException extends Exception {

   /**
    * The category, meaning, the kind of operation that failed.
    */
   private String category;

   public CategorizedException(String category, String message) {
      super(message);
      this.category = category;
   }

   public CategorizedException(String category, String message, Throwable cause) {
      super(message,cause);
      this.category = category;
   }

   public static boolean match(String category, Object o) {
      return (    o != null
               && o instanceof CategorizedException
               && ((CategorizedException) o).category.equals(category) );
   }

}

