/*
 * FormatHP.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.misc.Op;
import com.lifepics.neuron.object.XML;
import com.lifepics.neuron.struct.SKU;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Implementation of the HP format.
 */

public class FormatHP extends Format {

// --- constants ---

   private static final String LAYOUT_PREFIX = "Layout";
   private static final String LAYOUT_SUFFIX = ".xml";

   private static final String DIR_STATUS  = "Status";

   private static final String FILE_GENERATE_SUFFIX = ".xml";
   private static final String FILE_STATUS_PREFIX = "OMS-";
   private static final String FILE_STATUS_SUFFIX = ".xml";

   private static SimpleDateFormat dateFormat1 = new SimpleDateFormat("dd.MM.yyyy"); // European order
   private static SimpleDateFormat dateFormat2 = new SimpleDateFormat("HH:mm:ss");

// --- subclass hooks ---

   public String getShortName() { return Text.get(this,"s1"); }

   public int[] getAllowedCompletionModes() { return new int[] { COMPLETION_MODE_DETECT }; }
   public int   getCompletionMode(Object formatConfig) { return COMPLETION_MODE_DETECT; }
   public void  setCompletionMode(Object formatConfig, int mode) {}

// --- layout helpers ---

   /**
    * A holder for products that have layouts.
    */
   private static class Layout {

      public String name; // prefix plus number
      public SKU sku;
      public LinkedList filenames;
      public int quantity;
   }

   private static void makeLayout(LinkedList layouts, Order.Item item, int quantity) {
      Layout layout = new Layout();

      layout.name = LAYOUT_PREFIX + Convert.fromInt(layouts.size() + 1);
      layout.sku = item.sku;
      layout.filenames = item.filenames; // sharing is fine
      layout.quantity = quantity;

      layouts.add(layout);
   }

// --- format function ---

