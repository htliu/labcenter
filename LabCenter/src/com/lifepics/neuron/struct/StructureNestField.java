/*
 * StructureNestField.java
 */

package com.lifepics.neuron.struct;

import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.object.XML;

import org.w3c.dom.Node;

/**
 * A class that represents a LinkedList<Structure>-valued field of a structure
 * that stores itself in XML as a nested list.
 * StructureNest is to StructureList as StringList is to InlineList.
 */

public class StructureNestField extends StructureListField {

// --- fields ---

   private String xmlParentName;

// --- construction ---

   public StructureNestField(String javaName, String xmlParentName, String xmlName, StructureDefinition sd, int mergePolicy) {
      super(javaName,xmlName,sd,mergePolicy);
      this.xmlParentName = xmlParentName;
   }

   public StructureNestField(String javaName, String xmlParentName, String xmlName, StructureDefinition sd, int mergePolicy, int sinceVersion, int defaultCount) {
      super(javaName,xmlName,sd,mergePolicy,sinceVersion,defaultCount);
      this.xmlParentName = xmlParentName;
   }

// --- subclass hooks ---

   protected void loadNormal(Node node, Object o) throws ValidationException {
      super.loadNormal(XML.getElement(node,xmlParentName),o);
   }

   public void store(Node node, Object o) {
      super.store(XML.createElement(node,xmlParentName),o);
   }

   protected void tstoreNormal(int t, Node node, Object o) throws ValidationException {
      super.tstoreNormal(t,XML.createElement(node,xmlParentName),o);
   }

}

