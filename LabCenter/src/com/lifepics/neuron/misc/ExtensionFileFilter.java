/*
 * ExtensionFileFilter.java
 */

package com.lifepics.neuron.misc;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

/**
 * A filter that accepts files with appropriate extensions.
 */

public class ExtensionFileFilter extends FileFilter implements java.io.FileFilter {

   private String description;
   private String[] extensions;
   private boolean acceptDirs;

   /**
    * @param description A description of the general category of file.
    * @param extensions The extensions, which should be in lower case.
    */
   public ExtensionFileFilter(String description, String[] extensions) {
      this(description,extensions,/* acceptDirs = */ true);

      // this is the right acceptDirs setting for file choosers.
      // you can't really select directories because of the file selection mode,
      // but you need acceptDirs set to let you see directories and move around.
   }

   public ExtensionFileFilter(String description, String[] extensions, boolean acceptDirs) {
      this.description = description;
      this.extensions = extensions;
      this.acceptDirs = acceptDirs;
   }

   public static void clearFilters(JFileChooser chooser) {
      FileFilter[] filters = chooser.getChoosableFileFilters();
      for (int i=0; i<filters.length; i++) {
         chooser.removeChoosableFileFilter(filters[i]); // ignore result
      }
   }

   public boolean accept(File file) {
      if (file.isDirectory()) return acceptDirs;
      return hasExtension(file.getName(),extensions);
   }

   public static boolean hasExtension(String name, String[] extensions) {
      int dot = name.lastIndexOf('.');
      if (dot == -1) return false;

      String suffix = name.substring(dot+1).toLowerCase();
      for (int i=0; i<extensions.length; i++) {
         if (suffix.equals(extensions[i])) return true;
      }

      return false;
   }

   public String getDescription() {
      StringBuffer buffer = new StringBuffer();

      buffer.append(description);
      buffer.append(" (");

      for (int i=0; i<extensions.length;i ++) {
         if (i != 0) buffer.append(", ");
         buffer.append("*.");
         buffer.append(extensions[i]);
      }
      buffer.append(")");

      return buffer.toString();
   }

}

