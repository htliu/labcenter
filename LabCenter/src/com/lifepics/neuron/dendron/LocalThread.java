/*
 * LocalThread.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.ChainedException;
import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Log;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.meta.Accessor;
import com.lifepics.neuron.meta.FieldComparator;
import com.lifepics.neuron.misc.FileUtil;
import com.lifepics.neuron.misc.Resource;
import com.lifepics.neuron.object.XML;
import com.lifepics.neuron.struct.SKU;
import com.lifepics.neuron.table.Table;
import com.lifepics.neuron.thread.NotConfiguredException;
import com.lifepics.neuron.thread.StoppableThread;
import com.lifepics.neuron.thread.ThreadStatus;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TimeZone;
import java.util.logging.Level;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * A thread that polls for local orders.
 */

public class LocalThread extends StoppableThread {

// --- fields ---

   private LocalConfig config;
   private Table table;
   private File stylesheet;
   private ThreadStatus threadStatus;

   private HashMap bookMap;
   private boolean enableItemPrice;

// --- construction ---

   public LocalThread(LocalConfig config, Table table, File stylesheet, LinkedList coverInfos, boolean enableItemPrice, ThreadStatus threadStatus) {
      super(Text.get(LocalThread.class,"s1"));

      this.config = config;
      this.table = table;
      this.stylesheet = stylesheet;
      this.threadStatus = threadStatus;

      bookMap = CoverInfo.buildBookMap(coverInfos);
      this.enableItemPrice = enableItemPrice;
   }

// --- interface for thread fields ---

   protected void doInit() throws Exception {
   }

   protected void doRun() throws Exception {
      try {
         while ( ! isStopping() ) {
            poll();
            sleepNice(config.pollInterval);
         }
      } catch (Exception e) {
         if ( ! isStopping() ) threadStatus.fatal(e); // StoppableThread will log
         throw e;
      }
   }

   protected void doExit() {
   }

   protected void doStop() {
   }

// --- constants ---

   private static final int LENGTH_STATUS = 1; // must all be the same length
   private static final int LENGTH_ID_MAX = 9;

   private static final String STATUS_PREPARING       = "p"; // LC does not use
   private static final String STATUS_ORDER_AVAILABLE = "o";
   private static final String STATUS_EXAMINING       = "x";
   private static final String STATUS_ACCEPTED        = "a";
   private static final String STATUS_REJECTED        = "r";

// --- main function (cf. PollThread.poll) ---

   private void poll() throws Exception {

      if (config.directory == null) throw new NotConfiguredException(Text.get(this,"e19")).setHint(Text.get(this,"h19"));
      // basically, any time there's a validation tied to a process enable flag,
      // there also has to be a second validation in the thread that throws NCE,
      // because the user could start the process by hand and bypass the validation.

      if ( ! config.directory.exists() ) throw new Exception(Text.get(this,"e2",new Object[] { Convert.fromFile(config.directory) }));

      File[] file = config.directory.listFiles(new FileFilter() { public boolean accept(File file) {
         return (file.isDirectory() && file.getName().startsWith(STATUS_ORDER_AVAILABLE));
      } });
      if (file == null) throw new Exception(Text.get(this,"e3",new Object[] { Convert.fromFile(config.directory) }));

      for (int j=0; j<file.length && ! isStopping(); j++) {
         process1(file[j]);
      }
   }

// --- layer 1 - error reporting (cf. PollThread.processTryCatch) ---

   private void process1(File file) {
      try {

         Log.log(Level.INFO,this,"i1",new Object[] { Convert.fromFile(file) });

         process2(file);

         Log.log(Level.INFO,this,"i2",new Object[] { Convert.fromFile(file) });

      } catch (Exception e) {
         logError(e,"e4",file);
      }
   }

   private void logError(Exception e, String key, File file) {

      e = new Exception(Text.get(this,key,new Object[] { Convert.fromFile(file) }),e);
         // want file name to show up in the UI as well as in the log

      Log.log(Level.SEVERE,this,"e5",e);
      threadStatus.error(e);
   }

// --- layer 2 - other try-catch blocks ---

   // the error wrapper above is just to get a clear record in the log
   // of what happened with each folder.  here we get into more detail
   // about what the different code regions are, and what significance
   // it has if we fail in each one.

