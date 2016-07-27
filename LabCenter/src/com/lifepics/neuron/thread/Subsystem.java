/*
 * Subsystem.java
 */

package com.lifepics.neuron.thread;

import com.lifepics.neuron.core.ChainedException;
import com.lifepics.neuron.object.Copyable;
import com.lifepics.neuron.object.CopyUtil;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.ListIterator;

import java.awt.Frame;

/**
 * An abstract class that represents a subsystem that can be started and stopped.
 */

public abstract class Subsystem implements SubsystemController, ThreadStatus {

// --- subclass hook ---

   /**
    * Construct a new thread based on the config.
    */
   protected abstract StoppableThread makeThread();

// --- fields ---

   private LinkedList listeners;
   private LinkedList errors;

   /**
    * The object that contains configuration settings.
    * It must be copyable, and have an equals defined.
    */
   protected Copyable config;

   /**
    * A flag telling whether the subsystem is enabled.
    * Think of this as being part of the config;
    * it is not affected by the thread stopping or starting.
    */
   private boolean wasEnabled;

   /**
    * The thread, or null if the subsystem is stopped.
    */
   private StoppableThread thread;

   private int state;
   private String reason;

// --- construction ---

   // override these if you have variables besides the config

   public Subsystem(Copyable config) {

      listeners = new LinkedList();
      errors = new LinkedList();

      this.config = (Copyable) CopyUtil.copy(config);

      wasEnabled = false;
      thread = null;

      state = SubsystemListener.STOPPED;
      reason = null;
   }

   protected void configure(Copyable config) {
      this.config = (Copyable) CopyUtil.copy(config);
   }

// --- synchronization analysis ---

   // look at it by sections
   //
   // * construction
   //
   // these are only called when the thread is stopped, no problem there
   //
   // * report helpers
   // * listeners
   // * error helpers
   // * thread status (client thread)
   //
   // these are all finite operations that don't feed back,
   // so just synchronize all the entry points, no problem there.
   //
   // * start-stop (UI thread)
   // * init-exit (UI thread)
   //
   // these are the tricky ones that have to be analyzed case by case
   //
   // note that the config variable is read-only
   // unless the thread is stopped, no problem there.
   // ditto for the wasEnabled flag.
   //
   // * start synchronizes to check if there's a thread,
   //   and does nothing if there is, so, not a problem.
   //   also, it doesn't do anything after starting the thread.
   //
   // * stop has to synchronize to check if there's a thread,
   //   but then unsynchronize to call stopNice,
   //   in case the thread wants to report before it exits.
   //   so, it has to make a local copy of the thread variable.
   //   everything after stopNice is no problem.
   //
   // * init is only called when the thread is stopped, and if
   //   it starts the thread, it doesn't do anything afterward.
   //
   // * reinit looks at wasEnabled and config, which is fine,
   //   and then stops the thread before doing anything else.
   //   and, if it restarts the thread, it doesn't do anything after that.
   //
   // * exit is now the same as stop.

// --- init-exit (UI thread) ---

   // the idea is, this is like construction
   // the caller protocol: init reinit* exit

   public void init(boolean enabled) {
      wasEnabled = enabled;
      if (enabled) start();
      else report(); // always make initial report
   }

   public void reinit(Copyable config, Frame owner) {
      reinit(wasEnabled,config,owner);
   }

   public void reinit(boolean enabled, Copyable config, Frame owner) {

      if (enabled == wasEnabled && config.equals(this.config)) return;

      boolean wasRunning = stop(owner);

      // compute this now to avoid confusion regarding wasEnabled
      // you could factor out the "enabled", but I think this is clearer
      //
      boolean shouldStart =    (enabled && ! wasEnabled)
                            || (enabled && wasEnabled && wasRunning);

      configure(config);

      wasEnabled = enabled;
      if (shouldStart) start();
   }

   public void exit(Frame owner) {
      stop(owner); // ignore result
   }

// --- start-stop (UI thread) ---

   public void start() {
      synchronized (this) {
         if (thread != null) return;
      }

      thread = makeThread();
      errors.clear(); // thread not running, no need to synchronize
      report(SubsystemListener.RUNNING);
      thread.start(); // must come last to avoid misordered reports
   }

   public boolean stop(Frame owner) {
      StoppableThread temp;
      synchronized (this) {

         if (thread == null) return (state == SubsystemListener.ABORTED);
         // nothing to stop;
         // count aborted as running so that it can restart on reconfig.

         temp = thread;
      }

      temp.stopNice(owner);
      thread = null;
      report(SubsystemListener.STOPPED);
      return true; // stopped
   }

// --- report helpers ---

