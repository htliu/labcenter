/*
 * PSKUField.java
 */

package com.lifepics.neuron.struct;

import com.lifepics.neuron.core.ValidationException;

import java.io.File;

import org.w3c.dom.Node;

/**
 * A class that represents a non-null PSKU-valued field of a structure.
 */

public class PSKUField extends AtomicField {

// --- fields ---

   private String oldName;
   private String patternName;

   // default values don't make sense for this type,
   // since SKU codes are different for each dealer.

// --- construction ---

   public PSKUField(String javaName, String xmlName, String oldName, String patternName) {
      super(javaName,xmlName);
      this.oldName = oldName;
      this.patternName = patternName;
   }

// --- accessors ---

   protected PSKU tget(Object o) {
      try {
         return (PSKU) javaField.get(o);
      } catch (IllegalAccessException e) {
         throw new Error(e);
      }
   }

   protected void tset(Object o, PSKU value) {
      try {
         javaField.set(o,value);
      } catch (IllegalAccessException e) {
         throw new Error(e);
      }
   }

   public Class getFieldClass() { return PSKUField.class; }

   public Object get(Object o) {
      return tget(o);
   }

   public void put(Object o, Object value) {
      tset(o,(PSKU) value);
   }

// --- subclass hooks ---

   public void init(Object o) {
      // no action needed
   }

   public void makeRelativeTo(Object o, File base) {
      // no action needed
   }

   public void copy(Object oDest, Object oSrc) {
      // no action needed -- all the PSKU subclasses are immutable,
      // so a shallow copy is sufficient
   }

   public boolean equals(Object o1, Object o2) {
      return tget(o1).equals(tget(o2));
      // all the PSKU subclasses can cross-compare
   }

   public void merge(Object oDest, Object oBase, Object oSrc) {
      PSKU valBase = tget(oBase);
      PSKU valSrc  = tget(oSrc);
      if ( ! valSrc.equals(valBase) ) tset(oDest,valSrc);
   }

   public void loadDefault(Object o) {
      tset(o,null); // never called, see SKUField for details
   }

   protected void loadNormal(Node node, Object o) throws ValidationException {
      String s = getNullableText(node,xmlName);
      if (s != null) {
         tset(o,NewSKU.decode(s));
      } else {
         s = getNullableText(node,patternName);
         if (s != null) {
            tset(o,Pattern.decode(s));
         } else {
            s = getElementText(node,oldName);
            tset(o,OldSKU.decode(s));
            // do OldSKU last because it produces best error text on failure
            // and also because it should soon be rare
         }
      }
   }

   public void store(Node node, Object o) {
      PSKU val = tget(o);
      if (val instanceof NewSKU) {
         createElementText(node,xmlName,NewSKU.encode((NewSKU) val));
      } else if (val instanceof Pattern) {
         createElementText(node,patternName,Pattern.encode((Pattern) val));
      } else {
         createElementText(node,oldName,OldSKU.encode((OldSKU) val));
      }
   }

   // isDefault can remain false

}

