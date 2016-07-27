/*
 * EntityManipulator.java
 */

package com.lifepics.neuron.thread;

/**
 * A manipulator that applies to a standard Entity.
 */

public class EntityManipulator implements Manipulator {

   protected int statusFrom;
   protected int statusActive;
   protected int statusTo;

   public EntityManipulator(int statusFrom,
                            int statusActive,
                            int statusTo) {
      this.statusFrom = statusFrom;
      this.statusActive = statusActive;
      this.statusTo = statusTo;
   }

   public boolean select(Object o) {
      return ((Entity) o).testStatus(statusFrom,statusActive);
      // the status should never be statusActive, but check anyway
   }

   public String getID(Object o) {
      return ((Entity) o).getID();
   }

   public boolean markActive(Object o) {
      ((Entity) o).setStatus(statusActive);
      return true;
   }

   public boolean markComplete(Object o) {
      ((Entity) o).setStatus(statusTo);
      return true;
   }

   public boolean markIncomplete(Object o) {
      ((Entity) o).setStatus(statusFrom);
      return true;
   }

   public boolean markError(Object o, String lastError, boolean told,
                            boolean pauseRetry, long pauseRetryLimit) {
      ((Entity) o).setStatusError(statusFrom,lastError,told,pauseRetry,pauseRetryLimit);
      return true;
   }

   public boolean isRetrying(Object o) {
      return ((Entity) o).isRetrying();
   }

}

