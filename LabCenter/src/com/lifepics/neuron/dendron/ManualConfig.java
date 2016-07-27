/*
 * ManualConfig.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.*;

import java.io.File;

/**
 * An object that holds configuration information for the manual formats.
 */

public class ManualConfig extends Structure {

// --- fields ---

   public File dataDir;
   public boolean autoCreate;
   public boolean enableCompletion;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      ManualConfig.class,
      0,2,
      new AbstractField[] {

         new FileField("dataDir","DataDir",0,null),
         new BooleanField("autoCreate","AutoCreate",1,false),
         new BooleanField("enableCompletion","EnableCompletion",2,false)
      });

   protected StructureDefinition sd() { return sd; }

// --- validation ---

   public void validate() throws ValidationException {
   }

}

