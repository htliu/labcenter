/*
 * FormatDirectJPEG.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Log;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.gui.ImageLoader;
import com.lifepics.neuron.gui.Print;
import com.lifepics.neuron.gui.PrintScaledImage;

import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterJob;
import java.util.logging.Level;

/**
 * Implementation of the direct-print JPEG format.
 */

public class FormatDirectJPEG extends Format {

// --- subclass hooks ---

   public String getShortName() { return Text.get(this,"s1"); }

   public int[] getAllowedCompletionModes() { return new int[] { COMPLETION_MODE_MANUAL, COMPLETION_MODE_AUTO }; }
   public int   getCompletionMode(Object formatConfig) { return ((DirectJPEGConfig) formatConfig).completeImmediately ? COMPLETION_MODE_AUTO : COMPLETION_MODE_MANUAL; }
   public void  setCompletionMode(Object formatConfig, int mode) { ((DirectJPEGConfig) formatConfig).completeImmediately = (mode == COMPLETION_MODE_AUTO); }

// --- format function ---

   public void format(Job job, Order order, Object formatConfig) throws Exception {

      DirectJPEGConfig config = (DirectJPEGConfig) formatConfig;
      // because of validation, the cast is guaranteed to work

   // get the one item

      if (job.refs.size() != 1) throw new Exception(Text.get(this,"e1")); // shouldn't happen
      Job.Ref ref = (Job.Ref) job.refs.getFirst();

      Order.Item item = order.getItem(ref);

      // SKU code is totally ignored, a weird behavior of this queue

   // send to printer

      ImageLoader imageLoader = new ImageLoader(order.getPaths(item)).preload();
      // do first, preload the most likely point of failure before Print.print

      int quantity = job.getOutputQuantity(item,ref);
      String title = Text.get(this,"s2",new Object[] { order.getFullID() });

      PrinterJob pj = Print.getJob(config.pageSetup.printer,quantity,title);
      // in Print it says that the number of copies setting doesn't always work,
      // but it works on both the printers we're interested in, keep it for now.
      // it should definitely help reduce the size of the print jobs.

      PageFormat pageFormat = Print.getPageFormat(pj,config.pageSetup);
      PrintScaledImage printScaledImage = new PrintScaledImage(imageLoader,config.rotateCW,pageFormat);
      Printable printable = printScaledImage;

      Log.log(Level.FINE,this,"i1",new Object[] { Print.format(config.pageSetup ) });
      Log.log(Level.FINE,this,"i2",new Object[] { Print.format(       pageFormat) });
      printScaledImage.setLogReceived();

      printScaledImage.setTileParameters(config.tileEnable,config.tilePixels);

      Print.print(pj,pageFormat,printable);

      // notes:
      //
      // * orientation is kind of academic since we auto-rotate, but it's there in the API
      // and in PageSetup, so we might as well use it, and it lets us have a meaningful CW
      // vs. CCW feature
      //
      // * I thought adding more options to PrintScaledImage, like letting it do whitespace
      // (aligned left, center, or right) instead of center crop, or borders, but it's
      // really unnecessary  the margin values in PageSetup let you specify whatever region
      // on the page you want, and that's where the image will go, the end.  it's also way
      // less confusing this way!

   // done

      if (config.completeImmediately) job.property = "";
   }

// --- completion ---

   public boolean isCompleteWithoutDirectory(String property, Special special) throws Exception {
      return (property != null);
      // not null means job should complete immediately
   }

}

