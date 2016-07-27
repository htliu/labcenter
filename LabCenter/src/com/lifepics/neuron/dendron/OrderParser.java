/*
 * OrderParser.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Nullable;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.misc.FileUtil;
import com.lifepics.neuron.object.XML;
import com.lifepics.neuron.struct.SKU;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.TimeZone;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * A utility class for parsing an order object out of a server XML file.
 * This is how the fields in Order but not in OrderStub are filled in.<p>
 *
 * The class is not designed to allow instances to be reused.
 */

public class OrderParser implements XML.Persist {

   private int version;

   private SKUParser.FlagRewriter rewriter;
   private SKUParser skuParser; // initialized in load, which is the only real entry point

   private Order order;
   private int wsc;
   private HashMap conversionMap;
   private HashMap bookMap;
   private boolean enableItemPrice;
   private int setNumber;
   private HashMap setNumberMap; // unique filename to Integer set number, for redisambiguate
   private boolean localOrder;
   private HashMap summaryMap;

   public OrderParser(Order order, int wsc, HashMap conversionMap, HashMap bookMap, boolean enableItemPrice, boolean localOrder, HashMap summaryMap) {
      this.order = order;
      this.wsc = wsc;
      this.conversionMap = conversionMap;
      this.bookMap = bookMap;
      this.enableItemPrice = enableItemPrice;
      setNumber = 1;
      setNumberMap = new HashMap();
      this.localOrder = localOrder;
      this.summaryMap = summaryMap;
   }

   public void validate() throws ValidationException { order.validate(); }
   public void store(Node node) { throw new UnsupportedOperationException(); }
   public void tstore(int t, Node node) { throw new UnsupportedOperationException(); }

// --- notes ---

   // the parser wraps the order object and replaces
   // the basic persistence scheme with a second one

   // the XML tag names only appear once each,
   // since we're loading but not storing,
   // so there's no need for them to be constants

   /**
    * The top-level tag name, for use by the caller.
    */
   public static final String NAME_ORDER = "Order";

   public static final String NAME_ORDER_ID = "ID";
   public static final String NAME_ORDER_TIME = "OrderTime";
   public static final String NAME_DOWNLOAD_URL = "ImageURL";
   public static final String NAME_DOWNLOAD_SIZE = "ImageStreamSize";
   public static final String NAME_SUMMARY = "OrderSummary";
   public static final String NAME_SUMMARY_PRODUCT = "Product";
   public static final String NAME_SUMMARY_SKU = "SKU";
   public static final String NAME_SUMMARY_DESC = "Description";

// --- shared functions ---

   // these are used by Beaufort integration too;
   // better to share the code than just the string constants.

   public static int getVersion(Node order) throws ValidationException {
      int version;

      String s = XML.getAttributeTry(order,"Version");
      if (s != null) {
         version = Convert.toInt(s);
      } else {
         version = 1; // original version had no version attribute
      }
      if (version < 1 || version > 2) throw new ValidationException(Text.get(OrderParser.class,"e7",new Object[] { Convert.fromInt(version) }));

      return version;
   }

   public static Iterator getItems(Node order) throws ValidationException {
      Node items = XML.getElement(order,"Items");
      return XML.getElements(items,"Item");
   }

   public static Iterator getImages(Node item, int version) throws ValidationException {
      if (version >= 2) { // several images per item
         Node images = XML.getElement(item,"Images");
         return XML.getElements(images,"Image");
      } else { // version 1, just one image per item
         return Collections.singletonList(item).iterator();
      }
   }

   public static String getDownloadURL(Node image) throws ValidationException {
      return XML.getElementText(image,NAME_DOWNLOAD_URL);
   }

   public static boolean isProducedHere(Node summary) throws ValidationException {
      Boolean here = Convert.toNullableBool(XML.getNullableText(summary,"ProducedHere"));
      return (here == null || here.booleanValue()); // null means yes, for older IFP
   }

// --- top-level helpers ---

   public static void writePrefix(OutputStream stream, File stylesheet) throws IOException {
      PrintWriter printWriter = new PrintWriter(stream);
      printWriter.println("<?xml-stylesheet type=\"text/xsl\" href=\"" + Convert.fromFile(stylesheet) + "\"?>");
      printWriter.flush();
   }

   private static void skipPrefix(InputStream stream) throws IOException {
      // this is sloppy in how it treats bytes as characters
      while (true) {
         int c = stream.read();
         if (c == -1 || c == '\n') return;
      }
   }

