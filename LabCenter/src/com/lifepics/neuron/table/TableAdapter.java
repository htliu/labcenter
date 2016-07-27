/*
 * TableAdapter.java
 */

package com.lifepics.neuron.table;

import com.lifepics.neuron.core.ValidationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * An interface that provides the operations needed to allow
 * a given class of object to be stored in a {@link Table}.
 */

public interface TableAdapter {

   /**
    * Get the primary key for an object.
    */
   String getKey(Object o);

   /**
    * Set the primary key for an object.
    */
   boolean setKey(Object o, String key);

   /**
    * Make a deep copy of an object.
    */
   Object copy(Object o);

   /**
    * Load an object from a stream.
    */
   Object load(InputStream inputStream) throws IOException, ValidationException;

   /**
    * Store an object into a stream.
    */
   void store(OutputStream outputStream, Object o) throws IOException;

}

