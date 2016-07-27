/*
 * EnumeratedField.java
 */

package com.lifepics.neuron.struct;

import com.lifepics.neuron.core.ValidationException;

import org.w3c.dom.Node;

/**
 * A class that represents an int-valued field of a structure
 * that stores itself in XML as constants from an enumeration.
 */

public class EnumeratedField extends IntegerField {

// --- fields ---

   protected EnumeratedType type;
   //
   // store the type as a field rather than subclassing for each type
   // so that we don't have to write the same constructors repeatedly.

// --- construction ---

   public EnumeratedField(String javaName, String xmlName, EnumeratedType type) {
      super(javaName,xmlName);
      this.type = type;
   }

   public EnumeratedField(String javaName, String xmlName, EnumeratedType type, int sinceVersion, int defaultValue) {
      super(javaName,xmlName,sinceVersion,defaultValue);
      this.type = type;
   }

// --- subclass hooks ---

   protected void loadNormal(Node node, Object o) throws ValidationException {
      tset(o,type.toIntForm(getElementText(node,xmlName)));
   }

   public void store(Node node, Object o) {
      createElementText(node,xmlName,type.fromIntForm(tget(o)));
   }

}

