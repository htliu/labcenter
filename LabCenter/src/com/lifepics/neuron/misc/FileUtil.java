/*
 * FileUtil.java
 */

package com.lifepics.neuron.misc;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Log;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.meta.Accessor;
import com.lifepics.neuron.meta.FieldComparator;
import com.lifepics.neuron.meta.NoCaseComparator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.logging.Level;

/**
 * A class containing several kinds of file-related utilities.
 */

public class FileUtil {

// --- size functions ---

   /**
    * Get the size of a file.
    */
   public static long getSize(File file) throws IOException {

      if ( ! file.exists() ) throw new IOException(Text.get(FileUtil.class,"e1",new Object[] { Convert.fromFile(file) }));
      // length returns 0 for nonexistent, so we have to check for ourselves

      return file.length();
   }

   /**
    * Get the total size of an array of files.
    */
   public static long getTotalSize(File[] files) throws IOException {
      long total = 0;

      for (int i=0; i<files.length; i++) {
         total += getSize(files[i]);
      }

      return total;
   }

   /**
    * Figure out what the size will be
    * when the data is base-64 encoded.
    */
   public static long inBase64(long size) {
      return ((size+2) / 3) * 4;
      // 0   -> 0
      // 1-3 -> 4
      // 4-6 -> 8
      // 7-9 -> 12, etc.
   }

// --- callback interface ---

   public interface Callback {
      void copied(long size);
   }

// --- single-file copy functions ---

   private static final int CHUNK = 1024;

   /**
    * A weird function that takes variable-type arguments that can be streams or files.
    * The purpose is to guarantee that any streams that are opened are also closed.
    */
   private static void copyImpl(Object oDest, Object oSrc, Callback callback) throws IOException {

      OutputStream dest = null;
      InputStream src = null;

      if (callback != null) callback.copied(0); // convenience

      try {

         dest = (oDest instanceof OutputStream) ? (OutputStream) oDest : new FileOutputStream((File) oDest);
         src  = (oSrc  instanceof InputStream ) ? (InputStream ) oSrc  : new FileInputStream ((File) oSrc );

         byte[] buffer = new byte[CHUNK];
         long size = 0;

         while (true) {
            int len = src.read(buffer);
            if (len == -1) break;

            dest.write(buffer,0,len);

            size += len;
            if (callback != null) callback.copied(size);
         }

      } finally {

         try {
            if (src != null) src.close();
         } catch (IOException e) {
            // ignore
         }

         try {
            if (dest != null) dest.close();
         } catch (IOException e) {
            // ignore
         }
      }
   }

// --- copy layer 2 ---

   /**
    * Copy from one stream to another.
    */
   public static void copy(OutputStream dest, InputStream src, Callback callback) throws IOException {
      copyImpl(dest,src,callback);
   }

   /**
    * Copy a file to a stream.
    */
   public static void copy(OutputStream dest, File src, Callback callback) throws IOException {
      copyImpl(dest,src,callback);
   }

   public static void checkNotExists(File dest) throws IOException {
      if (dest.exists()) {
         throw new IOException(Text.get(FileUtil.class,"e2",new Object[] { Convert.fromFile(dest) }));
      }
   }

   /**
    * Make a file not exist, by deleting it if it does.<p>
    *
    * This function is not called by anything in FileUtil,
    * but this seemed like the right place for it to go.
    */
   public static void makeNotExists(File dest) throws IOException {
      if (dest.exists() && ! dest.delete()) {
         throw new IOException(Text.get(FileUtil.class,"e3",new Object[] { Convert.fromFile(dest) }));
      }
   }

   /**
    * Copy a stream to a file.
    */
   public static void copy(File dest, InputStream src, Callback callback) throws IOException {
      checkNotExists(dest);
      copyImpl(dest,src,callback);
   }

   /**
    * Copy a file to a new file.
    */
   public static void copy(File dest, File src, Callback callback) throws IOException {
      checkNotExists(dest);
      copyImpl(dest,src,callback);
   }

// --- copy layer 3 ---

   /**
    * Copy from one stream to another.
    */
   public static void copy(OutputStream dest, InputStream src) throws IOException {
      copy(dest,src,null);
   }

   /**
    * Copy a file to a stream.
    */
   public static void copy(OutputStream dest, File src) throws IOException {
      copy(dest,src,null);
   }

   /**
    * Copy a stream to a file.
    */
   public static void copy(File dest, InputStream src) throws IOException {
      copy(dest,src,null);
   }

   /**
    * Copy a file to a new file.
    */
   public static void copy(File dest, File src) throws IOException {
      copy(dest,src,null);
   }

// --- no-close copy functions ---

   private static class NoCloseStream extends OutputStream {
      private OutputStream stream;
      public NoCloseStream(OutputStream stream) { this.stream = stream; }

      public void close() throws IOException { /* ignore */ }
      public void flush() throws IOException { stream.flush(); }
      public void write(byte[] b) throws IOException { stream.write(b); }
      public void write(byte[] b, int off, int len) throws IOException { stream.write(b,off,len); }
      public void write(int b) throws IOException { stream.write(b); }
   }

   public static void copyNoClose(OutputStream dest, InputStream src, Callback callback) throws IOException {
      copy(new NoCloseStream(dest),src,callback);
   }

   public static void copyNoClose(OutputStream dest, File src, Callback callback) throws IOException {
      copy(new NoCloseStream(dest),src,callback);
   }

// --- multi-file copy helpers ---

