/*
 * Invoice.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Nullable;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.gui.Barcode;
import com.lifepics.neuron.gui.Code128;
import com.lifepics.neuron.gui.HTMLEditorPane;
import com.lifepics.neuron.gui.Print;
import com.lifepics.neuron.gui.PrintConfig;
import com.lifepics.neuron.gui.ProductBarcode;
import com.lifepics.neuron.gui.ProductBarcodeConfig;
import com.lifepics.neuron.gui.Rotation;
import com.lifepics.neuron.misc.FileUtil;
import com.lifepics.neuron.object.XML;
import com.lifepics.neuron.struct.NewSKU;
import com.lifepics.neuron.struct.OldSKU;
import com.lifepics.neuron.struct.SKU;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TimeZone;

import javax.swing.JFrame;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import javax.imageio.*;
import javax.imageio.stream.FileImageOutputStream;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * A utility class containing primitive operations for invoices and labels.
 */

public class Invoice {

// --- fields ---

   // all filled in at construction

   private Order orderObjectSave;
   private PrintConfig config;
   private boolean isLocalOrder;
   private File invoiceFile;
   private LinkedList labelInfos;
   private String invoiceTitle;
   private String labelTitle;
   private int labelCopies;
   private String labelFooter;
   private File postageXSL;
   private String postageOrderID;

   // information from order.xml
   private Document orderDoc;
   private boolean invoiceShipToHome;
   private String barcodePrice;

// --- accessors ---

   public File getInvoiceFile() { return invoiceFile; }
   public LinkedList getLabelInfos() { return labelInfos; }
   public String getInvoiceTitle() { return invoiceTitle; }
   public String getLabelTitle()   { return labelTitle;   }

// --- label info ---

   // there are now two kinds of labels:
   // * the new product labels, which have product set to some value and also
   //   shipMethod set to some value so that we can index to the OrderSummary
   //   block that the product came from.
   // * the original shipping and pickup labels, which have product set to null.

   private interface LabelInfoCallback {
      void enumerate(String shipMethod, String product, String labelFilename);
   }

   private static void enumerateLabelInfo(Order order, boolean productEnableForHome, boolean productEnableForAccount, LabelInfoCallback callback) {

   // normal labels

      switch (order.shipMethods.size()) {

      case 0: // local order with no shipping set
         callback.enumerate(null,null,Order.LABEL_FILE);
         break;

      case 1: // normal order
         callback.enumerate(((Order.ShipMethod) order.shipMethods.getFirst()).name,null,Order.LABEL_FILE);
         break;

      default: // multiple shipping types
         Iterator i = order.shipMethods.iterator();
         int n = 1;
         while (i.hasNext()) {
            callback.enumerate(((Order.ShipMethod) i.next()).name,null,Order.getLabelFile(n++));
         }
         break;
      }

   // product labels

      // just a reminder, products that aren't produced here
      // have already been filtered out by OrderParser.

      int n = 1;

      Iterator i = order.shipMethods.iterator();
      while (i.hasNext()) {
         Order.ShipMethod shipMethod = (Order.ShipMethod) i.next();

         boolean enable = Order.isShipToHome(shipMethod.name) ? productEnableForHome
                                                              : productEnableForAccount;
         // iterate even if not enabled so that the file numbers remain constant

         Iterator j = shipMethod.products.iterator();
         while (j.hasNext()) {
            String product = (String) j.next();

            if (enable) callback.enumerate(shipMethod.name,product,Order.getProductLabelFile(n));
            n++;
         }
      }
   }

   public static class LabelInfo {

      public String shipMethod;
      public String product;
      public File labelFile;

      public LabelInfo(String shipMethod, String product, File labelFile) { this.shipMethod = shipMethod; this.product = product; this.labelFile = labelFile; }

      public String toString() { // convenience for UI
         return (product != null) ? product : denull(shipMethod);
      }
      // if ship method is null, then this is a local order with one LabelInfo record,
      // therefore we won't prompt for which label to view, and this won't get called.
      // but, still, be nice and don't return null.
   }

   public static LinkedList getLabelInfos(final Order order, boolean productEnableForHome, boolean productEnableForAccount) {
      final LinkedList labelInfos = new LinkedList();
      enumerateLabelInfo(order,productEnableForHome,productEnableForAccount,new LabelInfoCallback() {
         public void enumerate(String shipMethod, String product, String labelFilename) {
            labelInfos.add(new LabelInfo(shipMethod,product,new File(order.orderDir,labelFilename)));
         }
      });
      return labelInfos;
   }

   public static LinkedList getLabelFilenames(Order order, boolean productEnableForHome, boolean productEnableForAccount) {
      final LinkedList labelFilenames = new LinkedList();
      enumerateLabelInfo(order,productEnableForHome,productEnableForAccount,new LabelInfoCallback() {
         public void enumerate(String shipMethod, String product, String labelFilename) {
            labelFilenames.add(labelFilename);
         }
      });
      return labelFilenames;
   }

// --- part 1 : constructor that generates XML files ---

