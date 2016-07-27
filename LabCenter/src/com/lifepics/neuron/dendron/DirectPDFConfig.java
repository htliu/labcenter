/*
 * DirectPDFConfig.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.gui.Orientation;
import com.lifepics.neuron.struct.*;

/**
 * An object that holds configuration information for the direct-print PDF format.
 */

public class DirectPDFConfig extends PJLHelper {

// --- fields ---

   // PJLHelper fields
   //
   public boolean completeImmediately;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      DirectPDFConfig.class,
      0,new History(new int[] {3,774,4}),
      new AbstractField[] {

         new NullableStringField("printer","Printer",0,null),
         new NullableStringField("tray","Tray",2,null),
         new BooleanField("trayXerox","TrayXerox",2,false),
         new NullableEnumeratedField("orientation","Orientation",Orientation.orientationType,3,null),
         new NullableEnumeratedField("sides","Sides",DirectEnum.sidesType,0,null),
         new NullableEnumeratedField("collate","Collate",DirectEnum.collateType,1,null),
         new BooleanField("sendPJL","SendPJL",4,true),
         new BooleanField("sendJOB","SendJOB",4,false),
         new BooleanField("sendEOJ","SendEOJ",4,true),
         new BooleanField("sendEOL","SendEOL",4,false),
         new BooleanField("sendUEL","SendUEL",4,false),
         new InlineListField("pjl","PJL",2),
         new BooleanField("completeImmediately","CompleteImmediately",0,false)
      });

   protected StructureDefinition sd() { return sd; }

// --- validation ---

   public void validate() throws ValidationException {
      super.validate();

      // no other validations at the moment
   }

}

