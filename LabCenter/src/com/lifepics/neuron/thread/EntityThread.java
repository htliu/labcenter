/*
 * EntityThread.java
 */

package com.lifepics.neuron.thread;

import com.lifepics.neuron.core.ChainedException;
import com.lifepics.neuron.core.Log;
import com.lifepics.neuron.core.Reportable;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.table.Table;
import com.lifepics.neuron.table.TableException;
import com.lifepics.neuron.table.View;

import java.util.Comparator;
import java.util.Date;
import java.util.logging.Level;

/**
 * A superclass for threads that operate on abstract entities.
 */

public abstract class EntityThread extends StoppableThread {

// --- subclass hooks ---

   // note, the subclass must provide error messages ee1 - ee3,
   // and also en1 - en3 if the non-entity facility is enabled.

   protected abstract boolean doEntity() throws Exception;

   protected boolean hasNonEntity() { return false; }
   protected void doNonEntity() throws Exception {}
   protected void endNonEntity() {}

   /**
    * @return True if you want to trigger a scan pass.
    */
   protected boolean doNonEntityTrigger() throws Exception { doNonEntity(); return false; }

   protected void checkConfigured() throws Exception {}

// --- fields ---

   protected Table table;
   private   Manipulator manipulator;
   private   boolean scanFlag; // true to scan all entities, false to work on topmost
   private   long idlePollInterval; // or scanInterval, if scanFlag is set
   protected ThreadStatus threadStatus;

   private   View view;
   protected boolean doNonEntityFirst;

   protected Date scanDate;
   protected Object lock;
   protected Object entity;

// --- construction ---

   public EntityThread(String name,
                       Table table,
                       Manipulator manipulator,
                       Comparator comparator,
                       boolean scanFlag,
                       long idlePollInterval,
                       ThreadStatus threadStatus) {
      super(name);

      this.table = table;
      this.manipulator = manipulator;
      this.scanFlag = scanFlag;
      this.idlePollInterval = idlePollInterval;
      this.threadStatus = threadStatus;

      view = table.select(manipulator,comparator,true,false);
      doNonEntityFirst = false;
      // doNonEntityFirst could easily be a constructor arg,
      // but I'll be lazy and just let subclasses set it directly.
      // it would work fine to always do non-entities first,
      // but it's also not hard to preserve the existing behavior.
   }

// --- interface for thread fields ---

   protected void doInit() throws Exception {
   }

   protected void doRun() throws Exception {
      try {
         checkConfigured();
         if (scanFlag) loopScan(); else loopFind();
      } catch (Throwable t) {

         // convert to exception since that's what we're allowed to rethrow.
         // also it's nice to have a prefix that identifies this exact case!
         Exception e;
         if (t instanceof Exception) {
            e = (Exception) t;
         } else {
            e = new Exception(Text.get(EntityThread.class,"e1"),t);
         }

         if ( ! isStopping() ) threadStatus.fatal(e);
         throw e;
      }
   }

   protected void doExit() {
   }

   protected void doStop() {
   }

// --- main loop (scan) ---

   private void loopScan() throws Exception {
      while ( ! regulateIsStopping() ) {
         scanEntities();
         sleepNice(idlePollInterval); // really scanInterval
      }
   }

   /**
    * Scan through all suitable entities.
    */
   private void scanEntities() throws Exception {

      scanDate = new Date(); // use same date for the whole scan

      scanInit();
      view.suspend();

      try {

         for (int i=0; i<view.size(); i++) {
            Object o = view.get(i);

            // allow filtering based on immutable properties
            int filter = scanFilter(o);
            if (filter == SCAN_SKIP) continue;
            if (filter == SCAN_STOP) break;

            lock = table.lockTry(o);
            if (lock != null) {
               entity = table.get(o); // get latest copy
               doEntityTryCatch();
            }

            if (regulateIsStopping()) break;
         }

      } finally {

         view.resume();
         scanExit();
      }
   }

   protected void scanInit() {}
   protected void scanExit() {}

   protected int scanFilter(Object o) { return SCAN_GO; }

   public static final int SCAN_GO   = 0;
   public static final int SCAN_SKIP = 1;
   public static final int SCAN_STOP = 2;

// --- main loop (find) ---

   private void loopFind() throws Exception {
      while ( ! regulateIsStopping() ) {
         if (doNonEntityFirst && hasNonEntity()) {
            if (doNonEntityTryCatch()) scanEntities();
         } else if (findEntity()) { // set entity, and lock if successful
            doEntityTryCatch();
         } else if ((!doNonEntityFirst) && hasNonEntity()) {
            if (doNonEntityTryCatch()) scanEntities();
         } else {
            sleepNice(idlePollInterval);
         }
      }
   }

