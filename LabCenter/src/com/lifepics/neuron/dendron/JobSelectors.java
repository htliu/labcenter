/*
 * JobSelectors.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.meta.Selector;

/**
 * A utility class containing selector objects that are needed in this package.
 */

public class JobSelectors {

   public static final Selector jobOpen = new Selector() {
      public boolean select(Object o) {
         Job job = (Job) o;
         return (    job.status >= Job.STATUS_JOB_PENDING
                  && job.status <  Job.STATUS_JOB_SENT    );
      }
   };

   public static final Selector jobHold = new Selector() {
      public boolean select(Object o) {
         Job job = (Job) o;
         return (job.hold != Order.HOLD_NONE);
      }
   };

}

