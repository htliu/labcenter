/*
 * KodakConfig.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.*;

import java.io.File;
import java.util.LinkedList;

import org.w3c.dom.Node;

/**
 * An object that holds configuration information for the Kodak kiosk format.
 */

public class KodakConfig extends Structure implements MappingConfig {

// --- fields ---

   public File dataDir;
   public boolean beginThermal;
   public boolean beginHold;
   public boolean claimedIsComplete;
   public boolean allowSingleSided;
   public boolean collateInReverse;
   public LinkedList mappings;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      KodakConfig.class,
      0,4,
      new AbstractField[] {

         new FileField("dataDir","DataDir",0,new File("\\\\machine name\\Inbox")),
         new BooleanField("beginThermal","BeginThermal",1,true),
         new BooleanField("beginHold","BeginHold",1,true),
         new BooleanField("claimedIsComplete","ClaimedIsComplete",1,true),
         new BooleanField("allowSingleSided","AllowSingleSided",3,false),
         new BooleanField("collateInReverse","CollateInReverse",4,false),
         new StructureListField("mappings","Mapping",KodakMapping.sd,Merge.IDENTITY,2,0) {
            protected void loadSpecial(Node node, Object o, int version) throws ValidationException {
               MappingUtil.migrate(tget(o),node,KodakMapping.sd);
            }
         }.with(MappingUtil.skuAccessor,MappingUtil.skuComparator)
      });

   protected StructureDefinition sd() { return sd; }

// --- validation ---

   public void validate() throws ValidationException {
      MappingUtil.validate(mappings);
   }

// --- implementation of MappingConfig ---

   public LinkedList getMappings() { return mappings; }
   public void putMappings(LinkedList mappings) { this.mappings = mappings; }

   public boolean mapsSpecialSKU() { return true; }

}

