/*
 * SnapshotFile.java
 */

package com.lifepics.neuron.admin;

import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.*;

/**
 * An object that holds the contents of a snapshot file.
 */

public class SnapshotFile extends Structure {

// --- fields ---

   public Parameters parameters;
   public Snapshot snapshot;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      SnapshotFile.class,
      0,0,
      new AbstractField[] {

         new StructureField("parameters","Parameters",Parameters.sd),
         new StructureField("snapshot","Snapshot",Snapshot.sd)
      });

   protected StructureDefinition sd() { return sd; }

// --- validation ---

   public void validate() throws ValidationException {

      parameters.validate();
      snapshot.validate();
   }

}

