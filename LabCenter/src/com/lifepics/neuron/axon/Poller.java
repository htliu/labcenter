/*
 * Poller.java
 */

package com.lifepics.neuron.axon;

import java.io.File;
import java.io.FileFilter;

/**
 * An interface for polling files (and/or subdirectories) within a directory.
 */

public interface Poller {

   /**
    * Get a file filter to identify suitable files.
    */
   FileFilter getFileFilter();

   /**
    * Process a suitable file.
    */
   void process(File file, PollCallback callback) throws Exception;

}

