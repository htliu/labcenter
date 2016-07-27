/*
 * Product.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.meta.Accessor;
import com.lifepics.neuron.meta.FieldComparator;
import com.lifepics.neuron.meta.NoCaseComparator;
import com.lifepics.neuron.struct.*;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * An object that contains product information from the server.
 */

public class Product extends Structure {

// --- fields ---

   // I use the SKU as the primary key, but I thought the productID should come first.

   public String productID; // really an integer, but to us it's just a token
   public String sku;
   public String description;
   public Long dueInterval; // millis ; nullable

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      Product.class,
      // no version
      new AbstractField[] {

         new StringField("productID","ProductID"),
         new StringField("sku","SKU"),
         new StringField("description","Description"),
         new NullableLongField("dueInterval","DueInterval-Millis")
      });

   protected StructureDefinition sd() { return sd; }

// --- find functions ---

   public static Product findProductByID(LinkedList products, String productID) {
      Iterator i = products.iterator();
      while (i.hasNext()) {
         Product p = (Product) i.next();
         if (p.productID.equals(productID)) return p;
      }
      return null;
   }

   public static Product findProductBySKU(LinkedList products, String sku) {
      Iterator i = products.iterator();
      while (i.hasNext()) {
         Product p = (Product) i.next();
         if (p.sku.equals(sku)) return p;
      }
      return null;
   }

// --- sort functions ---

   public static Accessor idAccessor = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return ((Product) o).productID; }
   };

   public static Accessor skuAccessor = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return ((Product) o).sku; }
   };

   public static Comparator skuComparator = new FieldComparator(skuAccessor,new NoCaseComparator());

// --- validation ---

   public static void validate(LinkedList products) throws ValidationException {

      HashSet ids  = new HashSet();
      HashSet skus = new HashSet();

      Iterator i = products.iterator();
      while (i.hasNext()) {
         Product p = (Product) i.next();

         p.validate();

         if ( ! ids .add(p.productID) ) throw new ValidationException(Text.get(Product.class,"e3",new Object[] { p.productID }));
         if ( ! skus.add(p.sku) ) throw new ValidationException(Text.get(Product.class,"e1",new Object[] { p.sku }));
      }
   }

   public void validate() throws ValidationException {
      if (dueInterval != null && dueInterval.longValue() < 1) throw new ValidationException(Text.get(this,"e4"));
   }

// --- iteration ---

   public static void iterate(LinkedList products, ProductCallback callback) {
      Iterator i = products.iterator();
      while (i.hasNext()) {
         iterate((Product) i.next(),callback);
      }
   }

   public static void iterate(Product product, ProductCallback callback) {
      callback.f(new OldSKU(product.sku),product.description,product.dueInterval);
   }

}

