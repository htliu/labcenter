/*
 * FormatZBE.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.misc.Op;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Implementation of the ZBE format.
 */

public class FormatZBE extends Format {

// --- constants ---

   private static final String SUFFIX_HOF = ".hof";

   private static final String SUBDIR_FINISHED = "ChromiraTiffFileFinished";
   private static final String SUBDIR_ABORTED  = "ChromiraTiffFileAborted";

// --- subclass hooks ---

   public String getShortName() { return Text.get(this,"s1"); }

   public int[] getAllowedCompletionModes() { return new int[] { COMPLETION_MODE_MANUAL, COMPLETION_MODE_ACCEPT }; }
   public int   getCompletionMode(Object formatConfig) { return ((ZBEConfig) formatConfig).enableCompletion ? COMPLETION_MODE_ACCEPT : COMPLETION_MODE_MANUAL; }
   public void  setCompletionMode(Object formatConfig, int mode) { ((ZBEConfig) formatConfig).enableCompletion = (mode == COMPLETION_MODE_ACCEPT); }

// --- format function ---

   public void format(Job job, Order order, Object formatConfig) throws Exception {

      ZBEConfig config = (ZBEConfig) formatConfig;
      // because of validation, the cast is guaranteed to work

      require(config.requestDir);
      require(config.imageDir);

   // (0) figure out generated file name

      File gen = new File(config.requestDir,config.prefix + Convert.fromInt(order.orderID) + SUFFIX_HOF);
      Variation v = new SuffixVariation();
      Activity  a = new OrActivity(new StandardActivity(),
                                   new RelocatedActivity(new File(config.requestDir,SUBDIR_FINISHED)),
                                   new RelocatedActivity(new File(config.requestDir,SUBDIR_ABORTED )));
      gen = vary(gen,v,a);

      String orderID = gen.getName();
      orderID = orderID.substring(0,orderID.length()-SUFFIX_HOF.length());

      // the ZBE can take control of files, but not of folders,
      // so here we don't make a new subdirectory for the images,
      // we just dump them in the image directory and vary them.
      // if that ever changes, you'll need to fix up the mapping
      // code in ZBEGenerate too!
      //
      // the other plan would be to keep control of the files
      // for ourselves, but that's risky ... we have an option
      // to complete the job as soon as the queue accepts it,
      // so we could easily purge the images before they print.
      //
      // somehow I've managed to avoid that problem until now
      // by luck -- there are no integrations that own files
      // and have auto or accept as an allowed completion mode.

   // (1) plan the operation ; also check that mappings are defined

      LinkedList ops = new LinkedList();
      LinkedList records = new LinkedList();
      HashSet skus = new HashSet();

      // suffix variation is fine, but we need a new activity
      HashActivity hashActivity = new HashActivity();
      a = new OrActivity(new StandardActivity(),hashActivity);

      Iterator i = job.refs.iterator();
      while (i.hasNext()) {
         Job.Ref ref = (Job.Ref) i.next();

         if (skus.add(ref.sku) && ! MappingUtil.existsMapping(config.mappings,ref.sku)) missingChannel(ref.sku,order.getFullID());

         File dest = new File(config.imageDir,ref.filename);
         dest = vary(dest,v,a);
         hashActivity.add(dest); // add final decision to set
         //
         // as noted above, we want ZBE to take control of the images.
         // we do that by setting the DeleteAfterPrinting flag ...
         // but we don't know what order the images will be printed in,
         // so we can't share the images across prints.

         ops.add(new Op.Copy(dest,order.getPath(ref.filename)));
         records.add(new Record(ref,dest)); // easy way to associate ref with dest
      }

      ops.add(new ZBEGenerate(gen,orderID,records,job,order,config));

   // (2) alter files

      Op.transact(ops);

   // (3) alter object

      if (config.enableCompletion) job.property = Convert.fromFile(gen);
   }

// --- record class ---

   private static class Record {
      public Job.Ref ref;
      public File dest;
      public Record(Job.Ref ref, File dest) { this.ref = ref; this.dest = dest; }
   }

// --- metafile generation ---

