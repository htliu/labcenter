/*
 * IInstance.java
 */

package com.lifepics.neuron.admin;

/**
 * An interface to abstract over Instance and NoInstance.
 */

public interface IInstance {

   String getLineType();
   Integer getLineID();

   Integer getInstanceID();
   Integer getPasscode();
   Integer getRevisionNumber();
   Integer getLocationID();
   Integer getWholesalerID();
   Integer getLabCenterVersionID();
   Boolean getUseDefaultVersion();
   String getDisplayVersion();
   Integer getInvoiceVersionID();
   String getReportedLabCenterVersion();
   String getReportedJavaVersion();
   String getReportedIntegrations();
   String getLastConfigRead();
   String getLastConfigUpdate();
   String getLastStatusReport();
   Boolean getReportedDL();
   Boolean getReportedUL();
   Boolean getHasReportRow();

   ILocation getLocation();
   Wholesaler getWholesaler();
   LabCenterVersion getLabCenterVersion();
   SoftwareVersion getInvoiceVersion();

}

