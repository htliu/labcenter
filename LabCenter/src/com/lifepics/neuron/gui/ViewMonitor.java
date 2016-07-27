/*
 * ViewMonitor.java
 */

package com.lifepics.neuron.gui;

import com.lifepics.neuron.table.View;
import com.lifepics.neuron.table.ViewListener;

import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * A component that monitors a view and notifies the user periodically
 * as long as the view isn't empty.  Compare SubsystemMonitor, Counter,
 * and MiniLight.
 */

public class ViewMonitor implements Monitor, ViewListener, ActionListener {

// --- fields ---

   private View view;
   private int code;
   private String message;
   private Timer timer;
   // monitor state is saved in the timer's running-ness

// --- construction ---

   public ViewMonitor(View view, int code, String message, int renotifyInterval) {
      this.view = view;
      this.code = code;
      this.message = message;
      timer = new Timer(renotifyInterval,this);
      timer.setInitialDelay(0);

      view.addListener(this);
      reportChange(); // otherwise no initial update
   }

   public void reinit(int renotifyInterval) {
      timer.setDelay(renotifyInterval);
      if (timer.isRunning()) timer.restart();
   }

// --- implementation of ViewListener ---

   public void reportInsert(int j, Object o) { reportChange(); }
   public void reportUpdate(int i, int j, Object o) {}
   public void reportDelete(int i) { reportChange(); }

   public void reportChange() {

      boolean isActive = timer.isRunning();
      boolean shouldBeActive = (view.size() > 0);

      if (isActive == shouldBeActive) return;

      if (shouldBeActive) {
         timer.start();
      } else {
         timer.stop();
      }
      // let the timer handle the initial notification too, that's easiest
   }

// --- implementation of ActionListener ---

   public void actionPerformed(ActionEvent e) {
      User.tell(code,message);
   }

}

