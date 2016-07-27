/*
 * FormatPixel.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.misc.FileUtil;
import com.lifepics.neuron.misc.Op;
import com.lifepics.neuron.net.DefaultHandler;
import com.lifepics.neuron.net.DescribeHandler;
import com.lifepics.neuron.net.GetTransaction;
import com.lifepics.neuron.net.Handler;
import com.lifepics.neuron.net.HTTPTransaction;
import com.lifepics.neuron.net.PostDataTransaction;
import com.lifepics.neuron.net.Query;
import com.lifepics.neuron.object.XML;
import com.lifepics.neuron.struct.SKU;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Implementation of the Pixel Magic format.
 */

public class FormatPixel extends Format {

// --- subclass hooks ---

   public String getShortName() { return Text.get(this,"s1"); }

   public int[] getAllowedCompletionModes() { return new int[] { COMPLETION_MODE_DETECT }; }
   public int   getCompletionMode(Object formatConfig) { return COMPLETION_MODE_DETECT; }
   public void  setCompletionMode(Object formatConfig, int mode) {}

// --- format function ---

   public void format(Job job, Order order, Object formatConfig) throws Exception {}
   //
   public boolean formatStoppable(Job job, Order order, Object formatConfig, FormatCallback fc) throws Exception {

      // note, we use handler.run(t,null), why?  because I'm too lazy
      // to bring the ThreadStatus field from FormatThread down here
      // and wrap a PauseAdapter around it.  otherwise we could have the
      // light in the UI for the job formatter showing a network pause.
      // (to get that to happen we'd also need to use a diagnose handler.)

      PixelConfig config = (PixelConfig) formatConfig;
      // because of validation, the cast is guaranteed to work

      require(config.dataDir);

      Handler handler = new DescribeHandler(new DefaultHandler());

   // get product information from server

      // do this before copying the files, since (a) it might fail, and (b) it's stateless.
      // make two passes to avoid bothering the server at all if we have missing mappings.

      int cdFiles = 0;
      HashMap fmap = new HashMap(); // map ref.filename to SizeRecord (for normal SKU only)

      HashSet skus = new HashSet();
      HashSet skusNoMultiPane = new HashSet();
      //
      Iterator i = job.refs.iterator();
      while (i.hasNext()) {
         Job.Ref ref = (Job.Ref) i.next();
         if (skus.add(ref.sku) && ! MappingUtil.existsMapping(config.mappings,ref.sku)) missingChannel(ref.sku,order.getFullID());

         boolean isMultiImage = Order.Item.isMultiImage(ref.filename);
         boolean isCD = ref.sku.equals(order.specialSKU);
         if (isMultiImage && isCD) throw new Exception(Text.get(this,"e14"));

         boolean printMultiImage = isMultiImage && ! config.printSingleSided;
         if (printMultiImage || isCD) skusNoMultiPane.add(ref.sku);

         if (printMultiImage) {
            // don't need to do anything yet
         } else if (isCD) {
            cdFiles++;
         } else { // normal SKU, we'll need the image size
            List filenames = isMultiImage ? order.getItem(ref).filenames : Collections.singletonList(ref.filename);
            Iterator j = filenames.iterator();
            while (j.hasNext()) {
               String filename = (String) j.next();
               if (fmap.get(filename) == null) {
                  SizeRecord r = new SizeRecord();
                  readWidthHeight(r,order.getPath(filename));
                  fmap.put(filename,r);
               }
            }
         }
      }

      // check CD overflow here ... again, the idea is to avoid bothering the server
      if (cdFiles > 0 && ! config.cdOverflowSplit) {

         if (config.cdOverflowLimit != 0) { // zero means no limit
            if (cdFiles > config.cdOverflowLimit) throw new Exception(Text.get(this,"e11",new Object[] { Convert.fromInt(cdFiles), Convert.fromInt(config.cdOverflowLimit) }));
         }

         if (config.cdOverflowBytes != 0) { // zero means no limit
            long cdBytes = 0;

            i = job.refs.iterator();
            while (i.hasNext()) {
               Job.Ref ref = (Job.Ref) i.next();
               if ( ! ref.sku.equals(order.specialSKU) ) continue;
               cdBytes += order.getPath(ref.filename).length();
               // note: we can use ref.filename as a path because
               // the case isMultiImage && isCD has been excluded
            }

            if (cdBytes > config.cdOverflowBytes) throw new Exception(Text.get(this,"e12",new Object[] { Convert.fromLong(cdBytes), Convert.fromLong(config.cdOverflowBytes) }));
         }
         // avoid hitting the file system for sizes if we don't need to.
         // but, don't try to save the sizes for later, too complicated.
         // we can just get them twice.
      }

      HashMap mats = new HashMap();
      i = skus.iterator();
      while (i.hasNext()) {
         SKU sku = (SKU) i.next();
         PixelMapping m = (PixelMapping) MappingUtil.getMapping(config.mappings,sku);

         boolean allowMultiPane = config.allowMultiPane && ! skusNoMultiPane.contains(sku);
         GetMatForProduct t = new GetMatForProduct(config.baseURL,m.product,config.deviceGroup,allowMultiPane);
         try {
            if ( ! handler.run(t,null) ) return false; // stopping
         } catch (Exception e) {
            // this is the first call, so if we can't connect to DNP at all,
            // explain that it's a connection problem.
            if (e.getCause() instanceof ConnectException) throw new Exception(Text.get(this,"e17"));
            throw e;
         }
         mats.put(sku,t.result); // map SKU to XML node
      }

   // plan file copy

      // it doesn't matter a great deal, since we're probably the only ones
      // writing into the folder, but it's nice to vary <i>after</i> all
      // the GetMatForProduct transactions are done, since they could be slow.

      File root = new File(config.dataDir,Convert.fromInt(order.orderID));
      root = vary(root);
      LinkedList files = new LinkedList();

      LinkedList ops = new LinkedList();
      ops.add(new Op.Mkdir(root));

      // yes, make a third pass through the job refs ... keeps the code simple

      HashSet fset = new HashSet();

      i = job.refs.iterator();
      while (i.hasNext()) {
         Job.Ref ref = (Job.Ref) i.next();

         List filenames = Order.Item.isMultiImage(ref.filename) ? order.getItem(ref).filenames : Collections.singletonList(ref.filename);
         Iterator j = filenames.iterator();
         while (j.hasNext()) {
            String filename = (String) j.next();
            if (fset.add(filename)) {
               String file = filename; // no subdirectory
               files.add(file);
               ops.add(new Op.Copy(new File(root,file),order.getPath(filename)));
            }
         }
      }

   // execute

      // it would be traditional to use a FileMapper to deal with mapDataDir,
      // but there's really no need.  the original concept of mapping was
      // that you'd say, "dataDir is C:\some\path\, and oh also map C:\ to \\netdrive",
      // but here we just know the two corresponding directories ... much easier.

      File mapRoot = (config.mapDataDir != null) ? new File(config.mapDataDir,root.getName()) : root;

      PixelSubmit submit = new PixelSubmit(config,job,order,handler,mapRoot,mats,fmap);
      ops.add(submit);

      try {
         Op.transact(ops);
      } catch (StopException e) {
         return false;
      }
      // note, the StopException isn't just a signal to return false,
      // it also causes the file copy to roll back.

   // alter the object

      job.dir = root;
      job.files = files;

      job.property = submit.orderID; // don't set until we're really done
      return true;
   }

// --- main transaction ---

