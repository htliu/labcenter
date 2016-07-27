/*
 * JobManager.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Log;
import com.lifepics.neuron.core.Pop;
import com.lifepics.neuron.core.Reportable;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.gui.User;
import com.lifepics.neuron.meta.Selector;
import com.lifepics.neuron.misc.FileUtil;
import com.lifepics.neuron.misc.Purge;
import com.lifepics.neuron.struct.SKU;
import com.lifepics.neuron.table.Table;
import com.lifepics.neuron.table.View;
import com.lifepics.neuron.thread.PauseRetryException;
import com.lifepics.neuron.thread.ToldException;

import java.io.File;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.logging.Level;

import java.awt.Window;

/**
 * The class that knows how to do things with jobs.
 */

public class JobManager {

// --- fields ---

   private Table orderTable;
   private Table jobTable;
   private QueueList queueList;

// --- construction ---

   public JobManager(Table orderTable, Table jobTable, QueueList queueList) {
      this.orderTable = orderTable;
      this.jobTable = jobTable;
      this.queueList = queueList;
   }

   public void reinit(QueueList queueList) {
      this.queueList = queueList;
   }

// --- helpers ---

   public Table getOrderTable() { return orderTable; }
   public Table getJobTable() { return jobTable; }
   public QueueList getQueueList() { return queueList; }

   public static String getKey(Object o) {
      return Convert.fromInt( ((Job) o).jobID );
   }

   public static String[] getKeys(Object[] o) {

      String[] keys = new String[o.length];
      for (int i=0; i<o.length; i++) {
         keys[i] = getKey(o[i]);
      }

      return keys;
   }

// --- mapping validation and related ---

   private static HashSet hashSKUs(Order order) {
      HashSet skus = new HashSet();

      Iterator i = order.items.iterator();
      while (i.hasNext()) {
         skus.add( ((Order.Item) i.next()).sku );
      }

      return skus;
   }

   private static HashSet hashQueues(HashSet skus, QueueList queueList) {
      HashSet queues = new HashSet();

      Iterator i = skus.iterator();
      while (i.hasNext()) {
         String queueID = queueList.findQueueIDBySKU((SKU) i.next());
         if (queueID != null) queues.add(queueID);
      }

      return queues;
   }

   public int countQueues(Order order) {
      return hashQueues(hashSKUs(order),queueList).size();

      // a nice side effect of making the function static
      // and passing in the queue list as an argument
      // is that the queue list can't change in mid-count.
   }

   public void validateSKUMapping(Order order) throws Exception {
      HashSet skus = hashSKUs(order);

      if (order.specialSKU != null) {
         skus.remove(order.specialSKU); // should always be present
         validateSKUMapping(order.specialSKU,/* isSpecial = */ true);
      }

      Iterator i = skus.iterator();
      while (i.hasNext()) {
         validateSKUMapping((SKU) i.next(),/* isSpecial = */ false);
      }
   }

   private void validateSKUMapping(SKU sku, boolean isSpecial) throws Exception {

      final long pauseRetryLimit = 86400000; // 1 day (arbitrary, not configurable yet)

      Queue queue = queueList.findQueueBySKU(sku);
      if (queue == null) {
         User.tell(User.CODE_MISSING_CHANNEL,Text.get(this,"e14a",new Object[] { sku.toString() }),
                                         sku,Text.get(this,"e14b"));
         throw new PauseRetryException(Text.get(this,"e13",new Object[] { sku.toString() }),
            new ToldException(new LocalException(LocalStatus.CODE_ERR_ORD_CHANNEL)),pauseRetryLimit);
      }

      if ( ! (queue.formatConfig instanceof MappingConfig) ) return; // format doesn't use mappings
      MappingConfig mc = (MappingConfig) queue.formatConfig;

      if (isSpecial && ! mc.mapsSpecialSKU()) return; // special SKU is not mapped, do not validate

      LinkedList mappings = mc.getMappings();
      if ( ! MappingUtil.existsMapping(mappings,sku) ) {
         Format format = Format.getFormat(queue.format);
         User.tell(User.CODE_MISSING_CHANNEL,Text.get(this,"e16a",new Object[] { sku.toString(), format.getShortName() }),
                                         sku,Text.get(this,"e16b"));
         throw new PauseRetryException(Text.get(this,"e15",new Object[] { sku.toString(), format.getShortName() }),
            new ToldException(new LocalException(LocalStatus.CODE_ERR_ORD_CHANNEL)),pauseRetryLimit);
      }
   }

// --- create ---

   /**
    * Create zero or more jobs, based on various restrictions.
    *
    * @param queueID       The queue to print, or null to print all queues.
    * @param skus  A collection of SKU values, or null to print all SKUs.
    * @param items A collection of Order.Item, or null to print all items.
    */
   public void create(String fullID, String queueID, Collection skus, Collection items, Integer overrideQuantity, boolean adjustIfPartial) throws Exception {
      String key = fullID;

      Object lock = orderTable.lock(key);
      try {

         OrderStub stub = (OrderStub) orderTable.get(key);
         //
         // incidentally, this gets the latest version of the object.
         // the object identity of the items doesn't matter, we only
         // look at the filename and SKU

         if ( ! (stub instanceof Order) ) throw new Exception(Text.get(this,"e1",new Object[] { key }));
         //
         // rely on system structure to not call this when status is wrong.
         // the job process moves the status in the range INVOICED to PRINTED,
         // (unless the status is COMPLETED or ABORTED), so if the status
         // were some earlier value, this function could cause weird behavior.

         Order order = (Order) stub;

         if (    adjustIfPartial // option used by OrderManager.doPrint
              && items == null
              && computeAltStatus(order) == Order.STATUS_ORDER_PRINTING ) {

            items = SpawnThread.getUnsentItems(order);
         }

         createWithLock(order,lock,queueID,skus,items,overrideQuantity,/* isAuto = */ false);

      } finally {
         orderTable.release(key,lock);
      }
   }

