/*
 * ReportQueue.java
 */

package com.lifepics.neuron.core;

import java.util.Date;
import java.util.LinkedList;
import java.util.logging.Level;

/**
 * A queue that holds {@link ReportRecord} objects.
 */

public class ReportQueue implements ReportInterface {

// --- fields ---

   private int size;
   private LinkedList list;

   public ReportQueue(int size) {
      this.size = size;
      list = new LinkedList();
   }

// --- placeholder class ---

   // this is what we use to provide information on discarded records.

   private static class Placeholder {

      public int count;
      public long timestampFirst;
      public long timestampLast;

      public Placeholder(ReportRecord r) {
         count = 1;
         timestampFirst = r.timestamp;
         timestampLast  = r.timestamp;
      }

      public void absorb(ReportRecord r) {
         count++;
         timestampLast  = r.timestamp;
      }

      public ReportRecord emit() {

         ReportRecord r = new ReportRecord();
         r.timestamp = timestampLast;
         r.level = Level.WARNING;
         r.message = Text.get(ReportQueue.class,"e1",new Object[] {
                                 Convert.fromInt(count),
                                 Convert.fromDateExternal(new Date(timestampFirst)),
                                 Convert.fromDateExternal(new Date(timestampLast)) });
         // rest remains null

         return r;
      }
   }

// --- add side ---

   public synchronized void report(ReportRecord r) {
      int sizeBefore = list.size();
      if (sizeBefore < size) { // still room, add
         list.add(r);
      } else { // full, add or absorb into placeholder
         Object o = list.getLast();
         if (o instanceof Placeholder) {
            ((Placeholder) o).absorb(r);
         } else {
            list.add(new Placeholder(r)); // so, length actually increases to size+1
         }
      }
   }

// --- remove side ---

   /**
    * Get a record.  This function is non-blocking because
    * (a) I don't want to maintain interrupt state here, and
    * (b) you have to check for interrupts before you wait.
    */
   public synchronized ReportRecord getRecord() {
      ReportRecord r = null;
      if (list.size() > 0) {
         Object o = list.getFirst();
         if (o instanceof Placeholder) {
            r = ((Placeholder) o).emit();
         } else {
            r = (ReportRecord) o;
         }
      }
      // else empty, return null
      return r;
   }


   /**
    * Two-stage get-remove process so that records aren't lost when there's a config change.
    */
   public synchronized void removeRecord() {
      list.removeFirst(); // not called when size 0
   }

}

