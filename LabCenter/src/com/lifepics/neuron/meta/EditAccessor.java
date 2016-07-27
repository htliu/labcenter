/*
 * EditAccessor.java
 */

package com.lifepics.neuron.meta;

/**
 * An interface for accessing an editable field of an object.
 */

public interface EditAccessor extends Accessor {

   /**
    * Put a value into a field.
    */
   void put(Object o, Object value);

}