   private static class JobInfo {
      public Queue queue;
      public HashSet skusForQueue;
   }

   private static JobInfo findOrCreateJobInfo(LinkedList jobInfoList, Queue queue) {

   // try and find

      Iterator i = jobInfoList.iterator();
      while (i.hasNext()) {
         JobInfo jobInfo = (JobInfo) i.next();
         if (jobInfo.queue == queue) return jobInfo; // note, compare by identity!
      }

   // not found, create

      JobInfo jobInfo = new JobInfo();
      jobInfo.queue = queue;
      jobInfo.skusForQueue = new HashSet();

      jobInfoList.add(jobInfo);
      return jobInfo;
   }

   /**
    * Now lock is not null, and the items and SKUs will be fixed right away.
    */
   public void createWithLock(Order order, Object lock, String queueID, Collection skus, Collection items, Integer overrideQuantity, boolean isAuto) throws Exception {

      if (items == null) items = order.items; // all items

   // work out the actual set of SKUs we want to use
   // this is the set defined by the items, intersected with the caller requirements

      HashSet skusActual = new HashSet();

      Iterator i = items.iterator();
      while (i.hasNext()) {
         SKU sku = ((Order.Item) i.next()).sku;
         if (skus != null && ! skus.contains(sku)) continue;
         skusActual.add(sku); // ignore result
      }

      if (skusActual.isEmpty()) throw new Exception(Text.get(this,"e20"));
      // this can only happen if you call with an empty item set,
      // an empty SKU set, or some combination of both that doesn't intersect.
      // so, it shouldn't.

   // build list of queue information

      LinkedList jobInfoList = new LinkedList();

      i = skusActual.iterator();
      while (i.hasNext()) {
         SKU sku = (SKU) i.next();

         Queue queue = queueList.findQueueBySKU(sku);
         if (queue == null) throw new Exception(Text.get(this,"e22",new Object[] { sku.toString() }));
         //
         // this exception can basically only occur through the UI.  in the spawn thread,
         // we call validateSKUMapping before spawning any jobs, so we know the queues
         // are all mapped.  maybe there's a tiny window of opportunity for a config change
         // hitting at just the wrong moment, but we can live with that.
         // so, that's why this is a plain exception, not a User.tell & PauseRetryException.

         JobInfo jobInfo = findOrCreateJobInfo(jobInfoList,queue);
         jobInfo.skusForQueue.add(sku);
      }

   // use subfunction to cover iteration over all queues

      if (queueID != null) {

         Queue queue = queueList.findQueueByID(queueID);
         if (queue == null) throw new Exception(Text.get(this,"e2",new Object[] { queueID }));

         JobInfo jobInfo = findOrCreateJobInfo(jobInfoList,queue);
         if (jobInfo.skusForQueue.isEmpty()) throw new Exception(Text.get(this,"e21"));

         create3(order,lock,jobInfo.queue,jobInfo.skusForQueue,items,overrideQuantity,isAuto);

      } else {

         i = jobInfoList.iterator();
         while (i.hasNext()) {
            JobInfo jobInfo = (JobInfo) i.next();

            create3(order,lock,jobInfo.queue,jobInfo.skusForQueue,items,overrideQuantity,isAuto);
         }
      }
   }

