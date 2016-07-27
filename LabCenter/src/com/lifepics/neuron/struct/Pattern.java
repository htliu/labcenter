/*
 * Pattern.java
 */

package com.lifepics.neuron.struct;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.meta.NoCaseComparator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * An object that represents a new-SKU pattern, which is
 * a product code plus some number of attribute value sets,
 * for example "4x6 Finish Glossy|Matte Cover ~Red|Green".
 * There shouldn't be many of these, so don't worry about
 * interning them.  Although they're not interned,
 * they *are* immutable, so if you make new ones in the UI
 * you can share them across mappings.
 */

public final class Pattern implements PSKU {

// --- fields ---

   private String product;
   private Map attributes; // String -> ValueSet

   private int    cachedHashCode;
   private String cachedToString;
   private String cachedEncode;
   // these are all expensive to compute, so cache them

// --- methods ---

   public Pattern(String product, HashMap attributes) {

      this.product = product;
      this.attributes = attributes;
      //
      // we require HashMap to get the standard attribute order,
      // and we don't bother copying since there's no iteration
      // over patterns like there is over NewSKU.

      computeHashCode();
      computeToString();
      computeEncode();
   }

   public String getProduct() { return product; }

   // immutable, so can't return attributes without copying, hence the custom accessors
   public int getAttributeCount() { return attributes.size(); }
   public boolean hasSameAttributes(HashMap attributes) { return this.attributes.equals(attributes); }

   public boolean equals(Object o) {
      if ( ! (o instanceof Pattern) ) return false;
      Pattern p = (Pattern) o;
      if (cachedHashCode != p.cachedHashCode) return false; // quick filter
      return (product.equals(p.product) && attributes.equals(p.attributes));
   }

   public boolean matches(SKU sku) {
      if ( ! (sku instanceof NewSKU) ) return false; // no patterns for old SKUs
      NewSKU ns = (NewSKU) sku;
      if ( ! product.equals(ns.getProduct()) ) return false;

      Iterator i = attributes.entrySet().iterator();
      while (i.hasNext()) {
         Map.Entry entry = (Map.Entry) i.next();

         String key = (String) entry.getKey();
         ValueSet vs = (ValueSet) entry.getValue();

         String value = ns.getAttribute(key);
         if (value == null || ! vs.matches(value)) return false;

         // every attribute in the pattern must be in the SKU,
         // but the SKU can contain attributes that aren't
         // in the pattern.  so, a pattern with no attributes
         // will matches all SKUs with the given product code.

         // what about optional attributes?  right now there's no way
         // to say "I want this optional attribute to be missing".
         // not a feature we need, but if someday we want it, what we
         // should do is allow null into the value set, and then just
         // take out the "value == null" condition above.
         // we could use a special fixed string like "^n" to store it,
         // then the only question is the toString form.  it's a hard
         // question, that's why I didn't implement it already.
         // if you need a kluge, you can invert all possible values.
      }

      return true;
   }

   public int    hashCode() { return cachedHashCode; }
   public String toString() { return cachedToString; }

   private void computeHashCode() {
      cachedHashCode = product.hashCode()*31 + attributes.hashCode();
   }

   private void computeToString() {

      StringBuffer b = new StringBuffer();
      b.append(product);

      b.append(' ');
      if ( ! attributes.isEmpty() ) {
         b.append(Text.get(this,"s1"));
      } else {
         b.append(Text.get(this,"s2"));
      }

      Iterator i = attributes.entrySet().iterator();
      while (i.hasNext()) {
         Map.Entry entry = (Map.Entry) i.next();

         b.append(' ');
         b.append(describeVS((String) entry.getKey(),(ValueSet) entry.getValue()));
      }

      cachedToString = b.toString();
   }

   private void computeEncode() {

      StringBuffer b = new StringBuffer();
      b.append(NewSKU.encodeString(product));

      Set set = attributes.entrySet();
      Map.Entry[] array = (Map.Entry[]) set.toArray(new Map.Entry[set.size()]);

      // sort the attributes so that we don't get spurious config file diffs;
      // of course we wouldn't see the diff inside LC, only with a diff tool.
      Arrays.sort(array,new NewSKU.KeyComparator(new NoCaseComparator()));

      for (int i=0; i<array.length; i++) {
         Map.Entry entry = array[i];

         b.append(NewSKU.CHAR_DELIM);
         b.append(NewSKU.encodeString((String) entry.getKey()));
         b.append(NewSKU.CHAR_DELIM);
         b.append(NewSKU.encodeString(encodeVS((ValueSet) entry.getValue())));
      }

      cachedEncode = b.toString();
   }

   public static String encode(Pattern p) {
      return p.cachedEncode;
   }

   public static Pattern decode(String s) throws ValidationException {

      String[] array = s.split(NewSKU.REGEX_DELIM,-1); // -1 to stop weird default behavior
      int i = 0;

      String product = NewSKU.decodeString(array[i++]); // always one element in array

      HashMap attributes = new HashMap();
      for ( ; i+1<array.length; i+=2) attributes.put(NewSKU.decodeString(array[i]),decodeVS(NewSKU.decodeString(array[i+1])));

      if (i != array.length) throw new ValidationException(Text.get(Pattern.class,"e3"));
      // if we didn't read the last element, the length was even ... that's an error

      return new Pattern(product,attributes);
   }

// --- value set ---

