/*
 * UploadThread.java
 */

package com.lifepics.neuron.axon;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Log;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.gui.User;
import com.lifepics.neuron.meta.Accessor;
import com.lifepics.neuron.meta.AndSelector;
import com.lifepics.neuron.meta.CompoundComparator;
import com.lifepics.neuron.meta.FieldComparator;
import com.lifepics.neuron.meta.NaturalComparator;
import com.lifepics.neuron.meta.ReverseComparator;
import com.lifepics.neuron.meta.Selector;
import com.lifepics.neuron.misc.FileUtil;
import com.lifepics.neuron.misc.KioskLog;
import com.lifepics.neuron.misc.PurgeConfig;
import com.lifepics.neuron.misc.TransferTracker;
import com.lifepics.neuron.net.BandwidthConfig;
import com.lifepics.neuron.net.BandwidthUtil;
import com.lifepics.neuron.net.FormDataTransaction;
import com.lifepics.neuron.net.GetTransaction;
import com.lifepics.neuron.net.Handler;
import com.lifepics.neuron.net.HTTPTransaction;
import com.lifepics.neuron.net.MerchantConfig;
import com.lifepics.neuron.net.PauseAdapter;
import com.lifepics.neuron.net.PostTransaction;
import com.lifepics.neuron.net.Query;
import com.lifepics.neuron.net.RawUploadTransaction;
import com.lifepics.neuron.net.RegulatedTransaction;
import com.lifepics.neuron.net.RetryableException;
import com.lifepics.neuron.net.SOAPUploadTransaction;
import com.lifepics.neuron.net.Transaction;
import com.lifepics.neuron.net.UploadTransaction;
import com.lifepics.neuron.object.XML;
import com.lifepics.neuron.table.Table;
import com.lifepics.neuron.table.TableException;
import com.lifepics.neuron.table.View;
import com.lifepics.neuron.thread.EntityManipulator;
import com.lifepics.neuron.thread.EntityThread;
import com.lifepics.neuron.thread.NormalOperationException;
import com.lifepics.neuron.thread.PauseRetryException;
import com.lifepics.neuron.thread.ThreadStatus;
import com.lifepics.neuron.thread.ThreadStopException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.logging.Level;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * A thread that uploads images to the LifePics web server.
 */

public class UploadThread extends RollThread {

// --- fields ---

   // all non-thread fields

   private MerchantConfig merchantConfig;
   private UploadConfig config;
   private File transformFile;
   private LinkedList dealers;
   private boolean prioritizeEnabled;
   private Long rollReceivedPurgeInterval;
   private Handler handler;
   private TransferTracker tracker;
   private PauseAdapter pauseAdapter;

   private long nextPriorityPoll;       // not used unless prioritizeEnabled ( = localShareEnabled )
   private View viewLocalImageID;       // ditto
   private HashMap priorityMap;         // null unless prioritizeEnabled, but we can look if we check for null
   private long nextScheduleTransition; // not used unless nontrivial schedule ( ! trivialRestrict )
   private boolean currentIsRestricted; // related but always valid

   private Method method;
   private int orderVersion;

// --- delete manipulator ---

   private static class DeleteManipulator extends EntityManipulator {

      public DeleteManipulator(int statusFrom,
                               int statusActive,
                               int statusTo) {
         super(statusFrom,statusActive,statusTo);
      }

      public boolean markComplete(Object o) {
         if ( ((Roll) o).status == Roll.STATUS_ROLL_DELETED ) {
            return true; // already marked
         } else {
            return super.markComplete(o);
         }
      }
   }

// --- construction ---

   public UploadThread(Table table, MerchantConfig merchantConfig, UploadConfig config, File transformFile, LinkedList dealers, boolean prioritizeEnabled, Long rollReceivedPurgeInterval,
                       Handler handler,
                       TransferTracker tracker,  ThreadStatus threadStatus) {
      super(Text.get(UploadThread.class,"s1"),
            table,
            new DeleteManipulator(
               Roll.STATUS_ROLL_PENDING,
               Roll.STATUS_ROLL_SENDING,
               Roll.STATUS_ROLL_COMPLETED),
            getPriorityComparator(null), // null map sorts kiosk images to end
            /* scanFlag = */ false,
            config.idlePollInterval,
            threadStatus);
      doNonEntityFirst = true; // usually the non-entity code doesn't run, but when it does, it comes first

      // no need to copy these, subsystem doesn't modify configs
      this.merchantConfig = merchantConfig;
      this.config = config;
      this.transformFile = transformFile;
      this.dealers = dealers;
      this.prioritizeEnabled = prioritizeEnabled;
      this.rollReceivedPurgeInterval = rollReceivedPurgeInterval;
      this.handler = handler;
      this.tracker = tracker;
      this.pauseAdapter = new PauseAdapter(threadStatus);

      this.config.bandwidthConfig.precomputeTrivial();

      if (prioritizeEnabled) { // otherwise these fields aren't used
         nextPriorityPoll = System.currentTimeMillis();
         Selector selector = new Selector() { public boolean select(Object o) { return (((Roll) o).getLocalImageID() != null); } };
         viewLocalImageID = table.select(new AndSelector(getManipulator(),selector),RollUtil.orderRollID,true,false);
         // manipulator is also a selector for statusFrom and statusActive, so this gives us the subview of all local images
         // no need for the full priority comparator here
      }

      // priority map starts out null, and stays null unless prioritizeEnabled

      if (config.bandwidthConfig.trivialRestrict) {
         currentIsRestricted = false;
      } else {
         nextScheduleTransition = System.currentTimeMillis();
         // we'll calculate currentIsRestricted before use
      }
   }

// --- regulation ---

   // see DownloadThread for the whole theory of regulation

   public boolean regulateIsStopping() {
      return BandwidthUtil.regulateIsStopping(config.bandwidthConfig,/* lastTransferDuration = */ 0,threadStatus,this);
   }

   private Transaction regulate(Transaction t) {
      return new RegulatedTransaction(config.bandwidthConfig,threadStatus,this,t);
   }

// --- priority list ---

   // here we sort orders into four categories, or levels:
   //
   // 1. high-priority kiosk images.  these are MediaClip uploads that we need
   // up on the server right away.  there should only be a few of these.
   // in restricted mode we still upload them.  there are actually two sublevels,
   // for local and remote production, but only the server side knows about that,
   // we just receive and obey the ordered list.
   //
   // 2. images to upload.  this includes normal uploads and also kiosk images
   // for registered users.  we don't do these in restricted mode.
   //
   // 3. images to delete.  this is tricky, see below for details.  we still
   // want to do this in restricted mode.  ideally we'd do it even if the bandwidth
   // was limited to 0, but that's not a real case and there's no easy way to do it.
   //
   // 4. images that we don't know what to do with yet.  we don't do anything
   // with these until we hear from the server.  in the misconfiguration case
   // where you're in restricted mode with kiosk turned off, all kiosk images
   // will fall into this category.
   //
   // level   priority map value(s)
   // -----   ---------------------
   // 1       N down to 1
   // 2       PRIORITY_UPLOAD =  0
   // 3       PRIORITY_DELETE = -1
   // 4       PRIORITY_WAIT   = -2
   //
   // we use one value for all levels except 1 so that the sort will fall back
   // to be by roll ID within the level.  the descending order is a historical
   // accident, left over from when priority level was a direct priorityMap lookup
   // and the infinitely-negative null values needed to go last.

   // here's the tricky part.  the function isLoopEndingEntity determines how far
   // down the list of uploads we go.  level 2 is loop-ending in restricted mode,
   // while levels 3 and 4 are always loop-ending.  so how do we ever call doRoll
   // on deleted images?  the answer is, there's a separate scan loop.
   // it has to work that way, because deleting is going to be time-dependent now,
   // based on rollReceivedPurgeInterval.
   //
   // the fundamental problem is that the LocationDeviceShopperRel records I was
   // supposed to use to tell whether guests are still logged in aren't reliable.
   // usually they're there, but then sometimes they aren't.  the kiosk team has
   // no idea, and I can't be bothered with talking to them about it any more.
   // with a 15-minute purge interval, we got lots of missing images as a result.
   // when we turned that up to an hour, that stopped the images from getting
   // purged, but it didn't stop them from getting marked deleted, so images for
   // MC orders wouldn't upload.
   //
   // so, the new plan is, turn up the purge interval even further (partly to
   // allow some guest reprints), and also don't mark images deleted until
   // after the purge interval has elapsed.  as a side benefit, this will also
   // cut down on TryPurgeLocalImage calls.  there's still a problem case if
   // the guest stays logged in for the entire interval *and* loses their LDSR
   // record, but that should be pretty rare.
   //
   // note, GetPriorityList is the only place Opie / LC / Closed accesses LDSR,
   // so we only need to fix the problem here.

   private static final int INT_PRIORITY_UPLOAD =  0;
   private static final int INT_PRIORITY_DELETE = -1;
   private static final int INT_PRIORITY_WAIT   = -2;

   private static final Integer PRIORITY_UPLOAD = new Integer(INT_PRIORITY_UPLOAD);
   private static final Integer PRIORITY_DELETE = new Integer(INT_PRIORITY_DELETE);
   private static final Integer PRIORITY_WAIT   = new Integer(INT_PRIORITY_WAIT);

   protected boolean hasNonEntity() {
      return    prioritizeEnabled
             && (System.currentTimeMillis() >= nextPriorityPoll)
             && viewLocalImageID.size() > 0;
             // if there are no local images, don't bother checking the priorities
   }

   protected void endNonEntity() {
      nextPriorityPoll = System.currentTimeMillis() + config.localPriorityInterval;
   }

   protected boolean doNonEntityTrigger() throws Exception {

      Log.log(Level.FINE,this,"i19");

      GetPriorityList t = new GetPriorityList(config.localPriorityURL,merchantConfig);
      if ( ! handler.run(regulate(t),pauseAdapter) ) return false;
      //
      // this is actually a tricky point.  if the transaction fails, we keep uploading
      // based on previous results, so why not also run delete-scans based on previous
      // results?  the answer is, uploading is harmless, but deleting is not harmless.
      // maybe we had a kiosk glitch and got some deletes, but now the customer has placed
      // a MC order and the deletes have turned into uploads.  or, a different angle on it,
      // deletes can change into uploads, but uploads can't change into anything else.
      // also, I'm not too worried about deletes not happening, the transaction is pretty
      // solid and shouldn't be failing much.

      priorityMap = buildPriorityMap(t.priorityList,t.uploadList,t.deleteList);
      sortEntities(getPriorityComparator(priorityMap));
      return (t.deleteList.size() > 0); // trigger a scan pass if there are any to delete (basically always true)
   }

   private static HashMap buildPriorityMap(LinkedList priorityList, LinkedList uploadList, LinkedList deleteList) {
      HashMap map = new HashMap();

      int n = priorityList.size();
      Iterator i = priorityList.iterator();
      while (i.hasNext()) {
         String imageID = (String) i.next();
         map.put(imageID,new Integer(n--));
      }

      i = uploadList.iterator();
      while (i.hasNext()) {
         String imageID = (String) i.next();
         map.put(imageID,PRIORITY_UPLOAD);
      }

      i = deleteList.iterator();
      while (i.hasNext()) {
         String imageID = (String) i.next();
         map.put(imageID,PRIORITY_DELETE);
      }

      return map;
   }

