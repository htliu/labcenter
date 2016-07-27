/*
 * FormatFuji3.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Nullable;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.misc.Op;
import com.lifepics.neuron.struct.SKU;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Implementation of the Fuji JobMaker 1.4 format.
 */

public class FormatFuji3 extends Format {

// --- constants ---

   private static final int ID_LENGTH_MIN = 6;
   private static final String ID_PADDING = "000000";

   private static final String SUFFIX_TXT = ".txt";

   private static SimpleDateFormat dateFormatFuji3 = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a");

// --- subclass hooks ---

   public String getShortName() { return Text.get(this,"s1"); }

   public int[] getAllowedCompletionModes() { return new int[] { COMPLETION_MODE_MANUAL, COMPLETION_MODE_DETECT }; }
   public int   getCompletionMode(Object formatConfig) { return ((Fuji3Config) formatConfig).enableCompletion ? COMPLETION_MODE_DETECT : COMPLETION_MODE_MANUAL; }
   public void  setCompletionMode(Object formatConfig, int mode) { ((Fuji3Config) formatConfig).enableCompletion = (mode == COMPLETION_MODE_DETECT); }

// --- format function ---

   public void format(Job job, Order order, Object formatConfig) throws Exception {}
   //
   public boolean formatStoppable(Job job, Order order, Object formatConfig, FormatCallback fc) throws Exception {

      Fuji3Config config = (Fuji3Config) formatConfig;
      // because of validation, the cast is guaranteed to work

      require(config.requestDir);
      require(config.imageDir);

   // (0) set up stuff

      // coupled variation not necessary here, but nice; see FormatFujiNew

      String fujiID = Convert.fromInt(order.orderID);
      int pad = ID_LENGTH_MIN - fujiID.length();
      if (pad > 0) fujiID = ID_PADDING.substring(0,pad) + fujiID;

      File f = new File(config.requestDir,fujiID);
      f = vary(f,new PrefixVariation(0),
                 new OrActivity(new SuffixedActivity(SUFFIX_TXT),
                                new RelocatedActivity(config.imageDir)));

      fujiID = f.getName();

      File genr = new File(config.requestDir,fujiID + SUFFIX_TXT);
      File root = new File(config.imageDir,fujiID);

      File mapImageDir = (config.mapImageDir != null) ? config.mapImageDir : config.imageDir;
      File mapRoot = new File(mapImageDir,fujiID);

      LinkedList files = new LinkedList();

   // (1) plan the operation ; also check that mappings are defined

      LinkedList ops = new LinkedList();
      ops.add(new Op.Mkdir(root));

      HashSet skus = new HashSet();
      HashSet printCodes = new HashSet();
      HashSet fset = new HashSet();
      HashSet normalSet = new HashSet();
      HashSet cdSet     = new HashSet();

      boolean first = true;
      String surface = null;

      Iterator i = job.refs.iterator();
      while (i.hasNext()) {
         Job.Ref ref = (Job.Ref) i.next();

         boolean cd = ref.sku.equals(order.specialSKU);

         // for every different SKU (except the CD one) ...
         if (skus.add(ref.sku) && ! cd) {

            Fuji3Mapping m = (Fuji3Mapping) MappingUtil.getMapping(config.mappings,ref.sku);
            if (m == null) missingChannel(ref.sku,order.getFullID());

            printCodes.add(m.printCode);

            // also get the surface, and check it's the same for all refs.
            // this isn't just a formality, since the surface is editable!
            if (first) {
               surface = m.surface;
               first = false;
            } else {
               if ( ! Nullable.equals(m.surface,surface) ) throw new Exception(Text.get(this,"e1"));
            }
         }

         // for every different file ...
         if (fset.add(ref.filename)) {
            files.add(ref.filename);
            ops.add(new Op.Copy(new File(root,ref.filename),order.getPath(ref.filename)));
         }

         // also keep track of file sets
         if (cd)  cdSet.add(ref.filename);
         else normalSet.add(ref.filename);
      }

      // only generate request file after images are copied
      // note, we don't own this, doesn't go in file list
      ops.add(new Fuji3Generate(genr,fujiID,mapRoot,surface,normalSet,cdSet,job,order,config,fc));

   // (2) alter files

      Op.transact(ops);

   // (3) alter object

      job.dir = root;
      job.files = files;

      // non-null property means we check for completion,
      // and the value tells what order ID to use.
      if (config.enableCompletion) job.property = FujiCompletion.getProperty(fujiID,job,order,printCodes);

      return true;
   }

// --- metafile generation ---

   private static class Fuji3Generate extends Op.Generate {

      private String fujiID;
      private File mapRoot;
      private String surface;
      private HashSet normalSet;
      private HashSet cdSet;
      private Job job;
      private Order order;
      private Fuji3Config config;
      private FormatCallback fc;

