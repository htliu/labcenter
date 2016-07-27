/*
 * FormatDP2.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.misc.Op;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Implementation of the DP2 format.
 */

public class FormatDP2 extends Format {

// --- constants ---

   private static final String SUFFIX_TXT = ".txt";
   private static final String NOTE_FILE = "note.txt";

   private static SimpleDateFormat dateFormat = new SimpleDateFormat("M/d/yyyy");

   private static final int LENGTH_LIMIT = 26;
   // what we really want is to limit the total length to 31, but the LengthLimit class
   // only works on the prefix part; so, assume the suffix won't be longer than ".jpeg".

// --- subclass hooks ---

   public String getShortName() { return Text.get(this,"s1"); }

   public int[] getAllowedCompletionModes() { return new int[] { COMPLETION_MODE_MANUAL, COMPLETION_MODE_AUTO }; }
   public int   getCompletionMode(Object formatConfig) { return ((DP2Config) formatConfig).completeImmediately ? COMPLETION_MODE_AUTO : COMPLETION_MODE_MANUAL; }
   public void  setCompletionMode(Object formatConfig, int mode) { ((DP2Config) formatConfig).completeImmediately = (mode == COMPLETION_MODE_AUTO); }

// --- format function ---

   public void format(Job job, Order order, Object formatConfig) throws Exception {

      DP2Config config = (DP2Config) formatConfig;
      // because of validation, the cast is guaranteed to work

      require(config.requestDir);
      require(config.imageDir);

   // (0) pick DP2 order ID

      File f = new File(config.requestDir,config.prefix + Convert.fromInt(order.orderID));
      f = vary(f,new OrActivity(new SuffixedActivity(SUFFIX_TXT),
                                new RelocatedActivity(config.imageDir)));

      String orderID = f.getName();

      File genr = new File(config.requestDir,orderID + SUFFIX_TXT);
      File root = new File(config.imageDir,  orderID);

   // (1) plan the operation ; also check that mappings are defined

      LinkedList ops = new LinkedList();
      ops.add(new Op.Mkdir(root));

      HashSet skus = new HashSet();
      HashSet fset = new HashSet();

      LinkedList files = new LinkedList(); // could just use fset instead, but I like order

      LengthLimit lengthLimit = new LengthLimit(true,LENGTH_LIMIT,/* exclude = */ true);
      // this is what prevents tildes from coming through in the file names

      Iterator i = job.refs.iterator();
      while (i.hasNext()) {
         Job.Ref ref = (Job.Ref) i.next();

         if (skus.add(ref.sku) && ! MappingUtil.existsMapping(config.mappings,ref.sku)) missingChannel(ref.sku,order.getFullID());

         if (fset.add(ref.filename)) {
            files.add(ref.filename);
            ops.add(new Op.Copy(new File(root,lengthLimit.transform(ref.filename)),order.getPath(ref.filename)));
         }
      }

      Detail detail = config.customIntegration ? (Detail) new CustomDetail()
                                               : (Detail) new NormalDetail();

      ops.add(new GenerateCommand(genr,job,order,config,orderID,files,lengthLimit,detail));
      // must come after file copy, since DP2 will run the commands as soon as the file closes

      File note = new File(config.imageDir,NOTE_FILE);
      if ( ! note.exists() ) ops.add(new GenerateNote(note)); // see comment right below

   // (2) alter files

      Op.transact(ops);

   // (3) alter object

      if (config.completeImmediately) job.property = ""; // else it stays null

      // no other changes to object, because the files aren't owned.
      // the command file is deleted by DP2 after it's processed.
      // the image files are semi-owned by DP2 ... when you delete
      // an order, it asks if you also want to delete the images,
      // and if so, it does, and also deletes the directory they're in.
      // in fact, it even tries to delete the parent directory, but we
      // prevent that by putting a file note.txt there.
   }

// --- command file generation ---

   private static class GenerateCommand extends Op.Generate {

      private Job job;
      private Order order;
      private DP2Config config;
      private String orderID;
      private LinkedList files;
      private LengthLimit lengthLimit;
      private Detail detail;

