/*
 * NoInstance.java
 */

package com.lifepics.neuron.admin;

import com.lifepics.neuron.core.Text;

/**
 * An object that represents a nonexistent instance.
 */

public class NoInstance implements IInstance {

   public ILocation location;
   public Wholesaler wholesaler;
   public String lineType;
   public Integer lineID;

   public NoInstance(ILocation location) {
      this.location = location;
      this.wholesaler = null;
      if (location.getLocationID() != null) { // Location
         lineType = LINE_TYPE_LOCATION;
         lineID = location.getLocationID();
      } else { // NoLocation
         lineType = LINE_TYPE_MERCHANT;
         lineID = new Integer(location.getMerchant().merchantID);
      }
   }

   public NoInstance(Wholesaler wholesaler) {
      this.location = null;
      this.wholesaler = wholesaler;
      lineType = LINE_TYPE_WHOLESALER;
      lineID = new Integer(wholesaler.wholesalerID);
   }

// --- constants ---

   private static String LINE_TYPE_MERCHANT = Text.get(NoInstance.class,"s1");
   private static String LINE_TYPE_LOCATION = Text.get(NoInstance.class,"s2");
   private static String LINE_TYPE_WHOLESALER = Text.get(NoInstance.class,"s3");

// --- implementation of IInstance ---

   public String getLineType() { return lineType; }
   public Integer getLineID() { return lineID; }

   public Integer getInstanceID() { return null; }
   public Integer getPasscode() { return null; }
   public Integer getRevisionNumber() { return null; }
   public Integer getLocationID() { return null; }
   public Integer getWholesalerID() { return null; }
   public Integer getLabCenterVersionID() { return null; }
   public Boolean getUseDefaultVersion() { return null; }
   public String getDisplayVersion() { return null; }
   public Integer getInvoiceVersionID() { return null; }
   public String getReportedLabCenterVersion() { return null; }
   public String getReportedJavaVersion() { return null; }
   public String getReportedIntegrations() { return null; }
   public String getLastConfigRead() { return null; }
   public String getLastConfigUpdate() { return null; }
   public String getLastStatusReport() { return null; }
   public Boolean getReportedDL() { return null; }
   public Boolean getReportedUL() { return null; }
   public Boolean getHasReportRow() { return null; }

   public ILocation getLocation() { return location; }
   public Wholesaler getWholesaler() { return wholesaler; }
   public LabCenterVersion getLabCenterVersion() { return null; }
   public SoftwareVersion getInvoiceVersion() { return null; }

}