   private static Comparator getPriorityComparator(HashMap map) {
      return new CompoundComparator(new ReverseComparator(new FieldComparator(new PriorityAccessor(map),new NaturalComparator())),
                                    RollUtil.orderRollID);
      // we could make PriorityAccessor non-static and have it use priorityMap directly, but that seems untidy
      // since the comparator can run at any time in other threads
   }

   // more generally we could have an accessor that maps another accessor;
   // the only trouble is, there's no nice way to implement getFieldClass.
   //
   private static class PriorityAccessor implements Accessor {

      private HashMap map;
      public PriorityAccessor(HashMap map) { this.map = map; }

      public Class getFieldClass() { return Integer.class; }
      public Object get(Object o) { return getPriorityValue(map,o); }
   }

   private static Integer getPriorityValue(HashMap map, Object o) {
      String imageID = ((Roll) o).getLocalImageID();
      if (imageID == null) return PRIORITY_UPLOAD; // normal upload
      if (map == null) return PRIORITY_WAIT; // could be we haven't received priorities yet, could be misconfig
      Integer val = (Integer) map.get(imageID);
      if (val == null) return PRIORITY_WAIT; // get rid of nulls, they just complicate the priority comparisons
      return val;
   }

   protected boolean isLoopEndingEntity(Object o) {

      // this gets called once a second when we're idling and not uploading
      // some kiosk image, so make sure it's not too expensive

   // check the priority value first

      int priorityValue = getPriorityValue(priorityMap,o).intValue();

      if (priorityValue > INT_PRIORITY_UPLOAD) return false; // level  1       is  never  loop-ending
      if (priorityValue < INT_PRIORITY_UPLOAD) return true;  // levels 3 and 4 are always loop-ending

   // check for schedule transition

      if ( ! config.bandwidthConfig.trivialRestrict ) {
         if (System.currentTimeMillis() >= nextScheduleTransition) {

            Calendar cNow = Calendar.getInstance();
            int timeNow = BandwidthUtil.getTimeNow(cNow);

            BandwidthUtil.IntRef ref = new BandwidthUtil.IntRef();
            BandwidthConfig.Schedule schedule = BandwidthUtil.findCurrent(config.bandwidthConfig,timeNow,ref);

            BandwidthConfig.Schedule next = BandwidthUtil.findNext(config.bandwidthConfig,ref.value);
            // transition may not change restricted-ness, but we don't care,
            // the main thing is to cut the number of schedule calculations way down

            Calendar cNext = BandwidthUtil.getTransition(cNow,timeNow,next);

            nextScheduleTransition = cNext.getTimeInMillis();
            currentIsRestricted = schedule.isRestricted();
         }
      }

   // level 2 is loop-ending in restricted mode

      return currentIsRestricted;
   }

   protected int scanFilter(Object o) {

   // check the priority value first

      int priorityValue = getPriorityValue(priorityMap,o).intValue();

      if (priorityValue > INT_PRIORITY_DELETE) return SCAN_SKIP;
      if (priorityValue < INT_PRIORITY_DELETE) return SCAN_STOP;

   // check for limits based on RRPI

      // code adapted from RollManager.autoPurge (where "now" is equal to scanDate) and RollUtil.getPurgeDate.
      // I'm checking "! before" instead of "after" just for consistency with that code.
      //
      // since the priority is PRIORITY_DELETE, we can only get here with SOURCE_LOCAL, no worries about that.
      // so, receivedDate will be non-null unless someone's been messing with the XML files.  for a correctly
      // configured store, RRPI will be non-null too.
      //
      // uploads are ordered within PRIORITY_DELETE by rollID, and receivedDate order equals creation order
      // equals rollID order.  so, as soon as we find one roll that's not ripe for deletion we can stop the
      // whole scan.  that's why we return SCAN_STOP instead of SCAN_SKIP.
      //
      // do we still need the RRPI check at purge time?  yes, because not all uploads go through the delete process!

      Date receivedDate = ((Roll) o).receivedDate;
      // don't use "roll", it's not set correctly!

      if (receivedDate != null && rollReceivedPurgeInterval != null) {
         Date deleteDate = PurgeConfig.increment(receivedDate,rollReceivedPurgeInterval.longValue());
         if ( ! deleteDate.before(scanDate) ) return SCAN_STOP;
      }

      return SCAN_GO;
   }

// --- upload functions ---

   protected boolean doRoll() throws Exception {

      // we shouldn't see any PRIORITY_WAIT rolls here, but if somehow we do see one,
      // we'll upload it.

   // kiosk delete function

      Integer priorityValue = getPriorityValue(priorityMap,roll);
      // a shame to call twice (see isLoopEndingEntity), but it's only a hash lookup

      if (priorityValue.equals(PRIORITY_DELETE)) {

         if (roll.source == Roll.SOURCE_LOCAL) { // provable but I like to say it
            KioskLog.log(this,"i20",new Object[] { roll.getLocalImageID() });
         }
         // else nothing, it's impossible

         roll.status = Roll.STATUS_ROLL_DELETED; // DeleteManipulator will preserve this
         return true;
      }

   // normal processing

      try {
         tracker.groupBegin1(roll);
         tracker.groupBegin2(roll); // must come early for totalSizeGoal calculation

         return upload();

      } finally {
         tracker.groupEnd();
      }
   }

   /**
    * @return True if the upload is complete.
    *         False if the upload is incomplete because the thread is stopping.
    *         If the upload is incomplete because of an error,
    *         that will be reported via an exception.
    */
   private boolean upload() throws Exception {

      if (roll.source == Roll.SOURCE_LOCAL) {
         // we don't need to hear about it
      } else {
         Log.log(Level.INFO,this,"i1",new Object[] { Convert.fromInt(roll.rollID) });
      }

      // a note about informational logging:
      //
      // user commands are logged at completion.
      // the idea is, these commands are (mostly) atomic events,
      // so if a command doesn't complete, nothing has happened.
      //
      // background thread activities are logged at initiation.
      // the idea here is, errors are already thoroughly logged,
      // so we can report when each atomic step begins,
      // and if there's no error in the log, then it succeeded.

      if (isStopping()) return false;
      // don't stop in the middle of a post, only between posts

      method = getMethod();
      method.validate();
      // just like with roll, we set this then never clear it.
      // it's a tiny object with little state, not a big deal.

      if ( ! method.initiate() ) return false;

      boolean complete = false;
      try {
         tracker.timeBegin();

         if ( ! uploadItems() ) return false;

         complete = true;
      } finally {
         tracker.timeEnd(complete);
      }

      if ( ! method.terminate() ) return false;

      if (roll.source == Roll.SOURCE_LOCAL) {
         KioskLog.log(this,"i21",new Object[] { roll.getLocalImageID() });
      } else {
         Log.log(Level.INFO,this,"i5");
      }
      roll.endRetry(this,"i7"); // operation is complete, clear retry info

      return true;
   }

   private boolean uploadItems() throws Exception {

      int successiveFailures = 0;
      int totalFailures = 0;
      Exception lastFailure = null;

      Iterator i = roll.items.iterator();
      while (i.hasNext()) {
         item = (Roll.Item) i.next();
         if (    item.status == Roll.STATUS_FILE_PENDING
              || item.status == Roll.STATUS_FILE_SENDING ) {
         // latter case shouldn't happen

            if (roll.source == Roll.SOURCE_LOCAL) {
               // we don't need to hear about it
            } else {
               Log.log(Level.INFO,this,"i3",new Object[] { item.getOriginalFilename() });
            }

            try {

               if ( ! uploadItem() ) return false; // (*)
               successiveFailures = 0;
               // what if we have failures available when we hit a thread stop?
               // it's debatable .. but since we didn't get through all the items,
               // I'll leave the roll in pending status so we can come back to it

            } catch (TableException e) { // we only need this one because of the unusual way
               throw e;                  // we're wrapping above where table access occurs
            } catch (ThreadStopException e) {
               throw e;
            } catch (Exception e) {

               if (config.successiveFailureLimit == 1) throw e;
               // one in a row is the old behavior, so stick with the old messages
               // in that case, no need for extra logging or extra exception text.

               successiveFailures++;
               totalFailures++;
               lastFailure = e;

               if (successiveFailures < config.successiveFailureLimit) {
                  Log.log(roll,Level.WARNING,this,"e33",e); // error, continuing
               } else {
                  Object[] args = new Object[] { new Integer(totalFailures),
                                             Convert.fromInt(totalFailures),
                                                 new Integer(successiveFailures),
                                             Convert.fromInt(successiveFailures) };
                  throw new Exception(Text.get(this,"e34",args),e);
               }

               // right now, PauseRetryException information isn't handled well ...
               // we just use whatever's on the last failure.  we could add some
               // code to take the maximum of the pause-retry limit, or do other
               // things, but the correct solution is that the errors need to be
               // attached to the items, not the whole upload, and that's hard.
               // fortunately, taking the last failure isn't too bad ...
               // if we get a bunch of errors, they'll often be all the same kind.
            }
         }

         if (isStopping()) return false; // see (*) above
      }

      if (totalFailures != 0) { // we went through all the items, *now* fail
         Object[] args = new Object[] { new Integer(totalFailures),
                                    Convert.fromInt(totalFailures) };
         throw new Exception(Text.get(this,"e35",args),lastFailure);
         // note, totalFailures != 0 clearly implies that lastFailure is set.
         // also, the lastFailure text has already been logged once; oh well.
      }

      return true;
   }

   private boolean uploadItem() throws Exception {

      tracker.fileBegin1(item);
      setFileStatus(Roll.STATUS_FILE_SENDING);

      try {

         File sendFile = new File(roll.rollDir,item.filename);
         Long sizeActual = null; // not scaled

         if (roll.source != Roll.SOURCE_LOCAL) // local images exist in two places at once, need to be identical
         try {
            try {

               TransformConfig tc;
               if      (roll.transformConfig != null) tc = roll.transformConfig;
               else if (roll.transformType   != null) tc = config.transformConfig.derive(roll.transformType.intValue());
               else                                   tc = config.transformConfig;
               // order doesn't matter, we validate that config and type aren't both set

               if (UploadTransform.transform(tc,sendFile,item.rotation,transformFile)) {

                  // send the transformed file instead
                  sendFile = transformFile;
                  sizeActual = new Long(FileUtil.getSize(transformFile));
               }
               // else transform wasn't needed, or couldn't be performed

            } catch (OutOfMemoryError e) {
               throw new Exception(Text.get(this,"e18"),e);
               // normally, out of memory is a serious condition,
               // hard to recover from because you can't expect
               // to be able to run any more code that does anything,
               // but here it probably means we failed to allocate
               // a gigantic buffer for TIFF transformation.
               // should still be plenty of memory for normal use.
            }
         } catch (ThreadStopException e) {
            throw e;
         } catch (Exception e) { // descriptive catch and rethrow
            throw new Exception(Text.get(this,"e32",new Object[] { describeItem(item) }),e);
         }
         // double try block because you can't catch twice in one block

         tracker.fileBegin2(item,roll,sizeActual);
         // pass in the actual size so tracker can report the scaled size.
         // the problem is that the untransformed size is used to
         // compute the total size goal at the start of the whole upload.
         // note, this is the right number even for base-64 web services,
         // see FormDataUploadTransaction.

         HTTPTransaction t = method.getImageUpload(sendFile);
         if ( ! handler.run(regulate(t),pauseAdapter) ) {

            tracker.fileEndIncomplete();
            setFileStatus(Roll.STATUS_FILE_PENDING);
            return false;
         }

         tracker.fileEndComplete();
         // assign before setFileStatus saves
         item.imageID = ((ImageUpload) t).getImageID();
         setFileStatus(Roll.STATUS_FILE_SENT);
         return true;

      } catch (Exception e) {

         tracker.fileEndIncomplete();
         setFileStatus(Roll.STATUS_FILE_PENDING);
         throw e;

      } finally {
         if (transformFile.exists()) transformFile.delete(); // ignore result
         // just to be tidy, try not to leave the transform file lying around
      }
   }

