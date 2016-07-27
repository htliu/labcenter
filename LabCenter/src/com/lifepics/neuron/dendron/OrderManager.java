/*
 * OrderManager.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.ChainedException;
import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Log;
import com.lifepics.neuron.core.Pop;
import com.lifepics.neuron.core.Reportable;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.gui.HTMLViewer;
import com.lifepics.neuron.gui.Print;
import com.lifepics.neuron.meta.Selector;
import com.lifepics.neuron.misc.FileUtil;
import com.lifepics.neuron.misc.Purge;
import com.lifepics.neuron.net.DefaultHandler;
import com.lifepics.neuron.net.Handler;
import com.lifepics.neuron.net.MerchantConfig;
import com.lifepics.neuron.net.PauseCallback;
import com.lifepics.neuron.table.LockException;
import com.lifepics.neuron.table.Table;
import com.lifepics.neuron.table.View;
import com.lifepics.neuron.thread.StopDialog;
import com.lifepics.neuron.thread.Subsystem;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.logging.Level;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * The class that knows how to do most things with orders.
 * (The {@link Order} class is mostly just passive data.)
 */

public class OrderManager {

// --- fields ---

   private Table table;
   private MerchantConfig merchantConfig;
   private DownloadConfig downloadConfig;
   private File dataDir; // used for unformatting
   private File stylesheet;
   private File postageXSL;
   private boolean warnNotPrinted;
   private boolean enableTracking;

   public JobManager jobManager; // set later

// --- construction ---

   public OrderManager(Table table, MerchantConfig merchantConfig, DownloadConfig downloadConfig, File dataDir, File stylesheet, File postageXSL, boolean warnNotPrinted, boolean enableTracking) {
      this.table = table;
      this.merchantConfig = merchantConfig;
      this.downloadConfig = downloadConfig;
      this.dataDir = dataDir;
      this.stylesheet = stylesheet;
      this.postageXSL = postageXSL;
      this.warnNotPrinted = warnNotPrinted;
      this.enableTracking = enableTracking;
   }

   public void reinit(MerchantConfig merchantConfig, DownloadConfig downloadConfig, File dataDir, File stylesheet, boolean warnNotPrinted, boolean enableTracking) {
      this.merchantConfig = merchantConfig;
      this.downloadConfig = downloadConfig;
      this.dataDir = dataDir;
      this.stylesheet = stylesheet;
      this.warnNotPrinted = warnNotPrinted;
      this.enableTracking = enableTracking;
   }

// --- helpers ---

   public Table getTable() { return table; }

   public static String getKey(Object o) {
      return ((OrderStub) o).getFullID();
   }

   public static String[] getKeys(Object[] o) {

      String[] keys = new String[o.length];
      for (int i=0; i<o.length; i++) {
         keys[i] = getKey(o[i]);
      }

      return keys;
   }

// --- open ---

   public void open(Window owner, Object o) throws Exception {

   // take care of the OrderStub case first
   // read-only is not even a question here, since there are no modifiable fields

      if ( ! (o instanceof Order) ) {
         OrderStubDialog.create(owner,(OrderStub) o).run();
         return;
      }

   // main case

      Order order;
      boolean readonly;

      Object lock = table.lockTry(o);
      try {

         if (lock != null) {

            order = (Order) table.get(o); // get latest copy
            readonly = (order.format == Order.FORMAT_FLAT);

            // for orders, the only editable field is the format,
            // and now it's only editable until you change it to flat.
            // it would be OK to leave it as editable, since the
            // dialog hides the combo, but the mode also affects the
            // buttons that appear at the bottom of the EditDialog.

            if (readonly) {
               table.release(o,lock); // can use "o", only key matters
               lock = null;
            }
            // have to release lock, otherwise send to queues won't work

         } else {
            order = (Order) o;
            readonly = true;
         }

         int formatBefore = order.format;
         //
         boolean ok = OrderDialog.create(owner,order,this,jobManager,readonly,/* tracking = */ false).run();
         if ( ! ok ) return; // we do have to check this - format could be changed
         // by an unsuccessful OK followed by a Cancel, at least in theory
         //
         int formatAfter  = order.format;

         if (formatAfter != formatBefore) {

            if (formatAfter != Order.FORMAT_FLAT) throw new Exception(Text.get(this,"e13"));

            // note, as a precondition for calling unformat,
            // we also need to know that formatBefore != Order.FORMAT_FLAT.
            // but, we do know that, because else you can't edit the field.

            FormatThread formatThread = new FormatThread(order);
            formatThread.start();
            StopDialog.joinSafe(formatThread,owner);

            if (formatThread.shouldUpdate) {
               table.update(order,lock);
               // note, format change doesn't affect recmod date
            }

            formatThread.rethrow();
         }

      } finally {
         if (lock != null) table.release(o,lock); // can use "o", only key matters
      }
   }

// --- format thread ---

