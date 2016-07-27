/*
 * ThreadStatus.java
 */

package com.lifepics.neuron.thread;

/**
 * An interface that threads use to report on their status.
 */

public interface ThreadStatus {

   /**
    * Report a network condition that has paused but not stopped a particular task.
    * Network conditions are relatively unpredictable and severe.
    */
   void pausedNetwork(String reason);

   /**
    * Report a wait condition that has paused but not stopped a particular task.
    * Wait conditions are relatively predictable and expected, thus less severe.
    *
    * @return A previous-state token that can be passed to unpaused.
    */
   Object pausedWait(String reason);

   /**
    * Report that a task has unpaused.
    */
   void unpaused();

   /**
    * Report that a task has unpaused.
    *
    * @param o A previous-state token that was sent from pausedWait.
    */
   void unpaused(Object token);

   /**
    * Report an error that stopped a particular task but not the whole thread.
    */
   void error(Throwable t);

   /**
    * Report an error that's going to cause the thread to terminate.
    */
   void fatal(Throwable t);

   /**
    * Report that a particular task completed successfully,
    * so that any previous related errors can be forgotten.
    */
   void success(String category);

}

