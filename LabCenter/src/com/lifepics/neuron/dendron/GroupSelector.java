/*
 * GroupSelector.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.meta.Selector;
import com.lifepics.neuron.object.Copyable;
import com.lifepics.neuron.object.CopyUtil;
import com.lifepics.neuron.struct.SKU;

/**
 * A selector for searching for groups with various parameters.
 */

public class GroupSelector implements Selector, Copyable {

// --- fields ---

   public QueueList queueList;

   public String queueID;
   public SKU sku;
   public Integer orderStatus; // the value null also causes some restriction
   public Integer groupStatus; // status PRINTING includes all mixed statuses

   // in the UI, the order status and print status will take only some values,
   // but as far as the selector is concerned any enumeration value is legal.

// --- construction ---

   public GroupSelector(QueueList queueList) {
      this.queueList = queueList;
      // all the rest start out null
   }

   public void reinit(QueueList queueList) {
      this.queueList = queueList;
      // caller has to force requery, we can't do that
   }

// --- copy function ---

   public Object clone() throws CloneNotSupportedException { return super.clone(); }
   public GroupSelector copy() { return (GroupSelector) CopyUtil.copy(this); }

// --- implementation of Selector ---

   /**
    * Decide whether an object belongs in the subset.
    */
   public boolean select(Object o) {
      Group group = (Group) o;

   // sku

      if ( sku != null && ! group.sku.equals(sku) ) return false;

   // orderStatus

      if (orderStatus == null) { // GroupPanel.STATUS_ANY
         if (    group.order.status < Order.STATUS_ORDER_INVOICED
              || group.order.status > Order.STATUS_ORDER_PRINTED  ) return false;

      } else if (orderStatus.intValue() < Order.STATUS_ORDER_MIN) { // GroupPanel.STATUS_ALT
         if (    group.order.status < Order.STATUS_ORDER_INVOICED
              || group.order.status > Order.STATUS_ORDER_COMPLETED ) return false;

      } else {
         if (group.order.status != orderStatus.intValue()) return false;
      }

   // groupStatus

      if (groupStatus == null) { // GroupPanel.STATUS_ANY
         // no restriction, just convenient to structure this way

      } else if (groupStatus.intValue() != Group.STATUS_GROUP_PRINTING) {
         if (group.statusOrder != groupStatus.intValue()) return false;

      } else { // status PRINTING includes all mixed statuses
         if (    group.statusOrder <= Group.STATUS_GROUP_RECEIVED
              || group.statusOrder >= Group.STATUS_GROUP_PRINTED  ) return false;
      }

   // queueID (test this last, it's slower than the others)

      if (queueID != null) {
         String id = queueList.findQueueIDBySKU(group.sku);
         if ( id == null || ! id.equals(queueID) ) return false;
      }

      return true;
   }

}

