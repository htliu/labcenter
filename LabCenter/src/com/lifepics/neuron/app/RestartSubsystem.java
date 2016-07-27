/*
 * RestartSubsystem.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.net.DiagnoseConfig;
import com.lifepics.neuron.net.MerchantConfig;
import com.lifepics.neuron.object.Copyable;
import com.lifepics.neuron.thread.StoppableThread;
import com.lifepics.neuron.thread.Subsystem;

import java.io.File;

/**
 * A subsystem that checks for new versions, downloads them
 * (when present), and then makes the application restart.
 */

public class RestartSubsystem extends Subsystem {

// --- config class ---

   public static class Config implements Copyable {

      public File baseDir;
      public File mainDir;
      public AutoUpdateConfig autoUpdateConfig;
      public MerchantConfig merchantConfig;
      public DiagnoseConfig diagnoseConfig;
      public RestartThread.Reconfig reconfig;

      public Object clone() throws CloneNotSupportedException {
         Config c = (Config) super.clone();

         c.autoUpdateConfig = autoUpdateConfig.copy();
         c.merchantConfig = merchantConfig.copy();
         c.diagnoseConfig = diagnoseConfig.copy();

         return c;
      }

      public boolean equals(Object o) {
         if ( ! (o instanceof Config) ) return false;
         Config c = (Config) o;

         return (    baseDir.equals(c.baseDir)
                  && mainDir.equals(c.mainDir)
                  && autoUpdateConfig.equals(c.autoUpdateConfig)
                  && merchantConfig.equals(c.merchantConfig)
                  && diagnoseConfig.equals(c.diagnoseConfig)
                  && reconfig == c.reconfig );
      }
   }

// --- construction ---

   public RestartSubsystem(Copyable config) {
      super(config);
   }

// --- subclass hook ---

   protected StoppableThread makeThread() {
      Config c = (Config) this.config;
      return new RestartThread(c.baseDir,
                               c.mainDir,
                               c.autoUpdateConfig,
                               c.merchantConfig,
                               c.diagnoseConfig,
                               c.reconfig,
                               makeThreadStatus());
   }

}

