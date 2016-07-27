/*
 * DescribeHandler.java
 */

package com.lifepics.neuron.net;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.thread.ThreadStopException;

/**
 * A top-level handler that adds descriptive information to error messages.
 */

public class DescribeHandler extends Handler {

   private Handler next;
   public DescribeHandler(Handler next) { this.next = next; }

   /**
    * Run a transaction.
    */
   public boolean run(Transaction t, PauseCallback callback) throws Exception {
      try {
         return next.run(t,callback);
      } catch (ThreadStopException e) {
         throw e; // things that stop the thread shouldn't depend on the transaction
      } catch (Exception e) {
         throw new Exception(Text.get(this,"e1",new Object[] { t.describe() }),e);
      }
   }

}