   // the report functions are only called from the UI thread
   // when thread isn't running; thus, no need to synchronize.

   // we have to remember the last report to fill in the args
   // when clearErrors is called ... no other reason but that.

   private void report(int state) {
      this.state = state;
      this.reason = null;
      report();
   }

   private void report(int state, String reason) {
      this.state = state;
      this.reason = reason;
      report();
   }

   private void report() {
      report(state,reason,hasErrors());
   }

// --- listeners ---

   // as with Table, the listeners are stored as weak references

   /**
    * Add a subsystem listener.
    */
   public synchronized void addListener(SubsystemListener a_listener) {
      listeners.add(new WeakReference(a_listener));
      a_listener.report(state,reason,hasErrors()); // send current status
   }

   /**
    * Remove a subsystem listener.
    * Only call this function if the listener is <i>not</i> being destroyed,
    * but you want it to stop receiving events anyway.
    * If the listener is being destroyed, you don't need to unregister it,
    * because the subsystem only holds a weak reference to it.
    */
   public synchronized void removeListener(SubsystemListener a_listener) {
      ListIterator li = listeners.listIterator();
      while (li.hasNext()) {
         WeakReference ref = (WeakReference) li.next();
         SubsystemListener listener = (SubsystemListener) ref.get();
         if (listener == null || listener == a_listener) {
            li.remove();
         }
      }
   }

   private void report(int state, String reason, boolean hasErrors) {
      ListIterator li = listeners.listIterator();
      while (li.hasNext()) {
         WeakReference ref = (WeakReference) li.next();
         SubsystemListener listener = (SubsystemListener) ref.get();
         if (listener == null) {
            li.remove();
         } else {
            listener.report(state,reason,hasErrors);
         }
      }
   }

// --- error helpers ---

   private void addError(Throwable t) {

      // it will often happen that we'll get several copies of the same error.
      // we want to discard all but the first, just to keep the list short.
      // comparing messages is not ideal, but it's OK, that's all the user sees.

      String s = ChainedException.format(t);

      ListIterator li = errors.listIterator();
      while (li.hasNext()) {
         if (ChainedException.format((Throwable) li.next()).equals(s)) return;
      }

      errors.add(t);
   }

   private boolean hasErrors() {
      return (errors.size() > 0);
   }

   public synchronized LinkedList getErrors() {
      return (LinkedList) errors.clone(); // avoids sync problems, also prevents misuse
   }

   public synchronized void clearErrors() {
      if ( ! hasErrors() ) return;

      if (state == SubsystemListener.ABORTED) { // don't remove fatal error
         if (wasEnabled) {

            Object t = errors.getLast();
            errors.clear();
            errors.add(t);
            // no report, hasErrors hasn't changed

         } else { // shouldn't have been on, allow turn off (e.g. NotConfiguredException)

            errors.clear();
            report(SubsystemListener.STOPPED);
         }
      } else {

         errors.clear();
         report();
      }
   }

// --- token class ---

   private static class Token {

      public int state;
      public String reason;

      public Token(int state, String reason) {
         this.state = state;
         this.reason = reason;
      }
   }

// --- thread status (client thread) ---

   protected ThreadStatus makeThreadStatus() { return this; }

   public synchronized void pausedNetwork(String reason) {
      report(SubsystemListener.PAUSED_NETWORK,reason);
   }

   public synchronized Object pausedWait(String reason) {
      Token token = new Token(state,this.reason);
      report(SubsystemListener.PAUSED_WAIT,reason);
      return token;
   }

   public synchronized void unpaused() {
      report(SubsystemListener.RUNNING);
   }

   public synchronized void unpaused(Object token) {
      Token tcast = (Token) token;
      report(tcast.state,tcast.reason);
   }

   public synchronized void error(Throwable t) {
      addError(t);
      report(SubsystemListener.RUNNING);
   }

   public synchronized void fatal(Throwable t) {
      thread = null; // trust that it will stop
      addError(t);
      report(SubsystemListener.ABORTED);
   }

   public synchronized void success(String category) {
      boolean removed = false;

      ListIterator li = errors.listIterator();
      while (li.hasNext()) {
         if (CategorizedException.match(category,li.next())) { li.remove(); removed = true; }
      }

      if (removed && ! hasErrors()) report();
   }

}

