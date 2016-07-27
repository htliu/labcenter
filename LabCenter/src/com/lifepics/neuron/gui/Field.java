/*
 * Field.java
 */

package com.lifepics.neuron.gui;

import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A set of utility functions for transferring data
 * to and from various kinds of fields in the user interface.
 */

public class Field {

// --- text field ---

   public static String get(JTextField f) {
      return f.getText().trim(); // remove whitespace
   }

   public static void put(JTextField f, String s) {
      f.setText(s);
      f.getCaret().moveDot(0);

      // setText deletes the existing text and inserts the new text,
      // leaving the caret at the end, where it would cause annoying scroll behavior.
      // so, move it to the beginning ... and, as a bonus, leave the text selected.
   }

// --- nullable text field ---

   // these just replace the empty string with a null

   public static String getNullable(JTextField f) {
      String s = get(f);
      return (s.length() != 0) ? s : null;
   }

   public static void putNullable(JTextField f, String s) {
      put(f,(s != null) ? s : "");
   }

// --- check box ---

   public static boolean get(JCheckBox f) {
      return f.isSelected();
   }

   public static void put(JCheckBox f, boolean b) {
      f.setSelected(b);
   }

// --- combo box ---

   public static int get(JComboBox f, int[] values) {
      return values[f.getSelectedIndex()];
   }

   public static void put(JComboBox f, int[] values, int i) {
      f.setSelectedIndex(lookup(values,i));
   }

   public static int lookup(int[] values, int i) {
      for (int j=0; j<values.length; j++) {
         if (values[j] == i) return j;
      }
      throw new IndexOutOfBoundsException();
   }

   public static int lookupSafe(int[] values, int i) {
      for (int j=0; j<values.length; j++) {
         if (values[j] == i) return j;
      }
      return -1;
   }

// --- list ---

   // JList isn't editable, but it's still useful to be able
   // to get and put data.  list is always LinkedList<String>.

   public static LinkedList get(JList f) {
      LinkedList list = new LinkedList();
      ListModel model = f.getModel();
      int n = model.getSize();
      for (int i=0; i<n; i++) {
         list.add(model.getElementAt(i));
      }
      return list;
   }

   public static void put(JList f, LinkedList list) {
      f.setListData(list.toArray());
   }

// --- text area ---

   public static LinkedList get(JTextArea f) {

      String[] s = f.getText().split("\n",-1);
      // in this rare case the default split behavior isn't totally wrong,
      // but it's also not totally right, since we want to trim the lines
      // before deciding if they're empty.

      int n = s.length;
      for (int i=0; i<n; i++) s[i] = s[i].trim(); // remove whitespace

      while (n > 0 && s[n-1].length() == 0) n--;

      LinkedList list = new LinkedList();
      for (int i=0; i<n; i++) list.add(s[i]);

      return list;
   }

   public static void put(JTextArea f, LinkedList list) {

      StringBuffer b = new StringBuffer();
      Iterator i = list.iterator();
      while (i.hasNext()) {
         b.append((String) i.next());
         b.append("\n");
         // yes, add newline even after last entry,
         // that way it's easy to add new lines at the end.
         // don't worry .. it's reversible
         // since empty strings at end are removed.
      }

      f.setText(b.toString());
      // no need to adjust caret in this case
   }

}

