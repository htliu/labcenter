/*
 * FormatKodak.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.misc.Op;
import com.lifepics.neuron.object.XML;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Implementation of the Kodak kiosk format.
 */

public class FormatKodak extends Format {

// --- constants ---

   private static final int  STATUS_LENGTH = 1;
   //
   // the NEW, FAIL, SUCC piece describes how LC understands the status;
   // the rest is the literal meaning of the code according to the kiosk.
   //
   private static final char STATUS_NEW_UNVERIFIED = 'U';
   private static final char STATUS_FAIL_ERROR     = 'E';
   private static final char STATUS_FAIL_ABORTED   = 'A';
   private static final char STATUS_SUCC_HELD      = 'H'; // held   for user at kiosk
   private static final char STATUS_SUCC_CLAIMED   = 'C'; // claimed by user at kiosk
   private static final char STATUS_SUCC_DONE      = 'D'; // done by associated minilab

   private static final String ORDERS_DIR = "Orders";

   private static final String COS_FILE   =   "Cos.xml";
   private static final String BEGIN_FILE = "Begin.xml";

   private static final int QUANTITY_MAX = 999; // max print quantity
   private static final int CHUNK_STD = 500;
   private static final int CHUNK_MAX = 999; // same as QUANTITY_MAX

   private static final char ID_IMAGE   = 'I';
   private static final char ID_DETAIL  = 'D';
   private static final char ID_PRODUCT = 'P';

   private static SimpleDateFormat dateFormatKodak = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

// --- subclass hooks ---

   public String getShortName() { return Text.get(this,"s1"); }

   public int[] getAllowedCompletionModes() { return new int[] { COMPLETION_MODE_DETECT }; }
   public int   getCompletionMode(Object formatConfig) { return COMPLETION_MODE_DETECT; }
   public void  setCompletionMode(Object formatConfig, int mode) {}

   // about completion modes ... you may notice that the UI for claimedIsComplete says
   // something that sounds a lot like COMPLETION_MODE_ACCEPT.  what's going on is,
   // we sort of have two different sub-queues, the kiosk and the minilab, and we don't
   // know which one any given job will go to.  on the minilab side, we always detect,
   // but on the kiosk side we have a choice between manual and accept.  we could add
   // a second mode combo (with some work), but even then we'd want to have some text to
   // explain what's going on ... so, I figure it's fine as is.

// --- filename variation ---

   private static Activity kodakActivity = new SubstringActivity(STATUS_LENGTH,0);

   // the activity is a little too broad, some status characters don't occur ...
   // but only a little, because there are a whole bunch of intermediate
   // status characters that I haven't bothered to describe anywhere in here.

// --- format function ---

   public void format(Job job, Order order, Object formatConfig) throws Exception {

      KodakConfig config = (KodakConfig) formatConfig;
      // because of validation, the cast is guaranteed to work

      File useDir = new File(config.dataDir,ORDERS_DIR);
      require(useDir);

      File root = new File(useDir,STATUS_NEW_UNVERIFIED + Convert.fromInt(order.orderID));
      root = vary(root,kodakActivity);

   // (1) plan the operation ; also check that mappings are defined

      LinkedList ops = new LinkedList();
      ops.add(new Op.Mkdir(root));

      LinkedList derefs = new LinkedList();
      // a bit of a kluge here -- we pass this to GenerateCOS, but it's
      // empty.  the objects only get put in afterward.

      // go ahead and generate the COS-XML file first, that way even if
      // there are some errors we won't have wasted time copying images.
      //
      ops.add(new GenerateCOS(new File(root,COS_FILE),derefs,order,config));

      HashSet skus = new HashSet();
      HashSet fset = new HashSet();

      Iterator i = job.refs.iterator();
      while (i.hasNext()) {
         Job.Ref ref = (Job.Ref) i.next();
         if (skus.add(ref.sku) && ! MappingUtil.existsMapping(config.mappings,ref.sku)) missingChannel(ref.sku,order.getFullID());

         Order.Item item = order.getItem(ref);

         boolean isMultiImage = item.isMultiImage();
         List filenames;

         if (isMultiImage) {
            if ( ! config.allowSingleSided ) throw new Exception(Text.get(FormatKodak.class,"e3"));
            filenames = item.filenames;
            collate(derefs,filenames,ref.sku,job.getOutputQuantity(item,ref),config.collateInReverse);
         } else {
            filenames = Collections.singletonList(item.filename);
            derefs.add(new Deref(ref.filename,ref.sku,job.getOutputQuantity(item,ref)));
         }

         Iterator j = filenames.iterator();
         while (j.hasNext()) {
            String filename = (String) j.next();

            // other formats get the item here, but it's not needed
            if (fset.add(filename)) {
               ops.add(new Op.Copy(new File(root,filename),order.getPath(filename)));
               // assumes the image file names don't collide with COS_FILE or BEGIN_FILE
            }
         }
      }

      // this tells the kiosk to begin processing, so it must come last
      //
      ops.add(new GenerateBegin(new File(root,BEGIN_FILE),config)); // no dependence on job info

   // (2) alter files

      Op.transact(ops);

   // (3) alter object

      // the kiosk takes ownership of the folder, but we need to watch for completion
      job.dir = root;
      job.dirOwned = new Boolean(false);
      //
      // not to worry ... even if something went wrong, which it won't,
      // we can't purge the directory because we don't track the files.

      if ( ! config.claimedIsComplete ) job.property = "";
   }

// --- begin-file generation ---

