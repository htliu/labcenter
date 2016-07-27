/*
 * Order.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.object.XML;
import com.lifepics.neuron.struct.*;

import java.io.File;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Node;

/**
 * An object that holds all the information about an order.
 */

public class Order extends OrderStub {

// --- fields ---

   // for local orders, most fields are optional, so now pretty much everything
   // can be null, and there are no correlations on what's null and not null.
   // so, the code should always check for nulls on each field independently.
   // one exception is the (ancient) deploy fields, which are still correlated.
   //
   // nameFirst, nameLast, email, and phone are not nullable, but can be empty.
   // from a database perspective it would make sense to use null for the case
   // where the field value isn't known, but in practice it would just be
   // a nuisance, because nearly everywhere we use them, we'd have to add code
   // saying, if null, use empty string instead.

   public String nameFirst;
   public String nameLast;
   public String email;
   public String lpCustomerID; // null for old orders; integer-valued but we don't care
   public String phone;
   public String specialInstructions; // nullable
   public SKU specialSKU; // null unless there's a CD
   public String dealerName; // only used for backprints, so the parser doesn't require it

   // null for old orders, also for medium-old ones grandTotal will be filled in
   // and the rest null.  the order here is logical display order on the invoice.
   //
   public String subtotal;
   public String discountFreePrints;
   public String discountCredit;
   public String discountPercent;   // amount of percent discount, not a percentage
   public String discountPermanent; // amount of permanent percent discount?
   //
   // there's now one more discount field, FreeShipping, and also a ShippingCost that
   // shows the shipping amount before the discount is applied.  we don't need either
   // of them, so leave them out for now.  they're used in invoice.xsl, though.
   //
   public String shippingAmount;
   public String taxAmount;
   public String grandTotal; // null for old orders

   // this is the billing address, no longer used for anything except DP2 integration
   public String company; // null for old orders
   public String street1;
   public String street2;
   // comment below about AltCSZ applies here too
   public String city;
   public String state;
   public String zipCode;
   public String country;

   // FormatFujiNew and FormatLucidiom use the shipping address,
   // but don't have any place for the company name, so, unused.
   //
   public String shipCompany; // null for old orders
   public String shipStreet1;
   public String shipStreet2;
   // there's an AltCSZ field in the XML now, but all we'd do with it here is send it
   // to the integrations, and all of them expect CSZ to be broken into three fields.
   public String shipCity;
   public String shipState;
   public String shipZipCode;
   public String shipCountry;
   public LinkedList shipMethods; // usually one, but can be zero or more than one

   public Date invoiceDate; // null for old orders
   public int dueStatus;

   // these don't come from the server, they're entered on the client and sent back
   public String carrier;
   public String trackingNumber;

   public int format;

   public LinkedList items;
   public LinkedList files;
   public LinkedList formatFiles; // list of string paths

   public File deployDir;
   public String deployPrefix;
   public LinkedList deployFiles; // list of string paths

