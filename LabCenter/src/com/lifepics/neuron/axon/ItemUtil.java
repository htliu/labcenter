/*
 * ItemUtil.java
 */

package com.lifepics.neuron.axon;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.misc.ExtensionFileFilter;
import com.lifepics.neuron.misc.FileUtil;
import com.lifepics.neuron.misc.Op;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

import javax.swing.filechooser.FileFilter;

import org.w3c.dom.Document;

/**
 * A utility class for manipulating items and preitems,
 * handling duplicates and subalbum paths correctly.
 */

public class ItemUtil {

// --- file filter ---

   // now that we upload everything as JPEG, every file type that appears here
   // should be one that imageio is capable of processing; see UploadTransform

   public static String description = Text.get(ItemUtil.class,"s1");
   public static String[] extensions = new String[] { "bmp","jpeg","jpg","png","tif","tiff" };

   public static ExtensionFileFilter imageFileFilter = new ExtensionFileFilter(description,extensions);
   public static ExtensionFileFilter imageOnlyFilter = new ExtensionFileFilter(description,extensions,/* acceptDirs = */ false);

// --- preitem construction ---

   // note, originalFilename is only set by disambiguate or user edit,
   // or now by XML poller when there are original file names there.

   public static Roll.PreItem makeItem(File file) {
      return makeItem(file,null); // subalbum stays null in this chain
   }
   public static Roll.PreItem makeItem(File file, String subalbum) {

      Roll.PreItem item = new Roll.PreItem();

      item.filename = file.getName();
      item.status = Roll.STATUS_FILE_PENDING;
      item.subalbum = subalbum;
      item.file = file;

      return item;
   }

   public static Roll.Item makeItemScanned(File file) {

      Roll.Item item = new Roll.Item();

      item.filename = file.getName();
      item.status = Roll.STATUS_FILE_PENDING;

      return item;
   }

   public static LinkedList makeItems(File dir) {
      return makeItems(dir,null);
   }
   public static LinkedList makeItems(File dir, String subalbum) {
      LinkedList list = new LinkedList();
      makeItemsInto(list,dir,subalbum);
      return list;
   }

   public static void makeItemsInto(LinkedList list, File dir) {
      makeItemsInto(list,dir,null);
   }
   public static void makeItemsInto(LinkedList list, File dir, String subalbum) {

      File[] files = dir.listFiles();
      if (files == null) return;
      FileUtil.sortByName(files);

      for (int i=0; i<files.length; i++) {
         if (files[i].isDirectory()) {
            String s = new File(subalbum,files[i].getName()).getPath();
            makeItemsInto(list,files[i],s);
            // note file constructor handles null subalbum correctly
         } else {
            if (imageFileFilter.accept(files[i])) list.add(makeItem(files[i],subalbum));
         }
      }
   }

// --- disambiguation ---

   // here are the disambiguate functions I know about.
   // I just finished making them all case-insensitive.
   //
   // FileUtil.disambiguate
   // ItemUtil.disambiguate
   // OrderParser.disambiguate (two variants)
   // Format activities (only SubstringFilter was case-sensitive)
   // Format.LengthLimit

   /**
    * Take a mixed list of items and preitems and disambiguate
    * all the preitems (within the context of the given list).
    * This is where all non-null originalFilename values come from!
    * (Except, see comment above, there are other methods now.)
    */
   public static void disambiguate(LinkedList items) {

      HashSet set = new HashSet();
      //
      // we can't tell the hash set to be case-insensitive, but we can
      // get the same effect by converting to lower case before adding.

      Iterator i = items.iterator();
      while (i.hasNext()) {
         Object o = i.next();
         if (o instanceof Roll.PreItem) continue; // careful, preitems <i>are</i> instances of items!
         Roll.Item item = (Roll.Item) o;

      // got item

         set.add(item.filename.toLowerCase()); // don't validate uniqueness, nothing we can do about it here
      }

      i = items.iterator();
      while (i.hasNext()) {
         Object o = i.next();
         if ( ! (o instanceof Roll.PreItem) ) continue;
         Roll.Item item = (Roll.Item) o; // we don't actually use preitem-ness, it's just a marker

      // got preitem

         disambiguateItem(set,item);
      }
   }

