/*
 * Dealer.java
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
 * A subobject that holds dealer information for wholesaler uploads.
 */

public class Dealer extends Structure {

// --- fields ---

   public String id; // really a number, but we don't care
   public String name;
   public String password;

   public String toString() { return name; } // convenience for combo boxes

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      Dealer.class,
      // no version
      new AbstractField[] {

         new StringField("id","ID"),
         new StringField("name","Name"),
         new PasswordField("password","Password",/* classifier = */ 1)
      });

   protected StructureDefinition sd() { return sd; }

// --- find function ---

   public static Dealer findByID(LinkedList dealers, String id) {
      Iterator i = dealers.iterator();
      while (i.hasNext()) {
         Dealer d = (Dealer) i.next();
         if (d.id.equals(id)) return d;
      }
      return null;
   }

// --- sort functions ---

   public static Accessor idAccessor = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return ((Dealer) o).id; }
   };

   public static Accessor nameAccessor = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return ((Dealer) o).name; }
   };

   public static Comparator nameComparator = new FieldComparator(nameAccessor,new NoCaseComparator());

   public static void sort(LinkedList dealers) {
      Collections.sort(dealers,nameComparator);
   }

// --- validation ---

   public static void validate(LinkedList dealers) throws ValidationException {

      Iterator i = dealers.iterator();
      while (i.hasNext()) {
         ((Dealer) i.next()).validate();
      }

   // prevent duplicate IDs and names

      HashSet ids   = new HashSet();
      HashSet names = new HashSet();

      i = dealers.iterator();
      while (i.hasNext()) {
         Dealer d = (Dealer) i.next();

         if ( ! ids  .add(d.id  ) ) throw new ValidationException(Text.get(Dealer.class,"e1",new Object[] { d.id   }));
         if ( ! names.add(d.name) ) throw new ValidationException(Text.get(Dealer.class,"e2",new Object[] { d.name }));
      }
   }

   public void validate() throws ValidationException {
      if (id  .length() == 0) throw new ValidationException(Text.get(this,"e3"));
      if (name.length() == 0) throw new ValidationException(Text.get(this,"e4"));
      // error messages reflect the fact that this is not a user error,
      // it's something that can only happen if the server sends back a bad list
   }

}