   public Invoice(Order order, File stylesheet, File postageXSL, JobManager jobManager, File jpegTemp) throws Exception {

   // fill in fields

      orderObjectSave = order; // I hoped I could avoid this, but it seems not

      config = Print.getConfig(); // note, we get the config once (atomically),
                                  // that way there's no synchronization issue
      isLocalOrder = (order.orderSeq != null);

      File orderFile;
      orderFile   = new File(order.orderDir,Order.ORDER_FILE  );
      invoiceFile = new File(order.orderDir,Order.INVOICE_FILE);
      labelInfos = getLabelInfos(order,config.productEnableForHome,config.productEnableForAccount);

      String fullID = order.getFullID();
      invoiceTitle = Text.get(this,"s2",new Object[] { fullID });
      labelTitle   = Text.get(this,"s3",new Object[] { fullID });

      // fill in and overwrite, easiest way to handle if-if structure
      labelCopies = 1;
      labelFooter = null;

      if (config.labelPerQueue) {
         int queues = jobManager.countQueues(order);
         if (queues > 0) {
            labelCopies = queues;
            labelFooter = Text.get(this,"s4");
         }
         // if there are no queues, hmm ... we don't want to print zero labels,
         // because the user may be relying on having at least one label per order,
         // and also the print code would probably break if we told it zero copies.
         // but, we also don't want to say "Label 1 of 1", that's misleading;
         // so, fall back on the original behavior and print it with no footer.
      }

      this.postageXSL = postageXSL;
      postageOrderID = fullID;

   // read

      Document doc = OrderParser.readFile(orderFile);
      orderDoc = doc;

   // shipping class field

      invoiceShipToHome = order.isAnyShipToHome(); // no good way to handle multiple shipping types

   // barcode price field

      Node n1 = XML.getElement(doc,OrderParser.NAME_ORDER);
      barcodePrice = getBarcodePrice(n1,config.barcodeIncludeTax);

   // time zone adjustment

      // here we want to see the actual time zone printed,
      // so we can't use Convert.fromDateAbsoluteExternal.

      String timestampInvoice;
      String timestampLabel;

      if (order.invoiceDate != null) {

         SimpleDateFormat formatOut;
         TimeZone timeZone = TimeZone.getTimeZone(config.timeZone);

         formatOut = new SimpleDateFormat(config.dateFormatInvoice);
         formatOut.setLenient(false);
         formatOut.setTimeZone(timeZone);

         timestampInvoice = formatOut.format(order.invoiceDate);

         formatOut = new SimpleDateFormat(config.dateFormatLabel);
         formatOut.setLenient(false);
         formatOut.setTimeZone(timeZone);

         timestampLabel = formatOut.format(order.invoiceDate);

      } else {
         timestampInvoice = "";
         timestampLabel   = "";
      }

   // product barcode

      if (config.productBarcodeEnable) createProductBarcodes(n1,config.productBarcodeType);

   // set up parameters

      HashMap parameters = new HashMap();

      parameters.put("accountsummary",new Boolean(config.accountSummary));

      parameters.put("lifepicsID",(order.wholesale == null) ? "" : order.getFullID());
      // the order ID in order.xml is always the merchant order ID;
      // what we need in the wholesale case is the LifePics order ID, which is order.orderID.
      // and, since orderSeq's null in that case by validation, that's the same as getFullID.

      parameters.put("items",new Boolean(config.showItems));
      parameters.put("color",new Boolean(config.showColor));

      // these two changes are currently linked to the summary invoice
      parameters.put("disposition",new Boolean( ! config.showItems ));
      parameters.put("enlargetype",new Boolean( ! config.showItems ));

      parameters.put("shownumber",new Boolean(config.showOrderNumber));
      parameters.put("override",new Boolean(config.overrideAddress));
      parameters.put("ovname",denull(config.overrideName));
      parameters.put("ovstreet1",denull(config.overrideStreet1));
      parameters.put("ovstreet2",denull(config.overrideStreet2));
      parameters.put("ovcity",denull(config.overrideCity));
      parameters.put("ovstate",denull(config.overrideState));
      parameters.put("ovzip",denull(config.overrideZIP));
      // if override is true, they must all be non-null except street2

      boolean ms1 = config.markShip &&   config.markShipUseMessage;
      boolean ms2 = config.markShip && ! config.markShipUseMessage;
      if (ms1) parameters.put("markshipvalue",config.markShipMessage);
      // if ms1, we set once now; if ms2, we set once per generation
      //
      // this method is slightly flawed.  with markPre, when the flag is set
      // we validate that the message isn't empty, so the "not null" tests
      // in the XSL will fire iff the flag is set.  in the ms2 case, however,
      // we send an empty message for invoice, JPEG label, and local orders.
      // the first two shouldn't be looking at the field anyway, but local orders
      // can potentially act like the box isn't checked.

      parameters.put("markpay",new Boolean(config.markPay));
      parameters.put("includetax",new Boolean(config.markPayIncludeTax));
      parameters.put("addmessage",denull(config.markPayAddMessage));
      parameters.put("markpro",new Boolean(config.markPro));
      if (config.markProNotMerchantID != null) parameters.put("markproID",Convert.fromInt(config.markProNotMerchantID.intValue()));
      if (config.markPre) parameters.put("markprevalue",config.markPreMessage);
      parameters.put("showpickup",new Boolean(config.showPickupLocation));

      if (config.logoFile != null) {
         parameters.put("logo",config.logoFile.toURL());
         if (config.logoWidth  != null) parameters.put("logowidth", Convert.fromInt(config.logoWidth .intValue()));
         if (config.logoHeight != null) parameters.put("logoheight",Convert.fromInt(config.logoHeight.intValue()));
      }

   // write

      parameters.put("label",new Boolean(false));
      parameters.put("timestamp",timestampInvoice);
      parameters.put("home",new Boolean(invoiceShipToHome));
      parameters.put("shipmethod","");
      parameters.put("product",   "");
      if (ms2) parameters.put("markshipvalue","");
      //
      XML.writeFile(invoiceFile,doc,stylesheet,parameters,null);

      Iterator i = labelInfos.iterator();
      while (i.hasNext()) {
         LabelInfo labelInfo = (LabelInfo) i.next();

         parameters.put("label",new Boolean(true));
         parameters.put("timestamp",timestampLabel);
         parameters.put("home",new Boolean(Order.isShipToHome(labelInfo.shipMethod)));
         parameters.put("shipmethod",denull(labelInfo.shipMethod)); // (*)
         parameters.put("product",   denull(labelInfo.product   ));
         if (ms2) parameters.put("markshipvalue",denull(labelInfo.shipMethod));
         //
         XML.writeFile(labelInfo.labelFile,doc,stylesheet,parameters,null);

         // (*) if ship method is null, then this is a local order with order XML
         // that has the old format with no OrderSummary/ShipMethod, therefore we
         // won't ever get into the case where we look at $shipmethod.
         // we just have to denull it because the XSL processor doesn't like null.
      }

   // maybe write JPEG temp file

      if (jpegTemp == null) return;

      // unfortunate that we do this here for the JPEG generation
      // and not for the other two files, but that's how it works.
      //
      if (isLocalOrder && Nullable.nbToB(config.jpegDisableForLocal)) return;
      if (invoiceShipToHome) {
         if ( ! config.jpegEnableForHome    ) return;
      } else {
         if ( ! config.jpegEnableForAccount ) return;
      }

      boolean jpegShowItems = false; // always just summary

      parameters.put("label",new Boolean(false));
      parameters.put("timestamp",timestampInvoice);
      parameters.put("home",new Boolean(invoiceShipToHome));
      parameters.put("shipmethod","");
      parameters.put("product",   "");
      if (ms2) parameters.put("markshipvalue","");
      //
      parameters.put("items",new Boolean(       jpegShowItems));
      parameters.put("color",new Boolean(config.jpegShowColor));
      parameters.put("disposition",new Boolean( ! jpegShowItems ));
      parameters.put("enlargetype",new Boolean( ! jpegShowItems ));
      //
      XML.writeFile(jpegTemp,doc,stylesheet,parameters,null);
   }

