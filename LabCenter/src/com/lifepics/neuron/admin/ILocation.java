/*
 * ILocation.java
 */

package com.lifepics.neuron.admin;

/**
 * An interface to abstract over Location and NoLocation.
 */

public interface ILocation {

   Integer getMerchantID();
   Integer getLocationID();
   String getName();
   String getCity();
   String getState();
   String getPhone();
   Integer getMerchantLocationID();
   String getFranchiseNumber();
   String getInstallerLookup();
   Integer getFulfillmentTypeID();
   Boolean getIsDeleted();
   Boolean getIsPickup();
   Integer getInvoiceVersionID();

   Merchant getMerchant();
   FulfillmentType getFulfillmentType();
   SoftwareVersion getInvoiceVersion();

}