   /**
    * Replace XML.readFile with OrderParser.readFile to skip prefix.
    */
   public static Document readFile(File file) throws IOException {
      Document doc;

      // if an XML version declaration is present, it must come first,
      // so prefixing an xml-stylesheet processing instruction is incorrect.
      // the right solution would be to insert the instruction;
      // the quick solution is to skip the instruction when parsing.
      // the XML parser that the users will use to view and print invoices
      // doesn't seem to care that it's incorrect, so that part is OK.

      InputStream stream = new FileInputStream(file);
      try {
         skipPrefix(stream);
         doc = XML.readStream(stream);
      } finally {
         stream.close();
      }

      return doc;
   }

// --- local order helpers ---

   /**
    * Basically getElementText, but becomes getNullableText for local orders.
    */
   private String getLocalNullable(Node node, String name) throws ValidationException {
      return localOrder ? XML.getNullableText(node,name) : XML.getElementText(node,name);
   }

   /**
    * Basically getAttribute, but becomes getAttributeTry for local orders.
    */
   private String getLocalAttribute(Node node, String name) throws ValidationException {
      return localOrder ? XML.getAttributeTry(node,name) : XML.getAttribute(node,name);
   }

   /**
    * Use this for the four fields I mentioned at the start of Order.java,
    * where technically right null is replaced by convenient empty string.
    */
   private static String denull(String s) { return (s == null) ? "" : s; }

   /**
    * A record class used to compute order summaries for local orders.
    * The SKU is redundant but convenient.  All fields except the SKU
    * and the quantity can be set to null to indicate inconsistent data
    * or unknown values.
    */
   public static class SummaryRecord {

      public SKU sku;
      public String description;
      public int quantity;
      public Integer displayQuantity;
      public Integer price;
      public Integer total;
   }

   private static void accumulate(HashMap summaryMap, SKU sku, int quantity, Node nItem) throws ValidationException {

   // collect other fields

      // skip over errors here, we just want a best attempt

      String description = XML.getNullableText(nItem,"ProductDescription");
      String s;
      Integer displayQuantity = null;
      Integer price = null;
      Integer total = null;

      s = XML.getNullableText(nItem,"DisplayQty");
      if (s != null) try {
         displayQuantity = new Integer(Convert.toInt(s));
      } catch (Exception e) {
      }

      s = XML.getNullableText(nItem,"Price");
      if (s != null) try {
         price = new Integer(Convert.toCents(s));
      } catch (Exception e) {
      }

      s = XML.getNullableText(nItem,"Total");
      if (s != null) try {
         total = new Integer(Convert.toCents(s));
      } catch (Exception e) {
      }

   // create or update record

      // can't combine these into one control flow because the null
      // in the new record would be taken to mean inconsistent data.

      SummaryRecord r = (SummaryRecord) summaryMap.get(sku);
      if (r == null) { // create

         r = new SummaryRecord();
         summaryMap.put(sku,r);

         r.sku = sku;
         r.description = description;
         r.quantity = quantity;
         r.displayQuantity = displayQuantity;
         r.price = price;
         r.total = total;

      } else { // update

         // we already know the SKU is a match
         r.description = (String) accumulateMatch(r.description,description);
         r.quantity += quantity;
         r.displayQuantity = accumulateSum(r.displayQuantity,displayQuantity);
         r.price = (Integer) accumulateMatch(r.price,price);
         r.total = accumulateSum(r.total,total);
      }
   }

   private static Object accumulateMatch(Object o1, Object o2) {
      return (o1 != null && o2 != null && o1.equals(o2)) ? o1 : null;
   }

   /**
    * Standard database addition of nullable integers.
    */
   private static Integer accumulateSum(Integer i1, Integer i2) {
      return (i1 != null && i2 != null) ? new Integer(i1.intValue() + i2.intValue()) : null;
   }

// --- load function ---

   public static SimpleDateFormat getDateFormat() {

      SimpleDateFormat format = new SimpleDateFormat(Text.get(OrderParser.class,"f1"));
      format.setLenient(false);
      format.setTimeZone(TimeZone.getTimeZone("US/Mountain"));
      // because the server sends the time as mountain time

      return format;
   }

