/*
 * DownloadThread.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Log;
import com.lifepics.neuron.core.Reportable;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.gui.User;
import com.lifepics.neuron.meta.Accessor;
import com.lifepics.neuron.meta.CompoundComparator;
import com.lifepics.neuron.meta.FieldComparator;
import com.lifepics.neuron.meta.NaturalComparator;
import com.lifepics.neuron.meta.ReverseComparator;
import com.lifepics.neuron.misc.FileUtil;
import com.lifepics.neuron.misc.Purge;
import com.lifepics.neuron.misc.TransferTracker;
import com.lifepics.neuron.net.BandwidthUtil;
import com.lifepics.neuron.net.DownloadTransaction;
import com.lifepics.neuron.net.GetTransaction;
import com.lifepics.neuron.net.Handler;
import com.lifepics.neuron.net.MerchantConfig;
import com.lifepics.neuron.net.NetUtil;
import com.lifepics.neuron.net.PauseAdapter;
import com.lifepics.neuron.net.PauseCallback;
import com.lifepics.neuron.net.Query;
import com.lifepics.neuron.net.RegulatedTransaction;
import com.lifepics.neuron.net.RetryableException;
import com.lifepics.neuron.net.Transaction;
import com.lifepics.neuron.net.TransactionGroup;
import com.lifepics.neuron.object.XML;
import com.lifepics.neuron.table.AlternatingFile;
import com.lifepics.neuron.table.Table;
import com.lifepics.neuron.table.TableException;
import com.lifepics.neuron.thread.CategorizedException;
import com.lifepics.neuron.thread.EntityManipulator;
import com.lifepics.neuron.thread.ThreadStatus;
import com.lifepics.neuron.thread.ThreadStopException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Level;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * A thread that downloads images from the LifePics web server.
 */

public class DownloadThread extends OrderThread implements DownloadTransaction.ContentLengthHandler {

// --- fields ---

   private OrderManager orderManager;
   private MerchantConfig merchantConfig;
   private DownloadConfig config;
   private File dataDir;
   private File stylesheet;
   private boolean holdInvoice;
   private File localShareDir;
   private Handler handler;
   private TransferTracker tracker;
   private PauseAdapter pauseAdapter;

   private HashMap conversionMap;
   private HashMap bookMap;
   private boolean enableItemPrice;
   private long nextPoll;
   private Order.OrderFile orderFile; // used instead of OrderThread.item
   private ImageProcess imageProcess;

// --- prioritization ---

   // this is the heart of the prioritization feature.
   // everything else we do just makes sure that
   // the order list is updated and checked often enough.
   //
   // note that we only check the order list between orders,
   // not between images.  the other way would get things
   // to come down a little faster, but we think this'll work.

   private static final Long longZero = new Long(0);

   private static class IntervalAccessor implements Accessor {

      private HashMap dueMap;
      public IntervalAccessor(HashMap dueMap) { this.dueMap = dueMap; }

      public Class getFieldClass() { return Long.class; }
      public Object get(Object o) {
         if (o instanceof Order) {
            // null means no due interval, but it sorts as infinitely negative,
            // which is highest priority.  to fix that, negate all other longs,
            // then put a reverse comparator on the whole thing (in the caller).
            Long l = DueUtil.getDueInterval((Order) o,dueMap);
            return (l == null) ? null : new Long(-l.longValue());
         } else {
            return longZero; // interval is validated to be strictly positive,
            // so this guarantees the stubs will always sort into first place.
         }
      }
   };

   private static Comparator getComparator(boolean prioritizeEnabled, ProductConfig productConfig) {
      Comparator c = OrderUtil.orderOrderID;

      if (prioritizeEnabled) {
         HashMap dueMap = DueUtil.buildMap(productConfig,User.SKU_DUE_ALL,null);
         Accessor a = new IntervalAccessor(dueMap);
         Comparator fc = new FieldComparator(a,new ReverseComparator(new NaturalComparator()));
         c = new CompoundComparator(fc,c);
      }

      return c;
   }

// --- construction ---

   public DownloadThread(Table table, OrderManager orderManager, MerchantConfig merchantConfig, DownloadConfig config,
                         File dataDir, File stylesheet, boolean holdInvoice,
                         ProductConfig productConfig, LinkedList coverInfos, boolean enableItemPrice, File localShareDir, Handler handler,
                         TransferTracker tracker, ThreadStatus threadStatus) {
      super(Text.get(DownloadThread.class,"s1"),
            table,
            new EntityManipulator(
               Order.STATUS_ORDER_PENDING,
               Order.STATUS_ORDER_RECEIVING,
               Order.STATUS_ORDER_RECEIVED),
            getComparator(config.prioritizeEnabled,productConfig),
            /* scanFlag = */ false,
            config.idlePollInterval,
            threadStatus);
      doNonEntityFirst = config.prioritizeEnabled;

      this.orderManager = orderManager;
      // no need to copy these, subsystem doesn't modify configs
      this.merchantConfig = merchantConfig;
      this.config = config;
      this.dataDir = dataDir;
      this.stylesheet = stylesheet;
      this.holdInvoice = holdInvoice;
      this.localShareDir = localShareDir;
      this.handler = handler;
      this.tracker = tracker;
      this.pauseAdapter = new PauseAdapter(threadStatus);

      conversionMap = config.conversionEnabled ? Conversion.buildConversionMap(productConfig.conversions) : null;
      bookMap = CoverInfo.buildBookMap(coverInfos);
      this.enableItemPrice = enableItemPrice;
      nextPoll = System.currentTimeMillis();
      // orderFile initialized later, per order
      imageProcess = new ImageProcess(); // this does absolutely nothing until we run into a local image

      this.config.bandwidthConfig.precomputeTrivial();
   }