   /**
    * Now the queue is not null; that's everything.
    */
   private void create3(Order order, Object lock, Queue queue, HashSet skusForQueue, Collection items, Integer overrideQuantity, boolean isAuto) throws Exception {

      if (isAuto && queue.noAutoPrint != null && queue.noAutoPrint.booleanValue()) return;

   // use subfunction to cover special behavior

      if (queue.format == Order.FORMAT_NORITSU) { // Noritsu, create one job per SKU or multi-image item

         LinkedList itemsNormal = new LinkedList();
         LinkedList itemsMulti  = new LinkedList();

         Iterator i = items.iterator();
         while (i.hasNext()) {
            Order.Item item = (Order.Item) i.next();
            if (item.isMultiImage()) {
               itemsMulti .add(item);
            } else {
               itemsNormal.add(item);
            }
         }

         if (itemsNormal.size() > 0) createJobPerSKU (order,lock,queue,skusForQueue,itemsNormal,overrideQuantity);
         if (itemsMulti .size() > 0) createJobPerItem(order,lock,queue,skusForQueue,itemsMulti, overrideQuantity);

      } else if (queue.format == Order.FORMAT_FUJI_NEW) { // Fuji PIC 2.6, split out CD

         splitSpecialSKU(order,lock,queue,skusForQueue,items,overrideQuantity);

         // some integrations (e.g. Fuji3, Pixel) look at specialSKU but still handle
         // the CD image set correctly.  others (e.g. FujiNew) look at it and then
         // burn all images in the job to CD because the integration isn't capable of
         // more specificity.  that was OK when the CD images were the same as the
         // print images, but now that we're planning to change Opie to send down the
         // original source images, we need to split the CD out into a separate job.

      } else if (    queue.format == Order.FORMAT_DKS3
                  || queue.format == Order.FORMAT_BURN ) { // create one job per SKU

         createJobPerSKU(order,lock,queue,skusForQueue,items,overrideQuantity);

      } else if (    queue.format == Order.FORMAT_DIRECT_PDF
                  || queue.format == Order.FORMAT_XEROX
                  || queue.format == Order.FORMAT_DIRECT_JPEG
                  || queue.format == Order.FORMAT_RAW_JPEG ) { // create one job per item

         createJobPerItem(order,lock,queue,skusForQueue,items,overrideQuantity);

         // why one job per item?

         // JPEG:
         // to keep the spooled print jobs small, I want to use PrinterJob.setCopies
         // instead of sending the same image several times, but that only works
         // if there's one job per item, since the quantity can vary at the item level.
         // also I'm worried that the spooler might require memory proportional to the
         // number of items.

         // Xerox:
         // this is not required by the spec, it's just convenient for me.
         //
         // the alternative would be to have one job for multiple PDFs,
         // name the image folder LP0024 (say), then create CSV files
         // LP002400, LP002401, etc.  but, what would that work get us?
         //
         // * doesn't save disk space, because PDFs are product-specific
         // * saves creating some folders, maybe, but there will usually
         //   only be one or two PDFs per order
         // * saves creating job records in the database, but same deal
         // * complicates the code
         // * complicates the UI to explain what's going on
         //
         // why is that the main alternative?
         //
         // * the folder name has to correspond to the order numbers
         //   in some way because that's how we guarantee uniqueness
         // * the spec allows multi-line CSV files, but Xerox discourages it,
         //   and anyway the lines would still need unique order numbers

         // PDF:
         // the reason I want to do this is, we can only stream one PDF to the printer
         // at a time, but the job object in LC doesn't have any way to keep track of
         // partial success.  there may be other integrations that have similar issues,
         // but here it's really bad because it's so easy to fail.

      } else if (queue.format == Order.FORMAT_FUJI3) { // Fuji JobMaker, split by surface type

         Collection skuSets = FormatFuji3.split(skusForQueue,items,(Fuji3Config) queue.formatConfig,order.specialSKU);

         Iterator i = skuSets.iterator();
         while (i.hasNext()) {
            Collection skuSet = (Collection) i.next();
            create4(order,lock,queue,skuSet,items,overrideQuantity);
         }

      } else { // normal integration

         create4(order,lock,queue,skusForQueue,items,overrideQuantity);
      }
   }

   private void createJobPerSKU(Order order, Object lock, Queue queue, Collection skusForQueue, Collection items, Integer overrideQuantity) throws Exception {

      Iterator i = skusForQueue.iterator();
      while (i.hasNext()) {
         LinkedList skusSingleton = new LinkedList();
         skusSingleton.add(i.next());
         create4(order,lock,queue,skusSingleton,items,overrideQuantity);
      }
   }

   private void createJobPerItem(Order order, Object lock, Queue queue, Collection skusForQueue, Collection items, Integer overrideQuantity) throws Exception {

      Iterator i = items.iterator();
      while (i.hasNext()) {
         Order.Item item = (Order.Item) i.next();

         if ( ! skusForQueue.contains(item.sku) ) continue; // minor optimization

         LinkedList itemSingleton = new LinkedList();
         itemSingleton.add(item);
         create4(order,lock,queue,skusForQueue,itemSingleton,overrideQuantity);
      }
   }

   private void splitSpecialSKU(Order order, Object lock, Queue queue, HashSet skusForQueue, Collection items, Integer overrideQuantity) throws Exception {
      LinkedList skusSingleton = null;

      if (skusForQueue.contains(order.specialSKU)) { // implies order.specialSKU != null
         skusForQueue = (HashSet) skusForQueue.clone(); // modifying original seems bad, caller still has access
         skusForQueue.remove(order.specialSKU);
         skusSingleton = new LinkedList();
         skusSingleton.add(order.specialSKU);
      }

      if (skusForQueue.size() > 0) create4(order,lock,queue,skusForQueue, items,overrideQuantity);
      if (skusSingleton != null  ) create4(order,lock,queue,skusSingleton,items,overrideQuantity);
   }

   // only used one place now, got inlined and modified in create4
   //
   private static LinkedList refsForSkus(Collection skus, Collection items) {

      LinkedList refs = new LinkedList();

      Iterator i = items.iterator();
      while (i.hasNext()) {
         Order.Item item = (Order.Item) i.next();
         if ( ! skus.contains(item.sku) ) continue;
         refs.add(new Job.Ref(item.filename,item.sku));
      }

      return refs;
   }

