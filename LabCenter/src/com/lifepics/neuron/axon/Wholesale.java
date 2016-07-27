/*
 * Wholesale.java
 */

package com.lifepics.neuron.axon;

/**
 * A utility class that holds the global wholesale flag.
 * This is a very ugly design, but it will work for now.
 */

public class Wholesale {

   private static Boolean wholesale; // start out null to guarantee no early misuse

   public static boolean isWholesale() { return wholesale.booleanValue(); }
   public static void setWholesale(boolean b) { wholesale = new Boolean(b); }

}

