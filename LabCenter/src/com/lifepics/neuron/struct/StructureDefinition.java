/*
 * StructureDefinition.java
 */

package com.lifepics.neuron.struct;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.object.XML;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

import org.w3c.dom.Node;

/**
 * A class that represents a structure along with its fields.
 *
 * The functions here are mutually recursive with the ones
 * in AbstractField, by way of StructureField, but the declarations
 * are slightly different because we're in a different situation.
 *
 * The reason for adding all these structure classes is to allow
 * me to merge changes during auto-config.  The merge has to be
 * at the Java object level, not the XML level, because in the XML
 * there's no way to deal with version changes.
 */

public class StructureDefinition {

// --- fields ---

   private Class c;
   private AbstractField[] fields; // constant, so array is nice

   private boolean versioned;
   private int versionMin;
   private int versionMax;
   private boolean attributed;
   private History history;

// --- construction ---

   /**
    * Constructor for simple structures that don't have a version field.
    */
   public StructureDefinition(Class c, AbstractField[] fields) {
      this.c = c;
      this.fields = fields;
      versioned = false;
      bind();
   }

   /**
    * Constructor for more complex structures that have a version field.
    * The odd argument order is much more convenient for initialization.
    */
   public StructureDefinition(Class c, int versionMin, int versionMax, AbstractField[] fields) {
      this.c = c;
      this.fields = fields;
      versioned = true;
      this.versionMin = versionMin;
      this.versionMax = versionMax;
      bind();
   }

   /**
    * Constructor for even more complex structures that have a version field
    * and know something about how the version number has changed over time.
    */
   public StructureDefinition(Class c, int versionMin, History history, AbstractField[] fields) {
      this.c = c;
      this.fields = fields;
      versioned = true;
      this.versionMin = versionMin;
      this.versionMax = history.getVersionMax();
      this.history = history;
      bind();
   }

   /**
    * Constructor for structures that are subclasses of other structures.
    */
   public StructureDefinition(Class c, StructureDefinition parent, AbstractField[] fields) {
      this.c = c;
      this.fields = append(parent.fields,fields); // copying is semi-inefficient but easy
      this.versioned = parent.versioned;
      this.versionMin = parent.versionMin;
      this.versionMax = parent.versionMax;
      this.history = parent.history;
      bind();
   }

   private void bind() {
      try {
         for (int i=0; i<fields.length; i++) {
            fields[i].bind(c);
         }
      } catch (NoSuchFieldException e) {
         throw new Error(e);
      }
   }

   public void setAttributed() {
      attributed = true;
      for (int i=0; i<fields.length; i++) {
         ((AtomicField) fields[i]).setAttributed();
      }
      // all fields on an attributed structure must be atomic,
      // otherwise it's programmer error (ClassCastException).
   }

   private static AbstractField[] append(AbstractField[] f1, AbstractField[] f2) {
      AbstractField[] f = new AbstractField[f1.length + f2.length];
      System.arraycopy(f1,0,f,0,        f1.length);
      System.arraycopy(f2,0,f,f1.length,f2.length);
      return f;
   }

   public Object construct() {
      try {

         return c.newInstance();
         // it seems like it might make sense to initialize the fields here,
         // but for now I'll preserve the old behavior, which is to let the
         // control come back to init via Structure constructor.

      } catch (InstantiationException e) {
         throw new Error(e);
      } catch (IllegalAccessException e) {
         throw new Error(e);
      }
   }

// --- helper for FieldNode ---

   public Collection getChildren() {
      return Collections.unmodifiableList(Arrays.asList(fields));
   }

// --- subclass hooks ---

   public void init(Object o) {
      for (int i=0; i<fields.length; i++) {
         fields[i].init(o);
      }
   }

   public void makeRelativeTo(Object o, File base) {
      for (int i=0; i<fields.length; i++) {
         fields[i].makeRelativeTo(o,base);
      }
   }

   public void copy(Object oDest, Object oSrc) {
      for (int i=0; i<fields.length; i++) {
         fields[i].copy(oDest,oSrc);
      }
   }

   public boolean equals(Object o1, Object o2) {
      for (int i=0; i<fields.length; i++) {
         if ( ! fields[i].equals(o1,o2) ) return false;
      }
      return true;
   }

   public boolean equalsNullable(Object o1, Object o2) {
      if (o1 == null) {
         return (o2 == null);
      } else {
         return (o2 != null && equals(o1,o2));
      }
      // different from Nullable.equals in that it checks both sides
      // for nullness, and avoids using Object.equals on the objects
   }

   public boolean equalsElements(LinkedList list1, LinkedList list2) {
      if (list1.size() != list2.size()) return false;
      Iterator i1 = list1.iterator();
      Iterator i2 = list2.iterator();
      while (i1.hasNext() && i2.hasNext()) {
         if ( ! equals(i1.next(),i2.next()) ) return false;
      }
      return true;
      // as above, avoids using Object.equals on the objects
   }

   public void merge(Object oDest, Object oBase, Object oSrc) {
      for (int i=0; i<fields.length; i++) {
         fields[i].merge(oDest,oBase,oSrc);
      }
   }

   public Object load(Node node, Object o) throws ValidationException {
      int version = versioned ? getVersion(node,versionMin,versionMax) : 0;
      for (int i=0; i<fields.length; i++) {
         fields[i].load(node,o,version);
      }
      return o; // convenience
   }

   public Object loadDefault(Object o) {
      for (int i=0; i<fields.length; i++) {
         fields[i].loadDefault(o);
      }
      return o; // convenience
   }

   public void store(Node node, Object o) {
      if (versioned) createVersion(node,versionMax);
      for (int i=0; i<fields.length; i++) {
         fields[i].store(node,o);
      }
   }

   public void tstore(int t, Node node, Object o) throws ValidationException {
      int version = versioned ? (history == null ? versionMax : history.getVersion(t)) : 0;
      if (versioned) createVersion(node,version);
      for (int i=0; i<fields.length; i++) {
         fields[i].tstore(t,node,o,version);
      }
   }

// --- version helpers ---

   private static final String NAME_VERSION = "Version";
   private static final String NAME_ATTRIBUTE = "v";

   /**
    * Get the version entry for the node, validating that it falls within the given range.
    */
   public int getVersion(Node node, int versionMin, int versionMax) throws ValidationException {

      String value;
      if (attributed) value = XML.getAttribute  (node,NAME_ATTRIBUTE);
      else            value = XML.getElementText(node,NAME_VERSION);

      int version = Convert.toInt(value);
      if ( ! (versionMin <= version && version <= versionMax) ) throw new ValidationException(Text.get(this,"e1",new Object[] { c.getName(),value,Convert.fromInt(versionMin),Convert.fromInt(versionMax) }));
      return version; // if versionMin == versionMax, caller can ignore result

      // we could include the node name (getNodeName) and the version number information,
      // but that would just be asking for people to change the version numbers by hand.
   }

   /**
    * Create a version entry for the node.
    */
   public void createVersion(Node node, int version) {
      String value = Convert.fromInt(version);

      if (attributed) XML.setAttribute     (node,NAME_ATTRIBUTE,value);
      else            XML.createElementText(node,NAME_VERSION,  value);
   }

}

