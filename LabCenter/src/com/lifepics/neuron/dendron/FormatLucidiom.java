/*
 * FormatLucidiom.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.misc.FileUtil;
import com.lifepics.neuron.misc.Op;
import com.lifepics.neuron.misc.Resource;
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
 * Implementation of the Lucidiom format.
 */

public class FormatLucidiom extends Format {

// --- constants ---

   private static final String SUFFIX_ORDER = ".order";
   private static final String SUFFIX_TMP   = ".tmp";
   private static final String SUFFIX_XML   = ".xml";

   private static SimpleDateFormat dateFormatLucidiom = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss~Z");

// --- subclass hooks ---

   public String getShortName() { return Text.get(this,"s1"); }

   public int[] getAllowedCompletionModes() { return new int[] { COMPLETION_MODE_MANUAL, COMPLETION_MODE_AUTO }; }
   public int   getCompletionMode(Object formatConfig) { return ((LucidiomConfig) formatConfig).completeImmediately ? COMPLETION_MODE_AUTO : COMPLETION_MODE_MANUAL; }
   public void  setCompletionMode(Object formatConfig, int mode) { ((LucidiomConfig) formatConfig).completeImmediately = (mode == COMPLETION_MODE_AUTO); }

// --- filename variation ---

   private static Activity lucidiomActivity = new OrActivity(new StandardActivity(),
                                                             new SuffixedActivity(SUFFIX_TMP));

   // we should be the only ones creating orders with our given APM ID,
   // so as long as we don't error out and fail to roll back,
   // there should be no question of finding colliding temp folders.
   // I've been getting errors in testing, though, and it's a pain ...
   // if there's a temp folder, all future jobs for that order will fail.
   // so, detect temp folders, it's easy.

// --- map record ---

   private static class Record extends SizeRecord {
      public String rename;
      public boolean hasSize; // image size not file size
      public long filesize;
   }

// --- format function ---

   public void format(Job job, Order order, Object formatConfig) throws Exception {

      LucidiomConfig config = (LucidiomConfig) formatConfig;
      // because of validation, the cast is guaranteed to work

      require(config.dataDir);

      Convert.validateUsable(LucidiomConfig.NDIGIT_ORDER_ID,order.orderID); // allow truncation

      File root = new File(config.dataDir,config.apmID + Convert.fromIntNDigit(LucidiomConfig.NDIGIT_ORDER_ID,order.orderID) + SUFFIX_ORDER);
      root = vary(root,new PrefixVariation(config.apmID.length()),lucidiomActivity);

      // note, we don't vary the APM ID part, just the order ID part.

      String base = root.getName().substring(0,root.getName().length() - SUFFIX_ORDER.length());

      File rootFinal = root;
      root = new File(config.dataDir,root.getName() + SUFFIX_TMP);

   // (1) plan the operation ; also check that mappings are defined

      LinkedList ops = new LinkedList();
      ops.add(new Op.Mkdir(root));

      HashSet skus = new HashSet();
      HashMap fmap = new HashMap();
      int fseq = 1;

      String generate = base + SUFFIX_XML;
      ops.add(new LucidiomGenerate(new File(root,generate),job,order,config,base,fmap));
      //
      // this is sneaky.  I like having the generation come first, so that
      // if there's an error, we don't waste time copying things; but then
      // how do we get the file map into the generate function?
      // one way, retroactively add the generate to the start of the list;
      // another way, add the empty file map and remember it's a shared object.

      Iterator i = job.refs.iterator();
      while (i.hasNext()) {
         Job.Ref ref = (Job.Ref) i.next();
         Order.Item item = order.getItem(ref);

         if (skus.add(ref.sku) && ! MappingUtil.existsMapping(config.mappings,ref.sku)) missingChannel(ref.sku,order.getFullID());

         List filenames = item.isMultiImage() ? item.filenames : Collections.singletonList(item.filename);
         Iterator j = filenames.iterator();
         while (j.hasNext()) {
            String filename = (String) j.next();

            Record r = (Record) fmap.get(filename);
            if (r == null) {

               // build the generated filename.  note, if we have more than a thousand,
               // the string will wrap and the copy step will fail; that's fine though.
               int index = filename.lastIndexOf('.');
               String suffix = (index == -1) ? "" : filename.substring(index);
               String rename = base + '_' + Convert.fromIntNDigit(LucidiomConfig.NDIGIT_IMAGE_ID,fseq++) + suffix;

               File file = order.getPath(filename);

               r = new Record();
               r.rename = rename;
               r.hasSize = false;
               r.filesize = FileUtil.getSize(file);

               fmap.put(filename,r);
               ops.add(new Op.Copy(new File(root,rename),file));
            }

            if ( ! r.hasSize ) {
               LucidiomMapping m = (LucidiomMapping) MappingUtil.getMapping(config.mappings,ref.sku);
               if (m.width == null || m.height == null) {
                  readWidthHeight(r,order.getPath(filename));
                  r.hasSize = true;
               }
               // else we don't need width and height for this SKU
            }
         }
      }

      ops.add(new Op.Move(rootFinal,root));

   // (2) alter files

      Op.transact(ops);

   // (3) alter object

      // actually don't alter it ... the other software
      // takes ownership of the directory as soon as we
      // rename it.

      if (config.completeImmediately) job.property = "";
   }

// --- metafile generation ---

   private static class LucidiomGenerate extends Op.GenerateXML {

