/*
 * Accessor.java
 */

package com.lifepics.neuron.meta;

/**
 * An interface for accessing a field of an object.
 * The difference from {@link java.lang.reflect.Field Field}
 * is that Accessor can be used to describe computed fields.
 */

public interface Accessor {

   /**
    * Get the class of object that will be returned by {@link #get(Object) get}.
    */
   Class getFieldClass();

   /**
    * Get a field of an object.
    */
   Object get(Object o);

}

