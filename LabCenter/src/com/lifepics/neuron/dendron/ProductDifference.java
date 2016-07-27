/*
 * ProductDifference.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.struct.NewSKU;
import com.lifepics.neuron.struct.OldSKU;
import com.lifepics.neuron.struct.SKU;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

/**
 * A utility class for taking the difference of {@link ProductConfig} objects.
 */

public class ProductDifference {

// --- overview ---

   // the idea here is, we have some product config that we're using,
   // and it defines some set of SKU codes.
   // when we receive another config, we can diff it with the first
   // and see which SKUs are new.  furthermore, for each new SKU,
   // we can decide whether it should transfer from an existing SKU
   // or whether it's something we've really never seen before.
   //
   // then, when we store the new product config, we also make the
   // changes implied by the diff, and everything stays consistent.
   //
   // the conversion objects change that model a little, because they
   // can cause transfers into existing SKUs.  ideally we'd receive
   // new conversions at exactly the same time as the corresponding
   // products, but we can't count on that.  unless you turn on old SKUs,
   // this is the only place where {@link Conversion} objects are used!
   //
   // the behavior isn't going to be mathematically perfect, because
   // for example the config might change and then change back, and
   // the results in LC will be different depending on whether or not
   // it noticed the middle state.  but, it's still pretty good!
   //
   // another example, using old products: if you rename a product,
   // LC can match it by ID, and if you change the ID but keep the
   // SKU the same, LC can match it that way, but if you do both
   // and LC doesn't see the middle state, there's just no way for
   // LC to figure it out.

// --- fields ---

   // the counts are of SKUs, not products, since SKUs are what's visible

   public int countTransferAuto;
   public int countTransferConv;
   public int countTransferOverwrite;
   public int countCreate;
   public int countCreateOverwrite;

   public HashMap skusTransfer; // map from new to old (*1)
   public HashSet skusCreate;

   // |skusTransfer| = countTransferAuto + countTransferConv - countTransferOverwrite
   // |skusCreate  | = countCreate - countCreateOverwrite
   // leave it to the caller to decide how to report counts to the user;
   // here we'll just provide complete information

// --- code ---

   public static ProductDifference diff(ProductConfig before, ProductConfig after) {

      // don't call the arguments old and new, we already have productsOld and productsNew

      ProductDifference d = new ProductDifference();

      d.diffProductsOld(before.productsOld,after.productsOld);
      d.diffProductsNew(before.productsNew,after.productsNew);
      d.diffConversions(before.conversions,after.conversions); // order matters (*2)

      return d;
   }

   private ProductDifference() {

      countTransferAuto = 0;
      countTransferConv = 0;
      countTransferOverwrite = 0;
      countCreate = 0;
      countCreateOverwrite = 0;

      skusTransfer = new HashMap();
      skusCreate = new HashSet();
   }

// --- old products ---

   // the idea behind loading the products and conversions into hash sets and maps
   // is that there might be a fairly large number of them.
   // so, it's worth converting the O(N^2) linear search into a faster hash lookup.
   // for groups and attributes it's not worth the trouble.
   // also it's not worth making a second map by ID, since mostly they should match.

   // a couple of notes about old products, that might help clarify
   // the situation for new products.
   // if an after-sku matches a before-sku, we never transfer into it,
   // period.  it's possible that we'll transfer out of it, if e.g.
   // we go from (1,4x6) to (1,5x7) (2,4x6) ... in that case we'll copy
   // the 4x6 settings to make 5x7 settings, but 4x6 remains valid.

   private void diffProductsOld(LinkedList before, LinkedList after) {

   // load

      HashSet set = new HashSet(); // set of String not SKU!

      Iterator i = before.iterator();
      while (i.hasNext()) {
         Product pb = (Product) i.next();

         set.add(pb.sku);
      }

   // scan

      i = after.iterator();
      while (i.hasNext()) {
         Product pa = (Product) i.next();

         if (set.add(pa.sku)) { // new SKU code?

            Product pb = Product.findProductByID(before,pa.productID);
            if (pb != null) {

               countTransferAuto++;
               skusTransfer.put(new OldSKU(pa.sku),new OldSKU(pb.sku));

            } else {

               countCreate++;
               skusCreate.add(new OldSKU(pa.sku));
            }
         }
      }
   }

// --- new products, part 1 ---

   // note about duplicate product codes and difference calculations:
   //
   // the one weird thing about duplicate product codes is that
   // if the config changes in such a way as to move a SKU from
   // one product to another, the diff may not be what you'd
   // expect as a human, because the change counts as a difference
   // even though the same SKU existed both before and after.
   //
   // example 1: say you have a 4x6 with one attribute, values ABC,
   // and another 4x6 with values DEF.  now the config changes to
   // have AB and CDEF ... "4x6 C" will show as a newly-created SKU.
   // not totally unreasonable, but a bit weird.
   //
   // example 2: same thing, but you remove C and rename D to C.
   // in that case you'll see a transfer from "4x6 D" to "4x6 C".
   //
   // one point of non-weirdness: every SKU that's created, or that
   // is the destination of a transfer, is a SKU that appears in
   // the "after" product list ... and, by validation, we know that
   // those SKUs are unique even though the codes may not be.
   // so, if a SKU exists in the "before" list and continues to exist,
   // there can't be create or transfer report for it.
   //
   // another point of non-weirdness: when we find a difference,
   // everything that we do is along the lines of "fill this in if
   // it's not already filled in".  so, if everything is already
   // filled in, no problem.  the one exception to that rule is the
   // JPEG SKU setting, which as a single value rather than a list
   // has to behave differently.  so, in example 2, if you renamed
   // C to D at the same time, the JPEG SKU would change even though
   // the full SKU set hadn't.  not unreasonable, though.

