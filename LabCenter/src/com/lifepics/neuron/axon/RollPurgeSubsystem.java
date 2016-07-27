/*
 * RollPurgeSubsystem.java
 */

package com.lifepics.neuron.axon;

import com.lifepics.neuron.net.DiagnoseConfig;
import com.lifepics.neuron.object.Copyable;
import com.lifepics.neuron.table.Table;
import com.lifepics.neuron.thread.StoppableThread;
import com.lifepics.neuron.thread.Subsystem;

/**
 * The roll purge subsystem.
 */

public class RollPurgeSubsystem extends Subsystem {

// --- config class ---

   public static class Config implements Copyable {

      public Table table;
      public long scanInterval;
      public RollManager rollManager;
      public DiagnoseConfig diagnoseConfig;

      public Object clone() throws CloneNotSupportedException {
         Config c = (Config) super.clone();

         c.diagnoseConfig = diagnoseConfig.copy();

         return c;
      }

      public boolean equals(Object o) {
         if ( ! (o instanceof Config) ) return false;
         Config c = (Config) o;

         return (    table == c.table
                  && scanInterval == c.scanInterval
                  && rollManager == c.rollManager
                  && diagnoseConfig.equals(c.diagnoseConfig) );
      }
   }

// --- construction ---

   public RollPurgeSubsystem(Copyable config) {
      super(config);
   }

// --- subclass hook ---

   protected StoppableThread makeThread() {
      Config c = (Config) this.config;
      return new RollPurgeThread(c.table,
                                 c.scanInterval,
                                 makeThreadStatus(),
                                 c.rollManager,
                                 c.diagnoseConfig);
   }

}

