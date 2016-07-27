/*
 * FormatNoritsu.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.misc.Op;
import com.lifepics.neuron.struct.SKU;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Implementation of the Noritsu format.
 */

public class FormatNoritsu extends Format {

// --- constants ---

   // the official name for this file format is DPOF, Digital Print Order Format.
   // like Exif, it's something developed by a bunch of Japanese companies.
   // you can find some public information online, but most of it is proprietary.

   private static final int    PREFIX_LENGTH = 1;
   private static final String PREFIX_P = "p"; // building -- only used within transaction
   private static final String PREFIX_O = "o"; // ready
   private static final String PREFIX_E = "e"; // accepted
   private static final String PREFIX_Q = "q"; // failed   -- not used, just for reference (*)

   // (*) the "q" code indicates job submission failure, not failure of the print engine

   private static final String IMAGE_DIR = "IMAGE";
   private static final String META_DIR  = "MISC";
   private static final String META_FILE = "AUTPRINT.MRK";

   private static SimpleDateFormat dateFormatNoritsu = new SimpleDateFormat("yyyy:MM:dd:HH:mm:ss");

// --- subclass hooks ---

   public String getShortName() { return Text.get(this,"s1"); }

   public int[] getAllowedCompletionModes() { return new int[] { COMPLETION_MODE_DETECT }; }
   public int   getCompletionMode(Object formatConfig) { return COMPLETION_MODE_DETECT; }
   public void  setCompletionMode(Object formatConfig, int mode) {}

// --- filename variation ---

   private static Variation noritsuVariation = new PrefixVariation(PREFIX_LENGTH);
   private static Activity noritsuActivity = new SubstringActivity(PREFIX_LENGTH,0);

   // the activity is too broad ... as far as I know, the only prefixes that
   // occur are the ones listed above.  however, maybe there are
   // intermediate ones that I don't know about, so go ahead and be too broad.

// --- format function ---

   public void format(Job job, Order order, Object formatConfig) throws Exception {

      NoritsuConfig config = (NoritsuConfig) formatConfig;
      // because of validation, the cast is guaranteed to work

      require(config.dataDir);

      if (job.refs.isEmpty()) throw new Exception(Text.get(this,"e6"));
      SKU sku = ((Job.Ref) job.refs.getFirst()).sku;
      NoritsuMapping m = (NoritsuMapping) MappingUtil.getMapping(config.mappings,sku);
      if (m == null) missingChannel(sku,order.getFullID());

      File root;
      if (config.strict) {

         // we have to validate ... fromIntNDigit will accept anything
         Convert.validateUsable(NoritsuConfig.NDIGIT_ORDER_ID,order.orderID); // allow truncation

         root = new File(config.dataDir,PREFIX_O + Convert.fromIntNDigit(NoritsuConfig.NDIGIT_ORDER_ID,order.orderID));
         root = vary(root,noritsuVariation,noritsuActivity);

      } else {

         Convert.validateUsable(NoritsuConfig.NDIGIT_SHORT_ID,order.orderID); // allow truncation

         root = new File(config.dataDir,PREFIX_O + Convert.fromIntNDigit(NoritsuConfig.NDIGIT_SHORT_ID,order.orderID) + "." + m.channel);
         root = vary(root,noritsuVariation,noritsuActivity);
      }
      // fortunately the completion behavior is the same for both

      LinkedList files = new LinkedList();
      LengthLimit lengthLimit = new LengthLimit(config.limitEnable,config.limitLength);

   // (0) now that the root has been varied, swap the prefix

      File rootFinal = root;
      root = new File(root.getParentFile(),PREFIX_P + root.getName().substring(PREFIX_LENGTH));

   // (1) plan the operation

      LinkedList ops = new LinkedList();
      ops.add(new Op.Mkdir(root));

      files.add(IMAGE_DIR);
      ops.add(new Op.Mkdir(new File(root,IMAGE_DIR)));
      files.add(META_DIR);
      ops.add(new Op.Mkdir(new File(root,META_DIR)));

      String file = new File(META_DIR,META_FILE).getPath();
      files.add(file);
      ops.add(new NoritsuGenerate(new File(root,file),job,order,config,sku,lengthLimit));

      HashSet fset = new HashSet();

      Iterator i = job.refs.iterator();
      while (i.hasNext()) {
         Job.Ref ref = (Job.Ref) i.next();
         if ( ! ref.sku.equals(sku) ) throw new Exception(Text.get(this,"e7",new Object[] { sku.toString(), ref.sku.toString() }));
         // check against actual SKU, just in case

         Order.Item item = order.getItem(ref);

         boolean itemIsMultiImage = item.isMultiImage();
         if (itemIsMultiImage && job.refs.size() > 1) throw new Exception(Text.get(this,"e8"));
         // another sanity check ...
         // the job splitter is supposed to put multi-image items into jobs by themselves

         List filenames = itemIsMultiImage ? item.filenames : Collections.singletonList(item.filename);
         Iterator j = filenames.iterator();
         while (j.hasNext()) {
            String filename = (String) j.next();
            if (fset.add(filename)) {
               file = new File(IMAGE_DIR,lengthLimit.transform(filename)).getPath();
               files.add(file);
               ops.add(new Op.Copy(new File(root,file),order.getPath(filename)));
            }
            // fset only acts on multi-image items, since regular single-image ones
            // are forced to be unique by the primary-key-ness of filename plus SKU.
         }
      }

      ops.add(new Op.Move(rootFinal,root));

   // (2) alter files

      Op.transact(ops);

   // (3) alter object

      job.dir = rootFinal;
      job.files = files;
   }

// --- metafile generation ---

