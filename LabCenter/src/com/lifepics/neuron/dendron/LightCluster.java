/*
 * LightCluster.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.gui.IndicatorLight;

import java.util.Iterator;
import java.util.LinkedList;

import java.awt.GridLayout;
import javax.swing.JPanel;

/**
 * A mutable cluster of {@link IndicatorLight} for job formatter threads.
 */

public class LightCluster extends JPanel {

   private int style;

   public LightCluster(LinkedList formatSubsystems, int style) {
      super(new GridLayout(1,0));
      this.style = style;
      rebuild(formatSubsystems);
   }

   public void rebuild(LinkedList formatSubsystems) {
      removeAll();
      Iterator i = formatSubsystems.iterator();
      while (i.hasNext()) {
         ThreadDefinition t = (ThreadDefinition) i.next();
         add(new IndicatorLight(t.formatSubsystem,style).getComponent());
      }
   }

}

