/*
 * TransferSubsystem.java
 */

package com.lifepics.neuron.net;

import com.lifepics.neuron.misc.TransferListener;
import com.lifepics.neuron.misc.TransferTracker;
import com.lifepics.neuron.object.Copyable;
import com.lifepics.neuron.thread.Subsystem;

/**
 * A kind of subsystem that performs file transfers via HTTP.
 */

public abstract class TransferSubsystem extends Subsystem {

// --- config class ---

   public static class Config implements Copyable {

      public DiagnoseConfig diagnoseConfig;

      public Object clone() throws CloneNotSupportedException {
         Config c = (Config) super.clone();

         c.diagnoseConfig = diagnoseConfig.copy(); // don't share

         return c;
      }

      public boolean equals(Object o) {
         if ( ! (o instanceof Config) ) return false;
         Config c = (Config) o;

         return diagnoseConfig.equals(c.diagnoseConfig);
      }
   }

// --- fields ---

   protected DiagnoseHandler diagnoseHandler;
   protected Handler handler;
   protected TransferTracker tracker;

// --- construction ---

   public TransferSubsystem(Copyable config) {
      super(config);

      Config c = (Config) this.config;
      diagnoseHandler = new DiagnoseHandler(new DefaultHandler(),c.diagnoseConfig);
      handler = new DescribeHandler(diagnoseHandler);
      tracker = new TransferTracker();
   }

   protected void configure(Copyable config) {
      super.configure(config);

      Config c = (Config) this.config;
      diagnoseHandler.reinit(c.diagnoseConfig);
   }

// --- passthroughs ---

   public void setTransferListener(TransferListener listener) {
      tracker.setListener(listener);
   }

   public void ping() {
      tracker.ping();
   }

   public Double getKbpsLast() {
      return tracker.getKbpsLast();
   }

}

