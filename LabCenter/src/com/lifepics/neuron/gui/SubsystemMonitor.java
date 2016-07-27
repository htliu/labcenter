/*
 * SubsystemMonitor.java
 */

package com.lifepics.neuron.gui;

import com.lifepics.neuron.core.ChainedException;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.thread.SubsystemController;
import com.lifepics.neuron.thread.SubsystemListener;
import com.lifepics.neuron.thread.ThreadStopException;

import java.util.LinkedList;

import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * A component that monitors a subsystem and notifies the user
 * if there's a fatal error, and also renotifies after a while.
 * It's in the GUI because it uses a timer and calls User.tell.
 * This could be integrated with SubsystemPanel,
 * but let's not do that ... this way both pieces are simpler.
 */

public class SubsystemMonitor implements Monitor, SubsystemListener, ActionListener {

// --- fields ---

   private SubsystemController subsystem;
   private String message;
   private Timer timer;
   // monitor state is saved in the timer's running-ness

// --- construction ---

   public SubsystemMonitor(SubsystemController subsystem, String message, int renotifyInterval) {
      this.subsystem = subsystem;
      this.message = message;
      timer = new Timer(renotifyInterval,this);
      timer.setInitialDelay(0);

      subsystem.addListener(this); // has to come last, might fire timer
   }

   public void reinit(int renotifyInterval) {

      // we could check for changed here, as with statusNewsInterval,
      // but since there are lots of these, we'll check once for all,
      // i.e., in the caller.

      timer.setDelay(renotifyInterval);
      if (timer.isRunning()) timer.restart();
      // this produces an immediate ping, so don't do it unless needed
   }

   public void stop() {
      timer.stop();
      // I confirmed that this is enough to let the subsystem get GC'd
   }

   public SubsystemController getSubsystem() { return subsystem; }

// --- implementation of SubsystemListener ---

   public void report(int state, String reason, boolean hasErrors) {

      boolean isActive = timer.isRunning();
      boolean shouldBeActive = (state == SubsystemListener.ABORTED);

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
      String s = message;

      LinkedList errors = subsystem.getErrors();
      if (errors.size() > 0) { // should always be true, but don't count on it

         Throwable t = (Throwable) errors.getLast();
         if (t instanceof ThreadStopException) {
            String hint = ((ThreadStopException) t).getHint();
            if (hint != null) {
               s = s + "\n\n" + hint;
            }
         }
         s = s + Text.get(this,"s1") + ChainedException.format(t);
      }

      User.tell(User.CODE_OTHER_PROCESS,s,subsystem);
   }

}

