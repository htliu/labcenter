/*
 * GetProducts.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.meta.NoCaseComparator;
import com.lifepics.neuron.net.GetTransaction;
import com.lifepics.neuron.net.MerchantConfig;
import com.lifepics.neuron.net.Query;
import com.lifepics.neuron.object.XML;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * A transaction that gets a list of {@link Product} from the server.
 */

public class GetProducts extends GetTransaction {

   private String url;
   private MerchantConfig merchantConfig;
   private LinkedList result;

   public GetProducts(String url, MerchantConfig merchantConfig, LinkedList result) {
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
      Node list = XML.getElement(doc,"SKUList");

      Iterator i = XML.getElements(list,"SKU");
      while (i.hasNext()) {
         Node node = (Node) i.next();

         Product p = new Product();

         p.productID = XML.getElementText(node,"ProductID");
         p.sku = XML.getAttribute(node,"ID");
         p.description = XML.getElementText(node,"Description");

         String temp = XML.getElementText(node,"TurnAroundTime"); // minutes
         p.dueInterval = (temp.length() == 0) ? null : new Long(Convert.toLong(temp) * 60000);

         add(result,p); // basically result.add(p)
      }

      Collections.sort(result,Product.skuComparator);
      // not really necessary, but since everything else is in SKU order,
      // it seems nicest to keep the product list in the same order too.

      Product.validate(result); // currently we don't run this transaction
      // in a DiagnoseHandler, so there are no retries, and it doesn't matter
      // whether this call is inside or outside.
      // but, if we added retries, it wouldn't be unreasonable to retry based
      // on validation failure.  in other cases I've seen the server return
      // parseable but invalid data due to temporary problems in the database.

      return true;
   }

   private static Comparator noCaseComparator = new NoCaseComparator();

   private static void add(LinkedList result, Product p) {

      Product q = Product.findProductBySKU(result,p.sku);
      if (q == null) { // normal case

         result.add(p);

      } else { // very rare case

         // there's at least one dealer that has multiple products
         // mapping to the same SKU .... it works because they're
         // only used at different locations, something like that.
         // anyway, LC has to accept it, so I'll merge the records.
         //
         // which one to keep should be determined by product ID,
         // so that the description doesn't fluctuate randomly
         // depending on the order in which the records come back.
         // numerical order would be better, but it would also
         // be a shame to require numerical IDs in just this case.

         int order = noCaseComparator.compare(p.productID,q.productID);
         if (order < 0) {        // p smaller, keep that

            merge(p,q);
            result.remove(q);
            result.add(p);

         } else if (order > 0) { // q smaller, keep that

            merge(q,p);

         } else {                // duplicate product ID, keep both, fail validation

            result.add(p);

            // if there are more records with the same SKU,
            // the duplicate product IDs might go away,
            // but there will always be two records with the same SKU.
         }
      }
   }

   /**
    * Merge information into pDest from pSrc.
    */
   private static void merge(Product pDest, Product pSrc) {

      if (pSrc.dueInterval != null) {
         if (    pDest.dueInterval == null
              || pDest.dueInterval.longValue() > pSrc.dueInterval.longValue() ) {
            pDest.dueInterval = pSrc.dueInterval;
         }
      }
   }

}

