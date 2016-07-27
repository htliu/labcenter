/*
 * FulfillmentType.java
 */

package com.lifepics.neuron.admin;

import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.*;

/**
 * An object that holds information about a fulfillment type.
 */

public class FulfillmentType extends Structure {

// --- fields ---

   public Integer merchantID; // null for generic fulfillment types
   public int fulfillmentTypeID; // PK
   public String displayName;
   public String configFileName;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      FulfillmentType.class,
      0,0,
      new AbstractField[] {

         new NullableIntegerField("merchantID","MerchantID"),
         new IntegerField("fulfillmentTypeID","FulfillmentTypeID"),
         new NullableStringField("displayName","DisplayName"),
         new NullableStringField("configFileName","ConfigFileName")
      });

   protected StructureDefinition sd() { return sd; }

// --- validation ---

   public void validate() throws ValidationException {
   }

}

