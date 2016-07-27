/*
 * FormatFujiNew.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.misc.FileMapper;
import com.lifepics.neuron.misc.Op;
import com.lifepics.neuron.struct.EnumeratedType;
import com.lifepics.neuron.thread.ThreadStopException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;
import java.sql.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Implementation of the Fuji PIC 2.6 format.
 */

public class FormatFujiNew extends Format {

// --- constants ---

   private static final String SUFFIX_TXT = ".txt";

// --- subclass hooks ---

   public String getShortName() { return Text.get(this,"s1"); }

   public int[] getAllowedCompletionModes() { return new int[] { COMPLETION_MODE_MANUAL, COMPLETION_MODE_DETECT }; }
   public int   getCompletionMode(Object formatConfig) { return ((FujiNewConfig) formatConfig).enableCompletion ? COMPLETION_MODE_DETECT : COMPLETION_MODE_MANUAL; }
   public void  setCompletionMode(Object formatConfig, int mode) { ((FujiNewConfig) formatConfig).enableCompletion = (mode == COMPLETION_MODE_DETECT); }

// --- format function ---

   public void format(Job job, Order order, Object formatConfig) throws Exception {

      FujiNewConfig config = (FujiNewConfig) formatConfig;
      // because of validation, the cast is guaranteed to work

      requireFujiRequest(config.requestDir);
      require(config.imageDir);

   // (0) set up stuff

      int len1 = config.prefix.length();
      int len2 = 7 - len1;

      // we have to validate ... fromIntNDigit will accept anything
      Convert.validateUsable(len2,order.orderID); // allow truncation

      File genr, root;
      String base = config.prefix + Convert.fromIntNDigit(len2,order.orderID);

      if ( ! config.proMode ) { // uncoupled variation

         genr = new File(config.requestDir,base + SUFFIX_TXT);
         genr = vary(genr,new PrefixVariation(len1));

         root = new File(config.imageDir,Convert.fromInt(order.orderID));
         root = vary(root);

      } else { // coupled variation since request file name must match dir

         File f = new File(config.requestDir,base);
         f = vary(f,new PrefixVariation(len1),
                    new OrActivity(new SuffixedActivity(SUFFIX_TXT),
                                   new RelocatedActivity(config.imageDir)));

         genr = new File(config.requestDir,f.getName() + SUFFIX_TXT);
         root = new File(config.imageDir,  f.getName());

         // this is technically correct, but that's not why I did it.
         // it's tempting to compute genr just as before,
         // then derive root from it and throw an error if it exists,
         // but I suspect that would fail fairly often ...
         // probably the root directories exist longer than the request files.
      }

      LinkedList files = new LinkedList();
      LengthLimit lengthLimit;
      if (config.limitEnable) {
         lengthLimit = new LengthLimit(true,config.limitLength,/* exclude = */ true);
      } else {
         lengthLimit = new LengthLimit(true,NO_LIMIT,/* exclude = */ false);
         // in this case, we don't want actual length limits,
         // but we still need the code to run so that it splits the names.
      }

   // compute the normal set

      // stale comments, see (*) for the current situation with CDs
      //
      // usually, the special SKUs will be covered by the normal ones,
      // so all the images will get sent and be available for the CD,
      // and there's no need to make duplicate copies of all of them.
      // however, if you're reprinting just the CD, or doing something
      // else weird like that, you might get a file with only a CD SKU,
      // and in that case you do need to copy the image.
      // also that's the case where we generate a neg without a unit.

      HashSet normalSet = new HashSet();

      Iterator i = job.refs.iterator();
      while (i.hasNext()) {
         Job.Ref ref = (Job.Ref) i.next();
         if ( ! ref.sku.equals(order.specialSKU) ) normalSet.add(ref.filename);
      }
      // note, transformed filenames have nothing to do with this

   // (1) plan the operation ; also check that mappings are defined

      LinkedList ops = new LinkedList();
      ops.add(new Op.Mkdir(root));

      HashSet skus = new HashSet();
      HashSet printCodes = new HashSet();
      int llseq = 0;

      i = job.refs.iterator();
      while (i.hasNext()) {
         Job.Ref ref = (Job.Ref) i.next();

         boolean cd = ref.sku.equals(order.specialSKU);

         if (cd && normalSet.contains(ref.filename)) continue;

         if (skus.add(ref.sku) && ! cd) {

            FujiNewMapping m = (FujiNewMapping) MappingUtil.getMapping(config.mappings,ref.sku);
            if (m == null) missingChannel(ref.sku,order.getFullID());

            printCodes.add(m.printCode);
         }

         // the generated file sometimes didn't work with multiple units per neg block,
         // so we had to make multiple neg blocks ... but now it almost never works
         // with duplicate filenames on different neg blocks.  so, we have to make multiple
         // copies of the same file.  that means, no hash map to exclude duplicates,
         // and use of an integer key with the lengthLimit object so that it splits names.

         String file = lengthLimit.transform(new Integer(llseq++),ref.filename);
         files.add(file);
         ops.add(new Op.Copy(new File(root,file),order.getPath(ref.filename)));
      }

      // Fuji will process generated files as soon as they appear,
      // so the copy operations have to come before the make ones.

      Op opGen = new FujiNewGenerate(genr,root,lengthLimit,job,order,config,normalSet);
      if ( ! config.proMode ) {
         ops.add(opGen);
      } else {
         if (config.proModeWaitInterval > 0) ops.addFirst(new OpWait(config.proModeWaitInterval));
         ops.addFirst(opGen);
         // so generate comes first, wait second
      }
      // note that genr is not added to the files list, because
      // (a) it's not in the same directory, like it needs to be
      // (b) the Fuji side deletes it as soon as it processes it

   // (2) alter files

      Op.transact(ops);

   // (3) alter object

      job.dir = root;
      job.files = files;

      // non-null property means we check for completion,
      // and the value tells what order ID to use.
      if (config.enableCompletion) job.property = getProperty(config.completionType,getFujiOrderID(genr),job,order,printCodes);
   }

// --- wait op ---

