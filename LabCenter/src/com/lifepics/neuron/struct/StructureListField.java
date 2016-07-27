/*
 * StructureListField.java
 */

package com.lifepics.neuron.struct;

import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.meta.Accessor;
import com.lifepics.neuron.object.CopyUtil;
import com.lifepics.neuron.object.XML;

import java.io.File;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;

import org.w3c.dom.Node;

/**
 * A class that represents a LinkedList<Structure>-valued field of a structure
 * that stores itself in XML as an inline list.
 */

public class StructureListField extends CompositeField {

// --- fields ---

   protected StructureDefinition sd;
   private int mergePolicy;
   private int defaultCount;

   private Accessor accessor;
   private Comparator comparator;

// --- construction ---

   public StructureListField(String javaName, String xmlName, StructureDefinition sd, int mergePolicy) {
      super(javaName,xmlName);
      this.sd = sd;
      this.mergePolicy = mergePolicy;
   }

   public StructureListField(String javaName, String xmlName, StructureDefinition sd, int mergePolicy, int sinceVersion, int defaultCount) {
      super(javaName,xmlName,sinceVersion);
      this.sd = sd;
      this.mergePolicy = mergePolicy;
      this.defaultCount = defaultCount;
   }

   public AbstractField with(Accessor accessor, Comparator comparator) {
      this.accessor = accessor;
      this.comparator = comparator;
      return this;
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
      Iterator i = tget(o).iterator();
      while (i.hasNext()) {
         sd.makeRelativeTo(i.next(),base);
      }
   }

   public void copy(Object oDest, Object oSrc) {
      tset(oDest,CopyUtil.copyList(tget(oSrc)));
      // this is the only requirement on the structure, that it has to be copyable.
      // in fact, the structure will be a Structure, but that's just a convenience.
   }

   public boolean equals(Object o1, Object o2) {
      return sd.equalsElements(tget(o1),tget(o2));
   }

   public void merge(Object oDest, Object oBase, Object oSrc) {
      switch (mergePolicy) {
      case Merge.EQUALITY:  tset(oDest,Merge.mergeByEquality(tget(oDest),tget(oBase),tget(oSrc),/* copy = */ true));       break;
      case Merge.IDENTITY:  tset(oDest,Merge.mergeByIdentity(tget(oDest),tget(oBase),tget(oSrc),sd,accessor,comparator));  break;
      case Merge.POSITION:  tset(oDest,Merge.mergeByPosition(tget(oDest),tget(oBase),tget(oSrc),sd));                      break;
      //   Merge.NO_MERGE is intentionally missing
      default:  throw new IllegalArgumentException();
      }
      // the equality case uses Object.equals, which is untidy but not worth fixing
   }

   public void loadDefault(Object o) {
      LinkedList list = tget(o);
      for (int i=0; i<defaultCount; i++) {
         list.add(sd.loadDefault(sd.construct()));
      }
      // adding some number of identical default objects might seem strange,
      // but really either the number is zero or the objects are backprints.
   }

   protected void loadNormal(Node node, Object o) throws ValidationException {
      LinkedList list = tget(o);
      Iterator i = XML.getElements(node,xmlName);
      while (i.hasNext()) {
         list.add(sd.load((Node) i.next(),sd.construct()));
      }
   }

   public void store(Node node, Object o) {
      Iterator i = tget(o).iterator();
      while (i.hasNext()) {
         sd.store(XML.createElement(node,xmlName),i.next());
      }
   }

   protected void tstoreNormal(int t, Node node, Object o) throws ValidationException {
      Iterator i = tget(o).iterator();
      while (i.hasNext()) {
         sd.tstore(t,XML.createElement(node,xmlName),i.next());
      }
   }

   protected boolean isDefault(Object o) {
      LinkedList list = tget(o);
      if (list.size() != defaultCount) return false;
      Object model = sd.loadDefault(sd.construct());
      Iterator i = list.iterator();
      while (i.hasNext()) {
         if ( ! sd.equals(i.next(),model) ) return false;
      }
      return true; // default is N copies of model
   }

}

