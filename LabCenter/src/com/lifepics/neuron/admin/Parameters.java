/*
 * Parameters.java
 */

package com.lifepics.neuron.admin;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.object.CopyUtil;
import com.lifepics.neuron.struct.*;

import java.util.LinkedList;

/**
 * An object that holds the parameters for a snapshot.
 */

public class Parameters extends Structure {

// --- fields ---

   public LinkedList merchants;
   public LinkedList locations;
   public LinkedList wholesalers;
   public LinkedList instances;

   public boolean includeDeletedLocations;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      Parameters.class,
      0,0,
      new AbstractField[] {

         new IntegerListField("merchants","Merchant"),
         new IntegerListField("locations","Location"),
         new IntegerListField("wholesalers","Wholesaler"),
         new IntegerListField("instances","Instance"),

         new BooleanField("includeDeletedLocations","IncludeDeletedLocations")
      });

   protected StructureDefinition sd() { return sd; }

// --- copy function ---

   public Parameters copy() { return (Parameters) CopyUtil.copy(this); }

// --- validation ---

   public void validate() throws ValidationException {

      if (    merchants.size() == 0
           && locations.size() == 0
           && wholesalers.size() == 0
           && instances.size() == 0 ) throw new ValidationException(Text.get(this,"e1"));
   }

}