   private Date loadInvoiceDate(Node node) throws ValidationException {

      SimpleDateFormat format = getDateFormat();
      try {
         String s = getLocalNullable(node,NAME_ORDER_TIME);
         return (s == null) ? null : format.parse(s);
      } catch (Exception e) {
         throw new ValidationException(Text.get(OrderParser.class,"e4"),e);
      }
   }

   public boolean needRewrite() { return (rewriter != null && rewriter.needRewrite()); }

   private String getItemPrice(Node node) throws ValidationException {
      return enableItemPrice ? XML.getElementText(node,"Price") : null;
      // price should always be there as far as I know, but since we
      // only need it for Meijer DNP integration, let's not require it.
   }

   public Object load(Node node) throws ValidationException {

   // fields

      String expectID;
      if (order.wholesale != null) {
         expectID = Convert.fromInt(order.wholesale.merchantOrderID);
      } else {
         expectID = order.getFullID();
      }
      // by validation, orderSeq must be null in the wholesale case

      // technically, this is a validation, but we have to do it now
      // because there's no place to remember the alternate order ID
      //
      String loadID = XML.getAttribute(node,NAME_ORDER_ID);
      if ( ! loadID.equals(expectID) ) {
         throw new ValidationException(Text.get(this,"e3",new Object[] { loadID, expectID }));
      }

      version = getVersion(node);

      rewriter = new SKUParser.FlagRewriter(new SKUParser.StandardRewriter());
      skuParser = new SKUParser(/* wholesale = */ (wsc == Order.WSC_WHOLESALE || wsc == Order.WSC_PRO),
                                /* allowOld  = */ (version <  2),
                                /* allowNew  = */ (version >= 2), // new shopping cart is version 2
                                /* rewriter  = */ rewriter,
                                /* conversionMap = */ conversionMap);
      // the wholesale flag is really a "use affiliate SKU" flag, so we can just
      // set it to the right value based on the wsc, no need to change SKUParser.

      Node dealer = XML.getElementTry(node,"Dealer");
      if (dealer != null) {
         order.dealerName = XML.getAttributeTry(dealer,"Name");
      }
      // this is probably always there, but don't require it

      Node customer = XML.getElement(node,"Customer");
      Node billTo = XML.getElement(customer,"BillTo");
      Node shipTo = XML.getElement(customer,"ShipTo");

      order.nameFirst = denull(getLocalNullable(customer,"First"));
      order.nameLast  = denull(getLocalNullable(customer,"Last"));
      order.email = denull(getLocalAttribute(customer,"ID"));
      order.lpCustomerID = getLocalAttribute(customer,"Number"); // ID was taken
      order.phone = denull(getLocalNullable(billTo,"Phone"));

      Node special = XML.getElement(node,"SpecialInstructions");

      String s = getLocalNullable(special,"Message");
      order.specialInstructions = (s != null && s.length() != 0) ? s : null;

      // special SKU is derived from order items

      order.subtotal = getLocalNullable(node,"SubTotal");
      //
      Node discount = XML.getElement(node,"Discount");
      order.discountFreePrints = getLocalNullable(discount,"FreePrints");
      order.discountCredit = getLocalNullable(discount,"Credit");
      order.discountPercent = getLocalNullable(discount,"PercentDiscount");
      order.discountPermanent = getLocalNullable(discount,"PermanentDiscount");
      //
      order.shippingAmount = getLocalNullable(node,"Shipping");
      order.taxAmount = getLocalNullable(node,"Tax");
      order.grandTotal = getLocalNullable(node,"GrandTotal");

      order.company = getLocalNullable(billTo,"Company");
      order.street1 = getLocalNullable(billTo,"Street1");
      order.street2 = getLocalNullable(billTo,"Street2");
      order.city    = getLocalNullable(billTo,"City");
      order.state   = getLocalNullable(billTo,"State");
      order.zipCode = getLocalNullable(billTo,"ZIP");
      order.country = getLocalNullable(billTo,"Country");

      order.shipCompany = getLocalNullable(shipTo,"Company");
      order.shipStreet1 = getLocalNullable(shipTo,"Street1");
      order.shipStreet2 = getLocalNullable(shipTo,"Street2");
      order.shipCity    = getLocalNullable(shipTo,"City");
      order.shipState   = getLocalNullable(shipTo,"State");
      order.shipZipCode = getLocalNullable(shipTo,"ZIP");
      order.shipCountry = getLocalNullable(shipTo,"Country");
      // ship method handled below, with the order summary

      order.invoiceDate = loadInvoiceDate(node);
      order.dueStatus = Order.DUE_STATUS_NONE;

      order.format = Order.FORMAT_FLAT; // format always flat during download

   // items

      Iterator i = getItems(node);
      while (i.hasNext()) {
         Node nItem = (Node) i.next();

         SKU sku = skuParser.parse(nItem,"ProductSku","AffiliateSku");
         int quantity = Convert.toInt(XML.getElementText(nItem,"Qty"));
         String comments = XML.getNullableText(nItem,"Comments");
         String price = getItemPrice(nItem);

         if (quantity == 0) continue;
         // (*) the server is occasionally sending zero-quantity items now
         // for some mysterious reason.  they shouldn't be there, just
         // ignore them.  previously they were getting caught in validation.
         // we'll leave them in the order XML, not perfect but too bad.

         if (summaryMap != null) accumulate(summaryMap,sku,quantity,nItem);
         // for regular orders the summary might include add-ons, but for
         // local orders there are only items; don't worry about the rest.

         Iterator j = getImages(nItem,version);

         LinkedList filenames = loadFiles(j);

         // see if this is a CD product.  the rule we're using is,
         // "if a product has multiple images and no page count, it's a CD".
         // that's not perfect, but it should hold us for now.
         // in the future we may have the server tell us which products
         // are CDs, or we may make it a config setting in LabCenter.
         //
         // for local orders there's no PageCount node, so any item
         // that had multiple images would look like a CD.
         // to avoid that, add an IsCD node that tells CD-ness directly.
         //
         boolean isCD = false;

         String textCD = XML.getNullableText(nItem,"IsCD");
         if (textCD != null) {
            isCD = Convert.toBool(textCD);

         } else if (filenames.size() > 1) {
            String pageCount = XML.getNullableText(nItem,"PageCount");
            if (pageCount == null || pageCount.length() == 0 || pageCount.equals("0")) {

               // equals("0") isn't exactly the same as Convert.toInt(..) == 0,
               // but it's not worth a whole try-catch block

               isCD = true;

               // if you compare this code to the old handleArchive code, you'll see
               // there's one difference, which is that there we validate
               // that there aren't any products with the CD SKU already in the order.
               // that worked there, since add-ons were processed after all the items,
               // but it doesn't work here ... so forget it, it wasn't ever something
               // that really happened anyway.
            }
         }

         makeItems(filenames,sku,quantity,comments,price,/* pageCount = */ null,/* isArchive = */ isCD,isCD);
         // other archive types not handled in this pathway for now

         // note, if/when we move the add-on products into the regular products section,
         // items with zero images will definitely happen, for example for memory cards
         // and other off the shelf products.  LC will ignore them.
      }

   // add-ons

      // these may or may not be gone in version 2, the jury's still out on that one,
      // but we need to keep the code around so we can still handle version 1 orders.

      Node addons = XML.getElementTry(node,"Addons");
      if (addons != null) {
         i = XML.getElements(addons,"AddonItem");
         while (i.hasNext()) {
            Node addon = (Node) i.next();

            SKU sku = skuParser.parse(addon,"SKU","AffiliateSKU");
            // do this out here so that we rewrite all add-on SKUs

            if (handlePhotoBook(addon,sku)) continue; // multiple books OK
            if (handleArchive  (addon,sku)) continue;
            // else it's not something we know about
         }
      }

   // other SKU rewrites

      // I'm not sure about the affiliate SKU names here, but
      // (a) these are good guesses, and (b) the values don't matter

      Node temp;

      // split-order products
      temp = XML.getElementTry(node,"SplitOrderProducts");
      if (temp != null) {
         i = XML.getElements(temp,"Item");
         while (i.hasNext()) {
            skuParser.parse((Node) i.next(),"ProductSku","AffiliateSku");
         }
      }

      int summaryCount = 0;
      int shipMethodCount = 0;

      // order summary
      Iterator h = XML.getElements(node,NAME_SUMMARY);
      while (h.hasNext()) {
         temp = (Node) h.next();

         Order.ShipMethod shipMethodObject = null;

         String shipMethod = XML.getNullableText(temp,"ShipMethod");
         if (shipMethod != null) {
            if (isProducedHere(temp)) {
               shipMethodObject = new Order.ShipMethod(shipMethod);
               order.shipMethods.add(shipMethodObject);
            }
            shipMethodCount++; // count ShipMethod nodes even if they're not produced here
         }
         summaryCount++;

         i = XML.getElements(temp,NAME_SUMMARY_PRODUCT);
         while (i.hasNext()) {
            Node product = (Node) i.next();
            skuParser.parse(product,NAME_SUMMARY_SKU,"AffiliateSKU");
            if (shipMethodObject != null) {
               shipMethodObject.products.add(XML.getElementText(product,NAME_SUMMARY_DESC));
            }
         }
      }

      // be pretty rigid about the numbers here, since the invoice.xsl code
      // is going to be pretty fragile

      if (summaryCount == 0) throw new ValidationException(Text.get(this,"e8"));

      if (summaryCount == 1 && shipMethodCount == 0) { // old format

         // there were no ship methods, so it's an old-format order;
         // fall back to the old way of parsing.
         // this covers local orders, too, but not in the way you'd
         // think -- the placeholder node gets counted as a summary
         // with no ship method filled in.

         String shipMethod = getLocalNullable(shipTo,"Comment");
         if (shipMethod != null) order.shipMethods.add(new Order.ShipMethod(shipMethod));

         // no need to collect the summary products in this case

      } else { // new format

         if (shipMethodCount != summaryCount) throw new ValidationException(Text.get(this,"e9"));

         if (order.shipMethods.size() == 0) throw new ValidationException(Text.get(this,"e10"));

         // the ShipTo comment can still be present in this case, but if it's there,
         // it's just a mistaken attempt at backward compatibility .. so, ignore it.
      }

   // files are filled in as items are loaded

      if (order.items.size() == 0) throw new ValidationException(Text.get(this,"e11"));
      //
      // this happens occasionally, rerouted orders and things like that.
      // in the past they fell through and errored out in autospawn, but
      // now that we're opening that up, we need to close this.
      // notes:
      // * much better to check this before an invoice JPEG is generated!
      // * it can't be an order-level validation because existing orders
      //   with no items would make LC not start
      // * all items come from makeItems, so all items have files, so it
      //   isn't possible to have zero files and nonzero items

      return order; // convenience
   }

