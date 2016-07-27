/*
 * CheckUpdateDialog.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.gui.Blob;
import com.lifepics.neuron.gui.GridBagHelper;
import com.lifepics.neuron.gui.InitialFocus;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A dialog for presenting the results of the "check for updates" function.
 */

public class CheckUpdateDialog extends JDialog {

// --- fields ---

   private boolean result;

// --- construction ---

   public CheckUpdateDialog(Frame owner, AutoUpdate.AnalysisRecord ar) {
      super(owner,Text.get(CheckUpdateDialog.class,"s1"),/* modal = */ true);

      setDefaultCloseOperation(DISPOSE_ON_CLOSE);
      result = false;
      // slightly different plan, set result in advance and just dispose

      setResizable(false);

   // layout

      int d1 = Text.getInt(this,"d1");
      int d2 = Text.getInt(this,"d2");
      int d3 = Text.getInt(this,"d3");

      int w2 = Text.getInt(this,"w2");

      ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(d1,d1,d1,d1));

      GridBagHelper helper = new GridBagHelper(getContentPane());

      helper.add(1,1,Box.createHorizontalStrut(d2));
      helper.add(3,1,Box.createHorizontalStrut(d2));
      helper.add(5,1,Box.createHorizontalStrut(d2));

      helper.add(0,1,Box.createVerticalStrut(d1));
      helper.add(0,3,Box.createVerticalStrut(d3));
      helper.add(0,5,Box.createVerticalStrut(d3));
      helper.add(0,7,Box.createVerticalStrut(d3));
      helper.add(0,11,Box.createVerticalStrut(d1));

      helper.addSpanFill(0,0,7,Blob.makeBlob(ar.message,null,Text.getInt(this,"d4"),Text.getInt(this,"w1")));

      helper.add(0,4,new JLabel(Text.get(this,"s2")));
      helper.add(0,6,new JLabel(Text.get(this,"s3")));
      helper.add(0,8,new JLabel(Text.get(this,"s4")));

      helper.add(2,2,new JLabel(Text.get(this,"s5")));
      helper.add(4,2,new JLabel(Text.get(this,"s6")));

      helper.add(2,4,disable(new JTextField(ar.curLabCenter,w2)));
      helper.add(2,6,disable(new JTextField(ar.curConfig,   w2)));
      helper.add(2,8,disable(new JTextField(ar.curInvoice,  w2)));
      helper.add(4,4,disable(new JTextField(ar.newLabCenter,w2)));
      helper.add(4,6,disable(new JTextField(ar.newConfig,   w2)));
      helper.add(4,8,disable(new JTextField(ar.newInvoice,  w2)));

      if (ar.showBurner) {
         helper.add(0,9,Box.createVerticalStrut(d3));
         helper.add(0,10,new JLabel(Text.get(this,"s9")));
         helper.add(2,10,disable(new JTextField(ar.curBurner,w2)));
         helper.add(4,10,disable(new JTextField(ar.newBurner,w2)));
      }

      GridBagConstraints constraints = new GridBagConstraints();
      constraints.gridheight = ar.showBurner ? 7 : 5;
      constraints.anchor = GridBagConstraints.EAST;

      JButton button = new JButton(Text.get(this,"s7"));
      button.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { result = true; dispose(); } });
      helper.add(6,4,button,constraints);

      button = new JButton(Text.get(this,"s8"));
      button.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { dispose(); } });
      helper.addSpanCenter(0,12,7,button);

      // default button doesn't matter when the focus is on a button, which it always is here
      setFocusTraversalPolicy(new InitialFocus(button));

   // finish up

      // need a double pack, just like in AboutDialog

      pack();
      pack();
      setLocationRelativeTo(getOwner());
   }

   private JTextField disable(JTextField f) { f.setEnabled(false); return f; }

// --- methods ---

   /**
    * Run the dialog.
    */
   public boolean run() {
      setVisible(true); // does the modal thing
      return result;
   }

}