   private static class ZBEGenerate extends Op.Generate {

      private String orderID;
      private LinkedList records;
      private Job job;
      private Order order;
      private ZBEConfig config;

      private Writer writer;
      private String indent;

      public ZBEGenerate(File dest, String orderID, LinkedList records, Job job, Order order, ZBEConfig config) {
         super(dest);
         this.orderID = orderID;
         this.records = records;
         this.job = job;
         this.order = order;
         this.config = config;
      }

      private void normal() { indent = "";    }
      private void indent() { indent = "   "; }

      private String start(String name) { return "<" +  name + ">"; }
      private String end  (String name) { return "</" + name + ">"; }
      private void tag(String name, String value) throws IOException {
         writer.write(indent + start(name) + value + end(name) + line);
      }
      // the format looks like XML, but it's not, among other reasons
      // because there's no top-level node

      private static String zeroOne(boolean b) { return b ? "1" : "0"; }

      public void subdo(Writer writer) throws IOException {

         this.writer = writer;
         normal();

         tag("OrderID",orderID); // optional, but we always send it
         if (config.includeCustomer) tag("Customer",order.getFullName());
         if (config.submitOnHold    != null) tag("SubmitOnHold",zeroOne(   config.submitOnHold   .booleanValue() ));
         if (config.submitForReview != null) tag("PrintJobs",   zeroOne( ! config.submitForReview.booleanValue() ));
         // yes, the second one is inverted; it reads better in the UI that way

         File mapImageDir = (config.mapImageDir != null) ? config.mapImageDir : config.imageDir;
         int n = 0;

         Iterator i = records.iterator();
         while (i.hasNext()) {
            Record r = (Record) i.next();
            Order.Item item = getOrderItem(order,r.ref);

            ZBEMapping m = (ZBEMapping) MappingUtil.getMapping(config.mappings,r.ref.sku);
            String print = "Print " + Convert.fromInt(n++);

            writer.write(start(print) + line);
            indent();

            tag("ImagePath",Convert.fromFile(new File(mapImageDir,r.dest.getName())));
            tag("Units",Convert.fromInt(config.units)); // I chose to make this per queue instead of per SKU
            tag("Width", Convert.fromDouble(m.width ));
            tag("Height",Convert.fromDouble(m.height));
            tag("DeleteAfterPrinting",zeroOne(true)); // optional, but we always send it
            tag("Copies",Convert.fromInt(job.getOutputQuantity(item,r.ref))); // ditto

            if (m.colorProfile != null) tag("Profiling",m.colorProfile.booleanValue() ? "2" : "1");
            // this is a three-way enumeration in the spec,
            // but the "0" value means "take the default", same as not saying anything.
            // logically it's a bit different, but it seems like overkill to have both.

            String backprint = Backprint.generate(config.backprints,"\\n",order,item,/* sequence = */ null); // validated no sequence
            if (backprint != null) tag("Notes",backprint);

            if (m.productPath != null) tag("ProductPath",Convert.fromFile(new File(config.productRoot,m.productPath)));
            // by validation, productPath != null implies productRoot != null

            normal();
            writer.write(end(print) + line);
         }
      }
   }

// --- completion ---

   public boolean isCompleteWithoutDirectory(String property, Special special) throws Exception {

      if (property == null) return false; // completion wasn't enabled
      File gen = Convert.toFile(property);

      File requestDir = gen.getParentFile();
      // even if we had access to the config object, we wouldn't want to use
      // config.requestDir here, since that could have changed since we were
      // created.

      File finished = new File(new File(requestDir,SUBDIR_FINISHED),gen.getName());
      File aborted  = new File(new File(requestDir,SUBDIR_ABORTED ),gen.getName());

      if (finished.exists()) return true;
      if (aborted .exists()) throw new Exception(Text.get(this,"e1"));

      return false;

      // this is fairly lax about cases where multiple files exist,
      // but that shouldn't happen.  it's possible that the file
      // might go missing entirely, but if so, I think it's enough
      // for us to just stall and not auto-complete.
   }

}

