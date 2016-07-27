/*
 * FujiCompletion.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

/**
 * A substructure and utility class for the shared Fuji completion code.
 * This is the only completion method for Fuji JobMaker 1.4 (Fuji3)
 * but is one of three semi-possible methods for Fuji PIC 2.6 (FujiNew).
 * (The other two will soon be technically possible but obsolete.)
 */

public class FujiCompletion extends Structure {

// --- fields ---

   public File scanDirectory;

   /**
    * The interval for scanning for stale completion files,
    * which will normally be from non-LabCenter print jobs.
    */
   public long staleScanInterval; // millis

   /**
    * The interval that defines staleness.
    */
   public long stalePurgeInterval; // millis

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      FujiCompletion.class,
      // no version
      new AbstractField[] {

         new FileField("scanDirectory","ScanDirectory",0,new File("D:\\LifePicsPrinted")),
         new LongField("staleScanInterval", "StaleScanInterval-Millis", 0,3600000),  // 1 hour
         new LongField("stalePurgeInterval","StalePurgeInterval-Millis",0,604800000) // 1 week
      });

   protected StructureDefinition sd() { return sd; }

// --- validation ---

   public void validate() throws ValidationException {

      if (staleScanInterval  < 0) throw new ValidationException(Text.get(this,"e1"));
      if (stalePurgeInterval < 0) throw new ValidationException(Text.get(this,"e2"));
   }

// --- completion code ---

   private static HashMap lastScan = new HashMap();

   private static final String FILE_SUFFIX = ".txt";
   private static final int    FILE_SIZE   = 1024; // my arbitrary definition of small

   // this is one of the rare places where we delete files that we don't have
   // explicit ownership of, so we have to be super-careful not to get things
   // we don't want.  the conditions are:  right suffix, small size, not a dir.

   // (*1) note, File.length returns zero instead of throwing an exception
   // if the file doesn't exist, which is why we normally use FileUtil.getSize,
   // but here we explicitly check existence and also want to avoid exceptions.

   // (*2) same for File.delete vs. FileUtil.makeNotExists and similar things.
   // we do want to consider what happens if delete returns false, though.

   public static String getProperty(String orderID, Job job, Order order, HashSet printCodes) throws Exception {

      if (Format.specialQuantity(job,order) != -1) {

         printCodes.add("CD");
         //
         // the exact code doesn't matter now that we're checking for equal size
         // rather than hash map equality.  for future reference, the one actual
         // code I've seen is "Premium CD", so if we go back to checking equality,
         // we'll need to figure out the right value(s) and handle CDs specially.
         //
         // yes, there's potential for collision with another product code, but
         // it seems unlikely.
      }

      return pack(orderID,printCodes);
   }

   public boolean isComplete(String property) {

      boolean result = isCompleteImpl(property);
      if (shouldScan()) doScan();
      return result;
   }

   private boolean isCompleteImpl(String property) {

      Property p = unpack(property);
      //
      // in the beta code, the property string was just the order ID.
      // that case is handled -- the new code will see it as an empty
      // print code set, and will in fact complete on an empty file,
      // just as before.  it also won't work, just as before, but the
      // point is, it won't crash or anything.

      File orderFile = new File(scanDirectory,p.orderID + FILE_SUFFIX);
      boolean result = false;

      if (    orderFile.isFile() // exists, not directory
           && orderFile.length() < FILE_SIZE ) { // (*1) we don't need to check the size now that we read the file, but let's do it anyway

         HashSet printCodes = null;
         try {
            printCodes = read(orderFile);
         } catch (IOException e) {
            // ignore, try again later
         }

         // the original idea was to test printCodes.equals(p.printCodes),
         // but it turns out the codes that appear in the files aren't
         // always the same as the ones LC sends over.  so, check by size
         // instead, but keep the codes in case we figure it out later.
         //
         if (printCodes != null && printCodes.size() == p.printCodes.size()) {

            result = orderFile.delete(); // (*2) if delete fails, just pretend we didn't
            // see it; maybe the creator just has it locked and we'll catch it next time.

            // deleting the file and marking the job complete are not as atomic
            // as one might like, but the connection is pretty solid ...
            // the stale-file scan shouldn't throw exceptions, and the rest is just
            // usual table writes.  and, if the connection breaks, we can handle it;
            // we'll see that the job doesn't complete in the monitor.
         }
      }

      return result;
   }

   private boolean shouldScan() {

      Long last = (Long) lastScan.get(scanDirectory);
      long now = System.currentTimeMillis();

      if (last != null && now < last.longValue() + staleScanInterval) return false;

      lastScan.put(scanDirectory,new Long(now));

      return (last != null);
      // to reduce the startup load a little, don't scan until the second interval
   }

   private void doScan() {

      final long purge = System.currentTimeMillis() - stalePurgeInterval;

      File[] files = scanDirectory.listFiles(new FileFilter() { public boolean accept(File file) {

         // pretty much everything in the directory should match the last three conditions,
         // they're just formalities, so go ahead and check on the modification time first.

         long lastModified = file.lastModified();
         return (    lastModified != 0 // ignore I/O error
                  && lastModified < purge
                  && file.getName().endsWith(FILE_SUFFIX)
                  && file.isFile()
                  && file.length() < FILE_SIZE ); // (*1)
      } });
      // I suppose filtering is faster than just getting everything and looping through?

      if (files == null) return; // same logic as below

      for (int i=0; i<files.length; i++) {
         files[i].delete(); // (*2) if it fails, that's not good, but what can we do?
      }
   }

// --- helpers ---

   private static final char CHAR_DELIM = '|';
   private static final String REGEX_DELIM = "\\|";

   // leave escape and unescape unimplemented for now, not needed.
   // also not needed is a validation for no duplicates in unpack.

   private static String escape  (String s) { return s; }
   private static String unescape(String s) { return s; }

   private static String pack(String orderID, HashSet printCodes) {
      StringBuffer b = new StringBuffer();

      b.append(escape(orderID));

      Iterator i = printCodes.iterator();
      while (i.hasNext()) {
         b.append(CHAR_DELIM);
         b.append(escape((String) i.next()));
      }

      return b.toString();
   }

   private static class Property {
      public String orderID;
      public HashSet printCodes;
   }
   private static Property unpack(String property) {
      Property p = new Property();
      String[] s = property.split(REGEX_DELIM,-1); // -1 to stop weird default behavior

      p.orderID = unescape(s[0]); // always at least one element, that's how split works

      p.printCodes = new HashSet();
      for (int i=1; i<s.length; i++) {
         p.printCodes.add(unescape(s[i]));
      }

      return p;
   }

   private static HashSet read(File file) throws IOException {

      // file consists of [printCode CRLF]*
      // note that BufferedReader doesn't report a blank line at the end

      FileReader fr = new FileReader(file);
      try {

         BufferedReader br = new BufferedReader(fr);

         HashSet printCodes = new HashSet();
         String line;
         while ((line = br.readLine()) != null) {
            printCodes.add(line);
         }

         return printCodes;

      } finally {
         fr.close();
      }
   }

}

