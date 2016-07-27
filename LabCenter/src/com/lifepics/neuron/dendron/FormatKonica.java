/*
 * FormatKonica.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.misc.Op;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Implementation of the Konica format.
 */

public class FormatKonica extends Format {

// --- constants ---

   private static final int    SUFFIX_LENGTH = 4;
   private static final int    SUFFIX_CUT    = 3;
   private static final String SUFFIX_ORD = ".ord";
   private static final String SUFFIX_999 = ".999";

   private static final String IMAGE_DIR = "Merge";
   private static final String META_DIR  = "MISC";
   private static final String META_FILE = "AUTPRINT.MRK";

   private static SimpleDateFormat dateFormatKonica = new SimpleDateFormat("yyyy:MM:dd:HH:mm:ss");

// --- subclass hooks ---

   public String getShortName() { return Text.get(this,"s1"); }

   public int[] getAllowedCompletionModes() { return new int[] { COMPLETION_MODE_DETECT }; }
   public int   getCompletionMode(Object formatConfig) { return COMPLETION_MODE_DETECT; }
   public void  setCompletionMode(Object formatConfig, int mode) {}

// --- filename variation ---

   private static Variation konicaVariation = new PrefixVariation(0);
   private static Activity konicaActivity = new SubstringActivity(0,SUFFIX_CUT);

   // the activity is too broad ... as far as I know, the only suffixes that
   // occur are the initial "ord" and the final numeric ones.
   // maybe intermediate ones occur, but the main reason is that it's easier.

// --- format function ---

   public void format(Job job, Order order, Object formatConfig) throws Exception {

      KonicaConfig config = (KonicaConfig) formatConfig;
      // because of validation, the cast is guaranteed to work

      require(config.dataDir);

      // we have to validate ... fromIntNDigit will accept anything
      Convert.validateUsable(KonicaConfig.NDIGIT_ORDER_ID,order.orderID); // allow truncation

      File root = new File(config.dataDir,Convert.fromIntNDigit(KonicaConfig.NDIGIT_ORDER_ID,order.orderID) + SUFFIX_ORD);
      root = vary(root,konicaVariation,konicaActivity);
      LinkedList files = new LinkedList();

   // (1) plan the operation ; also check that mappings are defined

      LinkedList ops = new LinkedList();
      ops.add(new Op.Mkdir(root));

      files.add(IMAGE_DIR);
      ops.add(new Op.Mkdir(new File(root,IMAGE_DIR)));
      files.add(META_DIR);
      ops.add(new Op.Mkdir(new File(root,META_DIR)));

      String file = new File(META_DIR,META_FILE).getPath();
      files.add(file);
      ops.add(new KonicaGenerate(new File(root,file),job,order,config));

      HashSet skus = new HashSet();
      HashSet fset = new HashSet();

      Iterator i = job.refs.iterator();
      while (i.hasNext()) {
         Job.Ref ref = (Job.Ref) i.next();
         if (skus.add(ref.sku) && ! MappingUtil.existsMapping(config.mappings,ref.sku)) missingChannel(ref.sku,order.getFullID());

         Order.Item item = order.getItem(ref);

         if (fset.add(item.filename)) {
            file = new File(IMAGE_DIR,item.filename).getPath();
            files.add(file);
            ops.add(new Op.Copy(new File(root,file),order.getPath(item.filename)));
         }
      }

   // (2) alter files

      Op.transact(ops);

   // (3) alter object

      job.dir = root;
      job.files = files;
   }

// --- metafile generation ---

   private static class KonicaGenerate extends Op.Generate {

      private Job job;
      private Order order;
      private KonicaConfig config;

      public KonicaGenerate(File dest, Job job, Order order, KonicaConfig config) {
         super(dest);
         this.job = job;
         this.order = order;
         this.config = config;
      }

