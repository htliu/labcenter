/*
 * OuterEditAccessor.java
 */

package com.lifepics.neuron.meta;

/**
 * An accessor that accesses a field of an object-valued field (possibly null).
 * If you try to put a value into a field where the outer join fails, nothing
 * will happen.  What else could we do?
 */

public class OuterEditAccessor implements EditAccessor {

// --- fields ---

   protected Accessor accessor1;
   protected EditAccessor accessor2;

// --- construction ---

   public OuterEditAccessor(Accessor accessor1, EditAccessor accessor2) {
      this.accessor1 = accessor1;
      this.accessor2 = accessor2;
   }

// --- implementation of EditAccessor ---

   public Class getFieldClass() { return accessor2.getFieldClass(); }

   public Object get(Object o) {
      o = accessor1.get(o);
      return (o == null) ? null : accessor2.get(o);
   }

   public void put(Object o, Object value) {
      o = accessor1.get(o);
      if (o != null) accessor2.put(o,value);
   }

}

