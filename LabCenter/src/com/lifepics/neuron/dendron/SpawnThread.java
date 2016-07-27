/*
 * SpawnThread.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Log;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.gui.User;
import com.lifepics.neuron.table.Table;
import com.lifepics.neuron.thread.EntityManipulator;
import com.lifepics.neuron.thread.ThreadStatus;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Level;

/**
 * A thread that spawns jobs for every order (if the configuration says to).
 */

public class SpawnThread extends OrderThread {

// --- fields ---

   private boolean autoSpawn;
   private boolean autoSpawnSpecial;
   private String autoSpawnPrice;
   private JobManager jobManager;

// --- status manipulator ---

   // I wanted to have this be non-static, and have it use the jobManager
   // in SpawnThread, but that didn't work ... you need the manipulator
   // to construct the SpawnThread, but you can't construct a non-static
   // manipulator until after the SpawnThread is constructed.

   private static class StatusManipulator extends EntityManipulator {

      private JobManager jobManager;
      public StatusManipulator(int statusFrom,
                               int statusActive,
                               int statusTo,
                               JobManager jobManager) {
         super(statusFrom,statusActive,statusTo);
         this.jobManager = jobManager;
      }

      public boolean markComplete(Object o) {
         Order order = (Order) o;
         order.setStatus(jobManager.computeOrderStatus(order));
         return true;
      }
      // note, this approach handles a tricky case ... if the order errors out
      // after some jobs are created, but then later completes successfully
      // without creating any more jobs (maybe if the queue settings changed?),
      // the status will still get moved to "in progress", not "ready".
   }

// --- construction ---

   public SpawnThread(Table table, long idlePollInterval, ThreadStatus threadStatus,
                      boolean autoSpawn, boolean autoSpawnSpecial, String autoSpawnPrice, JobManager jobManager) {
      super(Text.get(SpawnThread.class,"s1"),
            table,
            new StatusManipulator(
               Order.STATUS_ORDER_PRESPAWN,
               Order.STATUS_ORDER_SPAWNING,
               Order.STATUS_ORDER_INVOICED, // statusTo, not used (since there's an override)
               jobManager),
            /* scanFlag = */ false,
            idlePollInterval,
            threadStatus);

      this.autoSpawn = autoSpawn;
      this.autoSpawnSpecial = autoSpawnSpecial;
      this.autoSpawnPrice = autoSpawnPrice;
      this.jobManager = jobManager;
   }

// --- main function ---

   protected boolean doOrder() throws Exception {
      Order ocast = (Order) order;

      // even if not auto-spawning jobs, now is the time to check that
      // all the necessary mappings exist.  errors on orders have the
      // auto-retry facility available, and are easier for the user to
      // see and deal with if ve has to.
      // this isn't perfect ... if auto-spawn is off, the user has the
      // opportunity to mess things up before hitting the Print button.
      // but, that case is handled already, and this is pretty good.
      //
      jobManager.validateSKUMapping(ocast);

      if (autoSpawn) {
         if (isAllowed(ocast)) {
            spawn(ocast);
         }
      }

      order.endRetry(this,"i2"); // operation is complete, clear retry info

      if (ocast.specialInstructions != null) User.tell(User.CODE_SPECIAL_INSTRUCT,Text.get(this,"i3",new Object[] { ocast.getFullID() }) + ocast.specialInstructions);
         // notification applies even if we aren't spawning

      return true;
   }

   private boolean isAllowed(Order ocast) throws Exception {

      if ( ! autoSpawnSpecial ) { // is limit enabled?
         if (ocast.specialInstructions != null) { // does limit apply?
            return false;
         }
      }

      if (autoSpawnPrice != null) { // is limit enabled?

         int price;
         try {
            if (ocast.grandTotal == null) throw new ValidationException(Text.get(this,"e1"));
            price = Convert.toCents(ocast.grandTotal);
         } catch (ValidationException e) {
            throw new Exception(Text.get(this,"e2"),e);
         }

         int limit = Convert.toCents(autoSpawnPrice); // always works, by validation

         if (price >= limit) { // does limit apply?
            return false;
         }
      }

      return true;
   }

   public static LinkedList getUnsentItems(Order ocast) {

      LinkedList items = new LinkedList();

      Iterator i = ocast.items.iterator();
      while (i.hasNext()) {
         Order.Item item = (Order.Item) i.next();
         if (item.status == Order.STATUS_ITEM_RECEIVED) items.add(item);
      }

      return items;
   }

   private void spawn(Order ocast) throws Exception {

      LinkedList items = getUnsentItems(ocast);

      // what's the point of that?  well, if job creation fails midway,
      // so that an order goes into error status and later is unpaused,
      // we don't want to recreate all the jobs we already did.
      //
      // note, the item status update is (almost) atomic with job creation,
      // so if we create a job, we'll also update the status for all items.

      // the <i>order</i> status won't change until getFinalStatus.

      Log.log(Level.INFO,this,"i1",new Object[] { order.getFullID(), Convert.fromInt(items.size()) });

      if (items.isEmpty()) return; // nothing to spawn
      //
      // don't produce the "empty request list" error any more.  I know of
      // three ways this used to happen:
      //
      // (1) early printing.  there was no status check on the OrderDialog
      // print button, so users could print items before spawn time, and
      // if they did that for all items, we'd get this error.  it's fixed now.
      //
      // (2) order with no items.  it looks like this was the only place such
      // orders would get caught, but now OrderParser is going to handle them.
      //
      // (3) crash at wrong time.  when we're spawning jobs, we write to the
      // order file after every job is created, then once at the end to update
      // the order status.  if we crashed before the last step, we used to get
      // this, now we'll flow through correctly.

      jobManager.createWithLock(ocast,lock,null,null,items,null,/* isAuto = */ true);
   }

}

