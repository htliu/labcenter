/*
 * StatusNews.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.axon.ProMode;
import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Log;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.dendron.Format;
import com.lifepics.neuron.dendron.Order;
import com.lifepics.neuron.dendron.Queue;
import com.lifepics.neuron.dendron.QueueList;
import com.lifepics.neuron.net.DownloadTransaction;
import com.lifepics.neuron.net.HTTPTransaction;
import com.lifepics.neuron.net.PostTransaction;
import com.lifepics.neuron.net.Query;
import com.lifepics.neuron.struct.*;
import com.lifepics.neuron.table.AlternatingFile;
import com.lifepics.neuron.thread.StoppableGroup;
import com.lifepics.neuron.thread.StoppableThread;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TimeZone;
import java.util.logging.Level;

import java.awt.EventQueue;

/**
 * A utility class that handles the status-news transaction.
 */

public class StatusNews {

// --- data carrier ---

   public static class Counts {

      public int pausedOrders;
      public int pausedJobs;
      public int pausedRolls;
   }

// --- main ---

   public static void initiate(Config config, AutoUpdate.InstanceRecord ir,
                               Counts counts, Double kbpsDownload, Double kbpsUpload,
                               File statusFile,
                               File newsFile, File newsTemp, Runnable continuation) {

      // the config object is accessed entirely up front in the UI thread,
      // so there's no question of the config changing in mid-calculation.
      // we don't even hold a reference to it.

      Log.log(Level.FINER,StatusNews.class,"i2"); // in case we throw some exception

      Query data;
      try {
         data = getStatusData(config,ir,counts,kbpsDownload,kbpsUpload);
      } catch (IOException e) {
         return;
         // this doesn't happen, it's just a formality related to URLEncoder
      }
      StatusTransaction tStatus = new StatusTransaction(config.statusURL,data,config.logLevel);

      StatusInfo statusInfo = getStatusInfo(config);

      NewsTransaction tNews = new NewsTransaction(config.newsURL,newsTemp);

      new StatusNewsThread(active,tStatus,tNews,statusFile,statusInfo,newsFile,newsTemp,continuation).start();
      //
      // this is fairly unusual ... most threads fall into one of two categories.
      // (1) threads that run indefinitely and are tracked by the system
      // (2) threads that the UI uses to show a wait dialog on a long operation
      //
      // it seems to me now (much later) that the reason I did it this way
      // is that I need to be in the UI thread to get the count information.
      // now I need to be able to stop the thread, but I'm still not going
      // to make it a proper subsystem, for the same reason.
   }

   private static StoppableGroup active = new StoppableGroup();
   public static void terminate() { active.stopNice(); }
   // there should be at most one thread in the group at a time;
   // this is just easier than tracking the individual thread.

   private static class StatusNewsThread extends StoppableThread {

      private HTTPTransaction tStatus;
      private HTTPTransaction tNews;
      private File statusFile;
      private StatusInfo statusInfo;
      private File newsFile;
      private File newsTemp;
      private Runnable continuation;

      public StatusNewsThread(ThreadGroup group, HTTPTransaction tStatus, HTTPTransaction tNews, File statusFile, StatusInfo statusInfo, File newsFile, File newsTemp, Runnable continuation) {
         super(group,Text.get(StatusNews.class,"s1"));

         this.tStatus = tStatus;
         this.tNews = tNews;
         this.statusFile = statusFile;
         this.statusInfo = statusInfo;
         this.newsFile = newsFile;
         this.newsTemp = newsTemp;
         this.continuation = continuation;
      }

      protected void doInit() {
      }

