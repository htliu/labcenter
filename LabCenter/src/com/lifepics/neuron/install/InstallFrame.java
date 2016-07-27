/*
 * InstallFrame.java
 */

package com.lifepics.neuron.install;

import com.lifepics.neuron.core.Pop;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.gui.Graphic;
import com.lifepics.neuron.gui.GridBagHelper;
import com.lifepics.neuron.gui.Style;
import com.lifepics.neuron.gui.SubsystemPanel;
import com.lifepics.neuron.gui.TransferPanel;
import com.lifepics.neuron.gui.TransferProgress;
import com.lifepics.neuron.misc.AppUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * The main frame window for the install application.
 */

public class InstallFrame extends JFrame {

// --- fields ---

   private AppUtil.ControlInterface control;

// --- construction ---

   public InstallFrame(AppUtil.ControlInterface control, InstallSubsystem subsystem) {
      super(Text.get(InstallFrame.class,"s1"));

      this.control = control;

      setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
      Graphic.setFrameIcon(this);
      addWindowListener(new WindowAdapter() { public void windowClosing(WindowEvent e) { doWindowClose(); } });

      setResizable(false);

   // panels

      SubsystemPanel panelStatus = new SubsystemPanel(this,subsystem,null);

      TransferProgress progressSink = new TransferProgress() {
         public void setProgress(double fraction, int percent) {}
         public void clearProgress() {}
      };

      TransferPanel panelTransfer = new TransferPanel(Text.get(this,"s2"),null,progressSink);
      subsystem.setTransferListener(panelTransfer);

   // layout

      JPanel panelMain = new JPanel();
      GridBagHelper helper = new GridBagHelper(panelMain);

      int dp = Style.style.getPanelGap();

      helper.addFill(1,1,Style.style.adjustPanel(panelStatus));
      helper.addFill(1,3,Style.style.adjustPanel(panelTransfer));

      helper.add(0,0,Box.createRigidArea(new Dimension(dp,dp)));

      helper.add(2,0,Box.createHorizontalStrut(dp));

      helper.add(0,2,Box.createVerticalStrut(dp));
      helper.add(0,4,Box.createVerticalStrut(dp));

   // finish up

      getContentPane().add(panelMain,BorderLayout.CENTER);

      pack();
      setLocationRelativeTo(null); // center on screen
   }

// --- commands ---

   private void doWindowClose() {

      boolean confirmed = Pop.confirm(this,Text.get(this,"e1"),Text.get(this,"s3"));
      if ( ! confirmed ) return;

      control.exit();
   }

}

