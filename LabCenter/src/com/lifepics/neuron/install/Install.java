/*
 * Install.java
 */

package com.lifepics.neuron.install;

import com.lifepics.neuron.core.Log;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.gui.Style;
import com.lifepics.neuron.gui.User;
import com.lifepics.neuron.misc.AppUtil;
import com.lifepics.neuron.misc.ProcessException;
import com.lifepics.neuron.net.NetUtil;
import com.lifepics.neuron.table.AlternatingFileAutoNumber;
import com.lifepics.neuron.thread.StoppableThread;

import java.io.File;
import java.io.FileReader;
import java.util.logging.Level;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Timer;

/**
 * The main class for the install application.
 */

public class Install extends AppUtil.Stub {

// --- main ---

   public static void main(String[] args) {
      main(args,/* launched = */ false); // ignore result
   }

   public static String main(String[] args, boolean launched) {
      return new Install().execute(args,launched);
   }

// --- main functions ---

   protected String init(String causeKey, String[] args, boolean launched) throws ProcessException {

      // same order as in app.Main, just for consistency.

      AppUtil.initLookAndFeel();
      NetUtil.initNetwork();
      initLogging1();
      initMainDir(args);
      Style.setStyle(Style.getDefaultStyle());
      initLoggingC();
      initLogging2();
      // NetUtil.setDefaultTimeout(n);

      Log.log(Level.INFO,this,causeKey);

      initURL();
      initInstall();

      initFrame();
      initTimer();

      return null;
   }

   protected void run() {
      frame.setVisible(true);
      if (minimizeCommand) {
         User.iconify(frame); // see comments in app/Main.java
      }
      transferReady = true;
   }

   protected void exit(String causeKey) {
      exitTimer();
      exitInstall();
      exitFrame(); // need frame for subsystem exit
      Log.log(Level.INFO,this,causeKey);
      exitLogging();
   }

// --- constants ---

   private static final String URL_FILE = "install.txt";

// --- URL ---

   private String installURL;

   private void initURL() throws ProcessException {
      try {
         FileReader reader = new FileReader(new File(baseDir,URL_FILE));
         installURL = AlternatingFileAutoNumber.readAll(reader,200);
      } catch (Exception e) {
         throw new ProcessException(Text.get(this,"e2"),e);
      }
   }

// --- restart helpers ---

   // the restart (and exit) code needs to run in the UI thread,
   // not the install thread, so we need a UI transfer function.

   private boolean transferReady = false;
   private String  transferVersion;

   private void transferIn(String version) {

      while ( ! transferReady ) {

         StoppableThread thread = (StoppableThread) Thread.currentThread();
         try {
            thread.sleepNice(1000); // hardcoded constant
         } catch (InterruptedException e) {
            // won't happen
         }
         if (thread.isStopping()) return;
      }
      // the trouble here is, if we launch the app when all the files are marked done,
      // the install thread completes before the frame is set to non-null, then
      // exitFrame does nothing, the frame comes up, and the main thread exits leaving
      // a visible frame.
      // even worse would be if the thread completed between when the frame was set
      // and when it was made visible, because then we'd get a NullPointerException.
      //
      // we don't have this problem in LC because all restarts use the restart dialog,
      // and that won't run unless the frame is set.  there's a small window of
      // bad opportunity between when the frame is set and frame.setVisible is called,
      // but then we also don't relaunch programmatically until the timer has elapsed.
      //
      // in practice this isn't a likely problem, but it was annoying me in testing.

      transferVersion = version;
      EventQueue.invokeLater(new Runnable() { public void run() { transferOut(); } });
   }

   private void transferOut() {
      restart(transferVersion);
   }

// --- install ---

   private InstallSubsystem installSubsystem;

   private void initInstall() {

      InstallSubsystem.Config isc = new InstallSubsystem.Config();

      isc.control = new AppUtil.ControlInterface() {
         public void exit() { throw new UnsupportedOperationException(); }
         public void restart(String version) { transferIn(version); }
      };
      // using ControlInterface as a Runnable with a string argument

      isc.baseDir = baseDir;
      isc.mainDir = mainDir;
      isc.installURL = installURL;
      isc.installFile = new File(baseDir,INSTALL_FILE);
      isc.installName = NAME_INSTALL;

      installSubsystem = new InstallSubsystem(isc);
      installSubsystem.init(true);
   }

   private void exitInstall() {
      if (installSubsystem != null) {
         installSubsystem.exit(frame);
      }
   }

// --- frame ---

   private InstallFrame frame;

   private void initFrame() {
      frame = new InstallFrame(this,installSubsystem);
   }

   private void exitFrame() {
      if (frame != null) frame.dispose();
   }

// --- timers ---

   private Timer timer;

   private void initTimer() {
      timer = new Timer(1000,new ActionListener() { public void actionPerformed(ActionEvent e) { installSubsystem.ping(); } });
      timer.start();
   }

   private void exitTimer() {
      if (timer != null) timer.stop();
   }

}

