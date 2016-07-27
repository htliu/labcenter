/*
 * Entity.java
 */

package com.lifepics.neuron.thread;

/**
 * An interface for abstracting over rolls, orders, and jobs.
 */

public interface Entity {

   /**
    * Get an identifier suitable for use in error messages.
    */
   String getID();

   /**
    * Test whether the entity is in either status, and not on hold.
    */
   boolean testStatus(int statusFrom, int statusActive);

   /**
    * Put the entity into the given status.
    * This should also update the recmod date.
    */
   void setStatus(int status);

   /**
    * Put the entity into error status.
    * If the entity has an error status, it should ignore statusFrom,
    * but if it has a hold field, it should set status to statusFrom.
    * This should also update the recmod date.
    */
   void setStatusError(int statusFrom, String lastError, boolean told,
                       boolean pauseRetry, long pauseRetryLimit);

   /**
    * Test whether the entity is in a retrying state (second retry or later).
    */
   boolean isRetrying();

}

