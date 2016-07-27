/*
 * DueThread.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.gui.User;
import com.lifepics.neuron.table.Table;
import com.lifepics.neuron.thread.Manipulator;
import com.lifepics.neuron.thread.ThreadStatus;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.TimeZone;

/**
 * A thread that watches orders and tells if they're due soon, or overdue.
 */

public class DueThread extends OrderThread {

// --- fields ---

   private long soonInterval;
   private HashMap dueMap; // maps applicable SKU code to Long dueInterval

   private SimpleDateFormat format;
   private TimeZone timeZoneDefault;
   private TimeZone timeZoneActual;
   private Date scanDateCached;
   private Date scanDateAdjusted;

// --- manipulator ---

   // this is different from all other manipulators because it operates on dueStatus.

   private static class DueManipulator implements Manipulator {

      public boolean select(Object o) {

         if ( ((OrderStub) o).isPurgeableStatus() ) return false;
         // note, being on hold doesn't prevent becoming overdue

         if ( ! (o instanceof Order) ) return false; // no due status
         if ( ((Order) o).invoiceDate == null ) return false; // old order

         // don't filter out ones that are late, because I want to allow
         // backward transitions when the config changes.  anyway,
         // most orders never become late, so filtering isn't very useful.

         return true;
      }

      public String getID(Object o) {
         return ((OrderStub) o).getID();
      }

      public boolean markActive    (Object o) { return false; }
      public boolean markIncomplete(Object o) { return false; }
      // like SilentManipulator

      public boolean markComplete(Object o) {
         OrderStub order = (OrderStub) o;
         // dueStatus already set
         order.recmodDate = new Date();
         return true;
      }

      public boolean markError(Object o, String lastError, boolean told,
                               boolean pauseRetry, long pauseRetryLimit) {
         // this shouldn't happen
         // the original error will be logged by EntityThread.
         // the order might already be on full error hold,
         // so there's really nothing more we can do here.
         return false; // object not modified, error will recur.
      }

      public boolean isRetrying(Object o) {
         return ((OrderStub) o).isRetrying();
      }
   }

// --- construction ---

   public DueThread(Table table, long scanInterval, ThreadStatus threadStatus,
                    ProductConfig productConfig,
                    long soonInterval, int skuDue, LinkedList skus,
                    String timeZone) {
      super(Text.get(DueThread.class,"s1"),
            table,
            new DueManipulator(),
            /* scanFlag = */ true,
            scanInterval,
            threadStatus);

      this.soonInterval = soonInterval;
      dueMap = DueUtil.buildMap(productConfig,skuDue,skus);

      format = new SimpleDateFormat(Text.get(DueThread.class,"s2"));
      format.setLenient(false);
      timeZoneDefault = format.getTimeZone();
      timeZoneActual  = TimeZone.getTimeZone(timeZone);
      // scan dates start out null
   }

// --- main functions ---

   protected boolean doOrder() throws Exception {
      Order order = (Order) this.order; // since we use the cast form a lot

      int dueStatus;

      Long dueInterval = DueUtil.getDueInterval(order,dueMap);
      if (dueInterval == null) { // never due (with this config)

         dueStatus = Order.DUE_STATUS_NONE;

      } else {

         adjustScanDate();
         long elapsed = scanDateAdjusted.getTime() - order.invoiceDate.getTime();

         if      (elapsed >= dueInterval.longValue())                dueStatus = Order.DUE_STATUS_LATE;
         else if (elapsed >= dueInterval.longValue() - soonInterval) dueStatus = Order.DUE_STATUS_SOON;
         else                                                        dueStatus = Order.DUE_STATUS_NONE;
      }

      if (dueStatus == order.dueStatus) return false;
      if (dueStatus >  order.dueStatus) { // upward transition, notify

         if (dueStatus == Order.DUE_STATUS_SOON) {
            User.tell(User.CODE_ORDER_DUE_SOON,Text.get(this,"i1",new Object[] { order.getFullID() }));
         } else { // overdue
            User.tell(User.CODE_ORDER_OVERDUE, Text.get(this,"i2",new Object[] { order.getFullID() }));
         }
      }

      order.dueStatus = dueStatus;
      return true;
   }

   private void adjustScanDate() throws Exception { // not really

      // avoid redoing this slow operation on every order
      if (scanDate.equals(scanDateCached)) return; // first date never null
      scanDateCached = scanDate;

      format.setTimeZone(timeZoneDefault);
      String s = format.format(scanDate);
      format.setTimeZone(timeZoneActual);
      scanDateAdjusted = format.parse(s);

      // what's going on here?  I need an absolute time to check due-ness,
      // but I want to ignore the OS time zone setting and use mine instead.
      // but, currentTimeMillis and everything else uses the OS time zone!
      // so, format it without the time zone and read it back in ... best I can do.
   }

}

