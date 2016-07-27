/*
 * ErrorWindow.java
 */

package com.lifepics.neuron.thread;

import java.util.LinkedList;
import java.util.ListIterator;

/**
 * A utility class for computing the error level in a rolling window.
 * Inbound interface {@link SubsystemListener} plus control functions
 * reinit and ping, outbound interface {@link ExtendedListener}.
 * It is synchronized so that it can receive reports from one thread
 * and pings and reconfigurations from another.<p>
 *
 * Two notes:<p>
 *
 * (1) If you reconfigure to a longer interval, the previous history
 * may include some amount of error from the current transition block,
 * but mostly it will count as non-error.  This could be improved,
 * say by extrapolating backward to keep the percent constant ... but
 * it's just not worth it.<p>
 *
 * (2) If the actual error percentage is higher than the parameter,
 * as seems likely, the time it takes for the light to turn red will
 * be shorter.  For example, with the default settings, the light
 * will turn red after 81 minutes, not 90.  I don't know what else
 * to do, though ... the one option I can think of, scaling up the
 * interval, seems even weirder to me.  For example, if you set up
 * for 45 minutes at 50%, the interval would be 90 minutes, and then
 * had 45 minutes of error and 40 minutes of OK, a new error would
 * still immediately show red.  Actually 50% is weird no matter how
 * you slice it.
 */

public class ErrorWindow implements SubsystemListener {

// --- fields ---

   private LinkedList listeners;

   // configuration fields
   private boolean enabled;
   private long interval;
   private long minimum; // computed from percent

   private LinkedList transitions; // list of Long
   // transitions assume we start in non-error state.
   // there can be an even or odd number of items,
   // depending on whether we're currently in error.

   // saved values from last report
   private int     saveState;
   private String  saveReason;
   private boolean saveHasErrors;
   private boolean saveShow;

// --- construction ---

   public ErrorWindow(boolean enabled, long interval, int percent) {

      listeners = new LinkedList();

      this.enabled = enabled;
      this.interval = interval;
      minimum = (interval * percent) / 100;

      transitions = new LinkedList();

      saveState = SubsystemListener.STOPPED;
      saveReason = null;
      saveHasErrors = false;
      saveShow = calcShow();
   }

// --- control functions ---

   public synchronized void reinit(boolean enabled, long interval, int percent) {

      this.enabled = enabled;
      this.interval = interval;
      minimum = (interval * percent) / 100;

      recalculate();
   }

   public synchronized void ping() {
      recalculate();
   }

// --- listener functions ---

   public synchronized void addListener(ExtendedListener listener) {
      listeners.add(listener);
      sendReport(listener); // no computation, just send latest report
   }

   private void recalculate() {
      boolean show = calcShow();
      if (show != saveShow) {
         saveShow = show;
         sendReport();
      }
   }

   private void sendReport() {
      ListIterator li = listeners.listIterator();
      while (li.hasNext()) {
         sendReport((ExtendedListener) li.next());
      }
   }

   private void sendReport(ExtendedListener listener) {
      listener.report(saveState,saveReason,saveHasErrors,saveShow);
   }

// --- implementation of SubsystemListener ---

   public synchronized void report(int state, String reason, boolean hasErrors) {

      saveState = state;
      saveReason = reason;
      saveHasErrors = hasErrors;

      long now = System.currentTimeMillis();

      int classification = classify();
      if (classification == STATE_OTHER) {
         transitions.clear();
         // other states kill the statistics
      } else {
         boolean wasError = ((transitions.size() & 1) == 1);
         boolean  isError = (classification == STATE_ADJUSTABLE);
         if (isError != wasError) {
            purge(now);
            transitions.add(new Long(now));
         }
         // calcShow may or may not purge the transitions,
         // depending on the configuration and the state.
         // so, to prevent unlimited list growth, purge here too,
         // where we make the list longer.
         //
         // if there are no transitions, and no calculations,
         // stale entries can sit around for a while,
         // but there's no harm in that if the list doesn't grow.
      }

      // we classify twice, once above and once in calcShow,
      // but it's no big deal, it's a very simple function.

      saveShow = calcShow(now);
      sendReport(); // always pass report through, even if no change
   }

// --- utility functions ---

   private static final int STATE_NO_ERROR   = 0;
   private static final int STATE_ADJUSTABLE = 1;
   private static final int STATE_OTHER      = 2;

   /**
    * Classify the current state.
    */
   private int classify() {
      switch (saveState) {
      case SubsystemListener.RUNNING:
         return saveHasErrors ? STATE_ADJUSTABLE : STATE_NO_ERROR;
      case SubsystemListener.PAUSED_NETWORK:
         return STATE_ADJUSTABLE;
      case SubsystemListener.PAUSED_WAIT:
         return saveHasErrors ? STATE_ADJUSTABLE : STATE_NO_ERROR;
         // it's not an error when we wait to limit the bandwidth
      default:
         return STATE_OTHER;
      }
   }

   /**
    * Purge transitions that are outside the window.
    */
   private void purge(long now) {

      long limit = now - interval;

   // remove all error blocks that are fully outside the window
   // you could do this with an iterator, too, but I think this is easier to understand

      while (transitions.size() >= 2) { // while there's a closed block to look at

         long end = ((Long) transitions.get(1)).longValue(); // upper end of block
         if (end > limit) break; // still inside window, rest will be too

         transitions.removeFirst();
         transitions.removeFirst();
      }
   }

// --- the main function ---

   /**
    * Calculate the correct value for the show flag,
    * based on (a) the configuration settings,
    * (b) the transition history, (c) the current state,
    * and (d) the current time (optional argument).
    */
   private boolean calcShow() { return calcShow(System.currentTimeMillis()); }
   private boolean calcShow(long now) {

   // first test, easiest

      if ( ! enabled ) return true; // not enabled means always show immediately

   // second test

      if (classify() != STATE_ADJUSTABLE) return true; // show is ignored, call it true

   // now we have to calculate

      purge(now);

   // add up the error intervals

      long limit = now - interval;
      long error = 0;

      ListIterator li = transitions.listIterator();
      while (li.hasNext()) {

         long start = ((Long) li.next()).longValue();
         if (start < limit) start = limit; // can only happen on first block

         long end = li.hasNext() ? ((Long) li.next()).longValue() : now;

         error += end - start;
      }

   // done

      return (error >= minimum);
   }

}