   private static class StopException extends IOException {}

   private static class PixelSubmit extends Op {

      public String orderID; // result

      private PixelConfig config;
      private Job job;
      private Order order;
      private Handler handler;
      private File mapRoot;
      private HashMap mats;
      private HashMap fmap;

      public PixelSubmit(PixelConfig config, Job job, Order order, Handler handler, File mapRoot, HashMap mats, HashMap fmap) {
         this.config = config;
         this.job = job;
         this.order = order;
         this.handler = handler;
         this.mapRoot = mapRoot;
         this.mats = mats;
         this.fmap = fmap;
      }

      public void dodo() throws IOException {
         boolean b;
         try {
            b = run();
         } catch (Exception e) {
            throw (IOException) new IOException(Text.get(FormatPixel.class,"e10")).initCause(e);
         }
         if ( ! b ) throw new StopException();
      }

      public void undo() {
      }
      // there's no rollback transaction in the interface.
      // if something goes wrong, we just forget the order ID
      // and the server is supposed to clean it up eventually.

      private boolean run() throws Exception {

      // start submission

         // block to contain t
         {
            StartOrder t = new StartOrder(config,order);
            if ( ! handler.run(t,null) ) return false; // stopping
            orderID = t.result;
         }

      // send CD line item

         int specialQuantity = specialQuantity(job,order);
         if (specialQuantity != -1) {

            // in FormatFujiNew the lab only supported CDs that contained
            // all the images in the job, but here we can pick and choose.
            // iterate through the list again, not very efficient.

            LinkedList cds   = new LinkedList();
            LinkedList infos = new LinkedList();
            int  cdFiles = 0;
            long cdBytes = 0;

            Iterator i = job.refs.iterator();
            while (i.hasNext()) {
               Job.Ref ref = (Job.Ref) i.next();
               // don't need order item here, since we already have quantity

               if ( ! ref.sku.equals(order.specialSKU) ) continue; // not on CD

               int  thisFiles = 1;
               long thisBytes = (config.cdOverflowBytes != 0) ? order.getPath(ref.filename).length() : 0; // only get if needed

               while (true) {

                  // will this file fit on the current CD?
                  boolean fit = true;
                  if (config.cdOverflowLimit != 0 && cdFiles + thisFiles > config.cdOverflowLimit) fit = false;
                  if (config.cdOverflowBytes != 0 && cdBytes + thisBytes > config.cdOverflowBytes) fit = false;
                  if (fit) break; // no problem

                  if (infos.size() > 0) { // full, ship it

                     // we already checked for overflow error at the top level,
                     // so if it's over the limit in here, we should split it.

                     cds.add(infos);
                     infos = new LinkedList();
                     cdFiles = 0;
                     cdBytes = 0;

                  } else { // won't fit, but CD is empty

                     throw new Exception(Text.get(FormatPixel.class,"e18",new Object[] { Convert.fromLong(thisBytes) }));
                     // this can only happen when cdOverflowBytes is set
                  }
               }

               infos.add(new ImageInfo(order,ref.filename,mapRoot));
               cdFiles += thisFiles;
               cdBytes += thisBytes;
            }

            cds.add(infos); // there's always a CD in progress at the end

            PixelMapping m = (PixelMapping) MappingUtil.getMapping(config.mappings,order.specialSKU);
            Node matOriginal = (Node) mats.get(order.specialSKU);

            Iterator j = cds.iterator();
            while (j.hasNext()) {
               LinkedList send = (LinkedList) j.next();

               LineItem t = new LineItem(config.baseURL,orderID,m,specialQuantity,config.deviceGroup,/* price = */ null,
                                         new CDProduct(matOriginal,send));
               // prices on CD products not supported because of bad structure, see discussion in OrderParser.makeItems
               if ( ! handler.run(t,null) ) return false; // stopping
            }
         }

      // send line items

         Iterator i = job.refs.iterator();
         while (i.hasNext()) {
            Job.Ref ref = (Job.Ref) i.next();
            Order.Item item = order.getItem(ref);

            if (ref.sku.equals(order.specialSKU)) continue; // CD already handled

            PixelMapping m = (PixelMapping) MappingUtil.getMapping(config.mappings,ref.sku);
            int quantity = job.getOutputQuantity(item,ref);
            Node matOriginal = (Node) mats.get(ref.sku);

            LinkedList products = new LinkedList(); // only one element unless single-sided
            boolean isMultiImage = item.isMultiImage();
            boolean printMultiImage = isMultiImage && ! config.printSingleSided;
            if (printMultiImage) {

               LinkedList infos = new LinkedList();
               Iterator j = item.filenames.iterator();
               while (j.hasNext()) {
                  String filename = (String) j.next();
                  infos.add(new ImageInfo(order,filename,mapRoot));
               }

               products.add(new CreativeProduct(matOriginal,infos));

            } else { // normal SKU

               List filenames = isMultiImage ? item.filenames : Collections.singletonList(ref.filename);
               Iterator j = filenames.iterator();
               while (j.hasNext()) {
                  String filename = (String) j.next();

                  SizeRecord imageSize = (SizeRecord) fmap.get(filename);
                  products.add(new StandardProduct(matOriginal,new ImageInfo(order,filename,mapRoot),imageSize,config.allowMultiPane));
               }
            }

            Iterator j = products.iterator();
            while (j.hasNext()) {
               Product p = (Product) j.next();
               LineItem t = new LineItem(config.baseURL,orderID,m,quantity,config.deviceGroup,item.price,p);
               if ( ! handler.run(t,null) ) return false; // stopping
               // the item price isn't divided up correctly for single-sided, but I don't think it matters
            }
         }

      // finish submission

         // block to contain t
         {
            OrderEnd t = new OrderEnd(config.baseURL,orderID,config.separatorSheet);
            if ( ! handler.run(t,null) ) return false; // stopping
         }

         return true;
      }
   }

// --- completion ---