   // How the deploy fields work:  if they're filled in, it means that (all) the subdirectories
   // of the order directory have been deployed somewhere else.  The files.path and formatFiles
   // information is not modified to account for this, since the files are no longer relative to
   // the orderDir.  Instead, the path is constructed as in getPath; other than that, the actual
   // locations of the files are used only in the Format class, which handles them correctly.
   // But, note that deployFiles must be a list of all subdirectories, it cannot be partial list.

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      Order.class,
      OrderStub.sd,
      new AbstractField[] {

         new StringField("nameFirst","FirstName"),
         new StringField("nameLast","LastName"),
         new StringField("email","Email"),
         new NullableStringField("lpCustomerID","LPCustomerID"),
         new StringField("phone","Phone"),
         new NullableStringField("specialInstructions","SpecialInstructions"),
         new NullableSKUField("specialSKU","SpecialSku","SpecialSKU"),
         new NullableStringField("dealerName","DealerName"),

         new NullableStringField("subtotal","Subtotal"),
         new NullableStringField("discountFreePrints","DiscountFreePrints"),
         new NullableStringField("discountCredit","DiscountCredit"),
         new NullableStringField("discountPercent","DiscountPercent"),
         new NullableStringField("discountPermanent","DiscountPermanent"),
         new NullableStringField("shippingAmount","ShippingAmount"),
         new NullableStringField("taxAmount","TaxAmount"),
         new NullableStringField("grandTotal","GrandTotal"),

         new NullableStringField("company","Company"),
         new NullableStringField("street1","Street1"),
         new NullableStringField("street2","Street2"),
         new NullableStringField("city","City"),
         new NullableStringField("state","State"),
         new NullableStringField("zipCode","ZipCode"),
         new NullableStringField("country","Country"),

         new NullableStringField("shipCompany","ShipCompany"),
         new NullableStringField("shipStreet1","ShipStreet1"),
         new NullableStringField("shipStreet2","ShipStreet2"),
         new NullableStringField("shipCity","ShipCity"),
         new NullableStringField("shipState","ShipState"),
         new NullableStringField("shipZipCode","ShipZipCode"),
         new NullableStringField("shipCountry","ShipCountry"),

         new StructureListField("shipMethods","ShipMethod",ShipMethod.sd,Merge.NO_MERGE,3,0) {
            protected void loadSpecial(Node node, Object o, int version) throws ValidationException {
               // this used to be an inline list of names
               // cf. StructureListField and InlineListField.loadNormal
               LinkedList list = tget(o);
               Iterator i = XML.getInlineList(node,"ShipMethod").iterator(); // happens to use same xmlName
               while (i.hasNext()) {
                  list.add(new ShipMethod((String) i.next()));
               }
            }
         },
         new NullableDateField("invoiceDate","InvoiceDate") {
            protected void loadNormal(Node node, Object o) throws ValidationException {
               String temp = XML.getNullableText(node,xmlName);
               Date d = (temp == null) ? null : Convert.toDateAbsoluteInternal(temp);
               tset(o,d);
            }
            public void store(Node node, Object o) {
               Date d = tget(o);
               String temp = (d == null) ? null : Convert.fromDateAbsoluteInternal(d);
               XML.createNullableText(node,xmlName,temp);
            }
            // this could be another generic field type,
            // but I'm tired of setting them up for now.
         },
         new EnumeratedField("dueStatus","DueStatus",OrderEnum.dueStatusType) {
            protected void loadNormal(Node node, Object o) throws ValidationException {
               String temp = XML.getNullableText(node,xmlName);
               int i = (temp == null) ? DUE_STATUS_NONE : type.toIntForm(temp);
               tset(o,i);
            }
            // I should have just bumped the version and set a default,
            // but it's too late now.  instead, I accept null on input
            // but always write a non-null value on output.
         },

         new NullableStringField("carrier","Carrier"),
         new NullableStringField("trackingNumber","TrackingNumber"),

         new EnumeratedField("format","Format",OrderEnum.formatType),

         new StructureListField("items","Item",Item.sd,Merge.NO_MERGE),
         new StructureListField("files","File",OrderFile.sd,Merge.NO_MERGE,2,0) {
            protected void loadSpecial(Node node, Object o, int version) throws ValidationException {
               loadNormal(node,o);
               // load v1 just like v2, but then do a postprocessing step
               Order order = (Order) o;
               moveStatus(order.items,order.files); // field order matters, items must be loaded
            }
         },
         new InlineListField("formatFiles","FormatFile"),

         new NullableFileField("deployDir","DeployDir"),
         new NullableStringField("deployPrefix","DeployPrefix"),
         new InlineListField("deployFiles","DeployFile")
      });

   protected StructureDefinition sd() { return sd; }

// --- special filenames ---

   public static final String ORDER_FILE = "order.xml";
   public static final String INVOICE_FILE = "invoice.html";
   public static final String LABEL_FILE = "label.html"; // happens to equal prefix plus suffix
   public static final String LABEL_PREFIX = "label";
   public static final String LABEL_SUFFIX = ".html";
   public static final String PRODUCT_LABEL_PREFIX = "product";
   public static final String PRODUCT_LABEL_SUFFIX = ".html";
   public static final String JPEG_TEMP = "invoice.tmp";
   public static final String JPEG_FILE = "invoice.jpg"; // this gets varied
   public static final String LOCAL_FILE = "local.xml";

   public static String getLabelFile(int n) { return LABEL_PREFIX + Convert.fromInt(n) + LABEL_SUFFIX; }
   public static String getProductLabelFile(int n) { return PRODUCT_LABEL_PREFIX + Convert.fromInt(n) + PRODUCT_LABEL_SUFFIX; }

