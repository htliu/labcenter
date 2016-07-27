/*
 * DiagnoseHandler.java
 */

package com.lifepics.neuron.net;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Log;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.thread.PauseRetryException;
import com.lifepics.neuron.thread.StoppableThread;

import java.io.EOFException;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.Level;
import javax.net.ssl.SSLException;

/**
 * A handler that recognizes certain error conditions and responds appropriately.
 * The handler can only run within a {@link StoppableThread};
 * if you try to run it in any other kind of thread, you'll get a cast exception.
 */

public class DiagnoseHandler extends Handler implements PauseCallback {

// --- fields ---

   // these are permanent fields
   private Handler next;
   private DiagnoseConfig config;

   // these are only valid within an invocation
   private PauseCallback callback;
   private boolean paused;
   private int pausedDiagnosis; // valid only when paused
   private int failures;
   private int failuresNotDown;

// --- construction &c ---

   public DiagnoseHandler(Handler next, DiagnoseConfig config) {
      this.next = next;
      this.config = config;
   }

   public void reinit(DiagnoseConfig config) {
      this.config = config;
   }

// --- implementation of PauseCallback ---

   /**
    * This should only be called from within this handler.
    */
   public void paused(int diagnosis) {
      // make sure we don't send unnecessary report
      if ( ! paused || diagnosis != pausedDiagnosis ) {
         if (callback != null) callback.paused(diagnosis);
         paused = true;
         pausedDiagnosis = diagnosis;
      }
   }

   /**
    * This can be called from within this handler, but also by the transaction,
    * when it reaches a point where network activity has occurred successfully.
    */
   public void unpaused() {
      // make sure we don't send unnecessary report
      if (paused) {
         if (callback != null) callback.unpaused();
         paused = false;
      }
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

   // initialize per-invocation fields

      this.callback = callback;
      paused = false;
      // pausedDiagnosis valid only when paused
      failures = 0;
      failuresNotDown = 0;

   // do the diagnosis thing

      try {
         while (true) {
            try {
               return next.run(t,this); // note we insert "this" in the callback chain
            } catch (Exception e) {
               if ( ! pause(e) ) return false;
            }
         }
      } finally {
         unpaused();
      }
   }

// --- classification ---

   // testing the class of the exception is not perfect,
   // but it is a good pretest before diagnosing the network

   // isNetwork and isRetryable are mutually exclusive, so maybe
   // what we really want is a function that returns an enum,
   // sort of like Diagnose does.  this will do for now, though.

   private static boolean isHTTP(Exception e, int code) {
      return (    e instanceof HTTPException
               && ((HTTPException) e).getCode() == code );
   }

   public static boolean isNetwork(Exception e) {
      return (    e instanceof SocketException         // hard disconnect
               //     includes ConnectException        // can't connect
               || e instanceof InterruptedIOException  // can't connect
               //     includes SocketTimeoutException  // configurable timeout
               || e instanceof UnknownHostException    // can't resolve IP address (*)
               || isHTTP(e,HttpURLConnection.HTTP_UNAVAILABLE)    // (*6)
               || e instanceof SSLException            // SSL error  (*7)
             );
   }

   public static boolean isRetryable(Exception e) {
      return (    e instanceof RetryableException
               || e instanceof WrongSizeException   // wrong-size download
               || e instanceof EOFException         // premature EOF (*3)
               || isHTTP(e,HttpURLConnection.HTTP_NOT_FOUND)      // (*4)
               || isHTTP(e,HttpURLConnection.HTTP_FORBIDDEN)      // (*5)
               || isHTTP(e,HttpURLConnection.HTTP_INTERNAL_ERROR) // (**)
             );
   }

   // (*) at this point we can't tell whether the host is unknown
   // because it really doesn't exist or just because the net is down.
   // so, go ahead and continue to the next test.

   // (**) the 500 error usually means there's a deadlock.
   // the server can report code 500 for other reasons,
   // but it would be hard to figure out the exact one.

   // (*3) hard to classify.  premature EOF sounds like a hard disconnect,
   // but in the case that was reported, it was produced by HTTPClient
   // when the length of the input stream didn't match the Content-Length header.
   // so, it's really more like a wrong-size download.

   // (*4) apparently 404 errors can be produced in normal operation.
   // I thought it would be a non-retryable condition, but it's not.

   // (*5) ditto, for 403 errors.  FYI, you get this if the HTTP Content-Length
   // exceeds the IIS var MaxRequestEntityAllowed or AspMaxRequestEntityAllowed.

   // (*6) this code, 503, is also known as "server busy".
   // the server may use this to tell LC to slow down, so it
   // needs to be an infinitely retryable network condition.

   // (*7) normally these shouldn't happen, but SuperValu's got something crazy
   // going on with their network right now and we're running into a million of
   // them, specifically SSLPeerUnverifiedException.

// --- the main function ---

   /**
    * Decide what to do about an exception; often the thing to do is pause.
    *
    * @return True if we should try the operation again.
    *         False if the thread is stopping.
    *         This is different than the result from run!
    */
   private boolean pause(Exception e) throws Exception {

   // see if the exception is possibly due to a network condition

      boolean network   = isNetwork  (e);
      boolean retryable = isRetryable(e);

      if ( ! network && ! retryable ) throw e;

   // diagnose the network condition, count the failures

      int diagnosis = retryable ? Diagnose.RESULT_NET_UP_SITE_UP : DiagnoseCached.diagnose(config);
      if (diagnosis == Diagnose.RESULT_STOPPED) return false;

      failures++;

      if (diagnosis == Diagnose.RESULT_NET_UP_SITE_UP) failuresNotDown++;
      else                                             failuresNotDown = 0;
      // failuresNotDown measures <i>successive</i> failures not due to something being down

      if (failuresNotDown > config.downRetriesEvenIfNotDown) {

         final String retryFailed = Text.get(DiagnoseHandler.class,"e1");
         // add this message one way or another, to distinguish retry failure from no retry.
         // we don't need to examine the exception before wrapping it,
         // unlike DescribeHandler, because we've already done so above.

         if (config.pauseRetry) {
            // there's also a condition on pauseRetryLimit, but we don't have
            // all the information we need to test it.  so, pass what we have upward.
            e = new PauseRetryException(retryFailed,e,config.pauseRetryLimit);
         } else {
            e = new Exception(retryFailed,e);
         }
         throw e;
      }

   // we're going to discard the exception, but let's log it first

      Log.log(Level.FINE,this,"i1",new Object[] { Convert.fromInt(failures) },e);

   // now we know something is down, so wait before retrying

      if (failures > config.downRetriesBeforeNotification) paused(diagnosis);

      StoppableThread thread = (StoppableThread) Thread.currentThread();
      thread.sleepNice(config.downPollInterval);

      // this would be a nice place to unpause, because then
      // there would be no question of the flag being left on by accident;
      // but if we did that, the indicator would flicker all the time.
      // so, instead, we just track whether the current state is paused,
      // and make sure to call unpaused by using a "finally" clause above.

      // to get the indicator to turn off in a timely manner, transactions
      // should call unpaused as soon as they've established a connection

      return ( ! thread.regulateIsStopping() );
   }

}

