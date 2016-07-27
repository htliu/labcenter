/*
 * RestartThread.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.core.Log;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.net.DefaultHandler;
import com.lifepics.neuron.net.DiagnoseConfig;
import com.lifepics.neuron.net.DiagnoseHandler;
import com.lifepics.neuron.net.MerchantConfig;
import com.lifepics.neuron.net.PauseAdapter;
import com.lifepics.neuron.thread.StoppableThread;
import com.lifepics.neuron.thread.ThreadStatus;

import java.io.File;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.logging.Level;

/**
 * A thread that checks for new versions, downloads them
 * (when present), and then makes the application restart.
 */

public class RestartThread extends StoppableThread {

// --- fields ---

   private File baseDir;
   private File mainDir;
   private AutoUpdateConfig autoUpdateConfig;
   private MerchantConfig merchantConfig;
   private Reconfig reconfig;
   private ThreadStatus threadStatus;

   private DiagnoseHandler handler;
   private PauseAdapter pauseAdapter;

// --- construction ---

   public RestartThread(File baseDir, File mainDir, AutoUpdateConfig autoUpdateConfig,
                        MerchantConfig merchantConfig, DiagnoseConfig diagnoseConfig, Reconfig reconfig, ThreadStatus threadStatus) {
      super(Text.get(RestartThread.class,"s1"));

      this.baseDir = baseDir;
      this.mainDir = mainDir;
      this.autoUpdateConfig = autoUpdateConfig;
      this.merchantConfig = merchantConfig;
      this.reconfig = reconfig;
      this.threadStatus = threadStatus;

      handler = new DiagnoseHandler(new DefaultHandler(),diagnoseConfig);
      // no describe handler here
      pauseAdapter = new PauseAdapter(threadStatus);
   }

// --- interface ---

   // as with the AutoConfig code, define an interface
   // that holds the functions we need to perform,
   // and let the caller worry about how to make it happen.

   // any call to pullConfig *must* be followed by a done;
   // i.e., the protocol is  pullConfig [pushConfig] done.
   // (ignoring the server calls)

   public interface Reconfig extends AutoConfig.PushConfig {

      Config pullConfig();
      Config pullServer() throws Exception;

      /**
       * Let caller run whatever cleanup is necessary.
       */
      void done();
   }

// --- interface for thread fields ---

   protected void doInit() throws Exception {
   }

   protected void doRun() throws Exception {
      try {

         while ( ! isStopping() ) {

            sleepNice(getWaitInterval(autoUpdateConfig.restartHour1,autoUpdateConfig.restartHour2));
            if ( ! isStopping() ) { // always check after sleeping

               // time to check for an update
               try {

                  Log.log(Level.INFO,this,"i1");

                  boolean autoConfig = true;
                  LinkedList softwareList = new LinkedList();
                  Config server = null;
                  AutoUpdate.InstanceRecord ir = new AutoUpdate.InstanceRecord();

                  try {
                     server = reconfig.pullServer();
                     if (server != null) {
                        ir.instanceID = server.autoUpdateConfig.instanceID;
                        ir.passcode   = server.autoUpdateConfig.passcode;
                     }
                     // else just leave all three null, auto-update will allocate
                  } catch (Exception e) {
                     // server file is corrupt, do not auto-config (which would overwrite)
                     autoConfig = false;
                     Log.log(Level.SEVERE,this,"e2",e); // call for help
                  }

                  String result = AutoUpdate.preload(baseDir,autoUpdateConfig,merchantConfig,ir,softwareList,handler,pauseAdapter);

                  boolean isNewVersion = (result != null);
                  if (isNewVersion) {
                     Log.log(Level.INFO,this,"i2",new Object[] { result });
                  }

                  if (softwareList.isEmpty()) autoConfig = false; // empty list means server read failed
                  if (autoConfig) {
                     try {

                        Config config = reconfig.pullConfig(); // lock the config object

                        boolean restart = AutoConfig.execute(config,server,ir,softwareList,reconfig,handler,pauseAdapter,baseDir,mainDir);
                        // here we can pass in both files without copying

                        if (restart) {
                           Log.log(Level.INFO,this,"i3");
                           if (result == null) result = autoUpdateConfig.lc.currentVersion;
                        }

                     } finally {
                        reconfig.done(); // unlock
                     }
                     // there are no checked exceptions, but I like having a finally clause anyway
                  }

                  // there's a tiny loophole here.  we don't enter the new version
                  // into the config file right away, so if we fail to connect to
                  // the server after restart, LC won't know it's supposed to own it.
                  // or, rather, if we fail to connect *and* the desired version
                  // changes before we successfully restart into the downloaded file.

                  Log.log(Level.INFO,this,(result == null) ? "i4" : "i5");

                  if (result != null) RestartDialog.activate(result,(int) autoUpdateConfig.restartWaitInterval,isNewVersion);
                  // the int cast is OK because we validated for that.
                  // we have to go into the UI thread to run the dialog, so just stay over there.

               } catch (Exception e) {
                  // this thread / subsystem is not monitored in the UI, so just log the error.
                  // the only things this actually catches are the two in AutoUpdate.preload,
                  // so this can be severe, same as the corresponding ones in AutoUpdate.update.
                  Log.log(Level.SEVERE,this,"e1",e);
               }
               // only exception here is from AutoUpdate.preload
            }
         }

      } catch (Exception e) {
         if ( ! isStopping() ) threadStatus.fatal(e);
         throw e;
      }
      // only exception here is from sleepNice
   }

