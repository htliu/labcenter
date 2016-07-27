/*
 * Log.java
 */

package com.lifepics.neuron.core;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A utility class for logging.<p>
 *
 * Localized text is obtained just as in class {@link Text}.
 * When you want to obtain a piece of localized text,
 * you must specify which class the text is associated with.
 * This can be done either by naming the class object
 * or by providing an instance of the class,
 * which you can often do conveniently using "this".
 * The text is then retrieved from the file Text.properties
 * in the package associated with that class.
 * The properties file must be stored in the classpath.<p>
 *
 * The name of the package associated with the class
 * is also used as the name of the logger in the call to
 * {@link Logger#getLogger(String) Logger.getLogger}.
 */

public class Log {

   // the LogRecord class supports deferred localization,
   // but it's not convenient to use that feature here.
   // basically, a lot of our messages come from exceptions,
   // which are necessarily localized before being thrown,
   // and I want to keep things as simple and uniform as possible.

// --- original log functions ---

   /**
    * Log a fixed piece of localized text.
    *
    * @param level The log level.
    * @param o An object that specifies which class the text is associated with.
    *          The object can be the class object or an instance of the desired class.
    * @param key The key used to look up the text in the properties file.
    *            The final part of the class name is applied as a prefix.
    */
   public static void log(Level level, Object o, String key) {
      logImpl(null,level,o,Text.get(o,key),null,level);
   }

   /**
    * Log localized text with arguments substituted in.
    *
    * @param level The log level.
    * @param o An object that specifies which class the text is associated with.
    *          The object can be the class object or an instance of the desired class.
    * @param key The key used to look up the text in the properties file.
    *            The final part of the class name is applied as a prefix.
    * @param args The arguments to be substituted.
    *             The syntax "new Object[] { ... }" is particularly useful.
    */
   public static void log(Level level, Object o, String key, Object[] args) {
      logImpl(null,level,o,Text.get(o,key,args),null,level);
   }

   /**
    * Log a fixed piece of localized text, plus an exception.
    *
    * @param level The log level.
    * @param o An object that specifies which class the text is associated with.
    *          The object can be the class object or an instance of the desired class.
    * @param key The key used to look up the text in the properties file.
    *            The final part of the class name is applied as a prefix.
    * @param t The exception.
    */
   public static void log(Level level, Object o, String key, Throwable t) {
      logImpl(null,level,o,ChainedException.format(Text.get(o,key),t),t,level);
   }

   /**
    * Log localized text with arguments substituted in, plus an exception.
    *
    * @param level The log level.
    * @param o An object that specifies which class the text is associated with.
    *          The object can be the class object or an instance of the desired class.
    * @param key The key used to look up the text in the properties file.
    *            The final part of the class name is applied as a prefix.
    * @param args The arguments to be substituted.
    *             The syntax "new Object[] { ... }" is particularly useful.
    * @param t The exception.
    */
   public static void log(Level level, Object o, String key, Object[] args, Throwable t) {
      logImpl(null,level,o,ChainedException.format(Text.get(o,key,args),t),t,level);
   }

// --- constants ---

   // values for getReportType
   public static final String ROLL  = "R";
   public static final String ORDER = "O";
   public static final String JOB   = "J";

// --- new log functions ---

   public static void log(Reportable r, Level level, Object o, String key) {
      logImpl(r,level,o,Text.get(o,key),null,level);
   }

   public static void log(Reportable r, Level level, Object o, String key, Object[] args) {
      logImpl(r,level,o,Text.get(o,key,args),null,level);
   }

   public static void log(Reportable r, Level level, Object o, String key, Throwable t) {
      logImpl(r,level,o,ChainedException.format(Text.get(o,key),t),t,level);
   }

   public static void log(Reportable r, Level level, Object o, String key, Object[] args, Throwable t) {
      logImpl(r,level,o,ChainedException.format(Text.get(o,key,args),t),t,level);
   }

   public static void log(Reportable r, Level level, Object o, String key, Object[] args, Throwable t, Level reportAsLevel) {
      logImpl(r,level,o,ChainedException.format(Text.get(o,key,args),t),t,reportAsLevel);
   }

// --- implementation ---

   /**
    * Get a logger from an object that specifies a class.
    */
   public static Logger getLogger(Object o) {
      Class c = (o instanceof Class) ? (Class) o : o.getClass();
      Package p = c.getPackage();

      return (p == null) ? Logger.global : Logger.getLogger(p.getName());
   }

   private static void logImpl(Reportable reportable, Level level, Object o, String message, Throwable t, Level reportAsLevel) {
      getLogger(o).log(level,message,t); // ok if t is null

      // it would be nice to have the exact same timestamp on all 3 records,
      // but there's no easy way to do it ... I could create a LogRecord here,
      // but I don't have any clean way to reproduce the code in Logger.doLog.

      long timestamp = System.currentTimeMillis();

      // use reportAsLevel here instead of level so that the "error resolved"
      // informational messages will get sent if the errors are getting sent.
      //
      if (    reportInterface != null
           && reportLevel != null
           && reportAsLevel.intValue() >= reportLevel.intValue() ) {

         ReportRecord r = new ReportRecord();
         r.timestamp = timestamp;
         r.level = level;
         r.message = message;
         r.t = t;
         r.idType   = (reportable != null) ? reportable.getReportType()     : null;
         r.id       = (reportable != null) ? reportable.getReportID()       : null;
         r.merchant = (reportable != null) ? reportable.getReportMerchant() : null;

         // instead of copying all the fields, we could just send along
         // the reportable, but I like isolating the reportable objects
         // here so that the report thread doesn't see them.

         reportInterface.report(r);
      }

      // don't use reportAsLevel here, since we don't want to report resolution
      //
      if (    localReportInterface != null
           && level.intValue() >= Level.SEVERE.intValue() // hard-coded, errors only
           && reportable != null
           && reportable.getLocalOrderSeq() != null ) {   // better test comes later

         localReportInterface.report(reportable,timestamp,level,message,t);
      }

      // kind of obvious, but ... the first three reportable fields are used to fill in
      // the report, but the second three fields are used not just for that but also to
      // determine whether or not to send a report at all.
   }

   private static Level reportLevel;
   private static ReportInterface reportInterface;

   public static void setReportInterface(Level a_reportLevel, ReportInterface a_reportInterface) {
      reportLevel = a_reportLevel;
      reportInterface = a_reportInterface;
   }

   private static LocalReportInterface localReportInterface;

   public static void setLocalReportInterface(LocalReportInterface a_localReportInterface) {
      localReportInterface = a_localReportInterface;
   }

}

