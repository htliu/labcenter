/*
 * ProductConfig.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.object.CopyUtil;
import com.lifepics.neuron.object.XML;
import com.lifepics.neuron.struct.*;

import java.util.Iterator;
import java.util.LinkedList;

import org.w3c.dom.Node;

/**
 * An object that holds product and attribute definitions.
 * The definitions come from the server, and are not editable in LabCenter.
 */

public class ProductConfig extends Structure {

// --- fields ---

   public LinkedList productsOld;
   public LinkedList productsNew;
   public LinkedList conversions;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      ProductConfig.class,
      0,0,
      new AbstractField[] {

         new StructureListField("productsOld","ProductOld",Product.sd,Merge.IDENTITY).with(Product.idAccessor,Product.skuComparator),
         new StructureListField("productsNew","ProductNew",NewProduct.sd,Merge.IDENTITY).with(NewProduct.productIDAccessor,NewProduct.standardComparator),
         new StructureListField("conversions","Conversion",Conversion.sd,Merge.IDENTITY).with(Conversion.newSKUAccessor,Conversion.newSKUComparator)
      });

   protected StructureDefinition sd() { return sd; }

// --- copy function ---

   public ProductConfig copy() { return (ProductConfig) CopyUtil.copy(this); }

// --- validation ---

   public void validate() throws ValidationException {
      Product   .validate(productsOld);
      NewProduct.validateProductList(productsNew);
      Conversion.validate(conversions);
   }

// --- iteration ---

   public void iterate(ProductCallback callback) {
      Product   .iterate(productsOld,callback);
      NewProduct.iterate(productsNew,callback);
   }

// --- persistence ---

   public Object loadDefault(Node node) throws ValidationException {
      loadDefault();

      // read old products from their old home on QueueList.
      // cf. StructureList.loadNormal and MappingUtil.migrate, among other things.

      Iterator i = XML.getElements(node,"Product");
      while (i.hasNext()) {
         productsOld.add(new Product().load((Node) i.next()));
      }

      return this; // convenience
   }

}

