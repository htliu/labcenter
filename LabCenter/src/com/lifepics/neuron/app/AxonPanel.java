/*
 * AxonPanel.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.axon.RollUtil;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.gui.GridBagHelper;
import com.lifepics.neuron.gui.GridColumn;
import com.lifepics.neuron.gui.Style;
import com.lifepics.neuron.gui.SubsystemPanel;
import com.lifepics.neuron.gui.TransferPanel;
import com.lifepics.neuron.gui.TransferProgress;
import com.lifepics.neuron.gui.ViewPanel;
import com.lifepics.neuron.table.Table;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * The main panel for the Axon side of things.
 */

public class AxonPanel extends JPanel {

// --- fields ---

   private Global global;
   private Frame owner;

   private Component scanThreadPanel;
   private Component scanThreadGap;
   private Component kioskThreadPanel;
   private Component kioskThreadGap;

// --- construction ---

   public AxonPanel(Global global, Frame owner, SubsystemControl subsystemControl, JPanel newsPanel) {

      this.global = global;
      this.owner = owner;

   // status area

      SubsystemPanel panelStatus = subsystemControl.register(new SubsystemPanel(owner,global.uploadSubsystem,null,global.errorWindowUpload));
      SubsystemPanel panelScan   = subsystemControl.register(new SubsystemPanel(owner,global.scanSubsystem,Text.get(this,"s11")));
      SubsystemPanel panelKiosk  = subsystemControl.register(new SubsystemPanel(owner,global.localImageSubsystem,Text.get(this,"s13")));

   // views

      Table table = global.rollTable;

      ViewPanel.DoubleClick doubleClick = openRoll();

      GridColumn[] cols = new GridColumn[] { RollUtil.colRollID, RollUtil.colStatusHold };
      int rows = 2; // same for all views

      ViewPanel panelOpen   = new ViewPanel(Text.get(this,"s1"),Text.get(this,"s2"),
                                            table.select(Selectors.rollOpen,  RollUtil.orderRollID,true,true),
                                            cols,doubleClick,2,rows);

      ViewPanel panelClosed = new ViewPanel(Text.get(this,"s3"),null,
                                            table.select(Selectors.rollClosed,RollUtil.orderRollID,true,true),
                                            cols,doubleClick,2,rows);

      panelOpen.addButton(0,0,Text.get(this,"s4"),newRoll());
      panelOpen.addButton(1,0,Text.get(this,"s5"),deleteRolls());
      panelOpen.addButton(0,1,Text.get(this,"s6"),holdRolls());
      panelOpen.addButton(1,1,Text.get(this,"s7"),releaseRolls());

      panelClosed.addButton(0,0,Text.get(this,"s8"),recoverRolls());
      panelClosed.addButton(1,1,Text.get(this,"s12"),releaseRolls()); // no other way to unpause TPLI errors

   // progress bar

      JPanel panelProgress = Style.style.getProgressPanel();

   // status area

      TransferPanel panelTransfer = new TransferPanel(Text.get(this,"s9"),Text.get(this,"s10"),(TransferProgress) panelProgress);
      global.uploadSubsystem.setTransferListener(panelTransfer);

   // finish up

      int dp = Style.style.getPanelGap();

      GridBagHelper helper = new GridBagHelper(this);

      helper.addSpanFill(1,1,5,Style.style.adjustPanel(panelStatus));
      helper.addSpanFill(1,3,5, scanThreadPanel = Style.style.adjustPanel(panelScan ));
      helper.addSpanFill(1,5,5,kioskThreadPanel = Style.style.adjustPanel(panelKiosk));
      helper.add        (1,7,  Style.style.adjustPanel(panelOpen),    GridBagHelper.fillBoth);
      helper.add        (3,7,  Style.style.adjustPanel(panelProgress),GridBagHelper.fillVertical);
      helper.add        (5,7,  Style.style.adjustPanel(panelClosed),  GridBagHelper.fillBoth);
      helper.addSpanFill(1,9,5,Style.style.adjustPanel(panelTransfer));
      if (newsPanel != null) helper.addSpanFill(1,11,5,Style.style.adjustPanel(newsPanel));

      helper.add(0,0,Box.createRigidArea(new Dimension(dp,dp)));

      helper.add(2,0,Box.createHorizontalStrut(dp));
      helper.add(4,0,Box.createHorizontalStrut(dp));
      helper.add(6,0,Box.createHorizontalStrut(dp));

      helper.add(0,2,Box.createVerticalStrut(dp));
      helper.add(0,4, scanThreadGap = Box.createVerticalStrut(dp));
      helper.add(0,6,kioskThreadGap = Box.createVerticalStrut(dp));
      helper.add(0,8,Box.createVerticalStrut(dp));
      helper.add(0,10,Box.createVerticalStrut(dp));
      if (newsPanel != null) helper.add(0,12,Box.createVerticalStrut(dp));

      helper.setRowWeight(7,1);
      helper.setColumnWeight(1,1);
      helper.setColumnWeight(5,1);
   }

   public void reinit(boolean showScanThread, boolean showKioskThread) {
      scanThreadPanel.setVisible(showScanThread);
      scanThreadGap  .setVisible(showScanThread);
      kioskThreadPanel.setVisible(showKioskThread);
      kioskThreadGap  .setVisible(showKioskThread);
   }

// --- bindings ---

   public ViewPanel.ButtonPress newRoll() {
      return new ViewPanel.ButtonPress() { public void run(Object[] o) { global.rollManager.doNew(owner,global.control); } }; // ignore o
   }

   public ViewPanel.DoubleClick openRoll() {
      return new ViewPanel.DoubleClick() { public void run(Object   o) { global.rollManager.doOpen(owner,o,global.control); } };
   }

   public ViewPanel.ButtonPress holdRolls() {
      return new ViewPanel.ButtonPress() { public void run(Object[] o) { global.rollManager.doHold(owner,o,global.uploadSubsystem); } };
   }

   public ViewPanel.ButtonPress releaseRolls() {
      return new ViewPanel.ButtonPress() { public void run(Object[] o) { global.rollManager.doRelease(owner,o); } };
   }

   public ViewPanel.ButtonPress deleteRolls() {
      return new ViewPanel.ButtonPress() { public void run(Object[] o) { global.rollManager.doDelete(owner,o); } };
   }

   public ViewPanel.ButtonPress recoverRolls() {
      return new ViewPanel.ButtonPress() { public void run(Object[] o) { global.rollManager.doRecover(owner,o); } };
   }

}

