/*
 * StoreList.java
 */

package com.lifepics.neuron.setup;

import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.*;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * An object that holds a list of store locations.
 */

public class StoreList extends Structure {

// --- fields ---

   public LinkedList stores;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      StoreList.class,
      0,0,
      new AbstractField[] {

         new StructureListField("stores","Store",Store.sd,Merge.NO_MERGE)
      });

   protected StructureDefinition sd() { return sd; }

// --- find functions ---

   public Store findStoreByNumber(String storeNumber) {
      Iterator i = stores.iterator();
      while (i.hasNext()) {
         Store store = (Store) i.next();
         if (store.storeNumber.equals(storeNumber)) return store;
      }
      return null;
   }

// --- validation ---

   public void validate() throws ValidationException {

      Iterator i = stores.iterator();
      while (i.hasNext()) {
         ((Store) i.next()).validate();
      }

      // could check for duplicate IDs and so forth, or even define a merge,
      // but we just don't need it.
   }

}

