/*
 * FormatFuji.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.misc.FileMapper;
import com.lifepics.neuron.misc.FileUtil;
import com.lifepics.neuron.misc.Op;
import com.lifepics.neuron.misc.Resource;
import com.lifepics.neuron.object.Copyable;
import com.lifepics.neuron.object.CopyUtil;
import com.lifepics.neuron.struct.SKU;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.TimeZone;

/**
 * Implementation of the Fuji format.
 */

public class FormatFuji extends Format {

// --- constants ---

   private static final String PREFIX_LP  = "LP";
   private static final String SUFFIX_TXT = ".txt";

   private static SimpleDateFormat dateFormat1 = new SimpleDateFormat("yyMMdd");
   private static SimpleDateFormat dateFormat2 = new SimpleDateFormat("MM/dd/yyyy");

// --- subclass hooks ---

   public String getShortName() { return Text.get(this,"s1"); }

   public int[] getAllowedCompletionModes() { return new int[] { COMPLETION_MODE_MANUAL }; }
   public int   getCompletionMode(Object formatConfig) { return COMPLETION_MODE_MANUAL; }
   public void  setCompletionMode(Object formatConfig, int mode) {}

// --- format function ---

   public void format(Job job, Order order, Object formatConfig) throws Exception {

      FujiConfig config = (FujiConfig) formatConfig;
      // because of validation, the cast is guaranteed to work

      requireFujiRequest(config.requestDir);
      if (config.imageDir != null) require(config.imageDir);

   // (1) first stage of planning, sort by SKU
   //     we could do this just like Noritsu, at the job level,
   //     except we want files with sequential numbers per SKU

      HashMap skuMap = new HashMap();

      Iterator i = job.refs.iterator();
      while (i.hasNext()) {
         Job.Ref ref = (Job.Ref) i.next();
         SKU key = ref.sku;

         LinkedList refs = (LinkedList) skuMap.get(key);
         if (refs == null) {
            refs = new LinkedList();
            skuMap.put(key,refs);
         }

         refs.add(ref);
      }

   // (2) second stage of planning, create ops to copy images (if applicable)

      // there are two modes of operation here, the old one where imageDir is null,
      // and the new one where it's not.  in the old mode, we refer to the images
      // in their location in the order directory, and map the root of that file system
      // to mapImageDir.  this is not ideal because mapping the root forces you to
      // share a top-level folder, and some versions of Windows don't let you share
      // the Program Files directory.  in the new mode, we copy the images to imageDir,
      // refer to them there, and map imageDir to mapImageDir.  this is ideal!

      LinkedList opsCopy = new LinkedList();

      // these should not be referenced unless imageDir is defined
      File root = null;
      LinkedList files = new LinkedList();
      LengthLimit lengthLimit = new LengthLimit(config.limitEnable,config.limitLength);

      if (config.imageDir != null) {
         root = new File(config.imageDir,Convert.fromInt(order.orderID));
         root = vary(root);
         opsCopy.add(new Op.Mkdir(root));

         HashSet fset = new HashSet();

         i = job.refs.iterator(); // re-iterate, code is clearer this way
         while (i.hasNext()) {
            Job.Ref ref = (Job.Ref) i.next();
            Order.Item item = order.getItem(ref);

            if (fset.add(item.filename)) {
               String file = lengthLimit.transform(item.filename);
               files.add(file);
               opsCopy.add(new Op.Copy(new File(root,file),order.getPath(item.filename)));
            }
         }
      }

   // (3) third stage of planning, check mappings and make operation objects

      LinkedList opsMake = new LinkedList();
      Record r = new Record();

      String fileDate;
      synchronized (dateFormat1) {
         fileDate = dateFormat1.format(new Date());
      }
      // make same for all files, even if we cross midnight during generation

      // per-job fields
      r.orderID_external = Convert.fromIntNDigit(FujiConfig.NDIGIT_ORDER_ID,order.orderID);
      r.customer = order.getFullName();
      // note, NDIGIT_ORDER_ID is small, so the order ID is truncated
      //
      r.mapper = new FileMapper();
      if (config.mapRequestDir != null) {
         r.mapper.entries.add(new FileMapper.Entry(config.requestDir,config.mapRequestDir));
      }
      if (config.mapImageDir != null) {
         File temp = (config.imageDir != null) ? config.imageDir : FileMapper.getRoot(order.orderDir);
         r.mapper.entries.add(new FileMapper.Entry(temp,config.mapImageDir));
      }
      // note, if imageDir is null, the mapping order is important,
      // since dataDir will often be contained in getRoot(orderDir)

      String stub = Convert.fromIntNDigit(   FujiConfig.NDIGIT_ORDER_ID
                                           - FujiConfig.NDIGIT_SKU_COUNT, order.orderID);
      checkCollision(config.requestDir,PREFIX_LP + stub);

      int skuCount = 0;

      i = skuMap.entrySet().iterator();
      while (i.hasNext()) {
         Map.Entry entry = (Map.Entry) i.next();

         SKU sku = (SKU) entry.getKey();
         LinkedList refs = (LinkedList) entry.getValue();

      // check some things

         FujiMapping m = (FujiMapping) MappingUtil.getMapping(config.mappings,sku);
         if (m == null) missingChannel(sku,order.getFullID());

         // the order ID can be truncated, because we won't reuse it until many orders later,
         // but the SKU has to be validated, because we'll generate the files simultaneously.
         // if you have more than ten SKUs in a job, tough.
         try {
            Convert.validateNDigit(FujiConfig.NDIGIT_SKU_COUNT,skuCount);
         } catch (ValidationException e) {
            throw (IOException) new IOException(Text.get(this,"e2")).initCause(e);
         }

      // fill in record fields

         // per-SKU fields
         r.orderID_internal = stub + Convert.fromIntNDigit(FujiConfig.NDIGIT_SKU_COUNT,skuCount++);
         r.product = m.product;
         r.printCode = m.printCode;
         r.totalQuantity = totalQuantity(job,order,refs);

      // make operation objects

         int fileCount = 1;

         Iterator j = refs.iterator();
         while (j.hasNext()) {
            Job.Ref ref = (Job.Ref) j.next();
            Order.Item item = order.getItem(ref);

            // per-file fields
            r.dest = new File(config.requestDir,fileDate + PREFIX_LP + r.orderID_internal + "_" + Convert.fromInt(fileCount++) + SUFFIX_TXT);
            r.image = (config.imageDir != null) ? new File(root,lengthLimit.transform(item.filename)) : order.getPath(item.filename);
            r.quantity = job.getOutputQuantity(item,ref);
            r.last = ! j.hasNext();

            opsMake.add(new FujiGenerate(r));
         }
      }

   // (4) alter files

      LinkedList ops = new LinkedList();
      ops.addAll(opsCopy);
      ops.addAll(opsMake);
      // Fuji will process generated files as soon as they appear,
      // so the copy operations have to come before the make ones.

      Op.transact(ops);

   // (5) alter object

      if (config.imageDir != null) {
         job.dir = root;
         job.files = files;
      }
   }

// --- metafile record ---