   /**
    * Same as OrderParser.getVersion, but I don't want to share the code.
    */
   private static int getVersion(Node order) throws ValidationException {
      int version;

      String s = XML.getAttributeTry(order,"Version");
      if (s != null) {
         version = Convert.toInt(s);
      } else {
         version = 1; // original version had no version attribute
      }
      if (version < 1 || version > 2) throw new ValidationException(Text.get(UploadThread.class,"e36",new Object[] { Convert.fromInt(version) }));

      return version;
   }

   private String generateOrderXML() throws Exception {

   // read from file

      Document docOriginal = XML.readFile(new File(roll.rollDir,Roll.UPLOAD_FILE));
      Node orderOriginal = XML.getElement(docOriginal,Roll.UPLOAD_TAG_ORDER);

   // create copy

      Document doc = XML.createDocument();
      Node order = doc.importNode(orderOriginal,true);
      doc.appendChild(order);
      XML.removeWhitespace(order);

   // get version information

      orderVersion = getVersion(order);

      // it's slightly rude that we don't check this at poll time,
      // but it's not exactly a data-driven field ... should only
      // fail once before the programmer on the other side fixes it

   // maybe set customer ID

      Node orderInfo = XML.getElement(order,Roll.UPLOAD_TAG_ORDER_INFO);

      String customerID = XML.getNullableText(orderInfo,Roll.UPLOAD_TAG_CUSTOMER_ID);
      if (customerID == null) { // why not just look at roll.expectedCustomerID?

         if (roll.upload.customerID == null) throw new Exception(Text.get(UploadThread.class,"e29"));
         // this would take XML hacking, but I worry about it;
         // also I'll probably do it myself by accident in testing

         XML.createElementText(orderInfo,Roll.UPLOAD_TAG_CUSTOMER_ID,roll.upload.customerID);
      }

   // process images

      Iterator h = XML.getElements(order,Roll.UPLOAD_TAG_ORDER_ITEM);
      while (h.hasNext()) {
         Node orderItem = (Node) h.next();

         Node images = XML.getElement(orderItem,Roll.UPLOAD_TAG_IMAGES);

         Iterator i = XML.getElements(images,Roll.UPLOAD_TAG_FILENAME);
         while (i.hasNext()) {
            Node node = (Node) i.next();

            String name = XML.getText(node);
            Roll.Item item = roll.findItemByFilename(name);
            if (item == null) throw new Exception(Text.get(UploadThread.class,"e30",new Object[] { name }));

            if (item.imageID == null) throw new Exception(Text.get(UploadThread.class,"e31"));
            // again, this would take XML hacking

            XML.replaceElementText(images,node,Roll.UPLOAD_TAG_IMAGE,item.imageID);
            // we can replace nodes without breaking the iteration
         }
      }

   // write to string

      // leave out indentation, it isn't useful here

      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      XML.writeStream(buffer,doc);
      return buffer.toString();
   }

// --- method variation ---

   // keeping old comments below, but here's the new short version:
   // Swift has been shut down and we're using web services for everything now.
   // this doesn't work in various cases (see WebServicesMethod.validate)
   // but most of the cases are academic.  order upload and pro mode
   // are long gone.  the album-subalbum rule is now checked in RollDialog.
   // so, the only real problem is with the two groups of account fields.
   // those only come from FujiPoller and XMLPoller.  the FujiPoller case
   // is pretty easy.  it only fills in nameLast, and I think there might
   // still be people using it, so don't break it, instead allow the last name
   // to slip through and be ignored.  the XMLPoller case is tougher, since
   // there the whole point is often to create accounts with robust account
   // information.  for now, just let it fail.

   // the standard upload method has been working fine, but now we need to use
   // web services if we want to send a roll with tags (job number / lockdown)
   // or with the either of the other two flags set (watermark / exclusive).
   // but, the web services interface is not as full-featured as the standard one.

   // when we start work on a roll, we decide which method to use based on the
   // roll information and the config settings, but after we've started
   // we want to keep using the same method (and with the same settings
   // even if the config has changed).  the method can be determined by
   // looking at the upload object, which has a different set of fields in the
   // two cases.  the lockdown flag there is a good indicator.

   // some design points about the upload object: (*2)
   // * in the web services method, lockdown is always non-null, but watermark
   //   and exclusive have to allow null since they were added later
   // * I made the URL fields nullable because they're only used inside transactions,
   //   so a mistake will still be handled.  (in fact they're only used concatenated
   //   with a query string, so they turn into "null".)
   // * I didn't make the PID nullable because it's used in various places
   //   without null checking ... too much bother.  same idea for uploadID,
   //   except it's even more bother to change from int to Integer.
   // * actually now PID is non-null on both sides, for WSM it's the session ID!
   // * the fields after PID are checked everywhere they're used so we don't
   //   need to validate them
   // * in the standard method, CustomerID is filled in with the upload info,
   //   then OrderID is optionally filled in later
   // * in the web services method, MerchantID, CustomerID, and sometimes AlbumID,
   //   SubalbumInfo, EventCodeDone, and ExtraSessionID
   //   are filled in one at a time by separate transactions

   // now there's also a new interface for local images.  the method there
   // is totally determined by the roll source, and there's no upload data.

   private Method getMethod() { // n.b. none of this is static

      if (roll.source == Roll.SOURCE_LOCAL) {
         return new LocalMethod();

      } else if (roll.upload == null) {

         // Swift has been shut down, use web services for everything now
         return new WebServicesMethod(config.lockdownEnabled,config.watermarkEnabled,config.exclusiveEnabled);

         // for reference here's the old condition for using web services
         /*
                 roll.jobNumber != null // (*3) superset of fields in getTags
              || config.lockdownEnabled
              || config.watermarkEnabled
              || config.exclusiveEnabled
         */

      } else {
         if (roll.upload.lockdown != null) {
            return new WebServicesMethod(roll.upload.lockdown.booleanValue(),defaultFalse(roll.upload.watermark),defaultFalse(roll.upload.exclusive));
         } else {
            return new StandardMethod();
         }
      }
   }

   private static boolean defaultFalse(Boolean b) { return (b != null) ? b.booleanValue() : false; }

   private abstract class Method {

      /**
       * Check that the roll and upload are consistent with the method.
       * Failure should only occur if someone's been hacking the files.
       * Or if they've been using XMLPoller, see above.
       */
      public abstract void validate() throws ValidationException;

      protected void failUpload() throws ValidationException {
         throw new ValidationException(Text.get(UploadThread.class,"e37"));
      }
      protected void failRoll(String key) throws ValidationException {
         throw new ValidationException(Text.get(UploadThread.class,"e38",new Object[] { Text.get(UploadThread.class,key) }));
      }

      public abstract boolean initiate() throws Exception;
      public abstract boolean terminate() throws Exception;

      /**
       * @return A HTTPTransaction that implements ImageUpload.
       */
      public abstract HTTPTransaction getImageUpload(File sendFile);
   }

// --- local method ---

   private class LocalMethod extends Method {

      public void validate() throws ValidationException {

         // validation is important with the other two methods since an upload can
         // go to different methods under different conditions, but local uploads
         // only go here.  so, the only way to get unexpected fields in the record
         // is by hacking, not a case we worry about -- it's sufficient if we just
         // ignore any unexpected fields.
      }

      public boolean initiate() throws Exception {
         return true;
      }

      public boolean terminate() throws Exception {
         return true;
      }

      public HTTPTransaction getImageUpload(File sendFile) {
         return new UploadLocalImage(sendFile);
      }
   }

// --- standard method ---

   private class StandardMethod extends Method {

      public void validate() throws ValidationException {

         if (roll.upload != null) {
            if (    roll.upload.lockdown != null // this one is just a formality
                 || roll.upload.watermark != null
                 || roll.upload.exclusive != null
                 || roll.upload.uploadURL   == null
                 || roll.upload.errorURL    == null
                 || roll.upload.completeURL == null ) failUpload();
            // in theory, uploadID could be zero, or PID could be zero length,
            // so don't check those here.  see also (*2)
         }

         // no roll-based failure here ... this is the old standard method
         // that can send everything except job number and lockdown.
         // and, those were released earlier and are already in use, so we
         // have to grandfather them in (in case of in-progress uploads).
      }

      public boolean initiate() throws Exception {

         if (roll.upload == null) {

            Log.log(Level.INFO,UploadThread.class,"i2");

            if ( ! handler.run(regulate(new PostSecure()),pauseAdapter) ) return false;
            // the transaction fills in roll.upload

            roll.recmodDate = new Date();
            table.update(roll,lock);

            if (isStopping()) return false;
         }

         return true;
      }

      public boolean terminate() throws Exception {

         if (roll.source == Roll.SOURCE_HOT_ORDER && roll.upload.orderID == null) {

            // that's the main function of the order ID field,
            // to let us know when the order upload is done.
            // the fact that it also lets us keep track of the
            // order ID is nice, but doesn't matter right now.

            Log.log(Level.INFO,UploadThread.class,"i8a");

            String orderXML = generateOrderXML(); // before starting session,
               // and outside the handler's retry loop
            // this also fills in the orderVersion field in the thread object

            BeginSession t = new BeginSession(/* ws = */ false);
            if ( ! handler.run(regulate(t),pauseAdapter) ) return false;
            String sessionID = t.getSessionID();

            // it would be nice if we could reliably close the session,
            // but once the thread is told to stop, it doesn't want
            // to run any more transactions ... and I think that's the
            // correct behavior anyway.  the rare unclosed sessions
            // will just have to wait and time out on the server side.

            Log.log(Level.INFO,UploadThread.class,"i8b");

            if ( ! handler.run(regulate(new SubmitOrder(sessionID,orderXML)),pauseAdapter) ) return false;
            // this also uses the orderVersion field in the thread object

            roll.recmodDate = new Date();
            table.update(roll,lock);
            // try to avoid submitting the order more than once!

            Log.log(Level.INFO,UploadThread.class,"i8c");

            try {
               if ( ! handler.run(regulate(new EndSession(sessionID,/* ws = */ false)),pauseAdapter) ) return false;
            } catch (Exception e) {
               // ignore end session errors, the order's already submitted
            }

            if (isStopping()) return false;
         }

         Log.log(Level.INFO,UploadThread.class,"i4");

         if ( ! handler.run(regulate(new PostComplete()),pauseAdapter) ) return false;

         return true;
      }