      public void subdo(Writer writer) throws IOException {

         Backprint.Sequence sequence = new Backprint.Sequence();
         boolean hasSequence = Backprint.hasSequence(config.backprints);

         String generateDate;
         synchronized (dateFormatKonica) {
            generateDate = dateFormatKonica.format(new Date());
         }

         writer.write("[HDR]" + line);
         writer.write("GEN REV = \"01.00\"" + line);
         writer.write("GEN CRT = \"LifePics LabCenter\"" + line);
         writer.write("GEN DTM = " + generateDate + line);
         writer.write(line);

         int pid = 1;

         Iterator i = job.refs.iterator();
         while (i.hasNext()) {
            Job.Ref ref = (Job.Ref) i.next();
            Order.Item item = getOrderItem(order,ref);

            KonicaMapping m = (KonicaMapping) MappingUtil.getMapping(config.mappings,item.sku);

            Iterator k = new ChunkIterator(job.getOutputQuantity(item,ref),KonicaConfig.CHUNK_STD,KonicaConfig.CHUNK_MAX,hasSequence);
            while (k.hasNext()) {
               int quantity = ((Integer) k.next()).intValue();

               // these shouldn't happen, this is just for thoroughness
               try {
                  Convert.validateNDigit(KonicaConfig.NDIGIT_PID,pid);
                  Convert.validateNDigit(KonicaConfig.NDIGIT_QUANTITY,quantity); // should always pass now, but check anyway
               } catch (ValidationException e) {
                  throw (IOException) new IOException(Text.get(FormatKonica.class,"e2")).initCause(e);
               }

               writer.write("[JOB]" + line);
               writer.write("PRT PID = " + Convert.fromIntNDigit(KonicaConfig.NDIGIT_PID,pid++) + line);
               writer.write("PRT TYP = STD" + line);
               writer.write("PRT QTY = " + Convert.fromIntNDigit(KonicaConfig.NDIGIT_QUANTITY,quantity) + line);
               writer.write("<IMG SRC = \"../" + IMAGE_DIR + "/" + item.filename + "\">" + line);
               writer.write("VUQ RGN = BGN" + line);
               writer.write("VUQ VNM = \"Konica QD-21\" -ATR \"PrinterCommand V1.00\"" + line);
               writer.write("\tPRT PSL=NML -PCN \"" + m.channel + "\" -LAYOUT END" + line);
               writer.write("\tIMG CFG=-FIT WINDOW -WINDOW 0 -LAYOUT END" + line);

               if (config.backprints.size() > 0) {
                  Backprint b = (Backprint) config.backprints.getFirst();
                  String s = b.generate(order,item,sequence);
                  if (s != null && s.length() > 0) {
                     writer.write("\tPRT BAK=\"" + s + "\"" + line);
                  }
               }

               writer.write("VUQ RGN = END" + line);
               writer.write(line);

               sequence.advance(item);
            }
         }
      }
   }

// --- completion helper ---

   private static class AlternateFilter implements FileFilter {

      private String prefix;
      private int len;
      public AlternateFilter(String prefix) { this.prefix = prefix; this.len = prefix.length(); }

      public boolean accept(File file) {
         if ( ! file.isDirectory() ) return false;

         String name = file.getName();
         return (    name.startsWith(prefix)
                  && name.length() == len + 4
                  && name.charAt(len) == '.'
                  && isAsciiDigit(name.charAt(len+1))
                  && isAsciiDigit(name.charAt(len+2))
                  && isAsciiDigit(name.charAt(len+3)) );
      }
   }

   private static boolean isAsciiDigit(char c) { return (c >= '0' && c <= '9'); }

// --- completion ---

   // the Konica doesn't always rename to the same suffix.
   //
   //    999 - manual completion
   //    000 - cancellation
   //    anything in between - automatic completion (with that job number)
   //
   // so, if the order dir is gone, we look for a unique alternate.

   private String transform(String s) {
      return s.endsWith(SUFFIX_ORD) ? s.substring(0,s.length()-SUFFIX_LENGTH) : null;
   }

   protected File getComplete(File dir, String property) {

      String name = transform(dir.getName());
      if (name == null) return null; // don't know how to mark

      return new File(dir.getParentFile(),name + SUFFIX_999);
   }

   public File isComplete(File dir, String property) {

      if (dir.exists()) return null; // still there, not complete

      String name = transform(dir.getName());
      if (name == null) return null; // don't know how to mark

      File[] file = dir.getParentFile().listFiles(new AlternateFilter(name));
      if (file == null || file.length != 1) return null; // don't guess if ambiguous

      return file[0];
   }

   public void makeComplete(Order order) {
      order.orderDir = makeComplete(order.orderDir,null);
   }

}

