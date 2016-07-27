/*
 * LevelField.java
 */

package com.lifepics.neuron.struct;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.ValidationException;

import java.io.File;
import java.util.logging.Level;

import org.w3c.dom.Node;

/**
 * A class that represents a non-null Level-valued field of a structure.
 */

public class LevelField extends AtomicField {

// --- fields ---

   private Level defaultValue;

// --- construction ---

   public LevelField(String javaName, String xmlName) {
      super(javaName,xmlName);
   }

   public LevelField(String javaName, String xmlName, int sinceVersion, Level defaultValue) {
      super(javaName,xmlName,sinceVersion);
      this.defaultValue = defaultValue;
   }

// --- accessors ---

   protected Level tget(Object o) {
      try {
         return (Level) javaField.get(o);
      } catch (IllegalAccessException e) {
         throw new Error(e);
      }
   }

   protected void tset(Object o, Level value) {
      try {
         javaField.set(o,value);
      } catch (IllegalAccessException e) {
         throw new Error(e);
      }
   }

   public Class getFieldClass() { return LevelField.class; }

   public Object get(Object o) {
      return tget(o);
   }

   public void put(Object o, Object value) {
      tset(o,(Level) value);
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
      Level valBase = tget(oBase);
      Level valSrc  = tget(oSrc);
      if ( ! valSrc.equals(valBase) ) tset(oDest,valSrc);
   }

   public void loadDefault(Object o) {
      tset(o,defaultValue);
   }

   protected void loadNormal(Node node, Object o) throws ValidationException {
      tset(o,Convert.toLevel(getElementText(node,xmlName)));
   }

   public void store(Node node, Object o) {
      createElementText(node,xmlName,Convert.fromLevel(tget(o)));
   }

   protected boolean isDefault(Object o) {
      return tget(o).equals(defaultValue);
   }

}

