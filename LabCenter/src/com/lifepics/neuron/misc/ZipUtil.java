/*
 * ZipUtil.java
 */

package com.lifepics.neuron.misc;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * A utility class for zip and jar file operations.
 */

public class ZipUtil {

// --- extract ---

   // this chain is only good if you know the target directory is empty

   public static void extractAllFromResource(File dir, Object o, String name) throws Exception {

      // I had this great plan for accessing the resource file
      // via JarURLConnection.getJarFile, based on code that I found
      // in the JarClassLoader example in the Sun jar file tutorial ...
      // and it worked in testing, but failed when run in the launched
      // application, because JarURLConnection doesn't do nested jars.
      // so, just do the stupid thing and copy the jar file onto disk.

      File jarFile = new File(dir,name);
      FileUtil.copy(jarFile,Resource.getResourceAsStream(o,name));
      try {
         extractAll(jarFile,dir);
      } finally {
         jarFile.delete(); // ignore result
      }
   }

   // the only other "extract all" function is in InstallThread.
   // it has some differences in behavior so I'm still going to
   // keep them separate.

   public static void extractAll(File jarFile, File dir) throws Exception {
      ZipFile zf = new ZipFile(jarFile);
      try {
         extractAll(zf,dir);
      } finally {
         zf.close(); // else can't delete
      }
   }

   public static void extractAll(ZipFile zf, File dir) throws Exception {
      Enumeration e = zf.entries();
      while (e.hasMoreElements()) {
         ZipEntry ze = (ZipEntry) e.nextElement();
         String name = ze.getName();
         File file = new File(dir,name);

         if (ze.isDirectory()) {
            if ( ! file.mkdir() ) throw new Exception(Text.get(ZipUtil.class,"e1",new Object[] { name }));
         } else {
            FileUtil.copy(file,zf.getInputStream(ze));
            long t = ze.getTime();
            if (t != -1) file.setLastModified(t); // not critical, ignore result
         }
      }
   }

// --- refresh ---

   public static void refreshAllFromResource(File dir, Object o, String name, HashSet allowEdits) throws Exception {
      File jarFile = new File(dir,FileUtil.disambiguate(dir,name));
      FileUtil.copy(jarFile,Resource.getResourceAsStream(o,name));
      try {
         refreshAll(jarFile,dir,allowEdits);
      } finally {
         jarFile.delete(); // ignore result
      }
   }

   public static void refreshAll(File jarFile, File dir, HashSet allowEdits) throws Exception {
      ZipFile zf = new ZipFile(jarFile);
      try {
         LinkedList ops = new LinkedList();
         refreshAll(zf,dir,ops,allowEdits);
         if (ops.size() > 0) Op.transact(ops);
      } finally {
         zf.close(); // else can't delete
      }
   }

   private static final String TEMP_DIR = "temp"; // can't collide because of disambiguation
   private static final String REMOVE_PREFIX = "(remove)";

   public static void refreshAll(ZipFile zf, File dir, LinkedList ops, HashSet allowEdits) throws Exception {

      File tempDir = null;
      int next = 0;

      Enumeration e = zf.entries();
      while (e.hasMoreElements()) {
         ZipEntry ze = (ZipEntry) e.nextElement();
         String name = ze.getName();
         boolean remove = false;
         if (name.startsWith(REMOVE_PREFIX)) {
            name = name.substring(REMOVE_PREFIX.length());
            remove = true;
         }
         File file = new File(dir,name);

         if (ze.isDirectory()) {

            if (remove) throw new Exception(Text.get(ZipUtil.class,"e4",new Object[] { name })); // programmer error
            // could be done, but I don't see an immediate need for it,
            // and going in reverse order to remove directory after files would be a hassle

            if (file.exists()) {

               if ( ! file.isDirectory() ) throw new Exception(Text.get(ZipUtil.class,"e2",new Object[] { name }));
               // Q: why not just let the process fail when the first file is extracted?
               // A: because there might not be a file, then the refresh would be incorrect

            } else {
               ops.add(new Op.Mkdir(file));
            }
            // don't bother with timestamps on directories

         } else {

            boolean move = false;
            boolean extract = false;

            if (file.exists()) {

               if (file.isDirectory()) throw new Exception(Text.get(ZipUtil.class,"e3",new Object[] { name }));
               // necessary because File.length is 0 for directories,
               // so without this, we could match a size-zero file to a directory

               if (remove) { move = true; }
               else if ( ! match(file,ze) && ! allowEdits.contains(name) ) { move = true; extract = true; }

            } else {
               extract = ! remove;
            }

            if (move) {
               if (tempDir == null) {
                  tempDir = new File(dir,FileUtil.disambiguate(dir,TEMP_DIR));
                  ops.add(new Op.TempMkdir(tempDir));
               }
               File tempFile = new File(tempDir,Convert.fromInt(next++)); // could be duplicate names if zip has subfolders
               ops.add(new Op.TempMove(tempFile,file));
            }

            if (extract) {
               ops.add(new Extract(file,zf,ze));
            }
         }
      }
   }

   private static boolean match(File file, ZipEntry ze) {
      if (file.length() != ze.getSize()) return false;
      long t = ze.getTime();
      if (t != -1 && file.lastModified() != t) return false;
      return true;
   }

   public static class Extract extends Op { // cf Op.Copy
      protected File dest;
      protected ZipFile zf;
      protected ZipEntry ze;
      public Extract(File dest, ZipFile zf, ZipEntry ze) { this.dest = dest; this.zf = zf; this.ze = ze; }

      public void dodo() throws IOException {
         FileUtil.copy(dest,zf.getInputStream(ze));
         long t = ze.getTime();
         if (t != -1) dest.setLastModified(t); // not critical, ignore result
      }
      public void undo() {
         dest.delete(); // ignore result
      }
      public void undoPartial() { undo(); }
   }

}

