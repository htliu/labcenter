/*
 * CarrierList.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.*;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * An object that holds information about a list of shipping carriers.
 */

public class CarrierList extends Structure {

// --- fields ---

   public boolean enableTracking;
   public LinkedList carriers; // kept in order by display name
   public String defaultCarrier; // primary name, nullable

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      CarrierList.class,
      0,0,
      new AbstractField[] {

         new BooleanField("enableTracking","EnableTracking",0,false),
         new StructureListField("carriers","Carrier",Carrier.sd,Merge.IDENTITY,0,0).with(Carrier.primaryNameAccessor,Carrier.displayNameComparator),
         new NullableStringField("defaultCarrier","DefaultCarrier",0,null)
      });

   protected StructureDefinition sd() { return sd; }

// --- helper functions ---

   public Carrier findCarrierByPrimaryName(String primaryName) {
      Iterator i = carriers.iterator();
      while (i.hasNext()) {
         Carrier c = (Carrier) i.next();
         if (c.primaryName.equals(primaryName)) return c;
      }
      return null;
   }

   /**
    * Tiny optimization of getCarrierSubset for ConfigDialog.
    */
   public boolean hasCarrierSubset() {
      Iterator i = carriers.iterator();
      while (i.hasNext()) {
         Carrier c = (Carrier) i.next();
         if (c.show) return true;
      }
      return false;
   }

   /**
    * Get the subset of shown carriers, which is guaranteed not empty
    * if enableTracking is set and the CarrierList has been validated.
    * It's also guaranteed to contain the default carrier if there is one.
    */
   public LinkedList getCarrierSubset() {
      LinkedList subset = new LinkedList();
      Iterator i = carriers.iterator();
      while (i.hasNext()) {
         Carrier c = (Carrier) i.next();
         if (c.show) subset.add(c);
      }
      return subset;
   }

   public int mergeFrom(CarrierList listIn) {

      enableTracking = listIn.enableTracking;

      int countNew = 0;

      Iterator i = carriers.iterator();
      while (i.hasNext()) {
         Carrier cOut = (Carrier) i.next();

         // search old list for new
         Carrier cIn = listIn.findCarrierByPrimaryName(cOut.primaryName);
         if (cIn == null) countNew++;
         cOut.show = (cIn != null) ? cIn.show : false; // false since weird new carrier is the most likely case
      }

      // search new list for old
      boolean found = (listIn.defaultCarrier != null && findCarrierByPrimaryName(listIn.defaultCarrier) != null);
      defaultCarrier = found ? listIn.defaultCarrier : null;

      return countNew;
   }

// --- validation ---

   public void validate() throws ValidationException {

   // both names must be unique

      HashSet primary = new HashSet();
      HashSet display = new HashSet();

      Iterator i = carriers.iterator();
      while (i.hasNext()) {
         Carrier c = (Carrier) i.next();

         c.validate();

         if ( ! primary.add(c.primaryName) ) throw new ValidationException(Text.get(this,"e1",new Object[] { c.primaryName }));
         if ( ! display.add(c.displayName) ) throw new ValidationException(Text.get(this,"e2",new Object[] { c.displayName }));
      }

   // if set, default carrier must be in list and be shown

      if (defaultCarrier != null) {

         Carrier c = findCarrierByPrimaryName(defaultCarrier);
         if (c == null)  throw new ValidationException(Text.get(this,"e3",new Object[] { defaultCarrier }));
         if ( ! c.show ) throw new ValidationException(Text.get(this,"e4",new Object[] { defaultCarrier }));
      }
   }

}