   private void process2(File file) throws Exception {

   // preprocessing, what we need to be able to reject

      String core;
      File temp;
      int orderID;

      try {

         core = file.getName().substring(LENGTH_STATUS);

         temp = rename(file,core,STATUS_EXAMINING,"e6");
         // do nothing until this works, that way we're guaranteed not to process twice
         //
         // note, this is not a fatal error ... it could reasonably be one,
         // but this way we have a built-in check and retry for the case
         // when one of the files in the folder is still in use by someone.

         orderID = Convert.toInt(core);
         // not Convert.toIntNDigit, because we accept less than full length

         Convert.validateNDigit(LENGTH_ID_MAX,orderID);
         // the validation is mainly to check that the number isn't negative,
         // because that wouldn't work with the status reporting system.
         // the validation on number of digits is just a bonus from the spec.

      } catch (Exception e) {
         throw new Exception(Text.get(this,"e10"),e);
      }

   // main processing, where failure causes rejection

      Order order;
      Document docOrder;

      try {

         Document docLocal = readLocalXML(temp);
         docOrder = transformToOrderXML(docLocal);
         order = createOrderObject(orderID);
         postprocess1(docOrder,order.getFullID(),temp);
         HashMap summaryMap = new HashMap();
         parseOrderXML(order,docOrder,summaryMap);
         postprocess2(docOrder,summaryMap);

      } catch (Exception e) {

         try {

            File fail = rename(temp,core,STATUS_REJECTED,"e7");
            LocalStatus.report(orderID,LocalStatus.CODE_REJECTED_GENERIC,
                               System.currentTimeMillis(),Level.SEVERE,ChainedException.format(Text.get(this,"e17"),e),e);

         } catch (Exception e2) {
            logError(e2,"e9",file);
         }
         // just catch and log these, less important

         throw new Exception(Text.get(this,"e11"),e);
      }

   // atomic creation transaction, hopefully no errors

      // note that up to now we've made no changes to the folder contents,
      // so that if we reject an order, the caller has no surprises there.

      try {

         File done = rename(temp,core,STATUS_ACCEPTED,"e8");
         LocalStatus.report(orderID,LocalStatus.CODE_INFO_ACCEPTED,
                            System.currentTimeMillis(),Level.INFO,Text.get(this,"i3"),null);

         writeOrderXML(docOrder,done);
         insertOrderObject(order,done);

         // I'd like all this to be atomic, but there are so many points of failure,
         // it's hard to know where to begin.  plus, the local order submit process
         // might be checking on either the folder rename or the acceptance message,
         // so as soon as we've done one or the other, we're committed.

      } catch (Exception e) {
         throw new Exception(Text.get(this,"e12"),e);
      }
   }

   /**
    * @param key A text key for the error text.
    * @return The dest file that we renamed to.
    */
   private File rename(File src, String core, String status, String key) throws Exception {
      File dest = new File(src.getParentFile(),status + core);
      if ( ! src.renameTo(dest) ) throw new Exception(Text.get(this,key));
      return dest;
   }

// --- layer 3 - detail functions ---

   private Document readLocalXML(File temp) throws Exception {
      return XML.readFile(new File(temp,Order.LOCAL_FILE));
   }

   private Document transformToOrderXML(Document docLocal) throws Exception {
      Document docOrder = XML.createDocument();
      XML.transform(docOrder,docLocal,Resource.getResourceAsStream(this,"LocalTransform.xsl"),null);

      Node node = (Node) docOrder.getDocumentElement();
      if (node == null) throw new Exception(Text.get(this,"e18",new Object[] { OrderParser.NAME_ORDER }));
      // this means source document had wrong top-level node, resulting in empty destination document

      return docOrder;
   }

   private Order createOrderObject(int orderID) {

      // this fills in the stub part; the rest comes from the parser

      Order order = new Order();

      order.orderSeq = Order.ORDER_SEQUENCE_LOCAL;
      order.orderID = orderID;

      // orderDir filled in later
      order.status = Order.STATUS_ORDER_RECEIVED;
      order.hold = Order.HOLD_NONE;

      // recmodDate filled in later

      // wholesale and error info stays null

      return order;
   }

   /**
    * Fill in some fields we couldn't build in XSL
    * that we need to have in place before parsing.
    */
   private void postprocess1(Document docOrder, String fullID, File temp) throws Exception {
      Node node = XML.getElement(docOrder,OrderParser.NAME_ORDER);

      // this runs before parseOrderXML, so there's no guarantee that any nodes are around,
      // and we have to make sure any points of failure generate reasonable error messages.
      // with the top node checked in transformToOrderXML, and with the way the transform
      // is set up right now, the only point of failure is that there might be no node for
      // Images ... but the message there is clear enough.

   // order ID

      XML.setAttribute(node,OrderParser.NAME_ORDER_ID,fullID);

   // order time

      // reformat to server time to be consistent with other order files.
      // this means we don't have to change the parser, but
      // the main tangible benefit is, Beaufort format continues to work,

      Node nodeTime = XML.getElementTry(node,OrderParser.NAME_ORDER_TIME);
      if (nodeTime != null) {

         SimpleDateFormat fr = new SimpleDateFormat(Text.get(this,"f1"));
         fr.setLenient(false);
         fr.setTimeZone(TimeZone.getTimeZone("GMT"));
         SimpleDateFormat fw = OrderParser.getDateFormat();

         String s = XML.getText(nodeTime);

         Date time;
         try {
            time = Convert.parseZone(fr,"",s);
         } catch (Exception e) {
            throw new Exception(Text.get(this,"e15",new Object[] { s }),e);
         }

         s = fw.format(time);

         XML.replaceElementText(node,nodeTime,OrderParser.NAME_ORDER_TIME,s);
      }

   // file info

      int version = OrderParser.getVersion(node);
      HashMap map = new HashMap();

      Iterator i = OrderParser.getItems(node);
      while (i.hasNext()) {
         Node item = (Node) i.next();

         Iterator j = OrderParser.getImages(item,version);
         while (j.hasNext()) {
            Node image = (Node) j.next();

            Node nodeURL  = XML.getElement(image,OrderParser.NAME_DOWNLOAD_URL );
            Node nodeSize = XML.getElement(image,OrderParser.NAME_DOWNLOAD_SIZE);

            String file = XML.getText(nodeURL);
            XML.replaceElementText(image,nodeURL,OrderParser.NAME_DOWNLOAD_URL,"file:" + file);
            // read file name for use below, then rewrite with prefix.
            // the prefix doesn't make it an actual URL, but no matter, we only consider it as a string;
            // the point is to be unique per file and prevent collisions with the URL_INVOICE constants.

            String size = (String) map.get(file);
            if (size == null) {
               size = Convert.fromLong(FileUtil.getSize(new File(temp,file)));
               map.put(file,size);
               // can't put long in map, so go ahead and convert to string
            }
            XML.replaceElementText(image,nodeSize,OrderParser.NAME_DOWNLOAD_SIZE,size);
         }
      }

   // validation

      // make sure we understand everything that's in the directory
      String[] name = temp.list();
      if (name == null) throw new Exception(Text.get(this,"e13",new Object[] { Convert.fromFile(temp) }));

      for (int j=0; j<name.length; j++) {
         if (map.get(name[j]) == null && ! name[j].equals(Order.LOCAL_FILE)) throw new Exception(Text.get(this,"e14",new Object[] { name[j] }));
      }
   }

