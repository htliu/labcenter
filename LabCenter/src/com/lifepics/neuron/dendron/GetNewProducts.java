/*
 * GetNewProducts.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.net.GetTransaction;
import com.lifepics.neuron.net.MerchantConfig;
import com.lifepics.neuron.net.Query;
import com.lifepics.neuron.object.XML;
import com.lifepics.neuron.struct.NewSKU;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * A transaction that gets a list of {@link NewProduct} from the server.
 */

public class GetNewProducts extends GetTransaction {

   private String url;
   private MerchantConfig merchantConfig;
   private LinkedList result;

   public GetNewProducts(String url, MerchantConfig merchantConfig, LinkedList result) {
      this.url = url;
      this.merchantConfig = merchantConfig;
      this.result = result;
   }

   public String describe() { return Text.get(this,"s1"); }
   protected String getFixedURL() { return url; }
   protected void getParameters(Query query) throws IOException {
      if (merchantConfig.isWholesale) {
         query.add("wholesalerID",Convert.fromInt(merchantConfig.merchant));
      } else {
         query.add("mlrfnbr",Convert.fromInt(merchantConfig.merchant));
      }
      query.addPasswordObfuscate("encpassword",merchantConfig.password);
   }

   protected boolean receive(InputStream inputStream) throws Exception {

      result.clear(); // in case we're retrying

      Document doc = XML.readStream(inputStream);
      Node list = XML.getElement(doc,"ProductList");

      Iterator i = XML.getElements(list,"Product");
      while (i.hasNext()) {
         Node node = (Node) i.next();

         NewProduct p = new NewProduct();

         p.productID = Convert.toInt(XML.getElementText(node,"ProductID"));
         p.productCode = XML.getElementText(node,"ProductSku");
         p.description = XML.getElementText(node,"Description");
         p.dueInterval = getDueInterval(node);
         receiveGroups(p.groups,XML.getElement(node,"Groups"));

         Node temp = XML.getElementTry(node,"AttributeSets");
         if (temp != null) receiveAdjustments(p.adjustments,temp,p.productCode);

         result.add(p);
      }

      Collections.sort(result,NewProduct.standardComparator);

      NewProduct.validateProductList(result); // see comment in GetProducts
      return true;
   }

   private static void receiveGroups(LinkedList groups, Node list) throws Exception {

      Iterator i = XML.getElements(list,"Group");
      while (i.hasNext()) {
         Node node = (Node) i.next();

         NewProduct.Group g = new NewProduct.Group();

         g.groupID = Convert.toInt(XML.getElementText(node,"GroupID"));
         g.groupName = XML.getElementText(node,"GroupName");
         g.optional = toBoolean(XML.getNullableText(node,"Optional"));
         receiveAttributes(g.attributes,XML.getElement(node,"Attributes"));

         // for the optional flag, null means the same as false, but the LC field
         // is nullable for other reasons, so let's keep the original information

         groups.add(g);
      }

      Collections.sort(groups,NewProduct.groupNameComparator);
   }

   private static Boolean toBoolean(String s) throws ValidationException {
      if      (s == null        ) return null; // must come first
      else if (s.equals("True" )) return Boolean.TRUE;
      else if (s.equals("False")) return Boolean.FALSE;
      else throw new ValidationException(Text.get(GetNewProducts.class,"e1",new Object[] { s }));
   }
   // this is a one-way conversion, no need to define constants

   private static void receiveAttributes(LinkedList attributes, Node list) throws Exception {

      Iterator i = XML.getElements(list,"Attribute");
      while (i.hasNext()) {
         Node node = (Node) i.next();

         NewProduct.Attribute a = new NewProduct.Attribute();

         a.attributeID = Convert.toInt(XML.getElementText(node,"AttributeID"));
         a.value = XML.getElementText(node,"Value");

         addAttribute(attributes,a); // basically attributes.add(a)
      }

      Collections.sort(attributes,NewProduct.valueComparator);
   }

   // here and in GetProducts we remove duplicates at add time,
   // but in GetConversions we do it in a second pass afterward.
   // why?  I'm not sure ... best guess, the second pass one
   // is complicated by having a hash set, and I didn't want to
   // mix that into the existing code.
   //
   // note that the number of attributes is small, so we don't
   // need to do any hash set optimizations.
   //
   private static void addAttribute(LinkedList attributes, NewProduct.Attribute a) {

      NewProduct.Attribute b = NewProduct.findAttributeByValue(attributes,a.value);

      if (b == null) { // no duplicate
         attributes.add(a);
      } else if (b.attributeID <= a.attributeID) { // keep old one
         // just don't add new one
      } else { // keep new one
         attributes.remove(b); // inefficient second pass, but rare
         attributes.add(a);
      }

      // as in GetProducts, keep the one with the lower ID, for stability.
      // we shouldn't have equal IDs, but don't bother making it an error.
   }

   private static void receiveAdjustments(LinkedList adjustments, Node list, String productCode) throws Exception {

      Iterator i = XML.getElements(list,"AttributeSet");
      while (i.hasNext()) {
         Node node = (Node) i.next();

         NewProduct.Adjustment a = new NewProduct.Adjustment();

         a.psku = NewSKU.get(productCode,SKUParser.parseAttributeList(node));
         a.dueInterval = getDueInterval(node);

         adjustments.add(a);
      }

      Collections.sort(adjustments,MappingUtil.skuComparator);
   }

   private static Long getDueInterval(Node node) throws Exception {
      String temp = XML.getNullableText(node,"TurnAroundTime"); // minutes
      return (temp == null) ? null : new Long(Convert.toLong(temp) * 60000);
   }

}

