/*
 * LocalConfig.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.object.CopyUtil;
import com.lifepics.neuron.struct.*;

import java.io.File;

/**
 * An object that holds configuration information for the local order subsystem.
 */

public class LocalConfig extends Structure {

// --- fields ---

   public long pollInterval; // millis
   public File directory;

   // you might think we could allow multiple targets as in PollConfig,
   // with a different orderSeq for each target, but that doesn't work,
   // because orderSeq isn't really an arbitrary string.  it's really
   // an enum, a lot like roll source except that we make some behavior
   // changes based on its value.  we can't make it a real enum, though,
   // because we need to store it as part of the table file name.

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      LocalConfig.class,
      0,0,
      new AbstractField[] {

         new LongField("pollInterval","PollInterval",0,60000),
         new NullableFileField("directory","Directory",0,null)
      });

   protected StructureDefinition sd() { return sd; }

// --- copy function ---

   public LocalConfig copy() { return (LocalConfig) CopyUtil.copy(this); }

// --- validation ---

   public void validate() throws ValidationException {

      if (pollInterval < 1) throw new ValidationException(Text.get(this,"e1"));
   }

}