   private class FormatThread extends Thread {

      private Order order;
      private Exception exception;
      public boolean shouldUpdate;

      public FormatThread(Order order) {
         super(Text.get(OrderManager.class,"s1"));
         this.order = order;
         // exception starts out null
         shouldUpdate = false;
      }

      public void run() {
         try {

            Unformat.unformat(order,dataDir);
            order.format = Order.FORMAT_FLAT;

            shouldUpdate = true;

         } catch (Exception e) {
            exception = e;
         }
      }

      /**
       * Transfer exceptions back to main thread.
       */
      public void rethrow() throws Exception {
         if (exception != null) throw exception;
      }
   }

// --- hold and release ---

   /**
    * @return If the hold failed because of a lock, the thread that owns (or owned) the lock.
    */
   public Thread hold(String key) {
      try {
         Object lock = table.lock(key);
         try {
            OrderStub order = (OrderStub) table.get(key); // get latest copy

            if (    order.hold != Order.HOLD_USER // allow convert ERROR into USER
                 && order.status < Order.STATUS_ORDER_COMPLETED ) {

               order.hold = Order.HOLD_USER;

               order.recmodDate = new Date();
               table.update(order,lock);

               Log.log(Level.INFO,this,"i1",new Object[] { key });
            }
         } finally {
            table.release(key,lock);
         }
      } catch (LockException e) {
         return e.getLockThread();
      } catch (Exception e) {
         // ignore, user will see result
      }
      return null;
   }

   public void release(String key) {
      try {
         Object lock = table.lock(key);
         try {
            OrderStub order = (OrderStub) table.get(key); // get latest copy

            if (order.hold != Order.HOLD_NONE) {

               int holdSave = order.hold;

               order.hold = Order.HOLD_NONE;
               order.lastError = null;

               order.recmodDate = new Date();
               table.update(order,lock);

               Log.log(Level.INFO,this,"i2",new Object[] { key, OrderEnum.fromHold(holdSave) });
            }
         } finally {
            table.release(key,lock);
         }
      } catch (Exception e) {
         // ignore, user will see result
      }
   }

// --- tracking ---

   // OrderDialog doesn't work on locked orders because of the "send to queues" function,
   // so to edit the tracking we use optimistic locking -- show the fields on the screen,
   // let the user edit them, then hope we can acquire the lock at the end.

   public void updateTracking(String key, String carrier, String trackingNumber) throws Exception {

      Object lock = table.lock(key); // the main point of failure
      try {
         Order order = (Order) table.get(key); // get latest copy
         // OrderStub isn't possible, so class cast exception is a fine way to handle it

         // no need to check for changes, OrderDialog already does that

         order.carrier = carrier;
         order.trackingNumber = trackingNumber;

         order.recmodDate = new Date();
         table.update(order,lock);

      } finally {
         table.release(key,lock);
      }
   }

