/*
 * Mapping.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.object.Copyable;
import com.lifepics.neuron.struct.PSKU;

/**
 * A common interface for objects that hold SKU mapping information.
 */

public interface Mapping extends Copyable {

   /**
    * Get the SKU code that goes with the mapping.
    */
   PSKU getPSKU();

   /**
    * Set the SKU code that goes with the mapping. (Use only for transfer!)
    */
   void setPSKU(PSKU psku);

   /**
    * Validate the object, same as in XML.Persist.
    */
   void validate() throws ValidationException;

   /**
    * Migrate an old channel object into a new mapping object.
    */
   void migrate(Channel c) throws ValidationException;

}

