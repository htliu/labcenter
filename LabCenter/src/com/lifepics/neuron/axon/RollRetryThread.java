/*
 * RollRetryThread.java
 */

package com.lifepics.neuron.axon;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Log;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.table.Table;
import com.lifepics.neuron.thread.Manipulator;
import com.lifepics.neuron.thread.ThreadStatus;

import java.util.Date;
import java.util.logging.Level;

/**
 * A thread that unpauses rolls for retrying.
 */

public class RollRetryThread extends RollThread {

// --- fields ---

   private long pauseRetryInterval;

// --- hold manipulator ---

   // this is different from all other manipulators
   // because it operates on the hold code, not the status.
   // in fact, this is the reason manipulators were added.

   private static class HoldManipulator implements Manipulator {

      public boolean select(Object o) {
         return ( ((Roll) o).hold == Roll.HOLD_ROLL_RETRY );
      }

      public String getID(Object o) {
         return ((Roll) o).getID();
      }

      public boolean markActive    (Object o) { return false; }
      public boolean markIncomplete(Object o) { return false; }
      // like SilentManipulator in this respect

      public boolean markComplete(Object o) {
         Roll roll = (Roll) o;
         roll.hold = Roll.HOLD_ROLL_NONE;
         RollManager.adjustHold(roll);
         roll.recmodDate = new Date();
         return true;
      }
      // the only way the status could adjust to email hold
      // is if we tried to start uploading, went into retry,
      // then the user changed the email address.

      public boolean markError(Object o, String lastError, boolean told,
                               boolean pauseRetry, long pauseRetryLimit) {

         // this can't happen ... look at doRoll, there's nothing to cause errors.
         // if it does, though, then throw the order into a true error hold,
         // since further retrying clearly won't accomplish anything.  also,
         // we ignore the pauseRetry fields, because (a) they're never set,
         // since there's no network activity, and (b) there's nothing we could do with them.

         Roll roll = (Roll) o;
         roll.hold = Roll.HOLD_ROLL_ERROR;
         roll.lastError = lastError;
         roll.recmodDate = new Date();
         return true;
      }

      public boolean isRetrying(Object o) {
         return ((Roll) o).isRetrying();
      }
   }

// --- construction ---

   public RollRetryThread(Table table, long scanInterval, ThreadStatus threadStatus,
                          long pauseRetryInterval) {
      super(Text.get(RollRetryThread.class,"s1"),
            table,
            new HoldManipulator(),
            /* scanFlag = */ true,
            scanInterval,
            threadStatus);

      this.pauseRetryInterval = pauseRetryInterval;
   }

// --- main functions ---

   protected boolean doRoll() throws Exception {

      // we don't actually do anything to the roll, here,
      // we just decide whether to let the status update.

      if (    roll.failDateLast != null // always true, since we got into retry status
           && scanDate.getTime() >= roll.failDateLast.getTime() + pauseRetryInterval ) {

         if (roll.source == Roll.SOURCE_LOCAL) {
            // we don't need to hear about it.  in particular, we don't need to hear about
            // the very common TPLI retries.
         } else {
            Log.log(Level.INFO,this,"i1",new Object[] { Convert.fromInt(roll.rollID) });
         }

         return true;
      } else {
         return false;
      }
   }

}