   public void format(Job job, Order order, Object formatConfig) throws Exception {

      HPConfig config = (HPConfig) formatConfig;
      // because of validation, the cast is guaranteed to work

      require(config.dataDir);

      Convert.validateUsable(config.useDigits,order.orderID); // a formality

      File root = new File(config.dataDir,config.prefix + Convert.fromIntNDigit(config.useDigits,order.orderID));
      root = vary(root,new PrefixVariation(config.prefix.length()));
      LinkedList files = new LinkedList();

      LengthLimit lengthLimit = new LengthLimit(true,NO_LIMIT,/* exclude = */ true);

   // (0) precompute the set of layout SKUs

      // we check layout-SKU-ness for every job ref, not just every unique SKU,
      // so we want it to be faster than a linear search.

      HashSet layoutSkus = new HashSet();

      // actually with patterns we can't precompute this, but we can compute it
      // on demand as individual SKUs are examined below.

   // (1) plan the operation ; also check that mappings are defined

      LinkedList ops = new LinkedList();
      ops.add(new Op.Mkdir(root));

      HashSet skus = new HashSet();
      HashSet fset = new HashSet();

      LinkedList useRefs = new LinkedList();
      LinkedList layouts = new LinkedList();

      Iterator i = job.refs.iterator();
      while (i.hasNext()) {
         Job.Ref ref = (Job.Ref) i.next();

         boolean isSpecialSKU = ref.sku.equals(order.specialSKU);

         if (skus.add(ref.sku) && ! isSpecialSKU) {

            HPMapping m = (HPMapping) MappingUtil.getMapping(config.mappings,ref.sku);
            if (m == null) missingChannel(ref.sku,order.getFullID());

            if (m.layoutWidth != null && m.layoutHeight != null) layoutSkus.add(ref.sku);
            // don't worry about the single-null case, it's prevented by a validation
         }
         // as with Fuji, we don't need a mapping for the CD SKU.
         // one tricky point ... logically, to make layoutSkus be fully computed,
         // isSpecialSKU should go in the missingChannel test, not the main test,
         // but then we'd need a m != null check in the layoutSkus test,
         // and we really shouldn't be looking at the special SKU mapping anyway.
         // the only effect would be to let multi-image products that are somehow
         // also CD products flow through, and to block any CD products where the
         // irrelevant mapping has layout information.

         Order.Item item = order.getItem(ref);
         if (item.isMultiImage()) {

            if ( ! layoutSkus.contains(ref.sku) ) throw new Exception(Text.get(this,"e1",new Object[] { ref.sku.toString() }));
            makeLayout(layouts,item,job.getOutputQuantity(item,ref));
            // no regular copy operation for these files, or at least not via this ref

         } else {

            if (   layoutSkus.contains(ref.sku) ) throw new Exception(Text.get(this,"e2",new Object[] { ref.sku.toString() }));
            if ( ! isSpecialSKU ) {
               useRefs.add(ref);
               if (fset.add(ref.filename)) {
                  String file = lengthLimit.transform(ref.filename);
                  files.add(file);
                  ops.add(new Op.Copy(new File(root,file),order.getPath(ref.filename)));
               }
            }
            // soon we'll be changing Opie so that CDs use original images instead of
            // cropped / whitespaced / resized ones.  what happens with HP?
            //
            // well, possibly we could have gotten it to work.  there's a tiny chance
            // that HP might burn CDs using the entire contents of the job directory and
            // not just the images in the job file, in which case we could split out
            // the CD into a separate job as with FujiNew, copy all the files, and voila.
            //
            // but, assume that's not the case, then the HP CD system is already broken:
            // what you get on the CD is whatever other images you happen to print
            // at the same time.  it only works if (1) you only use one integration, and
            // (2) you always print or reprint the entire order.  so, those are the only
            // situations we need to cover.
            //
            // I used to copy the CD images even if they weren't listed in the job
            // file, but I just took that out.  under the assumption, no effect.
            //
            // now, what happens when we start sending original images?  nothing.
            // we still get a CD with the cropped images.  that's not great, but
            // it's all we can do.
            //
            // in fact, Ken says HP doesn't support CDs any more, so it's academic,
            // but I imagine someone somewhere might have gotten it to work before.
         }
      }

      i = layouts.iterator();
      while (i.hasNext()) {
         Layout layout = (Layout) i.next();

         fset.clear();

         files.add(layout.name);
         ops.add(new Op.Mkdir(new File(root,layout.name)));

         Iterator j = layout.filenames.iterator();
         while (j.hasNext()) {
            String filename = (String) j.next();

            if (fset.add(filename)) {
               String file = new File(layout.name,lengthLimit.transform(filename)).getPath();
               files.add(file);
               ops.add(new Op.Copy(new File(root,file),order.getPath(filename)));
            }
            // duplicate filenames are unlikely, but they're easy to handle
         }

         HPMapping m = (HPMapping) MappingUtil.getMapping(config.mappings,layout.sku);

         String file = new File(layout.name,layout.name + LAYOUT_SUFFIX).getPath(); // not transformed, see (**)
         files.add(file);
         ops.add(new LayoutGenerate(new File(root,file),m,layout.filenames,lengthLimit));
      }

      String file = root.getName() + FILE_GENERATE_SUFFIX;
      files.add(file);
      ops.add(new HPGenerate(new File(root,file),root.getName(),job,order,config,useRefs,layouts,lengthLimit));

   // (2) alter files

      Op.transact(ops);

   // (3) alter object

      // HP takes ownership of the files, we don't need to clean up

      File statusDir = new File(config.dataDir,DIR_STATUS);
      File statusFile = new File(statusDir,FILE_STATUS_PREFIX + root.getName() + FILE_STATUS_SUFFIX);
      job.property = Convert.fromFile(statusFile);
   }

// --- metafile generation ---

   private static class HPGenerate extends Op.GenerateXML {

      private String orderID;
      private Job job;
      private Order order;
      private HPConfig config;
      private LinkedList useRefs;
      private LinkedList layouts;
      private LengthLimit lengthLimit;

      public HPGenerate(File dest, String orderID, Job job, Order order, HPConfig config, LinkedList useRefs, LinkedList layouts, LengthLimit lengthLimit) {
         super(dest);

         this.orderID = orderID;
         this.job = job;
         this.order = order;
         this.config = config;
         this.useRefs = useRefs;
         this.layouts = layouts;
         this.lengthLimit = lengthLimit;
      }

