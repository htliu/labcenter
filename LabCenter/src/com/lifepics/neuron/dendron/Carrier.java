/*
 * Carrier.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.meta.Accessor;
import com.lifepics.neuron.meta.EditAccessor;
import com.lifepics.neuron.meta.FieldComparator;
import com.lifepics.neuron.meta.NoCaseComparator;
import com.lifepics.neuron.struct.*;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

/**
 * An object that holds information about a shipping carrier.
 */

public class Carrier extends Structure {

// --- fields ---

   public String primaryName;
   public String displayName;
   public boolean show;

   public String toString() { return displayName; } // convenience for combo boxes

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      Carrier.class,
      // no version
      new AbstractField[] {

         new StringField("primaryName","PrimaryName"),
         new StringField("displayName","DisplayName"),
         new BooleanField("show","Show")
      });

   static { sd.setAttributed(); }
   protected StructureDefinition sd() { return sd; }

// --- columns and stuff ---

   public static Accessor primaryNameAccessor = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return ((Carrier) o).primaryName; }
   };

   public static Comparator primaryNameComparator = new FieldComparator(primaryNameAccessor,new NoCaseComparator());

   public static Accessor displayNameAccessor = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return ((Carrier) o).displayName; }
   };

   public static Comparator displayNameComparator = new FieldComparator(displayNameAccessor,new NoCaseComparator());

   public static Accessor showAccessor = new EditAccessor() {
      public Class getFieldClass() { return Boolean.class; }
      public Object get(Object o) { return new Boolean(((Carrier) o).show); }
      public void put(Object o, Object value) { ((Carrier) o).show = ((Boolean) value).booleanValue(); }
   };

   public static void sort(LinkedList carriers) {
      Collections.sort(carriers,displayNameComparator);
   }

// --- validation ---

   public void validate() throws ValidationException {

      if (primaryName.length() == 0) throw new ValidationException(Text.get(this,"e1"));
      if (displayName.length() == 0) throw new ValidationException(Text.get(this,"e2"));
      // error messages reflect the fact that this is not a user error,
      // it's something that can only happen if the server sends back a bad list
   }

}

