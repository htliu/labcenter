/*
 * Snapshot.java
 */

package com.lifepics.neuron.admin;

import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.meta.CompoundComparator;
import com.lifepics.neuron.meta.FieldComparator;
import com.lifepics.neuron.meta.NaturalComparator;
import com.lifepics.neuron.struct.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * An object that holds a complete snapshot of a set of instances.
 */

public class Snapshot extends Structure {

// --- fields ---

   public LinkedList merchants;
   public LinkedList locations;
   public LinkedList wholesalers;
   public LinkedList instances;
   public int defaultLabCenterVersionID; // nullable on server, but it's not a real case, and it's a pain to handle
   public LinkedList labCenterVersions;
   public LinkedList softwareVersions;
   public LinkedList fulfillmentTypes;

   public HashMap locationIndex;

   // links
   public LabCenterVersion defaultLabCenterVersion;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      Snapshot.class,
      0,0,
      new AbstractField[] {

         new StructureListField("merchants","Merchant",Merchant.sd,Merge.NO_MERGE),
         new StructureListField("locations","Location",Location.sd,Merge.NO_MERGE),
         new StructureListField("wholesalers","Wholesaler",Wholesaler.sd,Merge.NO_MERGE),
         new StructureListField("instances","Instance",Instance.sd,Merge.NO_MERGE),
         new IntegerField("defaultLabCenterVersionID","DefaultLabCenterVersionID"),
         new StructureListField("labCenterVersions","LabCenterVersion",LabCenterVersion.sd,Merge.NO_MERGE),
         new StructureListField("softwareVersions","SoftwareVersion",SoftwareVersion.sd,Merge.NO_MERGE),
         new StructureListField("fulfillmentTypes","FulfillmentType",FulfillmentType.sd,Merge.NO_MERGE)
      });

   protected StructureDefinition sd() { return sd; }

// --- validation ---

   public void validate() throws ValidationException {
      Iterator i;

      i = merchants.iterator();
      while (i.hasNext()) ((Merchant) i.next()).validate();

      i = locations.iterator();
      while (i.hasNext()) ((Location) i.next()).validate();

      i = wholesalers.iterator();
      while (i.hasNext()) ((Wholesaler) i.next()).validate();

      i = instances.iterator();
      while (i.hasNext()) ((Instance) i.next()).validate();

      i = labCenterVersions.iterator();
      while (i.hasNext()) ((LabCenterVersion) i.next()).validate();

      i = softwareVersions.iterator();
      while (i.hasNext()) ((SoftwareVersion) i.next()).validate();

      i = fulfillmentTypes.iterator();
      while (i.hasNext()) ((FulfillmentType) i.next()).validate();
   }

// --- index ---

   // I want this to work well when there are, say, a thousand locations
   // and several instances each that link to them, so, hash map is good

   public void index() {
      Iterator i;

      locationIndex = new HashMap();
      i = locations.iterator();
      while (i.hasNext()) {
         Location o = (Location) i.next();
         locationIndex.put(new Integer(o.locationID),o);
      }
   }

// --- find 1 ---

   public Merchant findMerchant(int merchantID) {
      Iterator i = merchants.iterator();
      while (i.hasNext()) {
         Merchant o = (Merchant) i.next();
         if (o.merchantID == merchantID) return o;
      }
      return null;
   }

   public Location findLocation(int locationID) {
      return (Location) locationIndex.get(new Integer(locationID));
   }

   public Wholesaler findWholesaler(int wholesalerID) {
      Iterator i = wholesalers.iterator();
      while (i.hasNext()) {
         Wholesaler o = (Wholesaler) i.next();
         if (o.wholesalerID == wholesalerID) return o;
      }
      return null;
   }

   public LabCenterVersion findLabCenterVersion(int versionID) {
      Iterator i = labCenterVersions.iterator();
      while (i.hasNext()) {
         LabCenterVersion o = (LabCenterVersion) i.next();
         if (o.versionID == versionID) return o;
      }
      return null;
   }

   public SoftwareVersion findSoftwareVersion(int versionID) {
      Iterator i = softwareVersions.iterator();
      while (i.hasNext()) {
         SoftwareVersion o = (SoftwareVersion) i.next();
         if (o.versionID == versionID) return o;
      }
      return null;
   }

   public FulfillmentType findFulfillmentType(int fulfillmentTypeID) {
      Iterator i = fulfillmentTypes.iterator();
      while (i.hasNext()) {
         FulfillmentType o = (FulfillmentType) i.next();
         if (o.fulfillmentTypeID == fulfillmentTypeID) return o;
      }
      return null;
   }

