/*
 * MiniPanel.java
 */

package com.lifepics.neuron.gui;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.thread.SubsystemController;
import com.lifepics.neuron.thread.SubsystemListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A panel that displays just a few subsystem indicators and controls.
 *
 * The relationship between MiniPanel and SubsystemPanel
 * (the first is a smaller version of the second)
 * is not the same as the one between MiniLight and IndicatorLight
 * (MiniLight looks mainly at a view, not a subsystem).
 * Still, I like the name too much to choose anything else.
 */

public class MiniPanel extends JPanel implements SubsystemListener {

// --- fields ---

   private SubsystemController subsystem;

   private JCheckBox skipInvoice;
   private JCheckBox skipLabel;
   private JButton buttonStart;

// --- construction ---

   /**
    * SubsystemPanel has the option to put an error window
    * between the subsystem and the indicator light,
    * but that's not necessary here ... the light will always
    * be red at first, then you'll restart and it will
    * be green, maybe turning back to red if the error re-fires.
    * Unless you leave the window up for an hour,
    * you'll never notice that it doesn't ever turn yellow.
    */
   public MiniPanel(SubsystemController subsystem) {
      this.subsystem = subsystem;

   // construct

      IndicatorLight indicatorLight = new IndicatorLight(subsystem,Style.LIGHT_SUBSYSTEM_SMALL);

      buttonStart = Style.style.adjustButton(new JButton(Text.get(this,"s1")));
      buttonStart.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doStart(); } });

      // this is a weird hybrid of styled and unstyled.
      // normally, everything on a subdialog should be unstyled,
      // but it would be a pain to unstyle the light,
      // and weird to have an unstyled button next to the light.

   // lay out

      GridBagHelper helper = new GridBagHelper(this);

      if (subsystem instanceof InvoiceController) {

         skipInvoice = new JCheckBox();
         skipLabel   = new JCheckBox();
         //
         // initial values intentionally always the default of false

         helper.add(2,0,new JLabel(Text.get(this,"s2")));
         helper.add(3,0,skipInvoice);
         helper.add(3,1,skipLabel);
         helper.add(4,0,new JLabel(Text.get(this,"s3a")));
         helper.add(4,1,new JLabel(Text.get(this,"s3b")));
         //
         // normally you'd want a space in the labels on either side
         // of the controls, but here the check boxes provide plenty.
      }

      helper.add(0,2,indicatorLight.getComponent());
      helper.add(1,2,Box.createHorizontalStrut(Text.getInt(this,"d1")));
      helper.addSpan(2,2,3,buttonStart);

      int d2 = Text.getInt(this,"d2");
      setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), // not styled
                                                   BorderFactory.createEmptyBorder(d2,d2,d2,d2)));

   // finish

      subsystem.addListener(this);
   }

// --- implementation of SubsystemListener ---

   public void report(int state, String reason, boolean hasErrors) {

      boolean enable = (state == SubsystemListener.ABORTED);
      //
      // in SubsystemPanel the button is also enabled
      // if the state is STOPPED *and* the user is allowed to stop and start things,
      // but since we only get here in an abort state,
      // we can leave out that case and all its complications.

      buttonStart.setEnabled(enable);
      if (skipInvoice != null) skipInvoice.setEnabled(enable);
      if (skipLabel   != null) skipLabel  .setEnabled(enable);
   }

// --- commands ---

   private void doStart() {
      if (subsystem instanceof InvoiceController) {
         ((InvoiceController) subsystem).start(Field.get(skipInvoice),Field.get(skipLabel));
      } else {
         subsystem.start();
      }
   }

}

