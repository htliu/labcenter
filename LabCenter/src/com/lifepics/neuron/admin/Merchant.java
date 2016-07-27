/*
 * Merchant.java
 */

package com.lifepics.neuron.admin;

import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.*;

/**
 * An object that holds information about a merchant.
 */

public class Merchant extends Structure {

// --- fields ---

   public int merchantID;
   public String name;
   public Integer invoiceVersionID;

   // links
   public SoftwareVersion invoiceVersion;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      Merchant.class,
      0,0,
      new AbstractField[] {

         new IntegerField("merchantID","MerchantID"),
         new NullableStringField("name","Name"),
         new NullableIntegerField("invoiceVersionID","InvoiceVersionID")
      });

   protected StructureDefinition sd() { return sd; }

// --- validation ---

   public void validate() throws ValidationException {
   }

}

