/*
 * Location.java
 */

package com.lifepics.neuron.admin;

import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.*;

/**
 * An object that holds information about a location.
 */

public class Location extends Structure implements ILocation {

// --- fields ---

   public int merchantID;
   public int locationID; // PK
   public String name;
   public String city;
   public String state;
   public String phone;
   public Integer merchantLocationID;
   public String franchiseNumber;
   public String installerLookup; // fulfillment_id
   public Integer fulfillmentTypeID;
   public boolean isDeleted;
   public Boolean isPickup;
   public Integer invoiceVersionID;

   // links
   public Merchant merchant;
   public FulfillmentType fulfillmentType;
   public SoftwareVersion invoiceVersion;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      Location.class,
      0,0,
      new AbstractField[] {

         new IntegerField("merchantID","MerchantID"),
         new IntegerField("locationID","LocationID"),
         new StringField("name","Name"),
         new NullableStringField("city","City"),
         new NullableStringField("state","State"),
         new NullableStringField("phone","Phone"),
         new NullableIntegerField("merchantLocationID","MerchantLocationID"),
         new NullableStringField("franchiseNumber","FranchiseNumber"),
         new NullableStringField("installerLookup","InstallerLookup"),
         new NullableIntegerField("fulfillmentTypeID","FulfillmentTypeID"),
         new BooleanField("isDeleted","IsDeleted"),
         new NullableBooleanField("isPickup","IsPickup"),
         new NullableIntegerField("invoiceVersionID","InvoiceVersionID")
      });

   protected StructureDefinition sd() { return sd; }

// --- validation ---

   public void validate() throws ValidationException {
   }

// --- implementation of ILocation ---

   public Integer getMerchantID() { return new Integer(merchantID); }
   public Integer getLocationID() { return new Integer(locationID); }
   public String getName() { return name; }
   public String getCity() { return city; }
   public String getState() { return state; }
   public String getPhone() { return phone; }
   public Integer getMerchantLocationID() { return merchantLocationID; }
   public String getFranchiseNumber() { return franchiseNumber; }
   public String getInstallerLookup() { return installerLookup; }
   public Integer getFulfillmentTypeID() { return fulfillmentTypeID; }
   public Boolean getIsDeleted() { return new Boolean(isDeleted); }
   public Boolean getIsPickup() { return isPickup; }
   public Integer getInvoiceVersionID() { return invoiceVersionID; }

   public Merchant getMerchant() { return merchant; }
   public FulfillmentType getFulfillmentType() { return fulfillmentType; }
   public SoftwareVersion getInvoiceVersion() { return invoiceVersion; }

}