   /**
    * An internal function that I've factored out for reuse in order upload.
    *
    * @param set The set of names already in use.
    * @param item An item to be disambiguated relative to the set, and then added.
    */
   public static void disambiguateItem(HashSet set, Roll.Item item) {

      if (set.add(item.filename.toLowerCase())) return; // unique, added to set

      // start with n=2 so that we get dog.jpg, dog_2.jpg, etc.
      String s;
      for (int n=2; ; n++) {
         s = FileUtil.vary(item.filename,n);
         if (set.add(s.toLowerCase())) break;
      }
      // set is finite, this is guaranteed to terminate

      // don't change originalFilename.  usually that means converting null
      // to non-null, but the reverse could happen too, at least in theory.

      String save = item.getOriginalFilename();
      item.filename = s;
      item.setOriginalFilename(save);

      // last add attempt also added the name to the set
   }

// --- item matching ---

   private static boolean isItem(Object o) {
      return ( ! (o instanceof Roll.PreItem) );
      //
      // what's important is not that the object is an item
      // (they're all items), but that it's not a preitem
   }

   private static boolean matches(Object o1, Object o2) {
      Roll.Item item1 = (Roll.Item) o1;
      Roll.Item item2 = (Roll.Item) o2;
      return (item1.filename.equals(item2.filename));
      //
      // since we are only looking at items, not preitems,
      // the filenames have already been disambiguated,
      // so that's sufficient to tell if two items are the same.
      // we could compare the other fields, they should match,
      // but maybe one day someone will want to be able to edit
      // the subalbum or something, let's not break that here.
      //
      // about the status matching: if the status isn't pending,
      // then upload has started, and you can't edit the roll.
   }

   private static boolean removeMatchingItem(LinkedList list, Object item) {
      ListIterator li = list.listIterator();
      while (li.hasNext()) {
         Object o = li.next();
         if (isItem(o) && matches(o,item)) { li.remove(); return true; }
      }
      return false;
   }

   public static void removeMatchingItems(LinkedList list1, LinkedList list2) {
      ListIterator li = list1.listIterator();
      while (li.hasNext()) {
         Object o = li.next();
         if (isItem(o) && removeMatchingItem(list2,o)) li.remove();
      }
   }

// --- utility function ---

   public static String[] itemsToStrings(LinkedList items) {
      String[] files = new String[items.size()];
      int i = 0;

      ListIterator li = items.listIterator();
      while (li.hasNext()) {
         files[i++] = ((Roll.Item) li.next()).filename;
      }
      return files;
   }

// --- item conversion ---

   public static void replacePreitems(LinkedList items) {
      ListIterator li = items.listIterator();
      while (li.hasNext()) {
         Object o = li.next();
         if ( ! (o instanceof Roll.PreItem) ) continue;

         li.set(new Roll.Item((Roll.PreItem) o));
      }
   }

// --- transfer functions ---

   // these are the same as the corresponding functions in FileUtil,
   // except they take preitems instead of files so that the source
   // and destination file names can be different.

   /**
    * Create a new directory and copy files into it, as atomically as possible.
    */
   public static void copyToNewDirectory(File dir, LinkedList items, int method, Document order) throws IOException {
      LinkedList ops = new LinkedList();

      ops.add(new Op.Mkdir(dir));

      if (order != null) ops.add(new Op.WriteXML(new File(dir,Roll.UPLOAD_FILE),order));

      Iterator i = items.iterator();
      while (i.hasNext()) {
         Roll.PreItem item = (Roll.PreItem) i.next();

         File dest = new File(dir,item.filename);
         File src  = item.file;

         Op op;
         switch (method) {
         case RollManager.METHOD_COPY:             op = new Op.Copy         (dest,src);  break;
         case RollManager.METHOD_MOVE:             op = new Op.Move         (dest,src);  break;
         case RollManager.METHOD_COPY_AND_DELETE:  op = new Op.CopyAndDelete(dest,src);  break;
         default: throw new IllegalArgumentException();
         }
         ops.add(op);
      }

      Op.transact(ops);
   }

   /**
    * Alter an existing directory by removing some files and copying in others,
    * as atomically as possible, using a temp directory for rollback.
    *
    * @param tempDir The temporary directory, which should <i>not exist</i>.
    */
   public static void alter(File dir, File tempDir, String[] removeFiles, LinkedList addItems) throws IOException {
      LinkedList ops = new LinkedList();

      if (removeFiles.length > 0) { // don't make temp dir unnecessarily

         ops.add(new Op.TempMkdir(tempDir));

         for (int i=0; i<removeFiles.length; i++) {
            ops.add(new Op.TempMove(new File(tempDir,removeFiles[i]),new File(dir,removeFiles[i])));
         }
      }

      Iterator i = addItems.iterator();
      while (i.hasNext()) {
         Roll.PreItem item = (Roll.PreItem) i.next();
         ops.add(new Op.Copy(new File(dir,item.filename),item.file));
      }

      Op.transact(ops);
   }

}

