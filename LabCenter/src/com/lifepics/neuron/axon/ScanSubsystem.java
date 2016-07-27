/*
 * ScanSubsystem.java
 */

package com.lifepics.neuron.axon;

import com.lifepics.neuron.object.Copyable;
import com.lifepics.neuron.table.Table;
import com.lifepics.neuron.thread.StoppableThread;
import com.lifepics.neuron.thread.Subsystem;

/**
 * The scan subsystem.
 */

public class ScanSubsystem extends Subsystem {

// --- config class ---

   public static class Config implements Copyable {

      public Table table;
      public long idlePollInterval;
      public long scannerPollInterval;
      public ScanConfigDLS configDLS;
      public ScanThread.Callback callback;
      public RollManager rollManager;

      public Object clone() throws CloneNotSupportedException {
         Config c = (Config) super.clone();

         c.configDLS = configDLS.copy();

         return c;
      }

      public boolean equals(Object o) {
         if ( ! (o instanceof Config) ) return false;
         Config c = (Config) o;

         return (    table == c.table
                  && idlePollInterval == c.idlePollInterval
                  && scannerPollInterval == c.scannerPollInterval
                  && configDLS.equals(c.configDLS)
                  && callback == c.callback
                  && rollManager == c.rollManager );
      }
   }

// --- construction ---

   public ScanSubsystem(Copyable config) {
      super(config);
   }

// --- subclass hook ---

   protected StoppableThread makeThread() {
      Config c = (Config) this.config;
      return new ScanThread(c.table,
                            c.idlePollInterval,
                            makeThreadStatus(),
                            c.scannerPollInterval,
                            c.configDLS,
                            c.callback,
                            c.rollManager);
   }

}

