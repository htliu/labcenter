/*
 * FormatDLS.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.misc.Op;
import com.lifepics.neuron.thread.StoppableThread;
import com.lifepics.neuron.thread.ThreadStopException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import com.kodak.kias.clientlib.ClientLibException;
import com.kodak.kias.clientlib.KIASLocale;
import com.kodak.kias.clientlib.KIASSystem;
import com.kodak.kias.clientlib.Product;

/**
 * Implementation of the DLS format.
 */

public class FormatDLS extends FormatDLSStub {

   // *** WARNING!  This class requires an external jar file!
   // *** Callers must catch and handle NoClassDefFoundError!

// --- constants ---

   private static final int IMAGE_MAX    =  40; // max number of images per COS file
   private static final int CONSUMER_MAX =  64; // max number of characters in consumer ID

   private static final int QUANTITY_MAX = 500; // max print quantity
   private static final int CHUNK_STD = 500;
   private static final int CHUNK_MAX = 500; // *both* same as QUANTITY_MAX !

   private static int NDIGIT_DLS_ID  = 6;

// --- subclass hooks ---

   // these have been moved up to FormatDLSStub.
   //
   // now that this class is only called by the stub,
   // in theory we don't need to inherit from anyone.
   // in practice, we need to inherit from Format
   // because we call some of its utility functions;
   // so, we need to implement the subclass hooks;
   // so we might as well inherit from the stub instead
   // of reimplementing them ourselves.
   // the subclass hooks should never be called at this
   // level, so we could reimplement them to do nothing,
   // but it's just as easy to have them work correctly.

// --- filename variation ---

   private static Variation dlsVariation = new PrefixVariation(0);

// --- record class ---

   // non-copyable record to hold common fields.

   private static class Record {

      public Job job;
      public Order order;
      public DLSConfig config;
      public KIASSystem.Session session;
      public HashMap products;
      public Backprint.Sequence sequence;
   }

// --- format function ---

   public void format(Job job, Order order, Object formatConfig) throws Exception {

      DLSConfig config = (DLSConfig) formatConfig;
      // because of validation, the cast is guaranteed to work

      KIASSystem.Session session = null;
      try {

         String url = "http://" + config.host + ":" + Convert.fromInt(config.port) + "/" + config.path;
         KIASSystem system = new KIASSystem(url,"");

         session = system.new Session(config.userName,config.password);

         Product[] array = session.GetAvailableProducts(new KIASLocale());
         HashMap products = new HashMap();
         for (int i=0; i<array.length; i++) {
            products.put(array[i].getProductID(),array[i]);
         }
         // note, the "new KIASLocale()" produces a deprecation warning,
         // even though it clearly says you can do it in the online API.

         Record r = new Record();
         r.job = job;
         r.order = order;
         r.config = config;
         r.session = session;
         r.products = products;
         r.sequence = new Backprint.Sequence();

         format(r);

      } catch (ClientLibException e) {
         throw wrapMaybeStop(e);

      } finally {
         if (session != null) {
            try {
               session.Disconnect();
            } catch (Exception e) {
               // ignore
            }
         }
      }
   }

   private void format(Record r) throws Exception {

   // (1) validate things, build image list(s)

      HashSet fset = new HashSet();
      HashSet skus = new HashSet();

      LinkedList images = new LinkedList();
      LinkedList imagesList = new LinkedList();

      Iterator i = r.job.refs.iterator();
      while (i.hasNext()) {
         Job.Ref ref = (Job.Ref) i.next();

         if (fset.add(ref.filename)) { // new image
            getFileFormat(ref.filename); // make sure we know format
            images.add(ref.filename);
            if (images.size() == IMAGE_MAX) { // make sure not too many images per COS file
               imagesList.add(images);
               images = new LinkedList();
            }
         }

         if (skus.add(ref.sku)) { // new SKU
            DLSMapping m = (DLSMapping) MappingUtil.getMapping(r.config.mappings,ref.sku);
            if (m == null) missingChannel(ref.sku,r.order.getFullID());
            getProduct(r.products,m.product); // make sure product ID defined (on KIAS)
         }
      }

      if (images.size() != 0) imagesList.add(images);

   // (2) set up the operations

      LinkedList ops = new LinkedList();
      LinkedList sub = new LinkedList();

      // just a formality, make sure the order ID isn't negative
      Convert.validateUsable(NDIGIT_DLS_ID,r.order.orderID);
      String orderString = Convert.fromIntNDigit(NDIGIT_DLS_ID,r.order.orderID);
      String dlsString;
      int dlsID, varyNumber = 0;

      i = imagesList.iterator();
      while (i.hasNext()) {

         // loop is executed only once except in one special case
         do {
            dlsString = dlsVariation.vary(orderString,varyNumber++);
            dlsID = Convert.toIntNDigit(NDIGIT_DLS_ID,dlsString);
         } while (dlsID == 0);
         // the spec says the vendor order number must be between 1 and 999999.
         // so, it's not exactly an N-digit format, and zero is a special case.
         // note, duplicates are allowed, but the folks at LP don't want that.

         File dest = new File(r.order.orderDir,dlsString + ".COS");
         ops.add(new COSGenerate(dest,r,(LinkedList) i.next(),dlsID));
         sub.add(new COSSubmit(dest,r));
      }

      ops.addAll(sub); // submit after all generation complete

   // (3) alter files

      Op.transact(ops);

   // (4) alter object

      // DLS jobs don't have directories or files, just maybe a property value
      if (r.config.completeImmediately) r.job.property = ""; // else it stays null
   }

// --- COS submission ---

