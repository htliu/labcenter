/*
 * JobUtil.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.gui.GridColumn;
import com.lifepics.neuron.meta.Accessor;
import com.lifepics.neuron.meta.CompoundComparator;
import com.lifepics.neuron.meta.EnumComparator;
import com.lifepics.neuron.meta.FieldComparator;
import com.lifepics.neuron.meta.NaturalComparator;
import com.lifepics.neuron.meta.NoCaseComparator;
import com.lifepics.neuron.misc.PurgeConfig;

import java.util.Comparator;
import java.util.Date;

/**
 * A utility class containing accessors, comparators, and things.
 */

public class JobUtil {

// --- accessors ---

   public static Accessor jobIDRaw = new Accessor() {
      public Class getFieldClass() { return Integer.class; }
      public Object get(Object o) { return new Integer(((Job) o).jobID); }
   };

   public static Accessor jobID = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return Convert.fromInt(((Job) o).jobID); }
   };

   public static Accessor orderSeqRaw = new Accessor() { // raw because nullable
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return ((Job) o).orderSeq; }
   };

   public static Accessor orderIDRaw = new Accessor() {
      public Class getFieldClass() { return Integer.class; }
      public Object get(Object o) { return new Integer(((Job) o).orderID); }
   };

   public static Accessor orderID = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return ((Job) o).getFullID(); }
   };

   public static Accessor statusRaw = new Accessor() {
      public Class getFieldClass() { return Integer.class; }
      public Object get(Object o) { return new Integer(((Job) o).status); }
   };

   public static Accessor holdRaw = new Accessor() {
      public Class getFieldClass() { return Integer.class; }
      public Object get(Object o) { return new Integer(((Job) o).hold); }
   };

   public static Accessor statusHold = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) {
         Job job = (Job) o;
         if (job.hold != Order.HOLD_NONE) {
            String hold = OrderEnum.fromHold(job.hold);
            return Text.get(JobUtil.class,"s1",new Object[] { hold });
         } else {
            return JobEnum.fromJobStatusExternal(job.status);
         }
      }
   };

   public static Accessor recmodDateRaw = new Accessor() {
      public Class getFieldClass() { return Date.class; }
      public Object get(Object o) { return ((Job) o).recmodDate; }
   };

   public static Accessor recmodDate = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return Convert.fromDateExternal(((Job) o).recmodDate); }
   };

// --- purge date ---

   private static PurgeConfig purgeConfig = null;

   public static void setPurgeConfig(PurgeConfig a_purgeConfig) {
      purgeConfig = a_purgeConfig;
   }

   public static int getPurgeMode(Job job) {
      if (purgeConfig == null) return PurgeConfig.MODE_NONE; // just in case

   // check whether auto-purge applies

      if ( ! purgeConfig.autoPurgeJobs ) return PurgeConfig.MODE_NONE;

   // normal case

      if (job.isPurgeableStatus() && job.hold == Order.HOLD_NONE) {
         return PurgeConfig.MODE_NORMAL;
      }
      // the hold condition is kind of arbitrary, but that's how the old purge
      // thread used to work.


   // stale case

      if (job.orderSeq == null) {
         if (purgeConfig.autoPurgeStale     ) return PurgeConfig.MODE_STALE;
      } else {
         if (purgeConfig.autoPurgeStaleLocal) return PurgeConfig.MODE_STALE;
      }

   // done testing

      return PurgeConfig.MODE_NONE;
   }

   /**
    * @return The purge date, or null if the job will not be purged.
    */
   public static Date getPurgeDate(Job job) {
      long interval;
      switch (getPurgeMode(job)) {
      case PurgeConfig.MODE_NORMAL:  interval = purgeConfig.jobPurgeInterval;       break;
      case PurgeConfig.MODE_STALE:   interval = purgeConfig.jobStalePurgeInterval;  break;
      default:                       return null;
      }
      return PurgeConfig.increment(job.recmodDate,interval);
   }

// --- orders for enums ---

   // these depend upon the *actual numerical values* of the following enumerations
   //
   //    Job.STATUS_JOB_X

   private static int[] statusOrder = { // logical order
         0, // pending
         1, // sending
         2, // sent
         4, // completed
         3  // forgotten
      };

// --- columns ---

   private static GridColumn col(int n, Accessor accessor, Comparator comparator) {
      String suffix = Convert.fromInt(n);
      String name = Text.get(JobUtil.class,"n" + suffix);
      int width;
      try {
         width = Convert.toInt(Text.get(JobUtil.class,"w" + suffix));
      } catch (ValidationException e) {
         width = 1;
         // nothing we can do in a static context
      }
      return new GridColumn(name,width,accessor,comparator);
   }

   private static Comparator nc(Accessor accessor) {
      return new FieldComparator(accessor,new NaturalComparator());
   }

   private static Comparator sc(Accessor accessor) {
      return new FieldComparator(accessor,new NoCaseComparator());
   }

   private static Comparator ec(Accessor accessor, int[] order) {
      return new FieldComparator(accessor,new EnumComparator(0,order));
   }

   public static Comparator orderJobID = nc(jobIDRaw);
   public static Comparator orderOrderID = new CompoundComparator(sc(orderSeqRaw),nc(orderIDRaw));

   public static Comparator orderStatusHold = new CompoundComparator(ec(holdRaw,OrderUtil.holdOrder),ec(statusRaw,statusOrder));

   public static GridColumn colJobID      = col(1,jobID,orderJobID);
   public static GridColumn colOrderID    = col(2,orderID,orderOrderID);
   public static GridColumn colStatusHold = col(3,statusHold,orderStatusHold);
   public static GridColumn colRecmodDate = col(4,recmodDate,nc(recmodDateRaw));

}