   protected void doExit() {
      imageProcess.stop();
   }
   // I thought about doing the local image processing in another subsystem,
   // but it's kind of tied to downloading.  for example, if an order
   // mistakenly comes through with local images, we'll need to re-download it,
   // and we can't do that if we've already handed it to the next subsystem.

// --- regulation ---

   // here's the whole theory of regulation.
   //
   // the idea is, we look at the thread as a series of network transactions
   // and sleeps plus some local activity that's basically infinitely fast.
   // we want to regulate between all the transactions and sleeps, for two reasons.
   // first, we want to check and handle scheduled inactivity.  second, after
   // a network transaction we want to introduce some delay to limit the bandwidth.
   //
   // to achieve that, I use a two-pronged approach.  first, at the top level, I define
   // a hook function regulateIsStopping and call it at select places in EntityThread.
   // that breaks the infinite stream of transactions and sleeps into manageable chunks
   // where we call doEntity, doNonEntity, and sleepNice.  so, all we need to do
   // is make sure that doEntity and doNonEntity are properly divided up, and we're set.
   //
   // so, second, within those two functions, I regulate after each network transaction
   // and after each sleep.  that produces a duplicate regulation checkpoint at the end
   // of doEntity and doNonEntity, but that's not too expensive, and I think it's worth
   // it for the code simplicity.
   //
   // the actual regulation is accomplished by BandwidthUtil.regulateIsStopping.
   // it was convenient to combine the regulation check with an isStopping test,
   // but you shouldn't think of them as an unbreakable whole.  for example,
   // we want to regulate network transactions immediately after they occur, even
   // before DiagnoseHandler gets hold of them, so that the potentially infinite
   // pause-retry cycle will stop when we go into a period of scheduled inactivity.
   // however, we don't want to test isStopping there, because if we're successful
   // we need to get back out to the top level and record our success.
   //
   // the right way to think of it is, you can call regulateIsStopping anywhere
   // you want to.  if you were calling isStopping there anyway, great, use the
   // result; if not, just ignore the result and let the preexisting isStopping
   // calls take care of stopping.
   //
   // to put it another way, we already had all the isStopping checks we needed,
   // and the regulateIsStopping function exits promptly if we're stopping, so
   // you can insert a call to regulateIsStopping anywhere, with no bad effects.
   //
   // now, back to the point.  we need to regulate after transactions and sleeps.
   // how do we do that?
   //
   // for transactions, we wrap each network transaction in a RegulatedTransaction.
   // there's also a helper function for that that reduces the number of arguments.
   //
   // for sleeps, we replace the isStopping call afterward with regulateIsStopping.
   // actually there are only three sleeps, so let's look at all of them in detail.
   //
   //  * in DiagnoseHandler, we just do exactly what I said above.
   //
   //  * in DiagnoseCached, we don't do anything.  the sleep is only a one-second
   // synchronization wait, so it's simpler to keep regulation out of the picture.
   // ditto for the tiny network transactions in there.
   //
   //  * and, the surprise annoyance, the sleep in regulateIsStopping is a sleep.
   // if we sleep because of inactivity, we know we're active when we wake up,
   // and we don't need to bother using a bandwidth-limiting sleep at that point
   // because the inactivity sleep was surely long enough.  however, if we sleep
   // for bandwidth limiting, we do want to check after we wake up whether we've
   // landed in an inactive period.  BandwidthUtil.regulateIsStopping handles it.
   //
   // other notes:
   //
   // a side benefit of calling regulateIsStopping at the top level is, if the thread
   // is started during an inactive period, it immediately goes inactive instead of
   // locking up an order or upload.  (it can lock up an order or upload under some
   // other conditions, so that's not super-important, it's just nice behavior to have.)
   //
   // some network transactions send only a tiny amount of data, so the duration
   // is all from latency and server response time, not bandwidth, but we want
   // to regulate afterward anyway to prevent pause-retry cycles.  that will add
   // a little unnecessary time, but no big deal.
   //
   // measuring the transfer duration is similar to what TransferTracker does,
   // but the details are different enough that it's not worth trying to
   // unify them.  for example, when a transaction errors out, TransferTracker
   // ignores it, but for bandwidth limiting we want to add a delay anyway,
   // because it did still use bandwidth.
   //
   // both DownloadThread.download and UploadThread.upload start with an isStopping test.
   // I don't know why that's there, seems like the top-level one ought to be sufficient.
   // what to do?  don't remove it, but don't regulate it either.
   //
   // besides the pause-retry issue, another reason we want to regulate at a low level,
   // inside DiagnoseHandler instead of outside, is that the pause-retry delays should
   // certainly not be counted as bandwidth time.