      public void subdo(Document doc) throws IOException {

         Node data = XML.createElement(doc,"Data");
         XML.setAttribute(data,"Version","1");

         Node creator = XML.createElement(data,"Creator");
         XML.setAttribute(creator,"Name","LifePics LabCenter");
         XML.setAttribute(creator,"ID","0");
         XML.setAttribute(creator,"LANG","EN");

         Node dealer = XML.createElement(data,"Dealer");
         XML.setAttribute(dealer,"ID",  config.dealerID  );
         XML.setAttribute(dealer,"Name",config.dealerName);

         Date useDate = (order.invoiceDate != null) ? order.invoiceDate : new Date();
         // date and time are required, but invoiceDate can be null for old / local

         Node nodeOrder = XML.createElement(data,"Order");
         XML.setAttribute(nodeOrder,"ID",orderID);
         XML.setAttribute(nodeOrder,"SendTo",order.isAnyShipToHome() ? "Home" : "Shop"); // no good way to handle multiple shipping types
         XML.setAttribute(nodeOrder,"Date",dateFormat1.format(useDate));
         XML.setAttribute(nodeOrder,"Time",dateFormat2.format(useDate));
         XML.setAttribute(nodeOrder,"Priority",Convert.fromInt(config.priority));

         Node customer = XML.createElement(data,"Customer");
         XML.setAttribute(customer,"Company",  denull(order.shipCompany));
         XML.setAttribute(customer,"Name",     order.nameLast);
         XML.setAttribute(customer,"FirstName",order.nameFirst);
         XML.setAttribute(customer,"Street",   join(denull(order.shipStreet1),denull(order.shipStreet2)));
         XML.setAttribute(customer,"Zip",      denull(order.shipZipCode));
         XML.setAttribute(customer,"City",     denull(order.shipCity));
         XML.setAttribute(customer,"State",    denull(order.shipState));
         XML.setAttribute(customer,"Country",  denull(order.shipCountry));
         XML.setAttribute(customer,"Phone",    order.phone);

         Node images = XML.createElement(data,"Images");
         // attribute set later, see (*)

         Backprint b = (config.backprints.size() == 0) ? null : (Backprint) config.backprints.getFirst();
         Backprint.Sequence sequence = new Backprint.Sequence();
         boolean hasSequence = Backprint.hasSequence(config.backprints);

         int count = 0;

         Iterator i = useRefs.iterator();
         while (i.hasNext()) {
            Job.Ref ref = (Job.Ref) i.next();

            // layouts and CD products are handled later, that's what useRefs filters out

            // all items with multiple images have to be layouts, we checked that earlier,
            // so, now we know that we're looking at a single-image item, and that we can
            // use the filename safely.

            Order.Item item = getOrderItem(order,ref);

            HPMapping m = (HPMapping) MappingUtil.getMapping(config.mappings,ref.sku);

            Iterator k = new ChunkIterator(job.getOutputQuantity(item,ref),Integer.MAX_VALUE,Integer.MAX_VALUE,hasSequence);
            while (k.hasNext()) {
               int quantity = ((Integer) k.next()).intValue();

               String backprint = null;
               if (b != null) {
                  String s = b.generate(order,item,sequence);
                  if (s != null && s.length() > 0) {
                     backprint = s;
                  }
               }

               addImage(images,++count,quantity,backprint,ref.filename,m,/* layoutName = */ null,/* layoutHead = */ false);
               sequence.advance(item);
            }
         }

         i = layouts.iterator();
         while (i.hasNext()) {
            Layout layout = (Layout) i.next();

            HPMapping m = (HPMapping) MappingUtil.getMapping(config.mappings,layout.sku);
            String backprint = null;
            // no backprinting here, so no need for ChunkIterator (thankfully)

            addImage(images,++count,layout.quantity,backprint,layout.name + LAYOUT_SUFFIX,m,layout.name,/* layoutHead = */ true);

            Iterator j = layout.filenames.iterator();
            while (j.hasNext()) {
               String filename = (String) j.next();

               addImage(images,++count,layout.quantity,backprint,filename,/* m = */ null,layout.name,/* layoutHead = */ false);
            }
         }

         // (*) the idea is that the count should be job.refs.size(), but the CDs
         // and multi-image items skew it, so wait until we know the actual count
         //
         XML.setAttribute(images,"Count",Convert.fromInt(count));

         int quantity = specialQuantity(job,order);
         if (quantity != -1) {

            Node defaults = XML.createElement(data,"NonPrintDefaults");
            Node option = XML.createElement(defaults,"Option");

            XML.setAttribute(option,"type", "3"); // CD
            XML.setAttribute(option,"value",Convert.fromInt(quantity));
         }

         XML.addIndentation(data);
      }

      private void addImage(Node images, int id, int quantity, String backprint, String filename, HPMapping m, String layoutName, boolean layoutHead) {

         Node image = XML.createElement(images,"Image");
         XML.setAttribute(image,"ID",Convert.fromInt(id));
         XML.setAttribute(image,"Copies",Convert.fromInt(quantity));

         if (backprint != null) XML.setAttribute(image,"Backprint",backprint);

         Node picture = XML.createElement(image,"Picture");
         if (layoutName != null) {
            XML.setAttribute(picture,"Layout",layoutName);
            XML.setAttribute(picture,"MimeType",layoutHead ? "xml/xml" : "xml/layout");
         }
         XML.setAttribute(picture,"Filename",layoutHead ? filename : lengthLimit.transform(filename));
         XML.setAttribute(picture,"OrigName",layoutHead ? filename : order.findFileByFilename(filename).originalFilename);
         //
         // (**) the layout file name is unique, and so wouldn't transform even if we made the call,
         // but I'd rather not ... the name part of the hot folder structure, and we're not allowed
         // to change it.

         Node size = XML.createElement(image,"Size");
         XML.setAttribute(size,"ID",  (m != null) ? m.productID   : "0");
         XML.setAttribute(size,"Name",(m != null) ? m.productName : "" );

         Node crop = XML.createElement(image,"Crop");
         XML.setAttribute(crop,"Option",zeroOne((m != null) ? m.crop : false));

         Node edit = XML.createElement(image,"Edit");
         XML.setAttribute(edit,"DSCEnhance",zeroOne((m != null) ? m.enhance : false));
      }

