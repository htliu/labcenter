/*
 * NewSKU.java
 */

package com.lifepics.neuron.struct;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.meta.NoCaseComparator;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * An object that represents a new SKU (product plus attributes)
 * as opposed to an old SKU (string).  This is used mostly on
 * the dendron side of things, but it doesn't depend on anything,
 * and we need it here to define {@link SKUField}.
 *
 * NewSKU objects are immutable so that we can intern them and
 * also cache the hash code and string form.  Actually we don't
 * even intern them explicitly, we just prevent construction of
 * non-interned objects.  One big benefit of this is, we can
 * test NewSKU objects for equality using == instead of equals.
 * (Or, rather, we can use equals without overriding it.)
 * Another is, we don't have one hash map for every SKU object
 * in every item in every order, so we save a lot of memory.
 *
 * The guiding principle here was that there should be essentially
 * no information in the SKU object that isn't also visible in the
 * UI string form.  The attribute group names aren't shown, but
 * a human can infer the group name from the attribute value, even
 * if the computer can't, so it's no big loss.  Anyway, that's why
 * we don't store product and attribute IDs.
 *
 * Another important idea is that the existing SKU strings really
 * only perform a small number functions.  You can display them,
 * and you can compare them for equality, and you can save and load
 * them in internal files.  So, if we create an object class that
 * can perform the same functions, it shouldn't be too hard to make
 * it so that old and new SKUs can coexist.
 *
 * The reason we don't store the attributes in some ordered form
 * is that we don't trust the server to send them consistently.
 * Or, to put it another way, one day the dealer may want to change
 * the attribute order, and we have to handle it smoothly.
 * We could standardize the order internally, instead of just using
 * standard order in the computeEncode function, but I'd rather not,
 * because then we might do something that depends on the order.
 * It's a lot of trouble this way, but I still think it's the best.
 */

public final class NewSKU implements SKU {

// --- fields ---

   private String product;
   private Map attributes; // String -> String

   private int    cachedHashCode;
   private String cachedToString;
   private String cachedEncode;

// --- construction ---

   private NewSKU(String product, Map attributes) {

      this.product = product;
      this.attributes = attributes; // copying will happen later, if needed

      // cached fields will be computed later, if needed
   }

// --- public methods ---

   // if we ever have pattern matching, we'll need to expose
   // the product and attributes through read-only functions.
   // and now we do!
   public String getProduct() { return product; }
   public String getAttribute(String key) { return (String) attributes.get(key); }

   public boolean matches(SKU sku) { return equals(sku); }

   public int    hashCode() { return cachedHashCode; }
   public String toString() { return cachedToString; }

   // because of interning, Object.equals does what we want;
   // we do still need comparison code, it's just elsewhere.

   private static HashMap internTable = new HashMap();
   //
   // a HashMap that takes Index -> NewSKU, with the Index object
   // also being a wrapper around the same NewSKU object.
   // this is clumsy ... we ought to be able to say "use this function
   // in place of equals", then we wouldn't need the Index object.
   // in fact, if HashSet had a function "find an object equal to this
   // and return it, with this definition of equals", we wouldn't even
   // need a map.  but, go with what we have.

   public static NewSKU get(String product, Map attributes) {

      // to look up the SKU in the intern table,
      // we need a temporary object that can
      // respond correctly to hashCode and equals.
      //
      NewSKU nsTemp = new NewSKU(product,attributes);
      nsTemp.computeHashCode();
      Index i = new Index(nsTemp);

      synchronized (internTable) {

         NewSKU nsReal = (NewSKU) internTable.get(i);
         if (nsReal != null) {
            return nsReal;
         } else {

            // OK, we haven't seen this one before, finish constructing it
            nsTemp.attributes = new HashMap(attributes); // don't share, don't keep caller's map type
            // note (a) this doesn't change the already-computed hash code,
            //      (b) this makes computeToString use our standard internal attribute order
            nsTemp.computeToString();
            nsTemp.computeEncode();
            internTable.put(i,nsTemp);
            return nsTemp;
            // there's a small window of opportunity for a malicious caller
            // to change the attributes map in another thread, but I'm just
            // not going to worry about that.
            // I do think I shouldn't share, though, since the caller might
            // reasonably want to reuse the same map.
         }
      }
   }

// --- private methods ---

   /**
    * A wrapper class for full comparison of NewSKU objects.
    */
   private static class Index {