   private static class OpWait extends Op {

      private long interval;
      public OpWait(long interval) { this.interval = interval; }

      public void dodo() throws IOException {
         try {
            Thread.sleep(interval);
         } catch (InterruptedException e) {
            // won't happen
         }
         // note, this is not a nice (interruptible) wait!
         // the file copy operations themselves are not interruptible,
         // and take some time; the time added by the wait should not
         // be too much longer than that.
         //
         // also, it's not clear what we could do after an interrupt.
         // try to roll back, even though the lab has probably already
         // read and deleted the control file?  go ahead and copy the
         // image files, even though it's too soon?  error out the job?
      }

      public void undo() {}
   }

// --- metafile generation ---

   private static String getFujiOrderID(File dest) {
      // cutting the suffix is icky, but the name may have been varied
      String orderID = dest.getName();
      orderID = orderID.substring(0,orderID.length()-SUFFIX_TXT.length());
      return orderID;
   }

   private static class FujiNewGenerate extends Op.Generate {

      private File root;
      private LengthLimit lengthLimit;
      private Job job;
      private Order order;
      private FujiNewConfig config;
      private HashSet normalSet;

      public FujiNewGenerate(File dest, File root, LengthLimit lengthLimit, Job job, Order order, FujiNewConfig config, HashSet normalSet) {
         super(dest);
         this.root = root;
         this.lengthLimit = lengthLimit;
         this.job = job;
         this.order = order;
         this.config = config;
         this.normalSet = normalSet;
      }

