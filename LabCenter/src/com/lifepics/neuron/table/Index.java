/*
 * Index.java
 */

package com.lifepics.neuron.table;

/**
 * An interface representing an index on a set of objects.
 * The point is to be able to look up single objects with
 * some value that's not the primary key.
 */

public interface Index {

   /**
    * Look up the primary key from an index value.
    */
   String lookup(Object index);

   /**
    * Suspend dynamic updates of the index.
    * This should only be done for short intervals,
    * because all the updates are saved in a list.
    */
   void suspend();

   /**
    * Resume dynamic updates of the index.
    */
   void resume();

}

