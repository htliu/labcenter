/*
 * OrderPurgeThread.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.misc.Purge;
import com.lifepics.neuron.misc.PurgeConfig;
import com.lifepics.neuron.table.Table;
import com.lifepics.neuron.thread.SilentManipulator;
import com.lifepics.neuron.thread.ThreadStatus;

/**
 * A thread that purges orders (as appropriate).
 */

public class OrderPurgeThread extends OrderThread {

// --- fields ---

   private OrderManager orderManager;

// --- multi manipulator ---

   // a manipulator that accepts multiple possible statusFrom values

   private static class MultiManipulator extends SilentManipulator {

      public MultiManipulator(int statusFrom,
                              int statusTo) {
         super(statusFrom,statusTo);
      }

      public boolean select(Object o) {
         return (OrderUtil.getPurgeMode((OrderStub) o) != PurgeConfig.MODE_NONE);
      }

      public boolean markError(Object o, String lastError, boolean told,
                               boolean pauseRetry, long pauseRetryLimit) {
         OrderStub order = (OrderStub) o;
         order.setStatusError(order.status,lastError,told,pauseRetry,pauseRetryLimit);
         return true;
         // pass in order.status instead of statusFrom, since there are
         // three possible values of it.  this trick only works because
         // (a) the thread is silent
         // (b) the status field isn't used to store error-ness
         //
         // now there are many possible values, but the same point holds
         //
         // with the stale purge feature, I was worried that maybe we'd
         // keep trying to purge the same order over and over.  but, if
         // you look at the code, there's actually no way for markError
         // to get called.  see also the note about RESULT_FAILED below.
      }
   }

// --- construction ---

   public OrderPurgeThread(Table table, long scanInterval, ThreadStatus threadStatus,
                           OrderManager orderManager, boolean autoPurgeStale, boolean autoPurgeStaleLocal) {
      super(Text.get(OrderPurgeThread.class,"s1"),
            table,
            new MultiManipulator(
               Order.STATUS_ORDER_COMPLETED,
               Order.STATUS_ORDER_COMPLETED), // statusTo, not used (since order is deleted)
            /* scanFlag = */ true,
            scanInterval,
            threadStatus);

      this.orderManager = orderManager;

      // why autoPurgeStale and autoPurgeStaleLocal are here: mostly we use the static PurgeConfig that's
      // passed into OrderUtil.  that works fine, especially since the static config is pushed before the
      // subsystems are reinited.  however, since the actual set of orders under consideration depends on
      // the two stale flags, we need to force a thread restart when they change; that's why they're here.
   }

// --- main functions ---

   protected boolean doOrder() throws Exception {

      Integer i = orderManager.autoPurge(scanDate,order,lock);
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

