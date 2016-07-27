/*
 * DoubleField.java
 */

package com.lifepics.neuron.struct;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.ValidationException;

import java.io.File;

import org.w3c.dom.Node;

/**
 * A class that represents a double-valued field of a structure.
 */

public class DoubleField extends AtomicField {

// --- fields ---

   private double defaultValue;

// --- construction ---

   public DoubleField(String javaName, String xmlName) {
      super(javaName,xmlName);
   }

   public DoubleField(String javaName, String xmlName, int sinceVersion, double defaultValue) {
      super(javaName,xmlName,sinceVersion);
      this.defaultValue = defaultValue;
   }

// --- accessors ---

   protected double tget(Object o) {
      try {
         return javaField.getDouble(o);
      } catch (IllegalAccessException e) {
         throw new Error(e);
      }
   }

   protected void tset(Object o, double value) {
      try {
         javaField.setDouble(o,value);
      } catch (IllegalAccessException e) {
         throw new Error(e);
      }
   }

   public Class getFieldClass() { return DoubleField.class; }

   public Object get(Object o) {
      return new Double(tget(o));
   }

   public void put(Object o, Object value) {
      tset(o,((Double) value).doubleValue());
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
      double valBase = tget(oBase);
      double valSrc  = tget(oSrc);
      if (valSrc != valBase) tset(oDest,valSrc);
   }

   public void loadDefault(Object o) {
      tset(o,defaultValue);
   }

   protected void loadNormal(Node node, Object o) throws ValidationException {
      tset(o,Convert.toDouble(getElementText(node,xmlName)));
   }

   public void store(Node node, Object o) {
      createElementText(node,xmlName,Convert.fromDouble(tget(o)));
   }

   protected boolean isDefault(Object o) {
      return (tget(o) == defaultValue);
   }

}

