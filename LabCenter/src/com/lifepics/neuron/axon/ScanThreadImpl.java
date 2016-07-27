/*
 * ScanThreadImpl.java
 */

package com.lifepics.neuron.axon;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Log;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.thread.CategorizedException;
import com.lifepics.neuron.thread.StoppableThread;
import com.lifepics.neuron.thread.ThreadStatus;
import com.lifepics.neuron.thread.ThreadStopException;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Level;

import com.kodak.kias.clientlib.ClientLibException;
import com.kodak.kias.clientlib.ImageInfo;
import com.kodak.kias.clientlib.KIASSystem;
import com.kodak.kias.clientlib.OrderInfo;
import com.kodak.kias.clientlib.OrderStatus;

/**
 * An implementation class that lets us catch and handle
 * the NoClassDefFoundError we get if external jar files
 * are missing.
 */

public class ScanThreadImpl {

   // *** WARNING!  This class requires an external jar file!
   // *** Callers must catch and handle NoClassDefFoundError!

// --- construction ---

   private ScanConfigDLS configDLS;
   private ScanThread.Callback callback;
   private RollManager rollManager;
   private ThreadStatus threadStatus;

   public ScanThreadImpl(ScanConfigDLS configDLS, ScanThread.Callback callback, RollManager rollManager, ThreadStatus threadStatus) {

      this.configDLS = configDLS;
      this.callback = callback;
      this.rollManager = rollManager;
      this.threadStatus = threadStatus;
   }

// --- utilities ---

   private static boolean isStopping() {
      return ((StoppableThread) Thread.currentThread()).isStopping();
   }

// --- KIAS utilities ---

   private static Exception wrap(ClientLibException e) {
      return new Exception(Text.get(ScanThreadImpl.class,"e1",new Object[] { Integer.toHexString(e.getErrorCode()).toUpperCase() }),e);
   }

   private KIASSystem.Session connect() throws ClientLibException {
      String url = "http://" + configDLS.host + ":" + Convert.fromInt(configDLS.port) + "/" + configDLS.path;
      KIASSystem system = new KIASSystem(url,"");
      return system.new Session(configDLS.userName,configDLS.password);
   }

   private static long fromKiasDateTime(int ktime) {
      return ktime * ((long) 1000);
      // KIAS date-time is time_t -- like Java time except seconds instead of milliseconds
   }

   private static int toKiasDateTime(long jtime) throws Exception {
      long temp = jtime / 1000; // should be round number, don't worry about rounding
      if (    temp < Integer.MIN_VALUE
           || temp > Integer.MAX_VALUE ) throw new Exception(Text.get(ScanThreadImpl.class,"e3"));
      return (int) temp;
   }

   private static SimpleDateFormat dateFormat = new SimpleDateFormat(Text.get(ScanThreadImpl.class,"s2"));

// --- non-entity process ---

   private static final String CATEGORY_LIST = "list";

   public void doNonEntity() throws Exception {

      Log.log(Level.INFO,this,"i1");

      KIASSystem.Session.Order[] orders;

      // a standard categorization block
      try {

         // a standard session block
         KIASSystem.Session session = null;
         try {
            session = connect();

            int start = toKiasDateTime(configDLS.effectiveDate.getTime());
            orders = session.GetOrdersAdmin(start,0,OrderStatus.NOTFOUND,"");

         } catch (ClientLibException e) {
            throw wrap(e);
         } finally {
            if (session != null) {
               try {
                  session.Disconnect();
               } catch (Exception e) {
                  // ignore
               }
            }
         }

      } catch (ThreadStopException e) {
         throw e; // see comment in DownloadThread labeled (*)
      } catch (Exception e) {
         throw new CategorizedException(CATEGORY_LIST,Text.get(this,"e2"),e);
         // DownloadThread does some weird stuff here
         // to be parallel with the categorization in shouldCreate,
         // which in turn is weird because it's fairly easy
         // to get an error while processing a single order,
         // and yet we want to keep going and not throw an exception.
         // here errors are unlikely .. no need for any of that.
      }
      threadStatus.success(CATEGORY_LIST);

      // no need to check whether we should stop in here ...
      // there's only one slow operation, and it's over.

      // now we have an array of order information

      LinkedList dlsOrderIDs = callback.getDLSOrderIDs();
         // nobody else uses this, we can query once and hold

   // before we add new entries, clean out stale ones

      // this isn't quite perfect ... if you reconfigure to point
      // at a new DLS server, and it happens to have a roll you want
      // listed under an ID you already have, you're out of luck.
      // however, under all other conditions it should be just right.

      HashSet idSet = new HashSet();
      for (int i=0; i<orders.length; i++) {
         idSet.add(orders[i].getOrderInfo().getOrderID());
      }

      boolean cleaned = false;

      Iterator j = dlsOrderIDs.iterator();
      while (j.hasNext()) {
         if ( ! idSet.contains(j.next()) ) { j.remove(); cleaned = true; }
      }

      if (cleaned) callback.setDLSOrderIDs(dlsOrderIDs);

   // ok, now add new entries

      for (int i=orders.length-1; i>=0; i--) { // create in order from oldest to newest
         OrderInfo orderInfo = orders[i].getOrderInfo();
         String orderID = orderInfo.getOrderID();

         // optionally skip ones that don't have dashes
         if (configDLS.excludeByID && orderID.indexOf('-') == -1) continue;
         //
         // ones without dashes seem to be ones that are in some kind of
         // transitional status, so that after we create the roll and
         // ask for the images, the server says they're no longer available.
         // but, it turns out there's no way to find out the status ...
         // DLS doesn't support using it in a query, and the order info
         // doesn't contain it (correctly doesn't, since it's volatile).
         //
         // you might you should do this up front, before the cleanup,
         // but I think it's better here ... that way if the user already
         // has a roll with a non-dash ID, and flips the exclude on & off,
         // we won't forget that we've seen it and create another copy.
         // in other words, the memory list isn't just an arbitrary list,
         // it's a list of active IDs that we've created rolls for.

         // check whether we've seen it before
         if (dlsOrderIDs.contains(orderID)) continue;

         // add to memory, so if it fails we won't recreate the roll over and over
         dlsOrderIDs.add(orderID);
         callback.setDLSOrderIDs(dlsOrderIDs);

         // now create the roll
         try {

            Roll roll = new Roll();

            roll.bagID = orderID; // roll edit not allowed yet, so user can't mess up
            roll.source = Roll.SOURCE_DLS;
            roll.email = orderInfo.getCustomerID();
            roll.scans = new LinkedList(); // empty list is signal to read
            roll.album = dateFormat.format(new Date(fromKiasDateTime(orderInfo.getDateTime())));

            rollManager.createHaveItems(roll,RollManager.METHOD_COPY,null,null);

         } catch (Exception e) {

            // roll creation shouldn't fail, but it can happen.
            // so, to keep the list change and roll creation
            // as atomic as possible, try to roll back the list addition.
            try {
               dlsOrderIDs.removeLast();
               callback.setDLSOrderIDs(dlsOrderIDs);
            } catch (Exception e2) {
               // ignore ... at least we tried
            }
            throw e;
         }
      }
   }

// --- entity process ---

