/*
 * NewProduct.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.meta.Accessor;
import com.lifepics.neuron.meta.CompoundComparator;
import com.lifepics.neuron.meta.FieldComparator;
import com.lifepics.neuron.meta.NaturalComparator;
import com.lifepics.neuron.meta.NoCaseComparator;
import com.lifepics.neuron.struct.*;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * An object that contains product and attribute information from the server.
 */

public class NewProduct extends Structure {

// --- sort functions ---

   // must come first to prevent forward reference problem

   public static Accessor productIDAccessor = new Accessor() {
      public Class getFieldClass() { return Integer.class; }
      public Object get(Object o) { return new Integer( ((NewProduct) o).productID ); }
   };

   public static Comparator productIDComparator = new FieldComparator(productIDAccessor,new NaturalComparator());

   public static Accessor productCodeAccessor = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return ((NewProduct) o).productCode; }
   };

   public static Comparator productCodeComparator = new FieldComparator(productCodeAccessor,new NoCaseComparator());
   public static Comparator standardComparator = new CompoundComparator(productCodeComparator,productIDComparator);

   public static Accessor groupIDAccessor = new Accessor() {
      public Class getFieldClass() { return Integer.class; }
      public Object get(Object o) { return new Integer( ((Group) o).groupID ); }
   };

   public static Accessor groupNameAccessor = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return ((Group) o).groupName; }
   };

   public static Comparator groupNameComparator = new FieldComparator(groupNameAccessor,new NoCaseComparator());

   public static Accessor attributeIDAccessor = new Accessor() {
      public Class getFieldClass() { return Integer.class; }
      public Object get(Object o) { return new Integer( ((Attribute) o).attributeID ); }
   };

   public static Accessor valueAccessor = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return ((Attribute) o).value; }
   };

   public static Comparator valueComparator = new FieldComparator(valueAccessor,new NoCaseComparator());

// --- product class ---

   // indented for symmetry with the other classes

      public int productID;
      public String productCode; // the first part of the SKU
      public String description;
      public Long dueInterval; // millis ; nullable
      public LinkedList groups;
      public LinkedList adjustments;

      public static final StructureDefinition sd = new StructureDefinition(

         NewProduct.class,
         // no version
         new AbstractField[] {

            new IntegerField("productID","ProductID"),
            new StringField("productCode","ProductCode"),
            new StringField("description","Description"),
            new NullableLongField("dueInterval","DueInterval-Millis"),
            new StructureListField("groups","Group",Group.sd,Merge.IDENTITY).with(groupIDAccessor,groupNameComparator),
            new StructureListField("adjustments","Adjustment",Adjustment.sd,Merge.IDENTITY).with(MappingUtil.skuAccessor,MappingUtil.skuComparator)
         });

      protected StructureDefinition sd() { return sd; }

      public void validate() throws ValidationException { validateProduct(this); }

// --- group class ---

   public static class Group extends Structure {

      public int groupID;
      public String groupName;
      public Boolean optional;
      public LinkedList attributes;

      public static final StructureDefinition sd = new StructureDefinition(

         Group.class,
         // no version
         new AbstractField[] {

            new IntegerField("groupID","GroupID"),
            new StringField("groupName","GroupName"),
            new NullableBooleanField("optional","Optional"),
            new StructureListField("attributes","Attribute",Attribute.sd,Merge.IDENTITY).with(attributeIDAccessor,valueComparator)
         });

      protected StructureDefinition sd() { return sd; }

      public boolean isOptional() {
         return (optional != null) ? optional.booleanValue() : false;
      }

      public void validate() throws ValidationException { validateGroup(this); }
   }

// --- attribute class ---

   public static class Attribute extends Structure {

      public int attributeID;
      public String value;

      public static final StructureDefinition sd = new StructureDefinition(

         Attribute.class,
         // no version
         new AbstractField[] {

            new IntegerField("attributeID","AttributeID"),
            new StringField("value","Value")
         });

      protected StructureDefinition sd() { return sd; }

      public void validate() throws ValidationException { validateAttribute(this); }
   }

