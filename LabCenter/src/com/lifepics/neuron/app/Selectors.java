/*
 * Selectors.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.axon.Roll;
import com.lifepics.neuron.dendron.Job;
import com.lifepics.neuron.dendron.JobSelectors;
import com.lifepics.neuron.dendron.Order;
import com.lifepics.neuron.dendron.OrderSelectors;
import com.lifepics.neuron.dendron.OrderStub;
import com.lifepics.neuron.meta.AndSelector;
import com.lifepics.neuron.meta.NotSelector;
import com.lifepics.neuron.meta.OrSelector;
import com.lifepics.neuron.meta.Selector;

import java.util.Date;

/**
 * A utility class containing standard selector objects.
 */

public class Selectors {

// --- orders (1) ---

   public static final Selector orderOpen = OrderSelectors.orderOpen;

   public static final Selector orderPrint = new Selector() {
      public boolean select(Object o) {
         OrderStub order = (OrderStub) o;
         return (    order.status == Order.STATUS_ORDER_INVOICED
                  || order.status == Order.STATUS_ORDER_PRINTING
                  || order.status == Order.STATUS_ORDER_PRINTED  );
      }
   };

   public static final Selector orderClosed = new Selector() {
      public boolean select(Object o) {
         OrderStub order = (OrderStub) o;
         return (    order.status >= Order.STATUS_ORDER_COMPLETED );
      }
   };

// --- orders (2) ---

   public static final Selector orderHold   = OrderSelectors.orderHold;
   public static final Selector orderNoHold = new NotSelector(orderHold);

   public static final Selector orderOpenNoHold     = new AndSelector(orderOpen,    orderNoHold);
   public static final Selector orderPrintNoHold    = new AndSelector(orderPrint,   orderNoHold);
   public static final Selector orderClosedNoHold   = new AndSelector(orderClosed,  orderNoHold);

   public static final Selector orderOpenHold     = new AndSelector(orderOpen,    orderHold);
   public static final Selector orderPrintHold    = new AndSelector(orderPrint,   orderHold);
   public static final Selector orderClosedHold   = new AndSelector(orderClosed,  orderHold);

// --- jobs ---

   public static final Selector jobHold = JobSelectors.jobHold;

// --- rolls ---

   public static final Selector rollOpen = new Selector() {
      public boolean select(Object o) {
         Roll roll = (Roll) o;
         return (roll.status <  Roll.STATUS_ROLL_COMPLETED);
      }
   };

   public static final Selector rollClosed = new Selector() {
      public boolean select(Object o) {
         Roll roll = (Roll) o;
         return (roll.status >= Roll.STATUS_ROLL_COMPLETED);
      }
   };

   public static final Selector rollHold = new Selector() {
      public boolean select(Object o) {
         Roll roll = (Roll) o;
         return (roll.hold != Roll.HOLD_ROLL_NONE);
      }
   };

   public static final Selector rollOpenHold = new AndSelector(rollOpen,rollHold);

}

