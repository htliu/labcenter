/*
 * NullableSKUField.java
 */

package com.lifepics.neuron.struct;

import com.lifepics.neuron.core.Nullable;
import com.lifepics.neuron.core.ValidationException;

import java.io.File;

import org.w3c.dom.Node;

/**
 * A class that represents a nullable SKU-valued field of a structure.
 * The SKU may be a name represented as a string ({@link OldSKU})
 * or a product and attributes represented as an object ({@link NewSKU}).
 */

public class NullableSKUField extends AtomicField {

   // this is very similar to NullableStringField, actually

// --- fields ---

   protected String oldName;

   // default values don't make sense for this type,
   // since SKU codes are different for each dealer.

// --- construction ---

   public NullableSKUField(String javaName, String xmlName, String oldName) {
      super(javaName,xmlName);
      this.oldName = oldName;
   }

   public NullableSKUField(String javaName, String xmlName, String oldName, int sinceVersion) {
      super(javaName,xmlName,sinceVersion);
      this.oldName = oldName;
      // no defaultValue field, the only allowed default is null
   }
   // in SKUField this form wasn't allowed, but here it makes sense,
   // because null is a reasonable default for a nullable SKU field.
   // if you want some other default (computed from somewhere),
   // you should write a custom loadDefault or loadSpecial function

   // of course, if you have a nullable field that defaults to null,
   // there's no need for sinceVersion, you can just pretend that
   // it's always been there.  but, sometimes I do like to show the
   // version number, just to be clear.

// --- accessors ---

   protected SKU tget(Object o) {
      try {
         return (SKU) javaField.get(o);
      } catch (IllegalAccessException e) {
         throw new Error(e);
      }
   }

   protected void tset(Object o, SKU value) {
      try {
         javaField.set(o,value);
      } catch (IllegalAccessException e) {
         throw new Error(e);
      }
   }

   public Class getFieldClass() { return NullableSKUField.class; }

   public Object get(Object o) {
      return tget(o);
   }

   public void put(Object o, Object value) {
      tset(o,(SKU) value);
   }

// --- subclass hooks ---

   public void init(Object o) {
      // no action needed
   }

   public void makeRelativeTo(Object o, File base) {
      // no action needed
   }

   public void copy(Object oDest, Object oSrc) {
      // no action needed -- both OldSKU and NewSKU are immutable,
      // so a shallow copy is sufficient
   }

   public boolean equals(Object o1, Object o2) {
      return Nullable.equalsObject(tget(o1),tget(o2));
      // OldSKU and NewSKU can both cross-compare
   }

   public void merge(Object oDest, Object oBase, Object oSrc) {
      SKU valBase = tget(oBase);
      SKU valSrc  = tget(oSrc);
      if ( ! Nullable.equalsObject(valSrc,valBase) ) tset(oDest,valSrc);
   }

   public void loadDefault(Object o) {
      tset(o,null); // no defaultValue field, the only allowed default is null
   }

   protected void loadNormal(Node node, Object o) throws ValidationException {
      String s = getNullableText(node,xmlName); // null means "not there", just what we need
      if (s != null) {
         tset(o,NewSKU.decode(s));
      } else {
         s = getNullableText(node,oldName);
         tset(o,(s != null) ? OldSKU.decode(s) : null);
      }
   }

   public void store(Node node, Object o) {
      SKU val = tget(o);
      if (val instanceof NewSKU) {
         createElementText(node,xmlName,NewSKU.encode((NewSKU) val));
      } else {
         createNullableText(node,oldName,(val != null) ? OldSKU.encode((OldSKU) val) : null);
      }
   }

   protected boolean isDefault(Object o) {
      return Nullable.equalsObject(tget(o),/* defaultValue = */ null);
   }

}

