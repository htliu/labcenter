/*
 * LocalStatus.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.LocalReportInterface;
import com.lifepics.neuron.core.Log;
import com.lifepics.neuron.core.Reportable;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.misc.FileUtil;
import com.lifepics.neuron.misc.LineFormatter;

import java.util.logging.Level;
import java.io.File;
import java.io.FileWriter;

/**
 * A utility class for reporting status of local orders.
 */

public class LocalStatus implements LocalReportInterface {

// --- constants ---

   public static final int CODE_REJECTED_GENERIC = 0;

   public static final int CODE_INFO_GENERIC    = 20;
   public static final int CODE_INFO_ACCEPTED   = 21;
   public static final int CODE_INFO_COMPLETED  = 22;
   public static final int CODE_INFO_ABORTED    = 23;
   public static final int CODE_INFO_CANCELED   = 24;

   public static final int CODE_ERR_ORD_GENERIC = 40;
   public static final int CODE_ERR_ORD_CHANNEL = 41;

   public static final int CODE_ERR_JOB_GENERIC = 60;

   public static final int CODE_ERR_OTH_GENERIC = 80;

   private static final String STATUS_DIR = "status";

   private static final String SUFFIX_TMP = ".tmp";
   private static final String SUFFIX_TXT = ".txt";

// --- config ---

   private static File statusDir = null;

   public static void setConfig(boolean localEnabled, LocalConfig localConfig) {
      statusDir = localEnabled ? new File(localConfig.directory,STATUS_DIR) : null;
   }

   // originally I wanted to make statusDir relative to orderDir, so that orders
   // would keep reporting to the same statusDir throughout their lifespan.
   // that was inconvenient, though, because for job errors we don't know orderDir.
   // it was also a bad idea ... the point of statusDir is that someone's going to
   // poll there for status reports, and they won't want to poll multiple places.

// --- utility function ---

   public static int translate(int status) {
      switch (status) {
      case Order.STATUS_ORDER_COMPLETED:  return CODE_INFO_COMPLETED;
      case Order.STATUS_ORDER_ABORTED:    return CODE_INFO_ABORTED;
      case Order.STATUS_ORDER_CANCELED:   return CODE_INFO_CANCELED;
      default:
         throw new IllegalArgumentException(); // programmer error
      }
   }

// --- implementation of LocalReportInterface ---

   public void report(Reportable reportable,
                      long timestamp, Level level, String message, Throwable t) {

      if ( ! reportable.getLocalOrderSeq().equals(Order.ORDER_SEQUENCE_LOCAL) ) return;
      // caller already checked orderSeq not null

      Throwable tSave = t;
      int code = reportable.getLocalStatusCode();

      // check for override
      for ( ; t != null; t = t.getCause()) {

         if (t instanceof LocalException) {
            code = ((LocalException) t).getCode();
            break;
         }
      }

      report(reportable.getLocalOrderID(),code,timestamp,level,message,tSave);
   }

// --- report function ---

   private static LineFormatter lineFormatter = new LineFormatter();
   private static Object lock = new Object();

   /*
    * @param message The message, including the exception message if any.
    * @param t Null is allowed here.
    */
   public static void report(int orderID, int code,
                             long timestamp, Level level, String message, Throwable t) {

      if (statusDir == null) return; // no reports when disabled
      String s = lineFormatter.format(timestamp,level,message,t);

      // has to be synchronized so that we don't generate the same name in two threads,
      // and synchronization has to continue until file is opened, for the same reason.
      // otherwise, one FileWriter would overwrite the other, possibly leaving garbage
      // at the end!  in theory we could have one lock per folder, but in practice the
      // local orders will all mostly be in the same folder.
      //
      synchronized (lock) {

         File fileTmp;
         File fileTxt;

         for (int i=0; ; i++) {

            String name = Text.get(LocalStatus.class,"s1",new Object[] { Convert.fromInt(orderID), Convert.fromInt(code), Convert.fromInt(i) });
            // convert calls are inefficient, but we'll normally only do this once or twice

            fileTmp = new File(statusDir,name + SUFFIX_TMP);
            fileTxt = new File(statusDir,name + SUFFIX_TXT);

            if ( ! fileTmp.exists() && ! fileTxt.exists() ) break;
         }

         try {

            if ( ! statusDir.exists() ) FileUtil.makeDirectory(statusDir);

            FileWriter writer = new FileWriter(fileTmp);
            try {
               writer.write(s);
            } finally {
               writer.close();
            }
            if ( ! fileTmp.renameTo(fileTxt) ) throw new Exception(Text.get(LocalStatus.class,"e2"));

         } catch (Exception e) {

            if (fileTmp.exists()) fileTmp.delete(); // ignore result of delete

            Log.log(Level.WARNING,LocalStatus.class,"e1",new Object[] { Convert.fromFile(fileTxt) },e);
            //
            // don't log with order object, because (1) we don't have it here,
            // (2) for rejection reports there *is* no order object (and also
            // no full ID), and (3) we might produce recursion.
         }
      }
   }

}