      protected void doRun() {

         // if these fail, just log it and hope it works next time

         boolean success = false;

         try {
            if ( ! tStatus.run(null) ) return; // stopping
            success = true;
         } catch (Exception e) {
            Log.log(Level.WARNING,StatusNews.class,"e4",e);
         }

         if (success) { // update status file
            try {
               updateStatusFile(statusFile,statusInfo);
            } catch (Exception e) {
               Log.log(Level.WARNING,StatusNews.class,"e5",e);
            }
         }

         try {

            if ( ! tNews.run(null) ) return; // stopping

            // successful download, now try to swap files
            if ( newsFile.exists() && ! newsFile.delete() ) throw new Exception(Text.get(StatusNews.class,"e2"));
            if ( ! newsTemp.renameTo(newsFile)            ) throw new Exception(Text.get(StatusNews.class,"e3"));

            EventQueue.invokeLater(continuation);

         } catch (Exception e) {
            Log.log(Level.WARNING,StatusNews.class,"e1",e);
         }
      }

      protected void doExit() {
      }

      protected void doStop() {
      }
   }

// --- status data ---

   private static Query getStatusData(Config config, AutoUpdate.InstanceRecord ir,
                                      Counts counts, Double kbpsDownload, Double kbpsUpload) throws IOException {

      Query query = new Query();

      if (config.merchantConfig.isWholesale) {
         query.add("wholesalerID",Convert.fromInt(config.merchantConfig.merchant));
      } else {
         query.add("mlrfnbr",Convert.fromInt(config.merchantConfig.merchant));
      }
      query.addPasswordObfuscate("encpassword",config.merchantConfig.password);
      query.addPasswordCleartext("password",config.merchantConfig.password);
      if (ir.instanceID != null) query.add("instance",Convert.fromInt(ir.instanceID.intValue()));
      if (ir.passcode   != null) query.add("passcode",Convert.fromInt(ir.passcode  .intValue()));

      query.add("timestamp",timestampFormat.format(new Date()));

      query.add("version",AboutDialog.getVersion());

      String java = System.getProperty("java.version");
      if (java != null) query.add("java",java);

      query.add("download",zeroOne(config.downloadEnabled && ! ProMode.isPro(config.proMode)));
      query.add("upload",  zeroOne(config.  uploadEnabled));

      if (kbpsDownload != null) query.add("downkbps",kbpsFormat.format(kbpsDownload.doubleValue()));
      if (kbpsUpload   != null) query.add("upkbps",  kbpsFormat.format(kbpsUpload  .doubleValue()));

      query.add("integration",summarize(config.queueList));

      query.add("orders",Convert.fromInt(counts.pausedOrders));
      query.add("jobs",  Convert.fromInt(counts.pausedJobs  ));
      query.add("rolls", Convert.fromInt(counts.pausedRolls ));

      return query;
   }

   private static DecimalFormat kbpsFormat = new DecimalFormat("0.0");

   private static SimpleDateFormat timestampFormat = new SimpleDateFormat(Text.get(StatusNews.class,"f1"));
   static {
      timestampFormat.setTimeZone(TimeZone.getTimeZone("US/Mountain"));
      // could be anything, we send the zone too, but this is convenient
   }

   private static String zeroOne(boolean b) { return b ? "1" : "0"; }

   private static String summarize(QueueList queueList) {

   // get unique short names

      HashSet set = new HashSet();

      Iterator i = queueList.queues.iterator();
      while (i.hasNext()) {
         Queue q = (Queue) i.next();

         // don't report on manual integration types
         if (    q.format == Order.FORMAT_FLAT
              || q.format == Order.FORMAT_TREE ) continue;

         String name;
         try {
            name = Format.getFormat(q.format).getShortName();
         } catch (Exception e) {
            continue; // won't happen, but handle plausibly
         }

         // the mapping from format codes to short names is not one-to-one;
         // in particular all the Fuji formats map to "Fuji".
         // I think that's reasonable, but maybe we'll adjust it later.

         set.add(name);
      }

   // alphabetize

      LinkedList list = new LinkedList();
      list.addAll(set);
      Collections.sort(list);

   // format as string

      StringBuffer buffer = new StringBuffer();

      i = list.iterator();
      while (i.hasNext()) {
         if (buffer.length() > 0) buffer.append(',');
         buffer.append((String) i.next());
      }
      // the if-condition misfires if the first name
      // is empty string ... but that doesn't happen.

      return buffer.toString();
   }

// --- status file ---

