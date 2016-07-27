/*
 * OrderStub.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Log;
import com.lifepics.neuron.core.Reportable;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.gui.User;
import com.lifepics.neuron.object.CopyUtil;
import com.lifepics.neuron.struct.*;
import com.lifepics.neuron.thread.Entity;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Level;

/**
 * An object used to track orders before details are known.
 */

public class OrderStub extends Structure implements Reportable, Entity {

// --- fields ---

   // basically, the order sequence and ID form a compound key,
   // to be used together in all cases.  the one exception is,
   // lab integrations often require an order number, so there
   // we stick with just the order ID and handle the (rare)
   // collisions across sequences the same way we do reprints.

   public String orderSeq; // null for main sequence
   public int orderID;

   public File orderDir;
   public int status;
   public int hold;
   public String lastError; // nullable

   public Date recmodDate;
   public Date failDateFirst; // nullable
   public Date failDateLast;  //
   public String lastUpdated; // server time, not worth converting to local ; format is "M/d/yyyy h:mm:ss a"
   public Date lastAttempt;   // nullable ; these two and LastUpdated above are used only by DownloadThread
   public Date lastProgress;  //

   public Wholesale wholesale; // null unless wholesaler

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      OrderStub.class,
      1,3,
      new AbstractField[] {

         new NullableStringField("orderSeq","OrderSeq"),
         new IntegerField("orderID","OrderID"),

         new FileField("orderDir","OrderDir"),
         new EnumeratedField("status","Status",OrderEnum.orderStatusType),
         new EnumeratedField("hold","Hold",OrderEnum.holdType),
         new NullableStringField("lastError","LastError"),

         new DateField("recmodDate","RecmodDate"),
         new NullableDateField("failDateFirst","FailDateFirst"),
         new NullableDateField("failDateLast","FailDateLast"),
         new NullableStringField("lastUpdated","LastUpdated"),
         new NullableDateField("lastAttempt","LastAttempt"),
         new NullableDateField("lastProgress","LastProgress"),

         new NullableStructureField("wholesale","Wholesale",Wholesale.sd)
      });

   protected StructureDefinition sd() { return sd; }

// --- implementation of Reportable ---

   // if you make use of more fields here, be sure that they're filled in
   // in the part of DownloadThread where an incomplete OrderStub is used.

   public String getReportType() {
      return Log.ORDER;
   }

   public String getReportID() {
      return (wholesale != null) ? Convert.fromInt(wholesale.merchantOrderID) : getFullID();
      // we're not ignoring orderSeq with wholesale, we know by validation that it's null
   }

   public String getReportMerchant() {
      return (wholesale != null) ? Convert.fromInt(wholesale.merchant) : null;
   }

   public String getLocalOrderSeq() {
      return orderSeq;
   }

   public int getLocalOrderID() {
      return orderID;
   }

   public int getLocalStatusCode() {
      return LocalStatus.CODE_ERR_ORD_GENERIC;
   }

// --- implementation of Entity ---

   public String getID() {
      return getFullID();
   }

   public boolean testStatus(int statusFrom, int statusActive) {

      if (statusFrom == Order.STATUS_ORDER_RECEIVED) {

         // invoice thread, pull relic statuses too

         return (    (    status == statusFrom
                       || status == Order.STATUS_ORDER_FORMATTING
                       || status == Order.STATUS_ORDER_FORMATTED
                       || status == statusActive )
                  && hold == Order.HOLD_NONE );

      } else {

         return (    (    status == statusFrom
                       || status == statusActive )
                  && hold == Order.HOLD_NONE );
      }
   }

   public void setStatus(int status) {
      this.status = status;
      recmodDate = new Date();
   }

   public void setStatusError(int statusFrom, String lastError, boolean told,
                              boolean pauseRetry, long pauseRetryLimit) {
      Date now = new Date();

      if (pauseRetry) {

         // now we can check the condition on pauseRetryLimit; see DiagnoseHandler.
         // it is: if we've failed before, and are past the limit, no more retries.

         if (    failDateFirst != null
              && now.getTime() >= failDateFirst.getTime() + pauseRetryLimit ) {

            pauseRetry = false;
         }
      }

      status = statusFrom;
      hold = pauseRetry ? Order.HOLD_RETRY : Order.HOLD_ERROR;
      this.lastError = lastError;
      recmodDate = now;
      if (pauseRetry) {
         if (failDateFirst == null) failDateFirst = now;
         failDateLast = now;
      }

      if ( ! told && ! isRetrying() ) User.tell(User.CODE_ORDER_ERROR,Text.get(OrderStub.class,"e3",new Object[] { getFullID() }) + lastError);
      // the check for isRetrying makes the user notification match the server logging;
      // note that we check it <i>after</i> the status is set, just like in EntityThread.
   }

   // not part of Entity, but goes with setStatusError
   public void endRetry(Object o, String key) {

      if (failDateFirst != null) {
         // report as if severe, to fit with the original errors
         Log.log(this,Level.INFO,o,key,new Object[] { getFullID() },null,Level.SEVERE);
      }

      failDateFirst = null;
      failDateLast = null;
      //
      // it would make sense to put this part inside the if-test,
      // but it used to be unconditional, let's keep it that way.
      // also, what if somehow (hand editing) failDateFirst gets
      // set to null even though failDateLast isn't?  no problem
      // if we don't send a success notification, but the fields
      // should still get cleared.
   }

   public boolean isRetrying() {
      return (      hold == Order.HOLD_RETRY
               && ! failDateLast.equals(failDateFirst) );
   }