      public Fuji3Generate(File dest, String fujiID, File mapRoot, String surface, HashSet normalSet, HashSet cdSet, Job job, Order order, Fuji3Config config, FormatCallback fc) {
         super(dest);
         this.fujiID = fujiID;
         this.mapRoot = mapRoot;
         this.surface = surface;
         this.normalSet = normalSet;
         this.cdSet = cdSet;
         this.job = job;
         this.order = order;
         this.config = config;
         this.fc = fc;
      }

      public void subdo(Writer writer) throws IOException {

      // general info

         writer.write("[OrderInfo]" + line);
         writer.write("Order_ID=" + fujiID + line);
         writer.write("ImagePath=" + Convert.fromFile(mapRoot) + line);

         int cdQuantity = specialQuantity(job,order);
         if (cdQuantity != -1) {
            writer.write("CD=" + Convert.fromInt(cdQuantity) + line);
         }

         if (config.printer != null) {
            writer.write("Printer=" + config.printer + line);
         }

         if (surface != null) {
            writer.write("Surface=" + surface + line);
         }
         // surface could be null because that's how all the items are configured,
         // or it could be null because this is a pure CD job and we don't use it.

         if (config.autoCorrect != null) {
            writer.write("AutoCorrect=" + (config.autoCorrect.booleanValue() ? "1" : "0") + line);
         }
         // this is a boolean for us, but JobMaker thinks of it as an integer,
         // with any non-zero value meaning true.

         // the customer information is always non-null, but might be empty,
         // especially the phone number.
         // so, let's be polite and not send it unless it's filled in.

         String name = order.getFullName();
         if (name.length() > 0) {
            writer.write("CustomerName=" + name + line);
         }

         if (order.phone.length() > 0) {
            writer.write("Phone=" + order.phone + line);
         }

         if (order.email.length() > 0) {
            writer.write("Email=" + order.email + line);
         }

         HashMap dueMap = fc.getDueMap(); // if you use this in other integrations, also update usesDueMap !
         Long dueInterval = DueUtil.getDueInterval(order,dueMap);
         // note that the due interval is for the whole order, not just the prints in this job
         //
         if (order.invoiceDate != null && dueInterval != null) {
            // invoice date is always non-null now, so it's not inefficient to collect due information
            // before checking it.

            long dueTimeL = order.invoiceDate.getTime() + dueInterval.longValue();
            String dueTimeS;
            synchronized (dateFormatFuji3) {
               dueTimeS = dateFormatFuji3.format(new Date(dueTimeL));
            }
            writer.write("DueTime=" + dueTimeS + line);
         }

         writer.write(line);

      // normal images

         Iterator i = normalSet.iterator();
         while (i.hasNext()) {
            String filename = (String) i.next();

            writer.write("[ImageInfo]" + line);
            writer.write("ImageFile=" + filename + line); // path given by ImagePath here

            Backprint b = (config.backprints.size() == 0) ? null : (Backprint) config.backprints.getFirst();
               // there should always be exactly one backprint object,
               // but we don't validate for that, so cover everything.

            Iterator j = job.refs.iterator();
            while (j.hasNext()) {
               Job.Ref ref = (Job.Ref) j.next();

               if ( ! ref.filename.equals(filename) || ref.sku.equals(order.specialSKU) ) continue;

               Order.Item item = getOrderItem(order,ref);

               Fuji3Mapping m = (Fuji3Mapping) MappingUtil.getMapping(config.mappings,ref.sku);

               // normally, the backprint is associated with the print (order item),
               // not the image (filename), so the backprint generation assumes we
               // have an item available.  here we don't, but we can fake it nicely.
               // the trick is, the only things the order item is used for are
               // sequences, which we don't allow, and filename display, for which
               // it doesn't matter which item we use.  so, we can just generate
               // the backprint off the first item, before we emit the print header.
               //
               if (b != null) {
                  String s = b.generate(order,item,/* sequence = */ null); // validated no sequence
                  if (s != null && s.length() > 0) {
                     writer.write("BackPrint=" + s + line);
                  }
                  // there are some restrictions on what we can print,
                  // but the spec says JobMaker will take care of it.

                  b = null; // only want backprint for the first item
               }

               writer.write("[Print]" + line);
               writer.write("PrintCode=" + m.printCode + line);
               writer.write("PrintQty=" + Convert.fromInt(job.getOutputQuantity(item,ref)) + line);
            }

            writer.write(line);
         }

      // CD images

         // cdSet.size() > 0 iff cdQuantity != -1, so no need to check quantity, just iterate

         i = cdSet.iterator();
         while (i.hasNext()) {
            String filename = (String) i.next();

            writer.write("[ImageInfo]" + line);
            writer.write("OriginalPath=" + Convert.fromFile(new File(mapRoot,filename)) + line);

            writer.write(line);
         }
      }
   }

// --- job splitter ---

   // not sure where this will end up, but this is a decent place for it for now

