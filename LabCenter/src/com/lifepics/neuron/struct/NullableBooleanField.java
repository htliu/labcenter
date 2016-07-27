/*
 * NullableBooleanField.java
 */

package com.lifepics.neuron.struct;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Nullable;
import com.lifepics.neuron.core.ValidationException;

import java.io.File;

import org.w3c.dom.Node;

/**
 * A class that represents a Boolean-valued field of a structure.
 */

public class NullableBooleanField extends AtomicField {

// --- fields ---

   private Boolean defaultValue;

// --- construction ---

   public NullableBooleanField(String javaName, String xmlName) {
      super(javaName,xmlName);
   }

   public NullableBooleanField(String javaName, String xmlName, int sinceVersion, Boolean defaultValue) {
      super(javaName,xmlName,sinceVersion);
      this.defaultValue = defaultValue;
   }

// --- accessors ---

   protected Boolean tget(Object o) {
      try {
         return (Boolean) javaField.get(o);
      } catch (IllegalAccessException e) {
         throw new Error(e);
      }
   }

   protected void tset(Object o, Boolean value) {
      try {
         javaField.set(o,value);
      } catch (IllegalAccessException e) {
         throw new Error(e);
      }
   }

   public Class getFieldClass() { return NullableBooleanField.class; }

   public Object get(Object o) {
      return tget(o);
   }

   public void put(Object o, Object value) {
      tset(o,(Boolean) value);
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
      Boolean valBase = tget(oBase);
      Boolean valSrc  = tget(oSrc);
      if ( ! Nullable.equals(valSrc,valBase) ) tset(oDest,valSrc);
   }

   public void loadDefault(Object o) {
      tset(o,defaultValue);
   }

   protected void loadNormal(Node node, Object o) throws ValidationException {
      tset(o,Convert.toNullableBool(getNullableText(node,xmlName)));
   }

   public void store(Node node, Object o) {
      createNullableText(node,xmlName,Convert.fromNullableBool(tget(o)));
   }

   protected boolean isDefault(Object o) {
      return Nullable.equals(tget(o),defaultValue);
   }

}