   protected void doExit() {
   }

   protected void doStop() {
   }

// --- methods ---

   private static long getWaitInterval(int hour1, int hour2) {

      Calendar cNow = Calendar.getInstance();
      int y       = cNow.get(Calendar.YEAR);
      int m       = cNow.get(Calendar.MONTH);
      int dNow    = cNow.get(Calendar.DAY_OF_MONTH);
      int hourNow = cNow.get(Calendar.HOUR_OF_DAY);

      // we could clone cNow and modify it, but this approach is better.
      // it's correct even in weird cases, e.g., if there's a DST offset
      // that's not a round number of hours.
      // note that out-of-range values of day are allowed, e.g. July 32.

      // design note: if we're currently inside the auto-update period,
      // we wait until the next day.  there are various reasons for this.
      //
      //  * technically, it seems cleaner than cutting down the range.
      //  * it prevents the auto-update thread from running immediately
      //    after startup, when we've already checked for a new version.
      //    (this isn't entirely true, we might be right before the period,
      //    but close enough.)
      //  * it prevents a re-check after successful auto-update.
      //    (it doesn't prevent the re-check that's performed at startup,
      //    but that's not fixable here.)

      int d1 = dNow;
      if (hour1 <= hourNow) d1++;

      int d2 = d1;
      if (hour2 <= hour1  ) d2++; // equality is actually excluded by validation

      Calendar c1 = Calendar.getInstance();
      c1.clear(); // millis
      c1.set(y,m,d1,hour1,0,0);

      Calendar c2 = Calendar.getInstance();
      c2.clear(); // millis
      c2.set(y,m,d2,hour2,0,0);

      long tNow = cNow.getTimeInMillis();
      long t1   = c1  .getTimeInMillis();
      long t2   = c2  .getTimeInMillis();

      long base = t1 - tNow;
      if (base < 0) base = 0;
      // any positive number is possible, if we're just before the target hour.
      // I *think* zero and negative numbers are impossible, but maybe there's
      // some weird case I'm not seeing.  so, just prevent that.

      long span = t2 - t1;
      if (span < 3600000) span = 3600000;
      // zero is a real possibility ... on DST day, if hours are set to 2-3 AM.
      // but, it's not a desirable possibility, so prevent it .. negatives too.
      // actually, force a whole hour in all cases, to handle non-round DST offsets.

      return (long) (base + span * Math.random());
      // definitely no overflow ... base and span are both limited to
      // the number of milliseconds in a day, plus a bit more for DST.

      // for full correctness, we ought to ignore the OS time zone and use
      // the LabCenter one instead, but it's not worth messing with.
   }

}

