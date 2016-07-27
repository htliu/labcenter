/*
 * FormatRawJPEG.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import java.awt.print.PageFormat;

/**
 * Implementation of the direct-print JPEG format.
 */

public class FormatRawJPEG extends Format {

// --- subclass hooks ---

   public String getShortName() { return Text.get(this,"s1"); }

   public int[] getAllowedCompletionModes() { return new int[] { COMPLETION_MODE_MANUAL, COMPLETION_MODE_AUTO }; }
   public int   getCompletionMode(Object formatConfig) { return ((RawJPEGConfig) formatConfig).completeImmediately ? COMPLETION_MODE_AUTO : COMPLETION_MODE_MANUAL; }
   public void  setCompletionMode(Object formatConfig, int mode) { ((RawJPEGConfig) formatConfig).completeImmediately = (mode == COMPLETION_MODE_AUTO); }

// --- format function ---

   public void format(Job job, Order order, Object formatConfig) throws Exception {

      RawJPEGConfig config = (RawJPEGConfig) formatConfig;
      // because of validation, the cast is guaranteed to work

   // check things and gather information

      if (job.refs.size() != 1) throw new Exception(Text.get(this,"e1")); // shouldn't happen
      Job.Ref ref = (Job.Ref) job.refs.getFirst();

      Order.Item item = order.getItem(ref);

      List filenames = item.isMultiImage() ? item.filenames : Collections.singletonList(item.filename);
      Iterator j = filenames.iterator();
      while (j.hasNext()) {
         String filename = (String) j.next();

         if (    ! filename.toLowerCase().endsWith(".jpg")
              && ! filename.toLowerCase().endsWith(".jpeg") ) throw new Exception(Text.get(this,"e3"));
         // go ahead and check this before we do too much work
      }

      RawJPEGMapping m = (RawJPEGMapping) MappingUtil.getMapping(config.mappings,ref.sku);
      if (m == null) missingChannel(ref.sku,order.getFullID());

   // generate PJL commands
   // send to printer

      LinkedList streams = new LinkedList();
      SizeRecord r = new SizeRecord();

      j = filenames.iterator();
      while (j.hasNext()) {
         String filename = (String) j.next();

         LinkedList commands = new LinkedList();

         readWidthHeight(r,order.getPath(filename));
         int oImage = getOrientation(r.width,r.height);
         int oPaper = getOrientation(m.width,m.height);
         boolean reorient = (oImage*oPaper < 0);
         Integer orientation = new Integer(reorient ? PageFormat.LANDSCAPE : PageFormat.PORTRAIT);

         config.addStandardCommand(commands,job.getOutputQuantity(item,ref),orientation);
         // config.orientation is ignored, see PJLHelper for more

         commands.add("SET PAPERWIDTH="  + toDecipoints(m.width ));
         commands.add("SET PAPERLENGTH=" + toDecipoints(m.height));

         if (config.scaleEnable) {

            // without scaling, the HP printer prints the image at 300 DPI,
            // using one pixel per dot, which for us is rarely correct.
            // you can say "SCALE=0" to autoscale, but as HP explained,
            // "it will fit to the destination page size that if redefined
            // will not be the same as the real page size from the printer".
            // short version, we tried it and it printed at full 24" width.
            //
            // so, we have to specify the exact scale, and it has to be an
            // integer percentage, hence the round up / round down setting.
            // actually it's a little more complicated than that.  the two
            // behaviors that I think we might want are:
            //
            // 1. always cover the complete printable area, which means scale
            //    to crop and then round up.  this seems like the usual thing
            //    one would want to do.
            //
            // 2. always fit completely into the printable area, which means
            //    scale to fit and then round down.  not ideal, but we might
            //    have to do this if e.g. the printer chokes when the scaled
            //    image doesn't fit in the paper bounds.
            //
            // that seems like a big change in behavior, but actually I think
            // the crop/fit piece should be moot since the image aspect ratio
            // should be close to the paper aspect ratio.
            //
            // anyway, here's the code, cf. PrintScaledImage and surely other
            // places too.

            int w = r.width;
            int h = r.height;
            if (reorient) { int temp = w; w = h; h = temp; }

            int dpi = 300; // ideally, ought to factor code in app.ConfigPJL
            // and use it to scan the custom PJL for a HP resolution setting,
            // but it's a lot of work for no foreseeable benefit.

            double scaleX = dpi*m.width  / w;
            double scaleY = dpi*m.height / h;

            int percent = 100;
            int scale;

            if (config.scaleRoundDown) {
               scale = (int) Math.floor(percent*Math.min(scaleX,scaleY)); // case 2, use smaller scale
            } else {
               scale = (int) Math.ceil (percent*Math.max(scaleX,scaleY)); // case 1, use larger  scale
            }

            if (scale < RawJPEGConfig.SCALE_MIN) { // image resolution too high, yeah right
               throw new Exception(Text.get(this,"e4a",new Object[] { Convert.fromInt(scale), Convert.fromInt(RawJPEGConfig.SCALE_MIN) }));
            } else if (scale > RawJPEGConfig.SCALE_MAX) { // image resolution too low
               throw new Exception(Text.get(this,"e4b",new Object[] { Convert.fromInt(scale), Convert.fromInt(RawJPEGConfig.SCALE_MAX) }));
            }

            commands.add("SET SCALE=" + Convert.fromInt(scale));
         }

         if (m.margin != null) {
            RawJPEGMapping.Margin x = RawJPEGMapping.parse(m.margin);
            if (x.isBorderless()) {
               commands.add("SET MARGINS=NOMARGINS");
               commands.add("SET BORDERLESSMETHOD=" + x.getBorderlessMethod());
            } else {
               commands.add("SET MARGINS=NORMAL");
               commands.add("SET MARGINLAYOUT=" + x.getMarginLayout());
               double[] a = x.getMargins();
               if (a == null) a = new double[] { 0,0,0,0 }; // (*)
               commands.add("SET LEFTMARGIN="   + toDecipoints(a[RawJPEGMapping.MARGIN_L]));
               commands.add("SET RIGHTMARGIN="  + toDecipoints(a[RawJPEGMapping.MARGIN_R]));
               commands.add("SET TOPMARGIN="    + toDecipoints(a[RawJPEGMapping.MARGIN_T]));
               commands.add("SET BOTTOMMARGIN=" + toDecipoints(a[RawJPEGMapping.MARGIN_B]));

               // (*) what's this about?  the answer is, what we're specifying are the
               // logical margins, and in OVERSIZE and CLIPINSIDE layouts, the doc
               // says that the logical margins must be zero.  the printer has its own
               // built-in physical margins, and that's what gets applied.

               // STANDARD : paper size reduced by logical margins equals drawing area
               // OVERSIZE : paper size equals drawing area, actual paper is larger
               // CLIPINSIDE : paper size equals drawing area but don't draw in the margins
            }
         }

         config.addLanguageCommand(commands,config.language);

         config.createStreams(streams,commands,order.getPath(filename),order.getFullID());
      }

      config.sendToPrinter(streams);

   // done

      if (config.completeImmediately) job.property = "";
   }

   private static String toDecipoints(double d) {
      return Convert.fromLong(Math.round(d*720));
   }

// --- completion ---

   public boolean isCompleteWithoutDirectory(String property, Special special) throws Exception {
      return (property != null);
      // not null means job should complete immediately
   }

}

