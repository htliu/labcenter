/*
 * Light.java
 */

package com.lifepics.neuron.gui;

import javax.swing.JComponent;

/**
 * A common interface for indicator light subcomponents.
 */

public interface Light {

   public static final int LIGHT_OFF     = 0;
   public static final int LIGHT_NORMAL  = 1;
   public static final int LIGHT_WARNING = 2;
   public static final int LIGHT_ERROR   = 3;

   JComponent getComponent();
   void setState(int lstate);

}