   private static String denull(String s) { return (s == null) ? "" : s; }

// --- part 2 : function that auto-prints selected products ---

   // just to clarify, the enableFor flags tell whether we should print *automatically*
   // from the invoice thread and reprint thread.  the HTML viewer's print function
   // always prints what you're viewing, anything else would be too counterintuitive.

   // (**)
   // the shipPrint flags let us choose label / postage output.
   // the label file is always there, we just might not use it.
   //
   // this two-tier structure is super-confusing, so here's a
   // table that explains it.  remember that shipping is home,
   // pickup is account.
   //
   // type of output     enable
   // -----------------  --------------------------------------------
   // shipping invoice   setupInvoice.enableForHome
   // pickup invoice     setupInvoice.enableForAccount
   //
   // shipping label     setupLabel.enableForHome && shipPrintLabel
   // shipping postage   setupLabel.enableForHome && shipPrintPostage
   // pickup label       setupLabel.enableForAccount
   // shipping prod lbl  productEnableForHome
   // pickup prod lbl    productEnableForAccount
   //
   // shipping JPEG inv  jpegEnableForHome
   // pickup JPEG inv    jpegEnableForAccount
   //
   // the disableForLocal flags apply to entire groups of outputs (2+5+2).
   //
   // we always generate files for the invoice and the shipping and
   // pickup labels.  we only generate files for the a JPEG invoice
   // and the product labels if they're enabled.
   //
   // if we ever add a GUI for postage settings, here's a suggestion
   // for how to map two real checkboxes to the three boolean fields.
   //
   // label  postage      eFH  sPL  sPP
   // -----  -------  ->  ---  ---  ---
   //   .       .          .    ?    ?
   //   .       x          x    .    x
   //   x       .          x    x    .
   //   x       x          x    x    x

