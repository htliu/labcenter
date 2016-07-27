/*
 * SubsystemControl.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.gui.SubsystemPanel;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * A small utility class for managing subsystem panels.
 */

public class SubsystemControl {

   private LinkedList panels;

   public SubsystemControl() {
      panels = new LinkedList();
   }

   public SubsystemPanel register(SubsystemPanel panel) {
      panels.add(panel);
      return panel; // convenience
   }

   public void unregister(SubsystemPanel panel) {
      panels.remove(panel);
   }

   public void setPreventStopStart(boolean preventStopStart) {
      Iterator i = panels.iterator();
      while (i.hasNext()) {
         ((SubsystemPanel) i.next()).setPreventStopStart(preventStopStart);
      }
   }

}