   private static boolean isMultiImageAware(int format) {
      return (    format == Order.FORMAT_FLAT           //  2      e
               || format == Order.FORMAT_TREE           //  2      e
               || format == Order.FORMAT_NORITSU        // 12  ab
               || format == Order.FORMAT_KODAK          //  23 ab
               || format == Order.FORMAT_LUCIDIOM       // 1
               || format == Order.FORMAT_PIXEL          // 12    c
               || format == Order.FORMAT_HP             // 1
               || format == Order.FORMAT_DIRECT_JPEG    //  2    c
               || format == Order.FORMAT_BURN           //  2      e
               || format == Order.FORMAT_DNP            //  2    c
               || format == Order.FORMAT_PURUS          // 1
               || format == Order.FORMAT_RAW_JPEG );    //  2    c
   }
   // this seems like the best spot to discuss multi-image integrations.
   // there are three ways an integration can handle a multi-image item.
   //
   // (1) send it over as a true multi-image item
   // (2) turn it into a set of prints (a single-sided creative product)
   // (3) error out
   //
   // then, the trick is, for some integrations this behavior can be configured.
   // so, here are the actual types.
   //
   // (1)  automatically send as true multi-image
   // (12) you can configure to make single-sided ("Print single-sided creative products")
   // (2)  automatically send as single-sided
   // (23) you have to enable single-sided action ("Allow single-sided creative products")
   // (3)  not multi-image aware
   //
   // type (23) may seem strange.  the idea was, single-sided isn't the best way
   // to print things, so you have to consciously enable it.
   // types (13) and (123) are possible in theory, there just aren't any of them.
   //
   // then, the extra complication is, when you're doing (2) you can collate the
   // pages in different ways.  suppose you want two copies of a five-page book.
   //
   // (a) 1234512345
   // (b) 5432154321 ("collate in reverse order")
   // (c) 1122334455
   // (d) 5544332211
   // (e) order not specified
   //
   // and, of course integrations can support more than one and be configurable.
   // class Format.Deref was meant to help with collation, but it hasn't turned
   // out to be as useful as I thought.

   // also, if you're converting an integration to be multi-image aware, here's
   // a super useful transformation for making single loops over filenames into
   // double loops.  first use was in FormatFlat and FormatTree in version 5.4.0.
   //
   //    String filename = item.filename;
   //    etc
   //
   // becomes
   //
   // List filenames = item.isMultiImage() ? item.filenames : Collections.singletonList(item.filename);
   // Iterator j = filenames.iterator();
   // while (j.hasNext()) {
   //    String filename = (String) j.next();
   //    etc