   public void print(int mode) throws Exception {

      PrintConfig.Setup setup;
      int copies;
      String footer;
      String title;
      String shipMethod;
      boolean shipToHome;
      File file;

      switch (mode) {

      case Print.MODE_INVOICE:

         setup = config.setupInvoice;
         if (isLocalOrder && Nullable.nbToB(setup.disableForLocal)) break;

         copies = config.copies;
         footer = null;
         title = invoiceTitle;

         shipMethod = null; // not used for invoice
         shipToHome = invoiceShipToHome;
         file = invoiceFile;

         if (shipToHome) {
            if ( ! setup.enableForHome    ) break;
         } else {
            if ( ! setup.enableForAccount ) break;
         }

         printImpl(file,copies,footer,title,mode);

         break;

      case Print.MODE_LABEL:

         setup = config.setupLabel;
         if (isLocalOrder && Nullable.nbToB(setup.disableForLocal)) break; // disable applies to all outputs

         copies = labelCopies;
         footer = labelFooter;
         title = labelTitle;

         Iterator i = labelInfos.iterator();
         while (i.hasNext()) {
            LabelInfo labelInfo = (LabelInfo) i.next();

            shipMethod = labelInfo.shipMethod;
            shipToHome = Order.isShipToHome(shipMethod);
            file = labelInfo.labelFile;

            if (labelInfo.product == null) { // normal label

               if (shipToHome) {
                  if ( ! setup.enableForHome    ) continue;
               } else {
                  if ( ! setup.enableForAccount ) continue;
               }

               boolean flagPrint    = shipToHome ? config.shipPrintLabel   : true;
               boolean flagGenerate = shipToHome ? config.shipPrintPostage : false;
               // (**)

               if (flagPrint) printImpl(file,copies,footer,title,mode);
               if (flagGenerate) generatePostage(shipMethod);

            } else { // product label

               if (shipToHome) {
                  if ( ! config.productEnableForHome    ) continue;
               } else {
                  if ( ! config.productEnableForAccount ) continue;
               }
               // this is redundant because enumerateLabelInfo has
               // already checked enables, but keep it for clarity.

               printImpl(file,copies,footer,title,mode);
            }
         }

         // call printImpl once per shipping type.  if the user's got labelPerQueue turned on,
         // that will multiply with the shipping types, which isn't great but is good enough
         // for now.  the purpose of labelPerQueue is to deal with cases where the queues print
         // in different locations and ship separately, so really you want to intersect the
         // shipping type SKU sets with the queue SKU sets to get the smallest partition, then
         // print one label for each set.  once we switch from IFP to Opie and get the correct
         // attribute info into the order summary, that will be feasible if we want it.
         // in other words you want to call countQueues with the SKUs for each shipping type.

         break;

      default:
         throw new IllegalArgumentException();
      }
   }

   private void printImpl(File file, int copies, String footer, String title, int mode) throws Exception {

      HTMLEditorPane editorPane = new HTMLEditorPane(file.toURL());

      // if you use the editor pane without it being connected to a window,
      // you get assertion failures that cause the JVM to exit immediately.
      // so, don't do that.
      // in theory there should be problems with using "realized" objects
      // in a background thread, too, but I haven't seen any sign of that.

      JFrame frame = new JFrame();
      frame.getContentPane().add(editorPane);
      frame.pack();

      try {
         Print.print(editorPane,copies,footer,title,mode);
      } finally {
         frame.dispose();
      }
   }