   private void parseOrderXML(Order order, Document docOrder, HashMap summaryMap) throws Exception {
      OrderParser p = new OrderParser(order,/* wsc = */ Order.WSC_NORMAL,/* conversionMap = */ null,bookMap,enableItemPrice,/* localOrder = */ true,summaryMap);
      try {
         XML.loadDoc(docOrder,p,OrderParser.NAME_ORDER);
      } catch (Exception e) {
         throw new Exception(Text.get(this,"e16"),e);
      }
   }

   /**
    * Use the summary map to fill in the order summary.
    */
   private void postprocess2(Document docOrder, HashMap summaryMap) throws Exception {
      Node n1 = XML.getElement(docOrder,OrderParser.NAME_ORDER);
      Node n2 = XML.getElement(n1,OrderParser.NAME_SUMMARY); // transform creates placeholder

      // since the ship method has already been stored in the Comment field,
      // we should create an order summary with no ShipMethod

      LinkedList records = new LinkedList(summaryMap.values());

      Accessor skuAccessor = new Accessor() {
         public Class getFieldClass() { return SKU.class; }
         public Object get(Object o) { return ((OrderParser.SummaryRecord) o).sku; }
      };
      Collections.sort(records,new FieldComparator(skuAccessor,SKUComparator.displayOrder));

      Iterator i = records.iterator();
      while (i.hasNext()) {
         OrderParser.SummaryRecord r = (OrderParser.SummaryRecord) i.next();

         Node n3 = XML.createElement(n2,OrderParser.NAME_SUMMARY_PRODUCT);

         // in the ideal world, the server would send down a normal XML file
         // with a SKU and some attributes, and the SKU parser would rewrite
         // the SKU into a descriptive string plus a base SKU.
         //
         // in the real world, the server doesn't deal with attributes well,
         // so instead it generates some weird SKU string form and sends
         // that as the SKU, plus an empty attributes section which we then
         // process without really changing anything.
         //
         // here, the correct thing to do would be to write out a SKU, base
         // SKU, and attributes.  but, given that the server side is already
         // messed up, and given that we don't have any way to write a SKU
         // out as an attribute structure (though we could add that easily),
         // let's just write the string form.
         //
         XML.createElementText (n3,OrderParser.NAME_SUMMARY_SKU,r.sku.toString());

         XML.createNullableText(n3,"Description",r.description);
         XML.createElementText (n3,"QTY",Convert.fromInt(r.quantity));
         XML.createNullableText(n3,"DisplayQty",Convert.fromNullableInt(r.displayQuantity));

         // the price isn't currently used, but we receive it in the
         // standard order XML, so create it here too.
         // these are basically createNullableText(fromNullableCents)
         //
         if (r.price != null) XML.createElementText(n3,"Price",   Convert.fromCents(r.price.intValue()));
         if (r.total != null) XML.createElementText(n3,"Subtotal",Convert.fromCents(r.total.intValue()));
      }

      XML.addIndentation(n1); // can do this now that we're done adding things.
      // unfortunately it also breaks up the base SKU lines, but there's
      // no easy fix for that; order parser must run before summary generation.
   }

   private void writeOrderXML(Document docOrder, File done) throws Exception {
      FileOutputStream stream = new FileOutputStream(new File(done,Order.ORDER_FILE));
      try {
         OrderParser.writePrefix(stream,stylesheet);
         XML.writeStream(stream,docOrder);
      } finally {
         stream.close();
      }
   }

   private void insertOrderObject(Order order, File done) throws Exception {

      order.orderDir = done;
      order.recmodDate = new Date();

      Object lock = table.insert(order);
      table.release(order,lock);
   }

}