   public boolean regulateIsStopping() {
      return BandwidthUtil.regulateIsStopping(config.bandwidthConfig,/* lastTransferDuration = */ 0,threadStatus,this);
   }

   private Transaction regulate(Transaction t) {
      return new RegulatedTransaction(config.bandwidthConfig,threadStatus,this,t);
   }

// --- list updater ---

   protected boolean hasNonEntity() {
      return (System.currentTimeMillis() >= nextPoll);
   }

   protected void endNonEntity() {
      nextPoll = System.currentTimeMillis() + config.listPollInterval;
      // putting this in a finally clause is excellent,
      // it handles both error thrashing and slow response times
   }

   private static final String CATEGORY_LIST = "list";

   protected void doNonEntity() throws Exception { // a.k.a. getList

      Log.log(Level.FINE,this,"i1");

      GetList t = new GetList();

      // a standard categorization block
      try {

         if ( ! handler.run(regulate(t),pauseAdapter) ) return; // stopping

      } catch (ThreadStopException e) {
         throw e; // (*)
      } catch (Exception e) {
         e = new CategorizedException(CATEGORY_LIST,Text.get(this,"e9a"),e);

         Log.log(Level.SEVERE,this,"e9b",e);
         threadStatus.error(e);
         // do like EntityThread would if we threw the exception,
         // but with a different message.

         return;
      }
      threadStatus.success(CATEGORY_LIST);

      // now t.results contains a list of Result

      Iterator i = t.results.iterator();
      while (i.hasNext()) {
         Result result = (Result) i.next();

         // (a) fill in the Reportable part of the stub
         OrderStub stub = new OrderStub();
         // orderSeq is null for all downloaded orders
         stub.orderID = result.orderID;
         stub.wholesale = result.wholesale;

         File target = new File(dataDir,result.stringID);

         target = shouldCreate(result.stringID,result.lastUpdated,target,stub);
         if (target == null) continue; // null means don't create

         // ideally, the exists call and the insert call would be atomic.
         // but, it doesn't really matter ... nobody else creates orders.

         // (b) fill in the rest of the stub ... and note that we didn't
         //     know the final value of the target at the time of (a).
         stub.orderDir = target;
         stub.status = Order.STATUS_ORDER_PENDING;
         stub.hold = Order.HOLD_NONE;
         stub.recmodDate = new Date();
         stub.lastUpdated = result.lastUpdated;

         Object lock = table.insert(stub);
         // once this succeeds, we must either finish successfully or delete

         try {

            FileUtil.makeDirectory(stub.orderDir);

            table.release(stub,lock);
            lock = null;

            Log.log(Level.INFO,this,"i2",new Object[] { result.stringID });

         } finally {
            if (lock != null) {
               table.delete(stub,lock);
               lock = null;
            }
         }
      }
   }

// --- rebuild function ---

   // normally, we see an order in the list, download it,
   // and then send a status transaction that removes it from the list.
   // if the user goes to the web site and hits rebuild, though,
   // the order will be rebuilt, and will then reappear in the list.
   // in that case, we should purge the local order and start again.
   //
   // this is complicated by the possibility of an old incomplete purge,
   // which we want to catch here so that it's categorized correctly
   // (as opposed to letting the FileUtil.makeDirectory call fail, above).

   // some notes about the new directory-varying function:
   //
   // I liked the old behavior, which was to report an "incomplete purge"
   // error until the user cleaned up the directory full of garbage.
   // unfortunately, that seemed to cause too many calls to tech support,
   // so we're changing it; and now the disk can fill up with garbage.
   //
   // on the other hand, the old behavior only pushed the user to clean up
   // when the garbage was interfering with a rebuilt order, so most of it
   // was already going unnoticed, and a little more can't hurt.
   //
   // I've semi-arbitrarily decided not to call purgeRetry on variant dirs.
   // the other possibility is to integrate it into the vary function,
   // so that we try to purge each directory before moving on to the next.
   // why not call it?  it's actually rude to call purgeRetry at all,
   // because it's a case where LC acts on a directory it doesn't own yet;
   // and I guess calling it on a variant dir just seemed like too much.
   //
   // as a result, variant dirs, once created, are slightly more prone to
   // turn into garbage.  I don't think it will matter, though.

   private static String reasonIncomplete(boolean existed, File target) {
      // put together diagnostic information to see why incompletes are happening

      StringBuffer buffer = new StringBuffer();

      buffer.append(Text.get(DownloadThread.class,"e17",new Object[] { new Integer(existed ? 1 : 0) }));
      dump(buffer,target,0);

      return buffer.toString();
   }

   private static void dump(StringBuffer buffer, File file, int depth) {

      for (int i=0; i<depth; i++) buffer.append("   ");

      buffer.append( (depth == 0) ? Convert.fromFile(file) : file.getName() );

      boolean dir = file.isDirectory();
      if ( ! dir ) buffer.append(" (" + file.length() + ")");

      buffer.append("\n");

      if (dir) {
         File[] files = file.listFiles();
         for (int i=0; i<files.length; i++) dump(buffer,files[i],depth+1);
      }
   }