      public HTTPTransaction getImageUpload(File sendFile) {
         return new UploadItem(sendFile);
      }
   }

// --- web services method ---

   private class WebServicesMethod extends Method {

      private boolean lockdown;
      private boolean watermark;
      private boolean exclusive;

      public WebServicesMethod(boolean lockdown, boolean watermark, boolean exclusive) {
         this.lockdown = lockdown;
         this.watermark = watermark;
         this.exclusive = exclusive;
      }

      public void validate() throws ValidationException {

         // see the new first comment in the "method variation" section!

         if (roll.upload != null) {
            if (    roll.upload.lockdown == null // just a formality
                 // watermark can be null, see the comments up above
                 // exclusive, ditto
                 || roll.upload.uploadID != 0
                 || roll.upload.uploadURL   != null
                 || roll.upload.errorURL    != null
                 || roll.upload.completeURL != null ) failUpload();
            // PID holds session ID so of course it's not null
         }

         // web services doesn't support sending whole order XML, so this doesn't work
         if (    roll.source == Roll.SOURCE_HOT_ORDER
              || roll.expectedCustomerID != null ) failRoll("s18");

         // web services CreateAccount doesn't support this field, but we don't
         // want to break FujiPoller, so let it go through if that's the source
         if (    roll.nameLast != null
              && roll.source != Roll.SOURCE_HOT_FUJI ) failRoll("s19a");

         // web services CreateAccount doesn't support these fields
         if (    roll.street1  != null
              || roll.street2  != null
              || roll.city     != null
              || roll.state    != null
              || roll.zipCode  != null
              || roll.phone    != null
              || roll.country  != null ) failRoll("s19b");

         // I think one of these is account level and one not; anyway they're not there
         if (    roll.notify  != null
              || roll.promote != null ) failRoll("s20");

         // old pro field also not supported, but no big deal since it's not in use
         if (roll.priceList != null) failRoll("s21");

         // event code through web services requires album.  we'll check that in the UI
         // somewhere, this is just to make it foolproof
         if (roll.eventCode != null && roll.album == null) failRoll("s22");

         // claim number is not supported, but it turns out it's not supported in the
         // standard method either, just totally ignored, so don't worry about it.
         // the thing that's really used as the claim number is in fact the album name.

         if (roll.hasSubalbums() && roll.album == null) failRoll("s23");
      }

      public boolean initiate() throws Exception {

         if (roll.upload == null) {

            Log.log(Level.INFO,UploadThread.class,"i9");

            BeginPartnerSession t = new BeginPartnerSession();
            if ( ! handler.run(regulate(t),pauseAdapter) ) return false;

            Roll.Upload upload = new Roll.Upload();
            upload.lockdown = new Boolean(lockdown);
            upload.watermark = new Boolean(watermark);
            upload.exclusive = new Boolean(exclusive);
            upload.uploadID = 0; // the default value, just to be clear
            upload.PID = t.getSessionID();
            roll.upload = upload; // atomic update, not that it matters

            roll.recmodDate = new Date();
            table.update(roll,lock);

            if (isStopping()) return false;
         }

         if (roll.upload.merchantID == null) {

            // the merchant ID is required on UploadFile when there are tags
            // or when either of the other two flags is set, which right now
            // means always, and we also use it on CreateAccount.
            // so, although in theory maybe we could optimize this step away,
            // in practice we should just do it.  it's also nice to validate
            // the location ID a bit, since that's missing in this method.

            Log.log(Level.INFO,UploadThread.class,"i13");

            CheckLocationStatus t = new CheckLocationStatus();
            if ( ! handler.run(regulate(t),pauseAdapter) ) return false;

            roll.upload.merchantID = t.getMerchantID();

            roll.recmodDate = new Date();
            table.update(roll,lock);

            if (isStopping()) return false;
         }

         // order matters here: MerchantID is used on CreateAccount

         if (roll.upload.customerID == null) {
            String userID = null;

            if (userID == null) { // look up account

               Log.log(Level.INFO,UploadThread.class,"i10");

               GetUserID t = new GetUserID();
               if ( ! handler.run(regulate(t),pauseAdapter) ) return false;

               userID = t.getUserID(); // can still be null
            }

            if (userID == null) { // account doesn't exist, create it

               Log.log(Level.INFO,UploadThread.class,"i11");

               CreateAccount t = new CreateAccount();
               if ( ! handler.run(regulate(t),pauseAdapter) ) return false;

               userID = t.getUserID(); // can never be null
            }

            roll.upload.customerID = userID;

            roll.recmodDate = new Date();
            table.update(roll,lock);

            if (isStopping()) return false;
         }

         // order matters here: UserID is used on CreateAlbum

         if (roll.upload.albumID == null && roll.album != null) {

            // we only need the album ID if there's an album name filled in.
            // in fact the transaction will crash if the album name is null,
            // so be careful about that!
            //
            // in the old upload method, the server defaulted the album name
            // to the roll ID, but here it defaults to the date.  we could
            // preserve the old behavior by applying the default on the LC side,
            // but actually the change is an improvement, leave it as is.
            //
            // note, in spite of the name, the transaction doesn't always
            // create a new album, it can look up an existing one as well.

            Log.log(Level.INFO,UploadThread.class,"i14");

            CreateAlbum t = new CreateAlbum();
            if ( ! handler.run(regulate(t),pauseAdapter) ) return false;

            roll.upload.albumID = t.getAlbumID();

            roll.recmodDate = new Date();
            table.update(roll,lock);

            if (isStopping()) return false;
         }

         // order matters here: AlbumID is used to create subalbums

         Iterator i = roll.items.iterator();
         while (i.hasNext()) {
            Roll.Item ri = (Roll.Item) i.next(); // don't collide with UploadThread.item!
            if (ri.subalbum != null) {
               String subalbumID = findOrCreateSubalbum(ri.subalbum,null);
               if (subalbumID == null) return false; // stopping
            }
         }
         // we could put this in with the item loop, but I like it up here

         // order matters here: AlbumID is used on SetEventCode

         if (roll.eventCode != null && ! defaultFalse(roll.upload.eventCodeDone)) {

            Log.log(Level.INFO,UploadThread.class,"i17");

            if ( ! handler.run(regulate(new SetEventCode()),pauseAdapter) ) return false;

            roll.upload.eventCodeDone = Boolean.TRUE;

            roll.recmodDate = new Date();
            table.update(roll,lock);

            if (isStopping()) return false;
         }

         // order sort of matters here: UserID is used on BeginUploadSession

         if (roll.upload.extraSessionID == null && roll.email.equals(Roll.ANONYMOUS_ACCOUNT) && roll.album != null) {

            // this creates a record in the upload_session table.
            // that table is really just part of the standard upload method (Surge),
            // but the claim process uses it for now, so we need to create our own
            // record in that case.  by the way, the transaction assumes the album
            // name isn't null.
            // we can't use claimNumber as a condition because it's null for claims
            // that are entered by hand in the UI.

            // other design notes: right now, all OPEN uploads are pro stuff,
            // nothing we want to send email about.  if some day that changes,
            // remember that creating an upload_session won't produce email.
            // here are the options.
            //
            // * go back to the 6.3.1 method of calling Surge with an empty upload.
            // the problem here is, for more general uploads there might not be an album,
            // then we're in trouble because the default album names are different
            // in Surge and in OPEN.  probably the solution is to generate it here in LC.
            //
            // * split out the part of LCUploadComplete that sends email and call that.
            //
            // * add to the mail framework that John V is building for partner uploads.

            Log.log(Level.INFO,UploadThread.class,"i15");

            if ( ! handler.run(regulate(new BeginUploadSession()),pauseAdapter) ) return false;

            // transaction fills in roll.upload.extraSessionID

            roll.recmodDate = new Date();
            table.update(roll,lock);

            if (isStopping()) return false;
         }

         return true;
      }

      public boolean terminate() throws Exception {

         if (roll.upload.extraSessionID != null && roll.upload.extraSessionID.length() > 0) {

            // we make extraSessionID the empty string to mark this done;
            // note empty string is never returned by BeginUploadSession.

            Log.log(Level.INFO,UploadThread.class,"i16");

            if ( ! handler.run(regulate(new EndUploadSession()),pauseAdapter) ) return false;

            // transaction clears roll.upload.extraSessionID to empty string

            roll.recmodDate = new Date();
            table.update(roll,lock);

            if (isStopping()) return false;
         }

         if ( ! defaultFalse(roll.upload.uploadEmailDone) ) {

            Log.log(Level.INFO,UploadThread.class,"i22");

            if ( ! handler.run(regulate(new SendUploadEmail()),pauseAdapter) ) return false;

            roll.upload.uploadEmailDone = Boolean.TRUE;

            roll.recmodDate = new Date();
            table.update(roll,lock);

            if (isStopping()) return false;
         }

         Log.log(Level.INFO,UploadThread.class,"i12");

         EndSession t = new EndSession(roll.upload.PID,/* ws = */ true);
         if ( ! handler.run(regulate(t),pauseAdapter) ) return false;

         // will change status, that's how we remember session ended

         return true;
      }

      public HTTPTransaction getImageUpload(File sendFile) {
         return new UploadBase64(sendFile,lockdown,watermark,exclusive);
      }
   }

// --- subalbum helpers ---

   /**
    * @return The subalbum ID, or null if not found.
    */
   private String findSubalbum(String subalbum) {
      // iterate backward since we add forward and hits will usually be on most recent ones
      ListIterator li = roll.upload.subalbumInfo.listIterator(roll.upload.subalbumInfo.size());
      while (li.hasPrevious()) {
         Roll.SubalbumInfo rsi = (Roll.SubalbumInfo) li.previous();
         if (rsi.name.equals(subalbum)) return rsi.id;
      }
      return null;
   }

   /**
    * @return The subalbum ID, or null if stopping.
    */
   private String createSubalbum(String subalbum, String parentID, String name) throws Exception {

      Log.log(Level.INFO,UploadThread.class,"i18",new Object[] { subalbum });

      CreateSubalbum t = new CreateSubalbum(parentID,name);
      if ( ! handler.run(regulate(t),pauseAdapter) ) return null;

      Roll.SubalbumInfo rsi = new Roll.SubalbumInfo();
      rsi.name = subalbum;
      rsi.id = t.subalbumID;
      roll.upload.subalbumInfo.add(rsi);

      roll.recmodDate = new Date();
      table.update(roll,lock);

      return isStopping() ? null : t.subalbumID;
   }

