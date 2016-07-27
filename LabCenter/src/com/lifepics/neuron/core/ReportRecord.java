/*
 * ReportRecord.java
 */

package com.lifepics.neuron.core;

import java.util.logging.Level;

/**
 * Like {@link java.util.logging.LogRecord}, but with more fields; used for reporting to the server.
 */

public class ReportRecord {

   public long timestamp;
   public Level level;
   public String message;
   public Throwable t;   // for stack trace only, message is already included above; can be null
   public String idType; // can be null
   public String id;     // can be null
   public String merchant; // null unless wholesaler

}