   private static File vary(File target) {

      File parent = target.getParentFile();
      String name = target.getName();

      for (int n=2; ; n++) {
         File f = new File(parent,name + "_" + Convert.fromInt(n));
         if ( ! f.exists() ) return f;
      }
      // guaranteed to terminate, directory is finite
   }

   /**
    * @return The desired target, or null if we shouldn't try to create the order.
    */
   private File shouldCreate(String key, String lastUpdated, File target, Reportable reportable) throws Exception {

      // don't categorize table exceptions

      boolean exists = table.exists(key);
      if (exists) {

         // is it in the list because the download hasn't completed?
         //
         OrderStub stub = (OrderStub) table.get(key);
         if (stub.status < Order.STATUS_ORDER_RECEIVED) {

            if (    stub.lastUpdated == null
                 || stub.lastUpdated.equals(lastUpdated) ) return null; // don't create, already working on it

            // else the order has been updated (rebuilt)
            // and we should stop this download and start over
            //
            // notes:
            //
            // stub.lastUpdated is only null for old orders from before
            // LC started tracking that information.  in that case just
            // keep on as before and don't notice updates.
            //
            // actually, stub.lastUpdated is also null for orders after
            // they've downloaded, so the if-test order is important.
            //
            // the lastUpdated argument that comes in can never be null.
            //
            // if the stub object is really just a stub, it'd be nice to
            // just rewrite the lastUpdated field and save, but it's not
            // worth the hassle of changing the logic.
         }

         // we don't have it locked, so in theory the status could change, but ...
         // it can't go backward, and it only becomes received in this thread.
      }

      // ok, we want to purge whatever's there, if anything, and then (re)create

      // a standard categorization block
      try {

         if (exists) {
            switch (orderManager.purgeSingle(key,null,null,Purge.REASON_REBUILD,reportable,/* verify = */ false)) {

            case Purge.RESULT_FAILED:
               throw new Exception(Text.get(this,"e11"));

            // we want RESULT_INCOMPLETE to vary the directory until it finds an
            // unused one, but rather than do it here, just fall through and let
            // the code later on do it.  that lets us retry the purge, too.

            // of course we want RESULT_COMPLETE to fall through and continue
            }
         }

         // incomplete purges sometimes involve invoice.html (or similar label files),
         // not sure why.  best guess, the UI was generating (or reading)
         // the invoice file when the purge happened, so the file couldn't be deleted.
         //
         // one way to fix that would be to make the UI get a lock before showing the
         // invoice, but I don't like that ... for one thing, I'm not entirely sure
         // that's the problem; for another, by that logic I should put a lock on the
         // read-only order dialog, since it uses image files; and for another, I don't
         // like the idea of the UI locking up an order for extended periods, since it
         // needs to be unlocked for e.g. job updates to occur.
         //
         // we tried waiting and purging again (without any information about the order
         // that had been there) at the next order list read, but that didn't help much.
         //
         // anyway, now the plan is to vary the directory instead of erroring out.

         if (target.exists()) { // maybe an old incomplete purge?

            String reason = reasonIncomplete(exists,target);
            // get this before we change the target

            target = vary(target);
            // vary the directory until we find one we can use

            Log.log(reportable,Level.WARNING,this,"e18",new Object[] { key, target.getName(), reason });
         }

      } catch (ThreadStopException e) {
         throw e; // (*)
      } catch (Exception e) {
         e = new CategorizedException(key,Text.get(this,"e10a",new Object[] { key }),e);

         Log.log(reportable,Level.SEVERE,this,"e10b",e);
         threadStatus.error(e);
         // do like EntityThread would if we threw the exception,
         // but with a different message.  more importantly,
         // don't abort the processing of the rest of the list.

         // about the reportable object ... it's a little confusing
         // because in some cases we also have an existing order
         // with the same ID available.  if you look at the messages, though,
         // you'll see they refer to the new object, not the old one.

         return null; // don't create, purge failed
      }
      threadStatus.success(key);

      return target; // create
   }

   // (*) note on thread-stop exceptions and categorization.
   // it's unfortunate that the design makes the two incompatible,
   // but there's actually no harm done ... the only time
   // that categorization matters is when a task succeeds,
   // and no tasks are succeeding if the thread is stopped.

// --- download functions ---

   protected boolean doOrder() throws Exception {

      try {
         tracker.groupBegin1(order);

         return download();

      } finally {
         tracker.groupEnd();
      }
   }