      public void subdo(Writer writer) throws IOException {

         FileMapper mapper = new FileMapper();
         mapper.entries.add(new FileMapper.Entry(config.imageDir,config.mapImageDir));

         String orderID = getFujiOrderID(dest);

         writer.write("[Order]" + line);
         writer.write("OrderId=" + orderID + line);
         writer.write("CustomerName=" + f(order.nameLast) + f(order.nameFirst) + f(/* middle = */ "") + line);
         writer.write("Address=" + f(order.shipStreet1) + f(order.shipStreet2)
                                 + f(order.shipCity)    + f(order.shipState)
                                 + f(order.shipZipCode) + f(order.shipCountry) + line);
         writer.write("Phone=" + order.phone + line);
         writer.write("Email=" + order.email + line);
         writer.write("Rush=0" + line);
         if (config.useOrderSource) writer.write("Source=" + config.orderSource + line);
         if (config.useOrderTypeIn) writer.write("TypeIn=" + config.orderTypeIn + line);

         // we heard from a Fuji technician that a normal PIC Pro order file
         // doesn't have Address, Phone, Email, Rush, Source, or Optimize.
         // not sure if the example ve saw included a TypeIn.  I'm not excited
         // to filter them, though, because all it can do is break things.

      // CD product

         int quantity = specialQuantity(job,order);
         if (quantity != -1) {
            writer.write("Product=CD," + Convert.fromInt(quantity) + "," + line);
         }

         // (*) soon we'll be changing Opie so that CDs use original images instead of
         // cropped / whitespaced / resized ones.  here that would lead to CDs having
         // two copies of every image, so I'll be changing JobManager to split out CDs
         // for this integration.

         // stale comments, see (*) for the current situation with CDs
         //
         // what this means:  if the print job contains <i>any</i> CD SKU,
         // find the maximum quantity of that SKU, and produce
         // a CD containing <i>all</i> images in the job, in that quantity.
         //
         // obviously this is not exactly right, but the correct solution
         // would require splitting out the CD earlier, like with Noritsu,
         // and we want a quick fix.  the reason this is acceptable is,
         // right now you can only order a CD that contains all images.
         // also note that the reprint behavior is fine -- if the user
         // picks the CD SKU to reprint, that will bring in all the images,
         // and if they reprint without picking it, no CD will come out.
         //
         // taking the maximum is just a formality ... the order parser
         // puts the same quantity everywhere, as does the override function.

      // normal products

         int llseq = 0;

         Iterator i = job.refs.iterator();
         while (i.hasNext()) {
            Job.Ref ref = (Job.Ref) i.next();

            if (ref.sku.equals(order.specialSKU) && normalSet.contains(ref.filename)) continue;

            String file = lengthLimit.transform(new Integer(llseq++),ref.filename);
            String negNumber;
            if ( ! config.proMode ) {
               negNumber = Convert.fromFile(mapper.map(new File(root,file)));
            } else {
               negNumber = removeSuffix(file);
            }

            writer.write("[Neg]" + line);
            writer.write("NegNumber=" + negNumber + line);
            writer.write("Retouch=0" + line);
            writer.write("Orient=0" + line);
            writer.write("Crop=" + line);
            writer.write("Optimize=N" + line);

            if (ref.sku.equals(order.specialSKU)) continue; // latter can be null
            // it may be pointless to have a neg without a unit,
            // but the code has been working this way, don't break it.

            Order.Item item = getOrderItem(order,ref);

            FujiNewMapping m = (FujiNewMapping) MappingUtil.getMapping(config.mappings,ref.sku);

            writer.write("[Unit]" + line);
            writer.write("Code=" + m.printCode + line);
            writer.write("Qty=" + Convert.fromInt(job.getOutputQuantity(item,ref)) + line);
            writer.write("Color=C" + line);
         }
      }

      private static String f(String s) {
         if (s == null) s = ""; // handle nulls
         return s.replace(',',' ') + ",";
         // make sure we don't send more commas than expected
      }

      private static String removeSuffix(String s) {
         int i = s.lastIndexOf('.');
         return (i == -1) ? s : s.substring(0,i);
      }
   }

// --- completion type enumeration ---

   // the first two entries come from before I converted this to an enum,
   // so they're stored in some job files as numbers rather than strings.
   //
   private static final int TYPE_MIN      = 0;
   public  static final int TYPE_DATABASE = 0; // ** do not renumber **
   public  static final int TYPE_LIST_TXT = 1; // ** do not renumber **
   public  static final int TYPE_TXT_FILE = 2;
   private static final int TYPE_MAX      = 2;

   private static String[] completionTypeTable = {
         Text.get(FormatFujiNew.class,"ct0"),
         Text.get(FormatFujiNew.class,"ct1"),
         Text.get(FormatFujiNew.class,"ct2")
      };

