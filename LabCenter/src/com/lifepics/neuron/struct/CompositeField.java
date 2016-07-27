/*
 * CompositeField.java
 */

package com.lifepics.neuron.struct;

/**
 * An intermediate abstraction that represents a non-atomic field.
 */

public abstract class CompositeField extends AbstractField {

// --- construction ---

   public CompositeField(String javaName, String xmlName) {
      super(javaName,xmlName);
   }

   public CompositeField(String javaName, String xmlName, int sinceVersion) {
      super(javaName,xmlName,sinceVersion);
   }

// --- implementation of FieldNode ---

   public Class getFieldClass() { return FieldNode.class; }

   public Object get(Object o) {
      try {
         return javaField.get(o);
      } catch (IllegalAccessException e) {
         throw new Error(e);
      }
   }

}

