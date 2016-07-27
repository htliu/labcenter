/*
 * OrderRetryThread.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Log;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.table.Table;
import com.lifepics.neuron.thread.Manipulator;
import com.lifepics.neuron.thread.ThreadStatus;

import java.util.Date;
import java.util.logging.Level;

/**
 * A thread that unpauses orders for retrying.
 */

public class OrderRetryThread extends OrderThread {

// --- fields ---

   private long pauseRetryInterval;

// --- hold manipulator ---

   // this is different from all other manipulators
   // because it operates on the hold code, not the status.
   // in fact, this is the reason manipulators were added.

   private static class HoldManipulator implements Manipulator {

      public boolean select(Object o) {
         return ( ((OrderStub) o).hold == Order.HOLD_RETRY );
      }

      public String getID(Object o) {
         return ((OrderStub) o).getID();
      }

      public boolean markActive    (Object o) { return false; }
      public boolean markIncomplete(Object o) { return false; }
      // like SilentManipulator in this respect

      public boolean markComplete(Object o) {
         OrderStub order = (OrderStub) o;
         order.hold = Order.HOLD_NONE;
         order.lastError = null;
         order.recmodDate = new Date();
         return true;
      }

      public boolean markError(Object o, String lastError, boolean told,
                               boolean pauseRetry, long pauseRetryLimit) {

         // this can't happen ... look at doOrder, there's nothing to cause errors.
         // if it does, though, then throw the order into a true error hold,
         // since further retrying clearly won't accomplish anything.  also,
         // we ignore the pauseRetry fields, because (a) they're never set,
         // since there's no network activity, and (b) there's nothing we could do with them.

         OrderStub order = (OrderStub) o;
         order.hold = Order.HOLD_ERROR;
         order.lastError = lastError;
         order.recmodDate = new Date();
         return true;
      }

      public boolean isRetrying(Object o) {
         return ((OrderStub) o).isRetrying();
      }
   }

// --- construction ---

   public OrderRetryThread(Table table, long scanInterval, ThreadStatus threadStatus,
                           long pauseRetryInterval) {
      super(Text.get(OrderRetryThread.class,"s1"),
            table,
            new HoldManipulator(),
            /* scanFlag = */ true,
            scanInterval,
            threadStatus);

      this.pauseRetryInterval = pauseRetryInterval;
   }

// --- main functions ---

   protected boolean doOrder() throws Exception {

      // we don't actually do anything to the order, here,
      // we just decide whether to let the status update.

      if (    order.failDateLast != null // always true, since we got into retry status
           && scanDate.getTime() >= order.failDateLast.getTime() + pauseRetryInterval ) {

         Log.log(Level.INFO,this,"i1",new Object[] { order.getFullID() });
         return true;
      } else {
         return false;
      }
   }

}

