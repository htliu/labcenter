/*
 * InlineListField.java
 */

package com.lifepics.neuron.struct;

import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.object.XML;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;

import org.w3c.dom.Node;

/**
 * A class that represents a LinkedList<String>-valued field of a structure
 * that stores itself in XML as an inline list.
 * Actually the elements can be any immutable type, like Integer -- you just
 * have to override the load and store functions.
 */

public class InlineListField extends CompositeField {

// --- construction ---

   public InlineListField(String javaName, String xmlName) {
      super(javaName,xmlName);
   }

   public InlineListField(String javaName, String xmlName, int sinceVersion) {
      super(javaName,xmlName,sinceVersion);
      // the empty list is the only allowed default value
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
      tset(o,new LinkedList());
   }

   public void makeRelativeTo(Object o, File base) {
      // no action needed
   }

   public void copy(Object oDest, Object oSrc) {
      tset(oDest,new LinkedList(tget(oSrc)));
      // strings are immutable, no need for deeper copy
   }

   public boolean equals(Object o1, Object o2) {
      return tget(o1).equals(tget(o2));
   }

   public void merge(Object oDest, Object oBase, Object oSrc) {
      tset(oDest,Merge.mergeByEquality(tget(oDest),tget(oBase),tget(oSrc),/* copy = */ false));
   }

   public void loadDefault(Object o) {
      // no action needed
   }

   protected void loadNormal(Node node, Object o) throws ValidationException {
      tset(o,XML.getInlineList(node,xmlName));
   }

   public void store(Node node, Object o) {
      XML.createInlineList(node,xmlName,tget(o));
   }

   protected boolean isDefault(Object o) {
      return (tget(o).size() == 0); // default is empty list
   }

}

