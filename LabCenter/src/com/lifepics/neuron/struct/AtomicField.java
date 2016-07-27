/*
 * AtomicField.java
 */

package com.lifepics.neuron.struct;

import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.meta.EditAccessor;
import com.lifepics.neuron.object.XML;

import java.util.Collection;

import org.w3c.dom.Node;

/**
 * An intermediate abstraction that represents an atomic field
 * (as opposed to a structure-valued or list-valued field).
 * For now, the distinguishing characteristic of atomic fields
 * is that they can store themselves as attributes if desired.
 *
 * Attribute-ness shouldn't be a dynamic property, this is just
 * the easiest way I could see to implement it.
 */

public abstract class AtomicField extends AbstractField implements EditAccessor {

// --- fields ---

   private boolean attributed;

// --- construction ---

   public AtomicField(String javaName, String xmlName) {
      super(javaName,xmlName);
   }

   public AtomicField(String javaName, String xmlName, int sinceVersion) {
      super(javaName,xmlName,sinceVersion);
   }

   public void setAttributed() {
      attributed = true;
   }

// --- implementation of FieldNode ---

   public boolean isDynamic() { return false; }
   public Collection getChildren(Object o) { return null; }

// --- helpers ---

   protected String getElementText(Node node, String name) throws ValidationException {
      if (attributed) return XML.getAttribute  (node,name);
      else            return XML.getElementText(node,name);
   }

   protected String getNullableText(Node node, String name) throws ValidationException {
      if (attributed) return XML.getAttributeTry(node,name);
      else            return XML.getNullableText(node,name);
   }

   protected void createElementText(Node node, String name, String value) {
      if (attributed) XML.setAttribute     (node,name,value);
      else            XML.createElementText(node,name,value);
   }

   protected void createNullableText(Node node, String name, String value) {
      if (attributed) { if (value != null) XML.setAttribute      (node,name,value); }
      else                                 XML.createNullableText(node,name,value);
   }

}