   private static class GenerateBegin extends Op.GenerateXML {

      private KodakConfig config;

      public GenerateBegin(File dest, KodakConfig config) {
         super(dest);
         this.config = config;
      }

      public void subdo(Document doc) throws IOException {
         Node node = XML.createElement(doc,"BeginOrder");
         XML.createElementText(node,"DateTime",dateFormatKodak.format(new Date()));
         XML.createElementText(node,"PreferredMedia",config.beginThermal ? "Thermal" : "Lab");
         XML.createElementText(node,"PreferredFlow",config.beginHold ? "Hold" : "Release");
         XML.addIndentation(node);
      }
   }

// --- COS-file generation ---

   private static class GenerateCOS extends Op.GenerateXML {

      public LinkedList derefs;
      private Order order;
      private KodakConfig config;

      public GenerateCOS(File dest, LinkedList derefs, Order order, KodakConfig config) {
         super(dest,"../../Schema/COS.dtd");
         this.derefs = derefs;
         this.order = order;
         this.config = config;
      }

      public void subdo(Document doc) throws IOException {

         Node nodeOrder = XML.createElement(doc,"COSOrder");
         Node n1, n2;

         // there are some differences in which fields we use,
         // but model this on the code in FormatDLS anyway.

      // top-level stuff

         n1 = XML.createElement(nodeOrder,"COSFileInfo");
         n2 = XML.createElement(n1,"COSFileMod");
         XML.setAttribute(n2,"Application","LifePics LabCenter");
         XML.setAttribute(n2,"COSVersion","1.11");
         XML.setAttribute(n2,"SDKVersion","NotUsed");

         n1 = XML.createElement(nodeOrder,"COSConsumerInfo");
         XML.setAttribute(n1,"ConsumerID",order.email);
         n2 = XML.createElement(n1,"COSAddressInfo");
         XML.setAttribute(n2,"NameLine1",order.nameLast);
         XML.setAttribute(n2,"NameLine2",order.nameFirst);
         XML.setAttribute(n2,"DayPhone",order.phone);

         n1 = XML.createElement(nodeOrder,"COSVendorInfo");
         XML.setAttribute(n1,"VendorOrderNumber",Convert.fromInt(order.orderID));
            // this is what the kiosk uses as the claim ID

      // image ID assignment

         HashMap imageMap = new HashMap(); // filename to numeric ID
         int count = 1;

         HashSet fset = new HashSet();

         Iterator i = derefs.iterator();
         while (i.hasNext()) {
            Deref deref = (Deref) i.next();
            if ( ! fset.add(deref.filename) ) continue;

            String id = Convert.fromInt(count++);
            imageMap.put(deref.filename,id);
         }

      // products

         count = 1;

         i = derefs.iterator();
         while (i.hasNext()) {
            Deref deref = (Deref) i.next();

            KodakMapping m = (KodakMapping) MappingUtil.getMapping(config.mappings,deref.sku);

            Iterator k = new ChunkIterator(deref.outputQuantity,CHUNK_STD,CHUNK_MAX,/* hasSequence = */ false);
            while (k.hasNext()) {
               int quantity = ((Integer) k.next()).intValue();

               if (quantity > QUANTITY_MAX) throw new IOException(Text.get(FormatKodak.class,"e1",new Object[] { Convert.fromInt(QUANTITY_MAX) }));
               // should always pass now, but check anyway

               n1 = XML.createElement(nodeOrder,"COSProduct");
               XML.setAttribute(n1,"ProductName",ID_PRODUCT + Convert.fromInt(count++));
               XML.setAttribute(n1,"ProductDescription",m.product);
               n2 = XML.createElement(n1,"COSPrintInfo");
               XML.setAttribute(n2,"BackPrintMessage",Convert.fromInt(order.orderID));
               //
               // the backprint isn't displayed in normal kiosk printing, but apparently
               // it will show up if the order is sent to a minilab, and in that case we
               // want it to be something that identifies the order.
               // so, this backprint is not user-configurable, just correctly hard-coded.
               //
               if (m.surface != null) XML.setAttribute(n2,"MediaSurface",Convert.fromInt(Math.abs(m.surface.intValue())));
               if (m.border  != null) XML.setAttribute(n2,"PrintBorder",zeroOne(m.border.booleanValue()));
               //
               n2 = XML.createElement(n1,"COSFinishingInfo");
               XML.setAttribute(n2,"Quantity",Convert.fromInt(quantity));
               n2 = XML.createElement(n1,"ImageDetailRef");
               XML.setAttribute(n2,"ImageDetail",ID_DETAIL + (String) imageMap.get(deref.filename));
            }
         }

      // image details

         // just using the same iteration scheme out of habit, here.
         // we could equally well just go from 1 up to whatever the
         // max count was, if we'd saved it.

         fset.clear();

         i = derefs.iterator();
         while (i.hasNext()) {
            Deref deref = (Deref) i.next();
            if ( ! fset.add(deref.filename) ) continue;

            String id = (String) imageMap.get(deref.filename);

            n1 = XML.createElement(nodeOrder,"COSImageDetail");
            XML.setAttribute(n1,"ImageDetailID",ID_DETAIL + id);
            n2 = XML.createElement(n1,"COSDigitalInfo");
            XML.setAttribute(n2,"ImageLocation","./");
            n2 = XML.createElement(n1,"ImageRef");
            XML.setAttribute(n2,"Image",ID_IMAGE + id);
         }

      // images

         fset.clear();

         i = derefs.iterator();
         while (i.hasNext()) {
            Deref deref = (Deref) i.next();
            if ( ! fset.add(deref.filename) ) continue;

            String id = (String) imageMap.get(deref.filename);

            n1 = XML.createElement(nodeOrder,"COSImage");
            XML.setAttribute(n1,"FileName",deref.filename);
            XML.setAttribute(n1,"ImageID",ID_IMAGE + id);
         }

      // finish up

         XML.addIndentation(nodeOrder);
      }
   }

