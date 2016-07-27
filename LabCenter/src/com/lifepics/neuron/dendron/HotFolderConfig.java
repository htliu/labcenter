/*
 * HotFolderConfig.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.*;

import java.io.File;

/**
 * An object that holds configuration information for the hot folder format.
 */

public class HotFolderConfig extends Structure {

// --- fields ---

   public File dataDir;
   public boolean completeImmediately;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      HotFolderConfig.class,
      0,0,
      new AbstractField[] {

         new FileField("dataDir","DataDir",0,null),
         new BooleanField("completeImmediately","CompleteImmediately",0,false)
      });

   protected StructureDefinition sd() { return sd; }

// --- validation ---

   public void validate() throws ValidationException {
   }

}

