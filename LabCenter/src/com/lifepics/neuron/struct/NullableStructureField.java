/*
 * NullableStructureField.java
 */

package com.lifepics.neuron.struct;

import com.lifepics.neuron.core.Nullable;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.object.Copyable;
import com.lifepics.neuron.object.CopyUtil;
import com.lifepics.neuron.object.XML;

import java.io.File;
import java.util.Collection;

import org.w3c.dom.Node;

/**
 * A class that represents a nullable Structure-valued field of a structure.
 */

public class NullableStructureField extends CompositeField {

// --- fields ---

   protected StructureDefinition sd;

// --- construction ---

   public NullableStructureField(String javaName, String xmlName, StructureDefinition sd) {
      super(javaName,xmlName);
      this.sd = sd;
   }

   public NullableStructureField(String javaName, String xmlName, StructureDefinition sd, int sinceVersion) {
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
      // no action needed
   }

   public void makeRelativeTo(Object o, File base) {
      Object val = tget(o);
      if (val != null) sd.makeRelativeTo(val,base);
   }

   public void copy(Object oDest, Object oSrc) {
      Object val = tget(oSrc);
      tset(oDest,(val == null) ? null : CopyUtil.copy((Copyable) val));
      // this is the only requirement on the structure, that it has to be copyable.
      // in fact, the structure will be a Structure, but that's just a convenience.
   }

   public boolean equals(Object o1, Object o2) {
      return sd.equalsNullable(tget(o1),tget(o2));
   }

   public void merge(Object oDest, Object oBase, Object oSrc) {
      Object valDest = tget(oDest);
      Object valBase = tget(oBase);
      Object valSrc  = tget(oSrc);
      if (valDest != null && valBase != null && valSrc != null) {
         sd.merge(valDest,valBase,valSrc);
      } else {
         if ( ! sd.equalsNullable(valSrc,valBase) ) copy(oDest,oSrc);
      }
      // this is the canonical example of this null-pattern.
      // what's going on is, we have three nullable objects.
      // if they're all not null, we can look inside
      // and merge them, just like in the non-null version,
      // which is StructureField.
      // if any of them are null, we can't merge field-by-field,
      // so we fall back, treat the structure as an immutable
      // atomic entity, and get the same pattern as for all the
      // fundamental types .. except that we have to use copy
      // instead of tset since it's not really immutable and atomic.
   }

   public void loadDefault(Object o) {
      // no action needed
   }

   protected void loadNormal(Node node, Object o) throws ValidationException {
      Node child = XML.getElementTry(node,xmlName);
      if (child != null) tset(o,sd.load(child,sd.construct()));
   }

   public void store(Node node, Object o) {
      Object val = tget(o);
      if (val != null) sd.store(XML.createElement(node,xmlName),val);
   }

   protected void tstoreNormal(int t, Node node, Object o) throws ValidationException {
      Object val = tget(o);
      if (val != null) sd.tstore(t,XML.createElement(node,xmlName),val);
   }

   protected boolean isDefault(Object o) {
      return (tget(o) == null); // default is null
   }

}