   /**
    * @return True if the download is complete.
    *         False if the download is incomplete because the thread is stopping.
    *         If the download is incomplete because of an error,
    *         that will be reported via an exception.
    */
   private boolean download() throws Exception {

      if (isStopping()) return false;
      // don't stop in the middle of a transaction, only between them

      Log.log(Level.INFO,this,"i3",new Object[] { order.getFullID() });

      Date prevAttempt = order.lastAttempt;
      order.lastAttempt = new Date();
      // we want to set the official attempt time now,
      // but we need the previous value for our logic

      long threshold = order.lastAttempt.getTime() - config.startOverInterval;
         // lastAttempt contains current time

      // now, do we want to go back to being a stub?
      // there are many conditions

      if (    config.startOverEnabled             // start-over turned on
           && order instanceof Order              // not a stub
           && prevAttempt != null                 // not very first attempt
           && prevAttempt.getTime() >= threshold  // (*)
           && order.lastProgress != null          // a formality -- if no progress, then we're still an original stub
           && order.lastProgress.getTime() < threshold ) {
                                                  // no progress since start-over interval

         // the condition (*) needs more than a one-line explanation.
         // the concept here is, you'll set the order to pause and retry
         // every so often, and then tell it to purge and start over
         // less frequently.  so, if an order is pausing and retrying normally,
         // the condition will always be met.
         //
         // if, however, an order passes the pause-retry limit, or perhaps
         // is put on manual hold for some reason, the last attempt might
         // have been long ago.  in that case, we'd like to be able to unpause
         // the order without immediately starting over, and the condition (*)
         // gives us one shot at it.  two or three shots might be better,
         // but that starts to make things more complicated than they're worth.
         //
         // a nice side-effect of the way I've set it up is, it's guaranteed
         // that we'll never start over if the last attempt made progress.
         // when there was progress, we'll have lastProgress >= prevAttempt,
         // but then (*) implies lastProgress >= threshold, so the test for
         // lastProgress < threshold will fail.

         // ok, we're ready to make the order back into a stub

         Log.log(Level.INFO,this,"i11");

         OrderStub stub = new OrderStub();
         stub.copyFrom(order);

         stub.recmodDate = new Date();
         table.update(stub,lock);

         OrderStub orderOld = order;
         entity = order = stub; // don't accept new object until update succeeds

         if ( ! orderManager.purgeFiles(orderOld,/* rmdir = */ false) ) {
            Log.log(orderOld,Level.WARNING,this,"e19",new Object[] { orderOld.getFullID() });
         }
         // as usual, we don't have any good way to put the order table write
         // and the file purge into an atomic transaction.  fortunately, here
         // it doesn't matter much .. if we can't delete a file, for whatever
         // mysterious reason, it doesn't matter, we'll just come back and
         // overwrite it later (unless the order changes to have a different set
         // of files, which is rare); and if there are extra files that we don't
         // know how to delete, it doesn't matter, because we're not trying to
         // clean up the directory yet, just the files.
         //
         // the only thing that does matter is that we do the files second,
         // since it would be bad to delete some files and leave the order
         // pointing to nonexistent files.

         // it wouldn't make sense to set order.lastProgress here ...
         // going back to a stub is hardly a step forward.
         // fortunately, we don't need to, because the order will
         // be a stub (and hence not start over) until we do make
         // some progress.
      }

      if ( ! (order instanceof Order) ) { // need to download order file

         Log.log(Level.INFO,this,"i4");

         DownloadOrder t1 = new DownloadOrder();
         ParseOrder t2 = new ParseOrder(t1.getFile());

         TransactionGroup t = new TransactionGroup();
         t.add(regulate(t1));
         t.add(t2);
         if ( ! handler.run(t,pauseAdapter) ) return false;

         // t1 and t2 have to be grouped so that a parse failure
         // will cause a re-read of the order XML file.
         // the problem is, network conditions can cause truncation,
         // and we have no way of checking the size,
         // so we only notice when the parse throws an error.

         Order orderNew = t2.result;

         orderNew.recmodDate = orderNew.lastProgress = new Date();
         table.update(orderNew,lock);

         entity = order = orderNew; // don't accept new object until update succeeds

         if (config.prioritizeEnabled) return false;
         // if we're prioritizing, we process order stubs before orders,
         // but we have to bail here so that we can get all the stubs
         // before deciding which order really has the highest priority.
         // don't even tell the tracker we have items.
      }

      if (isStopping()) return false;

      tracker.groupBegin2(order);
      // now that we've got the order record, show it in the tracker

      boolean complete = false;
      try {
         tracker.timeBegin();

         if ( ! downloadItems() ) return false;

         complete = true;
      } finally {
         tracker.timeEnd(complete);
      }

      Log.log(Level.INFO,this,"i6");

      if ( ! handler.run(regulate(new StatusTransaction(order,Order.STATUS_ORDER_RECEIVED,merchantConfig,config)),pauseAdapter) ) return false;
      // don't use order.status, it's not what you think
      // the order is not received until <i>after</i> this transaction has completed.
      // local orders shouldn't be here, but if they are,
      // StatusTransaction will catch it and fail, no need to put another check here.

      // we just made progress, yes, but now we're done
      order.lastUpdated = null;
      order.lastAttempt = null;
      order.lastProgress = null;

      Log.log(Level.INFO,this,"i7");
      order.endRetry(this,"i10"); // operation is complete, clear retry info

      // the return (below) goes back to OrderThread.doEntity,
      // which immediately goes back to EntityThread.doEntityStatus,
      // which then immediately updates the order status.
      // in other words, there are no failure points after this.
      // so, if we want to hold before invoice, now's the time.
      //
      if (holdInvoice) {
         order.hold = Order.HOLD_INVOICE;
         Log.log(Level.INFO,this,"i8");
      }

      User.tell(User.CODE_ORDER_ARRIVED,Text.get(DownloadThread.class,"i9",new Object[] { order.getFullID() }));
      // this kind of goes with the i7 log entry above,
      // but I like it better if it comes at the very end.
      // do it now instead of when the order becomes
      // ready to print, since if holdInvoice is set,
      // there might not be any further processing at all.

      return true;
   }

