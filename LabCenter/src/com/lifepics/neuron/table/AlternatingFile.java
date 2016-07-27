/*
 * AlternatingFile.java
 */

package com.lifepics.neuron.table;

import com.lifepics.neuron.core.Log;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.object.XML;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.logging.Level;

/**
 * A utility class for dealing with files that are updated safely
 * by writing to an alternate file and then deleting and renaming.
 */

public class AlternatingFile {

   // here's some theory to explain why we have two alternates.
   //
   // first, define single-character codes to represent states of files.
   //
   //    .     nonexistent
   //    v     valid
   //    i     invalid
   //
   // then, combine those codes into three-character strings, with the
   // characters representing the base, alternate, and temporary files.
   // in that case, a normal write looks like this.
   //
   //    v..   initial state
   //    vi.   write in progress
   //    vv.   write complete
   //    .v.   base deleted
   //    v..   alternate renamed
   //
   // so, whenever there's an invalid file, there's also a valid base file,
   // and the valid file is the one that gets read after a crash.
   // if, however, we're creating the file (a create-write, not an update-write),
   // the state ".i." used to be able to occur.  if the system was powered down
   // at that exact moment, on restart the ".i." would be indistinguishable from
   // the state ".v." ... so the invalid file would be read, and cause an error.
   // to prevent that, we write to a different alternate file, like so.
   //
   //    ...   initial state
   //    ..i   write in progress
   //    ..v   write complete
   //    v..   alternate renamed

   // in the rare case that the system *is* powered down, and a temporary file
   // is left in place, how does it get deleted?  there are two cases.
   //
   // if the file is in a storage directory, then the next time LabCenter is
   // started (it has to be started, since it was powered down), the call to
   // generate the alternating file list will just find and delete it;
   // no need to test for existence in the normal read and write routines.
   //
   // if the file is somewhere else, then it's tricky.  if we were writing to
   // the file during AutoUpdate, say, then maybe we'll see it's still not
   // there, and write to it again, overwriting the existing temporary file.
   // if we were writing to some random file in some random place, though,
   // I think we're just out of luck, the user will have to delete the file.
   //
   // it would be especially unfortunate if we ever used AlternatingFile to
   // write into an order or job directory, since then the purge would fail;
   // but I don't think we do that.

// --- fields ---

   private File file;
   private File alternate;
   private File temporary;

   private File target; // for write
   private boolean create;

   private InputStream inputStream;
   private OutputStream outputStream;

// --- constants ---

   private static String infixAlternate = Text.get(AlternatingFile.class,"s1");
   private static String infixTemporary = Text.get(AlternatingFile.class,"s2");

// --- construction ---

   /**
    * Create an alternating file object for either reading or writing.
    * The object should be used once and then discarded,
    * it is not designed for reuse and is also not thread-safe.
    *
    * @param file The base file, not including the alternate part of the name.
    */
   public AlternatingFile(File file) {
      this.file = file;

      File parent = file.getParentFile();
      String name = file.getName();

      int i = name.lastIndexOf('.');
      String prefix = (i == -1) ? name : name.substring(0,i); // doesn't include dot
      String suffix = (i == -1) ? ""   : name.substring(  i);

      alternate = new File(parent,prefix + infixAlternate + suffix);
      temporary = new File(parent,prefix + infixTemporary + suffix);
   }

// --- helpers ---

   private void warn(String key) {
      Log.log(Level.WARNING,this,key,new Object[] { file.getName() });
   }

   private void warn(String key, Throwable t) {
      Log.log(Level.WARNING,this,key,new Object[] { file.getName() },t);
   }

   private void fail(String key) throws IOException {
      throw new IOException(Text.get(this,key,new Object[] { file.getName() }));
   }

// --- retry helpers ---

   // we still don't understand why the delete and rename steps sometimes
   // fail, but they do, so let's add some retries and see if it helps any.
   // I think my logic is solid and it's some process in Java or in the OS.

   // for now I'm limiting the retries to commitWrite, since that's where
   // the problems have been found, but it would be reasonable to cover
   // AlternatingFile.delete too.  endWrite I'm not sure about, since it's
   // already an error case, and the rest only occur when there's damage.

   private static final int RETRY_DELAY = 1000;
   private static final int RETRY_LIMIT = 1;
   // keep these very low to minimize non-interruptible delays in threads