   private void create4(Order order, Object lock, Queue queue, Collection skusForJob, Collection items, Integer overrideQuantity) throws Exception {

   // get parameters for chunk limiting

      // the deal with chunk limiting is, Konica and Noritsu have a PID field that counts chunks
      // (although they don't think of it that way), and apparently it's limited to three digits.
      //
      // actually, I don't even know that the PID and the quantity are limited, they just showed
      // up with three digits in the sample files and we went with that.  but, it's easier to
      // write the code and split the jobs than go out and test against all the different Konica
      // and Noritsu printers that are out there.  strange but true ...
      //
      // BTW, the DLS and Kodak quantities really *are* limited, I got those from documentation.
      // so, it's plausible that Konica and Noritsu have real limits too.

      // it's unfortunate that the logic here about chunk limits is so far removed from the logic
      // in the actual format classes.  on the plus side, the format classes still have all their
      // original validations in place, so if we produce an invalid job, it will fail.
      // so, all we have to worry about is whether the split jobs have the correct total contents.

      // one more note: assuming the jobs print in the correct order, and the prints are stacked
      // so that the last print of job N is next to the first print of job (N+1), then the prints
      // in split jobs will have the same stacking as if they'd been in a single job.  if there's
      // a backprint sequence number, it'll be messed up, but that'd take a lot more work to fix,
      // it's not a common or important case.

      boolean limitChunks = false;
      int chunkCountStd = 0;
      int chunkCountMax = 0;
      int chunkStd = 0;
      int chunkMax = 0;
      boolean hasSequence = false;

      if (queue.format == Order.FORMAT_NORITSU) {

         limitChunks = true;
         chunkCountStd = NoritsuConfig.CHUNK_COUNT_STD;
         chunkCountMax = NoritsuConfig.CHUNK_COUNT_MAX;
         chunkStd = NoritsuConfig.CHUNK_STD;
         chunkMax = NoritsuConfig.CHUNK_MAX;
         hasSequence = Backprint.hasSequence( ((NoritsuConfig) queue.formatConfig).backprints );

      } else if (queue.format == Order.FORMAT_KONICA) {

         limitChunks = true;
         chunkCountStd = KonicaConfig.CHUNK_COUNT_STD;
         chunkCountMax = KonicaConfig.CHUNK_COUNT_MAX;
         chunkStd = KonicaConfig.CHUNK_STD;
         chunkMax = KonicaConfig.CHUNK_MAX;
         hasSequence = Backprint.hasSequence( ((KonicaConfig) queue.formatConfig).backprints );
      }
      // else no limiting

   // compute the item subset, as refs

      // the way to think about this is, refs is a container we're packing up.
      // if limitChunks is false, it has infinite capacity, very simple.
      // otherwise, when the next item won't fit, ship the existing container
      // and start a new one.  (we could be smarter about making the existing
      // container completely full, either by splitting or by shuffling items,
      // but it's not worth it.)

      int chunkTotal = 0;
      LinkedList refs = new LinkedList();

      Iterator i = items.iterator();
      while (i.hasNext()) {
         Order.Item item = (Order.Item) i.next();
         if ( ! skusForJob.contains(item.sku) ) continue;

         // until now, the item list has just been a big list of all items,
         // but now we finally know that the item is going to *this* queue.
         // so, now we can block multi-image items out of non-aware queues.
         //
         boolean itemIsMultiImage = item.isMultiImage();
         if (itemIsMultiImage && ! isMultiImageAware(queue.format)) {
            Format format = Format.getFormat(queue.format);
            throw new Exception(Text.get(this,"e19",new Object[] { format.getCapitalizedShortName() }));
         }

         Job.Ref ref = new Job.Ref(item.filename,item.sku);

         if (limitChunks && ! itemIsMultiImage) { // don't chunk multi-image items

            int quantity = (overrideQuantity != null) ? overrideQuantity.intValue() : item.quantity;
            // unfortunate partial duplication of Job.getOutputQuantity.  we can't use that because
            // there's no job yet; we don't look at individualQuantity because this is where we set it.

            int chunkCount = Format.getChunkCount(quantity,chunkStd,chunkMax,hasSequence);

            if (chunkTotal + chunkCount > chunkCountMax) { // will it fit? if not ...

               // ship the existing container (unless it's empty, which can happen in the special case below)
               if ( ! refs.isEmpty() ) {
                  createJob(order,lock,queue,refs,overrideQuantity);
                  chunkTotal = 0;
                  refs = new LinkedList();
               }

               // special case, if the single item is too big for a container, chop it up
               if (chunkCount > chunkCountMax) {

                  // if we get in here without a sequence, then the quantity is greater than 500 * 998 + 999 = 500000-1.
                  // we *could* still handle that, but it's just not worth the complexity.
                  if ( ! hasSequence ) throw new Exception(Text.get(this,"e18",new Object[] { Convert.fromInt(quantity) }));

                  // now that we know it's a sequence, the chunk count is just the quantity, easy
                  while (chunkCount > chunkCountMax) {

                     Job.Ref ref2 = new Job.Ref(item.filename,item.sku);
                     ref2.individualQuantity = new Integer(chunkCountStd);

                     LinkedList refs2 = new LinkedList();
                     refs2.add(ref2);

                     createJob(order,lock,queue,refs2,overrideQuantity);
                     // include overrideQuantity for consistency with other jobs, even though it'll be ignored

                     chunkCount -= chunkCountStd;
                  }

                  ref.individualQuantity = new Integer(chunkCount);
                  // so now again chunkCount is the count for ref
               }
            }

            chunkTotal += chunkCount; // goes with refs.add below, in concept
         }

         refs.add(ref);
      }

      // ship the last container (should never be empty, but test just in case)
      if ( ! refs.isEmpty() ) {
         createJob(order,lock,queue,refs,overrideQuantity);
         // no need to clear the other fields, we're done
      }
   }

   private void createJob(Order order, Object lock, Queue queue, LinkedList refs, Integer overrideQuantity) throws Exception {

   // create a new job object

      Job job = new Job();

      // jobID is assigned by table
      job.queueID = queue.queueID;
      job.orderSeq = order.orderSeq;
      job.orderID = order.orderID;
      job.refs = refs;

      job.overrideQuantity = overrideQuantity;

      job.status = Job.STATUS_JOB_PENDING;
      job.hold = Order.HOLD_NONE;
      // lastError starts out null

      job.recmodDate = new Date();

   // update the tables

      Object lock2 = jobTable.insert(job);
      try {

         updateOrder(order,lock,refs,Order.STATUS_ITEM_PRINTING);
            // this may move them forward, or backward

         Log.log(Level.INFO,this,"i1",new Object[] { Convert.fromInt(job.jobID), order.getFullID(), queue.queueID, queue.name });

         Object temp = lock2;
         lock2 = null;
         jobTable.release(job,temp);

      } finally {
         if (lock2 != null) jobTable.delete(job,lock2);
      }

      // deleting isn't foolproof, but it greatly increases the atomicity.
      // now, to get job creation without order update, not only does the
      // order update have to fail, on a locked order even, but then also
      // the delete has to fail too, after the insert succeeded.
      //
      // note, release failure is not a reason to delete the job.
   }

// --- mark function ---

   public void mark(String fullID, LinkedList skus, int itemStatus) throws Exception {
      String key = fullID;

      Order order;

      Object lock = orderTable.lock(key);
      try {

         OrderStub stub = (OrderStub) orderTable.get(key);

         if ( ! (stub instanceof Order) ) throw new Exception(Text.get(this,"e7",new Object[] { key }));

         order = (Order) stub;
         LinkedList refs = refsForSkus(skus,order.items);

         // it's kind of inefficient to walk through the items,
         // build a list of refs, then use the refs to go back
         // and look up the items, but that's the easiest way.

         updateOrder(order,lock,refs,itemStatus);

      } finally {
         orderTable.release(key,lock);
      }

      makeComplete(order,null,false);
   }

// --- order status ---