// --- adjustment class ---

   // adjustments to the due interval for specific attribute combinations.
   // on the server side, these tie to the location-specific settings,
   // but in LC we're just going to show all combinations and then adjust
   // the due interval where necessary.

   // this isn't the normal use of the Mapping class, but it's helpful

   public static class Adjustment extends Structure implements Mapping {

      public PSKU getPSKU() { return psku; }
      public void setPSKU(PSKU psku) { this.psku = psku; }

      public PSKU psku; // currently always a new SKU, the PSKU part is just there so Mapping works
      public Long dueInterval; // null here means "override to null"

      public static final StructureDefinition sd = new StructureDefinition(

         Adjustment.class,
         // no version
         new AbstractField[] {

            new PSKUField("psku","A","SKU","Rule"),
            new NullableLongField("dueInterval","DueInterval-Millis")
         });

      protected StructureDefinition sd() { return sd; }

      public void validate() throws ValidationException { validateAdjustment(this); }
      public void migrate(Channel c) throws ValidationException {}
   }

   static { Adjustment.sd.setAttributed(); }

// --- real validation chain ---

   // the standard validate functions on the objects are probably never called,
   // instead the list owner calls validateProductList and it goes from there.

   public static void validateProductList(LinkedList products) throws ValidationException {

   // product objects must be valid, and product IDs must be unique

      HashSet ids = new HashSet();

      Iterator i = products.iterator();
      while (i.hasNext()) {
         NewProduct p = (NewProduct) i.next();

         validateProduct(p);

         if ( ! ids.add(new Integer(p.productID)) ) throw new ValidationException(Text.get(NewProduct.class,"e1",new Object[] { Convert.fromInt(p.productID) }));
      }

   // product codes don't need to be unique any more,
   // but duplicates can't produce duplicate NewSKUs

      final HashSet skus = new HashSet();
      final HashSet dupl = new HashSet();

      // probably a good idea not to iterate until we've passed all other validations!

      iterate(products,new ProductCallback() { public void f(SKU sku, String description, Long dueInterval) {
         if ( ! skus.add(sku) ) dupl.add(sku);
         // can't just throw exception here because we're inside iterator scope
      } });

      int count = dupl.size();
      if (count > 0) {
         SKU sku = (SKU) dupl.iterator().next(); // easiest way to grab an element
         throw new ValidationException(Text.get(NewProduct.class,"e2",new Object[] { new Integer(count), Convert.fromInt(count), sku.toString() }));
      }
   }

   private static void validateProduct(NewProduct p) throws ValidationException {
      if (p.dueInterval != null && p.dueInterval.longValue() < 1) throw new ValidationException(Text.get(NewProduct.class,"e3"));
      validateGroupList(p.groups);
      validateAdjustmentList(p.adjustments);
   }

   private static void validateGroupList(LinkedList groups) throws ValidationException {
      // groupID and groupName must be unique within a product.
      // a group can be used on multiple products, I think, so we can't require global uniqueness.

      HashSet ids = new HashSet();
      HashSet names = new HashSet();

      Iterator i = groups.iterator();
      while (i.hasNext()) {
         Group g = (Group) i.next();

         validateGroup(g);

         if ( ! ids.add(new Integer(g.groupID)) ) throw new ValidationException(Text.get(NewProduct.class,"e4",new Object[] { Convert.fromInt(g.groupID) }));
         if ( ! names.add(g.groupName) ) throw new ValidationException(Text.get(NewProduct.class,"e5",new Object[] { g.groupName }));
      }
   }

   private static void validateGroup(Group g) throws ValidationException {
      validateAttributeList(g.attributes);
   }

   private static void validateAttributeList(LinkedList attributes) throws ValidationException {
      // attributeID and value must be unique within a group.
      // I think they should also be unique across groups within a product, but I don't need that,
      // so let's not require it.  as discussed above, groups can reappear across products, so so
      // can attributes.

      HashSet ids = new HashSet();
      HashSet values = new HashSet();

      Iterator i = attributes.iterator();
      while (i.hasNext()) {
         Attribute a = (Attribute) i.next();

         validateAttribute(a);

         if ( ! ids.add(new Integer(a.attributeID)) ) throw new ValidationException(Text.get(NewProduct.class,"e6",new Object[] { Convert.fromInt(a.attributeID) }));
         if ( ! values.add(a.value) ) throw new ValidationException(Text.get(NewProduct.class,"e7",new Object[] { a.value }));
      }
   }

   private static void validateAttribute(Attribute a) throws ValidationException {
   }

   private static void validateAdjustmentList(LinkedList adjustments) throws ValidationException {
      // these are the same validations that MappingUtil.validate would do,
      // but let's spell it out to keep it parallel with the other classes.

      HashSet skus = new HashSet();

      Iterator i = adjustments.iterator();
      while (i.hasNext()) {
         Adjustment a = (Adjustment) i.next();

         validateAdjustment(a);

         if ( ! skus.add(a.psku) ) throw new ValidationException(Text.get(NewProduct.class,"e8",new Object[] { a.psku.toString() }));
      }
   }

   private static void validateAdjustment(Adjustment a) throws ValidationException {

      // could check product code matches SKU, but it's not important,
      // and right now the code is completely encapsulated in NewSKU.
      // also, this way we don't even check or require old vs. new SKU.

      if (a.dueInterval != null && a.dueInterval.longValue() < 1) throw new ValidationException(Text.get(NewProduct.class,"e9"));
   }