   private void generatePostage(String shipMethod) throws Exception {

      // shipMethod is definitely not null here, and we rely on that in the findMapping call.
      // the denull below is just good form.

      File postageFile = new File(config.postageDir,config.postageBatchMode ? config.postageBatchFile
                                                                            : FileUtil.disambiguate(config.postageDir,postageOrderID + ".xml"));

      HashMap parameters = new HashMap();
      parameters.put("shipmethod",denull(shipMethod));

      PrintConfig.PostageMapping mv = PrintConfig.findMapping(config.postageMappings,shipMethod);
      if (mv != null) {
         PrintConfig.PostageMapping mn = (PrintConfig.PostageMapping) config.postageMappings.getFirst();

         Iterator in = mn.values.iterator();
         Iterator iv = mv.values.iterator();
         while (in.hasNext()) {
            parameters.put(in.next(),iv.next()); // lists are same length by validation
         }
      }

      parameters.put("description",getDescription(orderObjectSave));

      InputStream stream = new FileInputStream(postageXSL);
      try {
         Object transform = stream;

         Document frameDoc;
         boolean frameIndent = false;
         if (config.postageBatchMode && postageFile.exists()) { // load existing frame

            frameDoc = XML.readFile(postageFile);

         } else { // build new frame

            Document temp = XML.createDocument();
            XML.createElement(temp,"Frame");
            frameDoc = XML.createDocument();
            transform = XML.transform(frameDoc,temp,transform,parameters);
            frameIndent = true;
         }

         Document itemDoc = XML.createDocument();
         transform = XML.transform(itemDoc,orderDoc,transform,parameters);

         Node frameNode = (Node) frameDoc.getDocumentElement();
         Node itemNode  = (Node) itemDoc .getDocumentElement();
         if (frameNode == null || itemNode == null) throw new Exception(Text.get(this,"e3"));
         // shouldn't happen, but since the XML and XSL exist as files,
         // it's possible they could be messed up (unlike Lucidiom which is built in memory)

         if (frameIndent) XML.addIndentation(frameNode);

         // need to normalize so that strings concatenated by XSL don't get indented
         itemNode.normalize();
         XML.addIndentation(itemNode);

         // notes about xmlns:
         //
         // 1. the problem is, a frame with xmlns plus an item without xmlns leads to a xmlns=""
         // on the item in Java 1.5.
         //
         // 2. Java doesn't treat xmlns as just another attribute, it's connected to getNamespaceURI.
         // normally getNamespaceURI is null, even when you read a doc that has a xmlns up front ...
         // probably this is because DocumentBuilder isn't set to be namespace aware.  but, when you
         // process in memory a template with a xmlns declaration, getNamespaceURI returns non-null
         // for the document element and all its children (but not the doc itself).
         //
         // 3. but, whether or not getNamespaceURI is set, in Java 1.5 somehow that info is tracked,
         // because an item with no namespace is recognized as foreign and gets xmlns="" thrown
         // on the item node during the file write, even when we're in batch mode and the frame doc
         // was read from a file.
         //
         // 4. a quick search online suggests there's no way to change the namespace once it's set,
         // so setting the item namespace in the XSL is the only solution.  this produces an xmlns
         // for every item in Java 1.4, but no big deal, nobody but me uses that any more.

         frameNode.appendChild(frameDoc.importNode(itemNode,true));
         frameNode.appendChild(frameDoc.createTextNode("\n"));
         // hard-coded structure, maybe we'll need to generalize some day

         XML.writeFile(postageFile,frameDoc);

      } finally {
         stream.close();
      }
   }

   private static String getDescription(Order order) {

      StringBuffer b = new StringBuffer();
      HashSet set = new HashSet();

      Iterator i = order.items.iterator();
      while (i.hasNext()) {
         Order.Item item = (Order.Item) i.next();

         String s;
         if (item.sku instanceof NewSKU) {
            s = ((NewSKU) item.sku).getProduct(); // drop attributes to keep it short
         } else { // OldSKU
            s = ((OldSKU) item.sku).toString();
         }

         if (set.add(s)) {
            if (set.size() > 1) b.append(' ');
            b.append(s);
         }
      }

      return b.toString();
   }

// --- part 3 : function that generates JPEG from temp XML ---

   /**
    * Generate an invoice JPEG.
    *
    * @return The SKU code that should be used to print the invoice JPEG.
    *         This is just a convenience for the caller,
    *         which doesn't have the PrintConfig object available.
    */
   public SKU generate(File jpegTemp, File jpegFile) throws Exception {

      HTMLEditorPane editorPane = new HTMLEditorPane(jpegTemp.toURL());
      JFrame frame = new JFrame();
      frame.getContentPane().add(editorPane);
      frame.pack();
      // same remarks about editor pane apply; try-finally makes it hard to factor

      try {
         if (config.barcodeEnable) generateWithBarcode   (editorPane,jpegFile);
         else                      generateWithoutBarcode(editorPane,jpegFile);
      } finally {
         frame.dispose();
      }

      return config.jpegSKU;
   }

   private static class Buffer {
      public BufferedImage image;
      public Graphics2D g;
   }