   private boolean handlePhotoBook(Node addon, SKU sku) throws ValidationException {

      Node pages = XML.getElementTry(addon,"Pages");
      if (pages == null) return false;
      Iterator j = XML.getElements(pages,"Page");
      if ( ! j.hasNext() ) return false;
      // as with CDs, allow the section to be either missing or empty

      int quantity = Convert.toInt(XML.getElementText(addon,"Qty"));
      String comments = XML.getNullableText(addon,"Comments");
      String price = getItemPrice(addon);
      Integer pageCount = Convert.toNullableInt(XML.getNullableText(addon,"PageCount"));
         // Opie doesn't generate nulls, but preserve compatibility w/ other sources

      if (quantity == 0) return true; // (*)

      // ok, now process the page list
      LinkedList filenames = loadFiles(j);
      makeItems(filenames,sku,quantity,comments,price,pageCount,/* isArchive = */ false,/* isCD = */ false);

      return true;
   }

   private boolean handleArchive(Node addon, SKU sku) throws ValidationException {

      Node images = XML.getElementTry(addon,"Images");
      if (images == null) return false;
      Iterator j = XML.getElements(images,"Image");
      if ( ! j.hasNext() ) return false;
      // not sure if images section will be absent, or present but empty, so handle both

      // OK, there is a nonempty images section,
      // and that means this is an archive product.
      // add these files to the ones to download,
      // and create an archive product item for each,
      // so that when you send that SKU to a queue,
      // all the images will go.

      int quantity = Convert.toInt(XML.getElementText(addon,"Qty"));
      String comments = XML.getNullableText(addon,"Comments");
      String price = getItemPrice(addon);

      if (quantity == 0) return true; // (*)

      boolean isCD;
      String archiveType = XML.getNullableText(addon,"ArchiveType");
      if (archiveType != null) {
         isCD = archiveType.equals("CD");
      } else {
         isCD = true; // before ArchiveType, all archives were CDs
      }

      // ok, now process the image list
      LinkedList filenames = loadFiles(j);
      makeItems(filenames,sku,quantity,comments,price,/* pageCount = */ null,/* isArchive = */ true,isCD);

      return true;
   }

