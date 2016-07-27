/*
 * AutoCompleteThread.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Log;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.misc.StoreHours;
import com.lifepics.neuron.net.DefaultHandler;
import com.lifepics.neuron.net.DiagnoseConfig;
import com.lifepics.neuron.net.DiagnoseHandler;
import com.lifepics.neuron.net.Handler;
import com.lifepics.neuron.net.PauseAdapter;
import com.lifepics.neuron.net.PauseCallback;
import com.lifepics.neuron.object.CopyUtil;
import com.lifepics.neuron.table.Table;
import com.lifepics.neuron.thread.SilentManipulator;
import com.lifepics.neuron.thread.ThreadStatus;

import java.util.LinkedList;
import java.util.logging.Level;

/**
 * A thread that autocompletes orders as soon as they're printed.
 */

public class AutoCompleteThread extends OrderThread {

// --- fields ---

   private OrderManager orderManager;
   private AutoCompleteConfig config;
   private LinkedList storeHoursAdjusted;

   private Handler handler;
   private PauseCallback callback;

// --- manipulator ---

   // this sometimes reduces the set of orders under consideration
   // by leaving out shipped orders when tracking is enabled

   private static class ReducedManipulator extends SilentManipulator {

      private boolean enableTracking;

      public ReducedManipulator(int statusFrom, int statusTo, boolean enableTracking) {
         super(statusFrom,statusTo);
         this.enableTracking = enableTracking;
      }

      public boolean select(Object o) {
         if ( ! super.select(o) ) return false;

         if (enableTracking && o instanceof Order && ((Order) o).isAnyShipToHome()) return false;
         // in other places (e.g. SpawnThread) I assume an object is an Order
         // instead of an OrderStub based on status, but that seems bad to me now.

         return true;
      }
   }

// --- construction ---

   public AutoCompleteThread(Table table, long scanInterval, ThreadStatus threadStatus,
                             OrderManager orderManager, AutoCompleteConfig config, LinkedList storeHours, DiagnoseConfig diagnoseConfig, boolean enableTracking) {
      super(Text.get(AutoCompleteThread.class,"s1"),
            table,
            new ReducedManipulator(
               Order.STATUS_ORDER_PRINTED,
               Order.STATUS_ORDER_COMPLETED,
               enableTracking),
            /* scanFlag = */ true,
            scanInterval,
            threadStatus);

      this.orderManager = orderManager;
      this.config = config;

      if (config.restrictToStoreHours) {
         // tempting to adjust the store hours once in the subsystem instead of every time
         // the thread starts, but if we did that it would mess up the subsys equals check
         storeHoursAdjusted = config.adjust(storeHours);
      }
      // else we don't need the hours, don't bother

      handler = new DiagnoseHandler(new DefaultHandler(),diagnoseConfig);
      callback = new PauseAdapter(threadStatus);
      //
      // in the download and upload domains, the subsystems allocate the handler,
      // but here I thought I'd just do it in the thread, then no reinit needed.
   }

// --- regulation ---

   // see DownloadThread for the whole theory of regulation

   // if we ever have something else that runs on store hours, it'd be nice
   // to factor out these functions in the same way that BandwidthUtil does.
   // however, it's a pain here ... we can't put it in StoreHours because
   // that's in misc, which is below where we should be calling thread code.

   // I thought I'd have to make something like RegulatedTransaction here,
   // but actually there's no need.  since there's at most one transaction
   // per entity (local orders have zero transactions), the duplicate
   // regulation checkpoint at the end of doEntity is sufficient to cover
   // everything!  the regulation (scheduling) during network retries
   // comes from DiagnoseHandler not RegulatedTransaction, so we still get that.
   // if we do need it some day, probably a handler instead of transaction
   // would be the way to go, it would work here.

   private void waitScheduled(long wait) { // cf. end of BandwidthUtil.regulate

      Object token = threadStatus.pausedWait(Text.get(this,"s2"));
      try {
         sleepNice(wait);
      } catch (InterruptedException e) {
         // won't happen
      }
      threadStatus.unpaused(token);
   }

   public boolean regulateIsStopping() { // cf. BandwidthUtil.regulateIsStopping

      if (isStopping()) return true;

      if ( ! config.restrictToStoreHours ) return false;
      //
      long wait = StoreHours.getWaitInterval(storeHoursAdjusted);
      if (wait == 0) return false;
      // by validation, the adjusted hours aren't empty

      waitScheduled(wait);

      if (isStopping()) return true;
      return false;
   }

// --- main functions ---

   protected boolean doOrder() throws Exception {

      // let zero mean no delay even if the order somehow gets a future timestamp
      if (    config.autoCompleteDelay != 0
           && scanDate.getTime() < order.recmodDate.getTime() + config.autoCompleteDelay ) {
         return false;
         // wait until the order has aged sufficiently.  recmodDate isn't perfect,
         // by the way.  if an order errors out on autocomplete, both the error
         // and the subsequent unpause will change the recmodDate, so the order won't
         // retry until after another delay.
      }

      Exception e = orderManager.completeWithLock(order,lock,handler,callback);
      if (e != null) {
         if (e == OrderManager.stopping) return false;
         else {
            Log.log(order,Level.WARNING,this,"e1",new Object[] { order.getFullID() },e);
         }
      }

      order.endRetry(this,"i1"); // operation is complete, clear retry info
      //
      // this is even less nice than the other two places I call endRetry,
      // because there are many ways to exit the retry scope without clearing the fields.
      // for example, the order could get completed manually,
      // or it could get rebuild before the auto-complete succeeds.
      // still, this is fairly helpful for the normal rebuild case.

      return true;
   }

}

