/*
 * OuterAccessor.java
 */

package com.lifepics.neuron.meta;

/**
 * An accessor that accesses a field of an object-valued field (possibly null).
 * I've been careful to avoid returning null from the get function in the past,
 * but actually it seems to be fine.
 */

public class OuterAccessor implements Accessor {

// --- fields ---

   private Accessor accessor1;
   private Accessor accessor2; // cf. FieldComparator, this has the comparator role

// --- construction ---

   public OuterAccessor(Accessor accessor1, Accessor accessor2) {
      this.accessor1 = accessor1;
      this.accessor2 = accessor2;
   }

// --- implementation of Accessor ---

   public Class getFieldClass() { return accessor2.getFieldClass(); }

   public Object get(Object o) {
      o = accessor1.get(o);
      return (o == null) ? null : accessor2.get(o);
   }

}