   // value sets must not be empty, and must not contain duplicates.
   // decode handles the first part automatically, but if you're
   // doing programmatic construction you need to look out for that.
   // HashSet handles the second part automatically.
   // we don't validate against duplicate values, but then we don't
   // validate against duplicate keys either, we just do what we're
   // told and construct whatever object.

   public static class ValueSet {

      public boolean inverted;
      public HashSet values; // String

      public boolean equals(Object o) {
         if ( ! (o instanceof ValueSet) ) return false;
         ValueSet vs = (ValueSet) o;
         return (inverted == vs.inverted && values.equals(vs.values));
      }

      public boolean matches(String value) {
         return (inverted ^ values.contains(value));
      }

      public int hashCode() {
         int hash = values.hashCode();
         return inverted ? ~hash : hash; // could just use hash, but this
         // makes the quick filter in Pattern.equals work a little better
      }
   }

   private static String describeVS(String key, ValueSet vs) {
      StringBuffer b = new StringBuffer();

      if (vs.inverted) b.append("Not "); // there's at least one value, so space is OK

      boolean first = true;

      Iterator i = vs.values.iterator();
      while (i.hasNext()) {
         String value = (String) i.next();

         if (first) first = false;
         else b.append('/');
         b.append(NewSKU.describe(key,value));
      }

      return b.toString();
   }

   private static String encodeVS(ValueSet vs) {
      StringBuffer b = new StringBuffer();

      if (vs.inverted) b.append(CHAR_NOT);

      Set set = vs.values;
      String[] array = (String[]) set.toArray(new String[set.size()]);

      // sort the attributes so that we don't get spurious config file diffs;
      // of course we wouldn't see the diff inside LC, only with a diff tool.
      Arrays.sort(array,new NoCaseComparator());

      for (int i=0; i<array.length; i++) {
         String value = array[i];

         if (i != 0) b.append(CHAR_OR);
         b.append(encodeSetString(value));
      }

      return b.toString();
   }

   private static ValueSet decodeVS(String s) throws ValidationException {

      ValueSet vs = new ValueSet();

      if (s.startsWith(STRING_NOT)) {
         s = s.substring(1);
         vs.inverted = true;
      } else {
         vs.inverted = false;
      }

      if (s.indexOf(CHAR_NOT) != -1) throw new ValidationException(Text.get(Pattern.class,"e4"));

      // an empty string will split into a single empty string
      // so there's actually no way to create an empty value set

      vs.values = new HashSet();

      String[] array = s.split(REGEX_OR,-1); // -1 to stop weird default behavior
      for (int i=0; i<array.length; i++) {
         vs.values.add(decodeSetString(array[i]));
      }

      return vs;
   }

// --- string encoding ---

   // could unify this with the code in NewSKU, but it's a bit different with two substitutions

   private static final char CHAR_ESC = '^';
   private static final char CHAR_NOT = '~';
   private static final char CHAR_OR  = '|';
   private static final char SUBST_NOT = '-';
   private static final char SUBST_OR  = '!';

   private static final String STRING_NOT = "~"; // must be length 1, see above
   private static final String REGEX_OR = "\\|";

   public static String encodeSetString(String trueForm) {
      StringBuffer b = new StringBuffer();

      int base = 0;
      int i;
      int len = trueForm.length(); // could go in loop, but keep parallel
      for (i=0; i<len; i++) {
         char c = trueForm.charAt(i);

         if (c == CHAR_ESC || c == CHAR_NOT || c == CHAR_OR) {

            if (i > base) b.append(trueForm.substring(base,i));
            base = i+1; // go ahead and set this

            b.append(CHAR_ESC);
            b.append( (c == CHAR_ESC) ? CHAR_ESC : ((c == CHAR_NOT) ? SUBST_NOT : SUBST_OR) );
         }
         // else do nothing, add to base segment
      }

      if (base == 0) return trueForm; // no changes

      if (i > base) b.append(trueForm.substring(base,i));
      // now base=i, but no need to update, we're done

      return b.toString();
   }

   public static String decodeSetString(String xmlForm) throws ValidationException {
      StringBuffer b = new StringBuffer();

      int base = 0;
      int i;
      int len = xmlForm.length(); // here we need this more than once
      for (i=0; i<len; i++) {
         char c = xmlForm.charAt(i);

         if (c == CHAR_ESC) {

            if (i > base) b.append(xmlForm.substring(base,i));
            base = i+2; // go ahead and set this, even if out of bounds

            if (++i == len) throw new ValidationException(Text.get(Pattern.class,"e1",new Object[] { xmlForm }));
            char e = xmlForm.charAt(i);

            if      (e == CHAR_ESC ) b.append(CHAR_ESC);
            else if (e == SUBST_NOT) b.append(CHAR_NOT);
            else if (e == SUBST_OR ) b.append(CHAR_OR );
            else throw new ValidationException(Text.get(Pattern.class,"e2",new Object[] { xmlForm }));
         }
         // else not an escape sequence, add to base segment
      }

      if (base == 0) return xmlForm; // no changes

      if (i > base) b.append(xmlForm.substring(base,i));
      // now base=i, but no need to update, we're done

      return b.toString();
   }

}