// --- special URL values ---

   public static final String URL_INVOICE_NEW = "invoice";
   public static final String URL_INVOICE_OLD = "";

// --- order sequence enumeration ---

   // it's represented as a string, but in concept it's an enumeration;
   // see also the comments in LocalConfig

   public static final String ORDER_SEQUENCE_LOCAL = "L";

   // I've been pretty sloppy in the naming and commenting elsewhere.
   // when I talk about local orders, sometimes that means orders with
   // orderSeq "L", but sometimes it just means orders with orderSeq
   // not null.  here's a quick current inventory of the various cases.
   //
   // * local means "L": local status reporting (2), local.xml cleanup
   //
   // * local means not null: initial status reporting check, validation
   // for not wholesale, blocks on download and status transactions, and
   // definition of local in getPurgeMode.  and disableForLocal.

// --- order status enumeration ---

   // the status and hold min/max values are public to share with OrderStub

   public static final int STATUS_ORDER_MIN = 0;

   public static final int STATUS_ORDER_PENDING = 0;
   public static final int STATUS_ORDER_RECEIVING = 1;
   public static final int STATUS_ORDER_RECEIVED = 2;
   public static final int STATUS_ORDER_FORMATTING = 3; // these two are relics
   public static final int STATUS_ORDER_FORMATTED = 4;  //
   public static final int STATUS_ORDER_INVOICING = 5;
   public static final int STATUS_ORDER_PRESPAWN = 6; // shows as INVOICED
   public static final int STATUS_ORDER_SPAWNING = 7; //          AUTOPRINTING
   public static final int STATUS_ORDER_INVOICED = 8; //          READY
   public static final int STATUS_ORDER_PRINTING = 9; //          IN PROGRESS
   public static final int STATUS_ORDER_PRINTED = 10;

   public static final int STATUS_ORDER_COMPLETED = 11;
   public static final int STATUS_ORDER_ABORTED = 12;
   public static final int STATUS_ORDER_CANCELED = 13; // actually refunded to user

   public static final int STATUS_ORDER_MAX = 13;

// --- order hold enumeration ---

   public static final int HOLD_MIN = 0;

   public static final int HOLD_NONE = 0;
   public static final int HOLD_USER = 1;
   public static final int HOLD_ERROR = 2;
   public static final int HOLD_INVOICE = 3;
   public static final int HOLD_RETRY = 4;

   public static final int HOLD_MAX = 4;

// --- due-status enumeration ---

   // numerical order of these used in DueThread

   private static final int DUE_STATUS_MIN = 0;

   public static final int DUE_STATUS_NONE = 0;
   public static final int DUE_STATUS_SOON = 1;
   public static final int DUE_STATUS_LATE = 2;

   private static final int DUE_STATUS_MAX = 2;

// --- order format enumeration ---

   public static final int FORMAT_MIN = 0;

   public static final int FORMAT_FLAT = 0;
   public static final int FORMAT_TREE = 1;
   public static final int FORMAT_NORITSU = 2;
   public static final int FORMAT_KONICA = 3;
   public static final int FORMAT_FUJI = 4;
   public static final int FORMAT_FUJI_NEW = 5;
   public static final int FORMAT_DLS = 6;
   public static final int FORMAT_KODAK = 7;
   public static final int FORMAT_AGFA = 8;
   public static final int FORMAT_LUCIDIOM = 9;
   public static final int FORMAT_PIXEL = 10;
   public static final int FORMAT_DP2 = 11;
   public static final int FORMAT_BEAUFORT = 12;
   public static final int FORMAT_DKS3 = 13;
   public static final int FORMAT_DIRECT_PDF = 14;
   public static final int FORMAT_ZBE = 15;
   public static final int FORMAT_FUJI3 = 16;
   public static final int FORMAT_HP = 17;
   public static final int FORMAT_XEROX = 18;
   public static final int FORMAT_DIRECT_JPEG = 19;
   public static final int FORMAT_BURN = 20;
   public static final int FORMAT_HOT_FOLDER = 21;
   public static final int FORMAT_DNP = 22;
   public static final int FORMAT_PURUS = 23;
   public static final int FORMAT_RAW_JPEG = 24;

   public static final int FORMAT_MAX = 24;

