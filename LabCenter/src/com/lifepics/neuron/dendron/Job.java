/*
 * Job.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Log;
import com.lifepics.neuron.core.Reportable;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.gui.User;
import com.lifepics.neuron.struct.*;
import com.lifepics.neuron.thread.Entity;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Date;

/**
 * An object that holds all the information about a queued print job.
 */

public class Job extends Structure implements Reportable, Entity {

// --- fields ---

   public int jobID;
   public String queueID;
   public String orderSeq; // null for main sequence
   public int orderID;
   public LinkedList refs; // primary keys of order items

   public Integer overrideQuantity; // nullable

   public int status;
   public int hold; // use Order.HOLD_X enumeration
   public String lastError; // nullable

   public Date recmodDate;

   public Integer format; // nullable ; cached in case queue changes
   public File dir; // nullable
   public LinkedList files; // list of string paths relative to dir
   public Boolean dirOwned; // nullable, with null meaning true
   public String property; // a user-definable property; nullable

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      Job.class,
      0,0,
      new AbstractField[] {

         new IntegerField("jobID","JobID"),
         new StringField("queueID","QueueID"),
         new NullableStringField("orderSeq","OrderSeq"),
         new IntegerField("orderID","OrderID"),
         // (*) see below for refs

         new NullableIntegerField("overrideQuantity","OverrideQuantity"),

         new EnumeratedField("status","Status",JobEnum.jobStatusInternalType),
         new EnumeratedField("hold","Hold",OrderEnum.holdType),
         new NullableStringField("lastError","LastError"),

         new DateField("recmodDate","RecmodDate"),

         // (*) out of order, but makes XML file easier to read
         new StructureListField("refs","Ref",Ref.sd,Merge.NO_MERGE),

         new NullableEnumeratedField("format","Format",OrderEnum.formatType),
         new NullableFileField("dir","Dir"),
         new InlineListField("files","File"),
         new NullableBooleanField("dirOwned","DirOwned"),
         new NullableStringField("property","Property")
      });

   protected StructureDefinition sd() { return sd; }

// --- implementation of Reportable ---

   // it might be nicer to send Log.ORDER and the order information,
   // but we can't do that without the order's wholesale record.
   // sending the job ID isn't all that useful, but there's no harm
   // in it, and we need to implement Reportable for local orders.

   public String getReportType() {
      return Log.JOB;
   }

   public String getReportID() {
      return Convert.fromInt(jobID);
   }

   public String getReportMerchant() {
      return null;
   }

   public String getLocalOrderSeq() {
      return orderSeq;
   }

   public int getLocalOrderID() {
      return orderID;
   }

   public int getLocalStatusCode() {
      return LocalStatus.CODE_ERR_JOB_GENERIC;
   }

// --- implementation of Entity ---

   public String getID() {
      return Convert.fromInt(jobID);
   }

   public boolean testStatus(int statusFrom, int statusActive) {
      return (    (    status == statusFrom
                    || status == statusActive )
               && hold == Order.HOLD_NONE );
   }

   public void setStatus(int status) {
      this.status = status;
      recmodDate = new Date();
   }

   public void setStatusError(int statusFrom, String lastError, boolean told,
                              boolean pauseRetry, long pauseRetryLimit) {

      // the pauseRetry fields are ignored ... jobs don't support that,
      // and don't need to, since it only applies to network activity.

      status = statusFrom;
      hold = Order.HOLD_ERROR;
      this.lastError = lastError;
      recmodDate = new Date();

      if ( ! told ) User.tell(User.CODE_JOB_ERROR,Text.get(this,"e3",new Object[] { getFullID() }) + lastError);
      // show order ID, not job ID, since that's what the user will see in the UI.
      // if we ever allow job retries, probably check isRetrying here, same as in OrderStub.
   }

   public boolean isRetrying() {
      return false; // jobs don't have retry states
   }

// --- job status enumeration ---

   private static final int STATUS_JOB_MIN = 0;

   public static final int STATUS_JOB_PENDING = 0;
   public static final int STATUS_JOB_SENDING = 1;
   public static final int STATUS_JOB_SENT = 2;
   public static final int STATUS_JOB_COMPLETED = 3;
   public static final int STATUS_JOB_FORGOTTEN = 4;

   private static final int STATUS_JOB_MAX = 4;

// --- helper functions ---

   public boolean isPurgeableStatus() {
      return (    status == Job.STATUS_JOB_COMPLETED
               || status == Job.STATUS_JOB_FORGOTTEN );
   }

   public String getFullID() {
      return OrderStub.getFullID(orderSeq,orderID);
   }

   public int getOutputQuantity(Order.Item item, Ref ref) {
      if (ref.individualQuantity != null) return ref.individualQuantity.intValue();
      if (overrideQuantity       != null) return overrideQuantity      .intValue();
      return item.quantity;
   }

   // I wanted to allow SKU override, too, but that's much more difficult.
   // e.g., you have to change how the jobs are sent to the queues,
   // how Noritsu splits jobs, and how Fuji organized the items by SKU.
   // but, the clincher is, overriding SKU breaks the primary key-ness
   // of (filename,SKU), so that you ought to re-merge, as in OrderParser.
   // there are details in the archive for 2005-06-11.

// --- validation ---

   public void validate() throws ValidationException {

      OrderStub.validateOrderSeq(orderSeq);
      // not really necessary, since every job has to come from an order, and we validate there

      Iterator i = refs.iterator();
      while (i.hasNext()) {
         ((Ref) i.next()).validate();
      }

      if (overrideQuantity != null) Order.validateQuantity(overrideQuantity.intValue());

      if (status < STATUS_JOB_MIN || status > STATUS_JOB_MAX) {
         throw new ValidationException(Text.get(this,"e1",new Object[] { Convert.fromInt(status) }));
      }

      if (hold < Order.HOLD_MIN || hold > Order.HOLD_MAX) {
         throw new ValidationException(Text.get(this,"e2",new Object[] { Convert.fromInt(hold) }));
      }

      if (format != null) Order.validateFormat(format.intValue());
   }

// --- ref class ---

   // the filename and SKU are the primary key of the referenced order item

   // the individual quantity works in general, but it's not accessible through any UI,
   // just used internally to split Konica and Noritsu jobs that are too large for one
   // job file.  (it wouldn't work to put it in a UI because the splitter code doesn't
   // read from it, just writes to it)

   public static class Ref extends Structure {

      public String filename; // may not actually be a file name; see Order.Item for details
      public SKU sku;
      public Integer individualQuantity; // nullable
      public Integer doneQuantity;       //

      public Ref() {}
      public Ref(String filename, SKU sku) { this.filename = filename; this.sku = sku; }

      public static final StructureDefinition sd = new StructureDefinition(

         Ref.class,
         // no version
         new AbstractField[] {

            new StringField("filename","Filename"),
            new SKUField("sku","Sku","SKU"),
            new NullableIntegerField("individualQuantity","IndividualQuantity"),
            new NullableIntegerField("doneQuantity","DoneQuantity")
         });

      protected StructureDefinition sd() { return sd; }

      public void validate() throws ValidationException {
         if (individualQuantity != null) Order.validateQuantity(individualQuantity.intValue());
         // not worth validating doneQuantity
      }

      public int getDoneQuantity() {
         // it's too late to add a validation for negative doneQuantity,
         // but we can treat negative as zero.
         return (doneQuantity != null) ? Math.max(doneQuantity.intValue(),0) : 0;
      }
   }

}