   public void updateOrder(String fullID, LinkedList refs, int itemStatus) throws Exception {
      String key = fullID;

      Object lock = orderTable.lock(key);
      try {

         OrderStub stub = (OrderStub) orderTable.get(key);

         if ( ! (stub instanceof Order) ) throw new Exception(Text.get(this,"e3",new Object[] { key }));

         updateOrder((Order) stub,lock,refs,itemStatus);

      } finally {
         orderTable.release(key,lock);
      }
   }

   /**
    * Set the status of items on an order, then update the order status accordingly.
    * The lock is used but not released.
    */
   private void updateOrder(Order order, Object lock, LinkedList refs, int itemStatus) throws Exception {

      Iterator i = refs.iterator();
      while (i.hasNext()) {
         order.getItem((Job.Ref) i.next()).status = itemStatus;
      }

      // if the status is not within the range of motion, don't move it.
      // it will get moved to the right place when SpawnThread finishes.
      //
      if (    order.status >= Order.STATUS_ORDER_INVOICED
           && order.status <= Order.STATUS_ORDER_PRINTED  ) {

         order.status = computeOrderStatus(order);
      }

      order.recmodDate = new Date();
      orderTable.update(order,lock);
   }

   /**
    * Compute the order status based on the status of items.<p>
    *
    * Here's how the status is computed.
    * If all items are RECEIVED,   the order is INVOICED;
    * If all items are PRINTED,    the order is PRINTED;
    * and for anything in between, the order is PRINTING.
    */
   public static int computeOrderStatus(Order order) {
      return computeOrderStatus(order,Order.STATUS_ITEM_PRINTED);
   }
   private static int computeOrderStatus(Order order, int statusPrinted) {

      boolean allReceived = true;
      boolean allPrinted  = true;

      Iterator i = order.items.iterator();
      while (i.hasNext()) {
         Order.Item item = (Order.Item) i.next();

         if (item.status > Order.STATUS_ITEM_RECEIVED) allReceived = false;
         if (item.status < statusPrinted             ) allPrinted  = false;
         if ( ! allReceived && ! allPrinted ) break;
      }

      if      (allReceived) return Order.STATUS_ORDER_INVOICED;
      else if (allPrinted ) return Order.STATUS_ORDER_PRINTED;
      else                  return Order.STATUS_ORDER_PRINTING;
   }

   /**
    * Compute an alternate status based on whether none, some,
    * or all of the items have been sent to the queues for printing.
    * I'm reusing the status codes since I need to return something,
    * but the meanings are different.
    */
   public static int computeAltStatus(Order order) {
      return computeOrderStatus(order,Order.STATUS_ITEM_PRINTING);
   }

// --- completion ---

   private Queue getQueue(Job job) throws Exception {

      // note, access to queueList isn't synchronized,
      // and it gets changed in the UI thread.
      // I think it's OK as long as we just use it once.

      Queue queue = queueList.findQueueByID(job.queueID);
      if (queue == null) {
         throw new Exception(Text.get(this,"e6",new Object[] { job.queueID }));
      }
      // this duplicates JobThread.getQueue ... oh well

      return queue;
   }

   public Format getFormat(Job job) throws Exception {

      int format;
      if (job.format != null) {
         format = job.format.intValue(); // this should always happen now
      } else {
         format = getQueue(job).format;
      }

      return Format.getFormat(format);
   }

   public Object getQueueConfig(Job job) throws Exception {

      // this is mainly a helper function for CompletionThread.
      // the reason it's here is, it's parallel with getFormat
      // in how it looks at both the job and the queue formats.

      Queue queue = getQueue(job);

      // make sure the queue format now matches the queue format
      // when the job was created, so that the caller can safely
      // cast the config to the expected type.  it would
      // be bizarre to change a queue format, but it could happen.
      //
      if (job.format != null) { // this should always be true
         if (job.format.intValue() != queue.format) throw new Exception(Text.get(this,"e17"));
      }

      return queue.formatConfig;
   }

   private static class SelectorComplete implements Selector {

      private String fullID;
      public SelectorComplete(String fullID) { this.fullID = fullID; }

      public boolean select(Object o) {
         Job job = (Job) o;
         return (    job.getFullID().equals(fullID)
                  && job.status == Job.STATUS_JOB_SENT
                  && job.hold == Order.HOLD_NONE       );

         // I don't think there's any way for a sent job to go on hold,
         // but check anyway, it would be bad to manipulate a held job.
      }
   }

   public void makeComplete(Order order, Object orderLock) throws Exception {
      makeComplete(order,orderLock,true);
   }