   /**
    * All CDs are archives, so the allowed values of isArchive and isCD are FF, TF, TT.
    * Currently the only difference with CDs is that they fill specialSKU on the order.
    */
   private void makeItems(LinkedList filenames, SKU sku, int quantity, String comments, String price, Integer pageCount, boolean isArchive, boolean isCD) throws ValidationException {

      if (filenames.size() == 0) return;

      if (isCD) {

         // multiple CD products not supported, because at present
         // they'd go into the same folder and hence to the same CD.
         // but, sometimes we get glitches from the server, so what
         // we really want to do is ignore all CDs after the first.
         //
         if (order.specialSKU != null) return;

         order.specialSKU = sku; // turns out there are a few cases
         // where we can't just handle the CD like a normal product.
      }

      // notes about the if-clause:
      // * we don't make multi-image items for archives, that will take some integration rework
      // * we don't make multi-image items for photo books with one image.  that may seem
      //   strange, but in practice the one "image" is really a PDF file that we want to be
      //   able to handle just as before.
      //
      if (filenames.size() > 1 && ! isArchive) {

         // load in map before we remove covers from list
         Integer setNumberInteger = new Integer(setNumber);
         Iterator j = filenames.iterator();
         while (j.hasNext()) {
            String filename = (String) j.next();
            if ( ! setNumberMap.containsKey(filename) ) setNumberMap.put(filename,setNumberInteger);
         }
         // why check containsKey?  it's not a huge deal, but I like it.
         // right now MC images are never shared across items, so the only effect
         // will be the blank page will take the set number of the first set
         // it's in rather than the last.  maybe in the future we'll have some way
         // to reprint the same MC book with different covers or something,
         // then the whole thing will take the set number of the first set, good.

         CoverInfo c = (CoverInfo) bookMap.get(sku);
         if (c != null) {

            // print front and/or back cover with a different SKU code.
            // those items only have one image, but we send them as
            // multi-image anyway so that Lucidiom shows the page count.
            // actually, the more important reason is, multi-image lets
            // us have a set number, which lets us see which cover goes
            // with which book!

            // for local orders, the cover split doesn't show up in the
            // summary map.  that's what we want, it's just the same as
            // with downloaded orders.

            String filenameFront = (c.frontSKU != null) ? (String) filenames.removeFirst() : null;
            String filenameBack  = (c.backSKU  != null) ? (String) filenames.removeLast () : null;
            // the list has at least two elements, so both removes will succeed

            if (filenames.size() > 0) {
               order.items.add(makeMultiImageItem(filenames,sku,quantity,comments,price,pageCount,null));
            }
            // otherwise it's a validation error

            if (c.frontSKU != null) {
               LinkedList temp = new LinkedList();
               temp.add(filenameFront);
               order.items.add(makeMultiImageItem(temp,c.frontSKU,quantity,comments,/* price = */ null,/* pageCount = */ null,"s2"));
            }
            if (c.backSKU != null) {
               LinkedList temp = new LinkedList();
               temp.add(filenameBack);
               order.items.add(makeMultiImageItem(temp,c.backSKU, quantity,comments,/* price = */ null,/* pageCount = */ null,"s3"));
            }
            // put the covers after the book, again because that's how Lucidiom does it

         } else {
            order.items.add(makeMultiImageItem(filenames,sku,quantity,comments,price,pageCount,null));
         }

         setNumber++;

         // note, we allow multi-image items to contain duplicate filenames.  I think the
         // server puts page numbers on the files, but maybe there will be blank pages or
         // something.  anyway, it seems like a reasonable feature.

      } else {

         if (isArchive) price = null;
         // since for archives we generate one item per file, the concept of item price
         // doesn't make sense.  setting it to null is the best plan I can come up with.
         // note, non-archive implies just one filename here.

         Iterator j = filenames.iterator();
         while (j.hasNext()) {
            Order.Item item = makeItem((String) j.next(),sku,quantity,comments,price,pageCount);

            // in tree format, if there were two items with the same filename, SKU, and quantity,
            // the formatting process would fail.  to prevent that,
            // merge items with the same filename and SKU ... the server should do that anyway.
            //
            // this will lose comments on the second item, but it's not a case I'm worried about.
            // I am worried about the item price though, so I'll validate that that's consistent.

            Order.Item match = findMatchingItem(order,item);
            if (match == null) {
               order.items.add(item);
            } else {

               // note, both guaranteed null if enableItemPrices is false
               if ( ! Nullable.equals(match.price,item.price) ) {
                  throw new ValidationException(Text.get(this,"e12",new Object[] { sku.toString(), match.price, item.price }));
                  // don't think I've ever used a nullable string in an error message before, not sure what the best way is
               }

               if ( ! Nullable.equals(match.pageCount,item.pageCount) ) {
                  throw new ValidationException(Text.get(this,"e13",new Object[] { sku.toString(), Convert.fromNullableInt(match.pageCount), Convert.fromNullableInt(item.pageCount) }));
                  // nullable strings here too; they come out as "null"
               }

               if ( ! isArchive ) match.quantity += item.quantity;
               // else duplicate filename in archive, ignore
            }
         }
      }
   }

