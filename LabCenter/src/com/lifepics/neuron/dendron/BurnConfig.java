/*
 * BurnConfig.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.*;

import java.io.File;
import java.util.LinkedList;

/**
 * An object that holds configuration information for the media burn format.
 */

public class BurnConfig extends Structure implements MappingConfig {

// --- fields ---

   public File dataDir;
   public String command;
   public File workingDir;
   public LinkedList mappings;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      BurnConfig.class,
      0,0,
      new AbstractField[] {

         new FileField("dataDir","DataDir",0,null),
         new NullableStringField("command","Command",0,null),
         new NullableFileField("workingDir","WorkingDir",0,null),
         new StructureListField("mappings","Mapping",BurnMapping.sd,Merge.IDENTITY,0,0).with(MappingUtil.skuAccessor,MappingUtil.skuComparator)
      });

   protected StructureDefinition sd() { return sd; }

// --- validation ---

   public void validate() throws ValidationException {

      if ((command != null) != (workingDir != null)) throw new ValidationException(Text.get(this,"e1"));

      MappingUtil.validate(mappings);
   }

// --- implementation of MappingConfig ---

   public LinkedList getMappings() { return mappings; }
   public void putMappings(LinkedList mappings) { this.mappings = mappings; }

   public boolean mapsSpecialSKU() { return true; }

}