   /**
    * @param orderLock If force is true, the lock object for the order; if force is false, ignored.
    */
   private void makeComplete(Order order, Object orderLock, boolean force) throws Exception {
      Selector selector = new SelectorComplete(order.getFullID());

      View view = jobTable.select(selector,JobUtil.orderJobID,false,false);
      for (int i=0; i<view.size(); i++) {
         String key = Convert.fromInt( ((Job) view.get(i)).jobID );

         try {
            Object jobLock = jobTable.lock(key);
            try {

               Job job = (Job) jobTable.get(key); // get latest copy
               if ( ! selector.select(job) ) continue;

               // that last step makes sure we don't update one that just became unavailable.
               // if one becomes <i>available</i>, we'll miss it ... but that's OK,
               // the user shouldn't be creating jobs and then completing in the same second.

               // as in CompletionThread, it's better to update the order first,
               // since it doesn't do any harm if we have to re-update it later.

               if (force) {

                  // here, the propagation is from jobs to order ... we go through the jobs,
                  // make them all complete, and incidentally update the order and item statuses.

                  updateOrder(order,orderLock,job.refs,Order.STATUS_ITEM_PRINTED);

               } else {

                  // here, the propagation is from order to jobs ... we go through the jobs,
                  // look at the items for the job and see if they're all printed, and, if so,
                  // make the job complete too.  there are some theoretical issues with this,
                  // (e.g., if there are two jobs with the same items) but ignore them for now.

                  boolean printed = true;

                  Iterator j = job.refs.iterator();
                  while (j.hasNext()) {
                     Order.Item item = order.getItem((Job.Ref) j.next());
                     if (item.status != Order.STATUS_ITEM_PRINTED) { printed = false; break; }
                  }

                  if ( ! printed ) continue;
               }

               job.status = Job.STATUS_JOB_COMPLETED;
               job.recmodDate = new Date();

               // can happen if format doesn't have directory (e.g., Fuji)
               if (job.dir != null) {
                  job.dir = getFormat(job).makeComplete(job.dir,job.property); // best effort, doesn't fail
               }

               jobTable.update(job,jobLock);

            } finally {
               jobTable.release(key,jobLock);
            }
         } catch (Exception e) {
            if (force) {
               // in this case the operation aborts
               throw e;
            } else {
               // in this case we want best effort
               Log.log(Level.WARNING,this,"e8",new Object[] { order.getFullID(), key },e);
               // we have an order ID here, so we could use Log.ORDER,
               // but this case is more about the jobs, not the order.
            }
         }
      }
   }

   // call this when an order is deleted or canceled
   // to cause all its jobs to be forgotten.
   // there's a fair amount of common code with makeComplete,
   // but also big differences, like no update of the order.
   //
   // the point has to do with labs that don't do completion.
   // when the order is completed, the jobs get completed,
   // and can be purged; makeNotComplete does the same thing
   // for when orders are deleted or canceled.
   // without it, the jobs would have to be purged manually.
   //
   public void makeNotComplete(String fullID) throws Exception {
      Selector selector = new SelectorComplete(fullID);

      View view = jobTable.select(selector,JobUtil.orderJobID,false,false);
      for (int i=0; i<view.size(); i++) {
         String key = Convert.fromInt( ((Job) view.get(i)).jobID );

         Object jobLock = jobTable.lock(key);
         try {

            Job job = (Job) jobTable.get(key); // get latest copy
            if ( ! selector.select(job) ) continue;

            job.status = Job.STATUS_JOB_FORGOTTEN;
            job.recmodDate = new Date();

            jobTable.update(job,jobLock);

         } finally {
            jobTable.release(key,jobLock);
         }
      }
   }

// --- purge functions ---

   private void purgeMultiple(Window owner, Object[] o) {

   // confirm before purging

      String s = Text.get(this,"e11",new Object[] { new Integer(o.length), Convert.fromInt(o.length) });
      boolean confirmed = Pop.confirm(owner,s,Text.get(this,"s3"));
      if ( ! confirmed ) return;

   // ok, now purge

      boolean warn = false;

      for (int i=0; i<o.length; i++) {
         warn |= (purgeSingle(getKey(o[i]),null,null,Purge.REASON_MANUAL,(Reportable) o[i],/* verify = */ true) == Purge.RESULT_INCOMPLETE);
      }

   // warn if purge not completely successful

      if (warn) {
         Pop.warning(owner,Text.get(this,"e12"),Text.get(this,"s4"));
      }
   }

   /**
    * Pass in both job and lock if the object is locked.
    * Pass in key in every case.
    *
    * @param reason A value from the Purge.REASON_X enumeration.
    * @return       A value from the Purge.RESULT_X enumeration.
    */
   private int purgeSingle(String key, Job job, Object lock, int reason, Reportable reportable, boolean verify) {

      boolean own = (lock == null);

      try {
         if (own) lock = jobTable.lock(key);
         try {

            if (own) job = (Job) jobTable.get(key); // get latest copy

            if (verify && ! job.isPurgeableStatus()) return Purge.RESULT_FAILED;

            jobTable.delete(key,lock);
            if (own) lock = null; // a successful delete releases the lock

            Log.log(Level.INFO,this,"i2" + Purge.getReasonSuffix(reason),new Object[] { key, JobEnum.fromJobStatusInternal(job.status) });

         } finally {
            if (own && lock != null) jobTable.release(key,lock);
         }
      } catch (Exception e) {

         // just log the exception -- if we were invoked from the UI,
         // the user will see the row remain in the table

         Log.log(reportable,Level.SEVERE,this,"e4",new Object[] { key },e);
         return Purge.RESULT_FAILED;
      }

      return purgeFiles(job) ? Purge.RESULT_COMPLETE : Purge.RESULT_INCOMPLETE;
   }

