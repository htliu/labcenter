/*
 * CarrierTransaction.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Log;
import com.lifepics.neuron.core.Pop;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.net.GetTransaction;
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
 * A utility class for the carrier refresh transaction,
 * and also for the wrapper function that calls it nicely.
 * This is different from DealerTransaction and
 * PriceListTransaction since we merge with existing data.
 */

public class CarrierTransaction extends GetTransaction {

// --- the transaction itself ---

   private String carrierURL;
   private LinkedList result;

   private CarrierTransaction(String carrierURL, LinkedList result) {
      this.carrierURL = carrierURL;
      this.result = result;
   }

   public String describe() { return Text.get(this,"s3"); }
   protected String getFixedURL() { return carrierURL; }
   protected void getParameters(Query query) throws IOException {}

   protected boolean receive(InputStream inputStream) throws Exception {

      result.clear(); // in case we're retrying

      Document doc = XML.readStream(inputStream);
      Node list = XML.getElement(doc,"ArrayOfCarrier");

      Iterator i = XML.getElements(list,"Carrier");
      while (i.hasNext()) {
         Node node = (Node) i.next();

         Carrier c = new Carrier();

         c.primaryName = XML.getElementText(node,"Name");
         c.displayName = XML.getElementText(node,"DisplayName");
         // show flag is set in mergeFrom

         result.add(c);
      }
      // caller will validate for internal consistency,
      // all we have to do here is fill in the fields.

      Carrier.sort(result);
      // server probably returns in some random order

      return true;
   }

// --- the wrapper function ---

   /**
    * Run a carrier refresh and report success or failure in the UI.
    *
    * @return The new list, or the old list on failure.
    */
   public static CarrierList refresh(Component parent, String carrierURL, CarrierList listIn) {
      Class c = CarrierTransaction.class;

   // talk to the server

      int countNew;

      CarrierList listOut = new CarrierList();
      try {
         new CarrierTransaction(carrierURL,listOut.carriers).runInline();
         countNew = listOut.mergeFrom(listIn);
         listOut.validate();
      } catch (Throwable t) {
         if (parent != null) {
            Pop.error(parent,t,Text.get(c,"s1"));
         } else {
            Log.log(Level.SEVERE,c,"i2",t);
         }
         return listIn;
      }

      // from here on out there should be no exceptions

   // report (so the user can tell if anything happened)

      Object[] args = new Object[] { new Integer(listOut.carriers.size()),
                                     Convert.fromInt(listOut.carriers.size()),
                                     new Integer(countNew),
                                     Convert.fromInt(countNew) };
      String message = Text.get(c,"i1",args);
      if (parent != null) {
         Pop.info(parent,message,Text.get(c,"s2"));
      } else {
         Log.log(Level.INFO,c,"i3",new Object[] { message });
      }

      return listOut;
   }

}

