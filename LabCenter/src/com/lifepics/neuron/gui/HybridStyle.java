/*
 * HybridStyle.java
 */

package com.lifepics.neuron.gui;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.*;

/**
 * A hybrid style for investigating weird GUI problems.
 */

public class HybridStyle implements Style.Interface {

   private Style.Interface style1;
   private Style.Interface style2;
   private boolean[] flags;
   private static final int LEN = 39; // the number of functions in this interface

   public HybridStyle(Style.Interface style1, Style.Interface style2, String flags) {
      this.style1 = style1;
      this.style2 = style2;
      this.flags = new boolean[LEN];
      int len = Math.min(LEN,flags.length());
      for (int i=0; i<len; i++) {
         this.flags[i] = (flags.charAt(i) != '.');
      }
      this.flags[2] = true; // force true since classic doesn't define getUnderlay
   }

   public Style.Interface f(int i) { return flags[i] ? style1 : style2; }

// --- implementation ---

   public Light getLight(Object owner, int style) { return f(0).getLight(owner,style); }
   public JPanel getLightLayout(JComponent[] lights) { return f(1).getLightLayout(lights); }

   public JLabel getUnderlay(String underlay) { return f(2).getUnderlay(underlay); }

   public JPanel getProgressPanel() { return f(3).getProgressPanel(); }

   public String getBrandName() { return f(4).getBrandName(); }
   public ImageIcon getLogo() { return f(5).getLogo(); }
   public void adjustMenuBar(JMenuBar menuBar) { f(6).adjustMenuBar(menuBar); }
   public void adjustMenu(JMenu menu) { f(38).adjustMenu(menu); }
   public void adjustMenu_LiveHelp(JMenu menu) { f(7).adjustMenu_LiveHelp(menu); }

   public void adjustTabbedPane(JTabbedPane tabbedPane) { f(8).adjustTabbedPane(tabbedPane); }
   public JPanel adjustTab(JPanel tab) { return f(9).adjustTab(tab); }
   public void refreshTabbedPane(JTabbedPane tabbedPane) { f(10).refreshTabbedPane(tabbedPane); }

   public boolean showSummaryGrid() { return f(11).showSummaryGrid(); }
   public boolean newStatusLayout() { return f(12).newStatusLayout(); }
   public boolean lowerTransferID() { return f(13).lowerTransferID(); }
   public boolean tweakViewPanel() { return f(14).tweakViewPanel(); }
   public boolean allowAdjacentFields() { return f(15).allowAdjacentFields(); }

   public int getPanelGap() { return f(16).getPanelGap(); }
   public int getDetailGap() { return f(17).getDetailGap(); }

   public JLabel adjustLine1  (JLabel label) { return f(18).adjustLine1(label); }
   public JLabel adjustLine2  (JLabel label) { return f(19).adjustLine2(label); }
   public JLabel adjustCounter(JLabel label) { return f(20).adjustCounter(label); }
   public JLabel adjustPlain  (JLabel label) { return f(21).adjustPlain(label); }
   public JLabel adjustControl(JLabel label) { return f(22).adjustControl(label); }
   public JLabel adjustHeader (JLabel label) { return f(23).adjustHeader(label); }

   public JButton adjustButton        (JButton button) { return f(24).adjustButton(button); }
   public JButton adjustButton_Details(JButton button) { return f(25).adjustButton_Details(button); }

   public void adjustDisabledField(JTextField field) { f(26).adjustDisabledField(field); }

   public Border createNormalBorder (String title) { return f(27).createNormalBorder(title); }
   public Border createMajorBorder  (String title) { return f(28).createMajorBorder(title); }
   public Border createCompactBorder(String title) { return f(29).createCompactBorder(title); }
   public Border createSidebarBorder(String title) { return f(30).createSidebarBorder(title); }
   public Border createTotalsBorder() { return f(31).createTotalsBorder(); }
   public Border createButtonBorder() { return f(32).createButtonBorder(); }
   public JPanel adjustPanel      (JPanel panel) { return f(33).adjustPanel(panel); }
   public JPanel adjustPanel_Group(JPanel panel) { return f(34).adjustPanel_Group(panel); }

   public void adjustScroll_Grid (JScrollPane scroll) { f(35).adjustScroll_Grid(scroll); }
   public void adjustScroll_Other(JScrollPane scroll) { f(36).adjustScroll_Other(scroll); }

   public void colorize(JFrame frame, boolean alert) { f(37).colorize(frame,alert); }

}

