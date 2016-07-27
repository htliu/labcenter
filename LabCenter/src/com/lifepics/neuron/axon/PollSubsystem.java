/*
 * PollSubsystem.java
 */

package com.lifepics.neuron.axon;

import com.lifepics.neuron.net.MerchantConfig;
import com.lifepics.neuron.object.Copyable;
import com.lifepics.neuron.object.CopyUtil;
import com.lifepics.neuron.thread.StoppableThread;
import com.lifepics.neuron.thread.Subsystem;

import java.util.LinkedList;

/**
 * The poll subsystem.
 */

public class PollSubsystem extends Subsystem {

// --- config class ---

   public static class Config implements Copyable {

      public PollConfig pollConfig;
      public MerchantConfig merchantConfig;
      public LinkedList dealers;
      public RollManager rollManager;

      public Object clone() throws CloneNotSupportedException {
         Config c = (Config) super.clone();

         c.pollConfig = pollConfig.copy();
         c.merchantConfig = merchantConfig.copy();
         c.dealers = CopyUtil.copyList(dealers);

         return c;
      }

      public boolean equals(Object o) {
         if ( ! (o instanceof Config) ) return false;
         Config c = (Config) o;

         return (    pollConfig.equals(c.pollConfig)
                  && merchantConfig.equals(c.merchantConfig)
                  && dealers.equals(c.dealers)
                  && rollManager == c.rollManager );
      }
   }

// --- construction ---

   public PollSubsystem(Copyable config) {
      super(config);
   }

// --- subclass hook ---

   protected StoppableThread makeThread() {
      Config c = (Config) this.config;
      return new PollThread(c.pollConfig,
                            c.merchantConfig,
                            c.dealers,
                            c.rollManager,
                            makeThreadStatus());
   }

}

