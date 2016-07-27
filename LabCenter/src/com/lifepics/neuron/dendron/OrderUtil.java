/*
 * OrderUtil.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.gui.GridColumn;
import com.lifepics.neuron.meta.Accessor;
import com.lifepics.neuron.meta.CompoundComparator;
import com.lifepics.neuron.meta.EnumComparator;
import com.lifepics.neuron.meta.FieldComparator;
import com.lifepics.neuron.meta.NaturalComparator;
import com.lifepics.neuron.meta.NoCaseComparator;
import com.lifepics.neuron.misc.PurgeConfig;
import com.lifepics.neuron.struct.SKU;

import java.util.Comparator;
import java.util.Date;
import java.util.TimeZone;

/**
 * A utility class containing accessors, comparators, and things.
 */

public class OrderUtil {

// --- accessors ---

   public static Accessor orderSeqRaw = new Accessor() { // raw because nullable
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return ((OrderStub) o).orderSeq; }
   };

   public static Accessor orderIDRaw = new Accessor() {
      public Class getFieldClass() { return Integer.class; }
      public Object get(Object o) { return new Integer(((OrderStub) o).orderID); }
   };

   public static Accessor orderID = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return ((OrderStub) o).getFullID(); }
   };

   public static Accessor orderDir = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return Convert.fromFile(((OrderStub) o).orderDir); }
   };

   public static Accessor statusRaw = new Accessor() {
      public Class getFieldClass() { return Integer.class; }
      public Object get(Object o) { return new Integer(((OrderStub) o).status); }
   };

   public static Accessor holdRaw = new Accessor() {
      public Class getFieldClass() { return Integer.class; }
      public Object get(Object o) { return new Integer(((OrderStub) o).hold); }
   };

   public static Accessor statusHold = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) {
         OrderStub order = (OrderStub) o;
         if (order.hold != Order.HOLD_NONE) {
            String hold = OrderEnum.fromHold(order.hold);
            return Text.get(OrderUtil.class,"s1",new Object[] { hold });
         } else if (order.status == Order.STATUS_ORDER_INVOICED && ((Order) order).specialInstructions != null) {
            return Text.get(OrderUtil.class,"s2");
         } else if (order.status == Order.STATUS_ORDER_ABORTED) {
            return Text.get(OrderUtil.class,"s3"); // a quick substitute for splitting fromOrderStatus
         } else {
            return OrderEnum.fromOrderStatus(order.status);
         }
      }
   };

   public static Accessor recmodDateRaw = new Accessor() {
      public Class getFieldClass() { return Date.class; }
      public Object get(Object o) { return ((OrderStub) o).recmodDate; }
   };

   public static Accessor recmodDate = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return Convert.fromDateExternal(((OrderStub) o).recmodDate); }
   };

   public static Accessor name = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return (o instanceof Order) ? ((Order) o).getFullName() : ""; }
   };

   public static Accessor email = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return (o instanceof Order) ? ((Order) o).email : ""; }
   };

   public static Accessor phone = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return (o instanceof Order) ? ((Order) o).phone : ""; }
   };

   public static Accessor grandTotal = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return (o instanceof Order) ? tidy(((Order) o).grandTotal) : ""; }
   };
   private static String tidy(String grandTotal) {
      return (grandTotal == null) ? "" : ("$" + grandTotal);
   }

// --- invoice date ---

   private static TimeZone timeZone = null;

   public static void setTimeZone(TimeZone a_timeZone) {
      timeZone = a_timeZone;
   }

   public static Accessor invoiceDateRaw = new Accessor() {
      public Class getFieldClass() { return Date.class; }
      public Object get(Object o) { return (o instanceof Order) ? ((Order) o).invoiceDate : null; }
   };
   // note, NaturalComparator can handle nulls

   public static Accessor invoiceDate = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return (o instanceof Order) ? Convert.fromDateAbsoluteExternal(((Order) o).invoiceDate,timeZone) : ""; }
   };
   // note, fromDateAbsoluteExternal can handle nulls

// --- purge date ---

   private static PurgeConfig purgeConfig = null;

   public static void setPurgeConfig(PurgeConfig a_purgeConfig) {
      purgeConfig = a_purgeConfig;
   }

   public static int getPurgeMode(OrderStub order) {
      if (purgeConfig == null) return PurgeConfig.MODE_NONE; // just in case

   // check whether auto-purge applies

      if ( ! purgeConfig.autoPurgeOrders ) return PurgeConfig.MODE_NONE;

   // normal case

      if (order.isPurgeableStatus() && order.hold == Order.HOLD_NONE) {
         return PurgeConfig.MODE_NORMAL;
      }
      // the hold condition is kind of arbitrary, but that's how the old purge
      // thread used to work.

   // stale case

      if (order.status >= Order.STATUS_ORDER_RECEIVED) {
         if (order.orderSeq == null) {
            if (purgeConfig.autoPurgeStale     ) return PurgeConfig.MODE_STALE;
         } else {
            if (purgeConfig.autoPurgeStaleLocal) return PurgeConfig.MODE_STALE;
         }
      }
      // when we finish downloading an order and report back to the server,
      // that takes it off the order list.  so, we can't purge orders that
      // aren't downloaded, because they'll just come back right away.

   // done testing

      return PurgeConfig.MODE_NONE;
   }

   /**
    * @return The purge date, or null if the order will not be purged.
    */
   public static Date getPurgeDate(OrderStub order) {
      long interval;
      switch (getPurgeMode(order)) {
      case PurgeConfig.MODE_NORMAL:  interval = purgeConfig.orderPurgeInterval;       break;
      case PurgeConfig.MODE_STALE:   interval = purgeConfig.orderStalePurgeInterval;  break;
      default:                       return null;
      }
      return PurgeConfig.increment(order.recmodDate,interval);
   }