   /**
    * Check whether a set of files (in different directories) contains any duplicate names,
    * and so cannot be copied to a single directory.
    */
   public static void checkDuplicateNames(File[] files) throws IOException {
      HashSet set = new HashSet();

      for (int i=0; i<files.length; i++) {
         String name = files[i].getName();

         // false result means didn't add because already present
         if ( ! set.add(name) ) {
            throw new IOException(Text.get(FileUtil.class,"e4",new Object[] { name }));
         }
      }
   }

   /**
    * Sort a set of files so they are in alphabetical order by name.
    */
   public static void sortByName(File[] files) {
      Arrays.sort(files,new FieldComparator(filenameAccessor,new NoCaseComparator()));
   }

   public static Accessor filenameAccessor = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return ((File) o).getName(); }
   };

// --- functions that use atomic operations ---

   /**
    * Create a new directory.  Atomicity doesn't really figure into this,
    * the point is just to throw a nice exception if the operation fails.
    */
   public static void makeDirectory(File dir) throws IOException {
      LinkedList ops = new LinkedList();
      ops.add(new Op.Mkdir(dir));
      Op.transact(ops);
   }

   /**
    * Create a new directory and copy files into it, as atomically as possible.
    */
   public static void copyToNewDirectory(File dir, File[] files) throws IOException {
      LinkedList ops = new LinkedList();
      ops.add(new Op.Mkdir(dir));
      for (int i=0; i<files.length; i++) {
         ops.add(new Op.Copy(new File(dir,files[i].getName()),files[i]));
      }
      Op.transact(ops);
   }

   /**
    * Create a new directory and move files into it, as atomically as possible.
    */
   public static void moveToNewDirectory(File dir, File[] files) throws IOException {
      LinkedList ops = new LinkedList();
      ops.add(new Op.Mkdir(dir));
      for (int i=0; i<files.length; i++) {
         ops.add(new Op.Move(new File(dir,files[i].getName()),files[i]));
      }
      Op.transact(ops);
   }

   /**
    * Alter an existing directory by removing some files and copying in others,
    * as atomically as possible, using a temp directory for rollback.
    *
    * @param tempDir The temporary directory, which should <i>not exist</i>.
    */
   public static void alter(File dir, File tempDir, String[] removeFiles, File[] addFiles) throws IOException {
      LinkedList ops = new LinkedList();

      if (removeFiles.length > 0) { // don't make temp dir unnecessarily

         ops.add(new Op.TempMkdir(tempDir));

         for (int i=0; i<removeFiles.length; i++) {
            ops.add(new Op.TempMove(new File(tempDir,removeFiles[i]),new File(dir,removeFiles[i])));
         }
      }

      for (int i=0; i<addFiles.length; i++) {
         ops.add(new Op.Copy(new File(dir,addFiles[i].getName()),addFiles[i]));
      }
      Op.transact(ops);
   }

// --- miscellaneous functions ---

   /**
    * Vary a filename to include a disambiguating number.
    */
   public static String vary(String filename, int n) {
      int i = filename.lastIndexOf('.');

      String prefix = (i == -1) ? filename : filename.substring(0,i); // doesn't include dot
      String suffix = (i == -1) ? ""       : filename.substring(  i);

      return prefix + '_' + n + suffix;
   }

   public static String disambiguate(File dir, String filename) {

      String s = filename;
      int n = 2;

      while (new File(dir,s).exists()) s = vary(filename,n++);
      // guaranteed to terminate, directory is finite

      // normally in code like this we have to be careful to ignore case,
      // but here the exists function will take care of it if it matters

      return s;
   }

   /**
    * Purge a directory by removing a known set of files
    * and then attempting to remove the directory itself.
    *
    * @return True if everything was successfully removed.
    */
   public static boolean purge(File dir, Object[] names) {
      return purge(dir,names,/* warn = */ true,/* rmdir = */ true);
   }
   public static boolean purge(File dir, Object[] names, boolean warn) {
      return purge(dir,names,warn,/* rmdir = */ true);
   }
   public static boolean purge(File dir, Object[] names, boolean warn, boolean rmdir) {

      // it would be possible to make this behave like "rm -r",
      // but don't do that, it would be bad if it ran on the wrong directory

      boolean notFound = false;
      boolean noDelete = false;

      for (int i=0; i<names.length; i++) {
         File file;

         if      (names[i] instanceof String) file = new File(dir,(String) names[i]); // the 99.9% case
         else if (names[i] instanceof File  ) file = (File) names[i];
         else throw new IllegalArgumentException();
         // we want to allow absolute paths, but we want to be really sure that we don't break the existing
         // code by misinterpreting a simple name as an absolute path ... hence the Object array

         if      ( ! file.exists() ) notFound = true;
         else if ( ! file.delete() ) noDelete = true;
      }

      if (rmdir) {
         if      ( ! dir.exists() ) notFound = true;
         else if ( ! dir.delete() ) noDelete = true;
      }

      // if some files are missing, it's sort of bad,
      // but probably the user deleted them manually.
      // so, write a log entry, but don't let it
      // affect any behavior outside this function.

      // if some files couldn't be deleted, it's bad,
      // because we can't recover from it ourselves.
      // probably there are extra files in a directory,
      // and the user needs to evaluate what they are.

      if (notFound && warn) Log.log(Level.WARNING,FileUtil.class,"e5",new Object[] { Convert.fromFile(dir) });

      return ( ! noDelete );
   }

}

