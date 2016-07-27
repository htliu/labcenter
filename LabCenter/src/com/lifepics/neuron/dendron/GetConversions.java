/*
 * GetConversions.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.net.GetTransaction;
import com.lifepics.neuron.net.MerchantConfig;
import com.lifepics.neuron.net.Query;
import com.lifepics.neuron.object.XML;
import com.lifepics.neuron.struct.OldSKU;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * A transaction that gets a list of {@link Conversion} from the server.
 */

public class GetConversions extends GetTransaction {

   private String url;
   private MerchantConfig merchantConfig;
   private LinkedList result;

   public GetConversions(String url, MerchantConfig merchantConfig, LinkedList result) {
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

      SKUParser skuParser;
      skuParser = new SKUParser(/* wholesale = */ false, // so nameWholesale isn't needed
                                /* allowOld  = */ false,
                                /* allowNew  = */ true,
                                /* rewriter  = */ null,
                                /* conversionMap = */ null);
      // can't use another SKUParser for oldSKU, because it would get newSKU's attributes.
      // but, why would we want to do that, anyway?

      Document doc = XML.readStream(inputStream);
      Node list = XML.getElement(doc,"Entries");

      Iterator i = XML.getElements(list,"Entry");
      while (i.hasNext()) {
         Node node = (Node) i.next();

         Conversion c = new Conversion();

         c.oldSKU = new OldSKU(XML.getElementText(node,"OldSku"));
         c.newSKU = skuParser.parse(node,"ProductSku",null);

         result.add(c);
      }

      Collections.sort(result,Conversion.newSKUComparator);

      // order of calls doesn't matter, sort is stable
      Conversion.removeDuplicates(result);
      Conversion.validate(result); // see comment in GetProducts
      return true;
   }

}