   public void completeWithTracking(Window owner, Object o) throws Exception {

      if (enableTracking && o instanceof Order && ((Order) o).isAnyShipToHome()) { // same as AutoCompleteThread exclusion

         Order order = (Order) o;

         if (order.status <  Order.STATUS_ORDER_INVOICED ) throw new Exception(Text.get(this,"e25"));
         if (order.status >= Order.STATUS_ORDER_COMPLETED) throw new Exception(Text.get(this,"e26"));
         //
         // these two conditions are partly redundant with updateStatusWithLock
         // (where we check the second and also a weaker version of the first),
         // but we need to check them now because the dialog behavior depends on them.
         // if the first condition fails, the dialog won't show the tracking area,
         // and if the second fails, it won't be editable.  I think the UI blocks
         // all of these except for completing a completed order via Queue Management,
         // but I want to be certain.
         //
         // also note that we're testing an unlocked object.  we could pull the test
         // for warnNotPrinted up here (since it only fires on complete and on owner
         // not null) but then it wouldn't be as solid.

         OrderDialog dialog = OrderDialog.create(owner,order,this,jobManager,/* readonly = */ true,/* tracking = */ true);
         dialog.run(); // always returns false
         if ( ! dialog.getTrackingResult() ) return; // tracking number canceled
         //
         // see OrderDialog for the full explanation of why we use readonly mode here.
         //
         // if the tracking result is true, that means the order object has the right
         // tracking info, which may have been updated just now or entered previously.
         //
         // in the first case, the object o is *not* updated (didn't seem right to do
         // in readonly mode), but the table is, and we only pass the key to complete.
         //
         // in the second case, why do we run the dialog at all?  because it would be
         // too weird and confusing to have it sometimes appear and sometimes not.
         // also it doesn't hurt to check the tracking number before submit to server.
         //
         // the tracking number update, if any, is separate from the order completion,
         // so even if that fails, the number will still be around for next time, and
         // you'll be able to see that because we run the dialog again.
      }

      complete(owner,getKey(o));
   }

// --- complete and abort ---

   // the little functions just make sure that
   // nobody does this with any other status value

   public void complete(Window owner, String key) throws Exception { updateStatus(owner,key,Order.STATUS_ORDER_COMPLETED); }
   public void abort   (Window owner, String key) throws Exception { updateStatus(owner,key,Order.STATUS_ORDER_ABORTED  ); }
   public void cancel  (Window owner, String key) throws Exception { updateStatus(owner,key,Order.STATUS_ORDER_CANCELED ); }

   public Exception completeWithLock(OrderStub order, Object lock, Handler handler, PauseCallback callback) throws Exception { return updateStatusWithLock(null,null,order,lock,Order.STATUS_ORDER_COMPLETED,handler,callback); }


   private static final Handler defaultHandler = new DefaultHandler();
   // note, we can only use a static handler because a default handler has no state!

   public static final Exception stopping = new Exception();
   // a special exception code that means the operation is stopping without warning or error


   private void updateStatus(Window owner, String key, int statusGoal) throws Exception {
      Object lock = table.lock(key);
      try {
         OrderStub order = (OrderStub) table.get(key); // get latest copy

         Exception e = updateStatusWithLock(owner,key,order,lock,statusGoal,defaultHandler,null);
         if (e != null) {
            if (e == stopping) ; // ignore, user will see result
            else throw e;
         }

      } finally {
         table.release(key,lock);
      }
   }

