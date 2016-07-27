/*
 * SKUField.java
 */

package com.lifepics.neuron.struct;

import com.lifepics.neuron.core.ValidationException;

import java.io.File;

import org.w3c.dom.Node;

/**
 * A class that represents a non-null SKU-valued field of a structure.
 * The SKU may be a name represented as a string ({@link OldSKU})
 * or a product and attributes represented as an object ({@link NewSKU}).
 */

public class SKUField extends AtomicField {

   // this is very similar to StringField, actually

// --- fields ---

   private String oldName;

   // default values don't make sense for this type,
   // since SKU codes are different for each dealer.

// --- construction ---

   public SKUField(String javaName, String xmlName, String oldName) {
      super(javaName,xmlName);
      this.oldName = oldName;
   }

   // it'd make sense to fill in sinceVersion if you also had a custom
   // loadDefault or loadSpecial that got a SKU value from somewhere,
   // but I worry that it'd be easy to forget to implement one of them,
   // then a null would sneak through.
   // so, don't allow that constructor form until we actually need it.

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

   public Class getFieldClass() { return SKUField.class; }

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
      return tget(o1).equals(tget(o2));
      // OldSKU and NewSKU can both cross-compare
   }

   public void merge(Object oDest, Object oBase, Object oSrc) {
      SKU valBase = tget(oBase);
      SKU valSrc  = tget(oSrc);
      if ( ! valSrc.equals(valBase) ) tset(oDest,valSrc);
   }

   public void loadDefault(Object o) {
      tset(o,null); // same as a StringField with no default
      // this should actually never be called,
      // since no constructor sets sinceVersion, but be safe
   }

   protected void loadNormal(Node node, Object o) throws ValidationException {
      String s = getNullableText(node,xmlName); // null means "not there", just what we need
      if (s != null) {
         tset(o,NewSKU.decode(s));
      } else {
         s = getElementText(node,oldName);
         tset(o,OldSKU.decode(s));
      }
      // so, if neither name is found, we'll error out and complain about the old one.
      // that seems bad, but typically the old name is more informative, "SKU" vs. "A"
   }

   public void store(Node node, Object o) {
      SKU val = tget(o);
      if (val instanceof NewSKU) {
         createElementText(node,xmlName,NewSKU.encode((NewSKU) val));
      } else {
         createElementText(node,oldName,OldSKU.encode((OldSKU) val));
      }
   }

   // isDefault can remain false

}

