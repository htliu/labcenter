/*
 * RetryHandler.java
 */

package com.lifepics.neuron.net;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Log;
import com.lifepics.neuron.core.Text;

import java.util.logging.Level;

/**
 * A handler that performs a limited number of retries,
 * basically a simpler form of {@link DiagnoseHandler}
 * for use at startup when we can't take too much time
 * and aren't in a {@link StoppableThread}.
 */

public class RetryHandler extends Handler {

// --- fields ---

   private Handler next;
   private int retries;
   private long retryInterval;

// --- construction ---

   public RetryHandler(Handler next, int retries, long retryInterval) {
      this.next = next;
      this.retries = retries;
      this.retryInterval = retryInterval;
   }

// --- implementation of Handler ---

   /**
    * Run a transaction.
    *
    * @return True if the transaction was performed successfully.
    *         False if the thread is stopping.
    *         If there's an error, an exception should be thrown.
    */
   public boolean run(Transaction t, PauseCallback callback) throws Exception {

      int failures = 0;

      while (true) {
         try {
            return next.run(t,callback);
         } catch (Exception e) {

            // don't retry permanent failures, not even here
            if (    ! DiagnoseHandler.isNetwork  (e)
                 && ! DiagnoseHandler.isRetryable(e) ) throw e;

            // see whether we're out of retries
            failures++;
            if (failures > retries) throw new Exception(Text.get(this,"e1"),e);

            // log the exception before discard
            Log.log(Level.FINE,this,"i1",new Object[] { Convert.fromInt(failures) },e);

            // wait and try again ; no thread stop test here
            Thread.sleep(retryInterval);
         }
      }
   }

}

