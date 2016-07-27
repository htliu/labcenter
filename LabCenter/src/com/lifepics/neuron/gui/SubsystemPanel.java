/*
 * SubsystemPanel.java
 */

package com.lifepics.neuron.gui;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.thread.ErrorWindow;
import com.lifepics.neuron.thread.SubsystemController;
import com.lifepics.neuron.thread.SubsystemListener;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.*;

/**
 * A panel that displays standard subsystem indicators and controls.
 */

public class SubsystemPanel extends JPanel implements SubsystemListener {

// --- fields ---

   private Frame frame;
   private SubsystemController subsystem;

   private IndicatorLight indicatorLight;
   private JLabel message;

   private JButton buttonView;
   private JButton buttonStart;
   private JButton buttonStop;

   private boolean preventStopStart;
   private int saveState;
   private boolean saveHasErrors;

// --- construction ---

   public SubsystemPanel(Frame frame, SubsystemController subsystem, String name) {
      this(frame,subsystem,name,null);
   }

   /**
    * @param errorWindow If not null, use this instead of the subsystem
    *                    to send events to the indicator light.
    */
   public SubsystemPanel(Frame frame, SubsystemController subsystem, String name, ErrorWindow errorWindow) {
      this.frame = frame;
      this.subsystem = subsystem;

      boolean newStatusLayout = Style.style.newStatusLayout();

      indicatorLight = new IndicatorLight(null,Style.LIGHT_SUBSYSTEM_SMALL);
      message = (newStatusLayout && name != null) ? Style.style.getUnderlay(name) : new JLabel();
      message = Style.style.adjustPlain(message);

      buttonView  = Style.style.adjustButton(new JButton(Text.get(this,"s2")));
      buttonStart = Style.style.adjustButton(new JButton(Text.get(this,"s3")));
      buttonStop  = Style.style.adjustButton(new JButton(Text.get(this,"s4")));

      buttonView .addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doView();  } });
      buttonStart.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doStart(); } });
      buttonStop .addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doStop();  } });

      Border border;
      String prefix;

      if (newStatusLayout) {

         border = Style.style.createSidebarBorder(Text.get(this,"s5"));
         prefix = "ds";

      } else if (name != null) {

         border = BorderFactory.createTitledBorder(name);
         prefix = "dt"; // titled

      } else {

         border = BorderFactory.createEtchedBorder();
         prefix = "dn"; // not titled
      }

      int d1 = Text.getInt(this,prefix + "1");
      int d2 = Text.getInt(this,prefix + "2");
      int d3 = Text.getInt(this,prefix + "3");
      int d4 = Text.getInt(this,prefix + "4");

      setBorder(BorderFactory.createCompoundBorder(border,BorderFactory.createEmptyBorder(d2,d3,d4,d3)));

      GridBagHelper helper = new GridBagHelper(this);

      if ( ! newStatusLayout ) {
         helper.add(0,0,new JLabel(Text.get(this,"s1")));
         helper.add(1,0,Box.createHorizontalStrut(d1));
      }
      helper.add(2,0,indicatorLight.getComponent());
      helper.add(3,0,Box.createHorizontalStrut(d1));
      helper.add(4,0,message,GridBagHelper.fillBoth); // else underlay is clipped to text rect
      helper.add(5,0,Box.createHorizontalStrut(d1));
      helper.add(6,0,buttonView);
      helper.add(7,0,Box.createHorizontalStrut(d1));
      helper.add(8,0,buttonStart);
      helper.add(9,0,Box.createHorizontalStrut(d1));
      helper.add(10,0,buttonStop);

      helper.setColumnWeight(4,1);
      // no row weight, so, center rather than fill

      Style.style.adjustPanel(this);

      preventStopStart = false;
      // set this to something before calling addListener,
      // so that it has a well-defined value ...
      // but we'll get a call to setPreventStopStart later.

      if (errorWindow != null) {
         errorWindow.addListener(indicatorLight);
      } else {
         subsystem.addListener(indicatorLight);
      }
      subsystem.addListener(this);
   }

   public JPanel createCluster() {
      int vgap = Text.getInt(this,"d1");
      JPanel cluster = new JPanel(new GridLayout(0,1,0,vgap));
      Style.style.adjustPanel(cluster); // make vgaps white
      cluster.setBorder(getBorder());
      setBorder(null);
      cluster.add(this);
      return cluster;
   }

   public void addToCluster(JPanel cluster) {
      setBorder(null);
      cluster.add(this);
   }

   public SubsystemController getSubsystem() { return subsystem; }

// --- implementation of SubsystemListener ---

   private static final String[][] messageTable = {
         { "msn", "mse" },
         { "mrn", "mre" },
         { "mpn", "mpe" },
         { "mwn", "mwe" },
         { "man", "mae" } // first one shouldn't happen
      };

   public void report(int state, String reason, boolean hasErrors) {

      message.setText(Text.get(this,messageTable[state][hasErrors ? 1 : 0],new Object[] { reason }));

      saveState = state;
      saveHasErrors = hasErrors;

      buttonUpdate();
   }

   public void setPreventStopStart(boolean preventStopStart) {

      this.preventStopStart = preventStopStart;

      buttonUpdate();
      // note, save fields are filled in at construction, so they
      // are guaranteed to be valid by the time this is called.
   }

   private void buttonUpdate() {

      boolean stateStopped = (saveState == SubsystemListener.STOPPED);
      boolean stateRunning = (saveState == SubsystemListener.RUNNING);
      boolean statePaused  = (    saveState == SubsystemListener.PAUSED_NETWORK
                               || saveState == SubsystemListener.PAUSED_WAIT    );
      boolean stateAborted = (saveState == SubsystemListener.ABORTED);

      buttonView.setEnabled(saveHasErrors);
      buttonStart.setEnabled( stateAborted || (stateStopped && ! preventStopStart) );
      buttonStop .setEnabled( (stateRunning || statePaused) && ! preventStopStart );
   }

// --- commands ---

   private void doView() {
      new SubsystemDialog(frame,subsystem).setVisible(true);
   }

   private void doStart() {
      subsystem.start();
   }

   private void doStop() {
      subsystem.stop(frame);
   }

}

