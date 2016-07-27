/*
 * MenuUtil.java
 */

package com.lifepics.neuron.gui;

import com.lifepics.neuron.core.Text;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Utility functions for constructing menus.
 */

public class MenuUtil {

   private static final char NO_MNEMONIC = '.';
   private static final char ACCELERATOR = '|';

   public static JMenu makeMenu(JMenuBar menuBar, Object o, String key) {
      String s = Text.get(o,key);

      JMenu menu = new JMenu(s.substring(1));

      char c = s.charAt(0);
      if (c != NO_MNEMONIC) {
         menu.setMnemonic(c);
      }

      menuBar.add(menu);
      return menu;
   }

   public static JMenuItem makeItem(JMenu menu, Object o, String key, ActionListener listener) {
      return makeItem(menu,Text.get(o,key),listener);
   }
   public static JMenuItem makeItem(JMenu menu, String s, ActionListener listener) {

      KeyStroke keyStroke = null;
      int i = s.indexOf(ACCELERATOR);
      if (i != -1) {
          keyStroke = KeyStroke.getKeyStroke(s.substring(i+1)); // will stay null if invalid
          s = s.substring(0,i);
      }

      JMenuItem item = new JMenuItem(s.substring(1));

      char c = s.charAt(0);
      if (c != NO_MNEMONIC) {
         item.setMnemonic(c);
      }

      if (keyStroke != null) {
         item.setAccelerator(keyStroke);
      }

      item.addActionListener(listener);

      menu.add(item);
      return item; // convenience
   }

   public static String noMnemonic(String s) {
      return NO_MNEMONIC + s;
   }

   public static void truncate(JMenu menu, int index) {
      while (menu.getItemCount() > index) {
         menu.remove(index);
      }
   }

}

