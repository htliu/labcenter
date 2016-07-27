/*
 * RawJPEGConfig.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.gui.Orientation;
import com.lifepics.neuron.struct.*;

import java.util.LinkedList;

/**
 * An object that holds configuration information for the direct-print JPEG format.
 */

public class RawJPEGConfig extends PJLHelper implements MappingConfig {

// --- constants ---

   // on HP it turns out that JPEG is the right one
   public static final String LANGUAGE_AUTO = "AUTO";
   public static final String LANGUAGE_JPEG = "JPEG";

   // minimum and maximum values for scaling on the HP
   public static final int SCALE_MIN = 25;
   public static final int SCALE_MAX = 419;

// --- fields ---

   // PJLHelper fields
   //
   public boolean completeImmediately;
   public String language;
   public boolean scaleEnable;
   public boolean scaleRoundDown;
   public LinkedList mappings;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      RawJPEGConfig.class,
      0,new History(new int[] {0,775,1}),
      new AbstractField[] {

         new NullableStringField("printer","Printer",0,null),
         new NullableStringField("tray","Tray",0,null),
         new BooleanField("trayXerox","TrayXerox",0,false),
         new NullableEnumeratedField("orientation","Orientation",Orientation.orientationType,0,null),
         new NullableEnumeratedField("sides","Sides",DirectEnum.sidesType,0,null),
         new NullableEnumeratedField("collate","Collate",DirectEnum.collateType,0,null),
         new BooleanField("sendPJL","SendPJL",0,false), // defaults here are different than in DirectPDFConfig !
         new BooleanField("sendJOB","SendJOB",0,true),  //
         new BooleanField("sendEOJ","SendEOJ",0,true),  //
         new BooleanField("sendEOL","SendEOL",0,true),  //
         new BooleanField("sendUEL","SendUEL",0,true),  //
         new InlineListField("pjl","PJL",0),
         new BooleanField("completeImmediately","CompleteImmediately",0,false),
         new StringField("language","Language",0,LANGUAGE_JPEG),
         new BooleanField("scaleEnable","ScaleEnable",1,false),
         new BooleanField("scaleRoundDown","ScaleRoundDown",1,false),
         new StructureListField("mappings","Mapping",RawJPEGMapping.sd,Merge.IDENTITY,0,0).with(MappingUtil.skuAccessor,MappingUtil.skuComparator)
      });

   protected StructureDefinition sd() { return sd; }

// --- validation ---

   public void validate() throws ValidationException {
      super.validate();

      if (language.length() == 0) throw new ValidationException(Text.get(this,"e1"));
      MappingUtil.validate(mappings);
   }

// --- implementation of MappingConfig ---

   public LinkedList getMappings() { return mappings; }
   public void putMappings(LinkedList mappings) { this.mappings = mappings; }

   public boolean mapsSpecialSKU() { return true; }

}