   private static class COSSubmit extends Op {

      private File dest;
      private Record r;

      public COSSubmit(File dest, Record r) {
         this.dest = dest;
         this.r = r;
      }

      public void undo() {
         // not sure this is possible
      }

      public void dodo() throws IOException {

         // run a retry loop to handle code C000301A ("Server busy. Try again later.")
         // maybe this should wrap other KIAS functions, but for now it's just here.

         int count = 0;
         while (true) {

         // switch exception into a result code

            ClientLibException e = null;
            try {
               r.session.SubmitCosOrder(Convert.fromFile(dest),null);
            } catch (ClientLibException e2) {
               e = e2;
            }

         // now decide what to do about it

            if (e == null) return; // success

            if (e.getErrorCode() != 0xC000301A) throw wrap(e); // failure

            if (count >= StaticDLS.busyRetries) throw wrap(wrap(e),"e15"); // retry failure
            count++;

            StoppableThread thread = (StoppableThread) Thread.currentThread();
            try {
               thread.sleepNice(StaticDLS.busyPollInterval);
            } catch (InterruptedException e2) {
               // won't happen
            }

            if (thread.isStopping()) throw wrap(wrap(e),"e16"); // thread stop
         }
      }
   }

// --- COS generation ---

   private static class COSGenerate extends Op { // not Op.Generate

      private File dest;
      private Record r;
      private LinkedList images;
      private int dlsID;

      public COSGenerate(File dest, Record r, LinkedList images, int dlsID) {
         this.dest = dest;
         this.r = r;
         this.images = images;
         this.dlsID = dlsID;
      }

      public void undo() {
         dest.delete(); // ignore result
      }
      public void undoPartial() { undo(); }
      public void finish     () { undo(); } // !

      public void dodo() throws IOException {
         COS.Ref ref = new COS.Ref();

         try {
            COS.init(); // ok to call init every time
         } catch (UnsatisfiedLinkError e) { // see note at the start of COS.java
            throw (IOException) new IOException(Text.get(FormatDLS.class,"e14"));
         }
         // usually you should catch NoClassDefFoundError after UnsatisfiedLinkError,
         // since that's what you get on a second try, but we already have a handler
         // for that in FormatDLSStub, and I don't want to change the code unnecessarily.

         check("c2",COS.Order_Constructor(Convert.fromFile(dest),COS.COS_CREATE,ref));
         int order = deref("d1",ref);

         // now we have an order object, so be sure to clean it up
         try {
            subdo(ref,order);
            order = 0; // subdo destroyed it
         } finally {
            if (order != 0) COS.Order_Destructor(order); // ignore result
         }
      }