   /**
    * @param subalbum The subalbum as a string object.
    * @param file The subalbum as a file object, if known.
    * @return The subalbum ID, or null if stopping.
    */
   private String findOrCreateSubalbum(String subalbum, File file) throws Exception {

      String subalbumID = findSubalbum(subalbum);
      if (subalbumID != null) return subalbumID;

      // not found, have to create it, and maybe its parents recursively

      if (file == null) file = new File(subalbum); // defer construction

      File parent = file.getParentFile();
      String parentID;
      if (parent == null) {
         parentID = roll.upload.albumID;
      } else {
         parentID = findOrCreateSubalbum(parent.getPath(),parent);
         if (parentID == null) return null;
      }

      return createSubalbum(subalbum,parentID,file.getName());
   }

// --- image tags ---

   private static final int IMAGE_TAG_JOBNUMBER = 7;
   private static final int IMAGE_TAG_LOCKDOWN  = 8;

   private static class Tag {

      public int tagID;
      public String tagValue;

      public Tag(int tagID, String tagValue) { this.tagID = tagID; this.tagValue = tagValue; }
   }

   /**
    * @return The TagsXML value as a string, or null if the roll
    *         doesn't require any tags.
    */
   private static String getTags(Roll roll, boolean lockdown) throws IOException {

   // build list

      // (*3) subset of fields in getMethod

      LinkedList list = new LinkedList();
      boolean locked = false;

      if (roll.jobNumber != null) {
         list.add(new Tag(IMAGE_TAG_JOBNUMBER,roll.jobNumber));
         locked = true;
         // job number tag also locks down roll
      }

      if (lockdown && ! locked) {
         list.add(new Tag(IMAGE_TAG_LOCKDOWN,"x")); // value doesn't matter
         locked = true;
         // just so that final value is correct
      }

   // check if empty

      if (list.isEmpty()) return null; // no tags

   // make into XML

      Document doc = XML.createDocument();
      Node top = XML.createElement(doc,"TAGINFO");

      Iterator i = list.iterator();
      while (i.hasNext()) {
         Tag tag = (Tag) i.next();

         Node node = XML.createElement(top,"IMAGETAG");
         XML.createElementText(node,"TAGID",Convert.fromInt(tag.tagID));
         XML.createElementText(node,"TAGVALUE",tag.tagValue);
      }

   // write to string

      // leave out indentation, it isn't useful here

      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      XML.writeStream(buffer,doc,/* omit = */ true);
      return buffer.toString();
   }

// --- transaction helpers ---

   private static void throwInvalidMerchant() throws Exception {
      throw new ThreadStopException(Text.get(UploadThread.class,"e4a"));
   }

   private static void throwInvalidPassword() throws Exception {
      User.tell(User.CODE_INVALID_PASSWORD,Text.get(UploadThread.class,"e16"));
      throw new ThreadStopException(Text.get(UploadThread.class,"e4b"));
   }

   /**
    * @param codeMissing Because ASP doesn't distinguish a blank parameter from a missing one,
    *                    a blank password may produce a different error than an incorrect one.
    *                    Thus, we need a second code to detect and handle password errors.
    */
   private Node receiveStandard(InputStream inputStream, int codeMerchant, int codePassword,
                                                         int codeMissing, int codeWarning,
                                                         int codeEventInUse, int codeEventWrong) throws Exception {

   // read response

      Document doc = XML.readStream(inputStream);
      Node n1 = XML.getElement(doc,"xml");
      Node n2 = XML.getElement(n1,"rs:data");
      Node n3 = XML.getElement(n2,"rs:insert");
      Node node = XML.getElement(n3,"z:row");

   // check error

      // do this before parsing any other fields,
      // because if there's an error they may be blank and not parse

      String error = XML.getAttribute(node,"Error");
      int errorCode = Convert.toInt(XML.getAttribute(node,"ErrorCode"));

      if (errorCode != 0) {
              if (errorCode == codeMerchant) throwInvalidMerchant();
         else if (errorCode == codePassword || errorCode == codeMissing) throwInvalidPassword();

         // for these two, we just want to replace the server message with a custom one
         else if (errorCode == codeEventInUse) {
            throw new IOException(Text.get(UploadThread.class,"e19"));
         } else if (errorCode == codeEventWrong) {
            throw new IOException(Text.get(UploadThread.class,"e20"));

         } else {
            Exception e = new IOException(Text.get(UploadThread.class,"e7",new Object[] { Convert.fromInt(errorCode), error }));
            if (errorCode == codeWarning) {
               Log.log(roll,Level.WARNING,UploadThread.class,"e17",e);
               // the roll ID is the only reason the function isn't static
            } else {
               throw e;
            }
         }
      }

   // so far so good

      return node;
   }

   private static String zeroOne(boolean b) { return b ? "1" : "0"; }

   private static String getRollID(Node node) throws Exception {

      // for the usual mysterious reasons, the server sometimes sends a reply
      // without a roll ID in it.  take a two-pronged approach, log and retry.

      try {

         return XML.getAttribute(node,"RollID");

      } catch (ValidationException e) {

         String text = XML.reportAttributes(node);
         Log.log(Level.FINE,UploadThread.class,"i6",new Object[] { text });

         throw new RetryableException(Text.get(UploadThread.class,"e21"),e);
      }
   }

   private void checkRollID(String rollID) throws IOException {

      // the returned roll ID should match either the roll ID or the album name.
      // technically it should always match the album name if that's not null,
      // but I want to allow either possibility, in case we want to revert later.
      //
      // see also the album name calculation in UploadItem.getParameters

      String stringID = Convert.fromInt(roll.rollID);
      if (rollID.equals(stringID)) return; // match

      if (roll.album == null) throw new IOException(Text.get(this,"e10",new Object[] { rollID, stringID }));
         // we don't have to exclude null from the next line,
         // since string.equals(null) is always false,
         // but we do have to exclude it from the format function.

      if (rollID.equals(Roll.limitAlbum(roll.album))) return; // match

      throw new IOException(Text.get(this,"e15",new Object[] { rollID, Roll.limitAlbum(roll.album), stringID }));
   }

   /**
    * When we send files with the same name to the server,
    * the server deals with it by putting the second name
    * into duplicate form, with "x.y" becoming "x(nnn).y".
    * Then it sends that name back to LabCenter as part of
    * the HTTP response, so LC needs to be able to
    * recognize such names.  That's what this function does.
    * Note, it does <i>not</i> recognize unmodified names.
    */
   private static boolean isDuplicateForm(String modified, String original) {

   // suffixes must match exactly

      if ( ! Roll.getSuffix(modified).equals(Roll.getSuffix(original)) ) return false;

      modified = Roll.getPrefix(modified);
      original = Roll.getPrefix(original);

   // prefix must be an extension of the other

      if ( ! modified.startsWith(original) ) return false;

      modified = modified.substring(original.length());

   // extension must be of the form '([0-9]+)'

      int len = modified.length();
      if (len < 3) return false;

      if ( modified.charAt(0) != '(' || modified.charAt(len-1) != ')' ) return false;

      for (int i=1; i<len-1; i++) {
         char c = modified.charAt(i);
         if (c < '0' || c > '9') return false;
      }

   // well, I guess that's a duplicate form then

      return true;
   }

   private static boolean isCloseEnough(String modified, String original) {

      if (modified.equals(original)) return true;
      if (isDuplicateForm(modified,original)) return true;

      // the rest of this works around a(nother) server issue.
      // LC sends the file name as raw bytes outside XML, e.g. "A&D001.jpg".
      // LC expects to get back the file name inside XML,
      // so that the raw bytes are encoded, e.g. "A&amp;D001.jpg",
      // but in fact the server sends "A&amp;amp;D001.jpg"

      modified = XML.manualDecode(modified);

      if (modified.equals(original)) return true;
      if (isDuplicateForm(modified,original)) return true;

      return false;
   }

   private String getDealerPassword() {

      // the dealer password is basically just roll.dealer.password,
      // but we want to look it up in the dealer list
      // in case the password has changed since the roll was created.

      Dealer dealer = Dealer.findByID(dealers,roll.dealer.id);
      if (dealer == null) dealer = roll.dealer;
      return dealer.password;
   }

   private static String describeItem(Roll.Item item) {
      // sending null as an argument to s8b isn't great, but it's harmless, formats as "null" if used
      String key = (item.subalbum != null) ? "s8a" : "s8b";
      return Text.get(UploadThread.class,key,new Object[] { item.getOriginalFilename(), item.subalbum });
   }

   private Boolean getUseNotify() {
      return roll.email.equals(Roll.ANONYMOUS_ACCOUNT) ? Boolean.FALSE : roll.notify;
      // never send upload notifications to the anonymous address, they would bounce
   }

// --- HTTP transactions ---

   // note, these are *not* static transactions, they use UploadThread fields

   private class PostSecure extends PostTransaction {

      public String describe() { return Text.get(UploadThread.class,"s2"); }
      protected String getFixedURL() { return config.secureURL; }
      protected void getParameters(Query query) throws IOException {

         roll.assertWholesale(merchantConfig.isWholesale);
         if (merchantConfig.isWholesale) {
            query.add("merchant",roll.dealer.id); // dealer ID is stored as a string here
            query.addPasswordObfuscate("encpassword",getDealerPassword());
         } else {
            query.add("merchant",Convert.fromInt(merchantConfig.merchant));
            query.addPasswordObfuscate("encpassword",merchantConfig.password);
         }

         query.add("Roll",Convert.fromInt(roll.rollID));
         query.add("email",roll.email);
         query.add("files",Convert.fromInt(roll.items.size()));
         query.add("total",Convert.fromLong(tracker.getTotalSizeGoal())); // untransformed

         if (roll.source == Roll.SOURCE_HOT_ORDER) query.add("OrderUpload",zeroOne(true));
         // else don't even send the field

         // optional fields
         if (roll.nameFirst != null) query.add("FirstName",roll.nameFirst);
         if (roll.nameLast  != null) query.add("LastName", roll.nameLast);
         if (roll.street1   != null) query.add("Street1",  roll.street1);
         if (roll.street2   != null) query.add("Street2",  roll.street2);
         if (roll.city      != null) query.add("City",     roll.city);
         if (roll.state     != null) query.add("State",    roll.state);
         if (roll.zipCode   != null) query.add("ZIP",      roll.zipCode);
         if (roll.phone     != null) query.add("Phone",    roll.phone);
         if (roll.country   != null) query.add("Country",  roll.country);
         if (roll.album     != null) query.add("Album",    Roll.limitAlbum(roll.album));
         Boolean useNotify = getUseNotify();
         if (  useNotify    != null) query.add("Notify",   zeroOne(useNotify.booleanValue()));
         if (roll.promote   != null) query.add("emailpromotions",
                                                           zeroOne(roll.promote.booleanValue()));
         if (roll.password    != null) query.add("UserPassword",roll.password); // (**)
         if (roll.claimNumber != null) query.add("ClaimNumber", roll.claimNumber);

         // optional fields for LC Pro
         if (roll.eventCode != null) query.add("EventCode",roll.eventCode);
         if (roll.priceList != null) query.add("PriceList",roll.priceList.id);
         // priceList.name is not sent, it's just a UI convenience

         // (**) roll passwords aren't treated as sensitive information.
         // they're not stored as PasswordField, and don't use addPasswordCleartext.
         // this seems reasonable, since they arrive as cleartext in hot folder XML.
      }

