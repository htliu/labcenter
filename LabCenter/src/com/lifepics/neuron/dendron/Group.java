/*
 * Group.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.struct.NewSKU;
import com.lifepics.neuron.struct.OldSKU;
import com.lifepics.neuron.struct.SKU;

/**
 * A derived object that represents a group of order items with the same SKU.
 * This is the granularity at which information is presented in the new view.
 */

public class Group {

// --- fields ---

   public String key; // computed
   public Order order;
   public SKU sku;
   public String statusString;
   public double statusOrder;
   public int totalItems;
   public int totalQuantity;

// --- group status enumeration ---

   // this is similar to Order.STATUS_ITEM_X, but not the same;
   // also the numerical values are different and important.
   //
   // this is not a complete enumeration, some mixed statuses are also possible,
   // including mixed statuses with the same value as STATUS_GROUP_PRINTING.

   public static final int STATUS_GROUP_NOT_RECEIVED = -2; // shouldn't be visible
   public static final int STATUS_GROUP_RECEIVED     = -1;
   public static final int STATUS_GROUP_PRINTING     =  0;
   public static final int STATUS_GROUP_PRINTED      =  1;

// --- helper ---

   public void compute() {

      char code;
      String id;

      if (sku instanceof NewSKU) {
         code = 'n';
         id = NewSKU.encode((NewSKU) sku);
      } else { // OldSKU
         code = 'o';
         id = OldSKU.encode((OldSKU) sku);
      }

      key = order.getFullID() + code + id;

      // the code chars are internal text, not user interface text.

      // the code follows the order ID, which is an integer,
      // so there's no way to confuse it with a SKU letter.
      // as a result, key equality implies order and SKU equality.
      //
      // actually the order ID may start with some letters now,
      // but the integer in the order ID is still well-defined.
   }

}

