/*
 * FixedPrefixPoller.java
 */

package com.lifepics.neuron.axon;

import java.io.File;

/**
 * A subclass of {@link PrefixPoller} that renames using fixed strings.
 */

public abstract class FixedPrefixPoller extends PrefixPoller {

   // must rename to "done" rather than "rollN" because
   // we predict the name and store it as an extra file.

   public FixedPrefixPoller() {
      super("temp","done",/* wantDirectories = */ false);
   }

   protected String getName(String core, boolean done, int rollID) {
      String prefix = done ? (prefixDone + MARK) : prefixTemp;
      return prefix + core;
      // override to remove roll ID from done form
   }

   protected String getFinalName(File original) {
      String core = getCore(original.getName());
      return getName(core,/* done = */ true,/* rollID = */ 0);
   }

}