   private static String zeroOne(boolean b) { return b ? "1" : "0"; }

// --- completion ---

   private static boolean transformable(String name) {
      return name.length() > 0 && name.charAt(0) == STATUS_NEW_UNVERIFIED;
      // just a small sanity check before we start changing it
   }
   private static String transform(String name, char status) {
      return status + name.substring(STATUS_LENGTH);
      // you should check transformable before calling this
   }

   private static boolean transformable(File dir) {
      return transformable(dir.getName());
   }
   private static File transform(File dir, char status) {
      return new File(dir.getParentFile(),transform(dir.getName(),status));
   }

   // this is an unusual case ... we want to watch the folder
   // to see if completion happens, but we don't own it,
   // so we don't ever want to mark it complete ourselves.
   //
   // the easiest way to do that is to override isComplete
   // (we'd have to anyway, since there are multiple options)
   // but leave getComplete returning null to say don't mark

   // note about held and claimed statuses:
   // there are two workflows for the thermal printer.
   //
   // in one, an operator completes jobs manually,
   // and we totally ignore held and claimed statuses.
   //
   // in the other, the kiosk is used standalone.
   // in that case, we want to call the order complete
   // as soon as it's held, so that an email will
   // go out, telling the customer to come claim it.
   // the claimed status is tested only as a backup,
   // in case the order somehow gets there directly
   // without first being in held status.

   private static final char[] statusArray = { STATUS_FAIL_ERROR,
                                               STATUS_FAIL_ABORTED,
                                               STATUS_SUCC_HELD,
                                               STATUS_SUCC_CLAIMED,
                                               STATUS_SUCC_DONE };
   private static final boolean[] successArray = { false, false, true, true, true };

   private static class ScanResult {
      public boolean success;
      public File transformed;
   }

   public ScanResult scanComplete(File dir, String property) {

      if (dir.exists()) return null; // still there, not complete
      if ( ! transformable(dir) ) return null; // don't know how to mark (shouldn't happen)

   // look for possible transformations

      File transformed = null;
      int foundIndex = -1; // for compiler's benefit only

      for (int i=0; i<statusArray.length; i++) {
         File temp = transform(dir,statusArray[i]);
         if (temp.exists()) {
            if (transformed != null) return null; // ambiguous result
            transformed = temp;
            foundIndex = i;
         }
      }

      if (transformed == null) return null; // dir has gone missing

   // if claimed is not complete, ignore status, as explained above

      // property set when claimedIsComplete is false
      if (property != null) {
         char status = statusArray[foundIndex];
         if (    status == STATUS_SUCC_HELD
              || status == STATUS_SUCC_CLAIMED ) return null;
      }

      // note on why this is implemented as a property field on the job
      // instead of by just looking up the queue config object ...
      // it's the config at the time the job was created that matters,
      // not the current config.  the queue could be pointed at
      // a different kiosk now, or could be a different integration type;
      // that doesn't mean we should suddenly notify the user.

   // found a unique transformed dir, now what?  caller will decide

      ScanResult result = new ScanResult();
      result.success = successArray[foundIndex];
      result.transformed = transformed;

      return result;
   }

   public File isComplete(File dir, String property) {
      ScanResult result = scanComplete(dir,property);
      if (result == null) return null;
      if (result.success) return result.transformed;
      else return null; // error, ergo not complete
   }

   public File isCompleteOrError(File dir, String property) throws Exception {
      ScanResult result = scanComplete(dir,property);
      if (result == null) return null;
      if (result.success) return result.transformed;
      else throw new Exception(Text.get(this,"e2",new Object[] { result.transformed.getName() }));
   }

}

