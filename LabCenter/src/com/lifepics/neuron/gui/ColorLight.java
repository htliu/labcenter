/*
 * ColorLight.java
 */

package com.lifepics.neuron.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A light subcomponent that shows an inset solid color.
 */

public class ColorLight extends JPanel implements Light {

   private static final Color gray = UIManager.getColor("control");

   private Object owner;

   public ColorLight(Object owner, Dimension size) {
      this.owner = owner;

      setOpaque(true);
      setBorder(BorderFactory.createLoweredBevelBorder());

      setMinimumSize(size);
      setMaximumSize(size);
      setPreferredSize(size);
   }

   public JComponent getComponent() { return this; }

   public void setState(int lstate) {
      Color color;

      switch (lstate) {
      case LIGHT_OFF:      color = gray;          break;
      case LIGHT_NORMAL:   color = Color.green;   break;
      case LIGHT_WARNING:  color = Color.yellow;  break;
      case LIGHT_ERROR:    color = Color.red;     break;
      default:             throw new IllegalArgumentException();
      }

      setBackground(color);
   }

}