   /**
    * @return An exception that represents a warning on a basically successful operation.
    */
   private Exception updateStatusWithLock(Window owner, String key, OrderStub order, Object lock, int statusGoal, Handler handler, PauseCallback callback) throws Exception {
      if (key == null) key = order.getFullID();

   // conditions

      if (order.status >= Order.STATUS_ORDER_COMPLETED) {
         throw new Exception(Text.get(this,"e1"));
      }

      if (statusGoal == Order.STATUS_ORDER_COMPLETED) { // abort and cancel are less picky

         // server wants download before complete
         if (order.status < Order.STATUS_ORDER_RECEIVED) throw new Exception(Text.get(this,"e14"));

         // ideally everything should be printed
         if (warnNotPrinted && order.status != Order.STATUS_ORDER_PRINTED && owner != null) {

            String s = Text.get(this,"e15",new Object[] { key });
            boolean confirmed = Pop.confirm(owner,s,Text.get(this,"s8"));
            if ( ! confirmed ) return stopping; // and do nothing
         }
      }

   // make jobs complete

      // do this before the server update, just as if in the natural course of events

      if (order instanceof Order) {
         if (statusGoal == Order.STATUS_ORDER_COMPLETED) {
            jobManager.makeComplete((Order) order,lock);
         } else {
            jobManager.makeNotComplete(key);
         }
      }
      // else it can't have jobs

   // tracking number adjustment

      // the idea here is that when an order moves into a final status,
      // the tracking fields ought to equal what's on the LP servers.
      // thus, if you delete an order they get cleared, and if you turn
      // tracking on, enter the fields, turn tracking off, and complete,
      // they get cleared.  this also simplifies the logic inside
      // StatusTransaction -- if the fields are there we send them, if
      // they're not we don't.

      if (order instanceof Order) { // else nothing to clear!
         Order orderObj = (Order) order;

         if (    statusGoal == Order.STATUS_ORDER_COMPLETED
              && enableTracking
              && orderObj.isAnyShipToHome() ) { // not sure how you'd get fields on an unshipped order, but let's check
            // do not clear
         } else {
            orderObj.carrier        = null;
            orderObj.trackingNumber = null;
         }
      }

      // the modified fields won't be written out until we get to the
      // table.update below, but I think that's OK.  if we didn't get
      // there, we'd also re-run the status transaction.

   // server update

      String suffix = ""; // suffix for messages that vary on failure
      if (order.orderSeq == null) {
         try {

            StatusTransaction t = new StatusTransaction(order,statusGoal,merchantConfig,downloadConfig);
            if ( ! handler.run(t,callback) ) return stopping;
            // exceptions may be thrown here too

         } catch (Exception e) {

            if (statusGoal == Order.STATUS_ORDER_ABORTED && owner != null) { // second condition is redundant, but let's make sure

               String s = Text.get(this,"e19",new Object[] { key }) + ChainedException.format(e);
               if (Pop.confirm(owner,s,Text.get(this,"s20"))) {
                  Log.log(order,Level.WARNING,this,"e20",new Object[] { key },e); // log and continue
                  suffix = "x";
               } else {
                  return stopping; // return but no need to report error further
               }
            } else {
               throw e;
            }
         }
      } else if (order.orderSeq.equals(Order.ORDER_SEQUENCE_LOCAL)) {

         LocalStatus.report(order.orderID,LocalStatus.translate(statusGoal),
                            System.currentTimeMillis(),Level.INFO,Text.get(this,"i7",new Object[] { OrderEnum.fromOrderStatus(statusGoal) }),null);

      }
      // else no external update defined

      Log.log(Level.INFO,this,(owner != null) ? ("i3" + suffix) : "i4",new Object[] { key, OrderEnum.fromOrderStatus(statusGoal) });
      // i4 doesn't need suffix because we only skip when owner isn't null

   // reformat if necessary

      // this can change order fields, so do before local update

      Exception reformatException = null;
      if (order instanceof Order) {
         try {
            Format.getFormat(((Order) order).format).makeComplete((Order) order);
         } catch (Exception e) {
            reformatException = e;
         }
      }
      // else it's a stub, certainly can't have been formatted

   // local update

      order.status = statusGoal;
      order.hold = Order.HOLD_NONE;
      order.recmodDate = new Date();

      try {
         table.update(order,lock);
      } catch (Exception e) {
         // this is bad ... status is in an inconsistent state.
         // explain that to the user along with the root error.
         Log.log(order,Level.SEVERE,this,"e12",new Object[] { key },e);
         throw new Exception(Text.get(this,("e3" + suffix)),e);
      }

   // if everything went well except the reformat, report that

      if (reformatException != null) {
         return new Exception(Text.get(this,("e4" + suffix)),reformatException);
      } else {
         return null;
      }
   }

// --- purge functions ---

   /**
    * Purge a set of orders, with user prompting and error reporting.
    */
   public void purgeMultiple(Window owner, Object[] o) {

   // confirm before purging

      String s = Text.get(this,"e5",new Object[] { new Integer(o.length), Convert.fromInt(o.length) });
      boolean confirmed = Pop.confirm(owner,s,Text.get(this,"s5"));
      if ( ! confirmed ) return;

   // ok, now purge

      boolean warn = false;

      for (int i=0; i<o.length; i++) {
         warn |= (purgeSingle(getKey(o[i]),null,null,Purge.REASON_MANUAL,(Reportable) o[i],/* verify = */ true) == Purge.RESULT_INCOMPLETE);
      }

   // warn if purge not completely successful

      if (warn) {
         Pop.warning(owner,Text.get(this,"e6"),Text.get(this,"s6"));
      }
   }

