/*
 * Print.java
 */

package com.lifepics.neuron.gui;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;

import java.util.HashMap;

import java.awt.*;
import java.awt.print.*;
import javax.print.PrintService;

/**
 * A utility class for printing.
 */

public class Print {

// --- config ---

   private static PrintConfig config;
   private static ProductBarcodeConfig productBarcodeConfig;
   private static HashMap productBarcodeMap;

   public static PrintConfig getConfig() { return config; }
   public static ProductBarcodeConfig getProductBarcodeConfig() { return productBarcodeConfig; }
   public static HashMap getProductBarcodeMap() { return productBarcodeMap; }

   public static void setConfig(PrintConfig config, ProductBarcodeConfig productBarcodeConfig) {
      Print.config = config;
      Print.productBarcodeConfig = productBarcodeConfig;
      productBarcodeMap = ProductBarcodeMapping.buildProductBarcodeMap(productBarcodeConfig.mappings);
   }

// --- main functions ---

   public static final int MODE_INVOICE = 0;
   public static final int MODE_LABEL   = 1;

   /**
    * @param footer For labels only, the parametrized footer text, or null for none.
    */
   public static void print(Component c, int copies, String footer, String title, int mode) throws Exception {

      PrintConfig.Setup setup;
      switch (mode) {
      case MODE_INVOICE:  setup = config.setupInvoice;  break;
      case MODE_LABEL:    setup = config.setupLabel;    break;
      default:  throw new IllegalArgumentException();
      }

   // set up job

      PrinterJob job = getJob(setup.printer,/* copies = */ 1,title);
      // the number of copies
      // should be copies, but that doesn't work on at least some systems
      // also that doesn't let us change the "Label N of N" text per copy

   // set up paper

      PageFormat pageFormat = getPageFormat(job,setup);

   // create printable

      Printable printable;
      switch (mode) {
      case MODE_INVOICE:  printable = new PrintPaginated(c,copies,pageFormat);         break;
      case MODE_LABEL:    printable = new PrintScaled   (c,copies,pageFormat,footer);  break;
      default:  throw new IllegalArgumentException();
      }

   // print

      print(job,pageFormat,printable);
   }

// --- helpers ---

   // these don't use the static config at all

   public static PrinterJob getJob(String printer, int copies, String title) throws Exception {

      PrinterJob job = PrinterJob.getPrinterJob();
      job.setCopies(copies);
      job.setJobName(title);
      if (printer != null) job.setPrintService(getPrintService(printer));
      // else default printer

      return job;
   }

   public static PrinterJob getJob(String printer) throws Exception {

      // could probably merge with previous one, but I worry the order
      // of the calls might matter

      PrinterJob job = PrinterJob.getPrinterJob();
      if (printer != null) job.setPrintService(getPrintService(printer));

      return job;
   }

   public static PageFormat getPageFormat(PrinterJob job, PageSetup setup) {

      // from PageSetup, printer goes to getJob, the rest goes here.

      // note, setup fields are in inches, don't forget to convert to points

      PageFormat pageFormat = job.defaultPage();
      Paper paper = pageFormat.getPaper();

      if ( ! setup.defaultSize ) paper.setSize(setup.width*PPI,setup.height*PPI);

      Adjuster x = new Adjuster(paper.getWidth(), setup.marginL*PPI,setup.marginR*PPI);
      Adjuster y = new Adjuster(paper.getHeight(),setup.marginT*PPI,setup.marginB*PPI);
      paper.setImageableArea(x.start,y.start,x.range,y.range);

      pageFormat.setPaper(paper);
      pageFormat.setOrientation(setup.orientation);
      // note, orientation change commutes with paper change, not that it matters here

      return job.validatePage(pageFormat);
      // this is important .. e.g., if you set margins to zero, this applies the minimum
   }

   public static void putPageFormat(PageSetup setup, PageFormat pageFormat) {

      // load all fields except printer

      setup.defaultSize = false;

      // not messing with adjusters this time, just write it out.
      // don't worry about precision of doubles, they all get pushed through Convert.fromDouble anyway
      //
      Paper paper = pageFormat.getPaper();
      //
      setup.width   =  paper.getWidth()      / PPI;
      setup.marginL =  paper.getImageableX() / PPI;
      setup.marginR = (paper.getWidth() - (paper.getImageableX() + paper.getImageableWidth())) / PPI;
      //
      setup.height  =  paper.getHeight()     / PPI;
      setup.marginT =  paper.getImageableY() / PPI;
      setup.marginB = (paper.getHeight() - (paper.getImageableY() + paper.getImageableHeight())) / PPI;

      setup.orientation = pageFormat.getOrientation();
   }

   public static String format(PageFormat pageFormat) {
      if (pageFormat == null) return formatNull;
      PageSetup setup = new PageSetup();
      putPageFormat(setup,pageFormat);
      // the actual page format is in points, not very useful
      return format(setup);
   }

   public static String format(PageSetup setup) {
      if (setup == null) return formatNull;
      return Text.get(Print.class,"s1",new Object[] {
            Convert.fromDouble(setup.width),
            Convert.fromDouble(setup.height),
            Convert.fromDouble(setup.marginL),
            Convert.fromDouble(setup.marginR),
            Convert.fromDouble(setup.marginT),
            Convert.fromDouble(setup.marginB),
            Orientation.fromOrientation(setup.orientation)
         });
   }

   private static final String formatNull = Text.get(Print.class,"s2");

   public static void print(PrinterJob job, PageFormat pageFormat, Printable printable) throws Exception {

      job.setPrintable(printable,pageFormat);
      try {
         job.print();
      } catch (PrinterException e) {
         throw new Exception(Text.get(Print.class,"e1"),e);
      } catch (OutOfMemoryError e) {
         throw new Exception(Text.get(Print.class,"e3"));
      } catch (Throwable t) {
         throw new Exception(Text.get(Print.class,"e4"),t);
      }
      // PrinterException is the only checked exception,
      // so it's fine to catch and wrap everything else.
      // note, the last-ditch "unhandled error" message
      // comes from EntityThread.
   }

// --- printer ---

   public static PrintService getPrintService(String name) throws Exception {

      if (name.startsWith(InstallHint.PREFIX)) throw new Exception(Text.get(Print.class,"e5",new Object[] { name }));

      PrintService[] service = PrinterJob.lookupPrintServices();

      // if there's an exact match, you get the single unique match
      for (int i=0; i<service.length; i++) {
         if (service[i].getName().equals(name)) return service[i];
      }

      // if there's a different-case match, hope it's the right one!
      // in other words, we're not detecting ambiguity like ResolveDialog does.
      for (int i=0; i<service.length; i++) {
         if (service[i].getName().equalsIgnoreCase(name)) return service[i];
      }

      throw new Exception(Text.get(Print.class,"e2",new Object[] { name }));
   }

// --- margins ---

   private static final int PPI = 72; // points per inch

   private static class Adjuster {

      public double start;
      public double range;

      public Adjuster(double total, double margin1, double margin2) {

         double limit = total/4; // arbitrary

         if (margin1 > limit) margin1 = limit;
         if (margin2 > limit) margin2 = limit;

         start = margin1;
         range = total - (margin1+margin2);
      }
   }

}

