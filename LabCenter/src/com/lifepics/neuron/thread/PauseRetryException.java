/*
 * PauseRetryException.java
 */

package com.lifepics.neuron.thread;

/**
 * An exception class used to report that retrying has failed for now,
 * but that the entity could be paused and tried again later.<p>
 *
 * As far as pause-ness is concerned, this is about entity pausing,
 * not thread pausing, and has nothing to do with PauseCallback or PauseAdapter.
 * As far as retry-ness is concerned, this is an upward message
 * from the diagnostic system to the caller, requesting an entity pause,
 * not to be confused with the upward message of RetryableException,
 * which is a message <i>to</i> the diagnostic system by a lower level.
 */

public class PauseRetryException extends Exception {

   public long pauseRetryLimit;

   public PauseRetryException(String message, long pauseRetryLimit) {
      super(message);
      this.pauseRetryLimit = pauseRetryLimit;
   }

   public PauseRetryException(String message, Throwable cause, long pauseRetryLimit) {
      super(message,cause);
      this.pauseRetryLimit = pauseRetryLimit;
   }

}

