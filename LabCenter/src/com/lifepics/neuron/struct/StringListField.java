/*
 * StringListField.java
 */

package com.lifepics.neuron.struct;

import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.object.XML;

import org.w3c.dom.Node;

/**
 * A class that represents a LinkedList<String>-valued field of a structure
 * that stores itself in XML as a nested list.
 * Actually the elements can be any immutable type, like Integer -- you just
 * have to override the load and store functions.
 */

public class StringListField extends InlineListField {

// --- fields ---

   private String xmlNestedName;

// --- construction ---

   public StringListField(String javaName, String xmlName, String xmlNestedName) {
      super(javaName,xmlName);
      this.xmlNestedName = xmlNestedName;
   }

   public StringListField(String javaName, String xmlName, String xmlNestedName, int sinceVersion) {
      super(javaName,xmlName,sinceVersion);
      this.xmlNestedName = xmlNestedName;
      // the empty list is the only allowed default value
   }

// --- subclass hooks ---

   protected void loadNormal(Node node, Object o) throws ValidationException {
      tset(o,XML.getStringList(node,xmlName,xmlNestedName));
   }

   public void store(Node node, Object o) {
      XML.createStringList(node,xmlName,xmlNestedName,tget(o));
   }

}

