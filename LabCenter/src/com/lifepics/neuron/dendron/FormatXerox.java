/*
 * FormatXerox.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.misc.Op;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.LinkedList;

/**
 * Implementation of the Xerox format.
 */

public class FormatXerox extends Format {

// --- constants ---

   private static final String SUFFIX_PDF = ".pdf";
   private static final String SUFFIX_CSV = ".csv";

// --- subclass hooks ---

   public String getShortName() { return Text.get(this,"s1"); }

   public int[] getAllowedCompletionModes() { return new int[] { COMPLETION_MODE_MANUAL, COMPLETION_MODE_AUTO }; }
   public int   getCompletionMode(Object formatConfig) { return ((XeroxConfig) formatConfig).completeImmediately ? COMPLETION_MODE_AUTO : COMPLETION_MODE_MANUAL; }
   public void  setCompletionMode(Object formatConfig, int mode) { ((XeroxConfig) formatConfig).completeImmediately = (mode == COMPLETION_MODE_AUTO); }

// --- format function ---

   public void format(Job job, Order order, Object formatConfig) throws Exception {

      XeroxConfig config = (XeroxConfig) formatConfig;
      // because of validation, the cast is guaranteed to work

      require(config.requestDir);
      require(config.imageDir);

      Convert.validateUsable(config.useDigits,order.orderID); // a formality

      File root = new File(config.imageDir,config.prefix + Convert.fromIntNDigit(config.useDigits,order.orderID));
      root = vary(root,new PrefixVariation(config.prefix.length()));
      LinkedList files = new LinkedList();

   // check things and gather information

      if (job.refs.size() != 1) throw new Exception(Text.get(this,"e1")); // shouldn't happen
      Job.Ref ref = (Job.Ref) job.refs.getFirst();

      if ( ! ref.filename.toLowerCase().endsWith(SUFFIX_PDF) ) throw new Exception(Text.get(this,"e2"));
      // go ahead and check this before we do too much work

      XeroxMapping m = (XeroxMapping) MappingUtil.getMapping(config.mappings,ref.sku);
      if (m == null) missingChannel(ref.sku,order.getFullID());

   // gather more information

      String useID = root.getName();

      File useDir = (config.mapImageDir != null) ? config.mapImageDir : config.imageDir;

      String useFile = ref.filename.substring(0,ref.filename.length()-SUFFIX_PDF.length());
      useFile = XeroxConfig.ruleStrict.matcher(useFile).replaceAll("") + SUFFIX_PDF;
      // have to remove and re-add suffix because the pattern excludes the period in it

      Order.Item item = order.getItem(ref);
      int useQuantity = job.getOutputQuantity(item,ref);

   // (1) plan the operation

      LinkedList ops = new LinkedList();
      ops.add(new Op.Mkdir(root));

      // filename computed above
      files.add(useFile);
      ops.add(new Op.Copy(new File(root,useFile),order.getPath(ref.filename)));

      File dest = new File(config.requestDir,useID + SUFFIX_CSV);
      // we don't own CSV file
      ops.add(new XeroxGenerate(dest,m,useID,useDir,useFile,useQuantity));

   // (2) alter files

      Op.transact(ops);

   // (3) alter object

      job.dir = root;
      job.files = files;

      if (config.completeImmediately) job.property = "";
   }

// --- generation ---

   private static class XeroxGenerate extends Op.Generate {

      private XeroxMapping m;
      private String useID;
      private File useDir;
      private String useFile;
      private int useQuantity;

      public XeroxGenerate(File dest, XeroxMapping m, String useID, File useDir, String useFile, int useQuantity) {
         super(dest);
         this.m = m;
         this.useID = useID;
         this.useDir = useDir;
         this.useFile = useFile;
         this.useQuantity = useQuantity;
      }

      public void subdo(Writer writer) throws IOException {

         writer.write("#Product,Order #,FileName,Description,Folder,Qty");
         writer.write(line);

         writer.write(m.productID);
         writer.write(",");
         writer.write(useID);
         writer.write(",");
         writer.write(useFile);
         writer.write(",");
         writer.write(m.description);
         writer.write(",");
         writer.write(Convert.fromFile(new File(useDir,useID))); // (*)
         writer.write(",");
         writer.write(Convert.fromInt(useQuantity));
         writer.write(",,,,,"); // 5 data fields
         writer.write(line);

         // (*) useDir has to satisfy ruleStrict.  we've validated that,
         // but we haven't validated useID .. but it's OK, we know
         // that's just an alphabetic prefix plus some number of digits.
      }
   }

// --- completion ---

   public boolean isCompleteWithoutDirectory(String property, Special special) throws Exception {
      return (property != null);
      // not null means job should complete immediately
   }

}

