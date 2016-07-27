/*
 * RenamePoller.java
 */

package com.lifepics.neuron.axon;

import com.lifepics.neuron.core.Text;

import java.io.File;

/**
 * A poller that renames target files in two steps to avoid repeats and show success.
 */

public abstract class RenamePoller implements Poller {

// --- subclass hooks ---

   // subclasses must implement getFileFilter

   protected abstract String getCore(String name);
   protected abstract String getName(String core, boolean done, int rollID);
   //
   // the names returned by getName must not be detected by the filter,
   // otherwise you'll get an infinite loop reprocessing the same file!

   protected abstract int processRenamed(File original, File file, PollCallback callback) throws Exception;

   protected void finish(File original, File file) {}

// --- utility function ---

   protected static boolean endsWithIgnoreCase(String s, String suffix) {
      int index = s.length() - suffix.length();
      if (index < 0) return false;
      return (s.substring(index).equalsIgnoreCase(suffix));
   }

// --- main function ---

   public void process(File file, PollCallback callback) throws Exception {

   // first renaming

      // do nothing until this works, that way we're guaranteed not to process twice

      String core = getCore(file.getName());

      File temp = new File(file.getParentFile(),getName(core,/* done = */ false,0));
      if ( ! file.renameTo(temp) ) throw new Exception(Text.get(RenamePoller.class,"e4"));

   // actual processing

      int rollID = processRenamed(file,temp,callback);

   // second renaming

      File done = new File(file.getParentFile(),getName(core,/* done = */ true,rollID));
      if ( ! temp.renameTo(done) ) throw new Exception(Text.get(RenamePoller.class,"e5"));

      // this will just be caught and logged, so really it's more of a warning.
      // no need to put in the file name, the logger will handle that part too.

   // let subclasses do whatever

      finish(file,temp);
   }

}