// --- copy functions ---

   public void copyFrom(OrderStub stub) {

      // this seems stupid, but I don't think there's any way
      // to clone an OrderStub into the stub part of an order.
      // the structure fields can't do it because they don't
      // actually copy anything, they all rely on Object.clone.

      orderSeq = stub.orderSeq;
      orderID = stub.orderID;
      orderDir = stub.orderDir;
      status = stub.status;
      hold = stub.hold;
      lastError = stub.lastError;
      recmodDate = stub.recmodDate;
      failDateFirst = stub.failDateFirst;
      failDateLast = stub.failDateLast;
      lastUpdated = stub.lastUpdated;
      lastAttempt = stub.lastAttempt;
      lastProgress = stub.lastProgress;
      wholesale = (stub.wholesale == null) ? null : stub.wholesale.copy();
   }

// --- helper functions ---

   public boolean isPurgeableStatus() {
      return (status >= Order.STATUS_ORDER_COMPLETED);
   }

   /**
    * Full ID, like full name, is a combination of two parts.
    */
   public String getFullID() {
      return getFullID(orderSeq,orderID);
   }
   public static String getFullID(String orderSeq, int orderID) {
      String s = Convert.fromInt(orderID);
      return (orderSeq == null) ? s : (orderSeq + s);
   }

   private static boolean isUCLetters(String s) {

      for (int i=0; i<s.length(); i++) {
         char c = s.charAt(i);
         if (c < 'A' || c > 'Z') return false;
      }

      return true;
   }

// --- validation ---

   public static void validateOrderSeq(String orderSeq) throws ValidationException {
      if ( ! (orderSeq == null || isUCLetters(orderSeq)) ) {
         throw new ValidationException(Text.get(OrderStub.class,"e5",new Object[] { orderSeq })); // not null
      }
      // the point here is to avoid collisions.  if we allowed numbers, the full IDs
      // could collide, for example L2 + 40 vs. L + 240; if we allowed lowercase,
      // the order files could collide in case-insensitive file systems like Windows.
      // for another problem example, see Group.compute.
   }

   public void validate() throws ValidationException {

      validateOrderSeq(orderSeq);

      if (orderSeq != null && wholesale != null) {
         throw new ValidationException(Text.get(OrderStub.class,"e6"));
      }
      // maybe there's a way to make sense of this combination,
      // but I can't see it.  the whole point of having an order
      // be wholesale is that we talk to the server differently,
      // but for local orders we don't talk to the server at all.
      // well, error reports, and maybe some status later, but
      // that's the idea.
      //
      // in the big picture, wholesale and sequences are kind of related.
      // there's one order ID sequence for each merchant,
      // plus one order ID sequence formed with LifePics global IDs.
      // local orders create more sequences for each merchant,
      // and also some totally new sequences for each wholesaler ID.

      if (status < Order.STATUS_ORDER_MIN || status > Order.STATUS_ORDER_MAX) {
         throw new ValidationException(Text.get(OrderStub.class,"e1",new Object[] { Convert.fromInt(status) }));
      }

      if (hold < Order.HOLD_MIN || hold > Order.HOLD_MAX) {
         throw new ValidationException(Text.get(OrderStub.class,"e2",new Object[] { Convert.fromInt(hold) }));
      }

      if (wholesale != null) wholesale.validate();
   }

   // used to be assertWholesale, if you're looking for parallels with the axon side
   public int getWholesaleCode(int merchant, boolean isWholesale) throws IOException {
      boolean isWholesaleOrder = (wholesale != null);
      if (isWholesale) {
         if (isWholesaleOrder) {
            return Order.WSC_WHOLESALE;
         } else {
            throw new IOException(Text.get(OrderStub.class,"e4"));
            // no mlrfnbr in this case, so it really doesn't work
         }
      } else {
         if (isWholesaleOrder) {
            return (merchant == wholesale.merchant) ? Order.WSC_PSEUDO : Order.WSC_PRO;
         } else {
            return Order.WSC_NORMAL;
         }
      }
   }

// --- wholesale class ---

   public static class Wholesale extends Structure {

      public String merchantName;
      public int merchant;
      public int merchantOrderID;

      public static final StructureDefinition sd = new StructureDefinition(

         Wholesale.class,
         0,0,
         new AbstractField[] {

            new StringField("merchantName","MerchantName"),
            new IntegerField("merchant","Merchant"),
            new IntegerField("merchantOrderID","MerchantOrderID")
         });

      protected StructureDefinition sd() { return sd; }

      public Wholesale copy() { return (Wholesale) CopyUtil.copy(this); }

      public void validate() throws ValidationException {
      }
   }

}