// --- item status enumeration ---

   private static final int STATUS_ITEM_INVALID = -1;
   private static final int STATUS_ITEM_MIN = 0;

   public static final int STATUS_ITEM_PENDING = 0;
   public static final int STATUS_ITEM_RECEIVING = 1;
   public static final int STATUS_ITEM_RECEIVED = 2;
   public static final int STATUS_ITEM_PRINTING = 3;
   public static final int STATUS_ITEM_PRINTED = 4;

   private static final int STATUS_ITEM_MAX = 4;

// --- wholesale code enumeration ---

   // the idea here is, the mlrfnbr can come from two places,
   // MerchantConfig or Wholesale.  originally the two were
   // mutually exclusive, but now we're allowing both of them.
   //
   // as a convenience for wholesaler DIN running the normal
   // location Suchfun, we're adding a mode where all orders
   // look like wholesale orders.  in that case the two mlrfnbr
   // will be the same.  that's WSC_PSEUDO.
   //
   // the other new case, WSC_PRO, is for pro orders that are
   // router to the fulfillment dealer.  in that case the two
   // mlrfnbr will be different.

   public static final int WSC_NORMAL    = 0;
   public static final int WSC_PSEUDO    = 1;
   public static final int WSC_PRO       = 2;
   public static final int WSC_WHOLESALE = 3;

// --- find functions ---

   // these two functions could be abstracted and combined,
   // using a selector, but then they'd be harder to call.
   // note that files are unique by both filename and downloadURL.

   public OrderFile findFileByFilenameIgnoreCase(String filename) {
      Iterator i = files.iterator();
      while (i.hasNext()) {
         OrderFile file = (OrderFile) i.next();
         if (file.filename.equalsIgnoreCase(filename)) return file;
      }
      return null;
   }

   public OrderFile findFileByFilename(String filename) {
      Iterator i = files.iterator();
      while (i.hasNext()) {
         OrderFile file = (OrderFile) i.next();
         if (file.filename.equals(filename)) return file;
      }
      return null;
   }

   public OrderFile findFileByDownloadURL(String downloadURL) {
      Iterator i = files.iterator();
      while (i.hasNext()) {
         OrderFile file = (OrderFile) i.next();
         if (file.downloadURL.equals(downloadURL)) return file;
      }
      return null;
   }

   public Item findItemByFilenameAndSKU(String filename, SKU sku) {

      // these two fields are a primary key for items,
      // because duplicates are merged in OrderParser.

      Iterator i = items.iterator();
      while (i.hasNext()) {
         Item item = (Item) i.next();
         if (item.filename.equals(filename) && item.sku.equals(sku)) return item;
      }
      return null;
   }

// --- get functions ---

   // same as find functions, but throw exception on failure.

   public Order.Item getItem(Job.Ref ref) throws Exception {
      Item item = findItemByFilenameAndSKU(ref.filename,ref.sku);
      if (item == null) throw new Exception(Text.get(Order.class,"e10",new Object[] { getFullID(), ref.filename, ref.sku.toString() }));
      return item;
   }

   public File getPath(String filename) throws Exception {
      OrderFile file = findFileByFilename(filename);
      if (file == null) throw new Exception(Text.get(Order.class,"e11",new Object[] { getFullID(), filename }));
      return getPath(file);
   }

   public File[] getPaths(Item item) throws Exception {
      List filenames = item.isMultiImage() ? item.filenames : Collections.singletonList(item.filename);

      File[] f = new File[filenames.size()];
      int i = 0;

      Iterator j = filenames.iterator();
      while (j.hasNext()) {
         String filename = (String) j.next();
         f[i++] = getPath(filename);
      }

      return f;
   }

