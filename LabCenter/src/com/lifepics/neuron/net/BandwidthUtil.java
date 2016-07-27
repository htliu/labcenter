/*
 * BandwidthUtil.java
 */

package com.lifepics.neuron.net;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.thread.StoppableThread;
import com.lifepics.neuron.thread.ThreadStatus;

import java.util.Calendar;

/**
 * A utility class that uses a {@link BandwidthConfig} object to
 * implement standard bandwidth limiting and inactivity behavior.
 */

public class BandwidthUtil {

   // the code is similar to RestartThread.getWaitInterval in some ways,
   // but I think it's not worth trying to unify them.
   // the large function is a little ugly, but it's hard to break apart.

   // it would be technically possible to get into FileUtil.copy and
   // regulate the bandwidth there, but I don't think that would be useful.
   // it would make server timeout more likely; it would mess with
   // our bandwidth statistics in the UI; and it isn't even that exact
   // because for downloads we'd get a burst from the server until the
   // buffers fill up.

   public static int getTimeNow() { // if you don't need to reuse the calendar later
      return getTimeNow(Calendar.getInstance());
   }
   public static int getTimeNow(Calendar cNow) {

      int h = cNow.get(Calendar.HOUR_OF_DAY);
      int m = cNow.get(Calendar.MINUTE);

      return h*60 + m;
   }

   /**
    * Check whether t1 is between t0 and t2, or, rather, is contained
    * in the interval [t0,t2).
    * Note that t0 and t2 are different by BandwidthConfig validation.
    */
   private static boolean between(int t0, int t1, int t2) {
      if (t0 < t2) { // normal case

         return (t0 <= t1 && t1 < t2);

      } else { // wraparound case

         return (t0 <= t1 || t1 < t2);
      }
   }

   public static class IntRef { public int value; }

   /**
    * Find the current schedule entry.
    *
    * @param ref An integer reference, or null if you're not interested in the schedule index value.
    */
   public static BandwidthConfig.Schedule findCurrent(BandwidthConfig bc, int timeNow, IntRef ref) {

      // why an index instead of an iterator?  the index is useful for
      // the looping search for next nonzero schedule, and the list is
      // short enough that it's not a huge hit.
      //
      BandwidthConfig.Schedule schedule = (BandwidthConfig.Schedule) bc.schedules.get(0);
      //
      int n = bc.schedules.size();
      int i;
      for (i=1; i<n; i++) {
         BandwidthConfig.Schedule next = (BandwidthConfig.Schedule) bc.schedules.get(i);

         if (between(schedule.startTime,timeNow,next.startTime)) break; // use schedule
         schedule = next;
      }
      // if we never hit the break, the result is still correct.  every time fits somewhere
      // in the schedule, and if it wasn't between any of the adjacent pairs, then it's
      // between the last and first entries .... and schedule is left set to the last entry.

      i--; // so it points to schedule instead of next

      if (ref != null) ref.value = i;

      return schedule;
   }

   /**
    * Find the next schedule entry.
    */
   public static BandwidthConfig.Schedule findNext(BandwidthConfig bc, int i) {
      int n = bc.schedules.size();

      // increment with looping
      i++;
      if (i == n) i = 0;

      return (BandwidthConfig.Schedule) bc.schedules.get(i);
   }

   /**
    * Find the next schedule entry with a nonzero bandwidth percent.
    * The entry marked by i is known to be zero and is not examined.
    */
   private static BandwidthConfig.Schedule findNextNonzero(BandwidthConfig bc, int i) {
      int n = bc.schedules.size();

      int j = i;
      while (true) {

         // increment with looping
         j++;
         if (j == n) j = 0;

         // check for full cycle (impossible by validation, but infinite loop is bad)
         if (j == i) throw new IllegalArgumentException(); // close enough

         BandwidthConfig.Schedule schedule = (BandwidthConfig.Schedule) bc.schedules.get(j);
         if (schedule.bandwidthPercent != 0) return schedule;
      }
   }

   public static Calendar getTransition(Calendar cNow, int timeNow, BandwidthConfig.Schedule next) {

      int y = cNow.get(Calendar.YEAR);
      int M = cNow.get(Calendar.MONTH);
      int d = cNow.get(Calendar.DAY_OF_MONTH);

      // note that out-of-range values of day are allowed, e.g. July 32.
      int dNext = d;
      if (next.startTime <= timeNow) dNext++; // equality is impossible because
      // timeNow is contained in the interval for schedule, but use that format
      // for consistency with RestartThread.

      int hNext = next.startTime / 60;
      int mNext = next.startTime % 60;

      Calendar cNext = Calendar.getInstance();
      cNext.clear(); // millis
      cNext.set(y,M,dNext,hNext,mNext,0);

      return cNext;
   }

