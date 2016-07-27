/*
 * Parse.java
 */

package com.lifepics.neuron.launch;

import java.io.File;

/**
 * A utility class that lets the launcher and the main application
 * parse the initial part of the argument list using the same code.
 *
 * I would have liked to put the functions into the launcher class,
 * but that wouldn't work.  The problem is, most dealers will still
 * be using the old launcher, so if we make a call to the launcher
 * class, we'll look in the root class loader and find the old code.
 * That's why there's generally no overlap between the class files.
 */

public class Parse {

   /**
    * Get an optional base directory argument.
    * @return The base directory, or null if none was specified.
    */
   public static File getBaseDir(String[] args) {
      return (args.length >= 2 && args[0].equals("-base")) ? new File(args[1]) : null;
      // the base directory will usually be an absolute path, but that's not required
   }

   /**
    * Get the count of arguments used when the base directory is present.
    * This is mostly just a reminder that the other code depends on this.
    */
   public static int getBaseDirOffset() { return 2; }

}

