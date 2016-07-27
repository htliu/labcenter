/*
 * ButtonHelper.java
 */

package com.lifepics.neuron.gui;

import com.lifepics.neuron.core.Text;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.*;

/**
 * A utility class for constructing standard centered buttons.
 */

public class ButtonHelper {

   private static int d1 = Text.getInt(ButtonHelper.class,"d1");
   private static int d2 = Text.getInt(ButtonHelper.class,"d2");

// --- class for the general case ---

   private JPanel buttons;

   public ButtonHelper() {
      buttons = new JPanel();
      buttons.setLayout(new BoxLayout(buttons,BoxLayout.X_AXIS));
   }

   public ButtonHelper add(Component c) { // button or whatever
      buttons.add(c);
      return this;
   }

   public ButtonHelper addButton(String s, ActionListener a) {
      JButton button = new JButton(s);
      button.addActionListener(a);
      buttons.add(button);
      return this;
   }

   public ButtonHelper addGlue() {
      buttons.add(Box.createHorizontalGlue());
      return this;
   }

   public ButtonHelper addStrut() {
      buttons.add(Box.createHorizontalStrut(d1));
      return this;
   }

   public JPanel end() {
      return buttons;
   }

// --- static functions for common cases ---

   public static JPanel makeButtons(JButton b1) {
      return new ButtonHelper().addGlue().add(b1).addGlue().end();
   }
   public static JPanel makeButtons(JButton b1, JButton b2) {
      return new ButtonHelper().addGlue().add(b1).addStrut().add(b2).addGlue().end();
   }
   public static JPanel makeButtons(JButton b1, JButton b2, JButton b3) {
      return new ButtonHelper().addGlue().add(b1).addStrut().add(b2).addStrut().add(b3).addGlue().end();
   }

   public static void doLayout(RootPaneContainer c, JComponent fields, JPanel buttons, JButton buttonDefault) {
      doLayout(c,fields,buttons,buttonDefault,/* fieldsBorder = */ true);
   }
   public static void doLayout(RootPaneContainer c, JComponent fields, JPanel buttons, JButton buttonDefault, boolean fieldsBorder) {

      if (fieldsBorder) {
         Border bNew = BorderFactory.createEmptyBorder(d2,d2,0,d2);
         Border bOld = fields.getBorder();
         if (bOld != null) bNew = BorderFactory.createCompoundBorder(bNew,bOld);
         fields.setBorder(bNew);
      }
      buttons.setBorder(BorderFactory.createEmptyBorder(d2,d2,d2,d2));

      c.getContentPane().add(fields,BorderLayout.CENTER);
      c.getContentPane().add(buttons,BorderLayout.SOUTH);

      if (buttonDefault != null) c.getRootPane().setDefaultButton(buttonDefault);
   }

}

