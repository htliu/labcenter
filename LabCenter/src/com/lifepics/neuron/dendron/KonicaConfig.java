/*
 * KonicaConfig.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.*;

import java.io.File;
import java.util.LinkedList;

import org.w3c.dom.Node;

/**
 * An object that holds configuration information for the Konica format.
 * This is not quite the same as Noritsu ... e.g., channels are strings.
 */

public class KonicaConfig extends Structure implements MappingConfig {

// --- fields ---

   public File dataDir;
   public LinkedList backprints;
   public LinkedList mappings;

// --- n-digit values ---

   // these aren't used here, but this is a good place for them
   public static final int NDIGIT_ORDER_ID = 9;
   public static final int NDIGIT_PID = 3;
   public static final int NDIGIT_QUANTITY = 3;

   public static final int CHUNK_COUNT_STD = 500;
   public static final int CHUNK_COUNT_MAX = 999; /// related to NDIGIT_PID

   public static final int CHUNK_STD = 500;
   public static final int CHUNK_MAX = 999; // related to NDIGIT_QUANTITY

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      KonicaConfig.class,
      0,1,
      new AbstractField[] {

         new FileField("dataDir","DataDir",0,null),
         new StructureListField("backprints","Backprint",Backprint.sd,Merge.POSITION,0,1),
         new StructureListField("mappings","Mapping",KonicaMapping.sd,Merge.IDENTITY,1,0) {
            protected void loadSpecial(Node node, Object o, int version) throws ValidationException {
               MappingUtil.migrate(tget(o),node,KonicaMapping.sd);
            }
         }.with(MappingUtil.skuAccessor,MappingUtil.skuComparator)
      });

   protected StructureDefinition sd() { return sd; }

// --- validation ---

   public void validate() throws ValidationException {
      Backprint.validate(backprints);
      MappingUtil.validate(mappings);
   }

// --- implementation of MappingConfig ---

   public LinkedList getMappings() { return mappings; }
   public void putMappings(LinkedList mappings) { this.mappings = mappings; }

   public boolean mapsSpecialSKU() { return true; }

}

