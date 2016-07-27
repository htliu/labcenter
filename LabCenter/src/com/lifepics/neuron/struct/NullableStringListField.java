/*
 * NullableStringListField.java
 */

package com.lifepics.neuron.struct;

import com.lifepics.neuron.core.Nullable;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.object.XML;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;

import org.w3c.dom.Node;

/**
 * A class that represents a LinkedList<String>-valued field of a structure
 * that stores itself in XML as a nested list,
 * but also allows the entire list to be absent, producing a null LinkedList.
 */

public class NullableStringListField extends CompositeField {

// --- fields ---

   private String xmlNestedName;

// --- construction ---

   public NullableStringListField(String javaName, String xmlName, String xmlNestedName) {
      super(javaName,xmlName);
      this.xmlNestedName = xmlNestedName;
   }

   public NullableStringListField(String javaName, String xmlName, String xmlNestedName, int sinceVersion) {
      super(javaName,xmlName,sinceVersion);
      this.xmlNestedName = xmlNestedName;
      // null is the only allowed default value
   }

// --- accessors ---

   protected LinkedList tget(Object o) {
      try {
         return (LinkedList) javaField.get(o);
      } catch (IllegalAccessException e) {
         throw new Error(e);
      }
   }

   protected void tset(Object o, LinkedList value) {
      try {
         javaField.set(o,value);
      } catch (IllegalAccessException e) {
         throw new Error(e);
      }
   }

// --- implementation of FieldNode ---

   public boolean isDynamic() { return true; }

   public Collection getChildren(Object o) {
      return null;
   }

// --- subclass hooks ---

   public void init(Object o) {
      // no action needed
   }

   public void makeRelativeTo(Object o, File base) {
      // no action needed
   }

   public void copy(Object oDest, Object oSrc) {
      LinkedList list = tget(oSrc);
      tset(oDest,(list == null) ? null : new LinkedList(list));
   }

   public boolean equals(Object o1, Object o2) {
      return Nullable.equalsObject(tget(o1),tget(o2));
   }

   public void merge(Object oDest, Object oBase, Object oSrc) {
      LinkedList valDest = tget(oDest);
      LinkedList valBase = tget(oBase);
      LinkedList valSrc  = tget(oSrc);
      if (valDest != null && valBase != null && valSrc != null) {
         tset(oDest,Merge.mergeByEquality(valDest,valBase,valSrc,/* copy = */ false));
      } else {
         if ( ! Nullable.equalsObject(valSrc,valBase) ) copy(oDest,oSrc);
      }
      // this is the null-pattern form of InlineListField.merge,
      // see NullableStructureField for details.
      // it might seem that a null list should be like an empty list,
      // but if so, why are you using this class?  null is different.
   }

   public void loadDefault(Object o) {
      // no action needed
   }

   protected void loadNormal(Node node, Object o) throws ValidationException {
      Node child = XML.getElementTry(node,xmlName);
      if (child != null) tset(o,XML.getInlineList(child,xmlNestedName));
   }

   public void store(Node node, Object o) {
      LinkedList list = tget(o);
      if (list != null) XML.createStringList(node,xmlName,xmlNestedName,list);
   }

   protected boolean isDefault(Object o) {
      return (tget(o) == null); // default is null
   }

}

