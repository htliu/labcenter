/*
 * BooleanField.java
 */

package com.lifepics.neuron.struct;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.ValidationException;

import java.io.File;

import org.w3c.dom.Node;

/**
 * A class that represents a boolean-valued field of a structure.
 */

public class BooleanField extends AtomicField {

// --- fields ---

   private boolean defaultValue;

// --- construction ---

   public BooleanField(String javaName, String xmlName) {
      super(javaName,xmlName);
   }

   public BooleanField(String javaName, String xmlName, int sinceVersion, boolean defaultValue) {
      super(javaName,xmlName,sinceVersion);
      this.defaultValue = defaultValue;
   }

// --- accessors ---

   protected boolean tget(Object o) {
      try {
         return javaField.getBoolean(o);
      } catch (IllegalAccessException e) {
         throw new Error(e);
      }
   }

   protected void tset(Object o, boolean value) {
      try {
         javaField.setBoolean(o,value);
      } catch (IllegalAccessException e) {
         throw new Error(e);
      }
   }

   public Class getFieldClass() { return BooleanField.class; }

   public Object get(Object o) {
      return new Boolean(tget(o));
   }

   public void put(Object o, Object value) {
      tset(o,((Boolean) value).booleanValue());
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
      return (tget(o1) == tget(o2));
   }

   public void merge(Object oDest, Object oBase, Object oSrc) {
      boolean valBase = tget(oBase);
      boolean valSrc  = tget(oSrc);
      if (valSrc != valBase) tset(oDest,valSrc);
   }

   public void loadDefault(Object o) {
      tset(o,defaultValue);
   }

   protected void loadNormal(Node node, Object o) throws ValidationException {
      tset(o,Convert.toBool(getElementText(node,xmlName)));
   }

   public void store(Node node, Object o) {
      createElementText(node,xmlName,Convert.fromBool(tget(o)));
   }

   protected boolean isDefault(Object o) {
      return (tget(o) == defaultValue);
   }

}