   private static boolean deleteWithRetries(File f1) {
      int failures = 0;
      while (true) {
         if (f1.delete()) return true;
         failures++;
         if (failures > RETRY_LIMIT) return false;
         try {
            Thread.sleep(RETRY_DELAY);
         } catch (InterruptedException e) {
         }
      }
   }

   private static boolean renameWithRetries(File f1, File f2) {
      int failures = 0;
      while (true) {
         if (f1.renameTo(f2)) return true;
         failures++;
         if (failures > RETRY_LIMIT) return false;
         try {
            Thread.sleep(RETRY_DELAY);
         } catch (InterruptedException e) {
         }
      }
   }

// --- input side ---

   /**
    * Check whether the alternating file exists (in any form).
    */
   public boolean exists() {
      return file.exists() || alternate.exists();
   }

   /**
    * Get an input stream to read from the alternating file.
    * Also, if the file structure is damaged, clean it up.
    *
    * The read code should be followed by a "finally" clause
    * containing a call to {@link #endRead() endRead}.
    */
   public InputStream beginRead() throws IOException {

      if (file.exists()) {
         if (alternate.exists()) { // case 1: file and alternate => damaged

            if (alternate.delete()) warn("e2");
            else warn("e3");

            inputStream = new FileInputStream(file);

         } else {                  // case 2: file, no alternate => the normal state of affairs

            inputStream = new FileInputStream(file);

         }
      } else {
         if (alternate.exists()) { // case 3: alternate, no file => damaged

            boolean result = alternate.renameTo(file);
            if (result) warn("e4");
            else warn("e5");

            inputStream = new FileInputStream(result ? file : alternate);

         } else {                  // case 4: neither file nor alternate => nonexistent

            fail("e1");
            // FileNotFoundException would be correct, but it adds no value
         }
      }

      // note that there are no exception-throwing points
      // between where the stream is set to non-null,
      // which indicates that a read is in progress,
      // and where the stream is returned to the caller.

      return inputStream;
   }

   /**
    * Complete a read by closing the input stream.
    */
   public void endRead() {

      if (inputStream == null) return; // not reading

      try {
         inputStream.close();
      } catch (IOException e) {
         warn("e6",e);
      }
      // endRead is supposed to be called in a "finally" clause,
      // so we don't want it to throw any exceptions

      inputStream = null;
   }

// --- output side ---

   /**
    * Get an output stream to write to the alternating file.
    * Also, if the file structure is damaged, clean it up.<p>
    *
    * The write code should be followed by a call to {@link #commitWrite() commitWrite}
    * and then by a "finally" clause containing a call to {@link #endWrite() endWrite}.
    */
   public OutputStream beginWrite() throws IOException {

      target = alternate;
      create = false;

      if (file.exists()) {
         if (alternate.exists()) { // case 1: file and alternate => damaged

            if (alternate.delete()) warn("e11");
            else fail("e12");

         } else {                  // case 2: file, no alternate => the normal state of affairs

            // ok, fall through

         }
      } else {
         if (alternate.exists()) { // case 3: alternate, no file => damaged

            if (alternate.renameTo(file)) warn("e13");
            else fail("e14");

         } else {                  // case 4: neither file nor alternate => nonexistent

            target = temporary; // see big comment at start
            create = true;
         }
      }

      outputStream = new FileOutputStream(target);

      // note that there are no exception-throwing points
      // between where the stream is set to non-null,
      // which indicates that a write is in progress,
      // and where the stream is returned to the caller.

      return outputStream;
   }

   /**
    * A special-purpose function that breaks encapsulation.
    */
   public File getTarget() {
      return target;
   }

   /**
    * Complete a write successfully
    * by closing the output stream and then deleting and renaming.
    */
   public void commitWrite() throws IOException {
      commitWrite(null);
   }
   public void commitWrite(File backup) throws IOException {

      if (outputStream == null) return; // not writing

      try {
         outputStream.close();
      } finally {
         outputStream = null; // mark write as complete, regardless
      }

      if (file.exists()) {

         if (backup != null && renameWithRetries(file,backup)) {
            // ok, done
         } else {
            if ( ! deleteWithRetries(file) ) fail("e9");
         }
         // if rename fails, fall back to deleting.  this might seem wrong,
         // but it covers at least two real situations ... the backup dir
         // could be missing; or we could save the config twice in the same
         // second, get the same file name, and hence be unable to rename.
      }

      if ( ! renameWithRetries(target,file) ) {

         if (create) fail("e19");
         else warn("e10");
      }
      // if we fail to delete the base,
      // reading the file will read the old object, so we've failed,
      // but if we fail to rename the alternate,
      // reading the file will still read the new object, so it's only a warning ...
      // unless the write is a create-write, in which case a read won't find the
      // temporary file, and the write has failed after all.
   }