   private void generateWithoutBarcode(Component component, File jpegFile) throws Exception {

   // compute sizes, as in PrintScaled constructor (except we don't know size goal in advance)

      // although Graphics2D can handle floating-point coordinates,
      // here the original is an integer number of pixels,
      // and we're only translating and rotating, not scaling.

      // rotation doesn't enter here, the aspect ratio is relative to the invoice

      Dimension size = component.getPreferredSize();
      double ratio = config.jpegHeight.doubleValue() / config.jpegWidth.doubleValue();

      int widthRel, heightRel;
      if (size.width * ratio >= size.height) {
         widthRel  = size.width;
         heightRel = (int) Math.round(size.width  * ratio);
      } else {
         widthRel  = (int) Math.round(size.height / ratio);
         heightRel = size.height;
      }
      // in the first case, width * ratio >= height, so heightRel >= height
      // in the second case, width * ratio < height,
      //                  so height / ratio > width, and widthRel >= width (after rounding)

      int translateX = (widthRel  - size.width ) / 2;
      int translateY = (heightRel - size.height) / 2;

      Buffer b = prepareBuffer(widthRel,heightRel);

   // render component, as in PrintScaled.print

      // in UploadTransform it was best to use an AffineTransformOp
      // to get the bilinear filtering, but here there's no scaling,
      // so we might as well just draw the image in the right place.

      b.g.translate(translateX,translateY);

      component.setBounds(0,0,size.width,size.height);
      //
      // this is really strange ... the problem was, the last few bits
      // of the invoice weren't getting drawn.
      // by fooling around, I figured out that the drawing was restricted
      // to the component bounds, which in turn were restricted to be
      // slightly less than the screen size by the frame packing algorithm.
      // so, that's fine, but ... why has this never affected printing??
      //
      // best guess, there's some sneaky internal communication going on.
      // the component is looking at some property of the graphics
      // to tell whether it's connected to the screen or a printer,
      // and is ignoring the component bounds when rendered on a printer.

      component.print(b.g);

   // write image to file, as in UploadTransform

      b.g.dispose();
      writeJpegFile(b.image,jpegFile);
   }

   private void generateWithBarcode(Component component, File jpegFile) throws Exception {

      // generate a JPEG with a barcode in the lower left corner.
      // this is different enough from a non-barcode JPEG
      // that it's worth putting the code in a separate function.

   // compute sizes (much simpler here!)

      double ratio = config.jpegHeight.doubleValue() / config.jpegWidth.doubleValue();

      int widthRel = config.barcodeWidthPixels.intValue();
      int heightRel = (int) Math.round(widthRel * ratio);

      Buffer b = prepareBuffer(widthRel,heightRel);

   // draw invoice

      int overlap = config.barcodeOverlapPixels.intValue();

      int widthUse = widthRel;
      int heightUse = heightRel - Barcode.getTotalHeight(config.barcodeConfig) + overlap;

      // this part is just like PrintScaled, except I optimize a little to avoid overlap

      Dimension size = component.getPreferredSize();
      double scale, translateX = 0, translateY = 0;

      double scaleX = widthUse  / (double) size.width;
      double scaleY = heightUse / (double) size.height;

      // use smaller scale, center in other direction
      if (scaleX < scaleY) {
         scale = scaleX;
         double excess = heightUse - size.height * scale;
         // up to overlap is absorbed with zero translation;
         // after that translate by half, as before
         if (excess > overlap) translateY = (excess - overlap) / 2;
      } else {
         scale = scaleY;
         translateX = (widthUse - size.width * scale) / 2;
      }

      AffineTransform transform = b.g.getTransform(); // gsave

      b.g.translate(translateX,translateY);
      b.g.scale(scale,scale);

      component.setBounds(0,0,size.width,size.height);
      component.print(b.g);

      b.g.setTransform(transform); // grestore

   // draw barcode (on top of invoice, if it comes to that)

      if (barcodePrice != null) Barcode.drawBarcode(b.g,config.barcodeConfig,0,heightRel,config.barcodePrefix + barcodePrice,config.barcodePrefixReplace);
      else                      Barcode.drawBox    (b.g,config.barcodeConfig,0,heightRel);

   // write to file

      b.g.dispose();
      writeJpegFile(b.image,jpegFile);
   }

   /**
    * Given a relative width and height, create a buffered image
    * in the correct orientation and rotate the coordinates
    * so that the graphics area has the desired width and height;
    * also erase the background.
    */
   private Buffer prepareBuffer(int widthRel, int heightRel) throws Exception {

   // add border space

      widthRel  += 2 * config.jpegBorderPixels;
      heightRel += 2 * config.jpegBorderPixels;

   // account for rotation

      int rotation;
      switch (config.jpegOrientation) {
      case PageFormat.PORTRAIT:           rotation = 0;  break;
      case PageFormat.LANDSCAPE:          rotation = 1;  break;
      case PageFormat.REVERSE_LANDSCAPE:  rotation = 3;  break;
      default:  throw new IllegalArgumentException();
      }

      int widthAbs, heightAbs;
      if (Rotation.isEven(rotation)) {
         widthAbs  = widthRel;
         heightAbs = heightRel;
      } else {
         widthAbs  = heightRel;
         heightAbs = widthRel;
      }

   // construct buffer, as in UploadTransform

      Buffer b = new Buffer();
      try {
         b.image = new BufferedImage(widthAbs,heightAbs,BufferedImage.TYPE_INT_RGB);
      } catch (OutOfMemoryError e) {
         throw new Exception(Text.get(this,"e2"));
      }
      // now that the width is configurable, we're bound to get out of memory errors,
      // so catch them and replace them with something that will get out to the user.

   // finish up

      b.g = (Graphics2D) b.image.getGraphics();

      b.g.setColor(Color.white);
      b.g.fillRect(0,0,widthAbs,heightAbs);

      b.g.transform(Rotation.corner(rotation,widthRel,heightRel));
      // use of invoice-relative width and height, strange but correct

      b.g.translate(config.jpegBorderPixels,config.jpegBorderPixels);

      // it would be hard to get a try-finally in the right place to guarantee
      // a call to Graphics.dispose, but fortunately it's not necessary ...
      // all the exceptions arise from out of memory, above, and writeJpegFile,
      // so we can just dispose before starting the write.

      return b;
   }

