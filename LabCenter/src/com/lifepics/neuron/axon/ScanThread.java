/*
 * ScanThread.java
 */

package com.lifepics.neuron.axon;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.table.Table;
import com.lifepics.neuron.thread.EntityManipulator;
import com.lifepics.neuron.thread.NotConfiguredException;
import com.lifepics.neuron.thread.ThreadStatus;

import java.util.LinkedList;

/**
 * A thread that communicates with scanners where the images aren't all available
 * on disk up front.  Right now that's just DLS, but there could be others later.
 */

public class ScanThread extends RollThread {

// --- callback interface ---

   public interface Callback {
      LinkedList getDLSOrderIDs();
      void       setDLSOrderIDs(LinkedList dlsOrderIDs) throws Exception;
   }

// --- fields ---

   private long scannerPollInterval;
   private ScanConfigDLS configDLS;
   private Callback callback;
   private RollManager rollManager;

   private long nextPoll;

   private ScanThreadImpl impl;

// --- construction ---

   public ScanThread(Table table, long idlePollInterval, ThreadStatus threadStatus,
                     long scannerPollInterval, ScanConfigDLS configDLS, Callback callback, RollManager rollManager) {
      super(Text.get(ScanThread.class,"s1"),
            table,
            new EntityManipulator(
               Roll.STATUS_ROLL_SCANNED,
               Roll.STATUS_ROLL_COPYING,
               Roll.STATUS_ROLL_PENDING),
            /* scanFlag = */ false,
            idlePollInterval,
            threadStatus);

      this.scannerPollInterval = scannerPollInterval;
      this.configDLS = configDLS;
      this.callback = callback;
      this.rollManager = rollManager;

      nextPoll = System.currentTimeMillis();

      // can't construct impl now, unsafe
   }

   protected void checkConfigured() throws Exception {
      if (configDLS.effectiveDate == null) throw new NotConfiguredException(Text.get(this,"e3")).setHint(Text.get(this,"h3"));
      // see explanation in LocalThread
   }

// --- non-entity process ---

   protected boolean hasNonEntity() {
      return (System.currentTimeMillis() >= nextPoll);
   }

   protected void endNonEntity() {
      nextPoll = System.currentTimeMillis() + scannerPollInterval;
   }

   protected void doNonEntity() throws Exception {
      try {
         if (impl == null) impl = new ScanThreadImpl(configDLS,callback,rollManager,threadStatus);
         impl.doNonEntity();
      } catch (NoClassDefFoundError e) {
         throw new Exception(Text.get(this,"e1"));
      }
   }

// --- entity process ---

   protected boolean doRoll() throws Exception {
      try {
         if (impl == null) impl = new ScanThreadImpl(configDLS,callback,rollManager,threadStatus);
         return impl.doRoll(roll);
      } catch (NoClassDefFoundError e) {
         throw new Exception(Text.get(this,"e2"));
      }
   }

}

