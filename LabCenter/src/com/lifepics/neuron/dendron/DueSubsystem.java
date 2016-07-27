/*
 * DueSubsystem.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.object.Copyable;
import com.lifepics.neuron.object.CopyUtil;
import com.lifepics.neuron.table.Table;
import com.lifepics.neuron.thread.StoppableThread;
import com.lifepics.neuron.thread.Subsystem;

import java.util.LinkedList;

/**
 * The order due subsystem.
 */

public class DueSubsystem extends Subsystem {

// --- config class ---

   public static class Config implements Copyable {

      public Table table;
      public long scanInterval;
      public ProductConfig productConfig;
      public long soonInterval;
      public int skuDue;
      public LinkedList skus;
      public String timeZone;

      public Object clone() throws CloneNotSupportedException {
         Config c = (Config) super.clone();

         c.productConfig = productConfig.copy();
         c.skus = new LinkedList(skus); // SKUs are immutable

         return c;
      }

      public boolean equals(Object o) {
         if ( ! (o instanceof Config) ) return false;
         Config c = (Config) o;

         return (    table == c.table
                  && scanInterval == c.scanInterval
                  && productConfig.equals(c.productConfig)
                  && soonInterval == c.soonInterval
                  && skuDue == c.skuDue
                  && skus.equals(c.skus)
                  && timeZone.equals(c.timeZone) );
      }
   }

// --- construction ---

   public DueSubsystem(Copyable config) {
      super(config);
   }

// --- subclass hook ---

   protected StoppableThread makeThread() {
      Config c = (Config) this.config;
      return new DueThread(c.table,c.scanInterval,makeThreadStatus(),
                           c.productConfig,
                           c.soonInterval,c.skuDue,c.skus,
                           c.timeZone);
   }

}

