/*
 * NullableDateField.java
 */

package com.lifepics.neuron.struct;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Nullable;
import com.lifepics.neuron.core.ValidationException;

import java.io.File;
import java.util.Date;

import org.w3c.dom.Node;

/**
 * A class that represents a nullable Date-valued field of a structure
 * that stores null as an absent token.
 */

public class NullableDateField extends AtomicField {

// --- construction ---

   public NullableDateField(String javaName, String xmlName) {
      super(javaName,xmlName);
   }

   public NullableDateField(String javaName, String xmlName, int sinceVersion) {
      super(javaName,xmlName,sinceVersion);
      // null is the only allowed default value
   }

// --- accessors ---

   protected Date tget(Object o) {
      try {
         return (Date) javaField.get(o);
      } catch (IllegalAccessException e) {
         throw new Error(e);
      }
   }

   protected void tset(Object o, Date value) {
      try {
         javaField.set(o,value);
      } catch (IllegalAccessException e) {
         throw new Error(e);
      }
   }

   public Class getFieldClass() { return NullableDateField.class; }

   public Object get(Object o) {
      return tget(o);
   }

   public void put(Object o, Object value) {
      tset(o,(Date) value);
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
      Date valBase = tget(oBase);
      Date valSrc  = tget(oSrc);
      if ( ! Nullable.equals(valSrc,valBase) ) tset(oDest,valSrc);
   }

   public void loadDefault(Object o) {
      // no action needed
   }

   protected void loadNormal(Node node, Object o) throws ValidationException {
      tset(o,Convert.toNullableDateInternal(getNullableText(node,xmlName)));
   }

   public void store(Node node, Object o) {
      createNullableText(node,xmlName,Convert.fromNullableDateInternal(tget(o)));
   }

   protected boolean isDefault(Object o) {
      return Nullable.equals(tget(o),/* defaultValue = */ null);
   }

}

