/*
 * SilentManipulator.java
 */

package com.lifepics.neuron.thread;

/**
 * A manipulator that doesn't show active entities.
 * The point is to reduce the number of updates.
 */

public class SilentManipulator extends EntityManipulator {

   public SilentManipulator(int statusFrom,
                            int statusTo) {
      super(statusFrom,statusFrom,statusTo);
      // statusActive is still used in testStatus,
      // so pass in statusFrom to make it ignored.
   }

   public boolean markActive    (Object o) { return false; }
   public boolean markIncomplete(Object o) { return false; }

}

