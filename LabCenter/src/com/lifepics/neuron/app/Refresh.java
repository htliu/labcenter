/*
 * Refresh.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.core.ChainedException;
import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.dendron.GetProducts;
import com.lifepics.neuron.dendron.GetConversions;
import com.lifepics.neuron.dendron.GetNewProducts;
import com.lifepics.neuron.dendron.ProductConfig;
import com.lifepics.neuron.dendron.ProductDifference;
import com.lifepics.neuron.net.MerchantConfig;
import com.lifepics.neuron.struct.PSKU;
import com.lifepics.neuron.struct.SKU;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

/**
 * Static code for the product refresh function.  This is used
 * from the UI by EditSKUDialog, and internally by AutoRefresh.
 */

public class Refresh {

   /**
    * An interface to let us deal with the fact that sometimes we refresh the config directly
    * but sometimes we refresh in the user interface without going through a validation cycle.
    */
   public interface RefreshInterface {

      void line(String s);
      void blank();
      void result(String key);

      MerchantConfig getMerchantConfig();
      String getOldProductURL();
      String getNewProductURL();
      String getConversionURL();

      ProductConfig getProductConfig();
      void setProductConfig(ProductConfig pc);

      /**
       * Function name meant to suggest that it can have side effects.
       */
      Collection recomputeSKUs(ProductConfig pc);

      void transfer(SKU skuNew, SKU skuOld);

      String getSingleQueue();
      void setIfNotSet(SKU sku, String queueID);

      String getDefaultQueue();
      void clearDefaultQueue();

      void clean();
      void showAll();
   }

   public static void refresh(RefreshInterface ri) {
      MerchantConfig merchantConfig = ri.getMerchantConfig();

   // talk to the server

      ProductConfig before = ri.getProductConfig();
      ProductConfig after  = before.copy();

      boolean fOld  = false;
      boolean fNew  = false;
      boolean fConv = false;

      // the transactions clear the lists before writing into them,
      // but we want to be able to continue on partial failure, so ...
      //
      LinkedList result;

      try {
         result = new LinkedList();
         new GetProducts(ri.getOldProductURL(),merchantConfig,result).runInline();
         fOld = true;
         after.productsOld = result;
         ri.line(Text.get(Refresh.class,"i12"));
      } catch (Throwable t) {
         ri.line(Text.get(Refresh.class,"e12",new Object[] { ChainedException.format(t) }));
      }

      try {
         result = new LinkedList();
         new GetNewProducts(ri.getNewProductURL(),merchantConfig,result).runInline();
         fNew = true;
         after.productsNew = result;
         ri.line(Text.get(Refresh.class,"i13"));
      } catch (Throwable t) {
         ri.line(Text.get(Refresh.class,"e13",new Object[] { ChainedException.format(t) }));
      }

      // in theory the conversions were supposed to be editable,
      // but in practice it didn't work out that way,
      // and now the conversions page is getting lots of errors.
      // so, don't call it unless we have no conversions at all.
      //
      if (before.conversions.size() == 0) {
         try {
            result = new LinkedList();
            new GetConversions(ri.getConversionURL(),merchantConfig,result).runInline();
            fConv = true;
            after.conversions = result;
            ri.line(Text.get(Refresh.class,"i14"));
         } catch (Throwable t) {
            ri.line(Text.get(Refresh.class,"e14",new Object[] { ChainedException.format(t) }));
         }
      }
      // else no result line and no change to conversions

      if ( ! (fOld || fNew || fConv) ) { ri.result("s6"); return; }

      // from here on out there should be no exceptions

   // simple updates

      ri.setProductConfig(after); // accept new config

      Collection skusAll = ri.recomputeSKUs(after);
      // skusAll is most likely not used, but call anyway, recompute has side effects

   // harder updates

      // well, they used to be hard, now ProductDifference does
      // all the work

      ProductDifference d = ProductDifference.diff(before,after);

      Iterator i = d.skusTransfer.entrySet().iterator();
      while (i.hasNext()) {
         Map.Entry entry = (Map.Entry) i.next();
         SKU skuNew = (SKU) entry.getKey();
         SKU skuOld = (SKU) entry.getValue();

         ri.transfer(skuNew,skuOld);
      }

      String singleQueue = ri.getSingleQueue();
      if (singleQueue != null) {

         // apply singleQueue to all newly-created SKUs
         i = d.skusCreate.iterator();
         while (i.hasNext()) {
            SKU sku = (SKU) i.next();
            ri.setIfNotSet(sku,singleQueue);
         }
      }

      // note about relation to special one-time behavior, below ...
      // transfer doesn't commute, but should definitely happen first.
      // the single queue bit does commute, because if there's both a
      // single queue and a default queue, the two must be the same.

   // special one-time behavior

      // what is this for?  well, if a user has manual queues, and is using
      // the default queue to send things there, then there will be no trace
      // of those SKU codes in the config file.  so, although we want to get
      // rid of the default queue field, we can't do it right away ... we
      // have to wait until we know all the SKU codes.  so, do exactly that!
      //
      // hiding the field afterward is what makes this a one-time behavior ...
      // after it's happened, defaultQueue will still exist as a field, but
      // it will be null, and will no longer be editable in the UI.
      // see (**) above for the code that hides the field at screen startup.

      // we can only get rid of the default queue when we know the products,
      // so don't do anything if the refresh is for conversions only.
      // new products?  old products?  I think either should be fine.
      // anyway, it's totally academic, the default queue is ancient history.

      String defaultID = ri.getDefaultQueue();
      if (defaultID != null && (fOld || fNew)) {

         // apply defaultQueue to all SKUs
         i = skusAll.iterator(); // note this is fully up to date
         while (i.hasNext()) {
            PSKU psku = (PSKU) i.next();
            if (psku instanceof SKU) {
               ri.setIfNotSet((SKU) psku,defaultID);
            }
            // else ignore it, because of how two reasons fit together.
            // one, it's not clear what the right action is.  "not set" is defined
            // in terms of pattern matching against the argument, so it doesn't
            // make a lot of sense for patterns -- we'd some crazy overlap checker.
            // two, it's not a real case.  the only way to have a default queue is
            // to use an ancient config file, in which case there shouldn't be any
            // patterns in it.
         }

         ri.clearDefaultQueue();
      }

   // finish up

      ri.clean(); // may make counts wrong, oh well
      ri.showAll(); // not just a userChange, since SKU lists may have changed

      int countNew = d.countTransferAuto + d.countCreate;
      // number of new SKUs, regardless of how populated
      //
      // here "new" means "new in the product definitions" ...
      // if the definitions have been changing back and forth,
      // maybe the SKU will be one that's already in the list.

      int countXfr = d.skusTransfer.size(); // can be computed, but why bother

      // if the products are the same as before, as is likely,
      // the user won't be able to see that anything happened,
      // so show a success message to make it perfectly clear.
      //
      Object[] args;

      ri.blank();

      args = new Object[] { new Integer(countNew), Convert.fromInt(countNew) };
      ri.line(Text.get(Refresh.class,"i1",args));

      args = new Object[] { new Integer(countXfr), Convert.fromInt(countXfr) };
      ri.line(Text.get(Refresh.class,"i2",args));

      ri.result((fOld && fNew && fConv) ? "s8" : "s7");
   }

}