   private void writeJpegFile(BufferedImage image, File jpegFile) throws Exception {

      FileImageOutputStream fios = null;
      ImageWriter writer = null;

      try {

         Iterator i = ImageIO.getImageWritersBySuffix("jpg");
         if ( ! i.hasNext() ) throw new Exception(Text.get(this,"e1"));
         writer = (ImageWriter) i.next(); // take first writer

         FileUtil.makeNotExists(jpegFile);
         // else ImageWriter will overwrite without truncating,
         // leaving unpredictable junk at the end of the file.

         fios = new FileImageOutputStream(jpegFile);
         writer.setOutput(fios);

         // scale compression into range 0-1
         double c = ((double) (config.jpegCompression)) / 100;

         ImageWriteParam param = writer.getDefaultWriteParam();
         param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
         param.setCompressionType("JPEG");
         param.setCompressionQuality((float) c);

         writer.write(null,new IIOImage(image,null,null),param);
         fios.flush(); // ImageIO.write does this, seems reasonable

      } finally {

         if (writer != null) writer.dispose();

         try {
            if (fios != null) fios.close();
         } catch (Exception e) {
            // ignore
         }
      }
   }

// --- part 4 : barcode helper functions ---

   // this is the same logic as in invoice.xsl -- see LabelPayPre, LabelPay,
   // and PrimaryPaymentAmountTaxQ
   //
   // I guess this is also the place to explain the payment data structures,
   // even though most of the usage is in the XSL and not here.
   //
   // in the old payment system, info about the single payment was stored
   // in OrdersNew, and came down in the Payment block in the order XML.
   // the amount had to be the same as GrandTotal and was not sent down separately.
   //
   // in the new payment system, there can be multiple payments in a new table and
   // maybe still one in OrdersNew.  in the XML, we make the following changes:
   //
   // * add a field Payment/Amount in all cases
   //
   // * allow Payment/Type to be 'No Primary Payment' meaning don't use that block.
   //   Payment/Amount shouldn't be used in that case, but it's set to 0.00 anyway
   //   to avoid the potential for crazy bugs.  probably the least confusing way to
   //   think about it is to say yes there's always a payment, but sometimes it has
   //   this type with a confusing name that we don't want to show.
   //   BTW the point of the name is to give a clue to anyone who doesn't realize
   //   there's a new XML format.
   //
   // * add PaymentItem sections for additional payments.  these may not have Type
   //   equal to 'Pay In Store' or 'On Account' (or 'No Primary Payment' of course)
   //   because that would complicate life for LC for no good reason.  for example,
   //   we aren't going to allow two 'Pay In Store' records to force two barcodes!
   //
   // because of that last restriction, the mapping between payments in the DB and
   // payments in the XML may not be completely simple.  as long as the new payments
   // are just gift cards and stuff, no problem.  but, if we shut down the OrdersNew
   // payments and move everything into the new system, we have to search for the
   // unique 'Pay In Store' or 'On Account' item in the new table (if it exists) and
   // send it down in the old Payment block since that's the only place it's allowed.
   //
   private static String getBarcodePrice(Node n1, boolean includeTax) {

      // never abort regular invoice processing because of a problem here.
      // null is a valid result that means show a box in the barcode area.
      try {

         // is this even a pay-in-store order at all?
         Node n2 = XML.getElement(n1,"Payment");
         if ( ! XML.getElementText(n2,"Type").equals("Pay In Store") ) return null;

         // we don't use this in all cases, but go ahead and parse it,
         // it's always there and it simplifies the logic
         int total = Convert.toCents(XML.getElementText(n1,"GrandTotal"));

         String temp = XML.getNullableText(n2,"Amount");
         int amount = (temp != null) ? Convert.toCents(temp) : total;

         if ( ! includeTax ) {

            int tax = Convert.toCents(XML.getElementText(n1,"Tax"));

            if (total < 0 || tax < 0 || tax > total) return null; // nonsense

            // I think total could legitimately be zero, like if the user has
            // a coupon that pays for the entire order.  no adjustment needed
            // in that case (since the amount must be zero, but don't check that).
            //
            if (total != 0) {
               double d = amount;
               d *= total-tax;
               d /= total;
               amount = (int) Math.round(d);
            }
         }

         Convert      .validateNDigit(4,amount); // also catches negatives
         return Convert.fromIntNDigit(4,amount);

      } catch (Exception e) {
         return null;
      }
   }

