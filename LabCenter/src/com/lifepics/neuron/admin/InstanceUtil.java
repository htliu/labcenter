/*
 * InstanceUtil.java
 */

package com.lifepics.neuron.admin;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.gui.GridColumn;
import com.lifepics.neuron.meta.Accessor;
import com.lifepics.neuron.meta.BooleanComparator;
import com.lifepics.neuron.meta.FieldComparator;
import com.lifepics.neuron.meta.NaturalComparator;
import com.lifepics.neuron.meta.NoCaseComparator;
import com.lifepics.neuron.meta.OuterAccessor;

import java.util.Comparator;

/**
 * A utility class containing accessors, comparators, and grid columns.
 * This is more complicated than usual because some of the columns use
 * outer joins to other objects.
 */

public class InstanceUtil {

// --- merchant ---

   public static Accessor merMerchantID = new Accessor() {
      public Class getFieldClass() { return Integer.class; }
      public Object get(Object o) { return new Integer(((Merchant) o).merchantID); }
   };

   public static Accessor merName = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return ((Merchant) o).name; }
   };

   public static Accessor merInvoiceVersion = new Accessor() {
      public Class getFieldClass() { return SoftwareVersion.class; }
      public Object get(Object o) { return ((Merchant) o).invoiceVersion; }
   };

// --- location ---

   public static Accessor locMerchant = new Accessor() {
      public Class getFieldClass() { return Merchant.class; }
      public Object get(Object o) { return ((ILocation) o).getMerchant(); }
   };

   public static Accessor locLocationID = new Accessor() {
      public Class getFieldClass() { return Integer.class; }
      public Object get(Object o) { return ((ILocation) o).getLocationID(); }
   };

   public static Accessor locName = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return ((ILocation) o).getName(); }
   };

   public static Accessor locCity = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return ((ILocation) o).getCity(); }
   };

   public static Accessor locState = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return ((ILocation) o).getState(); }
   };

   public static Accessor locPhone = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return ((ILocation) o).getPhone(); }
   };

   public static Accessor locMerchantLocationID = new Accessor() {
      public Class getFieldClass() { return Integer.class; }
      public Object get(Object o) { return ((ILocation) o).getMerchantLocationID(); }
   };

   public static Accessor locFranchiseNumber = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return ((ILocation) o).getFranchiseNumber(); }
   };

   public static Accessor locInstallerLookup = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return ((ILocation) o).getInstallerLookup(); }
   };

   public static Accessor locFulfillmentType = new Accessor() {
      public Class getFieldClass() { return FulfillmentType.class; }
      public Object get(Object o) { return ((ILocation) o).getFulfillmentType(); }
   };

   public static Accessor locIsDeleted = new Accessor() {
      public Class getFieldClass() { return Boolean.class; }
      public Object get(Object o) { return ((ILocation) o).getIsDeleted(); }
   };

   public static Accessor locIsPickup = new Accessor() {
      public Class getFieldClass() { return Boolean.class; }
      public Object get(Object o) { return ((ILocation) o).getIsPickup(); }
   };

   public static Accessor locInvoiceVersion = new Accessor() {
      public Class getFieldClass() { return SoftwareVersion.class; }
      public Object get(Object o) { return ((ILocation) o).getInvoiceVersion(); }
   };

// --- wholesaler ---

   public static Accessor whsWholesalerID = new Accessor() {
      public Class getFieldClass() { return Integer.class; }
      public Object get(Object o) { return new Integer(((Wholesaler) o).wholesalerID); }
   };

   public static Accessor whsName = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return ((Wholesaler) o).name; }
   };

   public static Accessor whsInvoiceVersion = new Accessor() {
      public Class getFieldClass() { return SoftwareVersion.class; }
      public Object get(Object o) { return ((Wholesaler) o).invoiceVersion; }
   };

