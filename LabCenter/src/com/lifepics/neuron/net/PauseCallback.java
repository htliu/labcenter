/*
 * PauseCallback.java
 */

package com.lifepics.neuron.net;

/**
 * A callback interface for reporting diagnostic pause information.
 */

public interface PauseCallback {

   void paused(int diagnosis);
   void unpaused();

}

