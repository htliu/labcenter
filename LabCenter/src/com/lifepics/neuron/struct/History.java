/*
 * History.java
 */

package com.lifepics.neuron.struct;

/**
 * A class that remembers the structure version
 * as a function of time (application version).
 * It's a step function, given as an array like [v0,t1,v1,t2,v2].
 * The history doesn't have to be complete, that's why there's
 * no function getVersionMin.
 */

public class History {

   private int[] f;

   public History(int[] f) {
      this.f = f;
      if ((f.length & 1) == 0) throw new IllegalArgumentException();
   }

   public int getVersionMax() {
      return f[f.length-1];
   }

   public int getVersion(int t) {
      int i = 0;
      while (i+2<f.length && f[i+1] <= t) i += 2;
      return f[i];
   }

}