// --- iteration ---

   public static void iterate(LinkedList products, ProductCallback callback) {
      RawImpl impl = new RawImpl(callback);
      Iterator i = products.iterator();
      while (i.hasNext()) {
         iterate((NewProduct) i.next(),impl);
      }
   }

   public static void iterate(NewProduct product, ProductCallback callback) {
      iterate(product,new RawImpl(callback));
   }

   public static void iterate(NewProduct product, RawCallback callback) {

      Record r = new Record();
      r.product = product;
      r.groupIndex = 0;
      r.groupLimit = product.groups.size();
      r.attributes = new HashMap();
      r.callback = callback;

      r.iterate();
   }

   public interface RawCallback {
      void f(String productCode, HashMap attributes, SKU sku, String description, Long dueInterval);
   }

   private static class RawImpl implements RawCallback {

      private ProductCallback callback;
      public RawImpl(ProductCallback callback) { this.callback = callback; }

      public void f(String productCode, HashMap attributes, SKU sku, String description, Long dueInterval) {
         callback.f(sku,description,dueInterval);
      }
   }

   private static class Record {

      private NewProduct product;
      private int groupIndex;
      private int groupLimit;
      private HashMap attributes;
      private RawCallback callback;

      public void iterate() {

         // we can store the attributes map outside the recursion
         // because every group name will be set in the end.
         // actually, that's not true any more; now we have to be
         // careful to clear the optional group names (*)

         if (groupIndex == groupLimit) {

            SKU sku = NewSKU.get(product.productCode,attributes);

            Adjustment a = (Adjustment) MappingUtil.getMapping(product.adjustments,sku);
            // list will almost never have entries, linear search is fine
            Long dueInterval = (a != null) ? a.dueInterval : product.dueInterval;
            // adjustment can override non-null to null, that's correct.
            // if you don't want that, don't make an adjustment record in the first place.

            callback.f(product.productCode,attributes,sku,product.description,dueInterval);
            return;
         }
         // you'd think you could put this test inside the iteration,
         // but then products with no attributes wouldn't work right

         Group g = (Group) product.groups.get(groupIndex);
         groupIndex++;

         if (g.isOptional()) {
            attributes.remove(g.groupName);  // (*) must actively remove due to recursion
            iterate();
         }

         Iterator i = g.attributes.iterator();
         while (i.hasNext()) {
            Attribute a = (Attribute) i.next();
            attributes.put(g.groupName,a.value);
            iterate();
         }

         groupIndex--;
      }
   }

// --- find functions ---

   // we happen not to need findProductByCode, but it could go here ...
   // except that productCode is no longer a unique identifier!

   public static NewProduct findProductByID(LinkedList products, int productID) {
      Iterator i = products.iterator();
      while (i.hasNext()) {
         NewProduct p = (NewProduct) i.next();
         if (p.productID == productID) return p;
      }
      return null;
   }

   public static Group findAndRemoveGroupByName(LinkedList groups, String groupName) {
      Iterator i = groups.iterator();
      while (i.hasNext()) {
         Group g = (Group) i.next();
         if (g.groupName.equals(groupName)) { i.remove(); return g; }
      }
      return null;
   }

   public static Group findAndRemoveGroupByID(LinkedList groups, int groupID) {
      Iterator i = groups.iterator();
      while (i.hasNext()) {
         Group g = (Group) i.next();
         if (g.groupID == groupID) { i.remove(); return g; }
      }
      return null;
   }

   public static Attribute findAttributeByValue(LinkedList attributes, String value) {
      Iterator i = attributes.iterator();
      while (i.hasNext()) {
         Attribute a = (Attribute) i.next();
         if (a.value.equals(value)) return a;
      }
      return null;
   }

   public static Attribute findAttributeByID(LinkedList attributes, int attributeID) {
      Iterator i = attributes.iterator();
      while (i.hasNext()) {
         Attribute a = (Attribute) i.next();
         if (a.attributeID == attributeID) return a;
      }
      return null;
   }

}

