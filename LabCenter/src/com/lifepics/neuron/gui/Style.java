/*
 * Style.java
 */

package com.lifepics.neuron.gui;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.*;

/**
 * A cover for the different possible style implementations.
 */

public class Style {

// --- global variable ---

   public static Style.Interface style = new ClassicStyle();

   public static final String STYLE_MODERN  = "MODERN";
   public static final String STYLE_QDPC    = "QDPC";
   public static final String STYLE_CLASSIC = "CLASSIC";
   public static final String STYLE_HYBRID  = "HYBRID";

   public static String getDefaultStyle() { return STYLE_MODERN; }

   public static void setStyle(String name) {

      // right now this enumeration appears only here ...
      // we read from the config but never write to it.

      if      (name.equals(STYLE_CLASSIC)) style = new ClassicStyle();
      else if (name.equals(STYLE_QDPC   )) style = new ModernStyle(Brand.brandQDPC);
      else {
         style = new ModernStyle(Brand.brandLifePics);
         if (name.startsWith(STYLE_HYBRID)) {
            style = new HybridStyle(style,new ClassicStyle(),name.substring(STYLE_HYBRID.length()));
         }
      }
   }

// --- interface ---

   public interface Interface {

      /**
       * @param owner The owner object that receives signals and passes them to the light.
       *              The owner isn't part of the GUI any more, so someone needs to own it,
       *              otherwise the weak listener references will cause it to be collected.
       */
      Light getLight(Object owner, int style);
      JPanel getLightLayout(JComponent[] lights);

      JLabel getUnderlay(String underlay);

      /**
       * @return An object that is both a JPanel and a TransferProgress.
       */
      JPanel getProgressPanel();

      String getBrandName();
      ImageIcon getLogo();
      void adjustMenuBar(JMenuBar menuBar);
      void adjustMenu(JMenu menu);
      void adjustMenu_LiveHelp(JMenu menu);

      void adjustTabbedPane(JTabbedPane tabbedPane);
      JPanel adjustTab(JPanel tab);
      void refreshTabbedPane(JTabbedPane tabbedPane);

      boolean showSummaryGrid();
      boolean newStatusLayout();
      boolean lowerTransferID();
      boolean tweakViewPanel();
      boolean allowAdjacentFields();

      int getPanelGap();
      int getDetailGap();

      JLabel adjustLine1  (JLabel label);
      JLabel adjustLine2  (JLabel label);
      JLabel adjustCounter(JLabel label);
      JLabel adjustPlain  (JLabel label);
      JLabel adjustControl(JLabel label);
      JLabel adjustHeader (JLabel label);

      JButton adjustButton        (JButton button);
      JButton adjustButton_Details(JButton button);

      void adjustDisabledField(JTextField field);

      Border createNormalBorder (String title);
      Border createMajorBorder  (String title);
      Border createCompactBorder(String title);
      Border createSidebarBorder(String title);
      Border createTotalsBorder();
      Border createButtonBorder();
      JPanel adjustPanel      (JPanel panel);
      JPanel adjustPanel_Group(JPanel panel);

      void adjustScroll_Grid (JScrollPane scroll);
      void adjustScroll_Other(JScrollPane scroll);

      void colorize(JFrame frame, boolean alert);
   }

// --- enumerations ---

   // light style
   public static final int LIGHT_SUBSYSTEM_LARGE = 0;
   public static final int LIGHT_SUBSYSTEM_THIN  = 1;
   public static final int LIGHT_SUBSYSTEM_SMALL = 2;
   public static final int LIGHT_SUBSYSTEM_VIEW  = 3;
   public static final int LIGHT_VIEW_HUGE       = 4;
   public static final int LIGHT_VIEW_LARGE      = 5;
   public static final int LIGHT_VIEW_SMALL      = 6;

// --- colorize function ---

   public static void colorize(Component c, Color color, boolean button) {
      colorize(c,color,button,0);
      // depth isn't used, but it's handy for debugging and overloading
   }

   public static boolean isTraversablePanel(Component c) {
      return (c instanceof JPanel && ! (c instanceof Light));
   }

   private static void colorize(Component c, Color color, boolean button, int depth) {

      boolean alter = false;
      boolean recur = false;

      // exclusions on JPanel reflect the current state of the gui package;
      // any new special components I create may need to be added here too.

      if (    isTraversablePanel(c)
           || c instanceof JTabbedPane ) {

         alter = true;
         recur = true;

      } else if (button && c instanceof JButton) {
      // no need to alter JLabels, they're not opaque

         alter = true;
      }

      if (alter) {
         c.setBackground(color);
      }
      if (recur) {
         Component[] sub = ((Container) c).getComponents();
         for (int i=0; i<sub.length; i++) {
            colorize(sub[i],color,button,depth+1);
         }
      }
   }

}

