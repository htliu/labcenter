/*
 * Context.java
 */

package com.lifepics.neuron.install;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.net.Query;
import com.lifepics.neuron.object.Obfuscate;
import com.lifepics.neuron.struct.*;

import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * A small execution context that contains string variables
 * and knows how to do substitution and other basic things.
 */

public class Context extends Structure {

// --- fields ---

   public LinkedList variables;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      Context.class,
      // no version
      new AbstractField[] {

         new StructureListField("variables","Variable",Variable.sd,Merge.NO_MERGE), // could implement MERGE_IDENTITY if needed
      });

   protected StructureDefinition sd() { return sd; }

// --- validation ---

   public void validate() throws ValidationException {

   // no duplicates

      HashSet set = new HashSet();

      Iterator i = variables.iterator();
      while (i.hasNext()) {
         Variable v = (Variable) i.next();
         if ( ! set.add(v.name) ) throw new ValidationException(Text.get(this,"e2",new Object[] { v.name }));
      }
   }

// --- transform ---

   private static final int TRANSFORM_LENGTH = 2;

   private static final int TRANSFORM_NONE  = -1;

   private static final int TRANSFORM_NORMAL = 0;
   private static final int TRANSFORM_SECURE = 1; // a.k.a. hidden
   private static final int TRANSFORM_OBFUSC = 2;
   private static final int TRANSFORM_URLENC = 3;

   private static final int TRANSFORM_CTXSEC = 4; // context side secure

   private static int getTransform(String name) throws Exception {
      int transform = TRANSFORM_NONE;
      if (name.length() >= TRANSFORM_LENGTH) {
         char c1 = name.charAt(1);
         if (c1 == '-' || c1 == '+') {
            char c0 = name.charAt(0);
            switch (c0) {
            case 'n':  transform = TRANSFORM_NORMAL;  break;
            case 's':  transform = TRANSFORM_SECURE;  break;
            case 'o':  transform = TRANSFORM_OBFUSC;  break;
            case 'u':  transform = TRANSFORM_URLENC;  break;
            default: throw new Exception(Text.get(Context.class,"e5",new Object[] { name }));
            }
            if (c1 == '+') transform += TRANSFORM_CTXSEC;
         }
      }
      return transform;
   }

   private static String transformOut(String value, int transform) throws Exception {
      boolean ctxsec = false;
      if (transform >= TRANSFORM_CTXSEC) { ctxsec = true; transform -= TRANSFORM_CTXSEC; }

      if (ctxsec) {
         value = Obfuscate.recover(value,/* classifier = */ 0);
      }

      switch (transform) {
      case TRANSFORM_SECURE:
         value = Obfuscate.hide(value,/* classifier = */ 0);
         break;
      case TRANSFORM_OBFUSC:
         value = Query.encode(Obfuscate.hideAlternate(value));
         break;
      case TRANSFORM_URLENC:
         value = URLEncoder.encode(value,"UTF-8");
         break;
      }

      return value;
   }

   private static String transformIn(String value, int transform) throws Exception {
      boolean ctxsec = false;
      if (transform >= TRANSFORM_CTXSEC) { ctxsec = true; transform -= TRANSFORM_CTXSEC; }

      switch (transform) {
      case TRANSFORM_SECURE:
         value = Obfuscate.recover(value,/* classifier = */ 0);
         break;
      case TRANSFORM_OBFUSC:
         throw new Exception(Text.get(Context.class,"e6"));
      case TRANSFORM_URLENC:
         throw new Exception(Text.get(Context.class,"e7"));
      }

      if (ctxsec) {
         value = Obfuscate.hide(value,/* classifier = */ 0);
      }

      return value;
   }

   // the "s+" mode is inefficient, but the double conversion guarantees that it's not
   // misused on something that's not in secure form.  (also you don't have to use it)

   // by the way, the point of ctxsec is that if there's password information, we don't
   // want to leave it sitting around in the clear in the install file.

// --- functions ---

   /**
    * @return The variable record, or null if not found.
    */
   private Variable lookup(String name) {
      Iterator i = variables.iterator();
      while (i.hasNext()) {
         Variable v = (Variable) i.next();
         if (v.name.equals(name)) return v;
      }
      return null;
      // contexts are small enough that linear search is fine,
      // and preserving creation order is useful for analysis
   }

   private String getRaw(String name) throws Exception {
      Variable v = lookup(name);
      if (v == null) throw new Exception(Text.get(this,"e1",new Object[] { name }));
      return v.value;
   }

   public String get(String name) throws Exception {
      int transform = getTransform(name);
      if (transform != TRANSFORM_NONE) name = name.substring(TRANSFORM_LENGTH);
      return transformOut(getRaw(name),transform);
   }

   private void setRaw(String name, String value) {
      Variable v = lookup(name);
      if (v == null) {
         v = new Variable();
         v.name = name;
         variables.add(v);
      }
      v.value = value;
   }

   public void set(String name, String value) throws Exception {
      int transform = getTransform(name);
      if (transform != TRANSFORM_NONE) name = name.substring(TRANSFORM_LENGTH);
      setRaw(name,transformIn(value,transform));
   }

   public void set(Context c) {
      Iterator i = c.variables.iterator();
      while (i.hasNext()) {
         Variable v = (Variable) i.next();
         setRaw(v.name,v.value);
      }
   }

   /**
    * Replace square-bracketed local variable names with values.
    */
   public String eval(String s) throws Exception {
      String save = s;
      String result = "";

      while (true) {
         int i1 = s.indexOf('[');
         int i2 = s.indexOf(']');

         if (i1 == -1 && i2 == -1) { // no more substitutions

            return result + s;

         } else if (i1 != -1 && i2 != -1 && i1 < i2) { // at least one more substitution

            int i3 = s.indexOf('[',i1+1);
            if (i3 == -1 || i3 > i2) { // prevent [ [ ] syntax

               result = result + s.substring(0,i1) + get(s.substring(i1+1,i2));
               s = s.substring(i2+1); // don't eval the result of the get call
               continue;
            }
         }
         // else syntax error

         throw new Exception(Text.get(this,"e3",new Object[] { save }));
      }
   }

   /**
    * Unwrap a single square-bracketed local variable name.
    */
   public static String unwrap(String s) throws Exception {
      String save = s;

      if (s.startsWith("[") && s.endsWith("]")) {
         s = s.substring(1,s.length()-1);

         if (s.indexOf('[') == -1 && s.indexOf(']') == -1) {
            return s;
         }
      }
      // else syntax error

      throw new Exception(Text.get(Context.class,"e4",new Object[] { save }));
   }

// --- variable class ---

   public static class Variable extends Structure {

      public String name;
      public String value;

      public static final StructureDefinition sd = new StructureDefinition(

         Variable.class,
         // no version
         new AbstractField[] {

            new StringField("name","Name"),
            new StringField("value","Value")
         });

      static { sd.setAttributed(); }
      protected StructureDefinition sd() { return sd; }

      public void validate() throws ValidationException {
      }
   }

}