   /**
    * Pass in both order and lock if the object is locked.
    * Pass in key in every case.
    *
    * @param reason A value from the Purge.REASON_X enumeration.
    * @return       A value from the Purge.RESULT_X enumeration.
    */
   public int purgeSingle(String key, OrderStub order, Object lock, int reason, Reportable reportable, boolean verify) {

      boolean own = (lock == null);

      try {
         if (own) lock = table.lock(key);
         try {

            if (own) order = (OrderStub) table.get(key); // get latest copy

            if (verify && ! order.isPurgeableStatus()) return Purge.RESULT_FAILED;

            table.delete(key,lock);
            if (own) lock = null; // a successful delete releases the lock

            Log.log(Level.INFO,this,"i5" + Purge.getReasonSuffix(reason),new Object[] { key, OrderEnum.fromOrderStatus(order.status) });

         } finally {
            if (own && lock != null) table.release(key,lock);
         }
      } catch (Exception e) {

         // just log the exception -- if we were invoked from the UI,
         // the user will see the row remain in the table

         Log.log(reportable,Level.SEVERE,this,"e7",new Object[] { key },e);
         return Purge.RESULT_FAILED;

         // if the passed-in order is null, the reportable object won't be the most
         // up-to-date version, but it doesn't matter, the reportable fields don't change
      }

      return purgeFiles(order,/* rmdir = */ true) ? Purge.RESULT_COMPLETE : Purge.RESULT_INCOMPLETE;
   }

   /**
    * Purge an order by removing the data files and the directory.
    *
    * @return True if everything was successfully removed.
    */
   public boolean purgeFiles(OrderStub order, boolean rmdir) {

      boolean ok = true;

      if (order instanceof Order) {
         if (((Order) order).format != Order.FORMAT_FLAT) {
            try {
               Unformat.unformat((Order) order,dataDir);
            } catch (IOException e) {
               ok = false;
            }
         }
         // this doesn't update the table, but the order's already deleted there
         // no need for a stop dialog on this, unformatting is not the slow step
      }

      LinkedList names = new LinkedList();

      names.add(Order.ORDER_FILE);

      if (order instanceof Order) {
         // these aren't guaranteed to be present, but they certainly aren't present
         // if the order's a stub, and we need a non-stub to get the label files now.
         names.add(Order.INVOICE_FILE);
         names.addAll(Invoice.getLabelFilenames((Order) order,/* productEnableForHome    = */ true,
                                                              /* productEnableForAccount = */ true));
         // set enables to true so that we can purge product labels even if settings have changed
      }

      if (order.orderSeq != null && order.orderSeq.equals(Order.ORDER_SEQUENCE_LOCAL)) names.add(Order.LOCAL_FILE);

      names.add("order.tmp"); // some other program creates this
      names.add("Thumbs.db"); // some versions of Windows create this when user looks in folder

      if (order instanceof Order) {
         ListIterator li = ((Order) order).files.listIterator();
         while (li.hasNext()) {
            Order.OrderFile file = (Order.OrderFile) li.next();
            names.add(file.filename);
         }
      }

      ok &= FileUtil.purge(order.orderDir,(String[]) names.toArray(new String[names.size()]),/* warn = */ false,rmdir);

      // as of now, it's possible to abort an order at any stage,
      // even before it starts downloading.  so, we really have
      // no idea which files should exist ... the best option is,
      // just turn off the not-found warning for all files.
      //
      // note:  we could guess at which files should exist, but not reliably.
      // order file exists if not a stub ... or if invalid order.xml received.
      // invoice and label files exist if status got to invoiced ... too bad
      // that status information isn't available now that order is completed / aborted.
      // image files exist if status RECEIVED ... or if a wrong size received.

      return ok;
   }

   /**
    * @return A purge result, or null if purge not attempted.
    */
   public Integer autoPurge(Date now, OrderStub order, Object lock) {

      Date purgeDate = OrderUtil.getPurgeDate(order);
      if (    purgeDate != null
           && purgeDate.before(now) ) {

         String key = order.getFullID();
         int result = purgeSingle(key,order,lock,Purge.REASON_AUTOMATIC,order,/* verify = */ false);
         if (result == Purge.RESULT_INCOMPLETE) {
            Log.log(order,Level.WARNING,this,"e8",new Object[] { key });
         }

         return new Integer(result);
      } else {
         return null;
      }
   }

// --- invoice ---

