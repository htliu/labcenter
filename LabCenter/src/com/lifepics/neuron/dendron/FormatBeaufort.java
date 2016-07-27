/*
 * FormatBeaufort.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.misc.Op;
import com.lifepics.neuron.object.XML;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Implementation of the Beaufort format.
 */

public class FormatBeaufort extends Format {

// --- constants ---

   private static final int    PREFIX_LENGTH = 1;
   private static final String PREFIX_Q = "q"; // only used within transaction
   private static final String PREFIX_O = "o";
   private static final String PREFIX_X = "x";

// --- subclass hooks ---

   public String getShortName() { return Text.get(this,"s1"); }

   public int[] getAllowedCompletionModes() { return new int[] { COMPLETION_MODE_DETECT }; }
   public int   getCompletionMode(Object formatConfig) { return COMPLETION_MODE_DETECT; }
   public void  setCompletionMode(Object formatConfig, int mode) {}

// --- filename variation ---

   private static Activity beaufortActivity = new SubstringActivity(PREFIX_LENGTH,0);

   // the activity is probably too broad, but do it this way in case it turns out
   // to be convenient for the receiver to use other prefixes to track the status

// --- format function ---

   public void format(Job job, Order order, Object formatConfig) throws Exception {

      BeaufortConfig config = (BeaufortConfig) formatConfig;
      // because of validation, the cast is guaranteed to work

      require(config.dataDir);

   // (1) pre-generate XML document

      Document doc = OrderParser.readFile(new File(order.orderDir,Order.ORDER_FILE));
      Node node = XML.getElement(doc,OrderParser.NAME_ORDER);

      int version = OrderParser.getVersion(node);

      XML.removeWhitespace(node); // whitespace is a tough call, here,
      // but since I'm adding tags, I'd better fix the whitespace too.

      XML.createInitialText(node,"LCImagePath",Convert.fromFile(order.orderDir));

      Iterator i = OrderParser.getItems(node);
      while (i.hasNext()) {
         Node item = (Node) i.next();

         Iterator j = OrderParser.getImages(item,version);
         while (j.hasNext()) {
            Node image = (Node) j.next();

            String downloadURL = OrderParser.getDownloadURL(image);
            Order.OrderFile file = order.findFileByDownloadURL(downloadURL);
            if (file == null) throw new Exception(Text.get(this,"e1",new Object[] { downloadURL }));

            // in these format handlers we often use the result of findFileByFilename
            // without checking whether it's null, but I don't like doing that here.

            XML.createInitialText(image,"LCImageName",file.filename);
         }
      }

      XML.addIndentation(node);

   // (2) figure out root directory

      File root = new File(config.dataDir,PREFIX_O + Convert.fromInt(order.orderID));
      root = vary(root,beaufortActivity);
      LinkedList files = new LinkedList();

      // now that the root has been varied, swap the prefix
      File rootFinal = root;
      root = new File(root.getParentFile(),PREFIX_Q + root.getName().substring(PREFIX_LENGTH));

   // (3) plan the operation

      LinkedList ops = new LinkedList();
      ops.add(new Op.Mkdir(root));

      String file = Order.ORDER_FILE;
      files.add(file);
      ops.add(new Op.WriteXML(new File(root,file),doc));

      ops.add(new Op.Move(rootFinal,root));

   // (4) alter files

      Op.transact(ops);

   // (5) alter object

      job.dir = rootFinal;
      job.files = files;
   }

// --- completion ---

   private String transform(String s) {
      return s.startsWith(PREFIX_O) ? PREFIX_X + s.substring(PREFIX_LENGTH) : null;
   }

   protected File getComplete(File dir, String property) {

      String name = transform(dir.getName());
      if (name == null) return null; // don't know how to mark

      return new File(dir.getParentFile(),name);
   }

}