      private NewSKU ns;
      public Index(NewSKU ns) { this.ns = ns; }

      public int hashCode() { return ns.cachedHashCode; }
         // might as well skip the hashCode call

      public boolean equals(Object o) {
         if ( ! (o instanceof Index) ) return false;
         Index i = (Index) o;

         return (    ns.product   .equals(i.ns.product   )
                  && ns.attributes.equals(i.ns.attributes) );
         // let Map.equals do all the hard work
      }
   }

   /**
    * Compute the hash code of the new SKU object.
    */
   private void computeHashCode() {

      // the string form includes all the relevant information,
      // but we can't just hash that, because one day we might
      // have attribute order hints, and changing the order
      // would change the string forms and then break everything.
      // or, we'd have to rebuild the intern table, which would
      // be a pain.  so ...

      cachedHashCode = product.hashCode()*31 + attributes.hashCode();
      //
      // I copied the 31 from the List.hashCode spec.  it doesn't
      // matter all that much -- just a way to distinguish things.
      //
      // Map.hashCode does the right thing already, so use that.
      // it adds the entry hash codes (not much else it can do,
      // it has to be symmetric over entries), and within each entry
      // it XORs the key and value hash codes, so that e.g.
      // { A->B C->D } will hash differently than { A->D C->B }.
   }

   /**
    * Compute the human-readable form of the new SKU object.
    * Maybe one day we'll have hints about attribute order,
    * but for now it's just totally arbitrary -- whatever the
    * attributes map happens to come up with.
    */
   private void computeToString() {
      cachedToString = computeToString(product,attributes,null);
   }

   public static String describe(String key, String value) {
      if (value.equals("True")) {
         return key;
      } else if (value.equals("False")) {
         return "No " + key;
      } else {
         return value;
      }
      // values True and False are special values that mean the attribute
      // is represented in the server-side UI by a checkbox.
      // actually we're not using this, I just haven't removed the code yet.
   }

   /**
    * Public entry point for callers that want to use an ordered map
    * or a comparator to produce a nonstandard human-readable form.
    */
   public static String computeToString(String product, Map attributes, Comparator comparator) {

      StringBuffer b = new StringBuffer();
      b.append(product);

      Set set = attributes.entrySet();
      Map.Entry[] array = (Map.Entry[]) set.toArray(new Map.Entry[set.size()]);

      if (comparator != null) Arrays.sort(array,new KeyComparator(comparator));

      for (int i=0; i<array.length; i++) {
         b.append(' ');
         b.append(describe((String) array[i].getKey(),(String) array[i].getValue()));
      }

      return b.toString();
   }

   /**
    * Compute the internal string form of the new SKU object.
    */
   private void computeEncode() {

      StringBuffer b = new StringBuffer();
      b.append(encodeString(product));

      Set set = attributes.entrySet();
      Map.Entry[] array = (Map.Entry[]) set.toArray(new Map.Entry[set.size()]);

      // sort the attributes so that we don't get spurious config file diffs;
      // of course we wouldn't see the diff inside LC, only with a diff tool.
      Arrays.sort(array,new KeyComparator(new NoCaseComparator()));

      for (int i=0; i<array.length; i++) {
         Map.Entry entry = array[i];

         b.append(CHAR_DELIM);
         b.append(encodeString((String) entry.getKey()));
         b.append(CHAR_DELIM);
         b.append(encodeString((String) entry.getValue()));
      }

      cachedEncode = b.toString();
   }

   public static class KeyComparator implements Comparator {

      private Comparator comparator;
      public KeyComparator(Comparator comparator) { this.comparator = comparator; }

      public int compare(Object o1, Object o2) {
         String s1 = (String) ((Map.Entry) o1).getKey();
         String s2 = (String) ((Map.Entry) o2).getKey();
         return comparator.compare(s1,s2);
      }
   }
   // why I didn't define KeyAccessor and use FieldComparator, who knows.

// --- object encoding ---

   // these functions could go in a little utility class, but having
   // them here saves us from having product and attribute accessors,
   // at least for now.

   // because of the caching, we can almost do without the encode function,
   // but not quite.  most NewSKU objects come from internal-form strings,
   // but we also get some from order.xml files and from user construction
   // from product and attribute info in the UI.

   public static String encode(NewSKU ns) {
      return ns.cachedEncode;
      // too bad this isn't symmetrical, but it doesn't make sense
      // to have a hash table from NewSKU to String when we can just
      // store the encoded string right on the NewSKU object and get
      // it with no lookup cost.
      // think of computeEncode as being here, that's the best plan.
   }

