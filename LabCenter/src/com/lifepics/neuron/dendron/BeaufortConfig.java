/*
 * BeaufortConfig.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.*;

import java.io.File;

/**
 * An object that holds configuration information for the Beaufort format.
 */

public class BeaufortConfig extends Structure {

// --- fields ---

   public File dataDir;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      BeaufortConfig.class,
      0,0,
      new AbstractField[] {

         new FileField("dataDir","DataDir",0,new File("C:\\Program Files\\image2print\\remote import"))
      });

   protected StructureDefinition sd() { return sd; }

// --- validation ---

   public void validate() throws ValidationException {
   }

}

