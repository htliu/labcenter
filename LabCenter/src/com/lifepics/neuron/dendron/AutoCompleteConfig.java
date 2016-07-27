/*
 * AutoCompleteConfig.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.misc.StoreHours;
import com.lifepics.neuron.object.CopyUtil;
import com.lifepics.neuron.struct.*;

import java.util.LinkedList;

/**
 * An object that holds configuration information for the autocomplete subsystem.
 */

public class AutoCompleteConfig extends Structure {

// --- fields ---

   public long autoCompleteDelay; // millis
   public boolean restrictToStoreHours;
   public long deltaOpen;  // millis, but positive or negative
   public long deltaClose; //

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      AutoCompleteConfig.class,
      0,0,
      new AbstractField[] {

         new LongField("autoCompleteDelay","AutoCompleteDelay-Millis",0,600000), // 10 minutes
         new BooleanField("restrictToStoreHours","RestrictToStoreHours",0,false),
         new LongField("deltaOpen","DeltaOpen-Millis",0,0),
         new LongField("deltaClose","DeltaClose-Millis",0,0)
      });

   protected StructureDefinition sd() { return sd; }

// --- copy function ---

   public AutoCompleteConfig copy() { return (AutoCompleteConfig) CopyUtil.copy(this); }

// --- validation ---

   public void validate() throws ValidationException {

      if (autoCompleteDelay < 0) throw new ValidationException(Text.get(this,"e1"));
   }

   /**
    * A cross-validation between this auto-complete config object
    * and the store hours (which are stored in a different object).
    */
   public void validateHours(LinkedList storeHours) throws ValidationException {
      if (restrictToStoreHours) {
         // the second test completely covers the first, but the extra area it covers is small,
         // and the error message is much more confusing
         if (StoreHours.isAlwaysClosed(       storeHours )) throw new ValidationException(Text.get(this,"e2"));
         if (StoreHours.isAlwaysClosed(adjust(storeHours))) throw new ValidationException(Text.get(this,"e3"));
      }
      // if not restricted, hours don't matter
   }

   public LinkedList adjust(LinkedList storeHours) {
      LinkedList result = CopyUtil.copyList(storeHours);
      StoreHours.adjustMillis(result,deltaOpen,deltaClose);
      return result;
   }

}

