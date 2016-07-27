/*
 * StoppableThread.java
 */

package com.lifepics.neuron.thread;

import com.lifepics.neuron.core.Log;
import com.lifepics.neuron.table.Stoppable;

import java.util.logging.Level;

import java.awt.Frame;

/**
 * A thread that can be stopped in a nice way,
 * and can throw exceptions while it's running.
 * Any such exception is logged.<p>
 *
 * The concept of thread fields is useful here.
 * The thread fields are the fields that are accessed
 * both from within the thread during its normal operation
 * and from without when the thread is being stopped.
 * Modification of these fields must be synchronized.<p>
 *
 * The functions {@link #doInit() doInit}, {@link #doExit() doExit},
 * and {@link #doStop() doStop} are the only ones that should modify the fields.
 * The StoppableThread class takes care of the synchronization.
 * (The function {@link #doRun() doRun} should use but not modify the fields.)
 */

public abstract class StoppableThread extends Thread implements Stoppable {

// --- state enumeration ---

   private static final int STATE_PREINIT = 0;
   private static final int STATE_RUN = 1;
   private static final int STATE_POSTEXIT = 2;

// --- fields ---

   // thread fields that are modified by the thread itself
   //
   // possible states: (0,F) -> (1,T) -> (2,T)
   //                        -> (1,F) -> (2,F)
   //
   private int state;
   private boolean inited; // whether doInit was successful

   // thread fields that are modified by other threads (via stop)
   //
   // possible states:  F -> T
   //
   private boolean stopped;

   // stop action
   //
   private Runnable stopAction;

   // subclasses can have other thread fields

// --- accessors ---

   public boolean regulateIsStopping() { return isStopping(); } // see DownloadThread

   public synchronized boolean isStopping() { return stopped; }

   /**
    * Sleep, but in a way that can be stopped nicely.
    */
   public synchronized void sleepNice(long millis) throws InterruptedException {

      if (stopped) return;
      // it's not enough for the caller to check isStopping first.
      // first, that's inconvenient and easy to forget;
      // second, that's unreliable, because the stop signal could arrive
      // in the interval between the first function call and the second.

      wait(millis);
   }

// --- construction ---

   /**
    * @param name A name to be used when logging exceptions.
    *             It is also used as the actual name of the thread.
    */
   public StoppableThread(String name) {
      this(null,name); // null group is OK, see documentation for Thread(String)
   }

   /**
    * @param name A name to be used when logging exceptions.
    *             It is also used as the actual name of the thread.
    */
   public StoppableThread(ThreadGroup group, String name) {
      super(group,name);

      state = STATE_PREINIT;
      inited = false;
      stopped = false;
      // stopAction starts out null
   }

// --- interface for thread fields ---

   /**
    * Construct the thread fields.
    */
   protected abstract void doInit() throws Exception;

   /**
    * Run the thread.
    * This won't be called if doInit throws an exception.
    */
   protected abstract void doRun() throws Exception;

   /**
    * Destroy the thread fields.
    * This will be called even if doInit throws an exception.
    */
   protected abstract void doExit();

   /**
    * Alter the thread fields to make the thread stop,
    * even if it's blocked in the middle of some operation.
    * This won't be called if doInit throws an exception,
    * or if doExit has already been called.
    */
   protected abstract void doStop();

// --- join wrappers ---

   private void joinSafe() {
      try {
         join();
      } catch (InterruptedException e) {
         // won't happen
      }
   }

   private void joinSafe(long millis) {
      try {
         join(millis);
      } catch (InterruptedException e) {
         // won't happen
      }
   }

// --- methods ---

   public void run() {
      Exception e;

      e = null;
      synchronized (this) {
         if (stopped) return; // allow immediate stop
         try {
            doInit();
            inited = true;
         } catch (Exception e2) {
            e = e2;
         }
         state = STATE_RUN;
      }

      if (e != null) Log.log(Level.SEVERE,StoppableThread.class,"e1",new Object[] { getName() },e);

      e = null;
      if (inited) { // ok to look at inited outside sync block, stop doesn't modify it
         try {
            doRun();
         } catch (Exception e2) {
            e = e2;
         }
      }

      synchronized (this) {
         if (stopped) e = null; // don't log exceptions caused by stopping
         doExit();
         state = STATE_POSTEXIT;
      }

      if (e != null) Log.log(Level.SEVERE,StoppableThread.class,"e2",new Object[] { getName() },e);
   }

   /**
    * Stop the thread in a nice way.
    *
    * @param owner The owner of the "please wait" dialog.
    *              If the owner is null, no dialog is shown.
    */
   public void stopNice(Frame owner) {

      synchronized (this) {

         if (stopped) return; // ignore all stop calls after the first

         if (state == STATE_RUN && inited) {
            if (stopAction != null) stopAction.run();
            doStop();
         }
         // in all other states, the thread will exit by itself

         stopped = true;
         notify(); // in case we're sleeping
      }

      // this can't be synchronized, the thread needs to acquire the lock to exit
      if (owner == null) {
         joinSafe();
      } else {
         StopDialog.joinSafe(this,owner);
      }
   }

   /**
    * Stop the thread in a nice way.
    */
   public void stopNice() {
      stopNice(null);
   }

// --- stop action ---

   // The deal here is, first you identify some scope in the code in which
   // the thread can block on an operation, and needs to be interrupted.
   // Then, around that scope, you place a try-finally block for an action,
   // which will be called if the thread needs to stop.
   //
   // The one actual example of this is in the network handler code.
   // The code can block inside a HTTPConnection, so it needs to be told to stop.
   //
   // Since a blocking operation can't contain another blocking operation,
   // there's no reason to ever have more than one stop action.
   //
   // If you're starting from scratch, you should use regular thread interrupts,
   // which will cause InterruptedIOExceptions without any additional work.
   // That would get rid of StoppableThread, but it's not worth breaking everything to do it.

   /**
    * @return The current stopping value, as a convenience.
    *         If it's true, the stop action won't be run,
    *         so you should avoid doing whatever's going to block.
    */
   public synchronized boolean setStopAction(Runnable stopAction) {
      this.stopAction = stopAction;
      return stopped;
   }

   public synchronized void clearStopAction() {
      stopAction = null;
   }

   /**
    * @return The current stopping value, as a convenience,
    *         or false if the thread isn't stoppable.
    */
   public static boolean setStopActionIfStoppable(Runnable stopAction) {
      Thread thread = Thread.currentThread();
      return (thread instanceof StoppableThread) ? ((StoppableThread) thread).setStopAction(stopAction) : false;
   }

   public static void clearStopActionIfStoppable() {
      Thread thread = Thread.currentThread();
      if (thread instanceof StoppableThread) ((StoppableThread) thread).clearStopAction();
   }

}