   /**
    * Purge a job by removing the data files and the directory.
    *
    * @return True if everything was successfully removed.
    */
   private boolean purgeFiles(Job job) {

      if (job.dir == null) return true; // could happen if job was canceled before transfer?
                                        // or if format doesn't have directory (e.g., Fuji).

      if (job.dirOwned != null && ! job.dirOwned.booleanValue() ) return true; // not owned

      // unlike orders and rolls, for jobs the files aren't all in one directory.
      // but, we know they were created in order, so we can delete in reverse order.
      //
      // we could also use LinkedList.toArray followed by Collections.reverse

      String[] names = new String[job.files.size()];
      int i = names.length;

      ListIterator li = job.files.listIterator();
      while (li.hasNext()) {
         names[--i] = (String) li.next();
      }

      // do one last test for directory rename.
      // why? mainly it's for forgotten jobs ... we don't watch them
      // to see if the lab has renamed them, but it can still happen,
      // and we want to purge as thoroughly as possible.
      //
      File useDir = job.dir;
      try {
         File completeDir = getFormat(job).isComplete(job.dir,job.property);
         if (completeDir != null) useDir = completeDir;
      } catch (Exception e) {
         // go with what we have
      }

      return FileUtil.purge(useDir,names);
   }

   /**
    * @return A purge result, or null if purge not attempted.
    */
   public Integer autoPurge(Date now, Job job, Object lock) {

      Date purgeDate = JobUtil.getPurgeDate(job);
      if (    purgeDate != null
           && purgeDate.before(now) ) {

         String key = Convert.fromInt(job.jobID);
         int result = purgeSingle(key,job,lock,Purge.REASON_AUTOMATIC,job,/* verify = */ false);
         if (result == Purge.RESULT_INCOMPLETE) {
            Log.log(job,Level.WARNING,this,"e5",new Object[] { key });
         }

         return new Integer(result);
      } else {
         return null;
      }
   }

// --- hold and release ---

   public void hold(String key) {
      try {
         Object lock = jobTable.lock(key);
         try {
            Job job = (Job) jobTable.get(key); // get latest copy

            if (    job.hold != Order.HOLD_USER // allow convert ERROR into USER
                 && job.status < Job.STATUS_JOB_COMPLETED ) {

               job.hold = Order.HOLD_USER;

               job.recmodDate = new Date();
               jobTable.update(job,lock);

               Log.log(Level.INFO,this,"i3",new Object[] { key });
            }
         } finally {
            jobTable.release(key,lock);
         }
      } catch (Exception e) {
         // ignore, user will see result
      }
   }

   public void release(String key) {
      try {
         Object lock = jobTable.lock(key);
         try {
            Job job = (Job) jobTable.get(key); // get latest copy

            if (job.hold != Order.HOLD_NONE) {

               job.hold = Order.HOLD_NONE;
               job.lastError = null;

               job.recmodDate = new Date();
               jobTable.update(job,lock);

               Log.log(Level.INFO,this,"i4",new Object[] { key });
            }
         } finally {
            jobTable.release(key,lock);
         }
      } catch (Exception e) {
         // ignore, user will see result
      }
   }

// --- forget ---

   public void forget(String key) {
      try {
         Object lock = jobTable.lock(key);
         try {
            Job job = (Job) jobTable.get(key); // get latest copy

            if ( ! job.isPurgeableStatus() ) {

               job.status = Job.STATUS_JOB_FORGOTTEN;
               job.hold = Order.HOLD_NONE;
               // keep last error just for information

               job.recmodDate = new Date();
               jobTable.update(job,lock);

               Log.log(Level.INFO,this,"i5",new Object[] { key });
            }
         } finally {
            jobTable.release(key,lock);
         }
      } catch (Exception e) {
         // ignore, user will see result
      }
   }

// --- UI commands ---

   public static void pleaseSelect(Window owner, boolean multi) {
      Pop.info(owner,Text.get(JobManager.class,multi ? "e23a" : "e23b"),Text.get(JobManager.class,"s5"));
   }

   public void doHold(Window owner, Object[] o) {
      if (o.length == 0) { pleaseSelect(owner,true); return; }
      String[] keys = getKeys(o);
      for (int i=0; i<keys.length; i++) {
         hold(keys[i]);
      }
   }

   public void doRelease(Window owner, Object[] o) {
      if (o.length == 0) { pleaseSelect(owner,true); return; }
      String[] keys = getKeys(o);
      for (int i=0; i<keys.length; i++) {
         release(keys[i]);
      }
   }

   public void doForget(Window owner, Object[] o) {
      if (o.length == 0) { pleaseSelect(owner,true); return; }

   // confirm before forgetting (and explain what it means)

      boolean confirmed = Pop.confirm(owner,Text.get(this,"e9"),Text.get(this,"s1"));
      if ( ! confirmed ) return;

   // ok, do it

      String[] keys = getKeys(o);
      for (int i=0; i<keys.length; i++) {
         forget(keys[i]);
      }
   }

   public void doPurge(Window owner, Object[] o) {
      if (o.length == 0) { pleaseSelect(owner,true); return; }

      // prevent ineffective purge commands
      for (int i=0; i<o.length; i++) {
         Job job = (Job) o[i];
         if ( ! job.isPurgeableStatus() ) {
            Pop.error(owner,Text.get(this,"e10"),Text.get(this,"s2"));
            return;
         }
      }

      purgeMultiple(owner,o);
   }

}

