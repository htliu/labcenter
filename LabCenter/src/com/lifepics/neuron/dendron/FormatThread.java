/*
 * FormatThread.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.gui.User;
import com.lifepics.neuron.table.Table;
import com.lifepics.neuron.thread.EntityManipulator;
import com.lifepics.neuron.thread.NotConfiguredException;
import com.lifepics.neuron.thread.ThreadStatus;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * A thread that formats jobs for printing.
 */

public class FormatThread extends JobThread implements Format.FormatCallback {

// --- fields ---

   private LinkedList queues;
   private Table orderTable;
   private HashMap dueMap;  // null for most installs
   private File burnerFile; // null for most installs (for different reasons)
   private boolean notConfigured;

// --- manipulator ---

   // a manipulator that only operates on a subset of queues, so that we
   // can run multiple copies of FormatThread simultaneously

   private static class QueueSubsetManipulator extends EntityManipulator {

      private LinkedList queues;

      public QueueSubsetManipulator(int statusFrom, int statusActive, int statusTo, LinkedList queues) {
         super(statusFrom,statusActive,statusTo);
         this.queues = queues;
      }

      public boolean select(Object o) {
         if ( ! super.select(o) ) return false;
         return (QueueList.findQueueByID(queues,((Job) o).queueID) != null);
      }
   }

// --- construction ---

   // isQueueSubset is an optimization to avoid searching through queue IDs.
   // it also makes the very common one-queue case more of a transformation.

   public FormatThread(Table jobTable, long idlePollInterval, ThreadStatus threadStatus,
                       LinkedList queues, boolean isQueueSubset, Table orderTable, HashMap dueMap, File burnerFile) {
      super(Text.get(FormatThread.class,"s1"),
            jobTable,
            isQueueSubset ? new QueueSubsetManipulator(Job.STATUS_JOB_PENDING,Job.STATUS_JOB_SENDING,Job.STATUS_JOB_SENT,queues)
                          : new EntityManipulator     (Job.STATUS_JOB_PENDING,Job.STATUS_JOB_SENDING,Job.STATUS_JOB_SENT),
            /* scanFlag = */ false,
            idlePollInterval,
            threadStatus);

      this.queues = queues;
      this.orderTable = orderTable;
      this.dueMap = dueMap;
      this.burnerFile = burnerFile;

      notConfigured = (isQueueSubset && queues.size() == 0);
   }

   protected void checkConfigured() throws Exception {

      if (notConfigured) throw new NotConfiguredException(Text.get(this,"e1")).setHint(Text.get(this,"h1"));
      // see explanation in LocalThread

      // this is a marginal case.  the original idea of NotConfiguredException
      // is that you should throw it when the thread will crash if run.
      // here the thread will run and do nothing, but I really want to make it
      // not run, to match the fact that the thread is disabled when no queues
      // are assigned to it.
   }

// --- due map helpers ---

   public static boolean usesDueMap(LinkedList queues) {
      Iterator i = queues.iterator();
      while (i.hasNext()) {
         Queue q = (Queue) i.next();
         if (q.format == Order.FORMAT_FUJI3) return true;
      }
      return false;
   }

   public static HashMap buildDueMap(ProductConfig productConfig) {
      return DueUtil.buildMap(productConfig,User.SKU_DUE_ALL,null);
   }

// --- main functions ---

   protected boolean doJob() throws Exception {

   // dereference

      Queue queue = getQueue(queues,job.queueID);
      Order order = getOrder(orderTable,job.getFullID());

   // format

      if ( ! Format.getFormat(queue.format).formatStoppable(job,order,queue.formatConfig,this) ) return false;

      job.format = new Integer(queue.format); // cache in case queue changes
      //
      // it would be possible to fill this in in SpawnThread, but since we
      // only use it to look at the directory name to detect completion,
      // it makes more sense to fill in here, where we set the directory name.

      return true;
   }

// --- implementation of FormatCallback ---

   public HashMap getDueMap() {
      return dueMap;
   }

   public File getBurnerFile() {
      return burnerFile;
   }

   public void saveJobState() throws Exception {
      job.recmodDate = new Date();
      table.update(job,lock);
      // cf. OrderThread.setItemStatus
   }

}