      private void subdo(COS.Ref ref, int order) throws IOException {

         boolean hasSequence = Backprint.hasSequence(r.config.backprints);

      // per-order information ; blocks are for scope control

         {
            check("c3",COS.Order_GetFileInfo(order,ref));
            int fileInfo = deref("d2",ref);
            check("c4",COS.FileInfo_GetFileMod(fileInfo,(short) 0,ref));
            int fileMod = deref("d3",ref);
            check("c5",COS.FileMod_SetApplication(fileMod,"LifePics LabCenter"));
         }

         {
            check("c6",COS.Order_GetVendorInfo(order,ref));
            int vendorInfo = deref("d4",ref);
            check("c7",COS.VendorInfo_SetVendorOrderNumber(vendorInfo,Convert.fromInt(dlsID)));
         }

         {
            // spec says there's a maximum length; maybe the library would
            // truncate it for me, but be nice and do it myself beforehand.
            String id = r.order.getFullName();
            if (id.length() > CONSUMER_MAX) id = id.substring(0,CONSUMER_MAX);

            id = id.replace('@','*'); // should do nothing, this is a name
            // replace '@' with something else similar looking, just in case,
            // so that if the order gets archived we won't see it and upload.

            check("c8",COS.Order_GetConsumerInfo(order,ref));
            int consumerInfo = deref("d5",ref);
            check("c9",COS.ConsumerInfo_SetConsumerID(consumerInfo,id));
         }

      // per-image information

         HashMap imageMap = new HashMap();
         int count = 1;

         Iterator i = images.iterator();
         while (i.hasNext()) {
            String filename = (String) i.next();

            check("c10",COS.Order_AddNewImage(order,Convert.fromInt(count++),ref));
            int image = deref("d6",ref);

            File file;
            try {
               file = r.order.getPath(filename);
            } catch (Exception e) {
               throw (IOException) new IOException(Text.get(FormatDLS.class,"e8")).initCause(e);
            }
            FileInputStream fis = new FileInputStream(file);
            FileChannel fch = fis.getChannel();
            ByteBuffer buffer = fch.map(FileChannel.MapMode.READ_ONLY,0,file.length());
            //
            check("c11",COS.Image_WriteImage(image,buffer));

            check("c12",COS.Order_AddNewImageDetail(order,ref));
            int imageDetail = deref("d7",ref);
            check("c13",COS.ImageDetail_AddImageRef(imageDetail,image));
            // this must come first, or else other methods won't work

            check("c14",COS.ImageDetail_SetImageType(imageDetail,COS.kEmbeddedDigitalData));

            check("c15",COS.ImageDetail_GetDigitalInfo(imageDetail,ref));
            int digitalInfo = deref("d8",ref);
            check("c16",COS.DigitalInfo_SetFileFormat(digitalInfo,getFileFormat(filename)));
            check("c17",COS.DigitalInfo_SetFileColorSpace(digitalInfo,COS.ksRGB));

            imageMap.put(filename,new Integer(imageDetail));
         }

      // per-product information

         count = 1;

         i = r.job.refs.iterator();
         while (i.hasNext()) {
            Job.Ref jobRef = (Job.Ref) i.next();

            Object o = imageMap.get(jobRef.filename);
            if (o == null) continue;
            // this way is faster than searching the images list,
            // plus we have to do it anyway to get the detail ID.

            Order.Item item;
            try {
               item = r.order.getItem(jobRef);
            } catch (Exception e) {
               throw (IOException) new IOException(Text.get(FormatDLS.class,"e9")).initCause(e);
            }

            DLSMapping m = (DLSMapping) MappingUtil.getMapping(r.config.mappings,item.sku);

            Iterator k = new ChunkIterator(r.job.getOutputQuantity(item,jobRef),CHUNK_STD,CHUNK_MAX,hasSequence);
            while (k.hasNext()) {
               int quantity = ((Integer) k.next()).intValue();

               if (quantity > QUANTITY_MAX) throw new IOException(Text.get(FormatDLS.class,"e10",new Object[] { Convert.fromInt(QUANTITY_MAX) }));
               // should always pass now, but check anyway

               check("c18",COS.Order_AddNewProduct(order,"P" + Convert.fromInt(count++),ref));
               int product = deref("d9",ref);
               check("c19",COS.Product_AddImageDetailRef(product,((Integer) o).intValue()));
               // presumably this must come first too

               check("c20",COS.Product_SetProductClass(product,COS.kStandardPrint));
               check("c21",COS.Product_SetProductDescription(product,m.product));

               check("c22",COS.Product_GetFinishingInfo(product,ref));
               int finishingInfo = deref("d10",ref);
               check("c23",COS.FinishingInfo_SetQuantity(finishingInfo,quantity));

               Product p = getProduct(r.products,m.product);
               float dShort, dLong;
               if (p.getImageWidth() < p.getImageHeight()) {
                  dShort = p.getImageWidth();
                  dLong  = p.getImageHeight();
               } else {
                  dShort = p.getImageHeight();
                  dLong  = p.getImageWidth();
               }
               short unit;
               switch (p.getLengthUnit()) {
               case Product.INCH: unit = COS.kInch;        break;
               case Product.MM:   unit = COS.kMillimeter;  break;
               default: throw new IOException(Text.get(FormatDLS.class,"e7",new Object[] { Convert.fromInt(p.getLengthUnit()) }));
               }

               check("c24",COS.Product_GetPrintInfo(product,ref));
               int printInfo = deref("d11",ref);
               check("c25",COS.PrintInfo_SetShortDimension(printInfo,dShort));
               check("c26",COS.PrintInfo_SetLongDimension (printInfo,dLong));
               check("c27",COS.PrintInfo_SetSizeUnit      (printInfo,unit));

               int surface = m.surface;
               if (surface < 0) surface = -surface;
               check("c28",COS.PrintInfo_SetMediaSurface(printInfo,(short) surface));

               String s = Backprint.generate(r.config.backprints,"\n",r.order,item,r.sequence);
               if (s != null) check("c29",COS.PrintInfo_SetBackprintMessage(printInfo,s));

               r.sequence.advance(item);
            }
         }

      // finish up

         check("c30",COS.Order_WriteOrder(order));
         check("c31",COS.Order_Destructor(order));
      }
   }

// --- COS utilities ---

