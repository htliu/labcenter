/*
 * HelpItem.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.*;

/**
 * An object that holds information for a help menu item.
 */

public class HelpItem extends Structure {

// --- fields ---

   public String itemText;
   public String itemURL;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      HelpItem.class,
      // no version
      new AbstractField[] {

         new StringField("itemText","ItemText",0,null),
         new StringField("itemURL","ItemURL",0,null)
      });

   static { sd.setAttributed(); }
   protected StructureDefinition sd() { return sd; }

// --- validation ---

   public void validate() throws ValidationException {
   }

}

