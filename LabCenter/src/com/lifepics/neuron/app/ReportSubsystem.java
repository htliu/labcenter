/*
 * ReportSubsystem.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.core.ReportQueue;
import com.lifepics.neuron.net.DiagnoseConfig;
import com.lifepics.neuron.net.MerchantConfig;
import com.lifepics.neuron.object.Copyable;
import com.lifepics.neuron.thread.StoppableThread;
import com.lifepics.neuron.thread.Subsystem;

/**
 * The report subsystem, that sends log information to the server.
 */

public class ReportSubsystem extends Subsystem {

// --- config class ---

   public static class Config implements Copyable {

      public ReportQueue reportQueue;
      public String reportURL;
      public long idlePollInterval;
      public MerchantConfig merchantConfig;
      public DiagnoseConfig diagnoseConfig;

      public Object clone() throws CloneNotSupportedException {
         Config c = (Config) super.clone();

         c.merchantConfig = merchantConfig.copy(); // don't share
         c.diagnoseConfig = diagnoseConfig.copy();

         return c;
      }

      public boolean equals(Object o) {
         if ( ! (o instanceof Config) ) return false;
         Config c = (Config) o;

         return (    reportQueue == c.reportQueue
                  && reportURL.equals(c.reportURL)
                  && idlePollInterval == c.idlePollInterval
                  && merchantConfig.equals(c.merchantConfig)
                  && diagnoseConfig.equals(c.diagnoseConfig) );
      }
   }

// --- construction ---

   public ReportSubsystem(Copyable config) {
      super(config);
   }

// --- subclass hook ---

   protected StoppableThread makeThread() {
      Config c = (Config) this.config;
      return new ReportThread(c.reportQueue,
                              c.reportURL,
                              c.idlePollInterval,
                              makeThreadStatus(),
                              c.merchantConfig,
                              c.diagnoseConfig);
   }

}

