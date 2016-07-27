/*
 * GroupDerivation.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.struct.SKU;
import com.lifepics.neuron.table.Derivation;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * A derivation that turns an {@linkplain Order order} into a set of {@linkplain Group SKU-groups}.
 */

public class GroupDerivation implements Derivation {

// --- getKey ---

   /**
    * Get the primary key for a derived object.
    */
   public String getKey(Object d) {
      return ((Group) d).key;
   }

// --- constants ---

   private static final String delimiter = ' ' + Text.get(GroupDerivation.class,"s1") + ' ';

// --- derive ---

   /**
    * Construct a set of derived objects from an original object.
    */
   public Collection derive(Object o) {

      LinkedList groups = new LinkedList();

      if ( ! (o instanceof Order) ) return groups; // stubs have no groups
      Order order = (Order) o;

   // first pass, gather unique SKUs

      HashSet skus = new HashSet();

      Iterator i = order.items.iterator();
      while (i.hasNext()) {
         Order.Item item = (Order.Item) i.next();
         skus.add(item.sku);
      }

   // second pass, construct group per SKU

      i = skus.iterator();
      while (i.hasNext()) {
         SKU sku = (SKU) i.next();
         groups.add(derive(order,sku));
      }

      return groups;
   }

   private Group derive(Order order, SKU sku) {

   // set up

      Group group = new Group();

      group.order = order;
      group.sku = sku;
      group.compute(); // compute key

      // status fields computed later

      // total fields incremented as we go
      group.totalItems = 0;
      group.totalQuantity = 0;

   // iterate

      // mostly, a group will go through the printing process as a whole,
      // so the status will be the same for all items.
      // however, we want to be able to describe other cases that occur.
      //
      // it's possible that we will show orders before they're downloaded,
      // so handle that case too ... that can all display as one status.

      int nNotReceived = 0;
      int nReceived = 0;
      int nPrinting = 0;
      int nPrinted = 0;

      int qNotReceived = 0;
      int qReceived = 0;
      int qPrinting = 0;
      int qPrinted = 0;

      Iterator i = order.items.iterator();
      while (i.hasNext()) {
         Order.Item item = (Order.Item) i.next();
         if ( ! item.sku.equals(sku) ) continue;

         group.totalItems++;
         group.totalQuantity += item.quantity;

         switch (item.status) {
         default:                          nNotReceived++;  qNotReceived += item.quantity;  break;
         case Order.STATUS_ITEM_RECEIVED:  nReceived++;     qReceived    += item.quantity;  break;
         case Order.STATUS_ITEM_PRINTING:  nPrinting++;     qPrinting    += item.quantity;  break;
         case Order.STATUS_ITEM_PRINTED:   nPrinted++;      qPrinted     += item.quantity;  break;
         }
      }

   // compute status

      int nTotal = group.totalItems; // just for readability
      int qTotal = group.totalQuantity;

      int status;

      if (nNotReceived > 0)         status = Group.STATUS_GROUP_NOT_RECEIVED;
      else if (nReceived == nTotal) status = Group.STATUS_GROUP_RECEIVED;
      else if (nPrinting == nTotal) status = Group.STATUS_GROUP_PRINTING;
      else if (nPrinted  == nTotal) status = Group.STATUS_GROUP_PRINTED;

      else { // some combination of the three

         // turns out we want to display quantity, not items. note that qTotal
         // can't be zero, because there's a validation that item.quantity > 0.

         group.statusString = qReceived + delimiter + qPrinting + delimiter + qPrinted;
         group.statusOrder = (qPrinted - qReceived) / (double) qTotal;
         return group;

         // statusOrder is weighted sum, -1 per received, 0 per printing, 1 per printed
         // so, it agrees with the above in the limiting cases (which don't reach here)
         //
         // even if called with a nonexistent SKU,
         // can't get divide by zero here,
         // because the nReceived case would fire
      }

      // finish up all the non-mixed cases

      group.statusString = GroupEnum.fromGroupStatus(status);
      group.statusOrder = status; // int into double
      return group;
   }

}

