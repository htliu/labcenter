/*
 * ViewListener.java
 */

package com.lifepics.neuron.table;

/**
 * An interface for receiving events (change notifications) from a {@link View}.
 */

public interface ViewListener {

   /**
    * Report that an object has been inserted.
    *
    * @param j The array index where the object now is.
    * @param o The object.
    */
   void reportInsert(int j, Object o);

   /**
    * Report that an object has been updated.
    *
    * @param i The array index where the object was.
    * @param j The array index where the object now is,
    *          which may be the same as where it was.
    * @param o The object.
    */
   void reportUpdate(int i, int j, Object o);

   /**
    * Report that an object has been deleted.
    *
    * @param i The array index where the object was.
    */
   void reportDelete(int i);

   /**
    * Report that the view has completely changed.
    */
   void reportChange();

}