// --- computed fields ---

   public static Accessor itemCountRaw = new Accessor() {
      public Class getFieldClass() { return Integer.class; }
      public Object get(Object o) { return (o instanceof Order) ? new Integer(((Order) o).items.size()) : null; }
   };

   public static Accessor itemCount = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return (o instanceof Order) ? Convert.fromInt(((Order) o).items.size()) : ""; }
   };

   public static Accessor purgeDateRaw = new Accessor() {
      public Class getFieldClass() { return Date.class; }
      public Object get(Object o) { return getPurgeDate((OrderStub) o); }
   };

   public static Accessor purgeDate = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return Convert.fromDateExternal(getPurgeDate((OrderStub) o)); }
   };

// --- item fields ---

   public static Accessor itemFilename = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return ((Order.Item) o).filename; }
   };

   public static Accessor itemStatusRaw = new Accessor() {
      public Class getFieldClass() { return Integer.class; }
      public Object get(Object o) { return new Integer(((Order.Item) o).status); }
   };

   public static Accessor itemStatus = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return OrderEnum.fromItemStatus(((Order.Item) o).status); }
   };

   public static Accessor itemSKURaw = new Accessor() {
      public Class getFieldClass() { return SKU.class; }
      public Object get(Object o) { return ((Order.Item) o).sku; }
   };

   public static Accessor itemSKU = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return ((Order.Item) o).sku.toString(); }
   };

   public static Accessor itemQuantityRaw = new Accessor() {
      public Class getFieldClass() { return Integer.class; }
      public Object get(Object o) { return new Integer(((Order.Item) o).quantity); }
   };

   public static Accessor itemQuantity = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return Convert.fromInt(((Order.Item) o).quantity); }
   };

// --- orders for enums ---

   // these depend upon the *actual numerical values* of the following enumerations
   //
   //    Order.STATUS_ORDER_X
   //    Order.HOLD_X

   private static int[] statusOrder = { // logical order
         0, // pending
         1, // receiving
         2, // received
         3, // formatting
         4, // formatted
         5, // invoicing
         6, // prespawn
         7, // spawning
         8, // invoiced
         9, // printing
         10, // printed
         13, // completed
         12, // aborted
         11  // canceled
      };

   public static int[] holdOrder = { // order by need for attention
         4, // none
         3, // user
         0, // error
         2, // invoice
         1, // retry
      };

// --- columns ---

   private static GridColumn col(int n, Accessor accessor, Comparator comparator) {
      String suffix = Convert.fromInt(n);
      String name = Text.get(OrderUtil.class,"n" + suffix);
      int width;
      try {
         width = Convert.toInt(Text.get(OrderUtil.class,"w" + suffix));
      } catch (ValidationException e) {
         width = 1;
         // nothing we can do in a static context
      }
      return new GridColumn(name,width,accessor,comparator);
   }

   private static Comparator nc(Accessor accessor) {
      return new FieldComparator(accessor,new NaturalComparator());
   }

   private static Comparator sc(Accessor accessor) {
      return new FieldComparator(accessor,new NoCaseComparator());
   }

   private static Comparator ec(Accessor accessor, int[] order) {
      return new FieldComparator(accessor,new EnumComparator(0,order));
   }

   public static Comparator orderOrderID = new CompoundComparator(sc(orderSeqRaw),nc(orderIDRaw));

   public static Comparator orderStatusHold = new CompoundComparator(ec(holdRaw,holdOrder),ec(statusRaw,statusOrder));

   public static Comparator orderItemFilename = sc(itemFilename);
   public static Comparator orderItemSKU = new FieldComparator(itemSKURaw,SKUComparator.displayOrder);
   public static Comparator orderItemQuantity = nc(itemQuantityRaw);

   public static GridColumn colOrderID    = col(1,orderID,orderOrderID);
   public static GridColumn colOrderDir   = col(2,orderDir,sc(orderDir));
   public static GridColumn colStatusHold = col(3,statusHold,orderStatusHold);
   public static GridColumn colRecmodDate = col(4,recmodDate,nc(recmodDateRaw));
   public static GridColumn colName       = col(5,name,sc(name));
   public static GridColumn colEmail      = col(6,email,sc(email));
   public static GridColumn colPhone      = col(7,phone,sc(phone));
   public static GridColumn colGrandTotal = col(15,grandTotal,null); // not yet sortable
   public static GridColumn colInvoiceDate = col(10,invoiceDate,nc(invoiceDateRaw));
   public static GridColumn colItemCount  = col(8,itemCount,nc(itemCountRaw));
   public static GridColumn colPurgeDate  = col(9,purgeDate,nc(purgeDateRaw));

   public static GridColumn colItemFilename = col(11,itemFilename,orderItemFilename);
   public static GridColumn colItemStatus   = col(12,itemStatus,null); // not yet sortable
   public static GridColumn colItemSKU      = col(13,itemSKU,orderItemSKU);
   public static GridColumn colItemQuantity = col(14,itemQuantity,orderItemQuantity);

}

