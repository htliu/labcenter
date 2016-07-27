/*
 * JobPurgeSubsystem.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.object.Copyable;
import com.lifepics.neuron.table.Table;
import com.lifepics.neuron.thread.StoppableThread;
import com.lifepics.neuron.thread.Subsystem;

/**
 * The job purge subsystem.
 */

public class JobPurgeSubsystem extends Subsystem {

// --- config class ---

   public static class Config implements Copyable {

      public Table jobTable;
      public long scanInterval;
      public JobManager jobManager;
      public boolean autoPurgeStale;
      public boolean autoPurgeStaleLocal;

      public Object clone() throws CloneNotSupportedException { return super.clone(); }

      public boolean equals(Object o) {
         if ( ! (o instanceof Config) ) return false;
         Config c = (Config) o;

         return (    jobTable == c.jobTable
                  && scanInterval == c.scanInterval
                  && jobManager == c.jobManager
                  && autoPurgeStale == c.autoPurgeStale
                  && autoPurgeStaleLocal == c.autoPurgeStaleLocal );
      }
   }

// --- construction ---

   public JobPurgeSubsystem(Copyable config) {
      super(config);
   }

// --- subclass hook ---

   protected StoppableThread makeThread() {
      Config c = (Config) this.config;
      return new JobPurgeThread(c.jobTable,
                                c.scanInterval,
                                makeThreadStatus(),
                                c.jobManager,
                                c.autoPurgeStale,
                                c.autoPurgeStaleLocal);
   }

}