   private static int toCompletionType(String s, boolean acceptOld) throws ValidationException {
      for (int i=0; i<completionTypeTable.length; i++) {
         if (s.equals(completionTypeTable[i])) return i;
      }

      // rather than accepting all numbers, just accept the allowed legacy ones
      if (acceptOld) {
         if (s.equals(Convert.fromInt(TYPE_DATABASE))) return TYPE_DATABASE;
         if (s.equals(Convert.fromInt(TYPE_LIST_TXT))) return TYPE_LIST_TXT;
      }

      throw new ValidationException(Text.get(FormatFujiNew.class,"e7",new Object[] { s }));
   }

   private static String fromCompletionType(int completionType) {
      return completionTypeTable[completionType];
   }

   public static EnumeratedType completionTypeType = new EnumeratedType() {
      public int toIntForm(String s) throws ValidationException { return toCompletionType(s,/* acceptOld = */ false); }
      public String fromIntForm(int i) { return fromCompletionType(i); }
   };

   public static void validateCompletionType(int completionType) throws ValidationException {
      if (completionType < TYPE_MIN || completionType > TYPE_MAX) throw new ValidationException(Text.get(FormatFujiNew.class,"e6",new Object[] { Convert.fromInt(completionType) }));
   }

// --- completion ---

   // this layer adds and removes a "<type>|" from the front of the property.
   // if there's no pipe, that means the property is old-format database one.
   // we read those, but we no longer produce them.

   private static final char DELIMITER = '|';

   private String getProperty(int type, String orderID, Job job, Order order, HashSet printCodes) throws Exception {
      String property;

      switch (type) {
      case TYPE_DATABASE:  property = getProperty_Database(orderID,job,order);  break;
      case TYPE_LIST_TXT:  property = getProperty_ListFile(orderID,job,order);  break;
      case TYPE_TXT_FILE:  property = getProperty_TextFile(orderID,job,order,printCodes);  break;
      default:  throw new Exception(Text.get(this,"e2",new Object[] { fromCompletionType(type) }));
      }

      return fromCompletionType(type) + DELIMITER + property;
   }

   public boolean isCompleteWithoutDirectory(String property, Special special) throws Exception {

      // note, ICWD is always called before the normal (directory-based) completion code.
      // unfortunately, if ICWD returns false, we go on to try the normal
      // completion code, which we know will do nothing.  in other words,
      // we have no way of saying "definitely not complete" as opposed to "ICWD not implemented".

      if (property == null) return false;

      int type;

      int i = property.indexOf(DELIMITER);
      if (i == -1) {
         type = TYPE_DATABASE;
      } else {
         type = toCompletionType(property.substring(0,i),/* acceptOld = */ true);
         property = property.substring(i+1);
      }

      switch (type) {
      case TYPE_DATABASE:  return isComplete_Database(property,special);
      case TYPE_LIST_TXT:  return isComplete_ListFile(property,special);
      case TYPE_TXT_FILE:  return isComplete_TextFile(property,special);
      default:  throw new Exception(Text.get(this,"e3",new Object[] { fromCompletionType(type) }));
      }
   }

// --- completion - text file ---

   private String getProperty_TextFile(String orderID, Job job, Order order, HashSet printCodes) throws Exception {
      return FujiCompletion.getProperty(orderID,job,order,printCodes);
   }

   private boolean isComplete_TextFile(String property, Special special) throws Exception {

      // we already know property is non-null

      FujiNewConfig config = (FujiNewConfig) special.getQueueConfig();
      return config.completion.isComplete(property);
   }

// --- completion - list.txt ---

   // this section is a bit stale, since we no longer use the counts,
   // but keep it as is until we're sure we really don't need them.

   private static final String LIST_FILE = "list.txt";

