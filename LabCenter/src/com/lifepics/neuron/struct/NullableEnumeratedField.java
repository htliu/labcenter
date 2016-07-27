/*
 * NullableEnumeratedField.java
 */

package com.lifepics.neuron.struct;

import com.lifepics.neuron.core.ValidationException;

import org.w3c.dom.Node;

/**
 * A class that represents an Integer-valued field of a structure
 * that stores itself in XML as constants from an enumeration.
 */

public class NullableEnumeratedField extends NullableIntegerField {

// --- fields ---

   protected EnumeratedType type;

// --- construction ---

   public NullableEnumeratedField(String javaName, String xmlName, EnumeratedType type) {
      super(javaName,xmlName);
      this.type = type;
   }

   public NullableEnumeratedField(String javaName, String xmlName, EnumeratedType type, int sinceVersion, Integer defaultValue) {
      super(javaName,xmlName,sinceVersion,defaultValue);
      this.type = type;
   }

// --- subclass hooks ---

   // it's not convenient to have nullable to/from functions for every enum,
   // so here we just have to write it out as more than one line.

   protected void loadNormal(Node node, Object o) throws ValidationException {
      String temp = getNullableText(node,xmlName);
      Integer i = (temp == null) ? null : new Integer(type.toIntForm(temp));
      tset(o,i);
   }

   public void store(Node node, Object o) {
      Integer i = tget(o);
      String temp = (i == null) ? null : type.fromIntForm(i.intValue());
      createNullableText(node,xmlName,temp);
   }

}

