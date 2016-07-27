/*
 * SubsystemController.java
 */

package com.lifepics.neuron.thread;

import java.util.LinkedList;

import java.awt.Frame;

/**
 * The external interface of Subsystem, for use by GUI components.
 */

public interface SubsystemController {

   /**
    * Add a listener to observe the subsystem.
    * The subsystem will send a report before returning.
    * A removeListener function is available,
    * but you never need to call it in normal operation
    * because listeners are held as weak references.
    */
   void addListener(SubsystemListener a_listener);

   /**
    * Start the subsystem's thread.
    * Do nothing if already started.
    */
   void start();

   /**
    * Stop the subsystem's thread.
    * @return False if already stopped.
    */
   boolean stop(Frame owner);

   /**
    * @return A list of posted errors, as Throwable.
    */
   LinkedList getErrors();

   /**
    * Clear normal errors but not fatal error(s).
    */
   void clearErrors();

}