      protected boolean receive(InputStream inputStream) throws Exception {
         Node node = receiveStandard(inputStream,34,35,32,0,70,71);
         // code 32 is "missing a required querystring", which is more general
         // than we want, but in practice it only happens for a blank password.

      // read other fields

         Roll.Upload upload = new Roll.Upload();
         upload.uploadID = Convert.toInt(XML.getAttribute(node,"UploadID"));
         XML.getAttribute(node,"Date"); // verify presence, ignore
         XML.getAttribute(node,"Time"); // verify presence, ignore
         upload.PID = XML.getAttribute(node,"PID");
         upload.uploadURL = XML.getAttribute(node,"URL");
         upload.errorURL = XML.getAttribute(node,"ErrorURL");
         upload.completeURL = XML.getAttribute(node,"AxonCompleteURL");
         upload.customerID = XML.getAttribute(node,"CustomerID");

      // process response

         if (roll.expectedCustomerID != null && ! upload.customerID.equals(roll.expectedCustomerID)) {
            throw new IOException(Text.get(UploadThread.class,"e28",new Object[] { upload.customerID, roll.expectedCustomerID }));
         }

         roll.upload = upload; // only update once success is guaranteed
         return true;
      }
   }

   private interface ImageUpload {
      String getImageID();
   }

   private class UploadItem extends UploadTransaction implements ImageUpload {

      private String imageID;
      public String getImageID() { return imageID; }

      // send file may be original or transformed
      private File sendFile;
      private String sendName;
      public UploadItem(File sendFile) {
         this.sendFile = sendFile;
         sendName = Roll.limitFilename(item.getOriginalFilename(),sendFile.getName());
      }

      public String describe() { return Text.get(UploadThread.class,"s3",new Object[] { describeItem(item) }); }
      protected String getFixedURL() { return roll.upload.uploadURL; }
      protected void getParameters(Query query) throws IOException {
         query.add("PID",roll.upload.PID);

         String albumPath = (roll.album != null) ? Roll.limitAlbum(roll.album) : Convert.fromInt(roll.rollID);
         if (item.subalbum != null) albumPath = new File(albumPath,item.subalbum).getPath();
         query.add("a",albumPath);
         // strange but true, parameter is supposed to include the top-level album name
      }

      protected String getFilename() { return sendName; }
      protected File getFile() { return sendFile; }
      protected FileUtil.Callback getCallback() { return tracker; }

      protected boolean receive(InputStream inputStream) throws Exception {
         Node node = receiveStandard(inputStream,0,0,0,0,0,0);

      // read other fields

         int uploadID = Convert.toInt(XML.getAttribute(node,"UploadID"));
         XML.getAttribute(node,"Date"); // verify presence, ignore
         XML.getAttribute(node,"Time"); // verify presence, ignore
         String PID = XML.getAttribute(node,"PID");
         String rollID = getRollID(node);
         int count = Convert.toInt(XML.getAttribute(node,"TotalImages"));
         String filename = XML.getAttribute(node,"FileName");
         long size = Convert.toLong(XML.getAttribute(node,"TotalTransferSize"));
         imageID = XML.getAttribute(node,"ImageID");

      // process response

         if (uploadID != roll.upload.uploadID) {
            throw new IOException(Text.get(UploadThread.class,"e8",new Object[] { Convert.fromInt(uploadID), Convert.fromInt(roll.upload.uploadID) }));
         }

         if ( ! PID.equals(roll.upload.PID) ) {
            throw new IOException(Text.get(UploadThread.class,"e9",new Object[] { PID, roll.upload.PID }));
         }

         checkRollID(rollID);

         if (count != 1) {
            throw new RetryableException(Text.get(UploadThread.class,"e11",new Object[] { Convert.fromInt(count), Convert.fromInt(1) }));
            //
            // the server sometimes reports count 0 ... we don't know why,
            // but it does, and then it goes away on retry.  so, retry it.
         }

         if ( ! isCloseEnough(filename,sendName) ) {
            throw new IOException(Text.get(UploadThread.class,"e12",new Object[] { filename, sendName }));
         }

         // I think this was necessary once, but on Surge it seems not to be,
         // and it causes trouble when Surge decides to resize a large image.
         //
         // why it's not necessary:  LabCenter sends a Content-length header
         // and the server pays attention to it.  the case I'm worried about
         // is when the image stream gets cut off in the middle without an
         // error signal being generated, and in that case the server just waits
         // and times out.  I tested it, see the archive folder dated 3/24/14.
/*
         long sizeGoal = FileUtil.getSize(sendFile);
         if (size != sizeGoal) {
            throw new RetryableException(Text.get(UploadThread.class,"e13",new Object[] { Convert.fromLong(size), Convert.fromLong(sizeGoal) }));
         }
*/

         return true;
      }
   }

   private class PostComplete extends PostTransaction {

      public String describe() { return Text.get(UploadThread.class,"s4"); }
      protected String getFixedURL() { return roll.upload.completeURL; }
      protected void getParameters(Query query) throws IOException {
         query.add("PID",roll.upload.PID);

         roll.assertWholesale(merchantConfig.isWholesale);
         if (merchantConfig.isWholesale) {
            query.add("MID",roll.dealer.id); // dealer ID is stored as a string here
            query.addPasswordObfuscate("encpassword",getDealerPassword());
         } else {
            query.add("MID",Convert.fromInt(merchantConfig.merchant));
            query.addPasswordObfuscate("encpassword",merchantConfig.password);
         }

         // re-send this to help the server guys out
         Boolean useNotify = getUseNotify();
         if (useNotify != null) query.add("Notify",zeroOne(useNotify.booleanValue()));
      }

      protected boolean receive(InputStream inputStream) throws Exception {
         Node node = receiveStandard(inputStream,24,25,23,26,0,0);
         // code 23 is specifically "missing password", no problem here

      // read other fields

         int uploadID = Convert.toInt(XML.getAttribute(node,"UploadID"));
         XML.getAttribute(node,"Date"); // verify presence, ignore
         XML.getAttribute(node,"Time"); // verify presence, ignore
         String PID = XML.getAttribute(node,"PID");
         String rollID = getRollID(node);

      // process response

         if (uploadID != roll.upload.uploadID) {
            throw new IOException(Text.get(UploadThread.class,"e5",new Object[] { Convert.fromInt(uploadID), Convert.fromInt(roll.upload.uploadID) }));
         }

         if ( ! PID.equals(roll.upload.PID) ) {
            throw new IOException(Text.get(UploadThread.class,"e6",new Object[] { PID, roll.upload.PID }));
         }

         checkRollID(rollID);

         return true;
      }
   }

// --- order upload helpers ---

   private static final String COMMAND_BEGIN_SESSION = "BeginSession";
   private static final String COMMAND_BEGIN_MERCHANT_SESSION = "BeginMerchantSession";
   private static final String COMMAND_END_SESSION   = "EndSession";
   private static final String COMMAND_SUBMIT_ORDER  = "SubmitOrder";
   private static final String COMMAND_SUBMIT_ORDER_NEW = "SubmitOrderNew";

   private static String insecure(String s) {
      final String prefix1 = "https:";
      final String prefix2 = "http:";
      if (s.toLowerCase().startsWith(prefix1)) s = prefix2 + s.substring(prefix1.length());
      return s;
   }

   private static boolean isSuccess(String s) throws ValidationException {
      if      (s.equals("Success")) return true;
      else if (s.equals("Fail"   )) return false;
      else throw new ValidationException(Text.get(UploadThread.class,"e22",new Object[] { s }));
   }
   // this is a one-way conversion, no need to define constants

   private static Node receiveNew(InputStream inputStream, String command) throws Exception {
      return receiveNew(inputStream,command,/* codeFail = */ -1,/* codeHide = */ -1,/* eHide = */ null);
   }
   private static Node receiveNew(InputStream inputStream, String command, int codeFail, int codeHide, Exception eHide) throws Exception {

      Document doc = XML.readStream(inputStream);
      Node node = XML.getElement(doc,command);

      return receiveNewImpl(node,codeFail,codeHide,eHide) ? node : null;
   }

   private static boolean receiveNewImpl(Node node) throws Exception {
      return receiveNewImpl(node,/* codeFail = */ -1,/* codeHide = */ -1,/* eHide = */ null);
   }
   private static boolean receiveNewImpl(Node node, int codeFail, int codeHide, Exception eHide) throws Exception {

      Node n2 = XML.getElement(node,"ErrorInfo");

      Iterator i = XML.getElements(n2,"Error");
      if ( ! i.hasNext() ) return true; // the normal case

      StringBuffer buffer = new StringBuffer();
      int count = 0;
      boolean invalidMerchant = false;
      boolean invalidPassword = false;

      while (i.hasNext()) {
         Node n3 = (Node) i.next();

         String codeString = XML.getElementText(n3,"Code");
         int codeInt = Convert.toInt(codeString);
         String description = XML.getElementText(n3,"Description");
         String message = XML.getElementText(n3,"Message");

         // if the specified failure mode occurs and there are no other errors present, handle.
         if (count == 0 && ! i.hasNext()) {
            if (codeFail != -1 && codeInt == codeFail) return false;
            if (codeHide != -1 && codeInt == codeHide) throw eHide;
         }

         if (codeInt == 16) invalidMerchant = true;
         if (codeInt ==  1) invalidPassword = true;

         buffer.append(' ');
         buffer.append(Text.get(UploadThread.class,"e24",new Object[] { codeString, description, message }));
         count++;
      }

      if (invalidMerchant) throwInvalidMerchant();
      if (invalidPassword) throwInvalidPassword();

      String key = (count == 1) ? "e25" : "e26";
      throw new IOException(Text.get(UploadThread.class,key,new Object[] { buffer.toString() }));
   }

// --- order upload transactions ---

   // as I'm writing these, we don't expect them to be used in wholesale mode,
   // but it's easy to set things up so it will work if we ever want to do it.

   private class BeginSession extends FormDataTransaction {

      private boolean ws;
      public BeginSession(boolean ws) { this.ws = ws; }

      private String sessionID;
      public String getSessionID() { return sessionID; }

      public String describe() { return Text.get(UploadThread.class,"s5"); }
      protected String getFixedURL() { return combine(ws ? config.webServicesURL : config.orderUploadURL,ws ? COMMAND_BEGIN_MERCHANT_SESSION : COMMAND_BEGIN_SESSION); }
      protected void getFormData(Query query) throws IOException {

         String loc = ws ? "LocationID" : "locationID";
         String pwd = ws ? "Password"   : "password";

         roll.assertWholesale(merchantConfig.isWholesale);
         if (merchantConfig.isWholesale) {
            query.add(loc,roll.dealer.id);
            query.addPasswordCleartext(pwd,getDealerPassword());
         } else {
            query.add(loc,Convert.fromInt(merchantConfig.merchant));
            query.addPasswordCleartext(pwd,merchantConfig.password);
         }
      }

