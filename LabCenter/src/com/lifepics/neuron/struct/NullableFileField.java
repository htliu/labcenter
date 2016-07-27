/*
 * NullableFileField.java
 */

package com.lifepics.neuron.struct;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Nullable;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.object.Relative;

import java.io.File;

import org.w3c.dom.Node;

/**
 * A class that represents a nullable File-valued field of a structure.
 */

public class NullableFileField extends AtomicField {

// --- fields ---

   private File defaultValue;

// --- construction ---

   public NullableFileField(String javaName, String xmlName) {
      super(javaName,xmlName);
   }

   public NullableFileField(String javaName, String xmlName, int sinceVersion, File defaultValue) {
      super(javaName,xmlName,sinceVersion);
      this.defaultValue = defaultValue;
   }

// --- accessors ---

   protected File tget(Object o) {
      try {
         return (File) javaField.get(o);
      } catch (IllegalAccessException e) {
         throw new Error(e);
      }
   }

   protected void tset(Object o, File value) {
      try {
         javaField.set(o,value);
      } catch (IllegalAccessException e) {
         throw new Error(e);
      }
   }

   public Class getFieldClass() { return NullableFileField.class; }

   public Object get(Object o) {
      return tget(o);
   }

   public void put(Object o, Object value) {
      tset(o,(File) value);
   }

// --- subclass hooks ---

   public void init(Object o) {
      // no action needed
   }

   public void makeRelativeTo(Object o, File base) {
      File f = tget(o);
      if (f != null) tset(o,Relative.makeRelativeTo(base,f));
   }

   public void copy(Object oDest, Object oSrc) {
      // no action needed
   }

   public boolean equals(Object o1, Object o2) {
      return Nullable.equals(tget(o1),tget(o2));
   }

   public void merge(Object oDest, Object oBase, Object oSrc) {
      File valBase = tget(oBase);
      File valSrc  = tget(oSrc);
      if ( ! Nullable.equals(valSrc,valBase) ) tset(oDest,valSrc);
   }

   public void loadDefault(Object o) {
      tset(o,defaultValue);
   }

   protected void loadNormal(Node node, Object o) throws ValidationException {
      tset(o,Convert.toNullableFile(getNullableText(node,xmlName)));
   }

   public void store(Node node, Object o) {
      createNullableText(node,xmlName,Convert.fromNullableFile(tget(o)));
   }

   protected boolean isDefault(Object o) {
      return Nullable.equals(tget(o),defaultValue);
   }

}

