/*
 * DiagnoseCached.java
 */

package com.lifepics.neuron.net;

import com.lifepics.neuron.thread.StoppableThread;

/**
 * A utility class that caches a diagnosis result for sharing across threads.
 */

public class DiagnoseCached {

// --- notes on synchronization ---

   // First, an experimental fact:  if a thread tries to enter a synchronized
   // method or synchronization block, it will wait until it obtains the lock.
   // Interrupting the thread won't throw an InterruptedException.

   // So, the idea here is that if one thread's diagnosing, the other threads
   // should wait until it completes and use its result.  We can't implement
   // that by having a giant synchronization block around the whole operation,
   // though, because then the other threads wouldn't be stoppable in any way,
   // especially not nicely.

   // So, here's the plan.  When you call getCache, you can get a cache hit,
   // in which case you're done, or you can get one of two other results.
   // One means "you go ahead and diagnose", the other means "you sit and wait".
   // If you get the first result, you absolutely must report something back,
   // either a result or a lack of a result, otherwise the others will block.

   // In a normal Java program, we'd have the threads all wait on the same object,
   // and call notifyAll on it, but here we can't ... the threads have to use the
   // wait object that stopNice notifies on, which is the thread itself.

   // In theory, "sit and wait" should mean an infinite wait with notification,
   // but I don't want to do that, since notification on the thread object
   // already has meaning as a stop signal.  The notification doesn't actually
   // force the stop, but I still don't like the idea of using notification.

   private static final int RESULT_WORK = -1;
   private static final int RESULT_WAIT = -2;

// --- cache variables ---

   private Thread cacheThread; // null means empty cache, other cache fields not valid
   private long   cacheResultTime;
   private int    cacheResult;

   private Thread workThread;  // null means nobody working

// --- cache object ---

   private static final DiagnoseCached diagnoseCached = new DiagnoseCached();

// --- cache functions ---

   private synchronized int getCache(long cacheInterval) {
      Thread currentThread = Thread.currentThread();

   // (1) see if cache hit

      // we return a cached result if all the following conditions are met:
      //
      // (a) there is a cached result
      //
      // (b) the thread is different from the one that computed the result.
      //     the point here is to avoid having a thread wait through the
      //     poll interval and then receive the old cached diagnosis due to
      //     minor discrepancies in the time values.
      //
      // (c) the current time is less than the cache expiration time.
      //     note that the expiration time is recomputed every time,
      //     that way we don't have to worry about refreshing the config.

      if (    cacheThread != null // a real test, not a null pointer guard
           && currentThread != cacheThread
           && System.currentTimeMillis() < cacheResultTime + cacheInterval ) {

         return cacheResult;
      }

   // (2) no hit, figure out what caller should do

      if (workThread == null) {
         workThread = currentThread;
         return DiagnoseCached.RESULT_WORK;
      } else {
         return DiagnoseCached.RESULT_WAIT;
      }
   }

   /**
    * @param result The diagnosis result, or RESULT_STOPPED if the diagnosis didn't complete.
    *               That includes, but isn't limited to, the case when diagnose returns that.
    */
   private synchronized void setCache(int result) {
      Thread currentThread = Thread.currentThread();

      if (currentThread == workThread) { // don't know how this could fail, but check anyway
         workThread = null;

         if (result != Diagnose.RESULT_STOPPED) {

            cacheThread = currentThread;
            cacheResultTime = System.currentTimeMillis();
            cacheResult = result;
         }
      }
   }

// --- code ---

   /**
    * Call the original diagnose function, but with result caching.
    * The execution thread must be an instance of {@link StoppableThread},
    * since we may need to wait for another thread to produce the result,
    * and that waiting needs to be done in a way that can be interrupted.
    *
    * @return One of the enumerated result codes in {@link Diagnose}.
    */
   public static int diagnose(DiagnoseConfig config) throws InterruptedException {
      StoppableThread thread = (StoppableThread) Thread.currentThread();

      while ( ! thread.isStopping() ) {

         int result = diagnoseCached.getCache(config.downPollInterval);
         switch (result) {

         default: // cache hit
            return result;

         case DiagnoseCached.RESULT_WAIT:
            thread.sleepNice(1000); // hardcoded constant, rare
            break;

         case DiagnoseCached.RESULT_WORK:
            result = Diagnose.RESULT_STOPPED; // no cache if exception
            try {
               result = Diagnose.diagnose(config);
            } finally {
               diagnoseCached.setCache(result); // <i>always</i> call this
            }
            return result;
         }
      }

      return Diagnose.RESULT_STOPPED;
   }

}