   public boolean isCompleteWithoutDirectory(String property, Special special) throws Exception {

      PixelConfig config = (PixelConfig) special.getQueueConfig();
      Handler handler = new DescribeHandler(new DefaultHandler());

      GetOrderStatus t = new GetOrderStatus(config.baseURL,property);
      if ( ! handler.run(t,null) ) return false; // stopping ... so not complete yet

      switch (t.result) {
      case 100: // completed
      case 110: // completed and deleted
         return true;

      case 550: // canceled
         throw new Exception(Text.get(this,"e7",new Object[] { Convert.fromInt(t.result) }));

      default:
         return false; // status not settled one way or the other
      }
   }

// --- transaction helpers ---

   private static String combineJSP(String baseURL, String page) {
      return HTTPTransaction.combine(baseURL,"xml/" + page + ".jsp");
   }

   private static Document readAndCheck(InputStream inputStream) throws Exception {

      // the point of this is to pass on errors from the server.

      // unfortunately, when an error does occur, the server does
      // the wrong thing ... just stops in the middle of the data
      // it was sending and says <Error>...</Error>.
      // so, the resulting XML is probably malformed, and needs special care.

      // this is kind of awkward, to read every single transaction
      // into a byte buffer, but how else can we read twice?
      // fortunately, we're not talking about large downloads here.

      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      FileUtil.copy(buffer,inputStream);
      String s = buffer.toString();
      //
      // just like HTTPTransaction.getText, except we want to keep access
      // to the original bytes so we don't have to change the string back.

      // check for error first, it's easy and saves us a wrapper layer.

      String error = parseError(s);
      if (error != null) {
         throw new Exception(parseErrorMore(error));
      }

      // no error tag, go ahead and try normal read

      return XML.readStream(new ByteArrayInputStream(buffer.toByteArray()));
   }

