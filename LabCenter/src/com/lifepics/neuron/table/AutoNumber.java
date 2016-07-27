/*
 * AutoNumber.java
 */

package com.lifepics.neuron.table;

/**
 * An interface for auto-numbering the objects inserted into a {@link Table}.
 */

public interface AutoNumber {

   /**
    * Get the next key, without advancing.
    */
   String getKey();

   /**
    * Advance to the next key.
    */
   void advance();

}