   private boolean downloadItems() throws Exception {

      Iterator i = ((Order) order).files.iterator();
      while (i.hasNext()) {
         orderFile = (Order.OrderFile) i.next();
         if (    orderFile.status == Order.STATUS_ITEM_PENDING
              || orderFile.status == Order.STATUS_ITEM_RECEIVING ) {
         // latter case shouldn't happen

            Log.log(Level.FINE,this,"i5",new Object[] { orderFile.filename });

            if ( ! downloadItem() ) return false;
         }

         if (isStopping()) return false;
      }

      return true;
   }

   private boolean downloadItem() throws Exception {

      tracker.fileBegin1(orderFile);
      tracker.fileBegin2(orderFile,order,/* sizeActual = */ (orderFile.size != null) ? null : new Long(0));
      setFileStatus(Order.STATUS_ITEM_RECEIVING);
      //
      // if we know the file size, we want sizeActual to be null, since we're not scaling the size.
      // if we don't know it, we'll set the true value of sizeActual in the content length handler,
      // but use zero for now so that we don't show the SIZE_ESTIMATE value from DownloadSubsystem
      // even for a second.

      try {

      // download

         if ( ! downloadItemSwitch() ) {

            tracker.fileEndIncomplete();
            setFileStatus(Order.STATUS_ITEM_PENDING);
            return false;
         }

      // done

         order.lastProgress = new Date();

         tracker.fileEndComplete();
         setFileStatus(Order.STATUS_ITEM_RECEIVED);
         return true;

      } catch (Exception e) {

         tracker.fileEndIncomplete();
         setFileStatus(Order.STATUS_ITEM_PENDING);
         throw e;
      }
   }

   private boolean downloadItemSwitch() throws Exception {
      final String prefixLocal = "local:";
      if (orderFile.downloadURL.startsWith(prefixLocal)) {
         return downloadItemLocal(orderFile.downloadURL.substring(prefixLocal.length()));
      } else {
         return downloadItemNormal();
      }
   }

   private boolean downloadItemLocal(String queryString) throws Exception {

      if (localShareDir == null) throw new Exception(Text.get(this,"e22"));

      imageProcess.call(ImageProcess.TOKEN_GENERATE,new String[] {
            Convert.fromFile(localShareDir),
            queryString,
            Convert.fromFile(new File(order.orderDir,orderFile.filename))
         });
      // ignore result, we know it's empty
      //
      // passing localShareDir every time might seem odd,
      // but it's nice for the protocol to have no state.

      imageProcess.call(ImageProcess.TOKEN_GC,new String[0]);
      // ignore result, just the before/after memory sizes
      //
      // some experimental facts and notes that explain why I do this.
      // * this does have an effect, the reported size goes down
      // * the same thing probably happens next time the process runs, but
      //   it doesn't happen when the process is sitting around being idle
      // * it's fast and harmless
      // * the process size in task manager does *not* go down
      // * but, folks online seem to think that unused heap space could be
      //   reclaimed by the OS under some conditions, so maybe it's useful

      return true;
   }

   private boolean downloadItemNormal() throws Exception {

      DownloadItem downloadItem = new DownloadItem();
      if (orderFile.size == null) {
         downloadItem.setContentLengthHandler(this); // get sizeActual from HTTP header
      }
      // else size is known, no need to check header, and in fact it might not be there

      return handler.run(regulate(downloadItem),pauseAdapter);
   }

   public void handle(long contentLength) {
      tracker.setSizeActual(new Long(contentLength));
   }

   private void setFileStatus(int status) throws TableException {

      orderFile.status = status;

      Iterator i = ((Order) order).items.iterator();
      while (i.hasNext()) {
         Order.Item item = (Order.Item) i.next();

         if (item.isMultiImage()) {
            if (item.filenames.contains(orderFile.filename)) {
               item.status = adjust(item,status);
            }
         } else {
            if (item.filename .equals  (orderFile.filename)) {
               item.status = status;
            }
         }
         // all items containing the file get updated
      }

      order.recmodDate = new Date();
      table.update(order,lock);
   }

   private int adjust(Order.Item item, int status) {

      // the idea here is similar to JobManager.computeOrderStatus.
      //
      // first,  if all files are RECEIVED,  the item is RECEIVED;
      // second, if any files are RECEIVING, the item is RECEIVING;
      // otherwise                           the item is PENDING.
      //
      // however, rather than compute the whole thing every time,
      // we take one shortcut.  we know which file is really being
      // operated on, and the status argument tells its new status.
      // so, any other file that says it's RECEIVING is wrong,
      // and is just a relic from a previous LC mid-download crash.
      //
      // (since files are downloaded in order, I think that case
      // is really completely impossible, even when you take into
      // account the fact that we used to iterate over items, but
      // I'm not basing my argument on that)
      //
      // so, the point is, not only RECEIVING but also PENDING
      // passes straight through from file status to item status.
      // for RECEIVED we still have to do the computation,
      // but we can simplify that too by ignoring RECEIVING ones.

      if (status != Order.STATUS_ITEM_RECEIVED) return status;

      Iterator i = item.filenames.iterator();
      while (i.hasNext()) {
         String filename = (String) i.next();
         Order.OrderFile file = ((Order) order).findFileByFilename(filename);
         if (file.status != Order.STATUS_ITEM_RECEIVED) return Order.STATUS_ITEM_PENDING;
      }

      return Order.STATUS_ITEM_RECEIVED;
   }

// --- transaction helpers ---

