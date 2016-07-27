/*
 * Relative.java
 */

package com.lifepics.neuron.object;

import java.io.File;

/**
 * A tiny class to standardize the makeRelativeTo behavior of paths.
 */

public class Relative {

   public static File makeRelativeTo(File base, File path) {
      return path.isAbsolute() ? path : new File(base,path.getPath());
   }

   public interface Path {
      void makeRelativeTo(File base);
   }

}

