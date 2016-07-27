/*
 * SubsystemListener.java
 */

package com.lifepics.neuron.thread;

/**
 * An interface that subsystems use to report on their status,
 * and that then drives various kinds of indicators in the UI.
 */

public interface SubsystemListener {

// --- state enumeration ---

   public static final int STOPPED = 0;
   public static final int RUNNING = 1;
   public static final int PAUSED_NETWORK = 2;
   public static final int PAUSED_WAIT    = 3;
   public static final int ABORTED = 4;

// --- functions ---

   /**
    * Report the current status.  All combinations of arguments
    * are possible, except for aborted with hasErrors set false.
    *
    * @param reason The reason the subsystem is paused; null if not paused.
    *               This is a string that can be displayed to the end user.
    */
   void report(int state, String reason, boolean hasErrors);

}

