/*
 * ProgressPanel.java
 */

package com.lifepics.neuron.gui;

import com.lifepics.neuron.core.Text;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A panel that displays progress information from a {@link TransferProgress} stream.
 */

public class ProgressPanel extends JPanel implements TransferProgress {

// --- fields ---

   private JProgressBar progressBar;

// --- construction ---

   public ProgressPanel() {

      // titled border clips title to fit area, we don't want that

      int d1 = Text.getInt(this,"d1");

      progressBar = new JProgressBar(JProgressBar.VERTICAL);

      GridBagHelper helper = new GridBagHelper(this);

      helper.add(0,0,new JLabel(Text.get(this,"s1")));
      helper.add(0,1,Box.createVerticalStrut(d1));
      helper.add(0,2,progressBar,GridBagHelper.fillVertical);
      helper.add(0,3,Box.createVerticalStrut(d1));

      helper.setRowWeight(2,1);
   }

// --- implementation of TransferProgress ---

   public void setProgress(double fraction, int percent) {
      progressBar.setValue(percent);
   }

   public void clearProgress() {
      progressBar.setValue(0);
   }

}

