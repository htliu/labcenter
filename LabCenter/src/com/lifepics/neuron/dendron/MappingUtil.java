/*
 * MappingUtil.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.meta.Accessor;
import com.lifepics.neuron.meta.FieldComparator;
import com.lifepics.neuron.object.XML;
import com.lifepics.neuron.struct.PSKU;
import com.lifepics.neuron.struct.SKU;
import com.lifepics.neuron.struct.StructureDefinition;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import org.w3c.dom.Node;

/**
 * A utility class for common mapping operations.
 */

public class MappingUtil {

// --- find functions ---

   public static Mapping getMapping(LinkedList mappings, SKU sku) {
      Iterator i = mappings.iterator();
      while (i.hasNext()) {
         Mapping m = (Mapping) i.next();
         if (m.getPSKU().matches(sku)) return m;
      }

      // the pattern search order is the list order, which is the order
      // defined by SKUComparator

      return null;
   }

   public static boolean existsMapping(LinkedList mappings, SKU sku) {
      return (getMapping(mappings,sku) != null);
   }

   /**
    * Find a mapping where the SKU is an exact match, without looking
    * at patterns.  This is almost never what you want to do.
    */
   public static Mapping getExactMapping(LinkedList mappings, PSKU psku) {
      Iterator i = mappings.iterator();
      while (i.hasNext()) {
         Mapping m = (Mapping) i.next();
         if (m.getPSKU().equals(psku)) return m;
      }
      return null;
   }

// --- sort functions ---

   public static Accessor skuAccessor = new Accessor() {
      public Class getFieldClass() { return PSKU.class; }
      public Object get(Object o) { return ((Mapping) o).getPSKU(); }
   };

   public static Comparator skuComparator = new FieldComparator(skuAccessor,SKUComparator.displayOrder);

// --- validation ---

   /**
    * For channels, there used to be additional validations on the channel strings,
    * but now that we have mapping objects, this function does complete validation.
    */
   public static void validate(LinkedList mappings) throws ValidationException {

   // validate individual objects

      Iterator i = mappings.iterator();
      while (i.hasNext()) {
         Mapping m = (Mapping) i.next();
         m.validate();
      }

   // prevent duplicate SKUs

      HashSet set = new HashSet();

      i = mappings.iterator();
      while (i.hasNext()) {
         Mapping m = (Mapping) i.next();
         if ( ! set.add(m.getPSKU()) ) throw new ValidationException(Text.get(MappingUtil.class,"e1",new Object[] { m.getPSKU().toString() }));
      }
   }

// --- migration ---

   /**
    * Migrate channels into mappings.  Think of this as loadNormal
    * for the old channel field plus a few lines of migration code.
    */
   public static void migrate(LinkedList mappings, Node node, StructureDefinition sd) throws ValidationException {

      Iterator i = XML.getElements(node,"Channel");
      while (i.hasNext()) {
         Channel c = new Channel();
         Channel.sd.load((Node) i.next(),c);
         Mapping m = (Mapping) sd.construct(); // some Mapping subclass
         m.migrate(c);
         mappings.add(m);
      }
      // could reuse channel object, but it's not worth worrying about
   }

}

