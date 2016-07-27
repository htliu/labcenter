/*
 * CompletionSubsystem.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.object.Copyable;
import com.lifepics.neuron.table.Table;
import com.lifepics.neuron.thread.StoppableThread;
import com.lifepics.neuron.thread.Subsystem;

/**
 * The completion subsystem.
 */

public class CompletionSubsystem extends Subsystem {

// --- config class ---

   public static class Config implements Copyable {

      public Table jobTable;
      public long scanInterval;
      public JobManager jobManager;

      public Object clone() throws CloneNotSupportedException { return super.clone(); }

      public boolean equals(Object o) {
         if ( ! (o instanceof Config) ) return false;
         Config c = (Config) o;

         return (    jobTable == c.jobTable
                  && scanInterval == c.scanInterval
                  && jobManager == c.jobManager );
      }
   }

// --- construction ---

   public CompletionSubsystem(Copyable config) {
      super(config);
   }

// --- subclass hook ---

   protected StoppableThread makeThread() {
      Config c = (Config) this.config;
      return new CompletionThread(c.jobTable,
                                  c.scanInterval,
                                  makeThreadStatus(),
                                  c.jobManager);
   }

}

