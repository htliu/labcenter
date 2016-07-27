/*
 * EmailPoller.java
 */

package com.lifepics.neuron.axon;

import java.io.File;

/**
 * A poller that accepts directories full of images
 * with the user's email address as the folder name.
 */

public class EmailPoller extends PrefixPoller {

   public EmailPoller() {
      super(/* wantDirectories = */ true);
   }

   protected int processRenamed(File original, File file, PollCallback callback) throws Exception {

      Roll roll = new Roll();

      roll.source = Roll.SOURCE_HOT_EMAIL;
      roll.email = original.getName();

      File[] files = file.listFiles();
      // if there are subdirectories, they will be listed, and the poll will error out.
      // this is the desired behavior ... the user might not notice if we ignored them.

      return callback.submit(roll,files); // this logs on success
   }

}

