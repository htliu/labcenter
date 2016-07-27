/*
 * InitialFocus.java
 */

package com.lifepics.neuron.gui;

import java.awt.Component;
import java.awt.Window;
import javax.swing.LayoutFocusTraversalPolicy;

/**
 * A utility class for setting the initial focus in a dialog.
 * It still amazes me that you can't just set what component
 * has the focus, but oh well.
 */

public class InitialFocus extends LayoutFocusTraversalPolicy {

   private Component c;
   public InitialFocus(Component c) { this.c = c; }

   public Component getInitialComponent(Window window) { return c; }

}