   private static class StatusInfo {
      public int locationID;
      public boolean isWholesale;
   }
   // maybe more later, so make a struct now to simplify

   private static StatusInfo getStatusInfo(Config config) {
      StatusInfo statusInfo = new StatusInfo();

      statusInfo.locationID  = config.merchantConfig.merchant;
      statusInfo.isWholesale = config.merchantConfig.isWholesale;

      return statusInfo;
   }

   public static class StatusBlock extends Structure {

      public int locationID;
      public String appVersion;
      public String initStatus; // date, but in a custom format
      public String lastStatus;

      public static final StructureDefinition sd = new StructureDefinition(

         StatusBlock.class,
         // no version
         new AbstractField[] {

            new IntegerField("locationID","LocationID"),
            new StringField("appVersion","AppVersion"),
            new StringField("initStatus","InitStatus"),
            new StringField("lastStatus","LastStatus")
         });

      protected StructureDefinition sd() { return sd; }

      public void validate() throws ValidationException {
      }
   }

   private static final String NAME_BLOCK = "Status";

   private static void updateStatusFile(File statusFile, StatusInfo statusInfo) throws Exception {
      if (statusInfo.isWholesale) return; // no status file format defined yet

   // generate timestamp

      SimpleDateFormat dateFormat = new SimpleDateFormat(Text.get(StatusNews.class,"f2"));
      dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

      String timestamp = dateFormat.format(new Date());
      // not the same date as in the status report, but that's fine,
      // it could be a while later now anyway because we do retries

   // read

      // double construction of alternating file, not great
      StatusBlock block = new StatusBlock();
      AlternatingFile af = new AlternatingFile(statusFile);
      if (af.exists()) {
         AlternatingFile.load(statusFile,block,NAME_BLOCK);
      } else {
         block.initStatus = timestamp;
      }

   // update

      block.locationID = statusInfo.locationID;
      block.appVersion = AboutDialog.getVersion();
      block.lastStatus = timestamp;

      // if we want to add fields about the last order received (ID, timestamp),
      // it could work like kbpsLast.  just be sure that if we haven't received
      // an order yet, we leave the fields alone instead of overwriting to null

   // write

      AlternatingFile.store(statusFile,block,NAME_BLOCK);
   }

// --- HTTP transactions ---

   private static class StatusTransaction extends PostTransaction {

      private String url;
      private Query data;
      private Level logLevel;

      public StatusTransaction(String url, Query data, Level logLevel) {
         this.url = url;
         this.data = data;
         this.logLevel = logLevel;
      }

      public String describe() { return Text.get(StatusNews.class,"s2"); }
      protected String getFixedURL() { return url; }

      protected void getParameters(Query query) throws IOException {
         query.add(data);
         // the MLRFNBR, password, and time stamp are true parameters,
         // not data, but it's convenient to handle them the same way.
      }

      protected boolean receive(InputStream inputStream) throws Exception {

         // there's no standard here for what we should receive,
         // but currently it's a rehash of what we sent.
         // or, if there's an error, who knows -- that's why we
         // want some logging.

         Level useLevel = Level.FINER;
         // reserve FINE for things that are intermittent, good advice I thought of earlier

         if (logLevel.intValue() <= useLevel.intValue()) { // easier than traversing Logger
            String text = getText(inputStream);
            Log.log(useLevel,StatusNews.class,"i1",new Object[] { text });
         }
         // else just totally ignore the response

         return true;
      }
   }

   // same as AutoUpdate.DownloadVersion except no size check
   //
   private static class NewsTransaction extends DownloadTransaction {

      private String url;
      private File file;

      public NewsTransaction(String url, File file) {
         super(/* overwrite = */ true);

         this.url = url;
         this.file = file;
      }

      public String describe() { return Text.get(StatusNews.class,"s3"); }
      protected String getFixedURL() { return url; }
      protected File getFile() { return file; }
   }

}

