/*
 * StatusTransaction.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.net.MerchantConfig;
import com.lifepics.neuron.net.PauseCallback;
import com.lifepics.neuron.net.PostTransaction;
import com.lifepics.neuron.net.Query;
import com.lifepics.neuron.net.RetryableException;

import java.io.IOException;
import java.io.InputStream;

/**
 * The transaction for reporting order status to the server.
 * It is used by both the UI and the download thread,
 * so I thought it would be best to put it in its own class.
 */

public class StatusTransaction extends PostTransaction {

// --- fields ---

   private OrderStub order;
   private int status;
   private MerchantConfig merchantConfig;
   private DownloadConfig config;

// --- construction ---

   public StatusTransaction(OrderStub order, int status, MerchantConfig merchantConfig, DownloadConfig config) {
      this.order = order;
      this.status = status;
      this.merchantConfig = merchantConfig;
      this.config = config; // no need to copy, the transaction exists only briefly
   }

// --- utility function ---

   private static String toServerStatus(int status) {

      // this is like Convert.toOrderStatus, except for this:
      // the server understands only a subset of statuses,
      // and it probably uses its own peculiar names for them

      switch (status) {
      case Order.STATUS_ORDER_RECEIVED:   return Text.get(StatusTransaction.class,"ss2");
      case Order.STATUS_ORDER_COMPLETED:  return Text.get(StatusTransaction.class,"ss5");
      case Order.STATUS_ORDER_ABORTED:    return Text.get(StatusTransaction.class,"ss6");
      case Order.STATUS_ORDER_CANCELED:   return Text.get(StatusTransaction.class,"ss7");
      default:
         throw new IllegalArgumentException(); // programmer error
      }
   }

// --- hooks for PostTransaction ---

   public String describe() { return Text.get(this,"s1"); }
   protected String getFixedURL() { return config.statusURL; }
   protected void getParameters(Query query) throws IOException {

      // usually we'd test order.wholesale and then ignore orderSeq in the
      // wholesale case since it's validated null.  that would work here,
      // except I want the error message for wholesaler local orders to be
      // this one, not the unhelpful one from getWholesaleCode.
      if (order.orderSeq != null) throw new IOException(Text.get(StatusTransaction.class,"e1"));

      int wsc = order.getWholesaleCode(merchantConfig.merchant,merchantConfig.isWholesale);
      switch (wsc) {
      case Order.WSC_NORMAL:
         query.add("MerchantOrderID",Convert.fromInt(order.orderID));
         query.add("LocationID",Convert.fromInt(merchantConfig.merchant));
         break;
      case Order.WSC_PSEUDO:
         query.add("MerchantOrderID",Convert.fromInt(order.wholesale.merchantOrderID));
         query.add("LocationID",Convert.fromInt(merchantConfig.merchant));
         // order.orderID is the LifePics ID, don't use that!
         // doesn't matter which mlrfnbr we use, they're the same here
         break;
      case Order.WSC_PRO:
         query.add("MerchantOrderID",Convert.fromInt(order.wholesale.merchantOrderID));
         query.add("LocationID",Convert.fromInt(order.wholesale.merchant));
         query.add("dealerLocID",Convert.fromInt(merchantConfig.merchant));
         break;
      case Order.WSC_WHOLESALE:
         query.add("MerchantOrderID",Convert.fromInt(order.wholesale.merchantOrderID));
         query.add("LocationID",Convert.fromInt(order.wholesale.merchant));
         query.add("WholesalerID",Convert.fromInt(merchantConfig.merchant));
         break;
      }
      query.addPasswordCleartext("Password",merchantConfig.password);
      query.add("Action",toServerStatus(status));

      if (order instanceof Order) { // should be
         Order orderObj = (Order) order;

         if (orderObj.carrier        != null) query.add("CarrierName",   orderObj.carrier       );
         if (orderObj.trackingNumber != null) query.add("TrackingNumber",orderObj.trackingNumber);
         // always null or not null together;
         // OrderManager takes care of clearing them when they shouldn't be sent
      }
   }

   public boolean run(PauseCallback callback) throws Exception {
      if (merchantConfig.password.length() == 0) DownloadThread.throwInvalidPassword();
      return super.run(callback);
      // go ahead and detect codeMissing, don't even send to server.
      // in the past, there was no password on this transaction; and
      // we don't want to lose backward compatibility; so the server
      // is going to accept blank passwords as meaning "old version".
      // so, in the new version, we want to block blank passwords up
      // front, and that's what I'm doing.
   }

   protected boolean receive(InputStream inputStream) throws Exception {

   // read the whole stream into a string

      String result = getText(inputStream);

   // check it -- the exact string "0" means no error, anything else is an error

      if (result.equals("0")) {
         // report successfully received
      } else if (result.equals("Order Not Found")) {
         // this happened briefly in late February 2008.  resetting the
         // server made it go away, but if it happens again, we want it
         // to be retryable
         throw new RetryableException(Text.get(this,"s3"));
      } else {
         throw new Exception(Text.get(this,"s2",new Object[] { result }));
      }

      return true;
   }

}

