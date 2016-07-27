/*
 * NullableDoubleField.java
 */

package com.lifepics.neuron.struct;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Nullable;
import com.lifepics.neuron.core.ValidationException;

import java.io.File;

import org.w3c.dom.Node;

/**
 * A class that represents a Double-valued field of a structure.
 */

public class NullableDoubleField extends AtomicField {

// --- fields ---

   private Double defaultValue;

// --- construction ---

   public NullableDoubleField(String javaName, String xmlName) {
      super(javaName,xmlName);
   }

   public NullableDoubleField(String javaName, String xmlName, int sinceVersion, Double defaultValue) {
      super(javaName,xmlName,sinceVersion);
      this.defaultValue = defaultValue;
   }

// --- accessors ---

   protected Double tget(Object o) {
      try {
         return (Double) javaField.get(o);
      } catch (IllegalAccessException e) {
         throw new Error(e);
      }
   }

   protected void tset(Object o, Double value) {
      try {
         javaField.set(o,value);
      } catch (IllegalAccessException e) {
         throw new Error(e);
      }
   }

   public Class getFieldClass() { return NullableDoubleField.class; }

   public Object get(Object o) {
      return tget(o);
   }

   public void put(Object o, Object value) {
      tset(o,(Double) value);
   }

// --- subclass hooks ---

   public void init(Object o) {
      // no action needed
   }

   public void makeRelativeTo(Object o, File base) {
      // no action needed
   }

   public void copy(Object oDest, Object oSrc) {
      // no action needed
   }

   public boolean equals(Object o1, Object o2) {
      return Nullable.equals(tget(o1),tget(o2));
   }

   public void merge(Object oDest, Object oBase, Object oSrc) {
      Double valBase = tget(oBase);
      Double valSrc  = tget(oSrc);
      if ( ! Nullable.equals(valSrc,valBase) ) tset(oDest,valSrc);
   }

   public void loadDefault(Object o) {
      tset(o,defaultValue);
   }

   protected void loadNormal(Node node, Object o) throws ValidationException {
      tset(o,Convert.toNullableDouble(getNullableText(node,xmlName)));
   }

   public void store(Node node, Object o) {
      createNullableText(node,xmlName,Convert.fromNullableDouble(tget(o)));
   }

   protected boolean isDefault(Object o) {
      return Nullable.equals(tget(o),defaultValue);
   }

}

