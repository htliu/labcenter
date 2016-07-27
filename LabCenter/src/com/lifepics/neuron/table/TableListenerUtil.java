/*
 * TableListenerUtil.java
 */

package com.lifepics.neuron.table;

import java.util.LinkedList;

import java.awt.EventQueue;

/**
 * A utility class containing objects that manipulate {@link TableListener} notifications.
 */

public class TableListenerUtil {

// --- encapsulated notifications ---

   private static class ReportInsert implements Runnable {
      private TableListener listener;
      private Object o;
      public ReportInsert(TableListener listener, Object o) { this.listener = listener; this.o = o; }
      public void run() { listener.reportInsert(o); }
   }

   private static class ReportUpdate implements Runnable {
      private TableListener listener;
      private Object o;
      public ReportUpdate(TableListener listener, Object o) { this.listener = listener; this.o = o; }
      public void run() { listener.reportUpdate(o); }
   }

   private static class ReportDelete implements Runnable {
      private TableListener listener;
      private String key;
      public ReportDelete(TableListener listener, String key) { this.listener = listener; this.key = key; }
      public void run() { listener.reportDelete(key); }
   }

   private static class ReportRefresh implements Runnable {
      private TableListener listener;
      public ReportRefresh(TableListener listener) { this.listener = listener; }
      public void run() { listener.reportRefresh(); }
   }

// --- accumulator ---

   /**
    * An object that accumulates {@link TableListener} notifications for later execution.
    */
   public static class Accumulator implements TableListener {

      private TableListener listener;
      private LinkedList list;

      public Accumulator(TableListener listener) {
         this.listener = listener;
         list = new LinkedList();
      }

      public void reportInsert(Object o) {
         list.add(new ReportInsert(listener,o));
      }

      public void reportUpdate(Object o) {
         list.add(new ReportUpdate(listener,o));
      }

      public void reportDelete(String key) {
         list.add(new ReportDelete(listener,key));
      }

      public void reportRefresh() {
         list.add(new ReportRefresh(listener));
      }

      public void run() {
         while ( ! list.isEmpty() ) {
            ((Runnable) list.removeFirst()).run();
         }
      }
   }

// --- transferrer ---

   /**
    * An object that transfers {@link TableListener} notifications into the UI dispatch thread.
    */
   public static class Transferrer implements TableListener {

      private TableListener listener;

      public Transferrer(TableListener listener) {
         this.listener = listener;
      }

      public void reportInsert(Object o) {
         EventQueue.invokeLater(new ReportInsert(listener,o));
      }

      public void reportUpdate(Object o) {
         EventQueue.invokeLater(new ReportUpdate(listener,o));
      }

      public void reportDelete(String key) {
         EventQueue.invokeLater(new ReportDelete(listener,key));
      }

      public void reportRefresh() {
         EventQueue.invokeLater(new ReportRefresh(listener));
      }
   }

}