   private static final int WAIT_NONE  = 0;
   private static final int WAIT_LIMIT = 1;
   private static final int WAIT_SCHED = 2;

   /**
    * The function that implements standard bandwidth limiting and inactivity behavior.
    * Since a wait may be involved, you generally want to check isStopping pretty soon
    * after you call this, just as with sleepNice.
    *
    * @return An integer from the wait type enumeration above.
    */
   private static int regulate(BandwidthConfig bc, long lastTransferDuration, ThreadStatus threadStatus) {

   // get current time value

      Calendar cNow = Calendar.getInstance();
      int timeNow = getTimeNow(cNow);

   // find current schedule

      IntRef ref = new IntRef();
      BandwidthConfig.Schedule schedule = findCurrent(bc,timeNow,ref);

   // define result variables

      String waitKey;
      int waitType;
      long waitDuration;

   // case 1 : bandwidth limiting

      if (schedule.bandwidthPercent != 0) {

         waitKey = "s1";
         waitType = WAIT_LIMIT;

         int waitPercent = 100 - schedule.bandwidthPercent;
         waitDuration = (lastTransferDuration * waitPercent) / schedule.bandwidthPercent;

         if (waitDuration > bc.delayCap) waitDuration = bc.delayCap;
         //
         // the scale factor can be as large as 99x, so in theory overflow is possible,
         // but as long as we ignore negatives and cap, nothing too weird can happen.
         // also note that if bandwidthPercent is 100, waitPercent will be 0, then the
         // wait duration will be 0 and there will be no wait, guaranteed.

   // case 2 : scheduled inactivity

      } else {

         waitKey = "s2";
         waitType = WAIT_SCHED;

         BandwidthConfig.Schedule next = findNextNonzero(bc,ref.value);
         Calendar cNext = getTransition(cNow,timeNow,next);
         waitDuration = cNext.getTimeInMillis() - cNow.getTimeInMillis();

         // delay cap doesn't apply in this case
      }

   // the actual wait, if any

      if (waitDuration > 0) { // ignore negatives, if one happens somehow

         Object token = threadStatus.pausedWait(Text.get(BandwidthUtil.class,waitKey));

         StoppableThread thread = (StoppableThread) Thread.currentThread();
         try {
            thread.sleepNice(waitDuration);
         } catch (InterruptedException e) {
            // won't happen
         }

         threadStatus.unpaused(token); // no need if stopping, but no huge harm either
         // we have to use the token form because we get called inside the scope
         // of DiagnoseHandler, where the state can be either running or pausedNetwork.

         return waitType; // waited

      } else {

         return WAIT_NONE; // didn't wait
      }
   }

   /**
    * The entry point for standard bandwidth limiting and inactivity behavior.
    * Note that the ThreadStatus and StoppableThread objects are not the same.
    *
    * @return True if stopping.
    */
   public static boolean regulateIsStopping(BandwidthConfig bc, long lastTransferDuration, ThreadStatus threadStatus, StoppableThread thread) {
      int waitType;

      if (thread.isStopping()) return true;
      // check this first since the schedule calculation is a bit pricey.
      // also if we're stopping, we don't want to blink the light
      // by trying to wait when sleepNice is going to return immediately.

      if (bc.trivial) return false;
      // check this too, for the same reason.  as a result, when the config
      // is trivial, regulateIsStopping acts exactly the same as isStopping.

      waitType = regulate(bc,lastTransferDuration,threadStatus);
      if (waitType == WAIT_NONE) return false;
      // didn't wait, so still not stopping

      if (thread.isStopping()) return true;
      // we did a wait, so check this again

      if (waitType == WAIT_SCHED) return false;
      // we waited for activity, so now we're active, no need to check again.
      // note that if lastTransferDuration is zero, you can't get WAIT_LIMIT.

      waitType = regulate(bc,/* lastTransferDuration = */ 0,threadStatus);
      if (waitType == WAIT_NONE) return false;
      // didn't wait, so still not stopping

      if (thread.isStopping()) return true;
      // we did a wait, so check this again

      return false;

      // the theory behind the second regulate call is, if the first call
      // produced a limit wait, that's a sleep, and we need to regulate
      // afterward to test if we entered a period of scheduled inactivity.
      // see DownloadThread for more discussion.
   }

}