   private static final String tag0 =   "error";
   private static final String tag1 =  "<error>";
   private static final String tag2 = "</error>";

   private static String parseError(String s) {

      // it's not appropriate to just search for the tags,
      // because an otherwise-valid response could (in theory)
      // include an error tag somewhere inside.  but, if the
      // whole response ends with </error>, it can't be valid.
      // searching backward for <error> is reasonable ...
      // not technically correct, but fine for cases that occur.

      int i = s.length() - 1;
      while (i >= 0) { // handles zero-length string BTW
         char c = s.charAt(i);
         if ( ! (c == ' ' || c == '\t' || c == '\r' || c == '\n') ) break;
         i--;
      }
      i++;
      // now i is an end index for the non-whitespace part

      final int len1 = tag1.length();
      final int len2 = tag2.length();

      if (i >= len2 && s.substring(i-len2,i).equals(tag2)) {
         int j = s.lastIndexOf(tag1,i-len1-len2); // ok if i-len1-len2 is negative
         if (j != -1) return s.substring(j+len1,i-len2);
      }

      return null;
   }

   private static String parseErrorMore(String error) {
      try {

         String s = tag1 + error + tag2; // might as well use same tags
         Document doc = XML.readStream(new ByteArrayInputStream(s.getBytes()));

         Node node = XML.getElement(doc,tag0);
         XML.removeWhitespace(node); // otherwise CDATA needs more work

         String code = XML.getElementText(node,"error_id"); // should be integer, but no reason to enforce

         Node temp = XML.getElement(node,"description");

         // this is a more severe version of XML.getText (but for CDATA)
         NodeList children = temp.getChildNodes();
         if (children.getLength() == 1) {
            Node child = children.item(0);
            if (child.getNodeType() == Node.CDATA_SECTION_NODE) {
               String description = child.getNodeValue();
               return Text.get(FormatPixel.class,"e1b",new Object[] { code, description });
               // can't throw exception here, because of catch below
            }
         }
         // else fall through

      } catch (Exception e) {
         // fall through to simpler exception
      }
      return Text.get(FormatPixel.class,"e1a",new Object[] { error });
   }

   private static Node getPane(Node mat, boolean allowMultiPane) throws Exception {
      // should be exactly one pane in each mat
      Iterator i = XML.getElements(mat,"pane");
      if ( ! i.hasNext() ) throw new Exception(Text.get(FormatPixel.class,"e8"));
      Node pane = (Node) i.next();

      if ( ! allowMultiPane ) {
         if (i.hasNext())  throw new Exception(Text.get(FormatPixel.class,"e9"));
      } else {

         int oPane = getPaneOrientation(pane);
         while (i.hasNext()) {
            if (getPaneOrientation((Node) i.next()) != oPane) throw new Exception(Text.get(FormatPixel.class,"e15"));
         }
         // since we can only flip the whole mat, the panes had better
         // have the same orientation.  this code is slightly stricter
         // than necessary, but I don't want to deal with the case where
         // some panes are square and some aren't.
      }

      return pane;
   }

   private static class ImageInfo {

      public File path;
      public String originalFilename;

      public ImageInfo(File path, String originalFilename) { // old constructor
         this.path = path;
         this.originalFilename = originalFilename;
      }

