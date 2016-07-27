/*
 * TabControl.java
 */

package com.lifepics.neuron.gui;

/**
 * An interface for controlling which application tab is active.
 */

public interface TabControl {

   public static final int TARGET_DOWNLOAD = 0;
   public static final int TARGET_UPLOAD   = 1;
   public static final int TARGET_JOB      = 2;
   public static final int TARGET_OTHER    = 3;

   void setSelectedTab(int target);

}

