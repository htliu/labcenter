/*
 * TransactionGroup.java
 */

package com.lifepics.neuron.net;

import com.lifepics.neuron.core.Text;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * A transaction that consists of a group of other transactions.
 * The result is not what you might expect ... if one transaction fails,
 * the whole fails, and will be retried, but there's no rollback at all.
 */

public class TransactionGroup extends Transaction {

// --- fields ---

   private LinkedList list;

// --- construction ---

   public TransactionGroup() {
      list = new LinkedList();
   }

   public void add(Transaction t) {
      list.add(t);
   }

// --- implementation of Transaction ---

   private static String andString = ' ' + Text.get(TransactionGroup.class,"s1") + ' ';

   public String describe() {
      StringBuffer buffer = new StringBuffer();

      boolean first = true;

      Iterator i = list.iterator();
      while (i.hasNext()) {
         Transaction t = (Transaction) i.next();

         if (first) first = false;
         else buffer.append(andString);

         buffer.append(t.describe());
      }

      return buffer.toString();
   }

   public boolean run(PauseCallback callback) throws Exception {

      Iterator i = list.iterator();
      while (i.hasNext()) {
         Transaction t = (Transaction) i.next();

         if ( ! t.run(callback) ) return false; // thread is stopping
      }

      return true;
   }

}

