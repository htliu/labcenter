/*
 * Storage.java
 */

package com.lifepics.neuron.table;

import com.lifepics.neuron.core.ValidationException;

import java.io.IOException;

/**
 * An interface for persistent storage, for use by a {@link Table}.
 */

public interface Storage {

   /**
    * Get a list of all the keys of objects that exist in the storage.
    */
   String[] list();

   /**
    * Load the object from the space identified by the given key.
    */
   Object load(String key, TableAdapter adapter) throws IOException, ValidationException;

   /**
    * Store an object into the space identified by the given key.<p>
    *
    * It should be true that adapter.getKey(o).equals(key),
    * but the persistent storage shouldn't need to know that.
    */
   void store(String key, Object o, TableAdapter adapter) throws IOException;

   /**
    * Delete the object from the space identified by the given key.
    */
   void delete(String key) throws IOException;

}

