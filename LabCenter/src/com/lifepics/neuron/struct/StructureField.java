/*
 * StructureField.java
 */

package com.lifepics.neuron.struct;

import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.object.Copyable;
import com.lifepics.neuron.object.CopyUtil;
import com.lifepics.neuron.object.XML;

import java.io.File;
import java.util.Collection;

import org.w3c.dom.Node;

/**
 * A class that represents a non-null Structure-valued field of a structure.
 */

public class StructureField extends CompositeField {

// --- fields ---

   protected StructureDefinition sd;

// --- construction ---

   public StructureField(String javaName, String xmlName, StructureDefinition sd) {
      super(javaName,xmlName);
      this.sd = sd;
   }

   public StructureField(String javaName, String xmlName, StructureDefinition sd, int sinceVersion) {
      super(javaName,xmlName,sinceVersion);
      this.sd = sd;
   }

// --- accessors ---

   protected Object tget(Object o) {
      try {
         return javaField.get(o);
      } catch (IllegalAccessException e) {
         throw new Error(e);
      }
   }

   protected void tset(Object o, Object value) {
      try {
         javaField.set(o,value);
      } catch (IllegalAccessException e) {
         throw new Error(e);
      }
   }

// --- implementation of FieldNode ---

   public boolean isDynamic() { return false; }
   public Collection getChildren(Object o) { return sd.getChildren(); }

// --- subclass hooks ---

   public void init(Object o) {
      // no action needed -- init doesn't operate recursively,
      // it's called as each individual object is constructed.
   }

   public void makeRelativeTo(Object o, File base) {
      sd.makeRelativeTo(tget(o),base);
   }

   public void copy(Object oDest, Object oSrc) {
      tset(oDest,CopyUtil.copy((Copyable) tget(oSrc)));
      // this is the only requirement on the structure, that it has to be copyable.
      // in fact, the structure will be a Structure, but that's just a convenience.
   }

   public boolean equals(Object o1, Object o2) {
      return sd.equals(tget(o1),tget(o2));
   }

   public void merge(Object oDest, Object oBase, Object oSrc) {
      sd.merge(tget(oDest),tget(oBase),tget(oSrc));
   }

   public void loadDefault(Object o) {
      tset(o,sd.loadDefault(sd.construct()));
   }

   protected void loadNormal(Node node, Object o) throws ValidationException {
      tset(o,sd.load(XML.getElement(node,xmlName),sd.construct()));
   }

   public void store(Node node, Object o) {
      sd.store(XML.createElement(node,xmlName),tget(o));
   }

   protected void tstoreNormal(int t, Node node, Object o) throws ValidationException {
      sd.tstore(t,XML.createElement(node,xmlName),tget(o));
   }

   protected boolean isDefault(Object o) {
      Object model = sd.loadDefault(sd.construct());
      return sd.equals(tget(o),model); // default is model
   }

}

