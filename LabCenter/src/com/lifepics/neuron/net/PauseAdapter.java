/*
 * PauseAdapter.java
 */

package com.lifepics.neuron.net;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.thread.ThreadStatus;

/**
 * An adapter that converts {@link PauseCallback} calls into {@link ThreadStatus} calls.
 */

public class PauseAdapter implements PauseCallback {

   private ThreadStatus threadStatus;
   public PauseAdapter(ThreadStatus threadStatus) { this.threadStatus = threadStatus; }

   private static String[] diagnosisTable = {
         Text.get(PauseAdapter.class,"di0"),
         Text.get(PauseAdapter.class,"di1"),
         Text.get(PauseAdapter.class,"di2")
      };

   public void paused(int diagnosis) {
      threadStatus.pausedNetwork(diagnosisTable[diagnosis]);
   }

   public void unpaused() {
      threadStatus.unpaused();
   }

}

