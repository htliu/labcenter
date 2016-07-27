/*
 * DiagnoseConfig.java
 */

package com.lifepics.neuron.net;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.object.CopyUtil;
import com.lifepics.neuron.struct.*;

import java.util.LinkedList;

/**
 * An object that holds configuration information for the diagnose subsystem.
 */

public class DiagnoseConfig extends Structure {

// --- fields ---

   // these are higher-level

   public boolean pauseRetry;
   public long pauseRetryInterval; // millis
   public long pauseRetryLimit;    // millis; roughly, interval * retry count

   // these are for DiagnoseHandler

   public long downPollInterval; // millis; retry interval would be a better name
   public int downRetriesBeforeNotification;
   public int downRetriesEvenIfNotDown;

   // these are for Diagnose

   /**
    * A list of URLs to test to determine whether the main site is up.
    * For the site to be up, all URLs must respond.
    */
   public LinkedList siteURLs;

   /**
    * A list of URLs to test to determine whether the network is up.
    * For the network to be up, at least one URL must respond.
    */
   public LinkedList netURLs;

   /**
    * The number of network URLs that will be tested before failure is reported.
    * If the list contains more elements, the URLs will be selected at random.
    */
   public int netFailLimit;

   /**
    * The timeout interval per URL, in milliseconds.
    */
   public int timeoutInterval;

   // these are for RetryHandler

   public long startupRetryInterval; // millis
   public int startupRetries;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      DiagnoseConfig.class,
      0,2,
      new AbstractField[] {

         new BooleanField("pauseRetry","PauseRetry",1,true),
         new LongField("pauseRetryInterval","PauseRetryInterval-Millis",1,3600000), // 1 hour
         new LongField("pauseRetryLimit","PauseRetryLimit-Millis",1,604800000),  // 1 week

         new LongField("downPollInterval","DownPollInterval-Millis"),
         new IntegerField("downRetriesBeforeNotification","DownRetriesBeforeNotification"),
         new IntegerField("downRetriesEvenIfNotDown","DownRetriesEvenIfNotDown"),

         new StringListField("siteURLs","SiteURLs","URL"),
         new StringListField("netURLs","NetURLs","URL"),
         new IntegerField("netFailLimit","NetFailLimit"),
         new IntegerField("timeoutInterval","TimeoutInterval-Millis"),

         new LongField("startupRetryInterval","StartupRetryInterval-Millis",2,5000),
         new IntegerField("startupRetries","StartupRetries",2,2)
      });

   protected StructureDefinition sd() { return sd; }

// --- copy function ---

   public DiagnoseConfig copy() { return (DiagnoseConfig) CopyUtil.copy(this); }

// --- validation ---

   public void validate() throws ValidationException {

      if (pauseRetryInterval < 1) throw new ValidationException(Text.get(this,"e6"));
      if (pauseRetryLimit < 1) throw new ValidationException(Text.get(this,"e7"));

      if (downPollInterval < 1) throw new ValidationException(Text.get(this,"e1"));
      if (downRetriesBeforeNotification < 0) throw new ValidationException(Text.get(this,"e2"));
      if (downRetriesEvenIfNotDown < 0) throw new ValidationException(Text.get(this,"e3"));

      if (netFailLimit < 1) throw new ValidationException(Text.get(this,"e4"));
      if (timeoutInterval < 1) throw new ValidationException(Text.get(this,"e5"));

      if (startupRetryInterval < 1) throw new ValidationException(Text.get(this,"e8"));
      if (startupRetries < 0) throw new ValidationException(Text.get(this,"e9"));
   }

// --- persistence ---

   // I didn't expect to be constructing DiagnoseConfig objects from scratch,
   // but now for the install package I need to.

   public Object loadDefault(int downRetriesBeforeNotification, int downRetriesEvenIfNotDown) {
      loadDefault();

      // when we call loadDefault on an atomic field without a defined default value,
      // we get the Java default, but we're not supposed rely on that.  so,
      // set all such fields.  I'll still count on the URL lists being empty, though.

      pauseRetry = false; // this does have a default, it's just wrong

      downPollInterval = 60000;
      this.downRetriesBeforeNotification = downRetriesBeforeNotification;
      this.downRetriesEvenIfNotDown = downRetriesEvenIfNotDown;

      // since we're leaving the URL lists empty, Diagnose will always return
      // RESULT_NET_UP_SITE_UP and the following two parameters are irrelevant.
      // still, it's nice to give them some values that would pass validation.
      //
      netFailLimit = 1;
      timeoutInterval = 1000;

      // the startup fields do have actual defaults, no need to override them

      return this; // convenience
   }

}