   private Invoice getInvoice(OrderStub order) throws Exception {

      if (order.status < Order.STATUS_ORDER_PRESPAWN) throw new Exception(Text.get(this,"e10a"));
      // the GUI shouldn't allow this, but check anyway
      // actually this can happen if you work via order history

      if ( ! (order instanceof Order) ) throw new Exception(Text.get(this,"e10b"));
      // this happens if the user aborts an order stub
      // and then tries to view the invoice on the aborted order

      Invoice invoice;
      try {
         invoice = new Invoice((Order) order,stylesheet,postageXSL,jobManager,null);
      } catch (Exception e) {
         // add a message layer, since it's not obvious what we were trying to do
         throw new Exception(Text.get(this,"e11"),e);
      }

      return invoice;
   }

   private void invoice(Window owner, OrderStub order, int mode) throws Exception {

      Invoice invoice = getInvoice(order);

      File file;
      String title;

      switch (mode) {

      case Print.MODE_INVOICE:

         file = invoice.getInvoiceFile();
         title = invoice.getInvoiceTitle();
         break;

      case Print.MODE_LABEL:

         LinkedList labelInfos = invoice.getLabelInfos();
         Invoice.LabelInfo labelInfo = (Invoice.LabelInfo) labelInfos.getFirst(); // always at least 1

         if (labelInfos.size() > 1) {
            Object o = Pop.inputSelect(owner,Text.get(this,"s19"),Text.get(this,"s18"),labelInfos.toArray());
            if (o == null) return; // canceled
            labelInfo = (Invoice.LabelInfo) o;
         }

         file = labelInfo.labelFile;
         title = invoice.getLabelTitle();
         break;

      default:
         throw new IllegalArgumentException();
      }

      invoice = null; // sizeable object now that it holds order doc, let it get cleaned up

      HTMLViewer.create(owner,title,file.toURL(),mode).run();
   }

// --- UI commands ---

   public static void pleaseSelect(Window owner, boolean multi) {
      Pop.info(owner,Text.get(OrderManager.class,multi ? "e24a" : "e24b"),Text.get(OrderManager.class,"s23"));
   }

   // roll manager handles exceptions, order manager doesn't
   //
   public void doOpen(Window owner, Object o) {
      try {
         open(owner,o);
      } catch (Exception e) {
         Pop.error(owner,e,Text.get(this,"s2"));
      }
   }

   public void doHold(Frame owner, Object[] o, Subsystem downloadSubsystem) {
      if (o.length == 0) { pleaseSelect(owner,true); return; }

      int saveCount = 0;
      String saveKey = null;

      String[] keys = getKeys(o);
      for (int i=0; i<keys.length; i++) {
         Thread lockThread = hold(keys[i]);
         if (lockThread instanceof DownloadThread) { // BTW null is not an instance of anything
            saveCount++;
            saveKey = keys[i];
         }
      }

      if (saveCount == 1) { // if greater, something weird is going on, better not get involved
         if (Pop.confirm(owner,Text.get(this,"e28",new Object[] { saveKey }),Text.get(this,"s26"))) {
            boolean wasRunning = downloadSubsystem.stop(owner); // true, else why is there a lock?
            hold(saveKey);
            if (wasRunning) downloadSubsystem.start();
            // cf. the Subsystem reinit function and the stop and start buttons in the GUI
         }
      }
      // we need this operation for dealers who mix large online orders
      // with small kiosk orders, and also have stop and start disabled.
      // the code works, but is ugly in several ways:
      //
      // * "instanceof DownloadThread" is tacky, better would be to see
      //   whether the thread is the one executing in downloadSubsystem
      // * there's already a timing issue with the hold function, but here
      //   it's worse because the confirm dialog can stay open for an
      //   arbitrarily long time.  but, bouncing the download thread isn't
      //   too harmful, and the hold function does test the status so that
      //   there's always a way to release through the UI.
      // * the wasRunning logic just doesn't seem airtight to me, particularly
      //   since there's the same timing issue
      //
      // the reason I saved the confirm dialog for last is, I think otherwise
      // we could get a cascade.  suppose we have ten orders and the first is
      // downloading, and we pause them all.  we interrupt and pause the first,
      // and then the downloader grabs the second, so we have to interrupt it
      // again, and so on.  also I think the "saveCount == 1" test is good, and
      // we don't know the count until the end.
   }

   public void doRelease(Window owner, Object[] o) {
      if (o.length == 0) { pleaseSelect(owner,true); return; }
      String[] keys = getKeys(o);
      for (int i=0; i<keys.length; i++) {
         release(keys[i]);
      }
   }

