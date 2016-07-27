/*
 * RollRetrySubsystem.java
 */

package com.lifepics.neuron.axon;

import com.lifepics.neuron.object.Copyable;
import com.lifepics.neuron.table.Table;
import com.lifepics.neuron.thread.StoppableThread;
import com.lifepics.neuron.thread.Subsystem;

/**
 * The roll retry subsystem.
 */

public class RollRetrySubsystem extends Subsystem {

// --- config class ---

   public static class Config implements Copyable {

      public Table table;
      public long scanInterval;
      public long pauseRetryInterval;

      public Object clone() throws CloneNotSupportedException { return super.clone(); }

      public boolean equals(Object o) {
         if ( ! (o instanceof Config) ) return false;
         Config c = (Config) o;

         return (    table == c.table
                  && scanInterval == c.scanInterval
                  && pauseRetryInterval == c.pauseRetryInterval );
      }
   }

// --- construction ---

   public RollRetrySubsystem(Copyable config) {
      super(config);
   }

// --- subclass hook ---

   protected StoppableThread makeThread() {
      Config c = (Config) this.config;
      return new RollRetryThread(c.table,
                                 c.scanInterval,
                                 makeThreadStatus(),
                                 c.pauseRetryInterval);
   }

}

