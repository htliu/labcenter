/*
 * LocalImageSubsystem.java
 */

package com.lifepics.neuron.axon;

import com.lifepics.neuron.core.Nullable;
import com.lifepics.neuron.object.Copyable;
import com.lifepics.neuron.thread.StoppableThread;
import com.lifepics.neuron.thread.Subsystem;

import java.io.File;

/**
 * The local image subsystem.
 */

public class LocalImageSubsystem extends Subsystem {

// --- config class ---

   public static class Config implements Copyable {

      public File localShareDir;
      public LocalImageConfig localImageConfig;
      public RollManager rollManager;

      public Object clone() throws CloneNotSupportedException {
         Config c = (Config) super.clone();

         c.localImageConfig = localImageConfig.copy();

         return c;
      }

      public boolean equals(Object o) {
         if ( ! (o instanceof Config) ) return false;
         Config c = (Config) o;

         return (    Nullable.equals(localShareDir,c.localShareDir)
                  && localImageConfig.equals(c.localImageConfig)
                  && rollManager == c.rollManager );
      }
   }

// --- construction ---

   public LocalImageSubsystem(Copyable config) {
      super(config);
   }

// --- subclass hook ---

   protected StoppableThread makeThread() {
      Config c = (Config) this.config;
      return new LocalImageThread(c.localShareDir,
                                  c.localImageConfig,
                                  c.rollManager,
                                  makeThreadStatus());
   }

}