   private static class COSException extends IOException {
      public COSException(String message) { super(message); }
   }

   private static void check(String key, int status) throws COSException {
      if (status == COS.COS_OK) { // ok
         // do nothing
      } else if (status < 0) { // error
         throw new COSException(Text.get(FormatDLS.class,"e1",new Object[] { Text.get(FormatDLS.class,key), Convert.fromInt(status) }));
      } else { // warning
         // make warnings into errors, relax as needed
         // Log.log(Level.WARNING,FormatDLS.class,"e2",new Object[] { Text.get(FormatDLS.class,key), Convert.fromInt(status) });
         //
         throw new COSException(Text.get(FormatDLS.class,"e2",new Object[] { Text.get(FormatDLS.class,key), Convert.fromInt(status) }));
      }
   }

   private static int deref(String key, COS.Ref ref) throws COSException {
      if (ref.value != 0) { // normal pointer
         return ref.value;
      } else { // null returned, apparently a possible condition
         throw new COSException(Text.get(FormatDLS.class,"e3",new Object[] { Text.get(FormatDLS.class,key) }));
      }
   }

   private static HashMap fileFormatMap = new HashMap();
   static {
      fileFormatMap.put("bmp", new Short(COS.kBMPWin));
      fileFormatMap.put("jpg", new Short(COS.kJPEG));
      fileFormatMap.put("jpeg",new Short(COS.kJPEG));
      fileFormatMap.put("tif", new Short(COS.kTIFF));
      fileFormatMap.put("tiff",new Short(COS.kTIFF));
   }
   // this is not ideal -- see paper notes from 10/5/05 for a nice diagram.
   // but, it's also academic, because everything gets downloaded as JPEGs.

   private static short getFileFormat(String filename) throws IOException {
      int i = filename.lastIndexOf('.');
      if (i == -1) throw new IOException(Text.get(FormatDLS.class,"e4",new Object[] { filename }));

      String suffix = filename.substring(i+1).toLowerCase();
      Object o = fileFormatMap.get(suffix);
      if (o == null) throw new IOException(Text.get(FormatDLS.class,"e5",new Object[] { suffix }));

      return ((Short) o).shortValue();
   }

   // the point of this one is to put the exception in one place
   private static Product getProduct(HashMap products, String productID) throws IOException {
      Object o = products.get(productID);
      if (o == null) throw new IOException(Text.get(FormatDLS.class,"e6",new Object[] { productID }));
      return (Product) o;
   }

   private static String describe(ClientLibException e) {
      return Text.get(FormatDLS.class,"e13",new Object[] { Integer.toHexString(e.getErrorCode()).toUpperCase() });
   }

   private static IOException wrap(ClientLibException e) {
      return (IOException) new IOException(describe(e)).initCause(e);
   }

   // can't use this inside COSSubmit because the result isn't always IOException,
   // but anyway I'm not sure I'd want to use it there because in that case we've
   // already successfully talked to the DLS to get the products.
   //
   private static Exception wrapMaybeStop(ClientLibException e) {
      if (e.getErrorCode() == 0xC000100B) { // can't connect
         return new ThreadStopException(describe(e),e).setHint(Text.get(FormatDLS.class,"h13"));
      } else {
         return (Exception) new IOException(describe(e)).initCause(e);
      }
   }

   private static IOException wrap(Exception e, String key) {
      return (IOException) new IOException(Text.get(FormatDLS.class,key)).initCause(e);
   }

// --- completion ---

   // this has been moved up to FormatDLSStub too.

}

