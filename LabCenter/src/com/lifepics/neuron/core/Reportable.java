/*
 * Reportable.java
 */

package com.lifepics.neuron.core;

/**
 * An interface for objects that can be the object of a report.
 */

public interface Reportable {

   /**
    * Get a type from the type enumeration in Log.
    */
   String getReportType();

   /**
    * Get the identifier to use in error reports.
    */
   String getReportID();

   /**
    * Get a merchant ID to use in wholesaler error reports, or null.
    */
   String getReportMerchant();

   /**
    * Get the order sequence to use in local error reports.
    */
   String getLocalOrderSeq();

   /**
    * Get the order ID to use in local error reports.
    */
   int getLocalOrderID();

   /**
    * Get the status code to use in local error reports.
    * The code is a default code that can be overridden.
    */
   int getLocalStatusCode();

   // I thought about putting the local functions into a new interface LocalReportable,
   // then testing for that, but there's not much benefit, because local vs. non-local
   // is determined by data, not just by class.  a bit cleaner for rolls, is all.

}