   private void diffProductsNew(LinkedList before, LinkedList after) {

   // load

      HashMap map = new HashMap();
      HashSet dupl = new HashSet();

      Iterator i = before.iterator();
      while (i.hasNext()) {
         NewProduct pb = (NewProduct) i.next();

         if (map.put(pb.productCode,pb) != null) dupl.add(pb.productCode);
         // can't remove duplicates right away in case there are more
      }

      // remove entries for duplicates so they can't match by product code
      i = dupl.iterator();
      while (i.hasNext()) map.remove(i.next());

   // scan

      i = after.iterator();
      while (i.hasNext()) {
         NewProduct pa = (NewProduct) i.next();

         NewProduct pb = (NewProduct) map.get(pa.productCode);
         if (pb == null) {
            pb = NewProduct.findProductByID(before,pa.productID);
         }

         // if we have a candidate, see if it's a usable match.
         //
         // note, it's possible we'll get the candidate by SKU
         // and fail to match, and not notice that there's
         // a candidate by ID that does match.  I think that's
         // correct behavior .. SKU takes precedence over ID.
         //
         ProductDelta pd = null;
         if (pb != null) pd = makeProductDelta(pb,pa);

         if (pd != null) { // match, mark SKUs in various ways

            if (pd.hasChange()) NewProduct.iterate(pa,pd);
            // else total match, nothing to report

         } else { // no match, mark all SKUs created

            NewProduct.iterate(pa,new ProductCallback() { public void f(SKU sku, String description, Long dueInterval) {
               countCreate++;
               skusCreate.add(sku);
            } });
         }
      }
   }

// --- conversions ---

   private void diffConversions(LinkedList before, LinkedList after) {

   // load

      HashMap map = new HashMap();

      Iterator i = before.iterator();
      while (i.hasNext()) {
         Conversion cb = (Conversion) i.next();

         map.put(cb.newSKU,cb);
         // no collisions, newSKU is validated unique
      }

   // scan

      i = after.iterator();
      while (i.hasNext()) {
         Conversion ca = (Conversion) i.next();

         Conversion cb = (Conversion) map.get(ca.newSKU);
         if (cb == null || ! cb.oldSKU.equals(ca.oldSKU)) {

            // new conversion, we should apply it
            countTransferConv++;
            Object o = skusTransfer.put(ca.newSKU,ca.oldSKU); // (*3)

            if (o != null) countTransferOverwrite++;

            // also we have to check skusCreate
            if (skusCreate.remove(ca.newSKU)) countCreateOverwrite++;
         }
      }

      // (*1)(*2)(*3) skusTransfer is a map from new to old,
      // and we do this after we've already entered any
      // transfers from the new products, so, conversions
      // will take precedence over product redefinitions.
      // that's a bit iffy, but I think it's correct.
      //
      // the points to keep in mind are ...
      //
      // one, it's only *new* conversions that take precedence.
      // normally, the conversions will just be sitting around,
      // and redefinitions will go through with no problem.
      //
      // two, conversions aren't guaranteed to arrive at the
      // same time as the associated products.  we might receive
      // new product A, but no conversion, then later have
      // someone go in and fix it by renaming to B and adding
      // a conversion for it.
      //
      // to put it another way, product redefinitions are all
      // in LC's imagination, while conversions are created
      // by hand by actual people who want something to happen.
   }

// --- new products, part 2 ---

   // the way to think about this is, a ProductDelta is a thing
   // that can be used to transform SKUs from new to old.
   // so, all we have to do is build one of them, and we're set.
   // also we can ask it whether it has any changes to apply,
   // and skip the product iteration in the common matched case.

   // I present GroupDelta first, since it's small.

   private static class GroupDelta {

      public String groupName; // null if no change
      public boolean  optionalDelta;
      public HashMap attributeDelta; // indexed by new attribute value; no entry if no change
         // if the value is created instead of unchanged, it maps to Object instead of String

      public GroupDelta() {
         attributeDelta = new HashMap();
      }

      public boolean hasChange() {
         return (groupName != null || optionalDelta || attributeDelta.size() > 0);
      }
   }

   /**
    * A class that describes how a NewProduct has changed,
    * and can make the corresponding changes to SKU codes.
    */
   private class ProductDelta implements NewProduct.RawCallback {
      // not static, because f writes into difference object

