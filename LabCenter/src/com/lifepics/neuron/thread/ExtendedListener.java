/*
 * ExtendedListener.java
 */

package com.lifepics.neuron.thread;

/**
 * An extended version of {@link SubsystemListener}.
 */

public interface ExtendedListener {

   // see SubsystemListener for state enumeration and other comments

   /**
    * @param show A flag telling whether to show the errors as full errors
    *             or as weak errors.  This only applies when the values of
    *             state and hasErrors indicate an adjustable error.
    */
   void report(int state, String reason, boolean hasErrors, boolean show);

}

