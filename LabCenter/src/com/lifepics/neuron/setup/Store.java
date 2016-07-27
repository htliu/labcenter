/*
 * Store.java
 */

package com.lifepics.neuron.setup;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.*;

/**
 * An object that holds info about a store location.
 */

public class Store extends Structure {

// --- fields ---

   public int locationID;     // LifePics mlrfnbr
   public String storeNumber; // merchant store number, maybe with leading zeros
   public String description;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      Store.class,
      // no version
      new AbstractField[] {

         new IntegerField("locationID","LocationID"),
         new StringField("storeNumber","StoreNumber"),
         new StringField("description","Description")
      });

   static { sd.setAttributed(); }
   protected StructureDefinition sd() { return sd; }

// --- string form ---

   // we may have lots of these in a combo box, so we need the label retrieval to
   // be pretty fast.  since the store objects don't change, we can just cache it.

   public String label;

   public String toString() {
      if (label == null) {
         label = Text.get(this,"s1",new Object[] { storeNumber, description });
      }
      return label;
   }

// --- validation ---

   public void validate() throws ValidationException {
   }

}

