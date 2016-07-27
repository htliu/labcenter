/*
 * XMLPoller.java
 */

package com.lifepics.neuron.axon;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Log;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.object.Relative;
import com.lifepics.neuron.object.XML;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Level;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * A poller that accepts XML specification files.
 */

public class XMLPoller extends PrefixPoller {

   public XMLPoller() {
      super(/* wantDirectories = */ false);
   }

   public boolean accept(File file) {
      return super.accept(file) && endsWithIgnoreCase(file.getName(),".xml");
   }

   protected int processRenamed(File original, File file, PollCallback callback) throws Exception {

      File base = file.getParentFile();
      int rollID = 0; // guaranteed to be replaced, but compiler can't understand that

      Iterator i = getRolls(file);
      if ( ! i.hasNext() ) throw new Exception(Text.get(this,"e3")); // check, else rollID might not get initialized
      while (i.hasNext()) {
         Node node = (Node) i.next();
         callback.throttle();
         rollID = callback.submitWithItems(parse(node,base,callback),RollManager.METHOD_COPY,null); // only last ID will come through
      }

      return rollID;
   }
   // there's a big flaw here, which is that if we get interrupted in the middle of a large file,
   // the file is already renamed to its temporary name, and won't be processed again;
   // and we don't even know where we were in the iteration.  all that is why there's XMLPollerLarge.

// --- iterate ---

   /**
    * Iterate over all nodes that contain rolls, regardless of whether there are one or many.
    */
   public static Iterator getRolls(File file) throws Exception {

      Document doc = XML.readFile(file);

      Log.log(Level.INFO,XMLPoller.class,"i1"); // finished parsing

      Node node = XML.getElementTry(doc,"Rolls");
      if (node != null) { // multiple rolls

         return XML.getElements(node,"Roll");

      } else { // single roll

         node = XML.getElement(doc,"Roll"); // must be there

         // now, iterate over one element
         LinkedList list = new LinkedList();
         list.add(node);
         return list.iterator();
      }
   }

// --- parse ---

   public static Roll parse(Node node, File base, PollCallback callback) throws Exception {

      Roll roll = new Roll();

   // main fields

      roll.source = Roll.SOURCE_HOT_XML;

      roll.email = XML.getNullableText(node,"Email"); // roll will pause if null or empty
      if (roll.email == null) roll.email = "";

   // optional fields

      roll.nameFirst = XML.getNullableText(node,"First");
      roll.nameLast = XML.getNullableText(node,"Last");
      roll.street1 = XML.getNullableText(node,"Street1");
      roll.street2 = XML.getNullableText(node,"Street2");
      roll.city = XML.getNullableText(node,"City");
      roll.state = XML.getNullableText(node,"State");
      roll.zipCode = XML.getNullableText(node,"ZIP");
      roll.phone = XML.getNullableText(node,"Phone");
      roll.country = XML.getNullableText(node,"Country");
      roll.album = Roll.getValidAlbum(XML.getNullableText(node,"Album"));

      roll.notify = Convert.toNullableBool(XML.getNullableText(node,"Notify"));
      roll.promote = Convert.toNullableBool(XML.getNullableText(node,"emailpromotions"));

      roll.password = XML.getNullableText(node,"password");

   // files

      // note about order of files and subalbums: if you have a poller
      // that's reading folders, it makes sense for the files and
      // subalbums to be put into alphabetical order, but here we have
      // an order that's given to us by the XML file, which might be
      // different from alphabetical order for a reason.  the UI allows
      // arbitrary ordering of files and folders, so we should too.
      //
      // it would be polite to regroup the files by subalbum without
      // changing the relative order of files or subalbums, but that
      // would be hard, and it works fine as is.

      Iterator i = XML.getElements(node,"File");
      while (i.hasNext()) {
         Node n = (Node) i.next();
         String filename = XML.getText(n);

         if (callback.ignore(filename)) {
            Log.log(Level.WARNING,XMLPoller.class,"e2",new Object[] { filename });
            continue;
            // again, this is weak, but it's what they want
         }

         File f = new File(filename);
         f = Relative.makeRelativeTo(base,f);

         if (f.exists()) {

            Roll.PreItem item = ItemUtil.makeItem(f);
            parseFileAttributes(n,item);
            roll.items.add(item);

         } else {
            Log.log(Level.WARNING,XMLPoller.class,"e1",new Object[] { filename });
            // file not found, log and ignore ...
            // I think this is weak, but it's what they want
         }
      }

   // done

      return roll;
   }

   public static void parseFileAttributes(Node n, Roll.PreItem item) throws Exception {

      String name = XML.getAttributeTry(n,"Name");
      if (name != null) {
         item.setOriginalFilename(name);
      }

      String subalbum = XML.getAttributeTry(n,"Subalbum");
      if (subalbum != null) {
         item.subalbum = subalbum;
      }
      // we don't really need the "if", and we could also
      // set the subalbum by passing it to makeItem, but
      // I think this structure is easier to make sense of.
   }

}

