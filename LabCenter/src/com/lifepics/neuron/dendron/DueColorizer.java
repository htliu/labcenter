/*
 * DueColorizer.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.gui.ViewHelper;
import com.lifepics.neuron.meta.Accessor;

import java.awt.Color;

/**
 * A small helper class to standardize order row coloring.
 */

public class DueColorizer implements ViewHelper.Colorizer {

   private Accessor accessor;

   /**
    * @param accessor An accessor that gets orders from row objects,
    *                 or null if the row objects are already orders.
    */
   public DueColorizer(Accessor accessor) { this.accessor = accessor; }

   public Color get(Object o) {
      if (accessor != null) o = accessor.get(o);

      Color color = Color.white;
      if (o instanceof Order) {
         Order order = (Order) o;

         if (order.isPurgeableStatus()) {
            // may have dueStatus, no longer matters
         } else {
            switch (order.dueStatus) {
            case Order.DUE_STATUS_SOON:  color = Color.yellow;  break;
            case Order.DUE_STATUS_LATE:  color = Color.red;     break;
            }
         }
      }
      // else might be OrderStub, might be programmer error

      return color;
   }

}