   private Order.Item makeItem(String filename, SKU sku, int quantity, String comments, String price, Integer pageCount) {

      Order.Item item = new Order.Item();

      item.filename = filename;
      item.status = localOrder ? Order.STATUS_ITEM_RECEIVED : Order.STATUS_ITEM_PENDING;
      item.sku = sku;
      item.quantity = quantity;
      item.comments = comments;
      // filenames stays empty
      item.price = price;
      item.pageCount = pageCount;

      return item;
   }

   private Order.Item makeMultiImageItem(LinkedList filenames, SKU sku, int quantity, String comments, String price, Integer pageCount, String key) {
      if (key == null) key = "s1";

      Order.Item item = new Order.Item();

      // I thought about other forms for the filename, notably "[12 images]", but this form
      // has the huge advantage that there's no disambiguation because set number is unique.
      // also it puts all the information in the middle where it's easier to read.

      // also note that the filename generated here needs to pass Order.Item.isMultiImage!
      // other than that, the detailed form of the filename isn't known anywhere but here.
      // so, we can change it if we want, it just won't be retroactive.

      // because the set number is unique, the SKU in Job.Ref is redundant for multi-image
      // items.  but, there's no harm in that, and it would be hard to do anything else.

      item.filename = Text.get(this,key,new Object[] { Convert.fromInt(setNumber), Convert.fromInt(filenames.size()) });
      item.status = localOrder ? Order.STATUS_ITEM_RECEIVED : Order.STATUS_ITEM_PENDING;
      item.sku = sku;
      item.quantity = quantity;
      item.comments = comments;
      item.filenames = filenames;
      item.price = price;
      item.pageCount = pageCount;

      return item;
   }