      public GenerateCommand(File dest, Job job, Order order, DP2Config config, String orderID, LinkedList files, LengthLimit lengthLimit, Detail detail) {
         super(dest);
         this.job = job;
         this.order = order;
         this.config = config;
         this.orderID = orderID;
         this.files = files;
         this.lengthLimit = lengthLimit;
         this.detail = detail;
      }

      public void subdo(Writer writer) throws IOException {

         // we know the config fields don't have tildes, because we validate for that,
         // so we only have to detilde fields that are genuinely data-driven.
         // we also don't have to check the lengths of the config fields, because the
         // user will be entering them to match things that are already in DP2.

         writer.write("Include: " + config.includeFile + ";" + line);
         writer.write(line);

         writer.write("OrderID = ~" + orderID + "~;" + line);
         writer.write(line);

         writer.write("Base = ~<OrderID>~;" + line);
         writer.write("Next = 2;" + line);
         writer.write("while (TRUE)" + line);
         writer.write("{" + line);
         writer.write("   if ( ! OrderExists(~<OrderID>~) ) break;" + line);
         writer.write("   OrderID = ~<Base>_n<Next++>~;" + line);
         writer.write("}" + line);
         writer.write(line);
         // this loop should really almost never fire ... if there's an order
         // with the same ID in DP2, there should also be a folder of images
         // with the same name, and so we would have advanced to the next name
         // during the file name variation.  still, this is easy and very safe.
         // I used "_n" instead of "_r" here so that we could tell where the
         // variation was coming from, and also match it up to the right folder.
         //
         // technical note: the idea here is to say "while (OrderExists(...))",
         // but the language doesn't allow user-defined functions in that spot.

         File mapImageDir = (config.mapImageDir != null) ? config.mapImageDir : config.imageDir;
         File mapRoot = new File(mapImageDir,orderID);
         String date = (order.invoiceDate == null) ? "" : dateFormat.format(order.invoiceDate);

         detail.doOrder(writer,line,order,date,config);
         writer.write(line);

         Iterator i = files.iterator();
         while (i.hasNext()) {
            String filename = (String) i.next();

            String frame = lengthLimit.transform(filename);
            String framePath = Convert.fromFile(new File(mapRoot,frame));

            detail.doImage(writer,line,config.rollName,frame,framePath);
         }
         writer.write(line);
         // in addition to limiting the length, the LengthLimit object also prevents tildes.
         // normally I wouldn't transform the displayed version of the filename,
         // but here the problem is with the command file quote system, not the file system.

         i = job.refs.iterator();
         while (i.hasNext()) {
            Job.Ref ref = (Job.Ref) i.next();
            Order.Item item = getOrderItem(order,ref);

            DP2Mapping m = (DP2Mapping) MappingUtil.getMapping(config.mappings,item.sku);

            String frame = lengthLimit.transform(item.filename);
            String framePath = Convert.fromFile(new File(mapRoot,frame));
            String quantity = Convert.fromInt(job.getOutputQuantity(item,ref));

            detail.doProduct(writer,line,frame,framePath,quantity,m);
         }
         writer.write(line);
      }
   }

   private static String detilde(String s) {
      return s.replace('~','-');
      // we need this because tilde is the quotation mark for DP2
   }

   private static String denull(String s) {
      return (s == null) ? "" : s;
   }

   private static String truncate(int len, String s) {
      return (s.length() <= len) ? s : s.substring(0,len);
   }

   private static String getCustomerID(Order order, DP2Config config) {
      return config.enableRealID ? config.prefixRealID + detilde(denull(order.lpCustomerID))
                                 : config.customerID;
   }

// --- command file detail ---

   private interface Detail {

      void doOrder  (Writer writer, String line,
                     Order order,
                     String date,
                     DP2Config config) throws IOException;

      void doImage  (Writer writer, String line,
                     String rollName,
                     String frame,
                     String framePath) throws IOException;

      void doProduct(Writer writer, String line,
                     String frame,
                     String framePath,
                     String quantity,
                     DP2Mapping m) throws IOException;
   }

   private static class NormalDetail implements Detail {

