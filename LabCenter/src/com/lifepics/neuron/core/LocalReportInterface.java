/*
 * LocalReportInterface.java
 */

package com.lifepics.neuron.core;

import java.util.logging.Level;

/**
 * An interface for transferring local error reports.
 */

public interface LocalReportInterface {

   void report(Reportable reportable,
               long timestamp, Level level, String message, Throwable t);

}