   private LinkedList loadFiles(Iterator j) throws ValidationException {
      LinkedList filenames = new LinkedList();
      while (j.hasNext()) {
         filenames.add(loadFile((Node) j.next()));
      }
      return filenames;
   }

   /**
    * @return The filename key of the OrderFile.
    */
   private String loadFile(Node node) throws ValidationException {

      String downloadURL = getDownloadURL(node);
      String originalFilename = XML.getElementText(node,"OrigImageName");
      Long size = Convert.toNullableLong(XML.getNullableText(node,NAME_DOWNLOAD_SIZE));

      Order.OrderFile file = getFile(originalFilename,downloadURL,size);

      return file.filename;
   }

// --- item matching ---

   private static Order.Item findMatchingItem(Order order, Order.Item item) {
      Iterator i = order.items.iterator();
      while (i.hasNext()) {
         Order.Item test = (Order.Item) i.next();
         if (    test.filename.equals(item.filename)
              && test.sku.equals(item.sku)           ) return test;
      }
      return null;
   }

// --- file manipulation ---

   private Order.OrderFile getFile(String originalFilename, String downloadURL, Long size) throws ValidationException {

      Order.OrderFile file = order.findFileByDownloadURL(downloadURL);
      if (file != null) {
         if ( ! originalFilename.equals(file.originalFilename) ) throw new ValidationException(Text.get(this,"e1",new Object[] { downloadURL, file.originalFilename, originalFilename }));
         if ( ! Nullable.equals(size,file.size) ) throw new ValidationException(Text.get(this,"e2",new Object[] { downloadURL, Convert.describeNullableLong(file.size), Convert.describeNullableLong(size) }));
      } else {

         file = new Order.OrderFile();

         file.filename = disambiguate(order,originalFilename); // so, filenames are unique
         file.status = localOrder ? Order.STATUS_ITEM_RECEIVED : Order.STATUS_ITEM_PENDING;
         file.path = null; // format always flat to begin with
         file.originalFilename = originalFilename;
         file.downloadURL = downloadURL;
         file.size = size;

         order.files.add(file);
      }

      // so, if the downloadURL was a duplicate, we've checked
      // the filename and size, and returned the existing file.

      return file;
   }

// --- disambiguation ---

