/*
 * StoreHours.java
 */

package com.lifepics.neuron.misc;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.meta.Accessor;
import com.lifepics.neuron.meta.FieldComparator;
import com.lifepics.neuron.meta.NaturalComparator;
import com.lifepics.neuron.struct.*;

import java.util.Calendar;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;

import org.w3c.dom.Node;

/**
 * An object that holds information about store hours.
 * Some of this could be combined with what's in BandwidthConfig,
 * but it's tricky because that's per day while this is per week.
 */

public class StoreHours extends Structure {

// --- fields ---

   public int id; // the logic here is the same as in BandwidthConfig
   public int open;
   public int close;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      StoreHours.class,
      // no version
      new AbstractField[] {

         new IntegerField("id","ID"),
         new IntegerField("open","Open") {
            protected void loadNormal(Node node, Object o) throws ValidationException {
               tset(o,Convert.toTOW(getElementText(node,xmlName)));
            }
            public void store(Node node, Object o) {
               createElementText(node,xmlName,Convert.fromTOW(tget(o)));
            }
         },
         new IntegerField("close","Close") {
            protected void loadNormal(Node node, Object o) throws ValidationException {
               tset(o,Convert.toTOW(getElementText(node,xmlName)));
            }
            public void store(Node node, Object o) {
               createElementText(node,xmlName,Convert.fromTOW(tget(o)));
            }
         }
      });

   static { sd.setAttributed(); }
   protected StructureDefinition sd() { return sd; }

// --- sort functions ---

   public static Accessor idAccessor = new Accessor() {
      public Class getFieldClass() { return Integer.class; }
      public Object get(Object o) { return new Integer(((StoreHours) o).id); }
   };

   public static Comparator idComparator = new FieldComparator(idAccessor,new NaturalComparator());

// --- validation ---

   public void validate() throws ValidationException {
   }

   /**
    * Special case, if there are no entries, that means the store is always closed.
    * This case can sometimes be handled like a normal case, but it doesn't always
    * work out.
    */
   public static boolean isAlwaysClosed(LinkedList storeHours) {
      return (storeHours.size() == 0);
   }

   /**
    * Special case, if there's a single entry with the same open and close times,
    * that means the store is always open.  Otherwise, same times aren't allowed,
    * not in one entry and not between one entry and the next.
    */
   public static boolean isAlwaysOpen(LinkedList storeHours) {
      if (storeHours.size() != 1) return false;
      StoreHours hours = (StoreHours) storeHours.getFirst();
      return (hours.open == hours.close);
   }

   public static void validate(LinkedList storeHours) throws ValidationException {

      if (    isAlwaysClosed(storeHours)
           || isAlwaysOpen  (storeHours) ) return; // special cases handled specially

      StoreHours first = null;
      StoreHours prev  = null;
      int towDistance = 0;

      Iterator i = storeHours.iterator();
      while (i.hasNext()) {
         StoreHours hours = (StoreHours) i.next();

         // single-entry validation
         hours.validate();

         // ordered pair validation
         if (first == null) {
            first = hours;
         } else {
            towDistance += validatePair(prev,hours,/* wrap = */ false);
         }

         prev = hours;
      }

      // final ordered pair validation
      towDistance += validatePair(prev,first,/* wrap = */ true);

      if (towDistance != Convert.MINUTES_PER_WEEK) throw new ValidationException(Text.get(StoreHours.class,"e1"));
      // it has to be a multiple, we just want to be sure it's only one.
   }

   /**
    * @return The TOW distance increment.
    */
   private static int validatePair(StoreHours h1, StoreHours h2, boolean wrap) throws ValidationException {

      // IDs must be strictly increasing (except at the wrap point), therefore unique
      if (h2.id <= h1.id && ! wrap) throw new ValidationException(Text.get(StoreHours.class,"e2",new Object[] { Convert.fromInt(h1.id), Convert.fromInt(h2.id) }));

      // adjacent times must not be the same (same for nonadjacent, but we detect that with towDistance)
      // kluge, the TOW value to report is h1.close in either case!
      if (    h1.close == h1.open
           || h2.open  == h1.close) throw new ValidationException(Text.get(StoreHours.class,"e3",new Object[] { Convert.fromTOW(h1.close) }));

      // figure out TOW distance increment
      return   Convert.deltaTOW(h1.close,h1.open )
             + Convert.deltaTOW(h2.open, h1.close);
   }