   private static class NoritsuGenerate extends Op.Generate {

      private Job job;
      private Order order;
      private NoritsuConfig config;
      private SKU sku;
      private LengthLimit lengthLimit;

      public NoritsuGenerate(File dest, Job job, Order order, NoritsuConfig config, SKU sku, LengthLimit lengthLimit) {
         super(dest);
         this.job = job;
         this.order = order;
         this.config = config;
         this.sku = sku;
         this.lengthLimit = lengthLimit;
      }

      public void subdo(Writer writer) throws IOException {

         // because the Noritsu only wants to see one SKU per job,
         // the sequence number ends up being the same as the PID.
         // still, it's nice to make the code match Konica.

         Backprint.Sequence sequence = new Backprint.Sequence();
         boolean hasSequence = Backprint.hasSequence(config.backprints);

         String generateDate;
         synchronized (dateFormatNoritsu) {
            generateDate = dateFormatNoritsu.format(new Date());
         }

         NoritsuMapping m = (NoritsuMapping) MappingUtil.getMapping(config.mappings,sku);

         // VUQ stands for Vendor UniQue, i.e., specific to Noritsu (in this case).
         // CVP stands for Correction Value Print; they think of backprints as
         // a place to display info like what kind of photo corrections were used.
         // the others you can pretty much figure out .. or see the archived docs.

         writer.write("[HDR]" + line);
         writer.write("GEN REV = 01.00" + line);
         writer.write("GEN CRT = \"NORITSU KOKI\" -01.00" + line);
         writer.write("GEN DTM = " + generateDate + line);
         writer.write("USR NAM = \"" + Convert.fromInt(order.orderID) + "\"" + line);
         writer.write("VUQ RGN = BGN" + line);
         writer.write("VUQ VNM = \"NORITSU KOKI\" -ATR \"QSSPrint\"" + line);
         writer.write("VUQ VER = 01.00" + line);
         writer.write("PRT PSL = NML -PSIZE \"" + sku.toString() + "\"" + line);
         writer.write("PRT PCH = " + m.channel + line);
         writer.write("GEN INP = \"ZIP\"" + line);

         Job.Ref refFirst = (Job.Ref) job.refs.getFirst();
         Order.Item itemFirst = getOrderItem(order,refFirst);
         boolean itemIsMultiImage = itemFirst.isMultiImage();
         if (itemIsMultiImage && ! config.printSingleSided) {

            // by validation earlier, we know this is the only item,
            // so write its quantity into the duplex quantity field.

            int quantity = job.getOutputQuantity(itemFirst,refFirst);
            try {
               Convert.validateNDigit(NoritsuConfig.NDIGIT_SET,quantity);
            } catch (ValidationException e) {
               throw (IOException) new IOException(Text.get(FormatNoritsu.class,"e3")).initCause(e);
            }
            writer.write("PRT SET = " + Convert.fromIntNDigit(NoritsuConfig.NDIGIT_SET,quantity) + line);

            // we don't do chunks for multi-image products,
            // but we do allow slightly more digits in the quantity field.
         }

         writer.write("VUQ RGN = END" + line);
         writer.write(line);

         int pid = 1;

         if (itemIsMultiImage) {

            // easier to just handle this case separately.
            // there are no chunks, backprints, or sequences;
            // the sequence object we constructed isn't used.

            // also, here it was easier to handle single-sided creative products directly, without
            // using the Deref utility stuff I wrote for Kodak.  the problem is, the Deref objects
            // don't carry enough information to produce backprints.  I'm not sure we really want
            // backprints, but right now it'd be premature to just drop them in the multi-image case.

            int count = config.printSingleSided ? job.getOutputQuantity(itemFirst,refFirst) : 1;
            while (count-- > 0) {

               Iterator i = (config.printSingleSided && config.collateInReverse) ? new ReverseIterator(itemFirst.filenames) : itemFirst.filenames.iterator();
               while (i.hasNext()) {
                  String filename = (String) i.next();

                  blockBegin(writer,pid++,/* quantity = */ 1, filename);
                  blockEnd(writer);

                  // the quantity here has to be nonzero,
                  // but is otherwise ignored.
                  // the duplex quantity controls it all.
                  // actually for single-sided creatives,
                  // it's not ignored,  has to be 1.
               }
            }

         } else {

            Iterator i = job.refs.iterator();
            while (i.hasNext()) {
               Job.Ref ref = (Job.Ref) i.next();
               Order.Item item = getOrderItem(order,ref);

               Iterator k = new ChunkIterator(job.getOutputQuantity(item,ref),NoritsuConfig.CHUNK_STD,NoritsuConfig.CHUNK_MAX,hasSequence);
               while (k.hasNext()) {
                  int quantity = ((Integer) k.next()).intValue();

                  blockBegin(writer,pid++,quantity,item.filename);
                  int cvp = 1;
                  Iterator j = config.backprints.iterator();
                  while (j.hasNext()) {
                     Backprint b = (Backprint) j.next();

                     writer.write("PRT CVP" + Convert.fromInt(cvp++) + " = " + toNoritsuString(b.generate(order,item,sequence)) + line);
                  }
                  blockEnd(writer);

                  sequence.advance(item);
               }
            }
         }
      }

