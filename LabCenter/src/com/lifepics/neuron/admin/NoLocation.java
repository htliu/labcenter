/*
 * NoLocation.java
 */

package com.lifepics.neuron.admin;

/**
 * An object that represents a nonexistent location.
 */

public class NoLocation implements ILocation {

   public Merchant merchant;

   public NoLocation(Merchant merchant) {
      this.merchant = merchant;
   }

// --- implementation of ILocation ---

   public Integer getMerchantID() { return null; }
   public Integer getLocationID() { return null; }
   public String getName() { return null; }
   public String getCity() { return null; }
   public String getState() { return null; }
   public String getPhone() { return null; }
   public Integer getMerchantLocationID() { return null; }
   public String getFranchiseNumber() { return null; }
   public String getInstallerLookup() { return null; }
   public Integer getFulfillmentTypeID() { return null; }
   public Boolean getIsDeleted() { return null; }
   public Boolean getIsPickup() { return null; }
   public Integer getInvoiceVersionID() { return null; }

   public Merchant getMerchant() { return merchant; }
   public FulfillmentType getFulfillmentType() { return null; }
   public SoftwareVersion getInvoiceVersion() { return null; }

}

