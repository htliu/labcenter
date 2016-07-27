/*
 * LocalSubsystem.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.object.Copyable;
import com.lifepics.neuron.object.CopyUtil;
import com.lifepics.neuron.table.Table;
import com.lifepics.neuron.thread.StoppableThread;
import com.lifepics.neuron.thread.Subsystem;

import java.io.File;
import java.util.LinkedList;

/**
 * The local order subsystem.
 */

public class LocalSubsystem extends Subsystem {

// --- config class ---

   public static class Config implements Copyable {

      public LocalConfig localConfig;
      public Table table;
      public File stylesheet;
      public LinkedList coverInfos;
      public boolean enableItemPrice;

      public Object clone() throws CloneNotSupportedException {
         Config c = (Config) super.clone();

         c.localConfig = localConfig.copy();
         c.coverInfos = CopyUtil.copyList(coverInfos);

         return c;
      }

      public boolean equals(Object o) {
         if ( ! (o instanceof Config) ) return false;
         Config c = (Config) o;

         return (    localConfig.equals(c.localConfig)
                  && table == c.table
                  && stylesheet.equals(c.stylesheet)
                  && coverInfos.equals(c.coverInfos)
                  && enableItemPrice == c.enableItemPrice );
      }
   }

// --- construction ---

   public LocalSubsystem(Copyable config) {
      super(config);
   }

// --- subclass hook ---

   protected StoppableThread makeThread() {
      Config c = (Config) this.config;
      return new LocalThread(c.localConfig,
                             c.table,
                             c.stylesheet,
                             c.coverInfos,
                             c.enableItemPrice,
                             makeThreadStatus());
   }

}