      private void blockBegin(Writer writer, int pid, int quantity, String filename) throws IOException {

         // these shouldn't happen, this is just for thoroughness
         // (except that multi-image items aren't chunked, and could fail the PID test if they have a thousand images)
         try {
            Convert.validateNDigit(NoritsuConfig.NDIGIT_PID,pid);
            Convert.validateNDigit(NoritsuConfig.NDIGIT_QUANTITY,quantity); // should always pass now, but check anyway
         } catch (ValidationException e) {
            throw (IOException) new IOException(Text.get(FormatNoritsu.class,"e2")).initCause(e);
         }

         writer.write("[JOB]" + line);
         writer.write("PRT PID = " + Convert.fromIntNDigit(NoritsuConfig.NDIGIT_PID,pid) + line);
         writer.write("PRT TYP = STD" + line);
         writer.write("PRT QTY = " + Convert.fromIntNDigit(NoritsuConfig.NDIGIT_QUANTITY,quantity) + line);
         writer.write("IMG FMT = EXIF2 -J" + line);
         writer.write("<IMG SRC = \"../" + IMAGE_DIR + "/" + lengthLimit.transform(filename) + "\">" + line);
         writer.write("VUQ RGN = BGN" + line);
         writer.write("VUQ VNM = \"NORITSU KOKI\" -ATR \"QSSPrint\"" + line);
         writer.write("VUQ VER = 01.00" + line);
      }

      private void blockEnd(Writer writer) throws IOException {

         writer.write("VUQ RGN = END" + line);
         writer.write(line);
      }

      private static String toNoritsuString(String s) {

         // it would make sense to allow a zero-length string to create an empty -STR,
         // but apparently that crashes some of the Noritsu labs.  so, don't allow it.

         return (s == null || s.length() == 0) ? "0" : ("1 -STR \"" + s + "\"");
      }
   }

// --- completion ---

   private String transform(String s) {
      return s.startsWith(PREFIX_O) ? PREFIX_E + s.substring(PREFIX_LENGTH) : null;
   }

   protected File getComplete(File dir, String property) {

      String name = transform(dir.getName());
      if (name == null) return null; // don't know how to mark

      return new File(dir.getParentFile(),name);
   }

   public void makeComplete(Order order) {
      if (order.deployDir != null) {

         String temp = transform(order.deployPrefix);
         if (temp == null) return; // don't know how to mark

         Iterator i = order.deployFiles.iterator();
         while (i.hasNext()) {
            File dir = new File(order.deployDir,order.deployPrefix + (String) i.next());
            makeComplete(dir,null); // ignore result
         }

         order.deployPrefix = temp;

      } else { // old Noritsu format, rename whole order directory

         order.orderDir = makeComplete(order.orderDir,null);
      }
   }

}

