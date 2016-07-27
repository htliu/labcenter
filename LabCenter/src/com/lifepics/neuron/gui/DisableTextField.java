/*
 * DisableTextField.java
 */

package com.lifepics.neuron.gui;

import java.awt.Color;
import javax.swing.JLabel;
import javax.swing.JTextField;

/**
 * A {@link JTextField} that alters the look and feel to make itself more visibly disabled.
 */

public class DisableTextField extends JTextField {

   private static Color colorEnabled = new JTextField().getBackground();
   private static Color colorDisabled = new JLabel().getBackground();

   public DisableTextField(int columns) {
      super(columns);
      setDisabledTextColor(Color.black);
   }

   public DisableTextField(String text) {
      super(text);
      setDisabledTextColor(Color.black);
   }

   public void setEnabled(boolean enabled) {
      super.setEnabled(enabled);
      setBackground(enabled ? colorEnabled : colorDisabled);
   }

}