   /**
    * Disambiguate a filename within the context of an order.
    * (If the filename is unambiguous, it won't be changed.)
    */
   public static String disambiguate(Order order, String filename) {

      String s = filename;
      int n = 2; // start with n=2 so that we get dog.jpg, dog_2.jpg, etc.

      while (order.findFileByFilenameIgnoreCase(s) != null) s = FileUtil.vary(filename,n++);
      // list is finite, this is guaranteed to terminate

      // I haven't had any reports of case causing trouble here,
      // I'm just adding the "ignore case" to parallel ItemUtil.
      // it could definitely happen here in theory.

      return s;
   }

   /**
    * A similar function that operates on HashSet instead of Order.
    */
   private static String disambiguate(HashSet set, String filename) {

      String s = filename;
      int n = 2;

      while (set.contains(s.toLowerCase())) s = FileUtil.vary(filename,n++);

      set.add(s.toLowerCase()); // small difference with previous function
      return s;
   }

   // a few notes about this second part ...
   //
   // the idea is, we want to rename book files so they're unambiguous
   // when dropped in the same directory together.  right now you might
   // have one book with files 1-20.jpg, then a second book with files
   // 1-24.jpg, and the first twenty would get renamed but the last four
   // would look like they're part of the first book.
   // so, plan is, add a "set1_" prefix to any item with multiple images.
   //
   // there are a few cases to watch out for, though.  maybe the same
   // file could be part of two different items, in which case it should
   // take the set number of whichever one comes first.  that's possible
   // in theory but not really in practice.  or, maybe the same file
   // could be part of a MC item and also get saved to CD.  that doesn't
   // happen right now, but it's a planned feature.  in that case,
   // the file should get the set name from the MC item and just use the
   // same name for the CD part.
   //
   // so, that's why this has to be a separate processing step, because
   // there's no guarantee whether we'll see the CD or MC item first.
   // if we have to go back and rename things, better to do it all at once
   // at the end, less chance of a mix-up.
   //
   // another reason it has to be a separate step is, we don't want to do
   // any of this for local orders, where the files already exist on disk
   // with specific names that we can't / shouldn't change.

   public void redisambiguate() { // not static, uses order and setNumberMap

      HashMap map = new HashMap(); // maps current filename to new filename
      HashSet set = new HashSet(); // tracks which new names have been used;
                                   // entries are lowercase

   // first pass, files

      Iterator i = order.files.iterator();
      while (i.hasNext()) {
         Order.OrderFile file = (Order.OrderFile) i.next();

         String s = file.originalFilename; // base on *original* name!
         Integer setNumber = (Integer) setNumberMap.get(file.filename);
         if (setNumber != null) {
            s = Text.get(this,"s4",new Object[] { Convert.fromInt(setNumber.intValue()) }) + pad(s);
         }
         s = disambiguate(set,s);

         map.put(file.filename,s); // no collision since file names were unique
         file.filename = s;
         // go ahead and apply the map to this OrderFile
      }

   // second pass, items

      i = order.items.iterator();
      while (i.hasNext()) {
         Order.Item item = (Order.Item) i.next();

         if (item.isMultiImage()) {
            apply(map,item.filenames); // no change to the multi-image marker
         } else {
            item.filename = (String) map.get(item.filename);
         }
      }
   }

   /**
    * Pad a (non-multi-image) filename with zeros to make it look pretty.
    */
   private static String pad(String filename) {

      final int PAD_LEN = 3;
      final String PAD_TEXT = "000"; // at least PAD_LEN long!

      int i = filename.lastIndexOf('.');
      if (i <= 0 || i >= PAD_LEN) return filename; // don't pad if empty or no suffix

      for (int j=0; j<i; j++) {
         if ( ! isDigit(filename.charAt(j)) ) return filename; // don't pad non-numeric
      }

      return PAD_TEXT.substring(i) + filename;
   }

   private static boolean isDigit(char c) { return (c >= '0' && c <= '9'); }

   private static void apply(HashMap map, LinkedList filenames) {
      ListIterator li = filenames.listIterator();
      while (li.hasNext()) {
         li.set(map.get(li.next())); // no need for String cast
      }
   }

}

