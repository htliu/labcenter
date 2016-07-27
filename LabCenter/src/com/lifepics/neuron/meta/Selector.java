/*
 * Selector.java
 */

package com.lifepics.neuron.meta;

/**
 * An interface for defining a subset of a set of objects.
 */

public interface Selector {

   /**
    * Decide whether an object belongs in the subset.
    */
   boolean select(Object o);

}

