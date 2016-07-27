/*
 * OrderThread.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.table.Table;
import com.lifepics.neuron.table.TableException;
import com.lifepics.neuron.thread.EntityThread;
import com.lifepics.neuron.thread.Manipulator;
import com.lifepics.neuron.thread.ThreadStatus;

import java.util.Comparator;
import java.util.Date;

/**
 * A superclass for threads that operate on orders.
 */

public abstract class OrderThread extends EntityThread {

// --- subclass hooks ---

   protected abstract boolean doOrder() throws Exception;

// --- fields ---

   protected OrderStub order; // usually also an Order
   protected Order.Item item; // convenience, not used here

// --- construction ---

   public OrderThread(String name,
                      Table table,
                      Manipulator manipulator,
                      boolean scanFlag,
                      long idlePollInterval,
                      ThreadStatus threadStatus) {

      super(name,table,manipulator,OrderUtil.orderOrderID,scanFlag,idlePollInterval,threadStatus);
   }

   public OrderThread(String name,
                      Table table,
                      Manipulator manipulator,
                      Comparator comparator,
                      boolean scanFlag,
                      long idlePollInterval,
                      ThreadStatus threadStatus) {

      super(name,table,manipulator,comparator,scanFlag,idlePollInterval,threadStatus);
   }

// --- methods ---

   protected boolean doEntity() throws Exception {
      order = (OrderStub) entity;
      return doOrder();
   }

// --- update helper ---

   protected void setItemStatus(int status) throws TableException {
      item.status = status;
      order.recmodDate = new Date();
      table.update(order,lock);
   }

}

