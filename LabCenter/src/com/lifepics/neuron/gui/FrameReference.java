/*
 * FrameReference.java
 */

package com.lifepics.neuron.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A class that keeps track of a frame.
 */

public class FrameReference {

   private JFrame frame;

   public boolean activate() {
      if (frame != null) {
         User.toFront(frame,/* alert = */ false);
         return true;
      } else {
         return false;
      }
   }

   /**
    * Only call this when activate has returned false.
    */
   public void run(JFrame frame) {
      this.frame = frame;

      frame.addWindowListener(new WindowAdapter() { public void windowClosed(WindowEvent e) { clear(); } });
      // if the frame calls dispose directly (as the history dialogs do
      // when you press the close button) then windowClosing never gets called.

      frame.setVisible(true);
   }

   private void clear() {
      frame = null;
   }

}