   private static void createProductBarcodes(Node n1, int productBarcodeType) throws Exception {

      ProductBarcodeConfig productBarcodeConfig = Print.getProductBarcodeConfig();
      HashMap productBarcodeMap = Print.getProductBarcodeMap();

      Iterator i1 = XML.getElements(n1,OrderParser.NAME_SUMMARY);
      while (i1.hasNext()) {
         Node n2 = (Node) i1.next();

         if ( ! OrderParser.isProducedHere(n2) ) continue; // can't be pay in store, don't add barcode

         Iterator i2 = XML.getElements(n2,OrderParser.NAME_SUMMARY_PRODUCT);
         while (i2.hasNext()) {
            Node n3 = (Node) i2.next();

            Node barcode = XML.createElement(n3,"Barcode");
            try {
               Barcode.Pair p = createProductBarcode(n3,productBarcodeType,productBarcodeConfig,productBarcodeMap);
               if (p.display == null || p.encoded == null) throw new BarcodeException(0); // shouldn't happen but I want to be 100% sure
               XML.createElementText(barcode,"d",p.display);
               createBarcodeElements(barcode,p.encoded);
            } catch (BarcodeException e) {
               createBarcodeElements(barcode,"101");
               XML.createElementText(barcode,"e",Convert.fromInt(e.getCode()));
               createBarcodeElements(barcode,"101");
            }
            // it might be better to replace BarcodeException with a message with proper text,
            // and let the order get paused at InvoiceThread instead of flowing through,
            // but I'm worried that there might be some Office Depot products (maybe add-ons?)
            // that would get stuck.  this way there's less chance of breaking things.
            // createProductBarcode can throw other exceptions, but those have been there all along,
            // so it's OK to keep them.
         }
      }
   }

   private static void createBarcodeElements(Node barcode, String s) {
      char[] c = s.toCharArray();
      for (int i=0; i<c.length; i++) {
         XML.createElement(barcode,(c[i] == '0') ? "o" : "l");
      }
   }

   private static class BarcodeException extends Exception {
      private int code;
      public BarcodeException(int code) { this.code = code; }
      public int getCode() { return code; }
   }

   private static Barcode.Pair createProductBarcode(Node n3, int productBarcodeType, ProductBarcodeConfig productBarcodeConfig, HashMap productBarcodeMap) throws Exception {

      String baseSKU = XML.getNullableText(n3,SKUParser.BASE_SKU);
      if (baseSKU == null) throw new BarcodeException(1);
      // should always be there, but if it's not, what can we do?
      // note, the point of the SKU conversion feature was to
      // keep old numeric SKU codes without attributes for certain
      // dealers.  so, if that's on, probably we want to read from
      // the regular SKU instead of BaseSku.  but, let's worry
      // about that if/when it happens.  maybe they won't even use
      // the barcode feature.

      if (productBarcodeConfig.enableMap) {
         String temp = (String) productBarcodeMap.get(baseSKU);
         if (temp != null) {
            baseSKU = temp;
         } else {
            if (productBarcodeConfig.forceMap) throw new BarcodeException(2);
         }
         // although the field names make it look like a product code table,
         // productBarcodeMap is actually a complete SKU substitution table,
         // even for CODE128_STRING.
      }

      switch (productBarcodeType) {

      case ProductBarcode.TYPE_CODE128_DIGITS:
         baseSKU = getNumericPrefix(baseSKU);
         if (baseSKU == null) throw new BarcodeException(3);
         // fall through

      case ProductBarcode.TYPE_CODE128_STRING:
         return new Barcode.Pair(baseSKU,Code128.toCode128(baseSKU)); // can throw ValidationException

      case ProductBarcode.TYPE_EAN13_20_5_5:
         baseSKU = getNumericPrefix(baseSKU);
         if (baseSKU == null) throw new BarcodeException(3);
         if (baseSKU.length() != 5) throw new BarcodeException(5);
         String price = getPrice(n3,"Subtotal",5); // not "SubTotal" as in totals area
         if (price == null) throw new BarcodeException(4);
         return Barcode.toEAN(2,"0" + baseSKU + price); // result already Barcode.Pair

      default:
         throw new Exception(Text.get(Invoice.class,"e4",new Object[] { ProductBarcode.fromType(productBarcodeType) }));
      }
   }

   private static boolean isDigit(char c) { return (c >= '0' && c <= '9'); }

   private static String getNumericPrefix(String s) {
      int i = 0;
      int len = s.length();
      while (i < len && isDigit(s.charAt(i))) i++;
      return (i == 0) ? null : s.substring(0,i);
   }

   private static String getPrice(Node node, String name, int n) {
      try {
         int price = Convert.toCents(XML.getElementText(node,name));
         Convert.validateNDigit(n,price);
         return Convert.fromIntNDigit(n,price);
      } catch (Exception e) {
         return null;
      }
   }

}

