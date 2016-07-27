/*
 * AutoCompleteSubsystem.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Nullable;
import com.lifepics.neuron.net.DiagnoseConfig;
import com.lifepics.neuron.object.Copyable;
import com.lifepics.neuron.object.CopyUtil;
import com.lifepics.neuron.table.Table;
import com.lifepics.neuron.thread.StoppableThread;
import com.lifepics.neuron.thread.Subsystem;

import java.util.LinkedList;

/**
 * The autocomplete subsystem.
 */

public class AutoCompleteSubsystem extends Subsystem {

// --- config class ---

   public static class Config implements Copyable {

      public Table table;
      public long scanInterval;
      public OrderManager orderManager;
      public AutoCompleteConfig autoCompleteConfig;
      public LinkedList storeHours;
      public DiagnoseConfig diagnoseConfig;
      public boolean enableTracking;

      public Object clone() throws CloneNotSupportedException {
         Config c = (Config) super.clone();

         c.autoCompleteConfig = autoCompleteConfig.copy(); // don't share
         c.storeHours = CopyUtil.copyList(storeHours);
         c.diagnoseConfig = diagnoseConfig.copy();

         return c;
      }

      public boolean equals(Object o) {
         if ( ! (o instanceof Config) ) return false;
         Config c = (Config) o;

         return (    table == c.table
                  && scanInterval == c.scanInterval
                  && orderManager == c.orderManager
                  && autoCompleteConfig.equals(c.autoCompleteConfig)
                  && storeHours.equals(c.storeHours)
                  && diagnoseConfig.equals(c.diagnoseConfig)
                  && enableTracking == c.enableTracking );
      }
   }

// --- construction ---

   public AutoCompleteSubsystem(Copyable config) {
      super(config);
   }

// --- subclass hook ---

   protected StoppableThread makeThread() {
      Config c = (Config) this.config;
      return new AutoCompleteThread(c.table,
                                    c.scanInterval,
                                    makeThreadStatus(),
                                    c.orderManager,
                                    c.autoCompleteConfig,
                                    c.storeHours,
                                    c.diagnoseConfig,
                                    c.enableTracking);
   }

}