      public ImageInfo(Order order, String filename, File mapRoot) {
         path = new File(mapRoot,filename);
         originalFilename = order.findFileByFilename(filename).originalFilename;
      }
   }

   private static int getPaneOrientation(Node pane) throws Exception {

      Node paneWidth  = XML.getTextNode(XML.getElement(pane,"width" ));
      Node paneHeight = XML.getTextNode(XML.getElement(pane,"height"));

      return getOrientation(Convert.toDouble(paneWidth .getNodeValue()),
                            Convert.toDouble(paneHeight.getNodeValue()));
   }

   private static void flip(Node node, String name1, String name2) throws Exception {
      String temp;

      Node node1 = XML.getTextNode(XML.getElement(node,name1));
      Node node2 = XML.getTextNode(XML.getElement(node,name2));

      temp             = node1.getNodeValue();
      node1.setNodeValue(node2.getNodeValue());
      node2.setNodeValue(temp);
   }

   private static void flip(Node node, boolean hasPos) throws Exception {
      flip(node,"width","height");
      if (hasPos) flip(node,"pos_x","pos_y");
   }

   private static void adjustOrientation(Node mat, Node pane, SizeRecord imageSize) throws Exception {

      // this is flawed in a couple of ways, but I'm leaving it as is
      // for backward compatibility.  if you want better behavior,
      // turn on allowMultiPane -- this code doesn't run in that case.
      //
      // flaw #1: it would be fine to have a mat with an asymmetrical
      // border that gave the pane a different orientation.
      //
      // flaw #2: when we flip, pos_x and pos_y also ought to flip, but they don't.

      // the idea is to flip the width and height to match the orientation of the image.

      // first make sure we have all the nodes.  we could allow these to be absent,
      // but they really should always be there, especially for single images that
      // we try to adjust the orientation on.  even CDs have these properties,
      // but they're zero, so we'd error out when we tried to get the orientation.
      //
      // use getElement plus getTextNode instead of getElementText so that we can
      // alter the data without worrying about preserving the parent's node order.
      // this is more of the weird XML stuff that goes on in this class.

      Node matWidth   = XML.getTextNode(XML.getElement(mat, "width" ));
      Node matHeight  = XML.getTextNode(XML.getElement(mat, "height"));
      Node paneWidth  = XML.getTextNode(XML.getElement(pane,"width" ));
      Node paneHeight = XML.getTextNode(XML.getElement(pane,"height"));

      int oMat  = getOrientation(Convert.toDouble(matWidth  .getNodeValue()),
                                 Convert.toDouble(matHeight .getNodeValue()));
      int oPane = getOrientation(Convert.toDouble(paneWidth .getNodeValue()),
                                 Convert.toDouble(paneHeight.getNodeValue()));

      if (oMat*oPane < 0) throw new Exception(Text.get(FormatPixel.class,"e13"));
      // mat and pane conflict?
      // allow one or the other to be square, and use sum below

      int oImage = getOrientation(imageSize.width,imageSize.height);

      if ((oMat+oPane)*oImage >= 0) return;
      // this is why getOrientation returns the values it does,
      // so that we have an easy test for whether to flip.
      // if either the mat or the image is square, do not flip.
      // note, test for sign, not for -1, since -2 is possible.

      String temp;

      temp                  = matWidth  .getNodeValue();
      matWidth  .setNodeValue(matHeight .getNodeValue());
      matHeight .setNodeValue(temp);

      temp                  = paneWidth .getNodeValue();
      paneWidth .setNodeValue(paneHeight.getNodeValue());
      paneHeight.setNodeValue(temp);
   }

// --- transactions ---

   // there's some commonality in the readAndCheck calls,
   // but it's not worth factoring out ... anyway it shouldn't go
   // any further than this class because of our special handling
   // of malformed XML with error messages.

   private static class GetMatForProduct extends GetTransaction {
      public Node result;

      private String baseURL;
      private String productID;
      private Integer deviceGroup;
      private boolean allowMultiPane;
      public GetMatForProduct(String baseURL, String productID, Integer deviceGroup, boolean allowMultiPane) { this.baseURL = baseURL; this.productID = productID; this.deviceGroup = deviceGroup; this.allowMultiPane = allowMultiPane; }

      public String describe() { return Text.get(FormatPixel.class,"s2",new Object[] { productID }); }
      protected String getFixedURL() { return combineJSP(baseURL,"getMatForProduct"); }
      protected void getParameters(Query query) throws IOException {
         query.add("PRODUCT_ID",productID);
         if (deviceGroup != null) query.add("DEVICE_GROUP_ID",Convert.fromInt(deviceGroup.intValue()));
      }

