/*
 * Structure.java
 */

package com.lifepics.neuron.struct;

import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.object.Copyable;
import com.lifepics.neuron.object.Relative;
import com.lifepics.neuron.object.XML;

import java.io.File;

import org.w3c.dom.Node;

/**
 * A class that implements the standard structure functions
 * with calls to a single static StructureDefinition object.
 */

public abstract class Structure implements XML.Persist, Copyable, Relative.Path {

// --- subclass hook ---

   protected abstract StructureDefinition sd();

   // each structure class will declare a static member "sd",
   // which you should think of as being like the class;
   // you even refer to it similarly, with X.class vs. X.sd.
   // unfortunately, we can't declare a static abstract
   // variable, so we have to declare a function and implement
   // it on each structure class.

// --- construction ---

   public Structure() {
      sd().init(this);
   }

// --- relativity ---

   public void makeRelativeTo(File base) {
      sd().makeRelativeTo(this,base);
   }

// --- copy function ---

   public Object clone() throws CloneNotSupportedException {
      Object o = super.clone();
      sd().copy(o,this);
      return o;
   }

// --- equals ---

   public boolean equals(Object o) {

      if (o == null) return false; // this is just good form for equals

      if ( ! getClass().equals(o.getClass()) ) return false;
      //
      // this preserves the existing behavior of the equals functions
      // in the old framework.  this is actually important ...
      // the price list combo in pro mode contains both PriceList objects
      // and an empty string, so they get compared to each other.
      // there may well be other examples, too, that's just the first one.
      //
      // once we get into the StructureDefinition-AbstractField recursion,
      // we know the types of everything, no need to check at every step.

      return sd().equals(o,this);
   }

// --- merge ---

   public void merge(Object oBase, Object oSrc) {
      sd().merge(this,oBase,oSrc);
   }

// --- persistence ---

   public Object load(Node node) throws ValidationException {
      return sd().load(node,this);
   }

   public Object loadDefault() {
      return sd().loadDefault(this);
   }

   public void store(Node node) {
      sd().store(node,this);
   }

   public void tstore(int t, Node node) throws ValidationException {
      sd().tstore(t,node,this);
   }

}

