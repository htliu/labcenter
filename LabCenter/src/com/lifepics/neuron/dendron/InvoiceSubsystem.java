/*
 * InvoiceSubsystem.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Nullable;
import com.lifepics.neuron.gui.InvoiceController;
import com.lifepics.neuron.object.Copyable;
import com.lifepics.neuron.table.Table;
import com.lifepics.neuron.thread.StoppableThread;
import com.lifepics.neuron.thread.Subsystem;

import java.io.File;

/**
 * The invoice subsystem.
 */

public class InvoiceSubsystem extends Subsystem implements InvoiceController {

// --- config class ---

   public static class Config implements Copyable {

      public Table table;
      public long idlePollInterval;
      public File stylesheet;
      public File postageXSL;
      public JobManager jobManager;
      public String printerInvoice; // not used by thread, just here to trigger restart on reconfig
      public String printerLabel;

      public Object clone() throws CloneNotSupportedException { return super.clone(); }

      public boolean equals(Object o) {
         if ( ! (o instanceof Config) ) return false;
         Config c = (Config) o;

         return (    table == c.table
                  && idlePollInterval == c.idlePollInterval
                  && stylesheet.equals(c.stylesheet)
                  && postageXSL.equals(c.postageXSL)
                  && jobManager == c.jobManager
                  && Nullable.equals(printerInvoice,c.printerInvoice)
                  && Nullable.equals(printerLabel,c.printerLabel) );
      }
   }

// --- construction ---

   private boolean skipInvoice;
   private boolean skipLabel;

   public InvoiceSubsystem(Copyable config) {
      super(config);

      skipInvoice = false;
      skipLabel   = false;
   }

// --- implementation of InvoiceController ---

   /**
    * Start the subsystem's thread with temporary options.
    */
   public void start(boolean skipInvoice, boolean skipLabel) {

      // make sure we only use the temporary options once
      try {
         this.skipInvoice = skipInvoice;
         this.skipLabel   = skipLabel;
         start();
      } finally {
         this.skipInvoice = false;
         this.skipLabel   = false;
      }
   }

// --- subclass hook ---

   protected StoppableThread makeThread() {
      Config c = (Config) this.config;
      return new InvoiceThread(c.table,
                               c.idlePollInterval,
                               makeThreadStatus(),
                               c.stylesheet,
                               c.postageXSL,
                               c.jobManager,
                               skipInvoice,
                               skipLabel);
   }

}