// --- methods ---

   // it seems like you ought to be able to narrow and widen by just
   // changing the hours and then looking at whether intervals are
   // negative length or the gaps between intervals are negative length,
   // but the concept of negative is tough here.  better to do cases.

   // ignoring the wraparound issues, the idea is that (open,close)
   // becomes (open+deltaOpen,close+deltaClose).  thus, to see what
   // kind of transformation we're talking about,
   // we want to compare the after interval to the before interval,
   // (close+deltaClose)-(open+deltaOpen) to close-open.
   // the difference is x = deltaClose-deltaOpen.  if x > 0, the
   // interval is wider, if x < 0 it's narrower, and if x = 0 it
   // can be thought of as either (it's n.n. a null transformation,
   // could be a shift).  there are only three places this matters,
   // marked with (*) below.

   public static void adjustMillis(LinkedList storeHours, long deltaOpen, long deltaClose) {
      adjust(storeHours,toMinutes(deltaOpen),toMinutes(deltaClose));
   }

   /**
    * Convert from a config setting in milliseconds to a delta value in minutes.
    * The rounding doesn't really matter, but it's easy enough to do it right.
    */
   private static int toMinutes(long millis) {
      return (int) ((millis < 0) ? (millis - 30000) / 60000
                                 : (millis + 30000) / 60000);
      // careful, negatives are allowed here and they round differently
   }

   public static void adjust(LinkedList storeHours, int deltaOpen, int deltaClose) {

      if (deltaClose-deltaOpen > 0) { // (*) see what transformation type
         widen (storeHours,deltaOpen,deltaClose);
      } else {
         narrow(storeHours,deltaOpen,deltaClose);
      }
   }

   /**
    * Here x = deltaClose-deltaOpen must be negative or zero.
    */
   public static void narrow(LinkedList storeHours, int deltaOpen, int deltaClose) {

      if (    isAlwaysClosed(storeHours)
           || isAlwaysOpen  (storeHours) ) return; // don't need to catch closed, but let's do it anyway

      Iterator i = storeHours.iterator();
      while (i.hasNext()) {
         StoreHours hours = (StoreHours) i.next();

         if (narrow(hours,deltaOpen,deltaClose)) i.remove(); // can lead to being always closed
      }
   }

   /**
    * Here x = deltaClose-deltaOpen must be positive or zero.
    */
   public static void widen(LinkedList storeHours, int deltaOpen, int deltaClose) {

      if (    isAlwaysClosed(storeHours)
           || isAlwaysOpen  (storeHours) ) return; // special cases handled specially

      StoreHours first = null;
      StoreHours prev  = null;

      Iterator i = storeHours.iterator();
      while (i.hasNext()) {
         StoreHours hours = (StoreHours) i.next();

         if (first == null) {
            first = hours;
         } else {
            if (widen(prev,hours,deltaOpen,deltaClose)) {
               prev.close = hours.close;
               i.remove();
               hours = prev; // kluge to make prev = hours have no effect
            }
         }

         prev = hours;
      }

      if (widen(prev,first,deltaOpen,deltaClose)) {

         // we want to remove first, but this can lead to being always open,
         // which needs special handling.
         // note the list always has at least one element here -- we're not
         // always closed, and there's no way i.remove can remove the first
         // since we don't enter that case.

         if (storeHours.size() > 1) {
            prev.close = first.close;
            storeHours.removeFirst();
         } else {
            prev.close = prev.open; // prev and first are same, make always open
         }
      }
   }

   private static boolean narrow(StoreHours h1, int deltaOpen, int deltaClose) {

      int len = Convert.deltaTOW(h1.close,h1.open);
      if (len > deltaOpen-deltaClose) { // (*) compare len to amount of narrowing, which is -x
         h1.open  = Convert.addTOW(h1.open, deltaOpen );
         h1.close = Convert.addTOW(h1.close,deltaClose);
         return false;
      } else {
         return true; // remove interval
      }
   }

   private static boolean widen(StoreHours h1, StoreHours h2, int deltaOpen, int deltaClose) {

      int len = Convert.deltaTOW(h2.open,h1.close);
      if (len > deltaClose-deltaOpen) { // (*) compare len to amount of widening, which is x
         h1.close = Convert.addTOW(h1.close,deltaClose);
         h2.open  = Convert.addTOW(h2.open, deltaOpen );
         return false;
      } else {
         return true; // remove gap between intervals
      }
   }

   public static String describe(LinkedList storeHours) {

      if (isAlwaysClosed(storeHours)) return Text.get(StoreHours.class,"s1");
      if (isAlwaysOpen  (storeHours)) return Text.get(StoreHours.class,"s2");

      StringBuffer b = new StringBuffer();
      String dash = ' ' + Text.get(StoreHours.class,"s3") + ' ';

      boolean first = true;

      Iterator i = storeHours.iterator();
      while (i.hasNext()) {
         StoreHours hours = (StoreHours) i.next();

         if (first) {
            first = false;
         } else {
            b.append('\n');
         }

         b.append(Convert.fromTOW(hours.open));
         b.append(dash);
         b.append(Convert.fromTOW(hours.close));
      }

      return b.toString();
   }

   /**
    * Check whether t1 is between t0 and t2, or, rather, is contained
    * in the interval [t0,t2).
    * Note that t0 and t2 are different by validation.
    */
   private static boolean between(int t0, int t1, int t2) {
      if (t0 < t2) { // normal case

         return (t0 <= t1 && t1 < t2);

      } else { // wraparound case

         return (t0 <= t1 || t1 < t2);
      }
   }

   /**
    * @return The next open TOW, or -1 if the store is already open.
    */
   private static int getNextOpen(LinkedList storeHours, int tow) {

      // the AC/AO check is done outside, that's why this is private

      StoreHours first = null;
      StoreHours prev  = null;

      Iterator i = storeHours.iterator();
      while (i.hasNext()) {
         StoreHours hours = (StoreHours) i.next();

         if (first == null) {
            first = hours;
         } else {
            if (between(prev.close,tow,hours.open)) return hours.open;
         }

         if (between(hours.open,tow,hours.close)) return -1;

         prev = hours;
      }

      return first.open; // we checked all intervals except this one
   }

   /**
    * The main calculation code, cf. RestartThread.getWaitInterval
    * and BandwidthUtil.regulate.
    *
    * @return The wait interval to the next open time, or zero if no wait needed.
    */
   public static long getWaitInterval(LinkedList storeHours) {

      // in practice these will be adjusted hours, but to us it's just a list

      if (    isAlwaysClosed(storeHours)
           || isAlwaysOpen  (storeHours) ) return 0; // special cases handled specially
      // you shouldn't be calling this if the store is always closed,
      // but handle it anyway so that we don't loop forever or crash or something

   // get current TOW value

      Calendar cNow = Calendar.getInstance();

      int dow = cNow.get(Calendar.DAY_OF_WEEK) - 1;
      // as noted in Convert, Calendar is off by 1 from convenient values

      int h = cNow.get(Calendar.HOUR_OF_DAY);
      int m = cNow.get(Calendar.MINUTE);

      int tNow = h*60 + m;
      int towNow = dow*Convert.MINUTES_PER_DAY + tNow;

   // get the next open TOW

      int towNext = getNextOpen(storeHours,towNow);
      if (towNext == -1) return 0;

   // work out the calendar value

      int y = cNow.get(Calendar.YEAR);
      int M = cNow.get(Calendar.MONTH);
      int d = cNow.get(Calendar.DAY_OF_MONTH);

      int dowNext = towNext / Convert.MINUTES_PER_DAY;
      int   tNext = towNext % Convert.MINUTES_PER_DAY;

      int hNext = tNext / 60;
      int mNext = tNext % 60;

      int delta = dowNext-dow;
      if (    delta <  0
           || delta == 0 && tNext < tNow ) delta += 7;
      // normally 0-6, but 7 is possible if the store is only open once a week

      int dNext = d + delta;
      // note that out-of-range values of day are allowed, e.g. July 32.

      Calendar cNext = Calendar.getInstance();
      cNext.clear(); // millis
      cNext.set(y,M,dNext,hNext,mNext,0);

   // done!

      long wait = cNext.getTimeInMillis() - cNow.getTimeInMillis();
      if (wait < 0) wait = 0; // impossible, but catch it anyway

      return wait;
   }

}