   // copyable record to make both parts of the process easy ...
   // filling in the records in a loop, then executing later.

   private static class Record implements Copyable {

      public Object clone() throws CloneNotSupportedException { return super.clone(); }
      public Record copy() { return (Record) CopyUtil.copy(this); }

   // --- per-job fields ---

      public String orderID_external; // doesn't include PREFIX_LP
      public String customer;
      public FileMapper mapper;

      // note, mapping must occur within the generation function,
      // because we need both mapped and unmapped forms of dest.

   // --- per-SKU fields ---

      public String orderID_internal; // doesn't include PREFIX_LP
      public String product;
      public String printCode;
      public int totalQuantity;

   // --- per-file fields ---

      public File dest;
      public File image;
      public int quantity;
      public boolean last;
   }

// --- metafile generation ---

   // unlike other generation code, here the information in the job, order, and config objects
   // has already been transferred into a single record containing all the necessary fields.

   private static class FujiGenerate extends Op.Generate {

      private Record r;
      public FujiGenerate(Record r) { super(r.dest); this.r = r.copy(); }

      public void subdo(Writer writer) throws IOException {

         // organize into blocks by whether the text is constant

         writer.write("REQUEST_TYPE=rendernow" + line);
         writer.write("BATCH_ID=AnyBatch" + line);
         writer.write("RECORD_COUNT=1" + line);
         writer.write("TYPE=photograph" + line);
         writer.write("REQUESTER=KIOSK;PrintCmd" + line);
         writer.write("TITLE=Frontier:DPC" + line);
         writer.write(line);
         writer.write("RENDER_ID=" + line);

         String imageString = Convert.fromFile(r.mapper.map(r.image));
         String totalString = Convert.fromInt(r.totalQuantity);

         writer.write("LOCATION=*" + imageString + line);

         writer.write(blob());

         writer.write("Var=OrderID:\"" + PREFIX_LP + r.orderID_internal + "\"" + line);
         writer.write("Var=Folio:\"" + r.customer + "\"" + line);
         writer.write("Var=Receipt Order:\"" + PREFIX_LP + r.orderID_external + "\"" + line);
         writer.write("Var=Total:" + totalString + line);
         writer.write("Var=Product:\"" + r.product + "=" + totalString + "\"" + line);

         writer.write("Var=Price:\"0.00\"" + line);

         writer.write("Var=printCode:\"" + r.printCode + "\"" + line);
         writer.write("Var=%filename[0]:\"" + imageString + "\"" + line);
         writer.write("Var=Quantity:" + Convert.fromInt(r.quantity) + line);
         writer.write("Var=ImageLocation:\"" + imageString + "\"" + line);

         writer.write("Var=Landscape:0" + line);

         writer.write("Var=OrderDate:\"" + getOrderDate(new Date()) + r.orderID_internal + "\"" + line);

         writer.write("Var=Laminate:0" + line);
         writer.write("Var=Copies:1" + line);
         writer.write("Var=Zoom[0]:0" + line);
         writer.write("Var=Xoffset[0]:0" + line);
         writer.write("Var=Yoffset[0]:0" + line);
         writer.write("Var=SepiaPage[0]:0" + line);
         writer.write("Var=Vignette[0]:0" + line);
         writer.write("Var=R[0]:0" + line);
         writer.write("Var=G[0]:0" + line);
         writer.write("Var=B[0]:0" + line);
         writer.write("Var=ImageIndex[0]:\"0,0\"" + line);

         if (r.last) {
            writer.write("Var=EndOrder:\"" + PREFIX_LP + r.orderID_internal + "\"" + line);
         }
         writer.write("Var=%requestFileName:\"" + removeSuffix(Convert.fromFile(r.mapper.map(r.dest)),SUFFIX_TXT) + "\"" + line);

         writer.write("Var=Thumbnail:\"c:\\agt\\nofile.jpg\"" + line);
         writer.write("Var=psFile:\"datafiles\\printsizes.txt\"" + line);
         writer.write("Var=ps:\"*!*!*FileError*!*!*\"" + line);
         writer.write("Var=%LayoutName:\"Frontier\"" + line);
         writer.write("Var=%GroupImages:1" + line);
      }
   }