      protected boolean receive(InputStream inputStream) throws Exception {
         Node node = receiveNew(inputStream,ws ? COMMAND_BEGIN_MERCHANT_SESSION : COMMAND_BEGIN_SESSION);

         sessionID = XML.getElementText(node,"SessionID");

         return true;
      }
   }

   private class EndSession extends FormDataTransaction {

      private String sessionID;
      private boolean ws;
      public EndSession(String sessionID, boolean ws) { this.sessionID = sessionID; this.ws = ws; }

      public String describe() { return Text.get(UploadThread.class,"s6"); }
      protected String getFixedURL() { return combine(ws ? config.webServicesURL : config.orderUploadURL,COMMAND_END_SESSION); }
      protected void getFormData(Query query) throws IOException {

         String ses = ws ? "SessionID" : "sessionID";

         query.add(ses,sessionID);
      }

      protected boolean receive(InputStream inputStream) throws Exception {
         Node node = receiveNew(inputStream,COMMAND_END_SESSION);

         // a failure should always be accompanied by errors, but check just in case
         String result = XML.getElementText(node,"Status");
         if ( ! isSuccess(result) ) throw new IOException(Text.get(UploadThread.class,"e23"));

         return true;
      }
   }

   private class SubmitOrder extends FormDataTransaction {

      private String sessionID;
      private String orderXML;
      public SubmitOrder(String sessionID, String orderXML) { this.sessionID = sessionID; this.orderXML = orderXML; }

      public String describe() { return Text.get(UploadThread.class,"s7"); }
      protected String getFixedURL() { return combine(config.orderUploadURL,(orderVersion >= 2) ? COMMAND_SUBMIT_ORDER_NEW : COMMAND_SUBMIT_ORDER); }
      protected void getFormData(Query query) throws IOException {

         query.add("sessionID",sessionID);
         query.add("orderXML",orderXML);
         query.add("throwError",""); // only for debugging, but required?!
      }

      protected boolean receive(InputStream inputStream) throws Exception {
         Node node = receiveNew(inputStream,COMMAND_SUBMIT_ORDER);

         // a failure should always be accompanied by errors, but check just in case
         String result = XML.getElementText(node,"OrderStatus");
         if ( ! isSuccess(result) ) throw new IOException(Text.get(UploadThread.class,"e27"));

         roll.upload.orderID = XML.getElementText(node,"OrderID");

         return true;
      }
   }

// --- web services transactions ---

   private static final String COMMAND_BEGIN_PARTNER_SESSION = "BeginPartnerSession";
   private static final String COMMAND_CHECK_LOCATION_STATUS = "CheckLocationStatus";
   private static final String COMMAND_GET_USER_ID = "GetUserIDPartnerSource";
   private static final String COMMAND_GET_USER_ID_SHORT = "GetUserID";
   private static final String COMMAND_CREATE_ACCOUNT = "CreateAccount";
   private static final String COMMAND_CREATE_ALBUM = "CreateAlbum";
   private static final String COMMAND_CREATE_ALBUM_CHILD = "CreateAlbumChild";
   private static final String COMMAND_SET_EVENT_CODE = "SetEventCode";
   private static final String COMMAND_SEND_UPLOAD_EMAIL = "SendUploadCompleteEmail";
   private static final String COMMAND_UPLOAD_FILE = "UploadFileWatermarkExclusiveTagsSource";
   private static final String COMMAND_UPLOAD_FILE_ALBUM = "UploadFileWatermarkExclusiveTagsSourceAlbum";
   private static final String COMMAND_UPLOAD_FILE_SHORT  = "UploadFile";

   private static final String PARTNER_ID_LIFEPICS = "5";
   private static final String PARTNER_PASSWORD_LIFEPICS = "k7we7a2";
   //
   private static final String PARTNER_SOURCE_LIFEPICS = "102";
   // if we ever do order uploads through this API, we should
   // think more about this, it would need to be configurable

   private static final String LABCENTER_PASSWORD = "vfB7PWB9y2nhmqNv6EXQzWmU9a02LrSHLWewIgGwOMm4e1Ti";
   // a special super-user password for GetUserID, since we don't generally know the account password

   private class BeginPartnerSession extends FormDataTransaction {

      private String sessionID;
      public String getSessionID() { return sessionID; }

      public String describe() { return Text.get(UploadThread.class,"s12"); }
      protected String getFixedURL() { return combine(config.webServicesURL,COMMAND_BEGIN_PARTNER_SESSION); }
      protected void getFormData(Query query) throws IOException {

         query.add("PartnerID",PARTNER_ID_LIFEPICS);
         query.addPasswordCleartext("Password",PARTNER_PASSWORD_LIFEPICS); // (*4)
      }

      protected boolean receive(InputStream inputStream) throws Exception {
         Node node = receiveNew(inputStream,COMMAND_BEGIN_PARTNER_SESSION);

         sessionID = XML.getElementText(node,"SessionID");

         return true;
      }
   }

   private class CheckLocationStatus extends FormDataTransaction {

      private String merchantID;
      public String getMerchantID() { return merchantID; }

      public String describe() { return Text.get(UploadThread.class,"s13"); }
      protected String getFixedURL() { return combine(config.webServicesURL,COMMAND_CHECK_LOCATION_STATUS); }
      protected void getFormData(Query query) throws IOException {

         roll.assertWholesale(merchantConfig.isWholesale);
         String locationID = merchantConfig.isWholesale ? roll.dealer.id : Convert.fromInt(merchantConfig.merchant);

         query.add("SessionID",roll.upload.PID);
         query.add("LocationID",locationID);
      }

      protected boolean receive(InputStream inputStream) throws Exception {
         Node node = receiveNew(inputStream,COMMAND_CHECK_LOCATION_STATUS);

         merchantID = XML.getElementText(node,"MerchantID");
         // plus a bunch of other stuff we don't care about

         return true;
      }
   }

   private class GetUserID extends FormDataTransaction {

      private String userID;
      public String getUserID() { return userID; }

      public String describe() { return Text.get(UploadThread.class,"s9"); }
      protected String getFixedURL() { return combine(config.webServicesURL,COMMAND_GET_USER_ID); }
      protected void getFormData(Query query) throws IOException {

         query.add("SessionID",roll.upload.PID);
         query.add("PartnerSourceID",PARTNER_SOURCE_LIFEPICS);
         query.add("EmailAddress",roll.email);
         query.addPasswordCleartext("Password",LABCENTER_PASSWORD); // (*4)

         // (*4) not really necessary to use addPassword__ functions for these,
         // since they're in the body not the query string, but it's good form.
      }

      protected boolean receive(InputStream inputStream) throws Exception {
         Node node = receiveNew(inputStream,COMMAND_GET_USER_ID_SHORT,/* codeFail = */ 5,/* codeHide = */ -1,/* eHide = */ null);

         if (node == null) { userID = null; return true; }
         // code 5 means invalid user or password, so for our purposes the account
         // doesn't exist and we need to create it.  report success with a null ID.
         // no need to set null, but it's good form in general because of retries.

         userID = XML.getElementText(node,"UserID");
         // there are four other fields but we don't care about them.
         // FYI, the user ID is encrypted, not just a simple integer

         return true;
      }
   }

   private class CreateAccount extends FormDataTransaction {

      private String userID;
      public String getUserID() { return userID; }

      public String describe() { return Text.get(UploadThread.class,"s10"); }
      protected String getFixedURL() { return combine(config.webServicesURL,COMMAND_CREATE_ACCOUNT); }
      protected void getFormData(Query query) throws IOException {

         query.add("SessionID",roll.upload.PID);
         query.add("PartnerSourceID",PARTNER_SOURCE_LIFEPICS);
         query.add("MerchantID",roll.upload.merchantID);
         query.add("EmailAddress",roll.email);
         query.add("Password", (roll.password  != null) ? roll.password  : generateDigits(4));
         query.add("FirstName",(roll.nameFirst != null) ? roll.nameFirst : "Valued Guest");
      }

      protected boolean receive(InputStream inputStream) throws Exception {
         Node node = receiveNew(inputStream,COMMAND_CREATE_ACCOUNT);

         userID = XML.getElementText(node,"UserID");
         // FYI, the user ID is encrypted, not just a simple integer

         return true;
      }
   }

   private class CreateAlbum extends FormDataTransaction {

      private String albumID;
      public String getAlbumID() { return albumID; }

      public String describe() { return Text.get(UploadThread.class,"s14"); }
      protected String getFixedURL() { return combine(config.webServicesURL,COMMAND_CREATE_ALBUM); }
      protected void getFormData(Query query) throws IOException {

         query.add("SessionID",roll.upload.PID);
         query.add("UserID",roll.upload.customerID);
         query.add("AlbumName",Roll.limitAlbum(roll.album)); // don't call this if album is null!
      }

      protected boolean receive(InputStream inputStream) throws Exception {
         Node node = receiveNew(inputStream,COMMAND_CREATE_ALBUM);

         albumID = XML.getElementText(node,"AlbumID");

         return true;
      }
   }

   private class CreateSubalbum extends FormDataTransaction { // almost same web service as CreateAlbum

      private String subalbumID;
      public String getSubalbumID() { return subalbumID; }

      private String parentID;
      private String name;
      public CreateSubalbum(String parentID, String name) { this.parentID = parentID; this.name = name; }

      public String describe() { return Text.get(UploadThread.class,"s24"); }
      protected String getFixedURL() { return combine(config.webServicesURL,COMMAND_CREATE_ALBUM_CHILD); }
      protected void getFormData(Query query) throws IOException {

         query.add("SessionID",roll.upload.PID);
         query.add("UserID",roll.upload.customerID);
         query.add("ParentAlbumID",parentID);
         query.add("AlbumName",name);
      }

      protected boolean receive(InputStream inputStream) throws Exception {
         Node node = receiveNew(inputStream,COMMAND_CREATE_ALBUM);

         subalbumID = XML.getElementText(node,"AlbumID");

         return true;
      }
   }

   private class SetEventCode extends FormDataTransaction {

      public String describe() { return Text.get(UploadThread.class,"s17"); }
      protected String getFixedURL() { return combine(config.webServicesURL,COMMAND_SET_EVENT_CODE); }
      protected void getFormData(Query query) throws IOException {

         query.add("SessionID",roll.upload.PID);
         query.add("MerchantID",roll.upload.merchantID);
         query.add("AlbumID",roll.upload.albumID);
         query.add("EventCode",roll.eventCode); // don't call this if event code is null!
      }

      protected boolean receive(InputStream inputStream) throws Exception {
         Node node = receiveNew(inputStream,COMMAND_SET_EVENT_CODE);

         // don't read anything except the SOAP headers.
         // this isn't ideal, really we should read and
         // check something.

         return true;
      }
   }

   private class SendUploadEmail extends FormDataTransaction {

