/*
 * OrderPurgeSubsystem.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.object.Copyable;
import com.lifepics.neuron.table.Table;
import com.lifepics.neuron.thread.StoppableThread;
import com.lifepics.neuron.thread.Subsystem;

/**
 * The order purge subsystem.
 */

public class OrderPurgeSubsystem extends Subsystem {

// --- config class ---

   public static class Config implements Copyable {

      public Table table;
      public long scanInterval;
      public OrderManager orderManager;
      public boolean autoPurgeStale;
      public boolean autoPurgeStaleLocal;

      public Object clone() throws CloneNotSupportedException { return super.clone(); }

      public boolean equals(Object o) {
         if ( ! (o instanceof Config) ) return false;
         Config c = (Config) o;

         return (    table == c.table
                  && scanInterval == c.scanInterval
                  && orderManager == c.orderManager
                  && autoPurgeStale == c.autoPurgeStale
                  && autoPurgeStaleLocal == c.autoPurgeStaleLocal );
      }
   }

// --- construction ---

   public OrderPurgeSubsystem(Copyable config) {
      super(config);
   }

// --- subclass hook ---

   protected StoppableThread makeThread() {
      Config c = (Config) this.config;
      return new OrderPurgeThread(c.table,
                                  c.scanInterval,
                                  makeThreadStatus(),
                                  c.orderManager,
                                  c.autoPurgeStale,
                                  c.autoPurgeStaleLocal);
   }

}