      public String productCode; // null if no change
      public HashMap groupDelta; // indexed by new group name; entries for all groups

      public ProductDelta() {
         groupDelta = new HashMap();
      }

      public boolean hasChange() {

         if (productCode != null) return true;

         Iterator i = groupDelta.values().iterator();
         while (i.hasNext()) {
            if (((GroupDelta) i.next()).hasChange()) return true;
         }

         return false;
      }

      public void f(String productCode, HashMap attributes, SKU sku, String description, Long dueInterval) {

         boolean delta  = false;
         boolean create = false;

         String useCode = productCode;
         if (this.productCode != null) { useCode = this.productCode; delta = true; }

         HashMap useAttributes = new HashMap();

         Iterator i = groupDelta.entrySet().iterator();
         while (i.hasNext()) {
            Map.Entry entry = (Map.Entry) i.next();

            String groupName = (String) entry.getKey();
            GroupDelta d = (GroupDelta) entry.getValue();

            String value = (String) attributes.get(groupName);

            if (value == null) { // absent optional attribute
               if (d.optionalDelta) create = true;
               continue;
            }
            // this test comes before the name test, since if the attribute is absent
            // the group name doesn't even appear in the SKU.
            // we also want to skip the call to useAttributes.put.

            if (d.groupName != null) { groupName = d.groupName; delta = true; }

            Object o = d.attributeDelta.get(value);
            if (o == null) {
               // no change
            } else if ( ! (o instanceof String) ) {
               // value is totally new, keep it the same but set the create flag
               create = true;
            } else {
               value = (String) o;
               delta = true;
            }

            useAttributes.put(groupName,value);
         }

         if (create) {

            countCreate++;
            skusCreate.add(sku);

         } else if (delta) {

            countTransferAuto++;
            skusTransfer.put(sku,NewSKU.get(useCode,useAttributes));

         } else {

            // the product has changed, since we're running this code,
            // but the particular SKU hasn't ...
            // this happens when the product code and group names are unchanged
            // but some attributes have been added or renamed.
         }
      }
   }

// --- new products, part 3 ---

   // basically, we construct the delta by matching the before-groups
   // to the after-groups and then comparing attributes within groups.
   // the groups must match with no leftovers, but the attributes can
   // have leftovers, and can leave unmatched ones on the before-side.

   /**
    * @return A delta record for the products, or null if they don't match.
    */
   private ProductDelta makeProductDelta(NewProduct pb, NewProduct pa) {

      // the groups must match with no leftovers, which gives us an easy precondition
      if (pb.groups.size() != pa.groups.size()) return null;

      ProductDelta d = new ProductDelta();

      if ( ! pb.productCode.equals(pa.productCode) ) d.productCode = pb.productCode;
      // we could remember whether we'd found the before-product by ID
      // or by product code, but it's easier to just (re)do the string comparison

      LinkedList groupsBefore = new LinkedList(pb.groups); // copy so we can remove stuff

      Iterator i = pa.groups.iterator();
      while (i.hasNext()) {
         NewProduct.Group ga = (NewProduct.Group) i.next();

         NewProduct.Group gb = NewProduct.findAndRemoveGroupByName(groupsBefore,ga.groupName);
         if (gb == null) {
            gb = NewProduct.findAndRemoveGroupByID(groupsBefore,ga.groupID);
         }

         if (gb == null) return null; // no match, game over

         d.groupDelta.put(ga.groupName,makeGroupDelta(gb,ga));
      }

      return d;
   }

   private static GroupDelta makeGroupDelta(NewProduct.Group gb, NewProduct.Group ga) {

      GroupDelta d = new GroupDelta();

      if ( ! gb.groupName.equals(ga.groupName) ) d.groupName = gb.groupName;
      // again, we could remember this, but it's not worth the trouble

      d.optionalDelta = ga.isOptional() && ! gb.isOptional();
      //
      // when a group is optional, the absence of the group is like another
      // allowed attribute value, but one that can't be renamed or match to
      // any other attribute.  so, the only time we're going to get a delta
      // is when the optional flag changes to true.

      // we don't need to remove from the list here.  if we go from (1,Glossy)
      // to (1,Luster) (2,Glossy), we'll see no change for value Glossy
      // and also see that Luster should transfer from Glossy.  it's weird, but
      // there's no harm in it, and it probably won't happen anyway.
      // the important question is, given an after-value, can we find a before-
      // value that's a plausible match for it?

      Iterator i = ga.attributes.iterator();
      while (i.hasNext()) {
         NewProduct.Attribute aa = (NewProduct.Attribute) i.next();

         NewProduct.Attribute ab = NewProduct.findAttributeByValue(gb.attributes,aa.value);
         if (ab == null) {
            ab = NewProduct.findAttributeByID(gb.attributes,aa.attributeID);
         }

         if (ab == null) { // no match, so new value
            d.attributeDelta.put(aa.value,new Object());
         } else if (ab.value.equals(aa.value)) {
            // no change
         } else {
            d.attributeDelta.put(aa.value,ab.value);
         }
      }

      return d;
   }

}

