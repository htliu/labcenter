/*
 * PixelConfig.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.*;

import java.io.File;
import java.util.LinkedList;

import org.w3c.dom.Node;

/**
 * An object that holds configuration information for the Pixel Magic format.
 */

public class PixelConfig extends Structure implements MappingConfig {

// --- fields ---

   public File dataDir;
   public File mapDataDir; // nullable (but usually not)
   public String baseURL;
   public int nodeID;
   public int transactionTypeID;
   public String refnumPrefix;
   public boolean separatorSheet;
   public boolean allowMultiPane;
   public Integer deviceGroup;
   public int  cdOverflowLimit; // special value zero meaning no limit
   public long cdOverflowBytes; //
   public boolean cdOverflowSplit;
   public boolean printSingleSided;
   public LinkedList mappings;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      PixelConfig.class,
      0,new History(new int[] {3,773,4,775,5}),
      new AbstractField[] {

         new FileField("dataDir","DataDir",0,null),
         new NullableFileField("mapDataDir","MapDataDir",0,null),
         new StringField("baseURL","BaseURL",0,null),
         new IntegerField("nodeID","NodeID",0,500),
         new IntegerField("transactionTypeID","TransactionTypeID",0,30),
         new StringField("refnumPrefix","RefnumPrefix",0,"LP #"), // like Agfa
         new BooleanField("separatorSheet","SeparatorSheet",0,true),
         new BooleanField("allowMultiPane","AllowMultiPane",3,false),
         new NullableIntegerField("deviceGroup","DeviceGroup",0,null),
         new IntegerField("cdOverflowLimit","CDOverflowLimit",1,0),
         new LongField   ("cdOverflowBytes","CDOverflowBytes",5,0),
         new BooleanField("cdOverflowSplit","CDOverflowSplit",1,false),
         new BooleanField("printSingleSided","PrintSingleSided",4,false),
         new StructureListField("mappings","Mapping",PixelMapping.sd,Merge.IDENTITY,2,0) {
            protected void loadSpecial(Node node, Object o, int version) throws ValidationException {
               MappingUtil.migrate(tget(o),node,PixelMapping.sd);
            }
         }.with(MappingUtil.skuAccessor,MappingUtil.skuComparator)
      });

   protected StructureDefinition sd() { return sd; }

// --- validation ---

   public void validate() throws ValidationException {

      if (    cdOverflowLimit < 0
           || cdOverflowBytes < 0 ) throw new ValidationException(Text.get(this,"e3"));

      MappingUtil.validate(mappings);
   }

// --- implementation of MappingConfig ---

   public LinkedList getMappings() { return mappings; }
   public void putMappings(LinkedList mappings) { this.mappings = mappings; }

   public boolean mapsSpecialSKU() { return true; }

}