// --- instance ---

   public static Accessor insLineType = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return ((IInstance) o).getLineType(); }
   };

   public static Accessor insLineID = new Accessor() {
      public Class getFieldClass() { return Integer.class; }
      public Object get(Object o) { return ((IInstance) o).getLineID(); }
   };

   public static Accessor insInstanceID = new Accessor() {
      public Class getFieldClass() { return Integer.class; }
      public Object get(Object o) { return ((IInstance) o).getInstanceID(); }
   };

   public static Accessor insPasscode = new Accessor() {
      public Class getFieldClass() { return Integer.class; }
      public Object get(Object o) { return ((IInstance) o).getPasscode(); }
   };

   public static Accessor insRevisionNumber = new Accessor() {
      public Class getFieldClass() { return Integer.class; }
      public Object get(Object o) { return ((IInstance) o).getRevisionNumber(); }
   };

   public static Accessor insLocation = new Accessor() {
      public Class getFieldClass() { return ILocation.class; }
      public Object get(Object o) { return ((IInstance) o).getLocation(); }
   };

   public static Accessor insWholesaler = new Accessor() {
      public Class getFieldClass() { return Wholesaler.class; }
      public Object get(Object o) { return ((IInstance) o).getWholesaler(); }
   };

   public static Accessor insLabCenterVersion = new Accessor() {
      public Class getFieldClass() { return LabCenterVersion.class; }
      public Object get(Object o) { return ((IInstance) o).getLabCenterVersion(); }
   };

   public static Accessor insUseDefaultVersion = new Accessor() {
      public Class getFieldClass() { return Boolean.class; }
      public Object get(Object o) { return ((IInstance) o).getUseDefaultVersion(); }
   };

   public static Accessor insDisplayVersion = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return ((IInstance) o).getDisplayVersion(); }
   };

   public static Accessor insInvoiceVersion = new Accessor() {
      public Class getFieldClass() { return SoftwareVersion.class; }
      public Object get(Object o) { return ((IInstance) o).getInvoiceVersion(); }
   };

   public static Accessor insReportedLabCenterVersion = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return ((IInstance) o).getReportedLabCenterVersion(); }
   };

   public static Accessor insReportedJavaVersion = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return ((IInstance) o).getReportedJavaVersion(); }
   };

   public static Accessor insReportedIntegrations = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return ((IInstance) o).getReportedIntegrations(); }
   };

   public static Accessor insLastConfigRead = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return ((IInstance) o).getLastConfigRead(); }
   };

   public static Accessor insLastConfigUpdate = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return ((IInstance) o).getLastConfigUpdate(); }
   };

   public static Accessor insLastStatusReport = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return ((IInstance) o).getLastStatusReport(); }
   };

   public static Accessor insCmpInvoiceVersion = new Accessor() {
      public Class getFieldClass() { return SoftwareVersion.class; }
      public Object get(Object o) { return Instance.getComputedInvoiceVersion((IInstance) o); }
   };

   public static Accessor insReportedDL = new Accessor() {
      public Class getFieldClass() { return Boolean.class; }
      public Object get(Object o) { return ((IInstance) o).getReportedDL(); }
   };

   public static Accessor insReportedUL = new Accessor() {
      public Class getFieldClass() { return Boolean.class; }
      public Object get(Object o) { return ((IInstance) o).getReportedUL(); }
   };

   public static Accessor insHasReportRow = new Accessor() {
      public Class getFieldClass() { return Boolean.class; }
      public Object get(Object o) { return ((IInstance) o).getHasReportRow(); }
   };

// --- LabCenter version ---

   public static Accessor lcvVersionID = new Accessor() {
      public Class getFieldClass() { return Integer.class; }
      public Object get(Object o) { return new Integer(((LabCenterVersion) o).versionID); }
   };

   public static Accessor lcvName = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return ((LabCenterVersion) o).versionName; }
   };

// --- software version ---

   public static Accessor swvName = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return ((SoftwareVersion) o).versionName; }
   };

