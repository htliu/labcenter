/*
 * FieldAccessor.java
 */

package com.lifepics.neuron.meta;

/**
 * An accessor that accesses a field of an object-valued field.
 */

public class FieldAccessor implements Accessor {

// --- fields ---

   private Accessor accessor1;
   private Accessor accessor2; // cf. FieldComparator, this has the comparator role

// --- construction ---

   public FieldAccessor(Accessor accessor1, Accessor accessor2) {
      this.accessor1 = accessor1;
      this.accessor2 = accessor2;
   }

// --- implementation of Accessor ---

   public Class getFieldClass() { return accessor2.getFieldClass(); }
   public Object get(Object o)  { return accessor2.get(accessor1.get(o)); }

}

