/*
 * RollPurgeThread.java
 */

package com.lifepics.neuron.axon;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.misc.Purge;
import com.lifepics.neuron.net.DiagnoseConfig;
import com.lifepics.neuron.net.DefaultHandler;
import com.lifepics.neuron.net.DescribeHandler;
import com.lifepics.neuron.net.DiagnoseHandler;
import com.lifepics.neuron.net.Handler;
import com.lifepics.neuron.net.PauseAdapter;
import com.lifepics.neuron.table.Table;
import com.lifepics.neuron.table.TableException;
import com.lifepics.neuron.thread.SilentManipulator;
import com.lifepics.neuron.thread.ThreadStatus;

/**
 * A thread that purges rolls (as appropriate).
 */

public class RollPurgeThread extends RollThread {

// --- fields ---

   private RollManager rollManager;

   private Handler handler;
   private PauseAdapter pauseAdapter;

// --- multi manipulator ---

   // a manipulator that accepts multiple possible statusFrom values

   private static class MultiManipulator extends SilentManipulator {

      public MultiManipulator(int statusFrom,
                              int statusTo) {
         super(statusFrom,statusTo);
      }

      public boolean select(Object o) {
         // could misuse Entity.testStatus, but let's not
         Roll roll = (Roll) o;
         return (roll.isPurgeableStatus() && roll.hold == Roll.HOLD_ROLL_NONE);
      }

      public boolean markError(Object o, String lastError, boolean told,
                               boolean pauseRetry, long pauseRetryLimit) {
         Roll roll = (Roll) o;
         roll.setStatusError(roll.status,lastError,told,pauseRetry,pauseRetryLimit);
         return true;
         // pass in roll.status instead of statusFrom, since there are
         // two possible values of it.   this trick only works because
         // (a) the thread is silent
         // (b) the status field isn't used to store error-ness
      }
   }

// --- construction ---

   public RollPurgeThread(Table table, long scanInterval, ThreadStatus threadStatus,
                          RollManager rollManager, DiagnoseConfig diagnoseConfig) {
      super(Text.get(RollPurgeThread.class,"s1"),
            table,
            new MultiManipulator(
               Roll.STATUS_ROLL_COMPLETED,
               Roll.STATUS_ROLL_COMPLETED), // statusTo, not used (since roll is deleted)
            /* scanFlag = */ true,
            scanInterval,
            threadStatus);

      this.rollManager = rollManager;

      handler = new DescribeHandler(new DiagnoseHandler(new DefaultHandler(),diagnoseConfig));
      // not sure why I left the describe handler out of other non-transfer network threads
      pauseAdapter = new PauseAdapter(threadStatus);
   }

// --- main functions ---

   protected boolean doRoll() throws Exception {
      try {

         Integer i = rollManager.autoPurge(scanDate,roll,lock,handler,pauseAdapter);
         if (i == null) return false; // not ready yet

         int result = i.intValue();

         if (result == Purge.RESULT_STOPPING) { // just say done = false, EntityThread will check for stopping
            return false;
         } else { // done = true, object has been deleted, any incomplete purge has been logged
            lock = null;
            return true;
         }

      // note, all exceptions have already been logged
      } catch (TableException e) {

         // we don't want to keep retrying,
         // but since it was a table operation that failed,
         // trying to attach the error probably won't work.
         //
         return false;

      }
      // other exceptions will be handled by EntityThread.
      // some notable cases produced by TPLI are
      // code 4 -> PauseRetryException and wrong password -> ThreadStopException
   }

}

