/*
 * StoreHoursTransaction.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Log;
import com.lifepics.neuron.core.Pop;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.misc.StoreHours;
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
 * A utility class for the store hours refresh transaction,
 * and also for the wrapper function that calls it nicely.
 * The store hours object is down in misc because someday it
 * might be shared, but this has to go over the net package.
 */

public class StoreHoursTransaction extends GetTransaction {

// --- the transaction itself ---

   private String storeHoursURL;
   private MerchantConfig merchantConfig;
   private LinkedList result;

   private StoreHoursTransaction(String storeHoursURL, MerchantConfig merchantConfig, LinkedList result) {
      this.storeHoursURL = storeHoursURL;
      this.merchantConfig = merchantConfig;
      this.result = result;
   }

   public String describe() { return Text.get(this,"s3"); }
   protected String getFixedURL() { return storeHoursURL; }
   protected void getParameters(Query query) throws IOException {
      if (merchantConfig.isWholesale) {
         throw new IOException(Text.get(this,"e1")); // not supported
      } else {
         query.add("locationID",Convert.fromInt(merchantConfig.merchant));
      }
      query.addPasswordCleartext("password",merchantConfig.password);
   }

   protected boolean receive(InputStream inputStream) throws Exception {

      result.clear(); // in case we're retrying

      Document doc = XML.readStream(inputStream);
      Node info = XML.getElement(doc, "HoursInfo");
      Node list = XML.getElement(info,"HoursList");

      Iterator i = XML.getElements(list,"Hours");
      while (i.hasNext()) {
         Node node = (Node) i.next();

         String sDay = XML.getElementText(node,"Day");
         int dayNumber = Convert.toInt(XML.getElementText(node,"DayNumber"));
         String sOpen  = XML.getElementText(node,"Open");
         String sClose = XML.getElementText(node,"Close");

         String day = (sDay.length() > 3) ? sDay.substring(0,3) : sDay; // note, always the first case
         int dMul = Convert.toDOW(day)*Convert.MINUTES_PER_DAY;

         int tOpen  = Convert.toTime(sOpen);
         int tClose = Convert.toTime(sClose);
         if (tOpen == 0 && tClose == 0) continue; // this is how the server represents being closed

         if (tOpen >= tClose) throw new Exception(Text.get(this,"e2",new Object[] { sDay, sOpen, sClose }));
         // go ahead and check this here where we have the nice full name of the day for the error text
         //
         // I thought about letting for example (09:00, 01:00) represent 9 AM on one day to 1 AM on the next,
         // but there's no demand for it.  it's more likely to result from a failed attempt at entering 9 AM
         // to 1 PM on the same day, so we want an error in that case.

         StoreHours hours = new StoreHours();

         hours.id = (dayNumber+1)*100; // nice round numbers
         hours.open  = dMul+tOpen;
         hours.close = dMul+tClose;

         // although the point of the ID field is to allow merging, the place where it would actually happen
         // is not here but in the autoconfig code.  here we just accept a complete data set from the server.

         if (tClose == Convert.MINUTES_PER_DAY-1) hours.close = Convert.addTOW(hours.close,1);
         // the server represents being open 24 hours as (00:00, 23:59);
         // I'll generalize a little and let any 23:59 actually mean 00:00 on the next day.
         // this creates the possibility of equal endpoints, but we'll clean that up below.

         result.add(hours);
      }
      // caller will validate for internal consistency,
      // all we have to do here is fill in the fields.
      // that's the usual plan, but it's not really true here.
      // there are extra validation and processing steps in
      // this case that are much easier to do down here where
      // we have the separate day and time fields.

      StoreHours.widen(result,0,0); // merges intervals with equal endpoints, possibly producing always open

      return true;
   }

// --- the wrapper function ---

   /**
    * Run a store hours refresh and report success or failure in the UI.
    *
    * @return True if the refresh succeeded, false if it failed.  The hours are always returned
    *         in the old list object because that's convenient for doEditAutoComplete.
    */
   public static boolean refresh(Component parent, String storeHoursURL, MerchantConfig merchantConfig, LinkedList storeHoursIn) {
      Class c = StoreHoursTransaction.class;

   // talk to the server

      // this part does have to happen in a separate list because it can fail

      LinkedList storeHoursOut = new LinkedList();
      try {
         new StoreHoursTransaction(storeHoursURL,merchantConfig,storeHoursOut).runInline();
         StoreHours.validate(storeHoursOut);
      } catch (Throwable t) {
         if (parent != null) {
            Pop.error(parent,t,Text.get(c,"s1"));
         } else { // not UI
            Log.log(Level.SEVERE,c,"i2",t);
         }
         return false;
      }

      // from here on out there should be no exceptions

      storeHoursIn.clear();
      storeHoursIn.addAll(storeHoursOut);

   // report (so the user can tell if anything happened)

      // the entry count isn't exactly right, we've thrown out closed days
      // and merged adjacent intervals (typically from being open 24 hours)

      Object[] args = new Object[] { new Integer(storeHoursOut.size()),
                                     Convert.fromInt(storeHoursOut.size()) };
      String message = Text.get(c,"i1",args);
      if (parent != null) {
         Pop.info(parent,message,Text.get(c,"s2"));
      } else { // not UI
         Log.log(Level.INFO,c,"i3",new Object[] { message });
      }

      return true;
   }

}