   private String getProperty_ListFile(String orderID, Job job, Order order) {

      // turns out there's one line per image, not per SKU.
      // is it really image?  or [Neg]?  or [Unit]?
      // doesn't matter now that we send in one-to-one form.
      // do exclude the special SKU, though.

      int countSpecial = 0;
      int countNormal  = 0;

      Iterator i = job.refs.iterator();
      while (i.hasNext()) {
         Job.Ref ref = (Job.Ref) i.next();
         if (ref.sku.equals(order.specialSKU)) countSpecial++;
         else                                  countNormal ++;
      }

      // empirical fact, if there's a CD product, there will be
      // three lines for it in the list file, with names
      // like CD_CDInstructions.jpg, CD_CDReceipt.jpg, and CD.jpg.
      if (countSpecial > 0) countNormal += 3;

      return orderID + DELIMITER + Convert.fromInt(countNormal);
   }

   private boolean isComplete_ListFile(String property, Special special) throws Exception {

      // this scan data maps the order ID to the number of completed rows,
      // and the property contains the order ID, a delimiter, and the desired number.
      // the number is stored in the map as an Integer, since the accumulation code
      // needs to be able to increment it.

      // see isComplete_Database for lots of relevant comments

      HashMap result = (HashMap) special.getQueueScanData(TYPE_LIST_TXT);
      if (result == null) {
         FujiNewConfig config = (FujiNewConfig) special.getQueueConfig();
         try {
            result = readListFile(config);
         } catch (ThreadStopException e) {
            throw e; // don't convert thread stop into queue error
         } catch (Exception e) {
            special.queueError(new Exception(Text.get(this,"e4"),e));
            result = new HashMap();
         }

         special.setQueueScanData(TYPE_LIST_TXT,result);
      }

      int i = property.indexOf(DELIMITER);
      if (i == -1) throw new Exception(Text.get(this,"e5",new Object[] { property }));

      String orderID  = property.substring(0,i);
      String nDesired = property.substring(i+1);

      Integer nActual = (Integer) result.get(orderID);
      return (nActual != null);
      // we used to compare nActual to nDesired, but that was unnecessary.
      // if we see any line for the order, we know the Fuji read the file
      // successfully, and the operator will take care of printing it all.
   }

   private HashMap readListFile(FujiNewConfig config) throws Exception {
      HashMap result = new HashMap();

      // the list file is rewritten every 30 seconds, so we have to be prepared
      // to find the file not there, or to find an invalid line at the end.
      // also, since we didn't get the format from an official source, we don't
      // want to be too strict about what we expect.
      // for now, I'll take the lenient approach .... read everything we can,
      // and if a line makes no sense, just ignore it, no matter where it is.
      //
      // when we do read an incomplete file, all that will happen is that some
      // of the numbers will be lower than they should, so we won't complete
      // the associated jobs until next time we read the file ... and that's exactly
      // what we'd want to happen.

      require(config.listDirectory);

      FileReader fr;
      try {
         fr = new FileReader(new File(config.listDirectory,LIST_FILE));
      } catch (FileNotFoundException e) {
         return result; // empty
         // if we do an exists check and then open the file, that leaves
         // a window of opportunity for confusion; this should be atomic.
      }

      try {
         BufferedReader br = new BufferedReader(fr);

         String line;
         while ((line = br.readLine()) != null) {

            if (line.length() != 0) {
               readListLine(result,line);
            }
            // every other line is blank, don't waste time on those.
         }

      // any I/O exceptions are probably real errors, don't catch.
      // remember, the caller will make them into queue errors,
      // so they won't cause trouble by marking the jobs with errors.

      } finally {
         fr.close(); // be prompt so as to interfere with Fuji as little as possible
      }

      return result;
   }

   private void readListLine(HashMap result, String line) {
      final int COLS = 20;

      int[] start = new int[COLS+1]; // column start positions
      start[0] = 0;
      for (int i=0; i<COLS; i++) {
         int next = line.indexOf(';',start[i]);
         if (next == -1) return; // too few
         start[i+1] = next+1;
      }
      if (start[COLS] != line.length()) return; // too many
      // so, yes, the line must always end with a semicolon

      // now col(i) = line.substring(start[i],start[i+1]-1),
      // but adjust by one since 3 and 8 are 1-based values.
      // actually we no longer look at col 3.

      String col8 = line.substring(start[7],start[8]-1);
      int index = col8.indexOf('.');
      if (index == -1) return;
      String orderID = col8.substring(0,index);

      // ok, now we want to increment the count for orderID by one

      Integer nOld = (Integer) result.get(orderID);
      int n = (nOld == null) ? 0 : nOld.intValue();
      result.put(orderID,new Integer(n+1));
   }

// --- completion - database ---

