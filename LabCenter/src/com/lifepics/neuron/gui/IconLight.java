/*
 * IconLight.java
 */

package com.lifepics.neuron.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A light subcomponent that shows one of several icons.
 */

public class IconLight extends JLabel implements Light {

   private Object owner;
   private Dimension size;

   public IconLight(Object owner, Dimension size) {
      this.owner = owner;
      this.size = size;

      setOpaque(true);
      setBorder(BorderFactory.createMatteBorder(1,1,1,1,Color.black));

      setMinimumSize(size);
      setMaximumSize(size);
      setPreferredSize(size);
   }

   public JComponent getComponent() { return this; }

   public void setState(int lstate) {
      String name;

      switch (lstate) {
      case LIGHT_OFF:      name = null;                    break;
      case LIGHT_NORMAL:   name = "indicator-green.gif";   break;
      case LIGHT_WARNING:  name = "indicator-yellow.gif";  break;
      case LIGHT_ERROR:    name = "indicator-red.gif";     break;
      default:             throw new IllegalArgumentException();
      }

      setIcon((name == null) ? null : Graphic.getIcon(name,size));
   }

}

