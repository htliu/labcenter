/*
 * DKS3Config.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.*;

import java.io.File;
import java.util.LinkedList;

import org.w3c.dom.Node;

/**
 * An object that holds configuration information for the DKS3 format.
 */

public class DKS3Config extends Structure implements MappingConfig {

// --- fields ---

   public File dataDir;
   public String prefix;
   public int priority;
   public boolean completeImmediately;
   public boolean autoArchive;

   public LinkedList backprints;
   public LinkedList mappings;

// --- constants ---

   public static final int PRIORITY_MIN    = 0;
   public static final int PRIORITY_LOW    = 0;
   public static final int PRIORITY_NORMAL = 1;
   public static final int PRIORITY_HIGH   = 2;
   public static final int PRIORITY_MAX    = 2;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      DKS3Config.class,
      0,1,
      new AbstractField[] {

         new FileField("dataDir","DataDir",0,new File("\\\\dks\\rep_dks\\sortie_print")),
         new StringField("prefix","Prefix",0,"LP"),
         new IntegerField("priority","Priority",0,PRIORITY_NORMAL),
         new BooleanField("completeImmediately","CompleteImmediately",0,false),
         new BooleanField("autoArchive","AutoArchive",0,false),

         new StructureListField("backprints","Backprint",Backprint.sd,Merge.POSITION,0,2),
         new StructureListField("mappings","Mapping",DKS3Mapping.sd,Merge.IDENTITY,1,0) {
            protected void loadSpecial(Node node, Object o, int version) throws ValidationException {
               MappingUtil.migrate(tget(o),node,DKS3Mapping.sd);
            }
         }.with(MappingUtil.skuAccessor,MappingUtil.skuComparator)
      });

   protected StructureDefinition sd() { return sd; }

// --- validation ---

   public void validate() throws ValidationException {

      if (priority < PRIORITY_MIN || priority > PRIORITY_MAX) {
         throw new ValidationException(Text.get(this,"e1",new Object[] { Convert.fromInt(priority) }));
      }

      if (Backprint.hasSequence(backprints)) throw new ValidationException(Text.get(this,"e9"));

      Backprint.validate(backprints);
      MappingUtil.validate(mappings);
   }

// --- implementation of MappingConfig ---

   public LinkedList getMappings() { return mappings; }
   public void putMappings(LinkedList mappings) { this.mappings = mappings; }

   public boolean mapsSpecialSKU() { return true; }

}

