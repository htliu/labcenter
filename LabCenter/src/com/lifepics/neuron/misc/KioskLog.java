/*
 * KioskLog.java
 */

package com.lifepics.neuron.misc;

import com.lifepics.neuron.core.ChainedException;
import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;

import java.io.File;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A utility class for logging information about kiosk images.
 * It's like {@link com.lifepics.neuron.core.Log}
 * except that it doesn't notify the server or the local report interface.
 * It's like {@link AppUtil} because it knows how to set up the logger.
 * If you call any of the log functions, a kiosk log file will be created
 * even if kiosk isn't enabled, so be careful!
 */

public class KioskLog {

   // this seems like a good place to explain the theory of kiosk logging.
   //
   // first, lines should always start with a code and an imageID,
   // so that the logs can be parsed using String.split(" ",5)
   // (with an extra test to remove a colon from the imageID part).
   // so, they look like this.
   // I think the rollID will only need to go on the create line.
   //
   // 0730 1811 C 123123123 79315
   // 0730 1812 U 123123123
   // 0730 1813 E 123123123: Connection refused: connect
   // 0730 1814 P 123123123
   //
   // second, the transformation is that existing code like this
   //
   // Log.log(...);
   //
   // becomes like this
   //
   // if (roll.source == Roll.SOURCE_LOCAL) {
   //    KioskLog.log(this,"i#",new Object[] { roll.getLocalImageID() });
   // } else {
   //    Log.log(...);
   // }
   //
   // so, we're not logging to two places, we're redirecting existing log lines
   // to a new file.
   //
   // the idea is that the kiosk log files are larger, more compact per line,
   // and only contain lines for important transitions, so that we'll have
   // a nice long history even though the kiosks can churn through lots of images
   // pretty quickly.  another goal is to remove the lines for normal kiosk image
   // events from the main log files, so they won't be full of useless stuff.
   //
   // so, when thinking about what log lines to transform or add, ask yourself ...
   // 1. is this part of the normal process for kiosk images?
   // 2. is this an important transition that could happen in abnormal cases?
   //
   // I'm not sure what to do with errors in the normal process, so for now I'll
   // keep those in the main logs.

// --- log functions ---

   // level is always info here BTW

   public static void log(Object o, String key) {
      logImpl(Text.get(o,key));
   }

   public static void log(Object o, String key, Object[] args) {
      logImpl(Text.get(o,key,args));
   }

   public static void log(Object o, String key, Throwable t) {
      logImpl(ChainedException.format(Text.get(o,key),t));
   }

   public static void log(Object o, String key, Object[] args, Throwable t) {
      logImpl(ChainedException.format(Text.get(o,key,args),t));
   }

// --- implementation ---

   private static void logImpl(String message) {
      if ( ! inited ) init();
      if ( ! active ) return;
      logger.log(Level.INFO,message,(Throwable) null);
   }

// --- constants ---

   private static final String LOG_PATTERN = "log/kiosk%g.txt";

// --- static state ---

   private static File mainDir;
   private static int logCount;
   private static int logSize;

   private static boolean inited;
   private static boolean active;
   //
   // there are three states of pair (inited,active):
   //
   // (f,f) which is the state we start in
   // (t,t) which we switch to when the first kiosk logging is requested
   // (t,f) if we can't construct the handler (in which case, no logging)

   private static Logger logger;
   private static FileHandler handler;

// --- setup functions ---

   public static void init(File a_mainDir, int a_logCount, int a_logSize) {
      mainDir  = a_mainDir;
      logCount = a_logCount;
      logSize  = a_logSize;
   }

   private static void init() { // the real init
      inited = true;

   // the part that can fail

      File logPattern = new File(mainDir,LOG_PATTERN);
      try {
         handler = new FileHandler(Convert.fromFile(logPattern),logSize,logCount);
      } catch (Throwable t) {
         return; // leave active false
      }

   // set it up

      logger = Logger.getAnonymousLogger();

      // stop console logging
      logger.setUseParentHandlers(false);

      handler.setFormatter(new KioskFormatter());
      logger.addHandler(handler);
      logger.setLevel(Level.INFO);

   // done

      active = true;
   }

   public static void exit() {
      if (logger != null && handler != null) {
         logger.removeHandler(handler);
         handler.close();
      }
   }

}