      protected boolean receive(InputStream inputStream) throws Exception {
         Document doc = readAndCheck(inputStream);

         Node node = XML.getElement(doc,"mats");

         // we don't have to check this, but I like to check things
         String s = XML.getAttribute(node,"product_id");
         if ( ! s.equals(productID) ) throw new Exception(Text.get(FormatPixel.class,"e2",new Object[] { s }));

         Iterator i = XML.getElements(node,"mat");
         if ( ! i.hasNext() ) throw new Exception(Text.get(FormatPixel.class,"e3"));
         result = (Node) i.next();
         if (   i.hasNext() ) throw new Exception(Text.get(FormatPixel.class,"e4"));

         getPane(result,allowMultiPane); // check this now, before we start the order

         return true;
      }
   }

   private static class StartOrder extends PostDataTransaction {
      public String result;

      private PixelConfig config;
      private Order order;
      public StartOrder(PixelConfig config, Order order) { this.config = config; this.order = order; }

      public String describe() { return Text.get(FormatPixel.class,"s3"); }
      protected String getFixedURL() { return combineJSP(config.baseURL,"order/startOrder"); }
      protected void getParameters(Query query) throws IOException {}

      protected Document getXMLData() throws Exception {
         Document doc = XML.createDocument();

         Node node = XML.createElement(doc,"order");

         XML.setAttribute(node,"docversion","1.0");

         XML.createElementText(node,"node_id",Convert.fromInt(config.nodeID));
         XML.createElementText(node,"transaction_type_id",Convert.fromInt(config.transactionTypeID));
         XML.createElementText(node,"external_order_id",config.refnumPrefix + Convert.fromInt(order.orderID));

         if (order.shippingAmount == null || Convert.toCents(order.shippingAmount) == 0) {

            XML.createNullableText(node,"total",    order.grandTotal);
            XML.createNullableText(node,"sub_total",order.subtotal);
            XML.createNullableText(node,"base_tax", order.taxAmount);

            boolean send = false;
            int discountTotal = 0;

            if (order.discountFreePrints != null) { send = true; discountTotal += Convert.toCents(order.discountFreePrints); }
            if (order.discountCredit     != null) { send = true; discountTotal += Convert.toCents(order.discountCredit    ); }
            if (order.discountPercent    != null) { send = true; discountTotal += Convert.toCents(order.discountPercent   ); }
            if (order.discountPermanent  != null) { send = true; discountTotal += Convert.toCents(order.discountPermanent ); }

            if (send) XML.createElementText(node,"coupon_discount",Convert.fromCents(discountTotal));
         }
         // else don't send, it wouldn't add up (because there's no place to put the
         // shipping amount in the XML).  should be fine, we expect no shipping.

         Node customer = XML.createElement(node,"customer");

         XML.createElementText(customer,"email",order.email); // same field order as in spec
         XML.createElementText(customer,"phone",order.phone);
         XML.createElementText(customer,"fname",order.nameFirst);
         XML.createElementText(customer,"lname",order.nameLast);

         // the address code was causing trouble, and nobody really wants it, so take it out.
         // the trouble was, we got an error about missing node line1 ... which is weird
         // because any modern order would have non-null street1.  maybe it was an empty string
         // and they didn't like the <line1/>?  anyway, problem solved now.

         XML.addIndentation(node);
         return doc;
      }

      protected boolean receive(InputStream inputStream) throws Exception {
         Document doc = readAndCheck(inputStream);

         Node node = XML.getElement(doc,"order");

         result = XML.getAttribute(node,"order_id");

         return true;
      }
   }

   private static class LineItem extends PostDataTransaction {

      private String baseURL;
      private String orderID;
      private PixelMapping m;
      private int quantity;
      private Integer deviceGroup;
      private String price;
      private Product product;

      public LineItem(String baseURL, String orderID, PixelMapping m, int quantity, Integer deviceGroup, String price, Product product) {
         this.baseURL = baseURL;
         this.orderID = orderID;
         this.m = m;
         this.quantity = quantity;
         this.deviceGroup = deviceGroup;
         this.price = price;
         this.product = product;
      }

      public String describe() { return Text.get(FormatPixel.class,"s4"); }
      protected String getFixedURL() { return combineJSP(baseURL,"order/lineitem"); }
      protected void getParameters(Query query) throws IOException {}

      public Node importNode(Document doc, Node original) {
         Node node = doc.importNode(original,true);
         XML.removeWhitespace(node); // else it messes up indentation
         return node;
      }

