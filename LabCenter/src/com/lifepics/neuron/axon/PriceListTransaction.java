/*
 * PriceListTransaction.java
 */

package com.lifepics.neuron.axon;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Log;
import com.lifepics.neuron.core.Pop;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.net.GetTransaction;
import com.lifepics.neuron.net.MerchantConfig;
import com.lifepics.neuron.net.Query;
import com.lifepics.neuron.object.XML;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Level;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.awt.Component;

/**
 * A utility class for the price list refresh transaction,
 * and also for the wrapper function that calls it nicely.
 * The dividing line between the two is fairly arbitrary;
 * I've chosen to do the same as EditSKUDialog.GetProducts:
 * have the transaction do minimal parsing, and let the
 * wrapper function handle the rest, including validation.
 */

public class PriceListTransaction extends GetTransaction {

// --- the transaction itself ---

   private String priceListURL;
   private MerchantConfig merchantConfig;
   private LinkedList result;

   private PriceListTransaction(String priceListURL, MerchantConfig merchantConfig, LinkedList result) {
      this.priceListURL = priceListURL;
      this.merchantConfig = merchantConfig;
      this.result = result;
   }

   public String describe() { return Text.get(this,"s3"); }
   protected String getFixedURL() { return priceListURL; }
   protected void getParameters(Query query) throws IOException {
      if (merchantConfig.isWholesale) {
         throw new IOException(Text.get(this,"e1")); // not supported
      } else {
         query.add("mlrfnbr",Convert.fromInt(merchantConfig.merchant));
      }
      query.addPasswordObfuscate("encpassword",merchantConfig.password);
   }

   protected boolean receive(InputStream inputStream) throws Exception {

      result.clear(); // in case we're retrying

      Document doc = XML.readStream(inputStream);
      Node list = XML.getElement(doc,"PriceLists");

      Iterator i = XML.getElements(list,"PriceList");
      while (i.hasNext()) {
         Node node = (Node) i.next();

         PriceList pl = new PriceList();

         pl.id = XML.getElementText(node,"ID");
         pl.name = XML.getElementText(node,"Name");

         result.add(pl);
      }
      // caller will validate for internal consistency,
      // all we have to do here is fill in the fields.

      PriceList.sort(result);
      // server probably returns in some random order

      return true;
   }

// --- the wrapper function ---

   /**
    * Run a price list refresh and report success or failure in the UI.
    *
    * @return The new list, or the old list on failure.
    */
   public static LinkedList refresh(Component parent, String priceListURL, MerchantConfig merchantConfig, LinkedList priceListsIn) {
      Class c = PriceListTransaction.class;

   // talk to the server

      LinkedList priceListsOut = new LinkedList();
      try {
         new PriceListTransaction(priceListURL,merchantConfig,priceListsOut).runInline();
         PriceList.validate(priceListsOut);
      } catch (Throwable t) {
         if (parent != null) {
            Pop.error(parent,t,Text.get(c,"s1"));
         } else {
            Log.log(Level.SEVERE,c,"i2",t);
         }
         return priceListsIn;
      }

      // from here on out there should be no exceptions

   // count new price lists

      // double list iteration (since findByID iterates too),
      // not the most efficient, but no matter, good enough.

      int countNew = 0;

      Iterator i = priceListsOut.iterator();
      while (i.hasNext()) {
         PriceList pl = (PriceList) i.next();
         if (PriceList.findByID(priceListsIn,pl.id) == null) countNew++;
      }

   // report (so the user can tell if anything happened)

      Object[] args = new Object[] { new Integer(priceListsOut.size()),
                                     Convert.fromInt(priceListsOut.size()),
                                     new Integer(countNew),
                                     Convert.fromInt(countNew) };
      String message = Text.get(c,"i1",args);
      if (parent != null) {
         Pop.info(parent,message,Text.get(c,"s2"));
      } else {
         Log.log(Level.INFO,c,"i3",new Object[] { message });
      }

      return priceListsOut;
   }

}

