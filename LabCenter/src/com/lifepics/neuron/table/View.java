/*
 * View.java
 */

package com.lifepics.neuron.table;

import com.lifepics.neuron.meta.Sortable;

import java.util.Comparator;

/**
 * An interface representing a view of a set of objects.
 * A view is a lot like a {@link javax.swing.ListModel},
 * but also supports sorting.
 */

public interface View extends Sortable {

   /**
    * Add a view listener.
    */
   void addListener(ViewListener listener);

   /**
    * Remove a view listener.
    */
   void removeListener(ViewListener listener);

   /**
    * Get the length of the view array.
    */
   int size();

   /**
    * Get the i-th element of the view array.
    */
   Object get(int i);

   /**
    * Suspend dynamic updates of the view.
    * This should only be done for short intervals,
    * because all the updates are saved in a list.
    */
   void suspend();

   /**
    * Resume dynamic updates of the view.
    */
   void resume();

}