   /**
    * Find (and lock) a suitable entity, if there is one.
    */
   protected boolean findEntity() {
      view.suspend();
      try {

         for (int i=0; i<view.size(); i++) {
            Object o = view.get(i);
            lock = table.lockTry(o);
            if (lock != null) {
               try {
                  entity = table.get(o); // get latest copy
                  if (isLoopEndingEntity(entity)) { table.release(entity,lock); break; }
                  return true;
               } catch (TableException e) {
                  // can't happen -- we locked the object, so it exists
               }
            }
         }

         return false; // no entity available

      } finally {
         view.resume();
      }

      // originally I wanted to make a non-polling system,
      // by having some method that would pick an unlocked element
      // of a query, or block until one was available.
      // that sounds nifty, but it's difficult to come up with a design,
      // not least because you have to lock an object while notifying
      // (which produces more notifications before the first is complete)

      // but, with that caveat, here's what I'd figured out ...
      // (1) add lock and unlock notifications to TableListener
      // (2) add another flag to select, telling whether to insert
      //     another TableListenerUtil filter that would restrict to
      //     unlocked objects. (you can't do that with a selector,
      //     because being locked is not a property of the object)
      // (3) if the view is empty, block, and get a notification
      //     when a new element is inserted into the view.
   }

   /**
    * @return True if we not only don't want to process this entity
    *         but also want to exit loop immediately with no result.
    */
   protected boolean isLoopEndingEntity(Object o) { return false; }

// --- handlers ---

   private Reportable getReportable() {
      return (entity instanceof Reportable) ? (Reportable) entity : null;
   }

   private void doEntityTryCatch() throws Exception {
      try {

         // make sure that the latest copy still belongs in the view.
         // test this here, since this is where we release the lock.

         if (manipulator.select(entity)) {
            doEntityStatus();
         } else { // must have just missed an update
            Thread.yield(); // try to produce update
         }

      } catch (TableException e) {

         Log.log(getReportable(),Level.SEVERE,this,"ee1",new Object[] { manipulator.getID(entity) },e);

         throw e;
         // if we are unable to update the database, let the thread exit ...
         // otherwise we might end up doing the same thing over and over.
         // other exceptions should be caught and handled.

      } catch (ThreadStopException e) {
         Log.log(Level.SEVERE,this,"ee2",e); // this is not about the current entity
         throw e;
      } catch (Exception e) {

         // I used to have a flag that let me not report retries at all, but now I think
         // that was a bad design.  instead, let's downgrade retries to warnings so that
         // they'll fall below the default filter level.
         Level reportAsLevel = manipulator.isRetrying(entity) ? Level.WARNING : Level.SEVERE;

         Log.log(getReportable(),Level.SEVERE,this,"ee3",new Object[] { manipulator.getID(entity) },e,reportAsLevel);

         // no need to report this, it's attached to the entity ...
         if (lock == null) threadStatus.error(e); // ... unless the entity was deleted

      } finally {
         if (lock != null) table.release(entity,lock);
      }
   }

   private boolean doNonEntityTryCatch() throws Exception {
      // same try-catch structure, but without the entity ID and lock release
      try {
         return doNonEntityTrigger();
      } catch (TableException e) {
         Log.log(Level.SEVERE,this,"en1",e);
         throw e;
      } catch (ThreadStopException e) {
         Log.log(Level.SEVERE,this,"en2",e);
         throw e;
      } catch (Exception e) {
         Log.log(Level.SEVERE,this,"en3",e);
         threadStatus.error(e);
         return false; // since we don't know the trigger result
      } finally {
         endNonEntity();
      }
   }

   private void doEntityStatus() throws Exception {

      if (manipulator.markActive(entity)) updateEntity();

      try {

         boolean complete = doEntity();
         if (complete) {
            if (manipulator.markComplete(entity)) updateEntity();
         } else {
            if (manipulator.markIncomplete(entity)) updateEntity();
         }

      } catch (ThreadStopException e) {

         // don't attach error, this is not about the current entity

         if (manipulator.markIncomplete(entity)) updateEntity();
         throw e;

      } catch (Exception e) {

         boolean told = false;
         boolean normal = false;
         boolean pauseRetry = false;
         long pauseRetryLimit = 0;

         for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof ToldException) told = true;
            if (t instanceof NormalOperationException) normal = true;
            if (t instanceof PauseRetryException) {
               pauseRetry = true;
               pauseRetryLimit = ((PauseRetryException) t).pauseRetryLimit;
            }
         }

         // attach error

         if (manipulator.markError(entity,ChainedException.format(e),told,pauseRetry,pauseRetryLimit)) updateEntity();
         if ( ! normal ) throw e;
         // else just return to doEntityTryCatch, net effect is that we don't log it
      }
   }

// --- update helpers ---

   private void updateEntity() throws TableException {
      if (lock != null) table.update(entity,lock);
   }

// --- subclass helpers ---

   protected void sortEntities(Comparator comparator) {
      view.sort(comparator);
   }

   protected Manipulator getManipulator() {
      return manipulator;
   }

}