      public void insertFinishType(Node mat, Node pane) {
         XML.insertElementText(mat,pane,"finish_type_id",Convert.fromInt(m.finishType));
      }

      public void createImage(Document doc, Node pane, ImageInfo info) {
         Node image = XML.createElement(pane,"image");
         Node temp = XML.createElement(image,"path");
         temp.appendChild(doc.createCDATASection(Convert.fromFile(info.path)));
         temp = XML.createElement(image,"orig_filename");
         temp.appendChild(doc.createCDATASection(info.originalFilename));
      }

      protected Document getXMLData() throws Exception {
         Document doc = XML.createDocument();

         Node node = XML.createElement(doc,"lineitem");

         XML.setAttribute(node,"docversion","1.0");

         XML.createElementText(node,"order_id",orderID);
         XML.createElementText(node,"product_id",m.product);
         XML.createElementText(node,"service_id",Convert.fromInt(m.service));
         XML.createElementText(node,"qty",Convert.fromInt(quantity));
         XML.createElementText(node,"item_price",(price != null) ? price : "0.00");

         Node sub = XML.createElement(node,"lineitem_product");

         XML.createElementText(sub,"product_id",m.product);
         XML.createNullableText(sub,"device_group",Convert.fromNullableInt(deviceGroup));
         XML.createElementText(sub,"copies","1"); // multiplied by quantity above

         product.generate(doc,sub,this);

         XML.addIndentation(node);
         return doc;
      }

      protected boolean receive(InputStream inputStream) throws Exception {
         Document doc = readAndCheck(inputStream);

         Node node = XML.getElement(doc,"lineitem");

         String s = XML.getAttribute(node,"lineitem_id");
         // we just want to make sure it's there ...
         // we don't want to save it and we have nothing to compare it to

         return true;
      }
   }

   private interface Product {
      void generate(Document doc, Node sub, LineItem lineItem) throws Exception;
   }

   private static class StandardProduct implements Product {

      private Node matOriginal;
      private ImageInfo info;
      private SizeRecord imageSize;
      private boolean allowMultiPane;

      public StandardProduct(Node matOriginal, ImageInfo info, SizeRecord imageSize, boolean allowMultiPane) {
         this.matOriginal = matOriginal;
         this.info = info;
         this.imageSize = imageSize;
         this.allowMultiPane = allowMultiPane;
      }

      public void generate(Document doc, Node sub, LineItem lineItem) throws Exception {

         // now we do some weird XML things

         Node mat = lineItem.importNode(doc,matOriginal);
         sub.appendChild(mat);

         if (allowMultiPane) {

            // use new code even if not actually multiple panes
            // so that we have a way to get around
            // the flaws in adjustOrientation (see comments there)

         // first pane

            Iterator i = XML.getElements(mat,"pane");
            if ( ! i.hasNext() ) throw new Exception(Text.get(FormatPixel.class,"e16"));
            // must call hasNext before next, this iterator is nasty and requires it
            Node pane = (Node) i.next();
            Node paneFirst = pane; // save for later

            int oPane  = getPaneOrientation(pane);
            int oImage = getOrientation(imageSize.width,imageSize.height);
            boolean flip = (oPane*oImage < 0);
            // getPane checked that all panes have same orientation

            lineItem.createImage(doc,pane,info);
            if (flip) {
               flip(mat, /* hasPos = */ false);
               flip(pane,/* hasPos = */ true );
            }

         // other panes

            while (i.hasNext()) {
               pane = (Node) i.next();

               lineItem.createImage(doc,pane,info);
               if (flip) {
                  flip(pane,/* hasPos = */ true);
               }
            }

         // finish up

            lineItem.insertFinishType(mat,paneFirst);
            // don't do this until iterator is done

         } else {

            Node pane = getPane(mat,/* allowMultiPane = */ false);
            lineItem.insertFinishType(mat,pane);

            lineItem.createImage(doc,pane,info);

            adjustOrientation(mat,pane,imageSize);
         }
      }
   }

   private static class CDProduct implements Product {

      private Node matOriginal;
      private LinkedList infos;

      public CDProduct(Node matOriginal, LinkedList infos) {
         this.matOriginal = matOriginal;
         this.infos = infos;
      }