   public boolean doRoll(Roll roll) throws Exception {

      Log.log(Level.INFO,this,"i2",new Object[] { Convert.fromInt(roll.rollID) });

      // one day this might switch by roll source,
      // but for now, everything is just DLS-KIAS.

      // a standard session block
      KIASSystem.Session session = null;
      try {
         session = connect();

      // find order

         int start = toKiasDateTime(configDLS.effectiveDate.getTime());
         KIASSystem.Session.Order[] orders = session.GetOrdersAdmin(start,0,OrderStatus.NOTFOUND,"");
         // the documentation says not to use the order ID parameter;
         // apparently it only works if the order has already been retrieved in this session

         KIASSystem.Session.Order order = null;
         for (int i=0; i<orders.length; i++) { // desired element might be on newer side
            if (orders[i].getOrderInfo().getOrderID().equals(roll.bagID)) { order = orders[i]; break; }
            // bag ID is nullable now, but it's OK since it's just an argument to equals
         }
         if (order == null) throw new Exception(Text.get(this,"e4"));

      // get list

         if (roll.scans.isEmpty()) { // signal to read

            if (isStopping()) return false; // check at the start,
            // just to match the image-retrieval loop.
            // probably GetImageRefs is local, hence fast, so that
            // the check is unnecessary, but it does no harm.

            ImageInfo[] imageInfo = order.GetImageRefs();
            for (int i=0; i<imageInfo.length; i++) {
               roll.scans.add(Convert.fromInt(imageInfo[i].getImageID()));
            }
         }

      // get images

         while ( ! roll.scans.isEmpty() ) {

            if (isStopping()) return false; // check at the start,
            // not the end, otherwise it breaks atomic-ness below.

            String imageIDString = (String) roll.scans.getFirst();
            int imageID = Convert.toInt(imageIDString);
            // don't remove from list until successful transfer

            // images are all JPEGs, apparently
            File dest = new File(roll.rollDir,imageIDString + ".jpg");
            order.GetHighResImage(imageID,Convert.fromFile(dest));
            // this should resume after partial download, at least on some servers

            roll.scans.removeFirst();
            roll.items.add(ItemUtil.makeItemScanned(dest));
         }

      // finish up

         // there are no failure points after this,
         // so we can go ahead and set scans to null ...
         // we won't error out and end up with
         // a roll with scanned status but no scans.
         //
         // in other words, we remove the last scan,
         // set scans to null, and become pending,
         // and it's all basically an atomic operation.

         roll.scans = null;
         if (configDLS.holdConfirm) roll.hold = Roll.HOLD_ROLL_CONFIRM;

         roll.status = Roll.STATUS_ROLL_PENDING;
         RollManager.adjustHold(roll);
         // actually we have to set pending status now,
         // otherwise RollManager won't adjust for us.
         // also note, adjust won't release confirm hold.

      } catch (ClientLibException e) {
         throw wrap(e);
      } finally {
         if (session != null) {
            try {
               session.Disconnect();
            } catch (Exception e) {
               // ignore
            }
         }
      }

      Log.log(Level.INFO,this,"i3",new Object[] { RollEnum.fromRollHold(roll.hold) });

      return true;
   }

}

