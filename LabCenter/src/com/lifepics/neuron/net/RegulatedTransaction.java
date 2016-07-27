/*
 * RegulatedTransaction.java
 */

package com.lifepics.neuron.net;

import com.lifepics.neuron.thread.StoppableThread;
import com.lifepics.neuron.thread.ThreadStatus;

/**
 * A transaction that regulates (bandwidth-limits) another transaction.
 */

public class RegulatedTransaction extends Transaction {

   // it would make a lot more sense if this were a handler instead of
   // a transaction, but that doesn't work, for two reasons.
   //
   // first, we only want the regulation block on network transactions,
   // not on, say, DownloadThread.ParseOrder or TransactionGroup.
   //
   // second, as a handler it would need to be linked to the handlers
   // in TransferSubsystem, which are per subsystem not per thread.
   // it would also need to include a StoppableThread reference to the
   // current thread.  but then, without heroic measures to make sure
   // the handler was unlinked or the reference was cleared at the end,
   // we wouldn't be able to garbage-collect one thread until the next
   // had started.  nasty!

   private BandwidthConfig bandwidthConfig;
   private ThreadStatus threadStatus;
   private StoppableThread stoppableThread;
   private Transaction next;

   public RegulatedTransaction(BandwidthConfig bandwidthConfig, ThreadStatus threadStatus, StoppableThread stoppableThread, Transaction next) {
      this.bandwidthConfig = bandwidthConfig;
      this.threadStatus = threadStatus;
      this.stoppableThread = stoppableThread;
      this.next = next;
   }

   public String describe() { return next.describe(); }
   // no need to add text, we know which transactions are regulated and which aren't

   public boolean run(PauseCallback callback) throws Exception {

      long base = System.currentTimeMillis();
      try {
         return next.run(callback);
      } finally {
         BandwidthUtil.regulateIsStopping(bandwidthConfig,System.currentTimeMillis()-base,threadStatus,stoppableThread);
         // if we're stopping, regulateIsStopping returns immediately.
         // if not, we want to regulate but then continue on to the next isStopping checkpoint,
         // so we just ignore the result.  see DownloadThread for more detail about why that's
         // the right approach.
      }
   }

}

