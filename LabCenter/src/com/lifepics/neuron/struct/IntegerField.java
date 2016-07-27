/*
 * IntegerField.java
 */

package com.lifepics.neuron.struct;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.ValidationException;

import java.io.File;

import org.w3c.dom.Node;

/**
 * A class that represents an int-valued field of a structure.
 */

public class IntegerField extends AtomicField {

// --- fields ---

   private int defaultValue;

// --- construction ---

   public IntegerField(String javaName, String xmlName) {
      super(javaName,xmlName);
   }

   public IntegerField(String javaName, String xmlName, int sinceVersion, int defaultValue) {
      super(javaName,xmlName,sinceVersion);
      this.defaultValue = defaultValue;
   }

// --- accessors ---

   protected int tget(Object o) {
      try {
         return javaField.getInt(o);
      } catch (IllegalAccessException e) {
         throw new Error(e);
      }
   }

   protected void tset(Object o, int value) {
      try {
         javaField.setInt(o,value);
      } catch (IllegalAccessException e) {
         throw new Error(e);
      }
   }

   public Class getFieldClass() { return IntegerField.class; }

   public Object get(Object o) {
      return new Integer(tget(o));
   }

   public void put(Object o, Object value) {
      tset(o,((Integer) value).intValue());
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
      int valBase = tget(oBase);
      int valSrc  = tget(oSrc);
      if (valSrc != valBase) tset(oDest,valSrc);
   }

   public void loadDefault(Object o) {
      tset(o,defaultValue);
   }

   protected void loadNormal(Node node, Object o) throws ValidationException {
      tset(o,Convert.toInt(getElementText(node,xmlName)));
   }

   public void store(Node node, Object o) {
      createElementText(node,xmlName,Convert.fromInt(tget(o)));
   }

   protected boolean isDefault(Object o) {
      return (tget(o) == defaultValue);
   }

}

