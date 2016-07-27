/*
 * FormatTree.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.misc.Op;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Implementation of the tree format.
 */

public class FormatTree extends Format {

// --- subclass hooks ---

   public String getShortName() { return Text.get(this,"s1"); }

   public int[] getAllowedCompletionModes() { return new int[] { COMPLETION_MODE_MANUAL, COMPLETION_MODE_DETECT }; }
   public int   getCompletionMode(Object formatConfig) { return ((ManualConfig) formatConfig).enableCompletion ? COMPLETION_MODE_DETECT : COMPLETION_MODE_MANUAL; }
   public void  setCompletionMode(Object formatConfig, int mode) { ((ManualConfig) formatConfig).enableCompletion = (mode == COMPLETION_MODE_DETECT); }

// --- format function ---

   public void format(Job job, Order order, Object formatConfig) throws Exception {

      ManualConfig config = (ManualConfig) formatConfig;
      // because of validation, the cast is guaranteed to work

      requireOrAutoCreate(config.dataDir,config.autoCreate);

      File root = new File(config.dataDir,order.getFullID()); // use full ID because manual
      root = vary(root);
      LinkedList files = new LinkedList();

   // (1) plan the operation

      LinkedList ops = new LinkedList();
      ops.add(new Op.Mkdir(root));

      HashMap map = new HashMap(); // map from dir name to set of files

      Iterator i = job.refs.iterator();
      while (i.hasNext()) {
         Job.Ref ref = (Job.Ref) i.next();
         Order.Item item = order.getItem(ref);

         // the original idea was that because (filename,SKU) is the primary key
         // for items, files in a SKU-directory will necessarily not collide.
         // that fails now for two reasons, which is why we do the hash map stuff.
         //
         // * one, the file names of multi-image items are not guaranteed to be unique.
         // * two, toString and expunge are not one-to-one mappings, so different SKUs
         //   can in theory produce the same directory name.
         //
         // there's still a weakness here, which is that if the multi-image items
         // really aren't unique, it's hard to tell what to do from the files.
         // I don't have any solution to that yet, but in practice they are unique.

         String dir = Convert.fromInt(job.getOutputQuantity(item,ref)) + " - " + expunge(item.sku.toString());
         HashSet fset = (HashSet) map.get(dir);
         if (fset == null) {
            fset = new HashSet();
            map.put(dir,fset);
            files.add(dir);
            ops.add(new Op.Mkdir(new File(root,dir)));
         }

         List filenames = item.isMultiImage() ? item.filenames : Collections.singletonList(item.filename);
         Iterator j = filenames.iterator();
         while (j.hasNext()) {
            String filename = (String) j.next();
            if (fset.add(filename)) {
               String file = new File(dir,filename).getPath();
               files.add(file);
               ops.add(new Op.Copy(new File(root,file),order.getPath(filename)));
            }
         }
      }

      if (config.enableCompletion) {
         String file = COMPLETION_FILE; // no subdirectory
         files.add(file);
         ops.add(new Op.MakeEmpty(new File(root,file)));
      }

   // (2) alter files

      Op.transact(ops);

   // (3) alter object

      job.dir = root;
      job.files = files;

      if (config.enableCompletion) {
         job.property = COMPLETION_FILE;
      }
   }

// --- completion ---

   public static String COMPLETION_FILE = Text.get(FormatTree.class,"s2");

   public File isComplete(File dir, String property) {

      if (property == null) return null; // no completion

      if (dir.exists() && ! new File(dir,property).exists()) {
         return dir;
      } else {
         return null;
      }
   }

}

