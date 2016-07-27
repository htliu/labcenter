/*
 * ClassicStyle.java
 */

package com.lifepics.neuron.gui;

import com.lifepics.neuron.core.Text;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.*;

/**
 * The classic style.
 */

public class ClassicStyle implements Style.Interface {

// --- implementation ---

   public Light getLight(Object owner, int style) {
      int width, height;

      switch (style) {

      case Style.LIGHT_SUBSYSTEM_LARGE:
      case Style.LIGHT_SUBSYSTEM_SMALL:
         width = height = Text.getInt(this,"d1");
         break;

      case Style.LIGHT_SUBSYSTEM_THIN:
         width = Text.getInt(this,"d1");
         height = width / 2;
         break;

      case Style.LIGHT_SUBSYSTEM_VIEW:
         height = Text.getInt(this,"d2");
         width = height / 2;
         break;

      case Style.LIGHT_VIEW_HUGE:
      case Style.LIGHT_VIEW_LARGE:
      case Style.LIGHT_VIEW_SMALL:
         width = height = Text.getInt(this,"d2");
         break;

      default:
         throw new IllegalArgumentException();
      }

      return new ColorLight(owner,new Dimension(width,height));
   }

   public JPanel getLightLayout(JComponent[] lights) {
      JPanel panel = new JPanel();
      GridBagHelper helper = new GridBagHelper(panel);

      int x = 0;
      int y = 0;

      for (int i=0; i<lights.length; i++) {
         helper.add(x,y,lights[i]);
         if (++y > 2) { y = 0; ++x; }
      }

      return panel;
   }

   public JLabel getUnderlay(String underlay) {
      throw new UnsupportedOperationException();
   }

   public JPanel getProgressPanel() {
      return new ProgressPanel();
   }

   public String getBrandName() {
      return Brand.brandLifePics.getName();
   }

   public ImageIcon getLogo() {
      return Brand.brandLifePics.getLogo();
   }

   public void adjustMenuBar(JMenuBar menuBar) {
   }

   public void adjustMenu(JMenu menu) {
   }

   public void adjustMenu_LiveHelp(JMenu menu) {
      menu.setFont(menu.getFont().deriveFont(Font.BOLD));
   }

   public void adjustTabbedPane(JTabbedPane tabbedPane) {
   }

   public JPanel adjustTab(JPanel tab) {
      return tab;
   }

   public void refreshTabbedPane(JTabbedPane tabbedPane) {
   }

   public boolean showSummaryGrid() {
      return true;
   }

   public boolean newStatusLayout() {
      return false;
   }

   public boolean lowerTransferID() {
      return false;
   }

   public boolean tweakViewPanel() {
      return false;
   }

   public boolean allowAdjacentFields() {
      return true;
   }

   public int getPanelGap() {
      return Text.getInt(this,"d3");
   }

   public int getDetailGap() {
      return Text.getInt(this,"d4");
   }

   public JLabel adjustLine1(JLabel label) {
      return label;
   }

   public JLabel adjustLine2(JLabel label) {
      return label;
   }

   public JLabel adjustCounter(JLabel label) {
      return label;
   }

   public JLabel adjustPlain(JLabel label) {
      return label;
   }

   public JLabel adjustControl(JLabel label) {
      return label;
   }

   public JLabel adjustHeader(JLabel label) {
      return label;
   }

   public JButton adjustButton(JButton button) {
      return button;
   }

   public JButton adjustButton_Details(JButton button) {
      return button;
   }

   public void adjustDisabledField(JTextField field) {
      field.setDisabledTextColor(Color.black);
      field.setBackground(new JLabel().getBackground());
   }

   public Border createNormalBorder(String title) {
      return BorderFactory.createTitledBorder(title);
   }

   public Border createMajorBorder(String title) {
      return BorderFactory.createTitledBorder(title);
   }

   public Border createCompactBorder(String title) {
      return BorderFactory.createTitledBorder(title);
   }

   public Border createSidebarBorder(String title) {
      return BorderFactory.createTitledBorder(title);
   }

   public Border createTotalsBorder() {
      int d5 = Text.getInt(this,"d5");
      int d6 = Text.getInt(this,"d6");
      return BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(d6,0,0,0),
             BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(),
                                                BorderFactory.createEmptyBorder(d5,d5,d5,d5)));
   }

   public Border createButtonBorder() {
      return null; // null is OK here, means no border
   }

   public JPanel adjustPanel(JPanel panel) {
      return panel;
   }

   public JPanel adjustPanel_Group(JPanel panel) {
      return panel;
   }

   public void adjustScroll_Grid(JScrollPane scroll) {
   }

   public void adjustScroll_Other(JScrollPane scroll) {
   }

   public void colorize(JFrame frame, boolean alert) {
      Color color = alert ? Color.pink : new JLabel().getBackground();
      Style.colorize(frame.getContentPane(),color,/* button = */ true);
   }

}

