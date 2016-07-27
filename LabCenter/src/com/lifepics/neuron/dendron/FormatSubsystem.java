/*
 * FormatSubsystem.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Nullable;
import com.lifepics.neuron.object.Copyable;
import com.lifepics.neuron.table.Table;
import com.lifepics.neuron.thread.StoppableThread;
import com.lifepics.neuron.thread.Subsystem;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * The format subsystem.
 */

public class FormatSubsystem extends Subsystem {

// --- config class ---

   public static class Config implements Copyable {

      public Table jobTable;
      public long idlePollInterval;
      public LinkedList queues;
      public boolean isQueueSubset;
      public Table orderTable;
      public HashMap dueMap;
      public File burnerFile;

      public Object clone() throws CloneNotSupportedException {
         Config c = (Config) super.clone();

         // somehow, the queue list has never been copied this whole time.
         // seems like a mistake, but I'm not sure enough to change it,
         // and who knows, maybe there's some theory about not copying it.

         // due map is shared, no copy needed

         return c;
      }

      public boolean equals(Object o) {
         if ( ! (o instanceof Config) ) return false;
         Config c = (Config) o;

         return (    jobTable == c.jobTable
                  && idlePollInterval == c.idlePollInterval
                  && queues.equals(c.queues)
                  && isQueueSubset == c.isQueueSubset
                  && orderTable == c.orderTable
                  && Nullable.equalsObject(dueMap,c.dueMap)
                  && Nullable.equals(burnerFile,c.burnerFile) );
      }
   }

// --- construction ---

   public FormatSubsystem(Copyable config) {
      super(config);
   }

// --- subclass hook ---

   protected StoppableThread makeThread() {
      Config c = (Config) this.config;
      return new FormatThread(c.jobTable,
                              c.idlePollInterval,
                              makeThreadStatus(),
                              c.queues,
                              c.isQueueSubset,
                              c.orderTable,
                              c.dueMap,
                              c.burnerFile);
   }

}