// --- find 2 ---

   public Merchant findMerchant(Integer merchantID) {
      return (merchantID == null) ? null : findMerchant(merchantID.intValue());
   }

   public Location findLocation(Integer locationID) {
      return (locationID == null) ? null : findLocation(locationID.intValue());
   }

   public Wholesaler findWholesaler(Integer wholesalerID) {
      return (wholesalerID == null) ? null : findWholesaler(wholesalerID.intValue());
   }

   public LabCenterVersion findLabCenterVersion(Integer versionID) {
      return (versionID == null) ? null : findLabCenterVersion(versionID.intValue());
   }

   public SoftwareVersion findSoftwareVersion(Integer versionID) {
      return (versionID == null) ? null : findSoftwareVersion(versionID.intValue());
   }

   public FulfillmentType findFulfillmentType(Integer fulfillmentTypeID) {
      return (fulfillmentTypeID == null) ? null : findFulfillmentType(fulfillmentTypeID.intValue());
   }

// --- bind ---

   public void bind() {
      Iterator i;

      i = merchants.iterator();
      while (i.hasNext()) {
         Merchant o = (Merchant) i.next();
         o.invoiceVersion = findSoftwareVersion(o.invoiceVersionID);
      }

      i = locations.iterator();
      while (i.hasNext()) {
         Location o = (Location) i.next();
         o.merchant = findMerchant(o.merchantID);
         o.fulfillmentType = findFulfillmentType(o.fulfillmentTypeID);
         o.invoiceVersion = findSoftwareVersion(o.invoiceVersionID);
      }

      i = wholesalers.iterator();
      while (i.hasNext()) {
         Wholesaler o = (Wholesaler) i.next();
         o.invoiceVersion = findSoftwareVersion(o.invoiceVersionID);
      }

      i = instances.iterator();
      while (i.hasNext()) {
         Instance o = (Instance) i.next();
         o.location = findLocation(o.locationID);
         o.wholesaler = findWholesaler(o.wholesalerID);
         o.labCenterVersion = findLabCenterVersion(o.labCenterVersionID);
         o.invoiceVersion = findSoftwareVersion(o.invoiceVersionID);
      }

      defaultLabCenterVersion = findLabCenterVersion(defaultLabCenterVersionID);
   }

// --- getRows ---

   public ArrayList getRows() {
      Iterator i;

      LinkedList list = new LinkedList();

      HashSet merchantSet = new HashSet();
      HashSet locationSet = new HashSet();
      HashSet wholesalerSet = new HashSet();

      i = instances.iterator();
      while (i.hasNext()) {
         Instance o = (Instance) i.next();
         list.add(o);
         locationSet.add(o.locationID);
         wholesalerSet.add(o.wholesalerID);
      }

      i = locations.iterator();
      while (i.hasNext()) {
         Location o = (Location) i.next();
         if ( ! locationSet.contains(new Integer(o.locationID)) ) list.add(new NoInstance(o));
         merchantSet.add(new Integer(o.merchantID));
      }

      i = merchants.iterator();
      while (i.hasNext()) {
         Merchant o = (Merchant) i.next();
         if ( ! merchantSet.contains(new Integer(o.merchantID)) ) list.add(new NoInstance(new NoLocation(o)));
      }

      i = wholesalers.iterator();
      while (i.hasNext()) {
         Wholesaler o = (Wholesaler) i.next();
         if ( ! wholesalerSet.contains(new Integer(o.wholesalerID)) ) list.add(new NoInstance(o));
      }

      // the goal here is to have merchants and wholesalers at the top,
      // since they're rare and noteworthy, and then to list the rest
      // by location ID with instanceID as a subsort.  the fact that nc
      // sorts null as infinitely negative is very helpful.

      ArrayList rows = new ArrayList(list); // better for sorting

      CompoundComparator cc = new CompoundComparator();
      Comparator nc = new NaturalComparator();
      cc.add(new FieldComparator(InstanceUtil.insLocLocationID,nc));
      cc.add(new FieldComparator(InstanceUtil.insInstanceID,nc));
      cc.add(new FieldComparator(InstanceUtil.insWhsWholesalerID,nc));
      cc.add(new FieldComparator(InstanceUtil.insMerMerchantID,nc));
      Collections.sort(rows,cc);

      return rows;
   }

   public void sort() {

      // most of the tables come back in random order (.NET dictionary order),
      // and that's fine, but it's convenient to have the LabCenter versions
      // listed in order by version ID.

      Collections.sort(labCenterVersions,new FieldComparator(InstanceUtil.lcvVersionID,new NaturalComparator()));
   }

}