      private static String zeroOne(boolean b) { return b ? "1" : "0"; }
      private static String denull(String s) { return (s == null) ? "" : s; }
      private static String join(String s1, String s2) { return s1 + ((s1.length() > 0 && s2.length() > 0) ? " " : "") + s2; }
   }

   private static class LayoutGenerate extends Op.GenerateXML {

      private HPMapping m;
      private LinkedList filenames;
      private LengthLimit lengthLimit;

      /**
       * @param m Just a convenient way to pass the width and heigth.
       */
      public LayoutGenerate(File dest, HPMapping m, LinkedList filenames, LengthLimit lengthLimit) {
         super(dest);

         this.m = m;
         this.filenames = filenames;
         this.lengthLimit = lengthLimit;
      }

      public void subdo(Document doc) throws IOException {

         Node layout = XML.createElement(doc,"Layout");

         Node specification = XML.createElement(layout,"Specification");

         Node position = XML.createElement(specification,"Position");
         XML.setAttribute(position,"Width", m.layoutWidth );
         XML.setAttribute(position,"Height",m.layoutHeight);

         XML.createElementText(specification,"Version","1");
         XML.createElementText(specification,"Bgcolor","16777215"); // 0xFFFFFF
         XML.createElementText(specification,"RealPagesOnPage","1");

         Node pages = XML.createElement(layout,"Pages");
         XML.setAttribute(pages,"Nr",Convert.fromInt(filenames.size()));

         Iterator i = filenames.iterator();
         while (i.hasNext()) {
            String filename = (String) i.next();

            Node page = XML.createElement(pages,"Page");
            XML.setAttribute(page,"Width", m.layoutWidth );
            XML.setAttribute(page,"Height",m.layoutHeight);

            Node image = XML.createElement(page,"Image");

            position = XML.createElement(image,"Position");
            XML.setAttribute(position,"Left","0.00cm");
            XML.setAttribute(position,"Top", "0.00cm");
            XML.setAttribute(position,"Width", m.layoutWidth );
            XML.setAttribute(position,"Height",m.layoutHeight);
            XML.setAttribute(position,"Z-Index","1");

            Node value = XML.createElement(image,"Value");
            XML.setAttribute(value,"ImageAngle","0");
            value.appendChild(doc.createTextNode(lengthLimit.transform(filename))); // createText
         }

         XML.addIndentation(layout);
      }
   }

// --- completion ---

   public boolean isCompleteWithoutDirectory(String property, Special special) throws Exception {

      if (property == null) return false; // impossible unless file hacked
      File statusFile = Convert.toFile(property);

      String status, statusCode, statusMessage;
      try {
         Document doc = XML.readFile(statusFile);

         Node data = XML.getElement(doc,"DATA");
         Node printStatus = XML.getElement(data,"PRINTSTATUS");

         // assume HP got the order ID right, not worth checking it

         status = XML.getElementText(printStatus,"STATUS");

         // the example in the spec suggested these were always present,
         // but in the real world I've seen a FIN that didn't have them.
         //
         statusCode    = XML.getNullableText(printStatus,"STATUSCODE");
         statusMessage = XML.getNullableText(printStatus,"STATUSMSG");
         //
         if (statusCode    == null) statusCode    = "";
         if (statusMessage == null) statusMessage = "";

      } catch (Exception e) {
         return false;
         // failed to read file for whatever reason, so we don't know status.
         // (maybe the file isn't there yet, or maybe it's a partial write.)
      }

      // ok, we successfully read the file, now see what we can make of it

      if (status.equals("FIN")) {
         return true;
      } else if (status.equals("ERROR")) {
         throw new Exception(Text.get(this,"e8",new Object[] { statusCode, statusMessage }));
      } else if (status.equals("CANCEL")) {
         throw new Exception(Text.get(this,"e9"));
      } else { // non-final status, no result yet
         return false;
      }
   }

}

