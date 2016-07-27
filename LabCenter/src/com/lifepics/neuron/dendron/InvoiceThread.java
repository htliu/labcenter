/*
 * InvoiceThread.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.gui.Print;
import com.lifepics.neuron.misc.FileUtil;
import com.lifepics.neuron.struct.SKU;
import com.lifepics.neuron.table.Table;
import com.lifepics.neuron.thread.EntityManipulator;
import com.lifepics.neuron.thread.ThreadStatus;
import com.lifepics.neuron.thread.ThreadStopException;

import java.io.File;
import java.util.LinkedList;

/**
 * A thread that generates and prints invoices.
 */

public class InvoiceThread extends OrderThread {

// --- fields ---

   private File stylesheet;
   private File postageXSL;
   private JobManager jobManager;

   private boolean skipInvoice;
   private boolean skipLabel;

// --- construction ---

   public InvoiceThread(Table table, long idlePollInterval, ThreadStatus threadStatus,
                        File stylesheet, File postageXSL, JobManager jobManager,
                        boolean skipInvoice, boolean skipLabel) {
      super(Text.get(InvoiceThread.class,"s1"),
            table,
            new EntityManipulator(
               Order.STATUS_ORDER_RECEIVED,
               Order.STATUS_ORDER_INVOICING,
               Order.STATUS_ORDER_PRESPAWN),
            /* scanFlag = */ false,
            idlePollInterval,
            threadStatus);

      this.stylesheet = stylesheet;
      this.postageXSL = postageXSL;
      this.jobManager = jobManager;

      this.skipInvoice = skipInvoice;
      this.skipLabel   = skipLabel;
   }

// --- main functions ---

   protected boolean findEntity() {
      boolean found = super.findEntity();
      if ( ! found ) {
         skipInvoice = false;
         skipLabel   = false;
      }
      return found;
      // this is what defines the "current orders" that the MiniPanel talks about.
      // the very first time we don't find an order to process, stop the skipping.
   }

   protected boolean doOrder() throws Exception {

      File jpegTemp = null;
      File jpegFile = null;

      Invoice invoice;
      try {

         // allow generation of JPEG invoice if we don't already have one
         if ( ! hasJpegInvoice() ) jpegTemp = new File(order.orderDir,Order.JPEG_TEMP);

         invoice = new Invoice((Order) order,stylesheet,postageXSL,jobManager,jpegTemp);

         if (jpegTemp != null && jpegTemp.exists()) { // JPEG invoice enabled for this order

            String filename = OrderParser.disambiguate((Order) order,Order.JPEG_FILE);
            jpegFile = new File(order.orderDir,filename);

            SKU sku = invoice.generate(jpegTemp,jpegFile);
            // it's a bit convoluted, but we know the SKU isn't null.
            // the temp file is only generated if JPEG is enabled,
            // and there's a conditional validation on the SKU in that case.

            addJpegInvoice(filename,sku);
         }

      } finally {
         // clean up the temp file, and the JPEG file too unless it's owned
         if (jpegTemp != null && jpegTemp.exists()) jpegTemp.delete();
         if (jpegFile != null && jpegFile.exists() && ! hasJpegInvoice()) jpegFile.delete();
      }

      // JPEG generation comes first, because it's better to regenerate on print failure
      // than to reprint on generation failure.  actually, we won't even regenerate,
      // because hasJpegInvoice can tell from the order object that generation succeeded.

      // note, the printing code is not transactionally nice ... if the
      // label print fails, we'll run the invoice print again next time.
      try {

         if ( ! skipInvoice ) invoice.print(Print.MODE_INVOICE);
         if ( ! skipLabel   ) invoice.print(Print.MODE_LABEL  );

      } catch (Exception e) {

         throw new ThreadStopException(Text.get(this,"e7"),e);
         //
         // the idea is, there's nothing that goes on in the print function
         // that is order-specific, so any exception should stop the thread.
         // and, yes, stop rather than pause, because printer errors,
         // unlike network errors, typically require user intervention.
      }

      return true;
   }

   private boolean hasJpegInvoice() {

      LinkedList files = ((Order) order).files;
      if (files.isEmpty()) return false;

      Order.OrderFile file = (Order.OrderFile) files.getFirst();
      return (    file.downloadURL.equals(Order.URL_INVOICE_NEW)
               || file.downloadURL.equals(Order.URL_INVOICE_OLD) );
   }

   private void addJpegInvoice(String filename, SKU sku) {

      Order.OrderFile file = new Order.OrderFile();
      file.filename = filename;
      file.status = Order.STATUS_ITEM_RECEIVED;
      file.originalFilename = Order.JPEG_FILE;
      file.downloadURL = Order.URL_INVOICE_NEW;
      // no need to record size, that's only for download now

      Order.Item item = new Order.Item();
      item.filename = filename;
      item.status = Order.STATUS_ITEM_RECEIVED;
      item.sku = sku;
      item.quantity = 1;
      // filenames stays empty

      ((Order) order).files.addFirst(file);
      ((Order) order).items.addFirst(item);
   }

}

