/*
 * DendronPanel.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.dendron.DueColorizer;
import com.lifepics.neuron.dendron.OrderUtil;
import com.lifepics.neuron.gui.GridBagHelper;
import com.lifepics.neuron.gui.GridColumn;
import com.lifepics.neuron.gui.Style;
import com.lifepics.neuron.gui.SubsystemPanel;
import com.lifepics.neuron.gui.TransferPanel;
import com.lifepics.neuron.gui.TransferProgress;
import com.lifepics.neuron.gui.ViewPanel;
import com.lifepics.neuron.table.Table;
import com.lifepics.neuron.table.View;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * The main panel for the Dendron side of things.
 */

public class DendronPanel extends JPanel {

// --- fields ---

   private Global global;
   private Frame owner;

   private View viewPrint;
   public View getViewPrint() { return viewPrint; }

// --- construction ---

   public DendronPanel(Global global, Frame owner, SubsystemControl subsystemControl) {

      this.global = global;
      this.owner = owner;

   // status area

      SubsystemPanel panelStatus = subsystemControl.register(new SubsystemPanel(owner,global.downloadSubsystem,null,global.errorWindowDownload));

   // views

      Table table = global.orderTable;

      ViewPanel.DoubleClick doubleClick = openOrder();

      GridColumn[] cols = new GridColumn[] { OrderUtil.colOrderID, OrderUtil.colStatusHold };
      int rows = 2; // same for almost all views

      viewPrint = table.select(Selectors.orderPrint,OrderUtil.orderOrderID,true,true);

      ViewPanel panelOpen   = new ViewPanel(Text.get(this,"s1"),Text.get(this,"s2"),
                                            table.select(Selectors.orderOpen,  OrderUtil.orderOrderID,true,true),
                                            cols,doubleClick,2,rows);

      ViewPanel panelPrint  = new ViewPanel(Text.get(this,"s3"),null,
                                            viewPrint,
                                            cols,doubleClick,2,3);

      ViewPanel panelClosed = new ViewPanel(Text.get(this,"s4"),null,
                                            table.select(Selectors.orderClosed,OrderUtil.orderOrderID,true,true),
                                            cols,doubleClick,2,rows);

      panelOpen  .colorize(new DueColorizer(null));
      panelPrint .colorize(new DueColorizer(null));
      panelClosed.colorize(new DueColorizer(null));

      panelOpen.addButton(0,1,Text.get(this,"s5"),holdOrders());
      panelOpen.addButton(1,1,Text.get(this,"s6"),releaseOrders());

      panelPrint.addWideButton(0,0,Text.get(this,"s13"),printOrders());

      panelPrint.addButton(1,1,Text.get(this,"s7"),completeOrders());

      String s8 = Text.get(this,"s8");
      panelOpen .addButton(1,0,s8,abortOrders());
      panelPrint.addButton(1,2,s8,abortOrders());

      String s11 = Text.get(this,"s11");
      panelPrint .addButton(0,1,s11,viewInvoice());
      panelClosed.addButton(0,0,s11,viewInvoice());

      String s12 = Text.get(this,"s12");
      panelPrint .addButton(0,2,s12,viewLabel());
      panelClosed.addButton(0,1,s12,viewLabel());

   // progress bar

      JPanel panelProgress = Style.style.getProgressPanel();

   // status area

      TransferPanel panelTransfer = new TransferPanel(Text.get(this,"s9"),Text.get(this,"s10"),(TransferProgress) panelProgress,/* hideTotalSize = */ true);
         // hide total size because with Opie we don't know it any more
         // old-cart orders will still have a valid total size for a while, but that will pass, not worth dealing with
      global.downloadSubsystem.setTransferListener(panelTransfer);

   // finish up

      int dp = Style.style.getPanelGap();

      GridBagHelper helper = new GridBagHelper(this);

      helper.addSpanFill(1,1,7,Style.style.adjustPanel(panelStatus));
      helper.add        (1,3,  Style.style.adjustPanel(panelOpen),    GridBagHelper.fillBoth);
      helper.add        (3,3,  Style.style.adjustPanel(panelProgress),GridBagHelper.fillVertical);
      helper.add        (5,3,  Style.style.adjustPanel(panelPrint),   GridBagHelper.fillBoth);
      helper.add        (7,3,  Style.style.adjustPanel(panelClosed),  GridBagHelper.fillBoth);
      helper.addSpanFill(1,5,7,Style.style.adjustPanel(panelTransfer));

      helper.add(0,0,Box.createRigidArea(new Dimension(dp,dp)));

      helper.add(2,0,Box.createHorizontalStrut(dp));
      helper.add(4,0,Box.createHorizontalStrut(dp));
      helper.add(6,0,Box.createHorizontalStrut(dp));
      helper.add(8,0,Box.createHorizontalStrut(dp));

      helper.add(0,2,Box.createVerticalStrut(dp));
      helper.add(0,4,Box.createVerticalStrut(dp));
      helper.add(0,6,Box.createVerticalStrut(dp));

      helper.setRowWeight(3,1);
      helper.setColumnWeight(1,1);
      helper.setColumnWeight(5,1);
      helper.setColumnWeight(7,1);
   }

// --- bindings ---

   public ViewPanel.DoubleClick openOrder() {
      return new ViewPanel.DoubleClick() { public void run(Object   o) { global.orderManager.doOpen(owner,o); } };
   }

   public ViewPanel.ButtonPress holdOrders() {
      return new ViewPanel.ButtonPress() { public void run(Object[] o) { global.orderManager.doHold(owner,o,global.downloadSubsystem); } };
   }

   public ViewPanel.ButtonPress releaseOrders() {
      return new ViewPanel.ButtonPress() { public void run(Object[] o) { global.orderManager.doRelease(owner,o); } };
   }

   public ViewPanel.ButtonPress printOrders() {
      return new ViewPanel.ButtonPress() { public void run(Object[] o) { global.orderManager.doPrint(owner,o); } };
   }

   public ViewPanel.ButtonPress completeOrders() {
      return new ViewPanel.ButtonPress() { public void run(Object[] o) { global.orderManager.doComplete(owner,o); } };
   }

   public ViewPanel.ButtonPress abortOrders() {
      return new ViewPanel.ButtonPress() { public void run(Object[] o) { global.orderManager.doAbort(owner,o); } };
   }

   public ViewPanel.ButtonPress viewInvoice() {
      return new ViewPanel.ButtonPress() { public void run(Object[] o) { global.orderManager.doInvoice(owner,o); } };
   }

   public ViewPanel.ButtonPress viewLabel() {
      return new ViewPanel.ButtonPress() { public void run(Object[] o) { global.orderManager.doLabel(owner,o); } };
   }

}