      public void doOrder(Writer writer, String line, Order order, String date, DP2Config config) throws IOException {
         writer.write("AddOrder(~<OrderID>~,~"
            + getCustomerID(order,config) + "~,~~,~~,~~,~"
            + config.status + "~,~~,~"
            + date + "~,~~,~~,~"
            + detilde(denull(order.specialInstructions)) + "~);" + line);
         writer.write("UpdateOrder(~<OrderID>~,OrderName,~"
            + truncate(60,detilde(order.getFullName())) + "~);" + line);
         // other fields have length limits, too, but they're not data-driven
      }

      public void doImage(Writer writer, String line, String rollName, String frame, String framePath) throws IOException {
         writer.write("AddImage(~<OrderID>~,~"
            + rollName + "~,~"
            + frame + "~,~"
            + framePath + "~);" + line);
      }

      public void doProduct(Writer writer, String line, String frame, String framePath, String quantity, DP2Mapping m) throws IOException {

         String clauses = "";
         if (m.surface    != null) clauses += ",~Keyword.Saveimage.Oemprintersurface~,~" + Convert.fromInt(m.surface.intValue()) + "~";
         if (m.paperWidth != null) clauses += ",~Keyword.Saveimage.Paperwidth~,~" + m.paperWidth + "~";
         if (m.autoCrop   != null) clauses += ",~Node.1.Autocropmode~,~" + m.autoCrop + "~";

         writer.write("CreateOrderItemAndJob(~<OrderID>~,~~,~~,~"
            + m.product + "~,~"
            + quantity + "~,Image,~"
            + framePath + "~"
            + clauses + ");" + line);
      }
   }

   private static class CustomDetail implements Detail {

      public void doOrder(Writer writer, String line, Order order, String date, DP2Config config) throws IOException {
         writer.write("LabCenterOrder(~<OrderID>~,~"
            + getCustomerID(order,config) + "~,~"
            + config.customerType + "~,~"
            + config.status + "~,~"
            + date + "~,~"

            + detilde(order.nameFirst) + "~,~"
            + detilde(order.nameLast) + "~,~"
            + detilde(order.email) + "~,~"
            + detilde(order.phone) + "~,~"

            + detilde(denull(order.company)) + "~,~"
            + detilde(denull(order.street1)) + "~,~"
            + detilde(denull(order.street2)) + "~,~"
            + detilde(denull(order.city)) + "~,~"
            + detilde(denull(order.state)) + "~,~"
            + detilde(denull(order.zipCode)) + "~,~"
            + detilde(denull(order.country)) + "~,~"

            + (config.includeShipCompany ? detilde(denull(order.shipCompany)) + "~,~" : "")
            + detilde(denull(order.shipStreet1)) + "~,~"
            + detilde(denull(order.shipStreet2)) + "~,~"
            + detilde(denull(order.shipCity)) + "~,~"
            + detilde(denull(order.shipState)) + "~,~"
            + detilde(denull(order.shipZipCode)) + "~,~"
            + detilde(denull(order.shipCountry)) + "~,~"

            + detilde(denull(order.specialInstructions)) + "~);" + line);
      }

      public void doImage(Writer writer, String line, String rollName, String frame, String framePath) throws IOException {
         writer.write("LabCenterImage(~<OrderID>~,~"
            + rollName + "~,~"
            + frame + "~,~"
            + framePath + "~);" + line);
      }

      public void doProduct(Writer writer, String line, String frame, String framePath, String quantity, DP2Mapping m) throws IOException {
         String surface = (m.surface == null) ? "" : Convert.fromInt(m.surface.intValue());
         writer.write("LabCenterProduct(~<OrderID>~,~"
            + frame + "~,~"
            + framePath + "~,~"
            + m.product + "~,~"
            + quantity + "~,~"
            + surface + "~,~" // it's an Integer, so can't just use denull
            + denull(m.paperWidth) + "~,~"
            + denull(m.autoCrop) + "~);" + line);
      }
   }

// --- note file generation ---

   private static class GenerateNote extends Op.Generate {

      public GenerateNote(File dest) { super(dest); }

      public void subdo(Writer writer) throws IOException {
         for (int i=1; i<=2; i++) {
            writer.write(Text.get(FormatDP2.class,"s2_" + Convert.fromInt(i)) + line);
         }
      }
   }

// --- completion ---

   public boolean isCompleteWithoutDirectory(String property, Special special) throws Exception {
      return (property != null);
      // not null means job should complete immediately
   }

}

