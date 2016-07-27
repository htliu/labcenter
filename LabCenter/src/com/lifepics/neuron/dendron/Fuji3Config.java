/*
 * Fuji3Config.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.*;

import java.io.File;
import java.util.LinkedList;

/**
 * An object that holds configuration information for the Fuji JobMaker 1.4 format.
 */

public class Fuji3Config extends Structure implements MappingConfig {

// --- fields ---

   public File requestDir;
   public File imageDir;
   public File mapImageDir;
   public String printer;
   public Boolean autoCorrect;

   public boolean enableCompletion;
   public FujiCompletion completion;

   public LinkedList backprints;
   public LinkedList mappings;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      Fuji3Config.class,
      0,1,
      new AbstractField[] {

         new FileField("requestDir","RequestDir",0,new File("D:\\jobmaker")),
         new FileField("imageDir","ImageDir",0,new File("C:\\LifePics Orders\\Fuji")),
         new NullableFileField("mapImageDir","MapImageDir",0,new File("\\\\machine name\\LifePics Orders\\Fuji")),
         new NullableStringField("printer","Printer",0,null),
         new NullableBooleanField("autoCorrect","AutoCorrect",0,null),

         new BooleanField("enableCompletion","EnableCompletion",1,false),
         new StructureField("completion","Completion",FujiCompletion.sd,1),

         new StructureListField("backprints","Backprint",Backprint.sd,Merge.POSITION,0,1),
         new StructureListField("mappings","Mapping",Fuji3Mapping.sd,Merge.IDENTITY,0,0).with(MappingUtil.skuAccessor,MappingUtil.skuComparator)
      });

   protected StructureDefinition sd() { return sd; }

// --- validation ---

   public void validate() throws ValidationException {

      completion.validate();

      if (Backprint.hasSequence(backprints)) throw new ValidationException(Text.get(this,"e1"));
         // in the metafile, the backprint goes with the image, not the print,
         // so there's no way to have a sequence number without lots of file copies.

      Backprint.validate(backprints);
      MappingUtil.validate(mappings);
   }

// --- implementation of MappingConfig ---

   public LinkedList getMappings() { return mappings; }
   public void putMappings(LinkedList mappings) { this.mappings = mappings; }

   public boolean mapsSpecialSKU() { return false; }

}

