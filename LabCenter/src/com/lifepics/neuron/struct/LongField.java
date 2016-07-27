/*
 * LongField.java
 */

package com.lifepics.neuron.struct;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.ValidationException;

import java.io.File;

import org.w3c.dom.Node;

/**
 * A class that represents a long-valued field of a structure.
 */

public class LongField extends AtomicField {

// --- fields ---

   private long defaultValue;

// --- construction ---

   public LongField(String javaName, String xmlName) {
      super(javaName,xmlName);
   }

   public LongField(String javaName, String xmlName, int sinceVersion, long defaultValue) {
      super(javaName,xmlName,sinceVersion);
      this.defaultValue = defaultValue;
   }

// --- accessors ---

   protected long tget(Object o) {
      try {
         return javaField.getLong(o);
      } catch (IllegalAccessException e) {
         throw new Error(e);
      }
   }

   protected void tset(Object o, long value) {
      try {
         javaField.setLong(o,value);
      } catch (IllegalAccessException e) {
         throw new Error(e);
      }
   }

   public Class getFieldClass() { return LongField.class; }

   public Object get(Object o) {
      return new Long(tget(o));
   }

   public void put(Object o, Object value) {
      tset(o,((Long) value).longValue());
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
      long valBase = tget(oBase);
      long valSrc  = tget(oSrc);
      if (valSrc != valBase) tset(oDest,valSrc);
   }

   public void loadDefault(Object o) {
      tset(o,defaultValue);
   }

   protected void loadNormal(Node node, Object o) throws ValidationException {
      tset(o,Convert.toLong(getElementText(node,xmlName)));
   }

   public void store(Node node, Object o) {
      createElementText(node,xmlName,Convert.fromLong(tget(o)));
   }

   protected boolean isDefault(Object o) {
      return (tget(o) == defaultValue);
   }

}

