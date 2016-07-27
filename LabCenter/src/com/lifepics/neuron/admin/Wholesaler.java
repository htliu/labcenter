/*
 * Wholesaler.java
 */

package com.lifepics.neuron.admin;

import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.*;

/**
 * An object that holds information about a wholesaler.
 */

public class Wholesaler extends Structure {

// --- fields ---

   public int wholesalerID;
   public String name;
   public Integer invoiceVersionID;

   // links
   public SoftwareVersion invoiceVersion;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      Wholesaler.class,
      0,0,
      new AbstractField[] {

         new IntegerField("wholesalerID","WholesalerID"),
         new StringField("name","Name"),
         new NullableIntegerField("invoiceVersionID","InvoiceVersionID")
      });

   protected StructureDefinition sd() { return sd; }

// --- validation ---

   public void validate() throws ValidationException {
   }

}

