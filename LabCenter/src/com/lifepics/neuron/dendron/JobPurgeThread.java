/*
 * JobPurgeThread.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.misc.Purge;
import com.lifepics.neuron.misc.PurgeConfig;
import com.lifepics.neuron.table.Table;
import com.lifepics.neuron.thread.SilentManipulator;
import com.lifepics.neuron.thread.ThreadStatus;

/**
 * A thread that purges jobs (as appropriate).
 */

public class JobPurgeThread extends JobThread {

// --- fields ---

   private JobManager jobManager;

// --- multi manipulator ---

   // a manipulator that accepts multiple possible statusFrom values

   private static class MultiManipulator extends SilentManipulator {

      public MultiManipulator(int statusFrom,
                              int statusTo) {
         super(statusFrom,statusTo);
      }

      public boolean select(Object o) {
         return (JobUtil.getPurgeMode((Job) o) != PurgeConfig.MODE_NONE);
      }

      public boolean markError(Object o, String lastError, boolean told,
                               boolean pauseRetry, long pauseRetryLimit) {
         Job job = (Job) o;
         job.setStatusError(job.status,lastError,told,pauseRetry,pauseRetryLimit);
         return true;
         // pass in job.status instead of statusFrom, since there are
         // two possible values of it.  this trick only works because
         // (a) the thread is silent
         // (b) the status field isn't used to store error-ness
         //
         // now there are many possible values, but the same point holds
         //
         // see comment in OrderPurgeThread about stale purge feature.
      }
   }

// --- construction ---

   public JobPurgeThread(Table jobTable, long scanInterval, ThreadStatus threadStatus,
                         JobManager jobManager, boolean autoPurgeStale, boolean autoPurgeStaleLocal) {
      super(Text.get(JobPurgeThread.class,"s1"),
            jobTable,
            new MultiManipulator(
               Job.STATUS_JOB_COMPLETED,
               Job.STATUS_JOB_COMPLETED), // statusTo, not used (since job is deleted)
            /* scanFlag = */ true,
            scanInterval,
            threadStatus);

      this.jobManager = jobManager;

      // see OrderPurgeThread for why autoPurgeStale and autoPurgeStaleLocal are here
   }

// --- main functions ---

   protected boolean doJob() throws Exception {

      Integer i = jobManager.autoPurge(scanDate,job,lock);
      if (i == null) return false; // not ready yet

      int result = i.intValue();

      if (result == Purge.RESULT_FAILED) { // error has been logged
         // we don't want to keep retrying,
         // but since it was a table operation that failed,
         // trying to attach the error probably won't work.
         return false;
      } else { // object has been deleted, incomplete purge has been logged
         lock = null;
         return true;
      }
   }

}

