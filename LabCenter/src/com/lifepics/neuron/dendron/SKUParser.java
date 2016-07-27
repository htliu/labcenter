/*
 * SKUParser.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.object.XML;
import com.lifepics.neuron.struct.NewSKU;
import com.lifepics.neuron.struct.OldSKU;
import com.lifepics.neuron.struct.SKU;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;

import org.w3c.dom.Node;

/**
 * A utility class for functions that parse SKUs from server format.
 * This is mainly for {@link OrderParser} .. the product update page
 * will use it, but only in the most primitive way.
 * In any case, it's nice to have the code cleanly factored out here.
 */

public class SKUParser {

// --- attribute helper ---

   /**
    * @return An attributes map, or null if there's no attributes section.
    *         The map is linked so the caller can tell the original order.
    */
   public static LinkedHashMap parseAttributes(Node node) throws ValidationException {

      Node a1 = XML.getElementTry(node,"Attributes");
      if (a1 == null) return null;

      return parseAttributeList(a1);
   }

   /**
    * @return An attributes map, never null.
    */
   public static LinkedHashMap parseAttributeList(Node a1) throws ValidationException {

      LinkedHashMap attributes = new LinkedHashMap();

      Iterator i = XML.getElements(a1,"Attribute");
      while (i.hasNext()) {
         Node a2 = (Node) i.next();

         String key   = XML.getElementText(a2,"GroupName");
         String value = XML.getElementText(a2,"Value"    );
         attributes.put(key,value);
      }

      return attributes;
   }

// --- rewriter interface ---

   // the point of rewriting is to make invoice.xsl keep working without
   // any changes.  for our main invoice.xsl that's nice,
   // but for all the custom invoice.xsl files out there it's essential.

   /**
    * An interface for writing processed SKUs back to the order.xml file.
    */
   public interface Rewriter {

      /**
       * Having found a SKU code in a node named nameNormal,
       * with associated attributes under a sibling node,
       * now replace the SKU code with a descriptive string
       * and save the SKU code in a node named nameBackup.
       * See the code for an example that doesn't fit here in HTML.
       *
       * @param nodeNormal The node to be replaced.  We really only want to replace the text node
       *                   underneath it, but it's not convenient to pass that around.
       * @param nameNormal The name of the original node.  We could deduce this from the node,
       *                   I guess, but we already know it, so why not use it?
       * @param nameBackup The name of the backup node.
       * @param sku The string SKU code, not including attribute descriptions.
       * @param skuRewrite The rewritten SKU code.
       */
      void rewrite(Node nodeNormal, String nameNormal, String nameBackup, String sku, String skuRewrite);
   }

   // for example, if you had this, plus attributes,
   //
   // <ProductSku>4x6</ProductSku>
   //
   // you'd end up with something like this (except no line break)
   //
   // <BaseSku>4x6</BaseSku>
   // <ProductSku>4x6 Matte No Border</ProductSku>
   //
   // the reason there's no line break is, it's not worth reformatting
   // the whole file just to figure out the right level of indentation

   public static final String BASE_SKU = "BaseSku";

// --- rewriter implementation ---

   // callers are free to implement whatever they want,
   // but in practice they'll want this or no rewriter.

   public static class FlagRewriter implements Rewriter {

      private Rewriter next;
      private boolean flag;
      public FlagRewriter(Rewriter next) { this.next = next; flag = false; }

      public void rewrite(Node nodeNormal, String nameNormal, String nameBackup, String sku, String skuRewrite) {
         flag = true;
         next.rewrite(nodeNormal,nameNormal,nameBackup,sku,skuRewrite);
      }

      public boolean needRewrite() { return flag; }
   }

   public static class StandardRewriter implements Rewriter {