   /**
    * Complete a write unsuccessfully
    * (unless {@link #commitWrite() commitWrite} has already been called)
    * by closing the output stream and removing the alternate file.
    */
   public void endWrite() {

      if (outputStream == null) return; // not writing

      try {
         outputStream.close();
      } catch (IOException e) {
         warn("e7",e);
      }
      // endWrite is supposed to be called in a "finally" clause,
      // so we don't want it to throw any exceptions

      outputStream = null;

      if ( ! target.delete() ) warn("e8");
   }

// --- persistent objects ---

   // it feels like these belong in XML.java, and
   // the package dependency could go either way,
   // but this keeps the levels as they have been.
   //
   // on the other hand, this code is a model for
   // how to use alternating files correctly.
   // if you scan for AlternatingFile you can find
   // several functions that are variants of these.

   /**
    * Read a persistent object from an alternating file.
    */
   public static Object load(File file, XML.Persist persist, String name) throws IOException, ValidationException {
      AlternatingFile af = new AlternatingFile(file);
      try {
         InputStream inputStream = af.beginRead();
         return XML.loadStream(inputStream,persist,name);
      } finally {
         af.endRead();
      }
   }

   /**
    * Write a persistent object to an alternating file.
    */
   public static void store(File file, XML.Persist persist, String name) throws IOException {
      AlternatingFile af = new AlternatingFile(file);
      try {
         OutputStream outputStream = af.beginWrite();
         XML.storeStream(outputStream,persist,name);
         af.commitWrite();
      } finally {
         af.endWrite();
      }
   }

// --- miscellaneous ---

   /**
    * Delete the alternating file, including the alternate, if present.
    */
   public void delete() throws IOException {

      boolean deleted = false;

      if (alternate.exists()) {

         if (alternate.delete()) { deleted = true; warn("e15"); }
         else fail("e16");
      }

      if (file.exists()) {

         if (file.delete()) { deleted = true; }
         else fail("e17");
      }

      if ( ! deleted ) fail("e18");
      // since an alternate without a base does count as an existing file,
      // it is OK if we don't find a base file to delete.
      // however, we must delete one or the other, or it's an error.
   }

   /**
    * Get the last-modified time of whatever component we would read from.
    * If neither component exists or there's an error, throw an exception.
    */
   public long getLastModified() throws IOException {

      File source = null;

      if      (file     .exists()) source = file;
      else if (alternate.exists()) source = alternate;
      else fail("e22");

      long t = source.lastModified();
      if (t == 0) fail("e23"); // can't get time

      return t;
   }

// --- list ---

   /**
    * List the distinct alternating files in a directory.
    */
   public static HashSet list(File dir) {
      HashSet set = new HashSet();

      File[] files = dir.listFiles(new FileFilter() { public boolean accept(File file) {
         return ( ! file.isDirectory() );
      } });
      // there shouldn't be any subdirectories, I don't think,
      // but there's no harm in excluding them just in case.

      if (files != null) { // null possible if dir not a directory
         for (int j=0; j<files.length; j++) {
            String name = files[j].getName();

            int i = name.lastIndexOf('.');
            String prefix = (i == -1) ? name : name.substring(0,i); // doesn't include dot
            String suffix = (i == -1) ? ""   : name.substring(  i);

            if (prefix.endsWith(infixTemporary)) { // temporary file, delete or ignore
               String key = files[j].delete() ? "e20" : "e21";
               Log.log(Level.WARNING,AlternatingFile.class,key,new Object[] { files[j].getName() });
               continue;
            }
            if (prefix.endsWith(infixAlternate)) prefix = prefix.substring(0,prefix.length()-infixAlternate.length());

            set.add(prefix + suffix); // ignore result
         }
      }

      return set;
   }

}

