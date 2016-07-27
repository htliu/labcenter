/*
 * PriceList.java
 */

package com.lifepics.neuron.axon;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.meta.Accessor;
import com.lifepics.neuron.meta.FieldComparator;
import com.lifepics.neuron.meta.NoCaseComparator;
import com.lifepics.neuron.struct.*;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * A subobject that holds price list information.  Note that price lists
 * are objects on the server, with lists of products and prices,
 * but that as far as LabCenter is concerned, they're just IDs and names.
 */

public class PriceList extends Structure {

// --- fields ---

   // both non-null and non-empty
   public String id;   // the ID is really a number, but we don't care
   public String name;

   public String toString() { return name; } // convenience for combo boxes

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      PriceList.class,
      // no version
      new AbstractField[] {

         new StringField("id","ID"),
         new StringField("name","Name")
      });

   protected StructureDefinition sd() { return sd; }

// --- find function ---

   public static PriceList findByID(LinkedList priceLists, String id) {
      Iterator i = priceLists.iterator();
      while (i.hasNext()) {
         PriceList pl = (PriceList) i.next();
         if (pl.id.equals(id)) return pl;
      }
      return null;
   }

// --- sort functions ---

   public static Accessor idAccessor = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return ((PriceList) o).id; }
   };

   public static Accessor nameAccessor = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return ((PriceList) o).name; }
   };

   public static Comparator nameComparator = new FieldComparator(nameAccessor,new NoCaseComparator());

   public static void sort(LinkedList priceLists) {
      Collections.sort(priceLists,nameComparator);
   }

// --- validation ---

   public static void validate(LinkedList priceLists) throws ValidationException {

      Iterator i = priceLists.iterator();
      while (i.hasNext()) {
         ((PriceList) i.next()).validate();
      }

   // prevent duplicate IDs and names

      HashSet ids   = new HashSet();
      HashSet names = new HashSet();

      i = priceLists.iterator();
      while (i.hasNext()) {
         PriceList pl = (PriceList) i.next();

         if ( ! ids  .add(pl.id  ) ) throw new ValidationException(Text.get(PriceList.class,"e1",new Object[] { pl.id   }));
         if ( ! names.add(pl.name) ) throw new ValidationException(Text.get(PriceList.class,"e2",new Object[] { pl.name }));
      }
   }

   public void validate() throws ValidationException {
      if (id  .length() == 0) throw new ValidationException(Text.get(this,"e3"));
      if (name.length() == 0) throw new ValidationException(Text.get(this,"e4"));
      // error messages reflect the fact that this is not a user error,
      // it's something that can only happen if the server sends back a bad list
   }

}

