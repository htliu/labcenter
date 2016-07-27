/*
 * StopDialog.java
 */

package com.lifepics.neuron.thread;

import com.lifepics.neuron.core.Text;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A utility class that monitors a thread until it exits,
 * and shows a "please wait" dialog if it takes too long.
 * It is similar to {@link javax.swing.ProgressMonitor},
 * but doesn't have a progress display or a cancel button.
 */

public class StopDialog extends JDialog implements ActionListener {

// --- constants ---

   // if it ever matters, we could make these non-final
   // and have the defaults overridden by config file info

   private static final long dialogInterval = 1000; // millis
   private static final int  pollInterval   = 1000; // millis

   // pollInterval should be long, too, but Timer constructor takes int

// --- main method ---

   public static void joinSafe(Thread thread, Window owner) {
      try {
         thread.join(dialogInterval);
      } catch (InterruptedException e) {
         // won't happen
      }

      if (thread.isAlive()) {
         // poll at pollInterval until not alive

         StopDialog dialog = (owner instanceof Dialog) ? new StopDialog(thread,(Dialog) owner)
                                                       : new StopDialog(thread,(Frame ) owner);
         dialog.setVisible(true);
         // two subtle point here ... (a) "null instanceof X" is always false
         // (b) Frame constructor works with null, Dialog constructor doesn't
      }
   }

// --- fields ---

   private Thread thread;
   private Timer timer;

// --- construction ---

   private static String getTitleText() {
      return Text.get(StopDialog.class,"s1");
   }

   private StopDialog(Thread thread, Frame owner) {
      super(owner,getTitleText(),/* modal = */ true);

      this.thread = thread;
      construct();
   }

   private StopDialog(Thread thread, Dialog owner) {
      super(owner,getTitleText(),/* modal = */ true);

      this.thread = thread;
      construct();
   }

   private void construct() {

      setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
      setResizable(false);

   // layout

      int d1 = Text.getInt(this,"d1");
      int d2 = Text.getInt(this,"d2");

      JLabel label = new JLabel(Text.get(this,"e1",new Object[] { thread.getName() }));
      label.setBorder(BorderFactory.createEmptyBorder(d1,d2,d1,d2));

      getContentPane().add(label);

   // timer

      timer = new Timer(pollInterval,this);
      timer.start();

   // finish up

      pack();
      setLocationRelativeTo(getOwner());
   }

// --- methods ---

   public void actionPerformed(ActionEvent e) {
      if ( ! thread.isAlive() ) {
         dispose();
         timer.stop();
      }
      // this only gets called through the timer, so we know it's not null
   }

}

