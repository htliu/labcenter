/*
 * StaticDLS.java
 */

package com.lifepics.neuron.dendron;

/**
 * Static fields factored out of {@link FormatDLS}
 * so that DLS libraries don't load unless needed.
 */

public class StaticDLS {

// --- pseudo-constants ---

   public static long busyPollInterval;
   public static int busyRetries;

   public static void configure(long a_busyPollInterval, int a_busyRetries) {
      busyPollInterval = a_busyPollInterval;
      busyRetries = a_busyRetries;
   }

}

