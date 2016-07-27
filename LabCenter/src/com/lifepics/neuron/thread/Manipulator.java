/*
 * Manipulator.java
 */

package com.lifepics.neuron.thread;

import com.lifepics.neuron.meta.Selector;

/**
 * An interface that's even more abstract than Entity,
 * to handle the pause-retry case
 * when the hold code is marked instead of the status.
 */

public interface Manipulator extends Selector {

   /**
    * Get an identifier suitable for use in error messages.
    */
   String getID(Object o);

   /**
    * Mark the object to show processing initiated.
    */
   boolean markActive(Object o);

   /**
    * Mark the object to show processing completed.
    */
   boolean markComplete(Object o);

   /**
    * Mark the object to show processing didn't complete (not because of an error).
    */
   boolean markIncomplete(Object o);

   /**
    * Mark the object to show processing didn't complete (because of an error).
    */
   boolean markError(Object o, String lastError, boolean told,
                     boolean pauseRetry, long pauseRetryLimit);

   /**
    * Test whether the object is in a retrying state (second retry or later).
    */
   boolean isRetrying(Object o);

}

