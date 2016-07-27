/*
 * AbstractField.java
 */

package com.lifepics.neuron.struct;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;

import java.io.File;

import org.w3c.dom.Node;

/**
 * An abstract class that represents a field of a structure.
 *
 * Most of the type logic exists in subclasses of this class.
 * The reason is, I don't want to go back and convert all my
 * existing structures from using int and Integer to using a
 * new mutable class IntegerType ... so I'm stuck with using
 * immutable integers that can only be understood as part of
 * a structure, and that's IntegerField.
 *
 * Don't confuse this class with java.lang.reflect.Field,
 * which it encompasses, or with com.lifepics.neuron.gui.Field,
 * which is a utility class for transferring to / from the GUI
 * that I may one day use here.
 *
 * The structure class should only call the hook functions,
 * so we could split out that interface from the rest of the
 * code here, but I don't see much reason to bother doing so.
 */

public abstract class AbstractField implements FieldNode {

// --- fields ---

   protected String javaName;
   protected java.lang.reflect.Field javaField;

   protected String xmlName;

   protected int sinceVersion;

   // primitive-type fields will have default values, too,
   // but I want to keep those in appropriate-type fields.

// --- construction ---

   // the convention is, any field that gets defaulted, whether by version upgrade
   // or new-object creation, should specify a sinceVersion, even if the version
   // is zero and the default value is the same as the built-in field default value.
   // and vice versa, too ... any field with a version should get defaulted somehow.

   protected AbstractField(String javaName, String xmlName) {
      this.javaName = javaName;
      // javaField is initialized later
      this.xmlName = xmlName;
      sinceVersion = 0; // fields without defaults will use this constructor
   }

   protected AbstractField(String javaName, String xmlName, int sinceVersion) {
      this.javaName = javaName;
      // javaField is initialized later
      this.xmlName = xmlName;
      this.sinceVersion = sinceVersion;
   }

   public void bind(Class c) throws NoSuchFieldException {
      javaField = c.getField(javaName);
   }

// --- implementation of FieldNode ---

   public String getName() { return xmlName; }

// --- subclass hooks ---

   /**
    * Initialize the object by (e.g.) allocating LinkedList fields.
    */
   public abstract void init(Object o);

   /**
    * Make any file paths on the object be relative to the base.
    */
   public abstract void makeRelativeTo(Object o, File base);

   /**
    * Given oDest that's a clone of oSrc, perform any deep copying
    * needed to make a fully independent copy of the source object.
    */
   public abstract void copy(Object oDest, Object oSrc);

   /**
    * Compare o1 and o2 and see if they're equal.
    */
   public abstract boolean equals(Object o1, Object o2);

   /**
    * Merge any changes that occurred between oBase and oSrc into oDest.
    * If independent changes occurred to oDest, changes to oSrc win out.
    */
   public abstract void merge(Object oDest, Object oBase, Object oSrc);

   /**
    * Load from an XML node, taking the version into account.
    */
   public void load(Node node, Object o, int version) throws ValidationException {
      if (version >= sinceVersion) {
         loadNormal(node,o);
      } else {
         loadSpecial(node,o,version);
      }
   }

   /**
    * Provide a hook for subclasses to do more than have defaults.
    */
   protected void loadSpecial(Node node, Object o, int version) throws ValidationException {
      loadDefault(o);
   }

   /**
    * Load the default value of a field that didn't exist before.
    */
   public abstract void loadDefault(Object o);

   /**
    * Load from an XML node, not taking the version into account.
    */
   protected abstract void loadNormal(Node node, Object o) throws ValidationException;

   /**
    * Store into an XML node.
    */
   public abstract void store(Node node, Object o);

   /**
    * Store into an XML node in the form that was used at the given time.
    * Any subclass that does recursion needs to override this to stay in
    * the tstore chain.  The "t" stands for time.
    */
   public void tstore(int t, Node node, Object o, int version) throws ValidationException {
      if (version >= sinceVersion) {
         tstoreNormal(t,node,o);
      } else {
         tstoreSpecial(t,node,o,version);
      }
   }

   protected void tstoreSpecial(int t, Node node, Object o, int version) throws ValidationException {
      tstoreDefault(t,o);
   }

   protected void tstoreDefault(int t, Object o) throws ValidationException {
      if ( ! isDefault(o) ) throw new ValidationException(Text.get(AbstractField.class,"e1",new Object[] { xmlName, new Integer(t) }));
   }
   // note, the isDefault test is not robust, it just covers the common case
   // when a new field is added with a default value.  if you've converted
   // a field with loadSpecial, you can convert it back with tstoreSpecial.
   // for more complex changes, you just have to reset the minimum tstore version
   // and then either deal with two different versions of LC Admin for a while or
   // update the default LC version.
   //
   // examples of more complex changes:  anything that requires a custom loadDefault,
   // adding new values to an enumeration, making a field nullable, removing a field.

   protected boolean isDefault(Object o) {
      return false;
      // don't allow rolling anything back unless we're sure it will default correctly
   }

   /**
    * Store into an XML node, not taking the version into account.
    */
   protected void tstoreNormal(int t, Node node, Object o) throws ValidationException {
      store(node,o);
   }

}

