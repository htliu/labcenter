/*
 * ReportThread.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.ReportQueue;
import com.lifepics.neuron.core.ReportRecord;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.misc.LineFormatter;
import com.lifepics.neuron.net.DefaultHandler;
import com.lifepics.neuron.net.DiagnoseConfig;
import com.lifepics.neuron.net.DiagnoseHandler;
import com.lifepics.neuron.net.FormDataTransaction;
import com.lifepics.neuron.net.MerchantConfig;
import com.lifepics.neuron.net.PauseAdapter;
import com.lifepics.neuron.net.Query;
import com.lifepics.neuron.thread.StoppableThread;
import com.lifepics.neuron.thread.ThreadStatus;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Level;

/**
 * A thread that sends log information to the server.
 */

public class ReportThread extends StoppableThread {

// --- fields ---

   private ReportQueue reportQueue;
   private String reportURL;
   private long idlePollInterval;
   private ThreadStatus threadStatus;
   private MerchantConfig merchantConfig;

   private DiagnoseHandler handler;
   private PauseAdapter pauseAdapter;

// --- construction ---

   public ReportThread(ReportQueue reportQueue, String reportURL, long idlePollInterval, ThreadStatus threadStatus,
                       MerchantConfig merchantConfig, DiagnoseConfig diagnoseConfig) {
      super(Text.get(ReportThread.class,"s1"));

      this.reportQueue = reportQueue;
      this.reportURL = reportURL;
      this.idlePollInterval = idlePollInterval;
      this.threadStatus = threadStatus;
      this.merchantConfig = merchantConfig;

      handler = new DiagnoseHandler(new DefaultHandler(),diagnoseConfig);
      // no describe handler here
      pauseAdapter = new PauseAdapter(threadStatus);
   }

// --- interface for thread fields ---

   protected void doInit() throws Exception {
   }

   protected void doRun() throws Exception {
      try {
         while ( ! isStopping() ) {
            ReportRecord r = reportQueue.getRecord();
            if (r != null) {
               if (doRecord(r)) reportQueue.removeRecord();
            } else {
               sleepNice(idlePollInterval);
            }
         }
      } catch (Exception e) {
         if ( ! isStopping() ) threadStatus.fatal(e); // StoppableThread will log
         throw e;
      }
   }

   protected void doExit() {
   }

   protected void doStop() {
   }

// --- methods ---

   private boolean doRecord(ReportRecord r) {
      try {
         return handler.run(new PostRecord(r),pauseAdapter);
      } catch (Exception e) {
         return true;
         // unable to send, even with diagnose and retry, so tough luck.
         // don't log anything about it, we'd just have to report *that*.
      }
   }

// --- timestamp format ---

   private static SimpleDateFormat timestampFormat = new SimpleDateFormat(Text.get(ReportThread.class,"f1"));

   static {
      timestampFormat.setTimeZone(TimeZone.getTimeZone("US/Mountain"));
      // could be anything, we send the zone too, but this is convenient
   }

// --- transactions ---

   private class PostRecord extends FormDataTransaction {
      private ReportRecord r;
      public PostRecord(ReportRecord r) { this.r = r; }

      public String describe() { return Text.get(ReportThread.class,"s2"); }
      protected String getFixedURL() { return reportURL; }
      protected void getFormData(Query query) throws IOException {

         // r.merchant is always null for rolls and jobs.  for orders it's copied
         // from wholesale.merchant, so nullness is a proxy for wholesale == null.
         // we can use that to reproduce the getWholesaleCode logic here.

         String configMerchant = Convert.fromInt(merchantConfig.merchant);
         if (merchantConfig.isWholesale) { // WSC_WHOLESALE
            query.add("wholesalerID",configMerchant);
            if (r.merchant != null) query.add("mlrfnbr",r.merchant);
            // r.merchant can be null if there's an order without wholesale information ...
            // or if there's an upload, or a job, or an error with no reportable object
         } else if (r.merchant != null && ! r.merchant.equals(configMerchant)) { // WSC_PRO
            query.add("dealerLocID",configMerchant);
            query.add("mlrfnbr",r.merchant);
         } else { // WSC_NORMAL and WSC_PSEUDO
            query.add("mlrfnbr",configMerchant);
         }
         query.addPasswordObfuscate("encpassword",merchantConfig.password);
         query.add("timestamp",timestampFormat.format(new Date(r.timestamp)));

         String stringLevel = "";

              if (r.level.equals(Level.SEVERE )) stringLevel = "E";
         else if (r.level.equals(Level.WARNING)) stringLevel = "W";
         else if (r.level.equals(Level.INFO   )) stringLevel = "N";
         else if (r.level.equals(Level.CONFIG )) stringLevel = "C";
         else if (r.level.equals(Level.FINE   )) stringLevel = "1";
         else if (r.level.equals(Level.FINER  )) stringLevel = "2";
         else if (r.level.equals(Level.FINEST )) stringLevel = "3";
         // else leave blank, but that's the complete enumeration

         query.add("level",stringLevel);

         query.add("message",r.message);

         String trace = LineFormatter.getStackTrace(r.t);
         if (trace == null) trace = "";
         query.add("trace",trace);

         String stringType, stringID;
         if (r.idType != null && r.id != null) {
            stringType = r.idType;
            stringID   = r.id;
         } else {
            stringType = "";
            stringID   = "";
         }
         // jobs come through with null type, non-null id,
         // and we don't want to send a partial blank row.
         query.add("obj-type",stringType);
         query.add("obj-id",  stringID  );
      }

      protected boolean receive(InputStream inputStream) throws Exception {
         return true;
      }
   }

}

