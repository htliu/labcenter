/*
 * StringField.java
 */

package com.lifepics.neuron.struct;

import com.lifepics.neuron.core.ValidationException;

import java.io.File;

import org.w3c.dom.Node;

/**
 * A class that represents a non-null String-valued field of a structure.
 */

public class StringField extends AtomicField {

// --- fields ---

   private String defaultValue;

// --- construction ---

   public StringField(String javaName, String xmlName) {
      super(javaName,xmlName);
   }

   public StringField(String javaName, String xmlName, int sinceVersion, String defaultValue) {
      super(javaName,xmlName,sinceVersion);
      this.defaultValue = defaultValue;
   }

// --- accessors ---

   protected String tget(Object o) {
      try {
         return (String) javaField.get(o);
      } catch (IllegalAccessException e) {
         throw new Error(e);
      }
   }

   protected void tset(Object o, String value) {
      try {
         javaField.set(o,value);
      } catch (IllegalAccessException e) {
         throw new Error(e);
      }
   }

   public Class getFieldClass() { return StringField.class; }

   public Object get(Object o) {
      return tget(o);
   }

   public void put(Object o, Object value) {
      tset(o,(String) value);
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
      return tget(o1).equals(tget(o2));
   }

   public void merge(Object oDest, Object oBase, Object oSrc) {
      String valBase = tget(oBase);
      String valSrc  = tget(oSrc);
      if ( ! valSrc.equals(valBase) ) tset(oDest,valSrc);
   }

   public void loadDefault(Object o) {
      tset(o,defaultValue);
   }

   protected void loadNormal(Node node, Object o) throws ValidationException {
      tset(o,getElementText(node,xmlName));
   }

   public void store(Node node, Object o) {
      createElementText(node,xmlName,tget(o));
   }

   protected boolean isDefault(Object o) {
      return tget(o).equals(defaultValue);
   }

}