      private Job job;
      private Order order;
      private LucidiomConfig config;
      private String base;
      private HashMap fmap;

      public LucidiomGenerate(File dest, Job job, Order order, LucidiomConfig config, String base, HashMap fmap) {
         super(dest);
         this.job = job;
         this.order = order;
         this.config = config;
         this.base = base;
         this.fmap = fmap;
      }

      public void subdo(Document doc) throws IOException {

         Document raw = XML.createDocument();
         Node nodeRoot = XML.createElement(raw,"Raw");

         XML.createElementText(nodeRoot,"Timestamp",dateFormatLucidiom.format(new Date()));

         XML.createElementText(nodeRoot,"ApmID",config.apmID);
         XML.createElementText(nodeRoot,"ApmOrderNumber",base);
         XML.createElementText(nodeRoot,"Brand",config.brand);
         XML.createElementText(nodeRoot,"Finish",config.glossy ? "glossy" : "matte"); // if-else is a pain in XSL

         XML.createElementText(nodeRoot,"FirstName",limit(15,order.nameFirst));
         XML.createElementText(nodeRoot,"LastName", limit(20,order.nameLast));
         XML.createElementText(nodeRoot,"Email",    limit(40,order.email));
         XML.createElementText(nodeRoot,"Phone",    limit(15,clean(order.phone)));

         XML.createElementText(nodeRoot,"Line1",  limit(32,denull(order.shipStreet1)));
         XML.createElementText(nodeRoot,"Line2",  limit(32,denull(order.shipStreet2)));
         XML.createElementText(nodeRoot,"City",   limit(32,denull(order.shipCity)));
         XML.createElementText(nodeRoot,"State",  limit(15,denull(order.shipState)));
         XML.createElementText(nodeRoot,"Zip",    limit(15,denull(order.shipZipCode)));
         XML.createElementText(nodeRoot,"Country",limit( 2,denull(order.shipCountry)));

         // for the other fields it's OK to convert nulls to empty strings,
         // but here we want to avoid claiming special instructions unless
         // there really are some.
         //
         String shipMethod = order.getFirstShipMethod(); // no good way to handle multiple shipping types
         if (shipMethod != null) {
            XML.createElementText(nodeRoot,"Method",shipMethod.toUpperCase());
         }

         Iterator i = job.refs.iterator();
         while (i.hasNext()) {
            Job.Ref ref = (Job.Ref) i.next();
            Order.Item item = getOrderItem(order,ref);

            LucidiomMapping m = (LucidiomMapping) MappingUtil.getMapping(config.mappings,item.sku);

            Node nodeItem = XML.createElement(nodeRoot,"Item");
            XML.createElementText(nodeItem,"Product",m.product);

            if (m.productType != null) {
               String s1, s2;
               int index = m.productType.indexOf(':');
               if (index != -1) {
                  s1 = m.productType.substring(0,index);
                  s2 = m.productType.substring(index+1);
               } else {
                  s1 = m.productType;
                  s2 = null;
               }
               XML.createElementText(nodeItem,"ProductType",s1);
               if (s2 != null) XML.createElementText(nodeItem,"ProductSubType",s2);
            }

            XML.createElementText(nodeItem,"Name",m.name);
            XML.createElementText(nodeItem,"Quantity",Convert.fromInt(job.getOutputQuantity(item,ref)));

            boolean itemIsMultiImage = item.isMultiImage();
            if (itemIsMultiImage) {
               XML.createElementText(nodeItem,"PageCount",Convert.fromInt(item.filenames.size()));
            }
            // page count may not be appropriate for all multi-image products, maybe later we'll have
            // a per-SKU option that says whether to include it.

            List filenames = itemIsMultiImage ? item.filenames : Collections.singletonList(item.filename);
            Iterator j = filenames.iterator();
            while (j.hasNext()) {
               String filename = (String) j.next();

               Record r = (Record) fmap.get(filename);
               Order.OrderFile file = order.findFileByFilename(filename);

               Node nodeImage = XML.createElement(nodeItem,"Image");
               XML.createElementText(nodeImage,"Path",r.rename);
               XML.createElementText(nodeImage,"OriginalName",itemIsMultiImage ? "" : file.originalFilename); // (*)
               XML.createElementText(nodeImage,"Size",Convert.fromLong(r.filesize));
               XML.createElementText(nodeImage,"Width", Convert.fromInt( (m.width  != null) ? m.width .intValue() : r.width  ));
               XML.createElementText(nodeImage,"Height",Convert.fromInt( (m.height != null) ? m.height.intValue() : r.height ));

               // (*) apparently for multi-image the original name was making the images come out in the wrong order
            }
         }

         // there are a lot of constant fields in this format, so rather than
         // hard-code them all, turn things inside out ... instead of me
         // controlling the constants, put them in XSL and let them control me.
         // also this is super-convenient for initial testing.
         //
         XML.transform(doc,raw,Resource.getResourceAsStream(this,"Lucidiom.xsl"),null);

         XML.addIndentation((Node) doc.getDocumentElement()); // my indentation is better than XSL's
      }
   }

   private static String denull(String s) { return (s == null) ? "" : s; }
   private static String limit(int len, String s) { return (s.length() <= len) ? s : s.substring(0,len); }
   private static String clean(String s) { return s.replaceAll("[^0-9a-zA-Z]",""); }

// --- completion ---

   public boolean isCompleteWithoutDirectory(String property, Special special) throws Exception {
      return (property != null);
      // not null means job should complete immediately
   }

}

