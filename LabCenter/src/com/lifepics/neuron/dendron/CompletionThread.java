/*
 * CompletionThread.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Log;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.table.Table;
import com.lifepics.neuron.thread.SilentManipulator;
import com.lifepics.neuron.thread.ThreadStatus;

import java.io.File;
import java.util.HashMap;
import java.util.logging.Level;

/**
 * A thread that checks whether any jobs are complete.
 */

public class CompletionThread extends JobThread implements Format.Special {

// --- fields ---

   private JobManager jobManager;

   private HashMap scanData; // map String queueID to Object

// --- construction ---

   public CompletionThread(Table jobTable, long scanInterval, ThreadStatus threadStatus,
                           JobManager jobManager) {
      super(Text.get(CompletionThread.class,"s1"),
            jobTable,
            new SilentManipulator(
               Job.STATUS_JOB_SENT,
               Job.STATUS_JOB_COMPLETED),
            /* scanFlag = */ true,
            scanInterval,
            threadStatus);

      this.jobManager = jobManager;

      scanData = new HashMap();
   }

// --- implementation of Format.Special ---

   // this is really just special code for the Fuji interface,
   // but I'm trying to keep it primarily in the format class.

   protected void scanInit() {
   }

   protected void scanExit() {
      scanData.clear();
   }

   private String getKey(int type) {
      return job.queueID + "|" + Convert.fromInt(type);
   }
   // we could use some kind of composite key
   // instead of String, but since both halves
   // are numbers, there's no ambiguity issue.

   public Object getQueueScanData(int type) {
      return scanData.get(getKey(type));
   }

   public void setQueueScanData(int type, Object o) {
      scanData.put(getKey(type),o);
   }

   public Object getQueueConfig() throws Exception {
      return jobManager.getQueueConfig(job);
   }

   public void queueError(Throwable t) {
      Log.log(Level.SEVERE,this,"e1",new Object[] { job.queueID },t);
      threadStatus.error(t);
      // normally when calling threadStatus.error you should report
      // entity ID, but here the error is associated with the queue.
   }

// --- main functions ---

   protected boolean doJob() throws Exception {

      Format format = jobManager.getFormat(job);

      if (format.isCompleteWithoutDirectory(job.property,this)) {
         updateOrder();
      } else { // the normal case

         if (job.dir == null) return false; // no directory, no completion

         File dir = format.isCompleteOrError(job.dir,job.property); // allow error
         if (dir == null) return false;

         updateOrder();

         job.dir = dir;
      }

      // caller will update status and save object
      return true;
   }

   private void updateOrder() throws Exception {

   // check referential integrity

      // this is not mathematically perfect, but it's close enough for now.
      // the problem is, updateOrder throws a TableException if the order
      // doesn't exist, and we only want a regular exception, since the error
      // is a job-stopper, not a thread-stopper.  we could add a conditional
      // wrapper around the lock in updateOrder, but then we wouldn't know if
      // the TableException was due to a missing object or to a lock failure.
      // so, take the easy way out ... it will fail once in a blue moon,
      // when an order is purged before a job (already rare), <i>and</i>
      // when it happens exactly between the getOrder and updateOrder calls.

      getOrder(jobManager.getOrderTable(),job.getFullID()); // ignore result

   // mark complete

      // this is not transactional, it's possible we might update the order
      // and then fail to update the job.  but, no big deal; since we don't
      // take the item status too seriously, we can update it again later.

      jobManager.updateOrder(job.getFullID(),job.refs,Order.STATUS_ITEM_PRINTED);
   }

}

