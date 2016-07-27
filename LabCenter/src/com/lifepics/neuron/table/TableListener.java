/*
 * TableListener.java
 */

package com.lifepics.neuron.table;

/**
 * An interface for receiving events (change notifications) from a {@link Table}.
 */

public interface TableListener {

   /**
    * Report that an object has been inserted.
    *
    * @param o A copy of the object that you can keep if you want.
    */
   void reportInsert(Object o);

   /**
    * Report that an object has been updated.
    * The object's key is not allowed to change,
    * that's how you can find the old object.
    *
    * @param o A copy of the object that you can keep if you want.
    */
   void reportUpdate(Object o);

   /**
    * Report that an object has been deleted.
    */
   void reportDelete(String key);

   /**
    * Report that a computed field has changed.
    */
   void reportRefresh();

}