// --- fulfillment type ---

   public static Accessor fftDisplayName = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return ((FulfillmentType) o).displayName; }
   };

   public static Accessor fftConfigFileName = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return ((FulfillmentType) o).configFileName; }
   };

// --- outer join paths ---

   // starting from an instance object, here are all the objects we can get to.
   // the five ins object accessors also count.

   public static Accessor insLocMerchant = new OuterAccessor(insLocation,locMerchant);
   public static Accessor insLocFulfillmentType = new OuterAccessor(insLocation,locFulfillmentType);
   public static Accessor insLocInvoiceVersion = new OuterAccessor(insLocation,locInvoiceVersion);

   public static Accessor insMerInvoiceVersion = new OuterAccessor(insLocMerchant,merInvoiceVersion);

   public static Accessor insWhsInvoiceVersion = new OuterAccessor(insWholesaler,whsInvoiceVersion);

// --- outer join fields ---

   // insLocMerchant
   public static Accessor insMerMerchantID = new OuterAccessor(insLocMerchant,merMerchantID);
   public static Accessor insMerName = new OuterAccessor(insLocMerchant,merName);

   // insLocation
   public static Accessor insLocLocationID = new OuterAccessor(insLocation,locLocationID);
   public static Accessor insLocName = new OuterAccessor(insLocation,locName);
   public static Accessor insLocCity = new OuterAccessor(insLocation,locCity);
   public static Accessor insLocState = new OuterAccessor(insLocation,locState);
   public static Accessor insLocPhone = new OuterAccessor(insLocation,locPhone);
   public static Accessor insLocMerchantLocationID = new OuterAccessor(insLocation,locMerchantLocationID);
   public static Accessor insLocFranchiseNumber = new OuterAccessor(insLocation,locFranchiseNumber);
   public static Accessor insLocInstallerLookup = new OuterAccessor(insLocation,locInstallerLookup);
   public static Accessor insLocIsDeleted = new OuterAccessor(insLocation,locIsDeleted);
   public static Accessor insLocIsPickup = new OuterAccessor(insLocation,locIsPickup);

   // insWholesaler
   public static Accessor insWhsWholesalerID = new OuterAccessor(insWholesaler,whsWholesalerID);
   public static Accessor insWhsName = new OuterAccessor(insWholesaler,whsName);

   // insLabCenterVersion
   public static Accessor insLcvName = new OuterAccessor(insLabCenterVersion,lcvName);

   // invoice versions
   public static Accessor insMerSwvName = new OuterAccessor(insMerInvoiceVersion,swvName);
   public static Accessor insLocSwvName = new OuterAccessor(insLocInvoiceVersion,swvName);
   public static Accessor insWhsSwvName = new OuterAccessor(insWhsInvoiceVersion,swvName);
   public static Accessor insSwvName = new OuterAccessor(insInvoiceVersion,swvName);
   public static Accessor insCmpSwvName = new OuterAccessor(insCmpInvoiceVersion,swvName);

   // insLocFulfillmentType
   public static Accessor insFftDisplayName = new OuterAccessor(insLocFulfillmentType,fftDisplayName);
   public static Accessor insFftConfigFileName = new OuterAccessor(insLocFulfillmentType,fftConfigFileName);