   public static void throwInvalidPassword() throws Exception {
      User.tell(User.CODE_INVALID_PASSWORD,Text.get(DownloadThread.class,"e15"));
      throw new ThreadStopException(Text.get(DownloadThread.class,"e7b"));
      // thread stop exception doesn't go on order, no need to use ToldException
   }

   private static class Result {
      public int orderID;
      public String stringID;
      public String lastUpdated;
      public OrderStub.Wholesale wholesale;
   }

   /**
    * Standard database addition of nullable integers.
    */
   private static Long addNullable(Long l, int i) {
      return (l != null) ? new Long(l.longValue() + i) : null;
   }

// --- HTTP transactions ---

   // note, these are *not* static transactions, they use DownloadThread fields

   private class GetList extends GetTransaction {
      public LinkedList results;

      public String describe() { return Text.get(DownloadThread.class,"s2"); }
      protected String getFixedURL() { return config.listURL; }
      protected void getParameters(Query query) throws IOException {
         if (merchantConfig.isWholesale) {
            query.add("wholesalerID",Convert.fromInt(merchantConfig.merchant));
         } else {
            query.add("MLRFNBR",Convert.fromInt(merchantConfig.merchant));
         }
         query.addPasswordObfuscate("encpassword",merchantConfig.password);
         // if you're wondering, MLRFNBR is Merchant Location ReFerence NumBeR
      }

      public boolean run(PauseCallback callback) throws Exception {
         if (merchantConfig.password.length() == 0) throwInvalidPassword();
         return super.run(callback);
         // go ahead and detect codeMissing, don't even send to server.
         // there's no harm in sending, actually; I'm just detecting it
         // so that the error will occur sooner rather than later -- in
         // StatusTransaction.
      }

      protected boolean receive(InputStream inputStream) throws Exception {
         results = new LinkedList();

         Document doc = XML.readStream(inputStream);

      // check for error

         Node error = XML.getElementTry(doc,"ERROR");
         if (error != null) {

            String text = XML.getText(error);
            int code = 0;
            boolean parsed = false;

            int i = text.indexOf(',');
            if (i != -1) try {
               code = Convert.toInt(text.substring(0,i)); // this is what fails
               parsed = true;
               text = text.substring(i+1).trim();
            } catch (Exception e) {
               // leave variables as they were
            }

            final int codeMerchant = 1;
            final int codePassword = 2;

                 if (parsed && code == codeMerchant) throw new ThreadStopException(Text.get(DownloadThread.class,"e7a"));
            else if (parsed && code == codePassword) throwInvalidPassword();
            else {
               throw new IOException(Text.get(DownloadThread.class,(parsed ? "e8b" : "e8a"),new Object[] { Convert.fromInt(code), text }));
            }
         }

      // get list

         Node list = XML.getElement(doc,"OrderList");

         if (merchantConfig.isWholesale) { // WSC_WHOLESALE

            receiveNested(list,"Location");

         } else { // WSC_NORMAL, WSC_PSEUDO, and WSC_PRO

            Iterator i = XML.getElements(list,"Order");
            while (i.hasNext()) {
               Node node = (Node) i.next();

               // here the LC order ID is the merchant order ID
               Result result = new Result();
               result.stringID = XML.getAttribute(node,"ID");
               result.orderID = Convert.toInt(result.stringID);
               result.lastUpdated = XML.getAttribute(node,"LastUpdated");
               result.wholesale = null;

               if (merchantConfig.wholesaleMerchantName != null) { // WSC_PSEUDO

                  OrderStub.Wholesale w = new OrderStub.Wholesale();
                  w.merchantName = merchantConfig.wholesaleMerchantName;
                  w.merchant = merchantConfig.merchant;
                  w.merchantOrderID = result.orderID; // copy before overwrite!

                  result.stringID = XML.getAttribute(node,"LifePicsOrderID");
                  result.orderID = Convert.toInt(result.stringID);
                  result.wholesale = w;
               }

               results.add(result);
            }

            receiveNested(list,"Pro"); // WSC_PRO
         }

         return true;
      }

      private void receiveNested(Node list, String name) throws Exception {

         Iterator j = XML.getElements(list,name);
         while (j.hasNext()) {
            Node loc = (Node) j.next();

            OrderStub.Wholesale w = new OrderStub.Wholesale();
            w.merchantName = XML.getAttribute(loc,"Name");
            w.merchant = Convert.toInt(XML.getAttribute(loc,"ID"));
            // merchantOrderID depends on the order

            Iterator i = XML.getElements(loc,"Order");
            while (i.hasNext()) {
               Node node = (Node) i.next();

               // here the LC order ID is the global order ID
               Result result = new Result();
               result.stringID = XML.getAttribute(node,"LPOrderID");
               result.orderID = Convert.toInt(result.stringID);
               result.lastUpdated = XML.getAttribute(node,"LastUpdated");
               result.wholesale = w.copy();
               result.wholesale.merchantOrderID = Convert.toInt(XML.getAttribute(node,"ID"));

               results.add(result);
            }
         }
      }
   }

