/*
 * NoritsuConfig.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.*;

import java.io.File;
import java.util.LinkedList;

import org.w3c.dom.Node;

/**
 * An object that holds configuration information for the Noritsu format.
 */

public class NoritsuConfig extends Structure implements MappingConfig {

// --- fields ---

   public File dataDir;
   public boolean strict;
   public boolean limitEnable;
   public int limitLength;
   public boolean printSingleSided;
   public boolean collateInReverse;
   public LinkedList backprints;
   public LinkedList mappings;

// --- n-digit values ---

   // these aren't used here, but this is a good place for them
   public static final int NDIGIT_ORDER_ID = 7;
   public static final int NDIGIT_SHORT_ID = 5;
   public static final int NDIGIT_PID = 3;
   public static final int NDIGIT_QUANTITY = 3;
   public static final int NDIGIT_SET = 4;

   public static final int CHUNK_COUNT_STD = 500;
   public static final int CHUNK_COUNT_MAX = 999; /// related to NDIGIT_PID

   public static final int CHUNK_STD = 500;
   public static final int CHUNK_MAX = 999; // related to NDIGIT_QUANTITY

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      NoritsuConfig.class,
      0,4,
      new AbstractField[] {

         new FileField("dataDir","DataDir",0,null),
         new BooleanField("strict","Strict",1,false),
         new BooleanField("limitEnable","LimitEnable",2,true),
         new IntegerField("limitLength","LimitLength",2,35),
         new BooleanField("printSingleSided","PrintSingleSided",4,false),
         new BooleanField("collateInReverse","CollateInReverse",4,false),
         new StructureListField("backprints","Backprint",Backprint.sd,Merge.POSITION,0,2),
         new StructureListField("mappings","Mapping",NoritsuMapping.sd,Merge.IDENTITY,3,0) {
            protected void loadSpecial(Node node, Object o, int version) throws ValidationException {
               MappingUtil.migrate(tget(o),node,NoritsuMapping.sd);
            }
         }.with(MappingUtil.skuAccessor,MappingUtil.skuComparator)
      });

   protected StructureDefinition sd() { return sd; }

// --- validation ---

   public void validate() throws ValidationException {

      if (limitLength < 8) throw new ValidationException(Text.get(this,"e1"));

      Backprint.validate(backprints);
      MappingUtil.validate(mappings);
   }

// --- implementation of MappingConfig ---

   public LinkedList getMappings() { return mappings; }
   public void putMappings(LinkedList mappings) { this.mappings = mappings; }

   public boolean mapsSpecialSKU() { return true; }

}