   /**
    * @return A partition of the skus collection, in the form of a collection
    *         of collections (sets) of SKUs.  In other words,
    *         the sets are disjoint and their union is the initial collection.
    *         The sets aren't required to be Java sets, just collections, and
    *         in fact they're actually linked lists.
    */
   public static Collection split(Collection skus, Collection items, Fuji3Config config, SKU specialSKU) {

   // first step, sort the SKUs by surface

      HashMap map = new HashMap(); // map from surface to linked list of SKU (*)
      int noMapping = 0;
      boolean hasSpecialSKU = false;

      // (*) also there are two other kinds of entries.  if a SKU has no mapping,
      // we'll create a set just for that SKU, and file it under integer >= 0
      // so that there's no possibility of collision with the surfaces (which can
      // be null or String).  if we need to create a CD-only set, we'll file that
      // under integer -1 (even though in that case there are no other entries).
      // actually now that we have OldSKU, we could use the SKU as a key with no
      // danger of collision if we wanted.
      //
      // why are the collections linked lists instead of, say, hash sets?
      // it could easily go either way, but I figure the collections
      // are small enough that a list will be faster than a hash set when
      // you account for the effort of hashing and scanning.

      Iterator i = skus.iterator();
      while (i.hasNext()) {
         SKU sku = (SKU) i.next();

         if (sku.equals(specialSKU)) {
            hasSpecialSKU = true; // deal with this later
            continue;
         }
         // special SKU won't have a mapping, so we have to capture it early

         Fuji3Mapping m = (Fuji3Mapping) MappingUtil.getMapping(config.mappings,sku);
         if (m == null) {
            map.put(new Integer(noMapping++),singletonList(sku));
            continue;
         }
         // normally we'd call missingChannel here, but I don't like the fact that
         // we might be running in the UI thread (if the user has issued one of the
         // print commands).  so, instead, put the missing-channel SKU into a job
         // by itself, and let the error message come from the job formatter thread.
         // it really doesn't matter much either way, we only get here after the
         // order has passed through the missing-channel validation in SpawnThread.

         // surface can be null, but we want that to be a sort category,
         // and HashMap allows null key.

         LinkedList list = (LinkedList) map.get(m.surface);

         if (list == null) {
            list = new LinkedList();
            map.put(m.surface,list);
         }
         list.add(sku);
      }

   // if there's a CD, attach it to whichever surface has the most overlap with it

      // Q: why not just send the CD as a separate job?
      // A: because we want to handle the most common case well, which is,
      //    one set of images, with one print each plus a CD.
      //    that kind of order should go over to Fuji as one job, not two.
      //
      // soon we'll be changing Opie so that CDs use original images instead of
      // cropped / whitespaced / resized ones, and after that there won't be
      // any overlap any more.  still, not much harm in trying, it just takes a second.

      if (hasSpecialSKU) {

         HashSet filenameSetSpecial = computeFilenameSet(singletonList(specialSKU),items);
         int max = -1;
         LinkedList maxList = null;

         i = map.values().iterator();
         while (i.hasNext()) {
            LinkedList list = (LinkedList) i.next();

            HashSet filenameSet = computeFilenameSet(list,items);
            filenameSet.retainAll(filenameSetSpecial); // intersection
            int overlap = filenameSet.size();
            // it'd be slightly faster to count the intersections
            // without modifying the set, but this is good enough

            if (overlap > max) { // always true first time
               max = overlap;
               maxList = list;
            }
         }

         if (maxList == null) { // there are no other SKUs!
            maxList = new LinkedList();
            map.put(new Integer(-1),maxList);
            // doesn't matter where we put it, but this is safe
         }
         maxList.add(specialSKU);
      }

   // done

      return map.values(); // a little weird, to return the values as a collection
      // and let that be the only reference to the underlying hash map, but it works
   }

   private static HashSet computeFilenameSet(LinkedList list, Collection items) {
      HashSet set = new HashSet();

      Iterator i = items.iterator();
      while (i.hasNext()) {
         Order.Item item = (Order.Item) i.next();
         if (list.contains(item.sku)) {
            if (item.isMultiImage()) {
               set.addAll(item.filenames);
            } else {
               set.add   (item.filename );
            }
            // Q: Fuji3 integration isn't multi-image aware, so why are we
            //    handling the multi-image case here?
            // A: because this code runs during job creation, before we've
            //    blocked multi-image items from non-multi-image queues,
            //    and it was easier to make this multi-image aware than to
            //    rework the whole job creation process.
         }
      }

      return set;
   }

   private static LinkedList singletonList(Object o) {
      LinkedList list = new LinkedList();
      list.add(o);
      return list;
      // Collections.singletonList is close, but we need mutable in some cases
   }

// --- completion ---

   public boolean isCompleteWithoutDirectory(String property, Special special) throws Exception {

      if (property == null) return false; // no completion

      Fuji3Config config = (Fuji3Config) special.getQueueConfig();
      return config.completion.isComplete(property);
   }

}

