/*
 * Memory.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.*;

import java.io.File;
import java.util.LinkedList;

/**
 * An object that holds information that changes more rapidly,
 * than true configuration info, like the email address book.
 */

public class Memory extends Structure {

// --- fields ---

   public File manualImageDir;
   public LinkedList addressList; // list of strings
   public LinkedList dlsOrderIDs; // list of strings

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      Memory.class,
      0,1,
      new AbstractField[] {

         new FileField("manualImageDir","ManualImageDir",0,null), // custom loadDefault
         new StringListField("addressList","AddressList","Address",0),
         new StringListField("dlsOrderIDs","DLSOrderIDList","ID",1)
      });

   protected StructureDefinition sd() { return sd; }

// --- validation ---

   public void validate() throws ValidationException {
   }

// --- persistence ---

   public Object loadDefault(File manualImageDir) {
      loadDefault();

      this.manualImageDir = manualImageDir; // overwrite null default

      return this; // convenience
   }

}