   private class DownloadOrder extends DownloadTransaction {

      public DownloadOrder() { super(/* overwrite = */ true); }

      public String describe() { return Text.get(DownloadThread.class,"s3"); }
      protected String getFixedURL() { return config.opieFlag ? config.opieURL : config.orderURL; }
      protected void getParameters(Query query) throws IOException {

         // usually we'd test order.wholesale and then ignore orderSeq in the
         // wholesale case since it's validated null.  that would work here,
         // except I want the error message for wholesaler local orders to be
         // this one, not the unhelpful one from getWholesaleCode.
         if (order.orderSeq != null) throw new IOException(Text.get(DownloadThread.class,"e21"));

         int wsc = order.getWholesaleCode(merchantConfig.merchant,merchantConfig.isWholesale);
         switch (wsc) {
         case Order.WSC_NORMAL:
            query.add("order",Convert.fromInt(order.orderID));
            query.add("MLRFNBR",Convert.fromInt(merchantConfig.merchant));
            break;
         case Order.WSC_PSEUDO:
            query.add("order",Convert.fromInt(order.wholesale.merchantOrderID));
            query.add("MLRFNBR",Convert.fromInt(merchantConfig.merchant));
            // order.orderID is the LifePics ID, don't use that!
            // doesn't matter which mlrfnbr we use, they're the same here
            break;
         case Order.WSC_PRO:
            query.add("order",Convert.fromInt(order.wholesale.merchantOrderID));
            query.add("MLRFNBR",Convert.fromInt(order.wholesale.merchant));
            query.add("dealerLocID",Convert.fromInt(merchantConfig.merchant));
            break;
         case Order.WSC_WHOLESALE:
            query.add("order",Convert.fromInt(order.wholesale.merchantOrderID));
            query.add("MLRFNBR",Convert.fromInt(order.wholesale.merchant));
            query.add("wholesalerID",Convert.fromInt(merchantConfig.merchant));
            break;
         }
         query.addPasswordObfuscate("encpassword",merchantConfig.password);
      }

      public File getFile() { return new File(order.orderDir,Order.ORDER_FILE); }
      protected FileUtil.Callback getCallback() { return null; }
      // the order file doesn't count, and anyway we don't know anything yet

      protected void writePrefix(OutputStream dest) throws IOException {
         OrderParser.writePrefix(dest,stylesheet);
      }
   }

   private class ParseOrder extends Transaction {
      public Order result;

      private File file;
      public ParseOrder(File file) { this.file = file; }

      public String describe() { return Text.get(DownloadThread.class,"s5"); }

      public boolean run(PauseCallback callback) throws Exception {

         result = new Order();
         result.copyFrom(order);

         int wsc = result.getWholesaleCode(merchantConfig.merchant,merchantConfig.isWholesale);
         OrderParser p = new OrderParser(result,wsc,conversionMap,bookMap,enableItemPrice,/* localOrder = */ false,/* summaryMap = */ null);
         Document doc;

         try {
            doc = OrderParser.readFile(file);
            XML.loadDoc(doc,p,OrderParser.NAME_ORDER);
            p.redisambiguate();
         } catch (Exception e) { // could be more specific
            throw new RetryableException(Text.get(DownloadThread.class,"e14"),e);
         }

         if (p.needRewrite())
         try {
            AlternatingFile af = new AlternatingFile(file);
            try {
               OutputStream dest = af.beginWrite();
               OrderParser.writePrefix(dest,stylesheet);
               XML.writeStream(dest,doc);
               af.commitWrite();
            } finally {
               af.endWrite();
            }
         } catch (Exception e) {
            throw new Exception(Text.get(DownloadThread.class,"e20"),e);
         }
         // no need to retry for this one, but custom text is nice

         return true;
      }
   }

   private class DownloadItem extends DownloadTransaction {

      public DownloadItem() {
         super( /* overwrite    = */ true,
                /* expectedSize = */ orderFile.size,
                /* acceptedSize = */ addNullable(orderFile.size,1) ); // order file currently misreports by 1, but don't require that
      }

      public String describe() {

         int timeout = NetUtil.getDefaultTimeout(); // in millis
         timeout = (timeout + 999) / 1000; // in seconds, rounded up so as not to print zero

         return Text.get(DownloadThread.class,"s4",new Object[] { orderFile.filename, getFixedURL(), Convert.fromInt(timeout) });
      }

      protected String getFixedURL() { return orderFile.downloadURL; }
      // no parameters, downloadURL should contain whatever is necessary

      public File getFile() { return new File(order.orderDir,orderFile.filename); }
      protected FileUtil.Callback getCallback() { return tracker; }

      // note, getFile produces flat format.  we have to do it that way,
      // otherwise we'd have all the complications of reformatting here.
      //
      // the format always starts out flat, and only changes after download.
      // if the status ever goes backward, the format must be reset to flat.
   }

}