   public static Selector selectorInvoice = new Selector() {
      public boolean select(Object o) {
         OrderStub order = (OrderStub) o;
         return (order.hold == Order.HOLD_INVOICE);
      }
   };

   public void doReleaseInvoice(Window owner) { // release everything with an invoice hold

      // this view is redundant with the view on GroupPanel,
      // but I want to keep the option of calling here from DendronPanel too

      View view = table.select(selectorInvoice,OrderUtil.orderOrderID,false,false);
      // order doesn't matter, but the select call requires some order, might as well be order ID

      int size = view.size();

      if (size == 0) return; // don't go into doRelease and say "please select"!
      // we could pop some other message but in context I think none is better

      Object[] o = new Object[size];
      for (int i=0; i<size; i++) {
         o[i] = view.get(i);
      }

      Log.log(Level.INFO,this,"i6",new Object[] { Convert.fromInt(size) });

      doRelease(owner,o);
   }

   public void doPrint(Window owner, Object[] o) {
      if (o.length == 0) { pleaseSelect(owner,true); return; }

      // count ones that have already been sent
      int partial = 0;
      int sent = 0;
      for (int i=0; i<o.length; i++) {
         OrderStub order = (OrderStub) o[i];

         int status = (order instanceof Order) ? JobManager.computeAltStatus((Order) order)
                                               : Order.STATUS_ORDER_INVOICED;
         // everything should be an order, but we have to do something with the other case.
         // it will error out in jobManager.create, below.

         if      (status == Order.STATUS_ORDER_PRINTING) partial++;
         else if (status >  Order.STATUS_ORDER_PRINTING) sent++;
      }

      // confirm since printing is a big deal and the button is near other buttons
      //
      JRadioButton b1 = null;
      JRadioButton b2 = null;
      Object messageObject;
      //
      String s = Text.get(this,"e21",new Object[] { new Integer(o.length), Convert.fromInt(o.length) });
      if (sent > 0) {
         s += "\n\n" + Text.get(this,(o.length == 1) ? "e22a" : "e22b",new Object[] { new Integer(sent), Convert.fromInt(sent) });
      }
      if (partial > 0) {
         s += "\n\n" + Text.get(this,(o.length == 1) ? "e27a" : "e27b",new Object[] { new Integer(partial), Convert.fromInt(partial) });

         b1 = new JRadioButton(Text.get(this,"s24"));
         b2 = new JRadioButton(Text.get(this,"s25"));
         ButtonGroup g = new ButtonGroup();
         g.add(b1);
         g.add(b2);
         b2.setSelected(true);

         messageObject = new Object[] { s, b1, b2 };
      } else {
         messageObject = s;
      }
      boolean confirmed = Pop.confirmVariant(owner,messageObject,Text.get(this,"s21"));
      if ( ! confirmed ) return;

      boolean adjustIfPartial = (b2 != null) ? b2.isSelected() : true;
      // the value "true" matters and is correct.  we don't have the objects locked
      // at this point, so it's possible we'll find a new partial.  but, item state
      // can't move backward to RECEIVED, so if we find a new partial, we know that
      // it used to be not sent at all, and the user asked to send all of it.
      // some has already been sent, so the right behavior is just to send the rest.

      // use the same error-handling logic here as in GroupPanel
      //
      String[] keys = getKeys(o);
      int printed = 0;
      for (int i=0; i<keys.length; i++) {

         Log.log(Level.INFO,this,"i8",new Object[] { keys[i] });
         try {
            jobManager.create(keys[i],null,null,null,null,adjustIfPartial);
         } catch (Exception e) {

            int unprinted = keys.length - (printed+1);
            String message = Text.get(this,"e23",new Object[] { keys[i], new Integer(printed), Convert.fromInt(printed), new Integer(unprinted), Convert.fromInt(unprinted) });

            Pop.error(owner,ChainedException.format(message,e),Text.get(this,"s22"));
            break;
         }
         printed++;
      }
   }

   public void doComplete(Window owner, Object[] o) {
      if (o.length == 0) { pleaseSelect(owner,true); return; }
      for (int i=0; i<o.length; i++) {
         try {
            completeWithTracking(owner,o[i]);
         } catch (Throwable t) {
            Pop.error(owner,t,Text.get(this,"s3"));
         }
      }
   }

