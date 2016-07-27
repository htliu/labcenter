/*
 * MinimumSize.java
 */

package com.lifepics.neuron.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * A little class for enforcing minimum sizes of resizable windows.
 */

public class MinimumSize extends ComponentAdapter {

   private Component component;
   private Dimension minimum;

   public MinimumSize(Component component) {
      this.component = component;
      minimum = component.getSize();
      component.addComponentListener(this);
   }

   public void update() {
      minimum = component.getSize();
   }

   public void componentResized(ComponentEvent e) {
      Dimension size = component.getSize();

      boolean changed = false;
      if (size.width  < minimum.width ) { size.width  = minimum.width;  changed = true; }
      if (size.height < minimum.height) { size.height = minimum.height; changed = true; }

      if (changed) component.setSize(size);

      // it would be better to apply the minimum size
      // while the user is resizing,
      // but I can't figure out how to control that
   }

}