// --- helper functions ---

   public File getPath(OrderFile file) {
      File dir;
      if (deployDir != null) {
         dir = new File(deployDir,deployPrefix + file.path);
         // path must not be null ; note the concatenation
      } else {
         dir = orderDir;
         if (file.path != null) dir = new File(dir,file.path);
      }
      return new File(dir,file.filename);
   }

   public String getLastName() {
      return nameLast;
   }

   public String getFullName() {
      if (nameFirst.length() == 0) return nameLast;
      if (nameLast .length() == 0) return nameFirst;
      return nameFirst + " " + nameLast;
   }

   public static boolean isShipToHome(String shipMethod) {
      return ! (shipMethod == null || shipMethod.toLowerCase().startsWith("pick"));
   }
   // the entries in shipMethods can't be null, but the entries
   // in Invoice.LabelInfo objects can be, so null case is good
   // to have covered.

   /**
    * Legacy helper function -- get the first shipMethod
    * that is for shipping rather than pickup, else null.
    */
   public String getFirstShipMethod() {

      Iterator i = shipMethods.iterator();
      while (i.hasNext()) {
         String shipMethod = ((ShipMethod) i.next()).name;
         if (isShipToHome(shipMethod)) return shipMethod;
      }

      return null;
   }

   /**
    * A good function, also used as a legacy function for cases when
    * there's no easy right way to deal with multiple shipping types.
    */
   public boolean isAnyShipToHome() {

      // null has become empty list now, but this still holds.
      //
      // null used to count as ship to home, but it makes more
      // sense that if there's no method at all, it's in store.
      // and, it can only be null on local or ancient orders.
      //
      // so, the rule I adopt is, say it's shipped if there are
      // any shipped parts, otherwise it's pickup.

      return (getFirstShipMethod() != null);
   }

// --- validation ---

   public static void validateFormat(int format) throws ValidationException {

      if (format < FORMAT_MIN || format > FORMAT_MAX) {
         throw new ValidationException(Text.get(Order.class,"e1",new Object[] { Convert.fromInt(format) }));
      }
   }

   public static void validateQuantity(int quantity) throws ValidationException {

      if (quantity <= 0) {
         throw new ValidationException(Text.get(Order.class,"e6",new Object[] { Convert.fromInt(quantity) }));
      }
   }

   public void validate() throws ValidationException {
      super.validate();

      if (dueStatus < DUE_STATUS_MIN || dueStatus > DUE_STATUS_MAX) {
         throw new ValidationException(Text.get(this,"e12",new Object[] { Convert.fromInt(dueStatus) }));
      }

      if ((carrier != null) != (trackingNumber != null)) throw new ValidationException(Text.get(this,"e14"));

      validateFormat(format);

      Iterator i = items.iterator();
      while (i.hasNext()) {
         ((Item) i.next()).validate();
      }

      i = files.iterator();
      while (i.hasNext()) {
         ((OrderFile) i.next()).validate();
      }
   }

// --- item class ---

   public static class Item extends Structure {

      public String filename; // use as index into file list (*)
      public int status;
      public SKU sku;
      public int quantity;
      public String comments;      // for local orders only
      public LinkedList filenames; // for multi-image items only
      public String price;         // unit price, not total;
                                   // only used by DNP integration, has to be enabled at top level
      public Integer pageCount;    // collected for use by FormatPurus; doesn't count cover/covers

      // (*) now that we support items with multiple images, the filename
      // is no longer guaranteed to be an actual file name, it might also
      // be a multi-image item description, in which case the actual file
      // names can be found in the filename list.  the right way to detect
      // such items is to call isMultiImage, below.  most lab integrations
      // are not aware of this change and are blocked from receiving jobs
      // that contain multi-image items.

      public static final StructureDefinition sd = new StructureDefinition(

         Item.class,
         // no version
         new AbstractField[] {

            new StringField("filename","Filename"),
            new EnumeratedField("status","Status",OrderEnum.itemStatusType),
            new SKUField("sku","Sku","SKU"),
            new IntegerField("quantity","Quantity"),
            new NullableStringField("comments","Comments"),
            new InlineListField("filenames","File"),
            new NullableStringField("price","Price"),
            new NullableIntegerField("pageCount","PageCount")
         });

      protected StructureDefinition sd() { return sd; }

      public void validate() throws ValidationException {

         if (status < STATUS_ITEM_MIN || status > STATUS_ITEM_MAX) {
            throw new ValidationException(Text.get(Order.class,"e5",new Object[] { Convert.fromInt(status) }));
         }

         validateQuantity(quantity);

         if (isMultiImage() && filenames.size() == 0) throw new ValidationException(Text.get(Order.class,"e9"));
         //
         // size 0 would cause real problems, mainly that downloads would complete without marking
         // all items received.  (at present, the other one is that OrderDialog thumbnail getFile would fail,
         // but we could fix that.)  size 1 is harmless, though, so permit it, even though it doesn't really
         // represent a multi-image product.
         //
         // not sure why I left out the converse, that filenames is empty if not multi-image.  too late now!
         // but, it's harmless, filenames will just be ignored in that case, I never look at them unless we
         // have a multi-image item.
      }

      public boolean isMultiImage() {
         return isMultiImage(filename);
      }
      public static boolean isMultiImage(String filename) {

         // the exact form of multi-image item descriptions is subject to change,
         // and is only known by the order parser.  but, that's fine ...
         // all we need to know here is that they start and end with square brackets.
         // regular file names end with an extension, so collision is very unlikely.

         // why not just use filenames.isEmpty()?
         // 1. it's not the same thing, see comments in Item.validate
         // 2. this way we can check the multi-image-ness of job refs

         return (filename.startsWith("[") && filename.endsWith("]"));
      }
   }