   // this section is completely stale; database completion no longer used

   // general note, the database entries are deleted fairly soon
   // after the job is printed, maybe on the order of 30 minutes.
   // but, we poll once a minute by default, that should be fine.

   private String getProperty_Database(String orderID, Job job, Order order) {
      return orderID;
   }

   private boolean isComplete_Database(String property, Special special) throws Exception {

      // the scan data is a summary of the database query results.
      // it maps the property (Fuji OrderID) to null if the ID
      // wasn't seen, non-null if it was ... and in the latter case,
      // the value is true if all the rows were marked complete.

      HashMap result = (HashMap) special.getQueueScanData(TYPE_DATABASE);
      if (result == null) {

         FujiNewConfig config = (FujiNewConfig) special.getQueueConfig();
         //
         // interesting design point here: if the queue no longer exists,
         // or is no longer suitable, should we error out, or just have
         // the job not autocomplete?  my answer for now, we should error out,
         // because something *is* definitely and permanently wrong.
         // so, keep this line outside the try-catch block.
         //
         // maybe slightly unfortunate that we're using the current config,
         // instead of the config at the time of job creation.
         // on the other hand, it's more likely that the user will correct
         // an error in the config, or change the machine address,
         // than that the user will repoint to a completely different host.

         // the query isn't job-specific, and could fail temporarily,
         // so make it show as a thread error, not a job error.
         try {
            result = query(config); // note, property and special don't go
         } catch (Exception e) {
            special.queueError(new Exception(Text.get(this,"e1"),e));

            result = new HashMap();
            // set empty result so that we don't try again until next scan.
         }

         special.setQueueScanData(TYPE_DATABASE,result);
      }

      Boolean b = (Boolean) result.get(property);
      return (b != null && b.booleanValue());
   }

   private HashMap query(FujiNewConfig config) throws Exception {

      Connection conn = null;
      Statement  stmt = null;
      ResultSet  rset = null;
      try {

         Class.forName(config.databaseClass);
         //
         // the DriverManager class prologue says the following, which is alarming.
         //
         //    When the method getConnection is called, the DriverManager
         //    will attempt to locate a suitable driver from amongst those loaded
         //    at initialization and those loaded explicitly
         //    using the same classloader as the current applet or application.
         //
         // but, if you look at the source code, it turns out they're determining
         // the classloader by looking at the calling code's classloader,
         // not the thread classloader or system classloader or whatever.
         // and, anyway, it all seems to work even when running from the jar file.

         conn = DriverManager.getConnection(config.databaseURL,config.databaseUser,config.databasePassword);
         stmt = conn.createStatement();
         rset = stmt.executeQuery("SELECT OrderID,Status FROM OrderTable");

         // if one day we want to query per order, the wildcard for SQL Server is '%'.
         // so, add "WHERE OrderID LIKE '[property]-%'", remove from the result cols.

         HashMap result = new HashMap();

         while (rset.next()) {
            String orderID = rset.getString(1);
            int status     = rset.getInt   (2);

            // for the status, null becomes 0, which is fine here;
            // otherwise we'd have to call rset.wasNull to detect.

            if (orderID == null) continue;
            orderID = orderID.trim();

            int i = orderID.indexOf('-');
            if (i == -1) continue;

            String key = orderID.substring(0,i);
            boolean complete = true; // (status == 6)
            //
            // although it's not to spec, they want to count an order
            // as complete if there are any rows of any kind at all.

            if (complete) { // check previous completeness
               Boolean b = (Boolean) result.get(key);
               if (b != null && b.booleanValue() == false) complete = false;
            }

            result.put(key,new Boolean(complete));
         }

         return result;

      } finally {
         try {
            if (rset != null) rset.close();
         } catch (SQLException e) {
            // ignore
         }
         try {
            if (stmt != null) stmt.close();
         } catch (SQLException e) {
            // ignore
         }
         try {
            if (conn != null) conn.close();
         } catch (SQLException e) {
            // ignore
         }
      }
   }

}

