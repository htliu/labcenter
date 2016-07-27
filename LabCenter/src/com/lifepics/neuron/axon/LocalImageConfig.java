/*
 * LocalImageConfig.java
 */

package com.lifepics.neuron.axon;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.object.CopyUtil;
import com.lifepics.neuron.struct.*;

/**
 * An object that holds configuration information for the local image subsystem.
 */

public class LocalImageConfig extends Structure {

// --- fields ---

   public long pollInterval; // millis

   // it's not much, but I think we'll be adding more later

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      LocalImageConfig.class,
      0,0,
      new AbstractField[] {

         new LongField("pollInterval","PollInterval",0,5000)
      });

   protected StructureDefinition sd() { return sd; }

// --- copy function ---

   public LocalImageConfig copy() { return (LocalImageConfig) CopyUtil.copy(this); }

// --- validation ---

   public void validate() throws ValidationException {

      if (pollInterval < 1) throw new ValidationException(Text.get(this,"e1"));
   }

}

