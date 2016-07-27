/*
 * QueueCombo.java
 */

package com.lifepics.neuron.dendron;

import java.util.Iterator;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

/**
 * A combo box that shows queue names, plus optionally a special name that maps to a special value.
 */

public class QueueCombo extends JComboBox {

// --- fields ---

   /**
    * The special name, which can be just about anything, depending on the situation.
    */
   private String specialName;

   /**
    * The special value, which will be either "" or null, depending on the situation.
    */
   private String specialValue;

// --- item class ---

   // this is annoying ... when you set the selection in a combo box,
   // it compares items using equals, not object identity.
   // so, if you don't use an indirection, you compare queue <i>data</i>.

   // on the plus side, the same indirection can be used on toString,
   // making the custom renderer unnecessary.

   private static class Item {

      public Queue queue;
      public Item(Queue queue) { this.queue = queue; }

      // no need to override equals, object identity is the default
      public String toString() { return queue.name; }
   }

// --- construction ---

   private static ComboBoxModel constructModel(QueueList queueList, String specialName) {

      DefaultComboBoxModel model = new DefaultComboBoxModel();
      // might as well use addElement as construct an array

      if (specialName != null) model.addElement(specialName);

      Iterator i = queueList.queues.iterator();
      while (i.hasNext()) {
         model.addElement(new Item((Queue) i.next()));
      }

      return model;
   }

   public QueueCombo(QueueList queueList, String specialName, String specialValue) {
      super(constructModel(queueList,specialName));

      this.specialName = specialName;
      this.specialValue = specialValue;
   }

// --- modification ---

   private boolean hasSpecial() {
      return (       getItemCount() > 0
               && ! (getItemAt(0) instanceof Item) );
   }

   public void reinit(QueueList queueList) {

      // save current value
      String value = get();

      setModel(constructModel(queueList,specialName));

      // restore current value
      if ( ! putTry(value) ) {
         if (getItemCount() > 0) setSelectedIndex(0);
         // else there can't be any selection anyway
      }
   }

// --- transfer ---

   public boolean putTry(String value) {

      // we don't have access to the queue list at this point,
      // which is good, because who knows, maybe it's changed.
      // so, instead, we have to search the combo values.

      // we know the special name is always in first position, if present.

      int i = 0;

      if (hasSpecial()) {

         if (value == null || value.equals("")) { setSelectedIndex(i); return true; }
            // for a new object the value might start as either one
         i++;
      }

      for (; i<getItemCount(); i++) {
         Item item = (Item) getItemAt(i);
         if (item.queue.queueID.equals(value)) { setSelectedIndex(i); return true; } // queueID never null
      }

      return false;
   }

   public void put(String value) {
      if ( ! putTry(value) ) throw new IndexOutOfBoundsException();
   }

   public String get() {
      // we know there's at most one non-queue object, which is the special one
      Object o = getSelectedItem();
      return (o instanceof Item) ? ((Item) o).queue.queueID : specialValue;
   }

   public Queue getQueueObject() {
      // we know there's at most one non-queue object, which is the special one
      Object o = getSelectedItem();
      return (o instanceof Item) ? ((Item) o).queue : null;
   }

}