// --- file class ---

   public static class OrderFile extends Structure {

      public String filename;
      public int status;
      public String path; // nullable
      public String originalFilename; // not nullable, but not stored in XML if equal to filename
      public String downloadURL;
      public Long size; // now a legacy field, set only for orders generated by IFP

      public static final StructureDefinition sd = new StructureDefinition(

         OrderFile.class,
         // no version
         new AbstractField[] {

            new StringField("filename","Filename"),
            new EnumeratedField("status","Status",OrderEnum.itemStatusType) {
               protected void loadNormal(Node node, Object o) throws ValidationException {
                  String temp = getNullableText(node,xmlName);
                  int i = (temp == null) ? STATUS_ITEM_INVALID : type.toIntForm(temp);
                  tset(o,i);
               }
               // just like NullableEnumeratedField, except null becomes invalid.
               // the invalid values will be replaced at the order level
               // if we're converting a v1 order, otherwise they're really wrong.
            },
            new NullableStringField("path","Path"),
            new StringField("originalFilename","OriginalFilename") {
               protected void loadNormal(Node node, Object o) throws ValidationException {
                  String temp = XML.getNullableText(node,xmlName);
                  String s = (temp == null) ? ((OrderFile) o).filename : temp;
                  tset(o,s);
               }
               public void store(Node node, Object o) {
                  String s = tget(o);
                  String temp = s.equals(((OrderFile) o).filename) ? null : s;
                  XML.createNullableText(node,xmlName,temp);
               }
            },
            new StringField("downloadURL","DownloadURL"),
            new NullableLongField("size","Size")
         });

      protected StructureDefinition sd() { return sd; }

      public void validate() throws ValidationException {

         if (status < STATUS_ITEM_MIN || status > STATUS_ITEM_RECEIVED) { // not MAX !
            throw new ValidationException(Text.get(Order.class,"e8",new Object[] { Convert.fromInt(status) }));
         }

         if (size != null && size.longValue() <= 0) {
            throw new ValidationException(Text.get(Order.class,"e7",new Object[] { Convert.describeNullableLong(size) })); // known not null, but describe seems best
         }
      }
   }

   private static void moveStatus(LinkedList items, LinkedList files) throws ValidationException {
      Iterator i = files.iterator();
      while (i.hasNext()) {
         OrderFile file = (OrderFile) i.next();

         int  status = getFirstItem(items,file.filename).status;
         file.status = (status < STATUS_ITEM_RECEIVED) ? status : STATUS_ITEM_RECEIVED;
         //
         // we might be able to just mark everything received based on
         // the order status, but it's not worth the added complexity.
         //
         // because we download items from first to last, the first item
         // with a given filename is guaranteed to hold the file status.
         //
         // ignore the existing status, it must be STATUS_ITEM_INVALID
         // because all unhacked v1 orders have null for the file status.
      }
   }

   private static Item getFirstItem(LinkedList items, String filename) throws ValidationException {
      Iterator i = items.iterator();
      while (i.hasNext()) {
         Item item = (Item) i.next();
         if (item.filename.equals(filename)) return item;
      }
      throw new ValidationException(Text.get(Order.class,"e13",new Object[] { filename }));
   }

// --- ship method class ---

   public static class ShipMethod extends Structure {

      public String name;
      public LinkedList products;

      public static final StructureDefinition sd = new StructureDefinition(

         ShipMethod.class,
         // no version
         new AbstractField[] {

            new StringField("name","Name"),
            new InlineListField("products","Product")
         });

      protected StructureDefinition sd() { return sd; }

      public ShipMethod() {}
      public ShipMethod(String name) { this.name = name; }

      public void validate() throws ValidationException {
      }
   }

}

