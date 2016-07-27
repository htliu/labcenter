/*
 * AgfaConfig.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.*;

import java.io.File;
import java.util.LinkedList;

import org.w3c.dom.Node;

/**
 * An object that holds configuration information for the Agfa format.
 */

public class AgfaConfig extends Structure implements MappingConfig {

// --- fields ---

   public File databaseDir;
   public String host;
   public String service; // basically port
   public int batchNumberPoolsize;
   public int imageNumberPoolsize;
   public String dealerNumber; // interface defines this as a string, I don't know why
   public String dealerName;
   public int operatorNumber;
   public String operatorName;
   public int sourceDeviceID;
   public Integer targetDeviceID; // null to omit
   public String refnumPrefix;

   public LinkedList backprints;
   public LinkedList mappings;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      AgfaConfig.class,
      0,1,
      new AbstractField[] {

         new FileField("databaseDir","DatabaseDir",0,null),
         new StringField("host","Host",0,null),
         new StringField("service","Service",0,Agfa.SERVICE_DEFAULT),
         new IntegerField("batchNumberPoolsize","BatchNumberPoolsize",0,10), // arbitrary default
         new IntegerField("imageNumberPoolsize","ImageNumberPoolsize",0,10),
         new StringField("dealerNumber","DealerNumber",0,""), // these four apparently do nothing
         new StringField("dealerName","DealerName",0,""),
         new IntegerField("operatorNumber","OperatorNumber",0,0),
         new StringField("operatorName","OperatorName",0,""),
         new IntegerField("sourceDeviceID","SourceDeviceID",0,Agfa.DEVICE_ID_CONNECTION_KIT),
         new NullableIntegerField("targetDeviceID","TargetDeviceID",0,null),
         new StringField("refnumPrefix","RefnumPrefix",0,"LP #"),

         new StructureListField("backprints","Backprint",Backprint.sd,Merge.POSITION,0,2),
         new StructureListField("mappings","Mapping",AgfaMapping.sd,Merge.IDENTITY,1,0) {
            protected void loadSpecial(Node node, Object o, int version) throws ValidationException {
               MappingUtil.migrate(tget(o),node,AgfaMapping.sd);
            }
         }.with(MappingUtil.skuAccessor,MappingUtil.skuComparator)
      });

   protected StructureDefinition sd() { return sd; }

// --- validation ---

   public void validate() throws ValidationException {

      if (batchNumberPoolsize < 1) throw new ValidationException(Text.get(this,"e1"));
      if (imageNumberPoolsize < 1) throw new ValidationException(Text.get(this,"e2"));

      Backprint.validate(backprints);
      MappingUtil.validate(mappings);
   }

// --- implementation of MappingConfig ---

   public LinkedList getMappings() { return mappings; }
   public void putMappings(LinkedList mappings) { this.mappings = mappings; }

   public boolean mapsSpecialSKU() { return true; }

}

