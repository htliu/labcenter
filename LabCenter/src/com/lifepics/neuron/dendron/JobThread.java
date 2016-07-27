/*
 * JobThread.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.table.Table;
import com.lifepics.neuron.table.TableException;
import com.lifepics.neuron.thread.EntityThread;
import com.lifepics.neuron.thread.Manipulator;
import com.lifepics.neuron.thread.ThreadStatus;

import java.util.LinkedList;

/**
 * A superclass for threads that operate on jobs.
 * This really adds very little, but do it anyway.
 */

public abstract class JobThread extends EntityThread {

// --- subclass hooks ---

   protected abstract boolean doJob() throws Exception;

// --- fields ---

   protected Job job;

// --- construction ---

   public JobThread(String name,
                    Table table,
                    Manipulator manipulator,
                    boolean scanFlag,
                    long idlePollInterval,
                    ThreadStatus threadStatus) {

      super(name,table,manipulator,JobUtil.orderJobID,scanFlag,idlePollInterval,threadStatus);
   }

// --- methods ---

   protected boolean doEntity() throws Exception {
      job = (Job) entity;
      return doJob();
   }

// --- utilities ---

   protected Queue getQueue(LinkedList queues, String queueID) throws Exception {

      Queue queue = QueueList.findQueueByID(queues,queueID);
      if (queue == null) {
         throw new Exception(Text.get(JobThread.class,"e1",new Object[] { queueID }));
      }

      return queue;
   }

   protected Order getOrder(Table orderTable, String fullID) throws Exception {

      Object order;
      try {
         order = orderTable.get(fullID);
      } catch (TableException e) {
         throw new Exception(Text.get(JobThread.class,"e2",new Object[] { fullID }));
      }
      // TableExceptions are assumed to come from a write failure,
      // which is a thread-stopping condition.
      // this one is still pretty bad, but it's not a thread stop.

      if ( ! (order instanceof Order) ) {
         throw new Exception(Text.get(JobThread.class,"e3",new Object[] { fullID }));
      }

      return (Order) order;
   }

}

