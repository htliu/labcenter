/*
 * ViewPanel.java
 */

package com.lifepics.neuron.gui;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.table.View;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A panel that shows the objects in a {@link View}
 * so that they can be double-clicked
 * and also be acted on by buttons underneath.
 */

public class ViewPanel extends ViewHelper {

// --- fields ---

   private GridBagHelper buttonHelper;

// --- construction ---

   public ViewPanel(String title, String message,
                    View view, GridColumn[] cols, DoubleClick doubleClick,
                    int nx, int ny) {
      super(view,Text.getInt(ViewPanel.class,"n1"),cols,doubleClick);

      setBorder(Style.style.createNormalBorder(title));

   // constants

      int d1 = Text.getInt(this,"d1");
      int d4 = Text.getInt(this,"d4");

   // overall layout

      GridBagHelper helper = new GridBagHelper(this);

      if (message == null || message.length() == 0) message = " "; // for uniform spacing
      helper.addCenter(1,1,Style.style.adjustPlain(new JLabel(message)));

      JScrollPane scroll = getScrollPane();
      Style.style.adjustScroll_Grid(scroll);

      helper.add(1,2,Box.createVerticalStrut(d1));
      helper.add(1,3,scroll,GridBagHelper.fillBoth);
      helper.add(1,4,Box.createVerticalStrut(d1));

      if (nx > 0 && ny > 0) {
         helper.addCenter(1,5,makeButtonPanel(nx,ny));
         helper.add(1,6,Box.createVerticalStrut(d1));
      }

      if (Style.style.tweakViewPanel()) {
         helper.add(0,0,Box.createHorizontalStrut(d4));
         helper.add(1,0,Box.createVerticalStrut  (d1));
         helper.add(2,0,Box.createHorizontalStrut(d4));
      }

      helper.setRowWeight(3,1);
      helper.setColumnWeight(1,1);
   }

// --- buttons ---

   private JPanel makeButtonPanel(int nx, int ny) {
      JPanel panel = new JPanel();

   // constants

      int d2 = Text.getInt(this,"d2");
      int d3 = Text.getInt(this,"d3");

      Dimension size = new JButton("nnnn").getPreferredSize();

   // struts

      buttonHelper = new GridBagHelper(panel);

      for (int x=1; x<nx; x++) buttonHelper.add(2*x-1,0,Box.createHorizontalStrut(d2));
      for (int y=1; y<ny; y++) buttonHelper.add(0,2*y-1,Box.createVerticalStrut(d3));

   // buttons

      // add rigid areas to make even blank rows and columns look decent.
      // only need one area per row or column, so add in a diagonal line.

      int n = Math.max(nx,ny);
      for (int i=0; i<n; i++) {
         int x = Math.min(i,nx-1);
         int y = Math.min(i,ny-1);
         buttonHelper.add(2*x,2*y,Box.createRigidArea(size));
      }

      return panel;
   }

   public void addButton(int x, int y, String name, ButtonPress buttonPress) {
      JButton button = Style.style.adjustButton(new JButton(name));
      button.addActionListener(getAdapter(buttonPress));
      buttonHelper.addFill(2*x,2*y,button);
   }

   public void addWideButton(int x, int y, String name, ButtonPress buttonPress) {
      JButton button = Style.style.adjustButton(new JButton(name));
      button.addActionListener(getAdapter(buttonPress));
      buttonHelper.addSpanCenter(2*x,2*y,3,button);
   }

}