   private static HashMap decodeTable = new HashMap();
   //
   // a HashMap that takes String -> NewSKU, with the string being
   // an encoded form of the SKU ... not *the* encoded form,
   // because you can permute the attributes and still have a valid
   // form that decodes to the same SKU.
   // although, now that we alphabetize by the attribute group name,
   // we shouldn't see more than one encoded form for any given SKU.

   public static NewSKU decode(String s) throws ValidationException {

      NewSKU ns;
      synchronized (decodeTable) { ns = (NewSKU) decodeTable.get(s); }
      if (ns != null) return ns;
      // I'm not sure we need to synchronize the lookup, but it seems like
      // it could be bad if the table gets modified while we're reading it.
      //
      // I *am* sure we don't need to synchronize the whole function body.
      // with the intern table it was important because we didn't want to
      // create and return two different but equal NewSKU objects,
      // but here the worst that can happen is that we do the same processing twice.
      // and, if we allow overlap, that speeds things up a bit the rest of the time.

      String[] array = s.split(REGEX_DELIM,-1); // -1 to stop weird default behavior
      int i = 0;

      String product = decodeString(array[i++]); // always one element in array

      HashMap attributes = new HashMap();
      for ( ; i+1<array.length; i+=2) attributes.put(decodeString(array[i]),decodeString(array[i+1]));

      if (i != array.length) throw new ValidationException(Text.get(NewSKU.class,"e3"));
      // if we didn't read the last element, the length was even ... that's an error

      ns = get(product,attributes);
      synchronized (decodeTable) { decodeTable.put(s,ns); } // not ns.cachedEncode!
      return ns;
   }

// --- string encoding ---

   // to store a delimited list of strings, we have to escape the delimiter.
   // and, rather than just put an escape character in front, we'll also
   // substitute another character so that the list splitter doesn't have to
   // worry about false hits, it can just split on all delimiter characters.

   private static final char CHAR_ESCAPE = '\\';
   public  static final char CHAR_DELIM = ' ';
   private static final char CHAR_SUBST = '_';

   public  static final String REGEX_DELIM = " ";

   // I really want space to be the delimiter, even though it might someday
   // happen that someone edits one of these by hand, puts in a SKU with
   // a space in it, and gets into trouble.  another likely set was, use '|'
   // as the delimiter and '!' as the substitution.

   public static String encodeString(String trueForm) {
      StringBuffer b = new StringBuffer();

      int base = 0;
      int i;
      int len = trueForm.length(); // could go in loop, but keep parallel
      for (i=0; i<len; i++) {
         char c = trueForm.charAt(i);

         if (c == CHAR_ESCAPE || c == CHAR_DELIM) {

            if (i > base) b.append(trueForm.substring(base,i));
            base = i+1; // go ahead and set this

            b.append(CHAR_ESCAPE);
            b.append( (c == CHAR_ESCAPE) ? CHAR_ESCAPE : CHAR_SUBST );
         }
         // else do nothing, add to base segment
      }

      if (base == 0) return trueForm; // no changes

      if (i > base) b.append(trueForm.substring(base,i));
      // now base=i, but no need to update, we're done

      return b.toString();
   }

   public static String decodeString(String xmlForm) throws ValidationException {
      StringBuffer b = new StringBuffer();

      int base = 0;
      int i;
      int len = xmlForm.length(); // here we need this more than once
      for (i=0; i<len; i++) {
         char c = xmlForm.charAt(i);

         if (c == CHAR_ESCAPE) {

            if (i > base) b.append(xmlForm.substring(base,i));
            base = i+2; // go ahead and set this, even if out of bounds

            if (++i == len) throw new ValidationException(Text.get(NewSKU.class,"e1",new Object[] { xmlForm }));
            char e = xmlForm.charAt(i);

            if      (e == CHAR_ESCAPE) b.append(CHAR_ESCAPE);
            else if (e == CHAR_SUBST ) b.append(CHAR_DELIM );
            else throw new ValidationException(Text.get(NewSKU.class,"e2",new Object[] { xmlForm }));
         }
         // else not an escape sequence, add to base segment
      }

      if (base == 0) return xmlForm; // no changes

      if (i > base) b.append(xmlForm.substring(base,i));
      // now base=i, but no need to update, we're done

      return b.toString();
   }

}

