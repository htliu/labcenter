/*
 * LineFormatter.java
 */

package com.lifepics.neuron.misc;

import com.lifepics.neuron.object.XML;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * A formatter cloned from SimpleFormatter that doesn't put the date on a separate line.
 */

public class LineFormatter extends Formatter {

   private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
   private static String lineSeparator = System.getProperty("line.separator");

   // these change, so they're not static
   private Date date = new Date();
   private FieldPosition fieldPosition = new FieldPosition(0);

   /**
    * The regular entry point for logging.
    */
   public synchronized String format(LogRecord record) {
      return format(record.getMillis(),record.getLevel(),formatMessage(record),record.getThrown());
   }

   /**
    * An alternate entry point so that we can write the same format
    * for local status even though we don't always have a LogRecord.
    */
   public synchronized String format(long timestamp, Level level, String message, Throwable t) {
      StringBuffer sb = new StringBuffer();

      date.setTime(timestamp); // save a memory allocation

      dateFormat.format(date,sb,fieldPosition); // null position is bad
      sb.append("  ");
      sb.append(level.getLocalizedName());
      sb.append(": ");
      sb.append(XML.replaceAll(message,new String[] { "\r", "\n" },new String[] { "", lineSeparator })); // (*)
      sb.append(lineSeparator);

      // (*) within LabCenter, CRLF is never used, only plain LF.
      // translation happens when we write out to files and APIs.
      // for example, if an error message with LF gets stored in
      // an object, it becomes CRLF in the file, did you know that?
      //
      // the initial step of replacing CR with the empty string
      // is just to fix cases where we include external data
      // (like a HTTP error message body) that may or may not have
      // the line separator we expect.  these should be rare,
      // so replacing them normally only costs us a string scan.

      String trace = getStackTrace(t);
      if (trace != null) {
         sb.append(lineSeparator);
         sb.append(trace);
         sb.append(lineSeparator);
      }

      return sb.toString();
   }

   /**
    * As a convenience, null produces null.
    */
   public static String getStackTrace(Throwable t) {
      if (t == null) return null;
      try {
         StringWriter sw = new StringWriter();
         PrintWriter pw = new PrintWriter(sw);
         t.printStackTrace(pw);
         pw.close();
         return sw.toString();
      } catch (Exception e) {
         return null;
      }
   }

}

