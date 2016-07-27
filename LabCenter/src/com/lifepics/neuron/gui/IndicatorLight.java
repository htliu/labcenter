/*
 * IndicatorLight.java
 */

package com.lifepics.neuron.gui;

import com.lifepics.neuron.thread.ExtendedListener;
import com.lifepics.neuron.thread.SubsystemController;
import com.lifepics.neuron.thread.SubsystemListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * An indicator light component.
 */

public class IndicatorLight implements SubsystemListener, ExtendedListener {

// --- fields ---

   private Light light;

// --- construction and destruction ---

   public IndicatorLight(SubsystemController subsystem, int style) {
      light = Style.style.getLight(this,style);
      if (subsystem != null) subsystem.addListener(this);
   }

   public JComponent getComponent() { return light.getComponent(); }

// --- implementation of SubsystemListener ---

   public void report(int state, String reason, boolean hasErrors) {
      report(state,reason,hasErrors,true);
   }

   public void report(int state, String reason, boolean hasErrors, boolean show) {

      int lstate = show ? Light.LIGHT_ERROR : Light.LIGHT_WARNING;
      // a default value that we want in many cases

      switch (state) {
      case SubsystemListener.STOPPED:
         lstate = Light.LIGHT_OFF;
         break;
      case SubsystemListener.RUNNING:
         if ( ! hasErrors ) lstate = Light.LIGHT_NORMAL;
         // else keep the default
         break;
      case SubsystemListener.PAUSED_NETWORK:
         // keep the default
         break;
      case SubsystemListener.PAUSED_WAIT:
         if ( ! hasErrors ) lstate = Light.LIGHT_WARNING;
         // else keep the default
         break;
      case SubsystemListener.ABORTED:
         lstate = Light.LIGHT_ERROR;
         break;
      default:
         throw new IllegalArgumentException();
      }

      light.setState(lstate);
   }

}

