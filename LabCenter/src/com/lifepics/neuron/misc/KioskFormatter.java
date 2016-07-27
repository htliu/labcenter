/*
 * KioskFormatter.java
 */

package com.lifepics.neuron.misc;

import com.lifepics.neuron.object.XML;

import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * A formatter cloned from LineFormatter that writes compact data
 * so that the kiosk log files will reach as far back as possible.
 */

public class KioskFormatter extends Formatter {

   private SimpleDateFormat dateFormat = new SimpleDateFormat("MMdd HHmm");
   private static String lineSeparator = System.getProperty("line.separator");

   // these change, so they're not static
   private Date date = new Date();
   private FieldPosition fieldPosition = new FieldPosition(0);

   /**
    * The regular entry point for logging.
    */
   public synchronized String format(LogRecord record) {
      StringBuffer sb = new StringBuffer();

      date.setTime(record.getMillis()); // save a memory allocation

      dateFormat.format(date,sb,fieldPosition); // null position is bad
      sb.append(" ");
      sb.append(XML.replaceAll(formatMessage(record),new String[] { "\r", "\n" },new String[] { "", lineSeparator }));
      sb.append(lineSeparator);

      // the throwable part has already been included in the message
      // and is not even sent down here

      return sb.toString();
   }

}