      public void generate(Document doc, Node sub, LineItem lineItem) throws Exception {

         // now we do some weird XML things

         Node mat = lineItem.importNode(doc,matOriginal);
         sub.appendChild(mat);

         Node pane = getPane(mat,/* allowMultiPane = */ false);
         lineItem.insertFinishType(mat,pane);

         Node paneOriginal = getPane(matOriginal,/* allowMultiPane = */ false);

         // iterate backwards because when there's more than
         // one pane we need to insert them in reverse order
         //
         ListIterator li = infos.listIterator(infos.size());
         while (li.hasPrevious()) {
            ImageInfo info = (ImageInfo) li.previous();
            lineItem.createImage(doc,pane,info);

         // remove id attribute if present

            // the trouble is, the pane in my old test data has id="1",
            // and if we copy that around, it might cause problems.

            final String name = "id";

            NamedNodeMap attributes = pane.getAttributes();
            if (attributes.getNamedItem(name) != null) {
               attributes.removeNamedItem(name);
            }
            // can't just remove, that errors if not found

         // create new pane if necessary

            if (li.hasPrevious()) {

               Node paneNew = lineItem.importNode(doc,paneOriginal);
               mat.insertBefore(paneNew,pane);
               // unfortunately there's no insertAfter function

               pane = paneNew;
            }
         }
      }
   }

   private static class CreativeProduct implements Product {

      private Node matOriginal;
      private LinkedList infos;

      public CreativeProduct(Node matOriginal, LinkedList infos) {
         this.matOriginal = matOriginal;
         this.infos = infos;
      }

      public void generate(Document doc, Node sub, LineItem lineItem) throws Exception {

         // here we generate the XML from scratch, just using the mat width and height

         // not worth converting to double or trying to share code with adjustOrientation.
         // note, DNP says there's no adjustment needed.
         String width  = XML.getElementText(matOriginal,"width");
         String height = XML.getElementText(matOriginal,"height");

         Node page = null;

         // all products are double-sided, but it's simple to bring the option out here.
         // when the image count is odd, the last page is single-sided, that's expected.
         boolean makePage = true;
         boolean doubleSided = true;

         Iterator i = infos.iterator();
         while (i.hasNext()) {
            ImageInfo info = (ImageInfo) i.next();

            if (makePage) {
               page = XML.createElement(sub,"page");
               XML.createElementText(page,"width",width);
               XML.createElementText(page,"height",height);
            }
            makePage ^= doubleSided;

            Node side = XML.createElement(page,"side");
            lineItem.createImage(doc,side,info);
            lineItem.insertFinishType(side,null); // null makes it insert at the end
         }
      }
   }

   private static class OrderEnd extends PostDataTransaction {

      private String baseURL;
      private String orderID;
      private boolean separatorSheet;
      public OrderEnd(String baseURL, String orderID, boolean separatorSheet) { this.baseURL = baseURL; this.orderID = orderID; this.separatorSheet = separatorSheet; }

      public String describe() { return Text.get(FormatPixel.class,"s5"); }
      protected String getFixedURL() { return combineJSP(baseURL,"order/orderend"); }
      protected void getParameters(Query query) throws IOException {}

      protected Document getXMLData() throws Exception {
         Document doc = XML.createDocument();

         Node node = XML.createElement(doc,"order");

         XML.setAttribute(node,"docversion","1.0");

         XML.createElementText(node,"order_id",orderID);
         if (separatorSheet) XML.createElement(node,"separator_sheet");
         // node with no content, same as createElementText( .... ,"")

         XML.addIndentation(node);
         return doc;
      }

      protected boolean receive(InputStream inputStream) throws Exception {
         Document doc = readAndCheck(inputStream);

         Node node = XML.getElement(doc,"order");

         String s = XML.getElementText(node,"order_id");
         if ( ! s.equals(orderID) ) throw new Exception(Text.get(FormatPixel.class,"e5",new Object[] { s, orderID }));

         return true;
      }
   }

   private static class GetOrderStatus extends GetTransaction {
      public int result;

      private String baseURL;
      private String orderID;
      public GetOrderStatus(String baseURL, String orderID) { this.baseURL = baseURL; this.orderID = orderID; }

      public String describe() { return Text.get(FormatPixel.class,"s6"); }
      protected String getFixedURL() { return combineJSP(baseURL,"order/getOrderStatus"); } // the "order/" is empirical
      protected void getParameters(Query query) throws IOException {
         query.add("ORDER_ID",orderID);
      }

      protected boolean receive(InputStream inputStream) throws Exception {
         Document doc = readAndCheck(inputStream);

         Node node = XML.getElement(doc,"order_status");

         Node temp = XML.getElementTry(node,"order");
         if (temp != null) node = temp;
         // accept a second format at Pixel's request

         String s = XML.getAttribute(node,"order_id");
         if ( ! s.equals(orderID) ) throw new Exception(Text.get(FormatPixel.class,"e6",new Object[] { s, orderID }));

         result = Convert.toInt(XML.getElementText(node,"global_status"));

         return true;
      }
   }

}

