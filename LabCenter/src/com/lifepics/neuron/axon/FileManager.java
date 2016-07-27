/*
 * FileManager.java
 */

package com.lifepics.neuron.axon;

import com.lifepics.neuron.misc.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.ListIterator;

/**
 * A utility class for manipulating the files in a roll.
 */

public class FileManager {

   /**
    * Get the total size of all files in a roll.
    */
   public static long getTotalSize(Roll roll) throws IOException {
      long total = 0;

      ListIterator li = roll.items.listIterator();
      while (li.hasNext()) {
         Roll.Item item = (Roll.Item) li.next();
         File file = new File(roll.rollDir,item.filename);
         total += FileUtil.getSize(file);
      }

      return total;
   }

   /**
    * Purge a roll by removing the data files and the directory.
    *
    * @return True if everything was successfully removed.
    */
   public static boolean purgeFiles(Roll roll) {

      boolean isOrderUpload = (roll.source == Roll.SOURCE_HOT_ORDER);

      int size = roll.items.size() + roll.extraFiles.size();
      if (isOrderUpload) size++;

      Object[] names = new Object[size];
      int i = 0;

      ListIterator li = roll.items.listIterator();
      while (li.hasNext()) {
         Roll.Item item = (Roll.Item) li.next();
         names[i++] = item.filename;
      }

      li = roll.extraFiles.listIterator();
      while (li.hasNext()) {
         String name = (String) li.next();
         File file = new File(name);
         names[i++] = file.isAbsolute() ? (Object) file : (Object) name;
      }

      if (isOrderUpload) names[i++] = Roll.UPLOAD_FILE;

      boolean rmdir = (roll.source != Roll.SOURCE_LOCAL);
      return FileUtil.purge(roll.rollDir,names,/* warn = */ true,rmdir);
   }

}

