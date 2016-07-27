/*
 * FileMapper.java
 */

package com.lifepics.neuron.misc;

import com.lifepics.neuron.core.Convert;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * A utility class for mapping file names to how they'll appear on a different system.
 * For example, maybe D:\requests\ should map to \\pics\requests\ ...this could be
 * accomplished by defining a mapping for D:\ to \\pics\, or for D:\requests\ specifically.
 */

public class FileMapper {

// --- fields ---

   public LinkedList entries;

// --- construction ---

   public FileMapper() {
      entries = new LinkedList();
   }

// --- helpers ---

   /**
    * @return The number of components in the file path:
    *         zero for a null argument, one for a file system root, and so on.
    */
   public static int getDepth(File file) {
      int n = 0;
      while (file != null) { file = file.getParentFile(); n++; }
      return n;
   }

   /**
    * @return The nth-level parent of the given file:
    *         zero for the file itself, one for its parent, and so on.
    */
   public static File getParentFile(File file, int n) {
      while (n-- > 0) file = file.getParentFile();
      return file;
   }

   /**
    * An optimization of getParentFile(getDepth()-1).
    */
   public static File getRoot(File file) {
      File save = null;
      while (true) {
         File temp = file.getParentFile();
         if (temp == null) return (file.getPath().equals("\\\\") && save != null) ? save : file;
         save = file;
         file = temp;
      }
      // for strings of the form "\\machine name\something",
      // the actual root part is "\\", not "\\machine name".
   }

   /**
    * Get the relative path from the root, i.e., the rest of the path.
    * The result could be made into a file, but I need it as a string.
    * The goal is for "new File(getRoot(file),getRest(file))" to give the original file.
    */
   public static String getRest(File file) {
      String path = Convert.fromFile(file);
      String root = Convert.fromFile(getRoot(file));

      if ( ! path.startsWith(root) ) return path;
      // should always be true, but fail semi-gracefully if it's not

      int skip = root.length();
      if (path.length() > skip && path.charAt(skip) == File.separatorChar) skip++;
      // root paths look like "C:\" and "\\machine name",
      // so the separator test may or may not do anything

      return path.substring(skip);
   }

   /**
    * @return The map argument with the last n names from the file argument appended:
    *         zero for the map itself, one for one name appended, and so on.
    */
   public static File transfer(File map, File file, int n) {
      return (n > 0) ? new File(transfer(map,file.getParentFile(),n-1),file.getName())
                     : map;
   }

// --- map function ---

   /**
    * @param file The absolute path of the file to be mapped.
    *
    * @return The mapped file, or the original file argument if no mapping was found.
    */
   public File map(File file) {
      int nFile = getDepth(file);

      Iterator i = entries.iterator();
      while (i.hasNext()) {
         Entry entry = (Entry) i.next();

         int nFrom = getDepth(entry.mapFrom);

         int n = nFile-nFrom; // number to transfer
         if (n < 0) continue;

         if ( ! getParentFile(file,n).equals(entry.mapFrom) ) continue;

         return transfer(entry.mapTo,file,n);

         // note, if one entry covers another, the result depends on
         // the order of the entries .. but think of it as a feature,
         // since it allows an earlier entry to override a later one.
      }

      return file;
   }

// --- entry class ---

   public static class Entry {

      public File mapFrom;
      public File mapTo;

      /**
       * @param mapFrom The absolute path of the mapped location.
       * @param mapTo   The absolute path it should be mapped to.
       */
      public Entry(File mapFrom, File mapTo) { this.mapFrom = mapFrom; this.mapTo = mapTo; }
   }

}