// --- columns ---

   private static GridColumn col(int n, Accessor accessor, Comparator comparator) {
      String suffix = Convert.fromInt(n);
      String name = Text.get(InstanceUtil.class,"n" + suffix);
      int width;
      try {
         width = Convert.toInt(Text.get(InstanceUtil.class,"w" + suffix));
      } catch (ValidationException e) {
         width = 1;
         // nothing we can do in a static context
      }
      return new GridColumn(name,width,accessor,comparator);
   }

   private static Comparator nc(Accessor accessor) {
      return new FieldComparator(accessor,new NaturalComparator());
   }

   private static Comparator sc(Accessor accessor) {
      return new FieldComparator(accessor,new NoCaseComparator());
   }

   private static Comparator bc(Accessor accessor) {
      return new FieldComparator(accessor,new BooleanComparator());
   }

   public static GridColumn colLineType = col(31,insLineType,sc(insLineType));
   public static GridColumn colLineID = col(32,insLineID,nc(insLineID));
   public static GridColumn colInstanceID = col(1,insInstanceID,nc(insInstanceID));
   public static GridColumn colPasscode = col(2,insPasscode,nc(insPasscode));
   public static GridColumn colRevisionNumber = col(3,insRevisionNumber,nc(insRevisionNumber));
   public static GridColumn colUseDefaultVersion = col(29,insUseDefaultVersion,bc(insUseDefaultVersion));
   public static GridColumn colDisplayVersion = col(33,insDisplayVersion,sc(insDisplayVersion));
   public static GridColumn colReportedLabCenterVersion = col(4,insReportedLabCenterVersion,sc(insReportedLabCenterVersion));
   public static GridColumn colReportedJavaVersion = col(5,insReportedJavaVersion,sc(insReportedJavaVersion));
   public static GridColumn colReportedIntegrations = col(30,insReportedIntegrations,sc(insReportedIntegrations));
   public static GridColumn colLastConfigRead = col(6,insLastConfigRead,sc(insLastConfigRead));
   public static GridColumn colLastConfigUpdate = col(7,insLastConfigUpdate,sc(insLastConfigUpdate));
   public static GridColumn colLastStatusReport = col(8,insLastStatusReport,sc(insLastStatusReport));
   public static GridColumn colReportedDL = col(37,insReportedDL,bc(insReportedDL));
   public static GridColumn colReportedUL = col(38,insReportedUL,bc(insReportedUL));
   public static GridColumn colHasReportRow = col(39,insHasReportRow,bc(insHasReportRow));

   public static GridColumn colMerMerchantID = col(9,insMerMerchantID,nc(insMerMerchantID));
   public static GridColumn colMerName = col(10,insMerName,sc(insMerName));

   public static GridColumn colLocLocationID = col(11,insLocLocationID,nc(insLocLocationID));
   public static GridColumn colLocName = col(12,insLocName,sc(insLocName));
   public static GridColumn colLocCity = col(34,insLocCity,sc(insLocCity));
   public static GridColumn colLocState = col(35,insLocState,sc(insLocState));
   public static GridColumn colLocPhone = col(36,insLocPhone,sc(insLocPhone));
   public static GridColumn colLocMerchantLocationID = col(13,insLocMerchantLocationID,nc(insLocMerchantLocationID));
   public static GridColumn colLocFranchiseNumber = col(14,insLocFranchiseNumber,sc(insLocFranchiseNumber));
   public static GridColumn colLocInstallerLookup = col(15,insLocInstallerLookup,sc(insLocInstallerLookup));
   public static GridColumn colLocIsDeleted = col(16,insLocIsDeleted,bc(insLocIsDeleted));
   public static GridColumn colLocIsPickup = col(17,insLocIsPickup,bc(insLocIsPickup));

   public static GridColumn colWhsWholesalerID = col(18,insWhsWholesalerID,nc(insWhsWholesalerID));
   public static GridColumn colWhsName = col(19,insWhsName,sc(insWhsName));

   public static GridColumn colLcvName = col(20,insLcvName,sc(insLcvName));

   public static GridColumn colMerSwvName = col(21,insMerSwvName,sc(insMerSwvName));
   public static GridColumn colLocSwvName = col(22,insLocSwvName,sc(insLocSwvName));
   public static GridColumn colWhsSwvName = col(23,insWhsSwvName,sc(insWhsSwvName));
   public static GridColumn colSwvName = col(24,insSwvName,sc(insSwvName));
   public static GridColumn colCmpSwvName = col(28,insCmpSwvName,sc(insCmpSwvName));

   public static GridColumn colFftDisplayName = col(25,insFftDisplayName,sc(insFftDisplayName));
   public static GridColumn colFftConfigFileName = col(26,insFftConfigFileName,sc(insFftConfigFileName));

}

