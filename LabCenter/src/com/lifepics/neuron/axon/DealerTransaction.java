/*
 * DealerTransaction.java
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
 * A utility class for the dealer refresh transaction,
 * and also for the wrapper function that calls it nicely.
 */

public class DealerTransaction extends GetTransaction {

// --- the transaction itself ---

   private String dealerURL;
   private MerchantConfig merchantConfig;
   private LinkedList result;

   private DealerTransaction(String dealerURL, MerchantConfig merchantConfig, LinkedList result) {
      this.dealerURL = dealerURL;
      this.merchantConfig = merchantConfig;
      this.result = result;
   }

   public String describe() { return Text.get(this,"s3"); }
   protected String getFixedURL() { return dealerURL; }
   protected void getParameters(Query query) throws IOException {
      if (merchantConfig.isWholesale) {
         query.add("wholesalerID",Convert.fromInt(merchantConfig.merchant));
      } else {
         throw new IOException(Text.get(this,"e1")); // not supported
      }
      query.addPasswordObfuscate("encpassword",merchantConfig.password);
   }

   protected boolean receive(InputStream inputStream) throws Exception {

      result.clear(); // in case we're retrying

      Document doc = XML.readStream(inputStream);
      Node list = XML.getElement(doc,"DealerList");

      Iterator i = XML.getElements(list,"Location");
      while (i.hasNext()) {
         Node node = (Node) i.next();

         Dealer d = new Dealer();

         d.id = XML.getAttribute(node,"ID");
         d.name = XML.getAttribute(node,"MerchantName");
         d.password = XML.getAttribute(node,"LocationPassword");

         result.add(d);
      }
      // caller will validate for internal consistency,
      // all we have to do here is fill in the fields.

      Dealer.sort(result);
      // server probably returns in some random order

      return true;
   }

// --- the wrapper function ---

   /**
    * Run a dealer refresh and report success or failure in the UI.
    *
    * @return The new list, or the old list on failure.
    */
   public static LinkedList refresh(Component parent, String dealerURL, MerchantConfig merchantConfig, LinkedList dealersIn) {
      Class c = DealerTransaction.class;

   // talk to the server

      LinkedList dealersOut = new LinkedList();
      try {
         new DealerTransaction(dealerURL,merchantConfig,dealersOut).runInline();
         Dealer.validate(dealersOut);
      } catch (Throwable t) {
         if (parent != null) {
            Pop.error(parent,t,Text.get(c,"s1"));
         } else { // not UI
            Log.log(Level.SEVERE,c,"i2",t);
         }
         return dealersIn;
      }

      // from here on out there should be no exceptions

   // count new dealers

      // double list iteration (since findByID iterates too),
      // not the most efficient, but no matter, good enough.

      int countNew = 0;

      Iterator i = dealersOut.iterator();
      while (i.hasNext()) {
         Dealer d = (Dealer) i.next();
         if (Dealer.findByID(dealersIn,d.id) == null) countNew++;
      }

   // report (so the user can tell if anything happened)

      Object[] args = new Object[] { new Integer(dealersOut.size()),
                                     Convert.fromInt(dealersOut.size()),
                                     new Integer(countNew),
                                     Convert.fromInt(countNew) };
      String message = Text.get(c,"i1",args);
      if (parent != null) {
         Pop.info(parent,message,Text.get(c,"s2"));
      } else { // not UI
         Log.log(Level.INFO,c,"i3",new Object[] { message });
      }

      return dealersOut;
   }

}

