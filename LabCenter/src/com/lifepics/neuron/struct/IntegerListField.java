/*
 * IntegerListField.java
 */

package com.lifepics.neuron.struct;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.object.XML;

import java.util.Iterator;
import java.util.LinkedList;

import org.w3c.dom.Node;

/**
 * A class that represents a LinkedList<Integer>-valued field of a structure
 * that stores itself in XML as an inline list, possibly with an enumeration.
 */

public class IntegerListField extends InlineListField {

// --- fields ---

   protected EnumeratedType type;
   //
   // store the type as a field rather than subclassing for each type
   // so that we don't have to write the same constructors repeatedly.

   public static EnumeratedType integerEnumeratedType = new EnumeratedType() {
      public int toIntForm(String s) throws ValidationException { return Convert.toInt(s); }
      public String fromIntForm(int i) { return Convert.fromInt(i); }
   };
   // just a technical convenience, saves creating EnumeratedListField subclass
   // and adds almost no overhead

// --- construction ---

   public IntegerListField(String javaName, String xmlName) {
      super(javaName,xmlName);
      type = integerEnumeratedType;
   }

   public IntegerListField(String javaName, String xmlName, int sinceVersion) {
      super(javaName,xmlName,sinceVersion);
      type = integerEnumeratedType;
      // the empty list is the only allowed default value
   }

   public IntegerListField(String javaName, String xmlName, EnumeratedType type) {
      super(javaName,xmlName);
      this.type = type;
   }

   public IntegerListField(String javaName, String xmlName, EnumeratedType type, int sinceVersion) {
      super(javaName,xmlName,sinceVersion);
      this.type = type;
      // the empty list is the only allowed default value
   }

// --- subclass hooks ---

   protected void loadNormal(Node node, Object o) throws ValidationException {
      LinkedList list = tget(o);
      Iterator i = XML.getInlineList(node,xmlName).iterator();
      while (i.hasNext()) {
         list.add(new Integer(type.toIntForm((String) i.next())));
      }
   }

   public void store(Node node, Object o) {
      LinkedList temp = new LinkedList();
      Iterator i = tget(o).iterator();
      while (i.hasNext()) {
         temp.add(type.fromIntForm(((Integer) i.next()).intValue()));
      }
      XML.createInlineList(node,xmlName,temp);
   }

}