      public void rewrite(Node nodeNormal, String nameNormal, String nameBackup, String sku, String skuRewrite) {
         Node parent = nodeNormal.getParentNode();

         // the base element comes first because that's what makes sense with no line break
         XML.insertElementText (parent,nodeNormal,nameBackup,sku);
         XML.replaceElementText(parent,nodeNormal,nameNormal,skuRewrite);
      }
   }

// --- construction ---

   private boolean wholesale;
   private boolean allowOld;
   private boolean allowNew;
   private Rewriter rewriter; // nullable
   private HashMap conversionMap;

   /**
    * @param rewriter A rewriter object, or null if none desired.
    */
   public SKUParser(boolean wholesale, boolean allowOld, boolean allowNew, Rewriter rewriter, HashMap conversionMap) {
      this.wholesale = wholesale;
      this.allowOld = allowOld;
      this.allowNew = allowNew;
      this.rewriter = rewriter;
      this.conversionMap = conversionMap;
   }

// --- main function ---

   /**
    * @param nameNormal The normal entry name.
    * @param nameWholesale The wholesale entry name.  This field is only used if the wholesale flag is set.
    *                      Thus, it <i>may</i> be null when the flag is clear,
    *                      but it's not required to be null, and it may be convenient to fill it in anyway.
    */
   public SKU parse(Node node, String nameNormal, String nameWholesale) throws ValidationException {

   // get attributes

      LinkedHashMap attributes = parseAttributes(node);
      // we will construct old or new SKUs according to whether this is null or not null.
      // so, check that it fits with what's allowed.
      if (attributes != null && ! allowNew) throw new ValidationException(Text.get(this,"e1"));
      if (attributes == null && ! allowOld) throw new ValidationException(Text.get(this,"e2"));

   // get SKUs

      Node nodeNormal = XML.getElement(node,nameNormal);
      String skuNormal = XML.getText(nodeNormal);
      // same as getElementText except we remember the node object

      String skuResult = skuNormal;

      // if wholesale, see if there's a usable wholesale SKU
      // (it may be there even when not wholesale, but we don't look)
      if (wholesale) {
         String s = XML.getNullableText(node,nameWholesale);
         if (s != null && s.length() != 0) skuResult = s;
         // in the sample I have, the node is there, but has no data.
      }

   // rewrite and finish

      SKU result;
      if (attributes != null) {

         result = NewSKU.get(skuResult,attributes);

         if (rewriter != null) {

            String skuRewrite = null;

            if (conversionMap != null && ! wholesale) {
               OldSKU oldSKU = (OldSKU) conversionMap.get(result);
               if (oldSKU != null) skuRewrite = oldSKU.toString();
            }
            //
            // skuRewrite will stay null if there's no entry in the conversion map.
            //
            // it'd be more correct to validate the config file to prevent back-conversion
            // for wholesalers, but I'd rather just exclude it here.
            // the problem is, for wholesalers, the conversion entries tell how to convert
            // wholesale SKUs .... they don't say anything about the dealer SKUs that show
            // up on the invoice for the user to see.
            //
            // we don't actually have an object that corresponds to NewSKU.get(skuNormal,attributes),
            // but since we're in the non-wholesaler case, skuResult == skuNormal

            if (skuRewrite == null) skuRewrite = NewSKU.computeToString(skuNormal,attributes,null);
            //
            // everywhere else in the code, the attribute order for the string form
            // of new SKUs is determined by rules in NewSKU (currently HashMap order).
            // here, though, we use the order that the attributes were listed
            // in the order.xml file  (which we know because of the LinkedHashMap).
            // the reason is, when the rules change, we can refresh most things,
            // but whatever we write into the processed order.xml file is what we're
            // stuck with.  so, assume the server knows what it's doing in this case.

            // note, invoice.xsl never looks at wholesale SKUs for any reason,
            // so here we use skuNormal even if skuResult is different!
            rewriter.rewrite(nodeNormal,nameNormal,BASE_SKU,skuNormal,skuRewrite);
         }

      } else {
         result = new OldSKU(skuResult);
      }

      return result;
   }

}

