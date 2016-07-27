/*
 * AlternatingFileStorage.java
 */

package com.lifepics.neuron.table;

import com.lifepics.neuron.core.ValidationException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Persistent storage implemented as a set of alternating files in a single directory.
 */

public class AlternatingFileStorage implements Storage {

// --- fields ---

   private File dir;
   private String suffix;

// --- construction ---

   /**
    * @param suffix An optional suffix to be applied to the file names, dot not included.
    */
   public AlternatingFileStorage(File dir, String suffix) {
      this.dir = dir;
      this.suffix = (suffix != null) ? ('.' + suffix) : null;
   }

// --- implementation of Storage ---

   /**
    * Get a list of all the keys of objects that exist in the storage.
    */
   public String[] list() {

      HashSet set = AlternatingFile.list(dir);
      Iterator i;

      // remove files that don't have the right suffix
      if (suffix != null) {
         i = set.iterator();
         while (i.hasNext()) {
            String name = (String) i.next();
            if ( ! name.endsWith(suffix) ) i.remove();
         }
      }

      String[] keys = new String[set.size()];
      int j = 0;

      i = set.iterator();
      while (i.hasNext()) {
         String name = (String) i.next();
         if (suffix != null) name = name.substring(0,name.length() - suffix.length());
         keys[j++] = name;
      }

      return keys;
   }

   private AlternatingFile getFile(String key) {
      String name = (suffix != null) ? (key + suffix) : key;
      return new AlternatingFile(new File(dir,name));
   }

   /**
    * Load the object from the space identified by the given key.
    */
   public Object load(String key, TableAdapter adapter) throws IOException, ValidationException {
      AlternatingFile af = getFile(key);
      try {
         InputStream inputStream = af.beginRead();
         return adapter.load(inputStream);
         // strange but true: the finally clause runs even if we return
      } finally {
         af.endRead();
      }
   }

   /**
    * Store an object into the space identified by the given key.
    */
   public void store(String key, Object o, TableAdapter adapter) throws IOException {
      AlternatingFile af = getFile(key);
      try {
         OutputStream outputStream = af.beginWrite();
         adapter.store(outputStream,o);
         af.commitWrite();
      } finally {
         af.endWrite();
      }
   }

   /**
    * Delete the object from the space identified by the given key.
    */
   public void delete(String key) throws IOException {
      AlternatingFile af = getFile(key);
      af.delete();
   }

}

