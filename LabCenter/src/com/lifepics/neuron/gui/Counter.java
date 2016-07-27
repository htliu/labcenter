/*
 * Counter.java
 */

package com.lifepics.neuron.gui;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.table.View;
import com.lifepics.neuron.table.ViewListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A component that displays a count of items in a {@link View}.
 */

public class Counter extends JLabel implements ViewListener {

// --- fields ---

   private View view;
   private boolean vanish;

   private Color colorNormal;
   private Color colorAlert;

// --- construction ---

   public Counter(View view) {
      this(view,null,true,false);
   }

   public Counter(View view, Color colorAlert) {
      this(view,colorAlert,true,false);
   }

   public Counter(View view, Color colorAlert, boolean large) {
      this(view,colorAlert,large,false);
   }

   /**
    * @param colorAlert The background color to use when the counter is nonzero,
    *                   or null if the background color isn't supposed to change.
    * @param vanish True if the counter should use the empty string instead of 0.
    */
   public Counter(View view, Color colorAlert, boolean large, boolean vanish) {
      this.view = view;
      this.vanish = vanish;

      colorNormal = getBackground();
      this.colorAlert = colorAlert;

      if (colorAlert != null) setOpaque(true);
      setHorizontalAlignment(CENTER);

      if (large) {
         Font font = getFont();
         setFont(font.deriveFont(2*font.getSize2D()));
      }

      view.addListener(this);
      reportChange(); // otherwise no initial update
   }

// --- implementation of ViewListener ---

   public void reportInsert(int j, Object o) { reportChange(); }
   public void reportUpdate(int i, int j, Object o) {}
   public void reportDelete(int i) { reportChange(); }

   public void reportChange() {
      int size = view.size();
      setText( (vanish && size == 0) ? "" : Convert.fromInt(size) );
      if (colorAlert != null) setBackground( (size > 0) ? colorAlert : colorNormal );
   }

}