   public void doAbort(Window owner, Object[] o) {
      if (o.length == 0) { pleaseSelect(owner,true); return; }
      String[] keys = getKeys(o);

      // confirm before deleting
      String s = Text.get(this,"e18",new Object[] { new Integer(keys.length), Convert.fromInt(keys.length) });
      boolean confirmed = Pop.confirm(owner,s,Text.get(this,"s15"));
      if ( ! confirmed ) return;

      for (int i=0; i<keys.length; i++) {
         try {
            abort(owner,keys[i]);
         } catch (Throwable t) {
            Pop.error(owner,t,Text.get(this,"s4"));
         }
      }
   }

   public void doCancel(Window owner, Object[] o) {
      if (o.length == 0) { pleaseSelect(owner,true); return; }
      String[] keys = getKeys(o);

      // confirm before canceling
      String s = Text.get(this,"e17",new Object[] { new Integer(keys.length), Convert.fromInt(keys.length) });
      boolean confirmed = Pop.confirm(owner,s,Text.get(this,"s14"));
      if ( ! confirmed ) return;

      for (int i=0; i<keys.length; i++) {
         try {
            cancel(owner,keys[i]);
         } catch (Throwable t) {
            Pop.error(owner,t,Text.get(this,"s13"));
         }
      }
   }

   public void doPurge(Window owner, Object[] o) {
      if (o.length == 0) { pleaseSelect(owner,true); return; }

      // prevent ineffective purge commands
      for (int i=0; i<o.length; i++) {
         OrderStub order = (OrderStub) o[i];
         if ( ! order.isPurgeableStatus() ) {
            Pop.error(owner,Text.get(this,"e9"),Text.get(this,"s7"));
            return;
         }
      }

      purgeMultiple(owner,o);
   }

   public void doInvoice(Window owner, Object[] o) { doInvoiceLabel(owner,o,Print.MODE_INVOICE); }
   public void doLabel  (Window owner, Object[] o) { doInvoiceLabel(owner,o,Print.MODE_LABEL  ); }

   private void doInvoiceLabel(Window owner, Object[] o, int mode) {
      if (o.length == 0) { pleaseSelect(owner,false); return; }

      OrderStub order = (OrderStub) o[0];
      // only operate on the first selected order.
      // technically we should get the latest copy,
      // but nothing we look at should ever change.

      try {
         invoice(owner,order,mode);
      } catch (Exception e) {
         Pop.error(owner,e,Text.get(this,"s9"));
      }
   }

   public void doReprint(Window owner, Object[] o, int mode) {
      if (o.length == 0) { pleaseSelect(owner,true); return; }

   // confirm, this will take some time

      String reprintObject;
      switch (mode) {
      case Print.MODE_INVOICE:  reprintObject = Text.get(this,"s16");  break;
      case Print.MODE_LABEL:    reprintObject = Text.get(this,"s17");  break;
      default:  throw new IllegalArgumentException();
      }

      String s = Text.get(this,"e16",new Object[] { new Integer(o.length), Convert.fromInt(o.length), reprintObject });
      boolean confirmed = Pop.confirm(owner,s,Text.get(this,"s12"));
      if ( ! confirmed ) return; // and do nothing

   // reprint

      try {
         ReprintThread reprintThread = new ReprintThread(o,mode);
         reprintThread.start();
         StopDialog.joinSafe(reprintThread,owner);
         reprintThread.rethrow();

      } catch (Exception e) {
         Pop.error(owner,e,Text.get(this,"s10"));
      }
   }

// --- reprint thread ---

   private class ReprintThread extends Thread {

      private Object[] o;
      private int mode;
      private Exception exception;

      public ReprintThread(Object[] o, int mode) {
         super(Text.get(OrderManager.class,"s11"));
         this.o = o;
         this.mode = mode;
         // exception starts out null
      }

      public void run() {
         try {

            // no need to lock the orders, we're just looking at files

            for (int i=0; i<o.length; i++) {
               OrderStub order = (OrderStub) o[i];

               Invoice invoice = getInvoice(order);
               invoice.print(mode);
            }

         } catch (Exception e) {
            exception = e;
         }
      }

      /**
       * Transfer exceptions back to main thread.
       */
      public void rethrow() throws Exception {
         if (exception != null) throw exception;
      }
   }

}

