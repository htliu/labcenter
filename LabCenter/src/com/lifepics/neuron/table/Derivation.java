/*
 * Derivation.java
 */

package com.lifepics.neuron.table;

import java.util.Collection;

/**
 * An abstraction that explains how to derive a set of new objects from a single old object,
 * and how to manipulate the new objects in certain simple ways.
 * The point is to be able to construct a {@link DerivedTable} from a {@link Table}.
 */

public interface Derivation {

   /**
    * Construct a set of derived objects from an original object.
    */
   Collection derive(Object o);

   /**
    * Get the primary key for a derived object.
    */
   String getKey(Object d);

}

