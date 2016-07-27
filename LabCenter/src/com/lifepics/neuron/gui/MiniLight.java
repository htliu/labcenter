/*
 * MiniLight.java
 */

package com.lifepics.neuron.gui;

import com.lifepics.neuron.table.View;
import com.lifepics.neuron.table.ViewListener;
import com.lifepics.neuron.thread.SubsystemController;
import com.lifepics.neuron.thread.SubsystemListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A mini indicator light that indicates if there are items in a {@link View}
 * (and that also goes disabled when the corresponding subsystem is disabled).
 */

public class MiniLight implements ViewListener, SubsystemListener {

// --- fields ---

   private View view;

   private boolean hasAlert;
   private boolean disabled;

   private Light light;

// --- construction ---

   public MiniLight(View view, SubsystemController subsystem, int style) {
      this.view = view;

      // flags will be set by initial updates

      light = Style.style.getLight(this,style);

      view.addListener(this);
      updateView(); // otherwise no initial update
      if (subsystem != null) {
         subsystem.addListener(this);
      } else {
         disabled = false;
         update();
      }
      // the subsystem always sends initial update
   }

   public JComponent getComponent() { return light.getComponent(); }

// --- implementation of ViewListener ---

   public void reportInsert(int j, Object o) { reportChange(); }
   public void reportUpdate(int i, int j, Object o) {}
   public void reportDelete(int i) { reportChange(); }

   private void updateView() {
      hasAlert = (view.size() > 0);
   }

   public void reportChange() {
      updateView();
      update();
   }

// --- implementation of SubsystemListener ---

   public void report(int state, String reason, boolean hasErrors) {
      disabled = (state == SubsystemListener.STOPPED);
      update();
   }

// --- update ---

   private void update() {
      int lstate;
      if      (disabled) lstate = Light.LIGHT_OFF;
      else if (hasAlert) lstate = Light.LIGHT_ERROR;
      else               lstate = Light.LIGHT_NORMAL;
      light.setState(lstate);
   }

}