   private static String removeSuffix(String s, String suffix) {
      return s.endsWith(suffix) ? s.substring(0,s.length()-suffix.length()) : s;
   }

// --- date utility function ---

   private static TimeZone gmt = TimeZone.getTimeZone("GMT");
   // actual time zone doesn't matter,
   // what matters is that it doesn't use daylight savings.

   public static String getOrderDate(Date date) {

   // first part, string date

      String s;
      synchronized (dateFormat2) {
         s = dateFormat2.format(date);
      }

      // as far as I can tell, DateFormat doesn't support space-padding,
      // so just poke them in in the two places where they'll be needed.

      if (s.length() >= 1 && s.charAt(0) == '0') s =                    ' ' + s.substring(1);
      if (s.length() >= 4 && s.charAt(3) == '0') s = s.substring(0,3) + ' ' + s.substring(4);

   // second part, numeric date

      // the date conversion functions all use the local time zone.
      // normally that's fine, but here it's a problem.
      // we want to know how far apart two calendar dates are.
      // but we can't just subtract and divide by milliseconds/day
      // because we get a +/- 1 hour offset if the two days are in
      // different phases of daylight savings time.

      Calendar calendar = Calendar.getInstance();
      calendar.setTime(date);

      int year  = calendar.get(Calendar.YEAR);
      int month = calendar.get(Calendar.MONTH); // zero-based
      int day   = calendar.get(Calendar.DAY_OF_MONTH);

      // now that we've got the date field values, change time zones and compute

      calendar.setTimeZone(gmt); // not affected by clear
      calendar.clear();

      calendar.set(year,month,day);
      long delta = calendar.getTimeInMillis() / 86400000; // division will be exact

      delta += 2; // to agree with sample Fuji output I have

   // third part is added by caller

      return s + ";" + Convert.fromLong(delta);
   }

// --- blob utility functions ---

   private static String blobString = null;

   private static String blob() {
      if (blobString == null) blobString = blob("FormatFuji.txt");
      return blobString;
   }

   private static String blob(String name) {

      InputStream src = Resource.getResourceAsStream(FormatFuji.class,name);
      ByteArrayOutputStream dest = new ByteArrayOutputStream();

      try {
         FileUtil.copy(dest,src);
      } catch (IOException e) {
         throw (MissingResourceException) new MissingResourceException("",FormatFuji.class.getName(),name).initCause(e);
      }

      return dest.toString();
   }

// --- collision detection ---

   // the files we send to the Fuji don't exist independently,
   // they're tied together by the internal order ID,
   // and also to a lesser extent by the receipt order ID.
   //
   // so, even if it's possible to create all the files we need to create,
   // we shouldn't do it if there are existing requests with the same IDs.
   // unfortunately, we can't check that ... Fuji deletes the files
   // right away, but could still be working on printing them, internally.
   //
   // however, we can at least make sure that the files are pulled out of
   // the directory in the right order ... all the end-order ones must be
   // gone before we create any new ones.  that should help some.

   // the filter could be narrower, but that shouldn't matter.

   private static class CollisionFilter implements FileFilter {

      private String infix;
      public CollisionFilter(String infix) { this.infix = infix; }

      public boolean accept(File file) {
         if (file.isDirectory()) return false; // just for good form
         return (file.getName().indexOf(infix) != -1);
      }
   }

   private static void checkCollision(File dir, String infix) throws IOException {

      File[] file = dir.listFiles(new CollisionFilter(infix));

      if (file == null || file.length == 0) ; // ok -- note either case is possible
      else throw new IOException(Text.get(FormatFuji.class,"e3",new Object[] { infix }));
   }

}