      public String describe() { return Text.get(UploadThread.class,"s28"); }
      protected String getFixedURL() { return combine(config.webServicesURL,COMMAND_SEND_UPLOAD_EMAIL); }
      protected void getFormData(Query query) throws IOException {

         query.add("SessionID",roll.upload.PID);
         query.add("PartnerSourceID",PARTNER_SOURCE_LIFEPICS);
         query.add("MerchantID",roll.upload.merchantID);
         query.add("UserID",roll.upload.customerID);

         query.add("AlbumName", (roll.album != null) ? Roll.limitAlbum(roll.album) : "");
         // here we decided to use empty string as a signal
         // instead of having variant API calls with different arguments
      }

      protected boolean receive(InputStream inputStream) throws Exception {
         Node node = receiveNew(inputStream,COMMAND_SEND_UPLOAD_EMAIL);

         // a failure should always be accompanied by errors, but check just in case
         String result = XML.getElementText(node,"Status");
         if ( ! isSuccess(result) ) throw new IOException(Text.get(UploadThread.class,"e44"));

         return true;
      }
   }

   private class UploadBase64 extends SOAPUploadTransaction implements ImageUpload {

      private String imageID;
      public String getImageID() { return imageID; }

      // send file may be original or transformed
      private File sendFile;
      private boolean lockdown;
      private boolean watermark;
      private boolean exclusive;
      private String sendName;

      public UploadBase64(File sendFile, boolean lockdown, boolean watermark, boolean exclusive) {
         this.sendFile = sendFile;
         this.lockdown = lockdown;
         this.watermark = watermark;
         this.exclusive = exclusive;
         sendName = Roll.limitFilename(item.getOriginalFilename(),sendFile.getName());
      }

      public String describe() { return Text.get(UploadThread.class,"s11",new Object[] { describeItem(item) }); }
      protected String getFixedURL() { return insecure(config.webServicesURL); }
      protected String getNamespace() { return "https://api.lifepics.com/v3/"; }
      protected String getAction() { return (roll.album != null) ? COMMAND_UPLOAD_FILE_ALBUM : COMMAND_UPLOAD_FILE; }
      protected String getActionShort() { return COMMAND_UPLOAD_FILE_SHORT; }
      protected String getFileTagName() { return "FileBinaryArray"; }
      protected File getFile() { return sendFile; }
      protected FileUtil.Callback getCallback() { return tracker; }

      protected void sendImpl(Node node) throws Exception {

         XML.createElementText(node,"SessionID",roll.upload.PID);
         XML.createElementText(node,"PartnerSourceID",PARTNER_SOURCE_LIFEPICS);
         XML.createElementText(node,"UserID",roll.upload.customerID); // guaranteed non-null by initiate
         XML.createElementText(node,getFileTagName(),getFileTagValue()); // empty parameter to hold file
         XML.createElementText(node,"FileStreamSize",Convert.fromLong(FileUtil.getSize(sendFile)));
         XML.createElementText(node,"Filename",sendName);

         // Jeff's spec says to use "True" and "False", but that's wrong, MS XML wants lowercase or numeric
         XML.createElementText(node,"Watermark",zeroOne(watermark));
         XML.createElementText(node,"ExclusiveToDealer",zeroOne(exclusive));

         XML.createElementText(node,"MerchantID",roll.upload.merchantID);

         String tags = getTags(roll,lockdown);
         XML.createElementText(node,"TagsXML",(tags != null) ? tags : ""); // empty str is allowed, but the field is required
         //
         // note from when we had FormDataUploadTransaction instead of SOAPUploadTransaction:
         // the tags get a ton of escaping, with e.g. "<" and ">" becoming "%3C" and "%3E".
         // this happens even though the query string goes in the message body not the URL.
         // but, apparently that's correct .. we still need "=" and "&" to be recognizable,
         // so it makes some sense.
         // with SOAP the "<" and ">" become "&lt;" and "&gt;" since they're inside other XML.

         XML.createElementText(node,"Rotate","0");

         if (roll.album != null) {
            XML.createElementText(node,"AlbumID",(item.subalbum != null) ? findSubalbum(item.subalbum) : roll.upload.albumID);
         }
         // albumID is guaranteed non-null by initiate *if* album is not null
      }

      protected void recvImpl(Node node) throws Exception {
         receiveNewImpl(node); // always true since no codeFail

         imageID = XML.getElementText(node,"ImageID");
         // FYI, the image ID is encrypted, not just an integer as in old method
      }
   }

// --- upload session transactions ---

   // the claims process requires a record in Upload_Session,
   // but OPEN doesn't create one, hence these transactions.

   // I tried these as PostTransaction, but SOAP didn't like that.
   // you also need to have HttpPost enabled in the server config.

   private class BeginUploadSession extends FormDataTransaction {

      public String describe() { return Text.get(UploadThread.class,"s15"); }
      protected String getFixedURL() { return combine(config.uploadSessionURL,"BeginUploadSession"); }
      protected void getFormData(Query query) throws IOException {

         roll.assertWholesale(merchantConfig.isWholesale);
         if (merchantConfig.isWholesale) {
            query.add("locationID",roll.dealer.id); // dealer ID is stored as a string here
            query.addPasswordCleartext("password",getDealerPassword());
         } else {
            query.add("locationID",Convert.fromInt(merchantConfig.merchant));
            query.addPasswordCleartext("password",merchantConfig.password);
         }

         query.add("shopperID",roll.upload.customerID);
         query.add("album",Roll.limitAlbum(roll.album)); // don't use this if album is null!

         query.add("files",Convert.fromInt(roll.items.size()));
         query.add("total",Convert.fromLong(tracker.getTotalSizeGoal())); // untransformed
      }

      protected boolean receive(InputStream inputStream) throws Exception {
         Node node = receiveNew(inputStream,"UploadSessionResult");

         String sessionID = XML.getElementText(node,"SessionID");
         roll.upload.extraSessionID = sessionID;

         return true;
      }
   }

   private class EndUploadSession extends FormDataTransaction {

      public String describe() { return Text.get(UploadThread.class,"s16"); }
      protected String getFixedURL() { return combine(config.uploadSessionURL,"EndUploadSession"); }
      protected void getFormData(Query query) throws IOException {

         roll.assertWholesale(merchantConfig.isWholesale);
         if (merchantConfig.isWholesale) {
            query.add("locationID",roll.dealer.id); // dealer ID is stored as a string here
            query.addPasswordCleartext("password",getDealerPassword());
         } else {
            query.add("locationID",Convert.fromInt(merchantConfig.merchant));
            query.addPasswordCleartext("password",merchantConfig.password);
         }

         query.add("sessionID",roll.upload.extraSessionID);
      }

      protected boolean receive(InputStream inputStream) throws Exception {
         Node node = receiveNew(inputStream,"UploadSessionResult");

         String sessionID = XML.getElementText(node,"SessionID");
         if ( ! sessionID.equals(roll.upload.extraSessionID) ) throw new IOException(Text.get(UploadThread.class,"e39"));
         roll.upload.extraSessionID = ""; // clear, but not back to null

         return true;
      }
   }

// --- local image upload ---

   private class UploadLocalImage extends RawUploadTransaction implements ImageUpload {

      public String getImageID() { return item.imageID; } // do not change the ID

      private File sendFile;
      public UploadLocalImage(File sendFile) { this.sendFile = sendFile; }

      public String describe() { return Text.get(UploadThread.class,"s25",new Object[] { describeItem(item) }); }
      protected String getFixedURL() { return config.localImageURL; }
      protected void getParameters(Query query) throws IOException {

         roll.assertWholesale(merchantConfig.isWholesale);
         if (merchantConfig.isWholesale) {
            throw new IOException(Text.get(UploadThread.class,"e40")); // not supported
         } else {
            query.add("locationID",Convert.fromInt(merchantConfig.merchant));
            query.addPasswordObfuscate("encpassword",merchantConfig.password);
         }

         query.add("imageID",item.imageID);
      }

      protected File getFile() { return sendFile; }
      protected FileUtil.Callback getCallback() { return tracker; }

      protected boolean receive(InputStream inputStream) throws Exception {
         Node node = receiveNew(inputStream,"ErrorInfoResult");
         return true;
      }
   }

   // not an upload transaction, but has the same error handling code
   public static class TryPurgeLocalImage extends FormDataTransaction {

      private MerchantConfig merchantConfig;
      private String tryPurgeLocalImageURL;
      private Roll roll; // only used for the assertWholesale call
      private Roll.Item item; // really only need the imageID, but let's be consistent with other transactions

      public TryPurgeLocalImage(MerchantConfig merchantConfig, String tryPurgeLocalImageURL, Roll roll, Roll.Item item) {
         this.merchantConfig = merchantConfig;
         this.tryPurgeLocalImageURL = tryPurgeLocalImageURL;
         this.roll = roll;
         this.item = item;
      }

      public String describe() { return Text.get(UploadThread.class,"s26",new Object[] { describeItem(item) }); }
      protected String getFixedURL() { return tryPurgeLocalImageURL; }
      protected void getFormData(Query query) throws IOException {

         roll.assertWholesale(merchantConfig.isWholesale);
         if (merchantConfig.isWholesale) {
            throw new IOException(Text.get(UploadThread.class,"e41")); // not supported
         } else {
            query.add("locationID",Convert.fromInt(merchantConfig.merchant));
            query.addPasswordCleartext("password",merchantConfig.password);
         }

         query.add("imageID",item.imageID);
      }

      protected boolean receive(InputStream inputStream) throws Exception {
         final long pauseRetryLimit = 604800000; // 1 week (arbitrary, not configurable yet)
         Node node = receiveNew(inputStream,"ErrorInfoResult",/* codeFail = */ 3,/* codeHide = */ 4,
            /* eHide = */ new NormalOperationException(new PauseRetryException(Text.get(UploadThread.class,"e42"),pauseRetryLimit)));
         // note, entity PauseRetryException not network RetryableException
         // note, code 3 was "error but purge anyway", for misconfigured kiosks and so forth.
         // we don't use that result code any more, instead the server just returns no error.
         return true;
      }
   }

   public static class GetPriorityList extends GetTransaction {

      public LinkedList priorityList;
      public LinkedList uploadList;
      public LinkedList deleteList;

      private String localPriorityURL;
      private MerchantConfig merchantConfig;

      private GetPriorityList(String localPriorityURL, MerchantConfig merchantConfig) {
         this.localPriorityURL = localPriorityURL;
         this.merchantConfig = merchantConfig;
      }

      public String describe() { return Text.get(UploadThread.class,"s27"); }
      protected String getFixedURL() { return localPriorityURL; }
      protected void getParameters(Query query) throws IOException {

         if (merchantConfig.isWholesale) {
            throw new IOException(Text.get(UploadThread.class,"e43")); // not supported
         } else {
            query.add("locationID",Convert.fromInt(merchantConfig.merchant));
            query.addPasswordCleartext("password",merchantConfig.password);
         }
      }

      protected boolean receive(InputStream inputStream) throws Exception {
         Node node = receiveNew(inputStream,"PriorityListResult");

         priorityList = XML.getStringList(node,"ImageIDList","int");
         uploadList = XML.getStringList(node,"ToUpload","int");
         deleteList = XML.getStringList(node,"ToDelete","int");
         // server returns List<int> but we want to store them as strings

         return true;
      }
   }

}

