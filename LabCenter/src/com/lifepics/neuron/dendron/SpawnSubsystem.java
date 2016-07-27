/*
 * SpawnSubsystem.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Nullable;
import com.lifepics.neuron.object.Copyable;
import com.lifepics.neuron.table.Table;
import com.lifepics.neuron.thread.StoppableThread;
import com.lifepics.neuron.thread.Subsystem;

/**
 * The spawn (autoprint) subsystem.
 */

public class SpawnSubsystem extends Subsystem {

// --- config class ---

   public static class Config implements Copyable {

      public Table table;
      public long idlePollInterval;
      public boolean autoSpawn;
      public boolean autoSpawnSpecial;
      public String autoSpawnPrice;
      public JobManager jobManager;

      public Object clone() throws CloneNotSupportedException { return super.clone(); }

      public boolean equals(Object o) {
         if ( ! (o instanceof Config) ) return false;
         Config c = (Config) o;

         return (    table == c.table
                  && idlePollInterval == c.idlePollInterval
                  && autoSpawn == c.autoSpawn
                  && autoSpawnSpecial == c.autoSpawnSpecial
                  && Nullable.equals(autoSpawnPrice,c.autoSpawnPrice)
                  && jobManager == c.jobManager );
      }
   }

// --- construction ---

   public SpawnSubsystem(Copyable config) {
      super(config);
   }

// --- subclass hook ---

   protected StoppableThread makeThread() {
      Config c = (Config) this.config;
      return new SpawnThread(c.table,
                             c.idlePollInterval,
                             makeThreadStatus(),
                             c.autoSpawn,
                             c.autoSpawnSpecial,
                             c.autoSpawnPrice,
                             c.jobManager);
   }

}

