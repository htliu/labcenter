/*
 * OrderSelectors.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.meta.AndSelector;
import com.lifepics.neuron.meta.Selector;

/**
 * A utility class containing selector objects that are needed in this package.
 * Most selectors are at the app level so that they can share the start date.
 */

public class OrderSelectors {

   public static final Selector orderOpen = new Selector() {
      public boolean select(Object o) {
         OrderStub order = (OrderStub) o;
         return (    order.status >= Order.STATUS_ORDER_PENDING
                  && order.status <  Order.STATUS_ORDER_INVOICED );
      }
   };

   public static final Selector orderGo = new Selector() {
      public boolean select(Object o) {
         OrderStub order = (OrderStub) o;
         if (order.hold == Order.HOLD_INVOICE) return false;
         return (    order.status >= Order.STATUS_ORDER_RECEIVED
                  && order.status <  Order.STATUS_ORDER_INVOICED );
      }
   };

   public static final Selector orderHold = new Selector() {
      public boolean select(Object o) {
         OrderStub order = (OrderStub) o;
         return (    order.hold != Order.HOLD_NONE
                  && order.hold != Order.HOLD_INVOICE );

         // invoice hold isn't a condition that needs to be corrected,
         // it's something that occurs as part of the normal workflow.
      }
   };

   public static final Selector orderOpenHold = new AndSelector(orderOpen,orderHold);
   // this is redundant with the code in Selectors,
   // but to fix it I'd have to break the parallel structure there

}

