/*
 * Blob.java
 */

package com.lifepics.neuron.gui;

import com.lifepics.neuron.core.Text;

import java.text.MessageFormat;
import java.util.MissingResourceException;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.*;

/**
 * A utility class for manipulating text blobs in the UI.
 */

public class Blob {

   /**
    * Get a text blob from a resource file.
    *
    * @param o The object for the blob.
    * @param key The base key for the blob -- if the base is "b1", the keys will be "b1_1" and so on.
    */
   public static String getBlob(Object o, String key) {
      return getBlob(o,key,null);
   }
   public static String getBlob(Object o, String key, Object[] args) {
      StringBuffer buffer = new StringBuffer();

      try {
         for (int i=1; ; i++) {
            String s = Text.get(o,key + '_' + i); // this is where exception will occur
            if (args != null) s = MessageFormat.format(s,args);

            if (i > 1) buffer.append(' ');
            buffer.append(s);
         }
      } catch (MissingResourceException e) {
      }

      return buffer.toString();
   }

   /**
    * Make a component to present a text blob as static, unselectable text.
    *
    * @param s The text blob.
    * @param title The border title, or null for untitled.
    * @param d The border margin.
    * @param columns The number of columns, i.e., the number of characters wide the component should be.
    */
   public static JComponent makeBlob(String s, String title, int d, int columns) {

      JTextArea field = new JTextArea();

      field.setEnabled(false);
      field.setDisabledTextColor(Color.black);
      field.setBackground(new JLabel().getBackground());

      field.setFont(new JLabel().getFont()); // for some reason, the default is Courier
      field.setLineWrap(true);
      field.setWrapStyleWord(true);

      field.setColumns(columns);
      field.setText(s);

      // the text area doesn't take borders into account
      // in its size calculations, so wrap it in a panel

      Border b1, b2;
      if (title != null) {
         b1 = BorderFactory.createTitledBorder(title);
         b2 = BorderFactory.createEmptyBorder(0,d,d,d);
      } else {
         b1 = BorderFactory.createEtchedBorder();
         b2 = BorderFactory.createEmptyBorder(d,d,d,d);
      }

      JPanel panel = new JPanel();
      panel.setBorder(BorderFactory.createCompoundBorder(b1,b2));
      panel.setLayout(new FlowLayout(FlowLayout.LEFT,0,0)); // remove default hgap and vgap
      panel.add(field);

      return panel;
   }

   /**
    * Make a component to present a text blob as scrollable, selectable text (but not editable).
    *
    * @param s The text blob.
    * @param rows The number of rows, i.e., the number of characters high the component should be.
    * @param columns The number of columns, i.e., the number of characters wide the component should be.
    */
   public static JComponent makeScrollBlob(String s, int rows, int columns) {

      JTextArea field = new JTextArea();

      field.setEditable(false);

      field.setFont(new JLabel().getFont()); // for some reason, the default is Courier
      field.setLineWrap(true);
      field.setWrapStyleWord(true);

      field.setRows(rows);
      field.setColumns(columns);
      field.setText(s);
      field.getCaret().setDot(0); // else it starts at the end

      JScrollPane scroll = new JScrollPane(field,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                 JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

      JLabel margin = new JLabel("  ");
      margin.setOpaque(true);
      margin.setBackground(field.getBackground());
      scroll.setRowHeaderView(margin);

      return scroll;
   }

   public static void updateScrollBlob(JComponent blob, String s) {

      JScrollPane scroll = (JScrollPane) blob;
      JTextArea field = (JTextArea) scroll.getViewport().getView();
      field.setText(s);
      field.getCaret().setDot(0); // else it starts at the end
   }

   public static JTextArea makeEditBlob(int rows, int columns) {

      JTextArea field = new JTextArea();

      field.setFont(new JLabel().getFont()); // for some reason, the default is Courier
      // line wrap defaults to false,
      // then the wrap style is academic

      field.setRows(rows);
      field.setColumns(columns);

      return field; // can't put in scroll pane yet, we need access to the actual field
   }

}

