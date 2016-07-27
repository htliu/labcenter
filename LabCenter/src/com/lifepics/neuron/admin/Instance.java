/*
 * Instance.java
 */

package com.lifepics.neuron.admin;

import com.lifepics.neuron.app.Config;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.*;

/**
 * An object that holds information about a single instance.
 * The information comes from LabCenterConfig joined to
 * various other tables (LCSoftware, MELOCS_LabCenterStatus, etc.)
 */

public class Instance extends Structure implements IInstance {

// --- fields ---

   public int instanceID;
   public Integer passcode;
   public Integer revisionNumber;
   public Integer locationID;
   public Integer wholesalerID;
   public Integer labCenterVersionID;
   public Boolean useDefaultVersion;
   public Integer invoiceVersionID;
   public String reportedLabCenterVersion;
   public String reportedJavaVersion;
   public String reportedIntegrations;
   public String lastConfigRead;
   public String lastConfigUpdate;
   public String lastStatusReport;
   public Boolean reportedDL;
   public Boolean reportedUL;
   public boolean hasReportRow;

   // links
   public Location location;
   public Wholesaler wholesaler;
   public LabCenterVersion labCenterVersion;
   public SoftwareVersion invoiceVersion;

   public Config configBase;
   public Config configEdit;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      Instance.class,
      0,0,
      new AbstractField[] {

         new IntegerField("instanceID","InstanceID"),
         new NullableIntegerField("passcode","Passcode"),
         new NullableIntegerField("revisionNumber","RevisionNumber"),
         new NullableIntegerField("locationID","LocationID"),
         new NullableIntegerField("wholesalerID","WholesalerID"),
         new NullableIntegerField("labCenterVersionID","LabCenterVersionID"),
         new NullableBooleanField("useDefaultVersion","UseDefaultVersion"),
         new NullableIntegerField("invoiceVersionID","InvoiceVersionID"),
         new NullableStringField("reportedLabCenterVersion","ReportedLabCenterVersion"),
         new NullableStringField("reportedJavaVersion","ReportedJavaVersion"),
         new NullableStringField("reportedIntegrations","ReportedIntegrations"),
         new NullableStringField("lastConfigRead","LastConfigRead"),
         new NullableStringField("lastConfigUpdate","LastConfigUpdate"),
         new NullableStringField("lastStatusReport","LastStatusReport"),
         new NullableBooleanField("reportedDL","ReportedDL"),
         new NullableBooleanField("reportedUL","ReportedUL"),
         new BooleanField("hasReportRow","HasReportRow"),
      });

   protected StructureDefinition sd() { return sd; }

// --- validation ---

   public void validate() throws ValidationException {
   }

// --- implementation of IInstance ---

   public String getLineType() { return null; }
   public Integer getLineID() { return new Integer(instanceID); }

   public Integer getInstanceID() { return new Integer(instanceID); }
   public Integer getPasscode() { return passcode; }
   public Integer getRevisionNumber() { return revisionNumber; }
   public Integer getLocationID() { return locationID; }
   public Integer getWholesalerID() { return wholesalerID; }
   public Integer getLabCenterVersionID() { return labCenterVersionID; }
   public Boolean getUseDefaultVersion() { return useDefaultVersion; }
   public String getDisplayVersion() { return DisplayVersion.get(this).toString(); }
   public Integer getInvoiceVersionID() { return invoiceVersionID; }
   public String getReportedLabCenterVersion() { return reportedLabCenterVersion; }
   public String getReportedJavaVersion() { return reportedJavaVersion; }
   public String getReportedIntegrations() { return reportedIntegrations; }
   public String getLastConfigRead() { return lastConfigRead; }
   public String getLastConfigUpdate() { return lastConfigUpdate; }
   public String getLastStatusReport() { return lastStatusReport; }
   public Boolean getReportedDL() { return reportedDL; }
   public Boolean getReportedUL() { return reportedUL; }
   public Boolean getHasReportRow() { return new Boolean(hasReportRow); }

   public ILocation getLocation() { return location; }
   public Wholesaler getWholesaler() { return wholesaler; }
   public LabCenterVersion getLabCenterVersion() { return labCenterVersion; }
   public SoftwareVersion getInvoiceVersion() { return invoiceVersion; }

// --- other methods ---

   public static SoftwareVersion getComputedInvoiceVersion(IInstance instance) {
      SoftwareVersion invoice;

      invoice = instance.getInvoiceVersion(); // instance goes first
      if (invoice != null) return invoice;

      ILocation location = instance.getLocation();
      if (location != null) {
         invoice = location.getInvoiceVersion();
         if (invoice != null) return invoice;

         Merchant merchant = location.getMerchant();
         if (merchant != null) {
            invoice = merchant.invoiceVersion;
            if (invoice != null) return invoice;
         }
      }

      Wholesaler wholesaler = instance.getWholesaler();
      if (wholesaler != null) {
         invoice = wholesaler.invoiceVersion;
         if (invoice != null) return invoice;
      }

      return null; // possibly systemwide default, but we don't track that here
   }

}

