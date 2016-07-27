/*
 * FormatDNP.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.gui.ImageLoader;
import com.lifepics.neuron.gui.PrintScaledImage;
import com.lifepics.neuron.thread.StoppableThread;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

/**
 * Implementation of the DNP direct format.
 */

public class FormatDNP extends Format {

// --- subclass hooks ---

   public String getShortName() { return Text.get(this,"s1"); }

   public int[] getAllowedCompletionModes() { return new int[] { COMPLETION_MODE_MANUAL, COMPLETION_MODE_AUTO }; }
   public int   getCompletionMode(Object formatConfig) { return ((DNPConfig) formatConfig).completeImmediately ? COMPLETION_MODE_AUTO : COMPLETION_MODE_MANUAL; }
   public void  setCompletionMode(Object formatConfig, int mode) { ((DNPConfig) formatConfig).completeImmediately = (mode == COMPLETION_MODE_AUTO); }

// --- format function ---

   public void format(Job job, Order order, Object formatConfig) throws Exception {}
   //
   public boolean formatStoppable(Job job, Order order, Object formatConfig, FormatCallback fc) throws Exception {

      DNPConfig config = (DNPConfig) formatConfig;
      // because of validation, the cast is guaranteed to work

   // initial communication with printer

      // do this first so we can check media compatibility in the SKU loop

      initialize();

      byte[] data = new byte[64]; // so, limited to 32 printers on one machine
      int count = checkGetPrinterPortNum(data);
      int port = findPort(count,data,config.printerType,config.printerIDType,config.printerID);

      if ( ! lock(port) ) throw new Exception(Text.get(this,"e11"));
      try {
      // there's no reason to have two threads writing to the same port
      // at the same time.  the same is true with other integrations,
      // but here it seems likely to cause fatal crashes, so prevent it.

      String code = checkGetMedia(port);
      DNPMedia paper = DNPMedia.findMedia(code); // exception on failure
      // the size is really for the film not the paper, but close enough

   // fairly standard SKU loop

      HashMap mediaMap = new HashMap(); // m.media to LinkedList<Slide>
      //
      // might be nice to order by SKU within each media list, but let's
      // save that for later.  in practice the refs will probably be
      // ordered by SKU anyway, and will go into the lists in that order.

      Iterator i = job.refs.iterator();
      while (i.hasNext()) {
         Job.Ref ref = (Job.Ref) i.next();

      // collect information and check if already done

         Order.Item item = getOrderItem(order,ref);
         int quantity = job.getOutputQuantity(item,ref); // positive by validation
         int doneQuantity = ref.getDoneQuantity();       // getDoneQuantity makes nonnegative, but no upper limit
         boolean isMultiImage = item.isMultiImage();
         int imageCount = isMultiImage ? item.filenames.size() : 1;
         int goalQuantity = quantity * imageCount;

         if (doneQuantity >= goalQuantity) continue;     // no validation on doneQuantity so use inequality

      // OK, do more expensive work

         // can't use a hash set to reduce the workload here
         // since we need to look up the media for every ref.

         DNPMapping m = (DNPMapping) MappingUtil.getMapping(config.mappings,ref.sku);
         if (m == null) missingChannel(ref.sku,order.getFullID());

         Integer key = new Integer(m.media);
         LinkedList slides = (LinkedList) mediaMap.get(key);
         if (slides == null) {

            // this part at least we can do just once
            DNPMedia media = DNPMedia.findMedia(m.media);
            if (    media.width  > paper.width
                 || media.height > paper.height ) throw new Exception(Text.get(this,"e6",new Object[] { paper.describe(), media.describe() }));
            // note, yes, media width can be less than paper width,
            // though it's not recommended since you'll have to do some cutting

            slides = new LinkedList();
            mediaMap.put(key,slides);
         }

      // create slides

         List filenames = isMultiImage ? item.filenames : Collections.singletonList(item.filename);
         Iterator j = filenames.iterator();
         while (j.hasNext()) {
            String filename = (String) j.next();

            if (doneQuantity >= quantity) { doneQuantity -= quantity; continue; }
            int todoQuantity = quantity - doneQuantity;
            doneQuantity = 0; // only need to clear it the first time, but it's harmless

            File file = order.getPath(filename);
            slides.add(new Slide(ref,file,todoQuantity,m));
         }
      }

   // main printing loop

      i = mediaMap.entrySet().iterator();
      while (i.hasNext()) {
         Map.Entry me = (Map.Entry) i.next();

         // see if we can use multi-cut
         int multi = 1;
         int printMedia = ((Integer) me.getKey()).intValue();

         DNPMedia media = DNPMedia.findMedia(printMedia);

         if        (media.multi3 != 0 && paper.height >= 3*media.height) {
            multi = 3;
            printMedia = media.multi3;
         } else if (media.multi2 != 0 && paper.height >= 2*media.height) {
            multi = 2;
            printMedia = media.multi2;
         }

         check("c4",DNP.SetMediaSize(port,printMedia));

         LinkedList slides = (LinkedList) me.getValue();
         Screen r = new Screen(media,config.rotateCW);
         // note that only one screen object is allocated at a time;
         // this is important because it contains a large buffer.

         Projector p = new Projector(slides,r);
         while (p.hasNext()) {
            if ( ! wait(port,config.maxWaitInterval) ) return false;
            print(port,multi,p,r,fc);
         }
      }

      } finally {
         unlock(port);
      }

   // done

      if (config.completeImmediately) job.property = "";
      return true;
   }

   private static boolean wait(int port, long maxWaitInterval) throws Exception {

      StoppableThread thread = (StoppableThread) Thread.currentThread();

      // hardcoded constant
      int retryDelay = 1000;

      long retryCount = maxWaitInterval / retryDelay;
      // so, 0-999 means no retries, 1000-1999 means one retry, etc.
      // often we want to round the other way,
      // but this makes the most sense to me in this situation.

      while (true) {

      // check for error and exit conditions

         if (thread.isStopping()) return false;
         checkStatus(DNP.GetStatus(port));

      // see if printer is ready for more data

         if (check("c6",DNP.GetFreeBuffer(port)) > 0) return true;

      // have we tried enough?

         if (retryCount-- == 0) throw new Exception(Text.get(FormatDNP.class,"e8"));

      // wait and retry

         thread.sleepNice(retryDelay);
         // you should always check isStopping after sleepNice.
         // in this case we do it at the top of the loop
         // so that even a long run with no waits can be stopped.
      }
   }

   private static void print(int port, int multi, Projector p, Screen r, FormatCallback fc) throws Exception {

      if (multi != 1) check("c7",DNP.StartPageLayout(port));
      for (int i=0; i<multi; i++) {

         if ( ! p.next() ) break; // can't happen on first image
         // because we tested hasNext before print function call

         if (p.finish != DNP.FINISH_GLOSSY) {
            check("c8",DNP.SetOvercoatFinish(port,p.finish));
         }

         check("c9",DNP.SendImageData(port,r.data,r.x,r.y,r.w,r.h));
      }
      if (multi != 1) check("c10",DNP.EndPageLayout(port));

      p.commit();
      fc.saveJobState();
   }

// --- overview ---

   // imagine an old slide projector: you press the button,
   // and, ka-chunk, the next slide appears on screen.
   // only, here, the same slide stays for several presses.

   // the main complication here is, we don't want to
   // increment Job.Ref.doneQuantity until the print has
   // actually completed.  so, we store all the refs
   // to be incremented in the transaction list, usually
   // with duplicates, and increment them at the end.

   // for multi-image items, we print the images uncollated.
   // doneQuantity is incremented every time,
   // so it goes up to the job quantity times the image count.

   // note, the printer does have a SetPQTY function, but
   // I'm not using it because (1) DNP doesn't, (2) it's
   // stored persistently on the printer, so I'd have to be
   // super-careful to clear it, and (3) the quantity is
   // for an entire layout, so it complicates the multi-cut
   // logic a lot .. and (4), this method is easy enough.

// --- slide ---

   private static class Slide {

      public Job.Ref ref;
      public File file;
      public int todoQuantity;
      public DNPMapping m;

      public Slide(Job.Ref ref, File file, int todoQuantity, DNPMapping m) {
         this.ref = ref;
         this.file = file;
         this.todoQuantity = todoQuantity;
         this.m = m;
      }
   }

// --- screen ---

   private static class Screen {

      private boolean rotateCW;

      public int x;
      public int y;
      public int w;
      public int h;

      private BufferedImage buffer;
      public byte[] data;

      public Screen(DNPMedia media, boolean rotateCW) throws Exception {

         this.rotateCW = rotateCW;

         // always allocate full-size image.  I tried implementing margins
         // by adjusting these, but odd-valued margins caused hard crashes.
         x = 0;
         y = 0;
         w = media.imageWidth;
         h = media.imageHeight;

         try {
            buffer = new BufferedImage(w,h,BufferedImage.TYPE_3BYTE_BGR);
         } catch (OutOfMemoryError e) {
            throw new Exception(Text.get(FormatDNP.class,"e9"));
         }

         data = ((DataBufferByte) buffer.getRaster().getDataBuffer()).getData();
         // note, buffer.getData() also returns a raster,
         // but that's a copy of the current state, not the master copy
      }

      public void load(File file, DNPMapping m) throws Exception {

         // it could happen that load gets called twice in a row with the same arguments,
         // in which case we could optimize by doing nothing, but it's definitely a rare
         // case, not worth the trouble.

      // load image

         ImageLoader imageLoader = new ImageLoader(new File[] { file }).preload();
         // this also checks for OutOfMemoryError

      // figure out coords

         int[] marginArray;
         if (m.margin != null) {
            marginArray = Convert.toFlexArray(m.margin,DNPMapping.MARGIN_N);
         } else {
            marginArray = Convert.makeUniform(0,DNPMapping.MARGIN_N); // margin 0
         }

         int[] tilesArray;
         if (m.tiles != null) {
            tilesArray = Convert.toIntArray(m.tiles,DNPMapping.TILES_N);
         } else {
            tilesArray = Convert.makeUniform(1,DNPMapping.TILES_N); // 1x1
         }

         int nx = tilesArray[DNPMapping.TILES_X];
         int ny = tilesArray[DNPMapping.TILES_Y];

         int availx = w - marginArray[DNPMapping.MARGIN_L] - marginArray[DNPMapping.MARGIN_R];
         int availy = h - marginArray[DNPMapping.MARGIN_T] - marginArray[DNPMapping.MARGIN_B];

         // quotient
         int tw = availx / nx; // could use availx-extrax, but same result
         int th = availy / ny;

         // remainder
         int extrax = availx % nx; // note, mod 1 works
         int extray = availy % ny;

         int halfx = extrax / 2;
         int halfy = extray / 2;

         marginArray[DNPMapping.MARGIN_L] += halfx;
         marginArray[DNPMapping.MARGIN_R] += extrax-halfx;
         marginArray[DNPMapping.MARGIN_T] += halfy;
         marginArray[DNPMapping.MARGIN_B] += extray-halfy;

         int mx = marginArray[DNPMapping.MARGIN_R]; // right margin because flipped
         int my = marginArray[DNPMapping.MARGIN_T];

         int mx2 = marginArray[DNPMapping.MARGIN_L];
         int my2 = marginArray[DNPMapping.MARGIN_B];

      // draw image

         Graphics g = buffer.getGraphics();

         g.setColor(Color.white);
         g.fillRect(0,0,w,h);
         // should all be repainted, but let's make sure no black

         if (tw == 0 || th == 0) return; // unlikely but possible

         PrintScaledImage psi = new PrintScaledImage(imageLoader,rotateCW,mx,my,tw,th,/* flip = */ true);
         psi.print(g,/* pageFormat = */ null,/* pageIndex = */ 0);

         for (int ix=1; ix<nx; ix++) { // copy across
            g.copyArea(mx,my,tw,th,/* dx = */ tw*ix,/* dy = */ 0);
         }

         for (int iy=1; iy<ny; iy++) { // copy down
            g.copyArea(mx,my,tw*nx,th,/* dx = */ 0,/* dy = */ th*iy);
         }

         // redraw margins, since PrintScaledImage doesn't clip,
         // but avoid making calls when width or height is zero,
         // which it usually is.
         if (my  > 0) g.fillRect(0,0,    w,my );
         if (my2 > 0) g.fillRect(0,h-my2,w,my2);
         if (mx  > 0) g.fillRect(0,    my,mx, th*ny);
         if (mx2 > 0) g.fillRect(w-mx2,my,mx2,th*ny);
      }
   }

// --- projector ---

   private static class Projector {

      private LinkedList slides;
      private Screen r;

      private Slide slide;
      private LinkedList transaction;

      public int finish;

      public Projector(LinkedList slides, Screen r) {

         this.slides = slides;
         this.r = r;

         slide = null;
         transaction = new LinkedList();

         // finish is undefined
      }

      public boolean hasNext() {
         return ! (slide == null && slides.isEmpty());
      }

      public boolean next() throws Exception {

         if (slide == null) {
            if (slides.isEmpty()) return false;
            slide = (Slide) slides.removeFirst();

            finish = slide.m.finish;
            r.load(slide.file,slide.m);
         }

         transaction.add(slide.ref);

         if (--slide.todoQuantity == 0) slide = null; // done with this.
         // we know todoQuantity was positive, so equality test is fine.

         return true;
      }

      public void commit() {
         while ( ! transaction.isEmpty() ) {
            incrementDoneQuantity((Job.Ref) transaction.removeFirst());
         }
      }
   }

// --- helpers ---

   public static void initialize() throws Exception {
      try {
         check("c1",DNP.SetPrinterFilter(DNP.FILTER_ON));
         // according to the specs, SetOvercoatFinish is the only function
         // I use that's not supported on all models and firmware versions
      } catch (UnsatisfiedLinkError e) {
         throw new Exception(Text.get(FormatDNP.class,"e1"));
      } catch (NoClassDefFoundError e) {
         throw new Exception(Text.get(FormatDNP.class,"e5"));
         // the first time through you get an UnsatisfiedLinkException,
         // but the second time through you get this.  I guess Java
         // marks the class as unable to load, and doesn't try again?
      }
   }

   // static locking helpers, synchronized on the class object
   private static HashSet lockSet = new HashSet();
   private static synchronized boolean lock(int port) {
      return lockSet.add(new Integer(port));
   }
   private static synchronized void unlock(int port) {
      lockSet.remove(new Integer(port));
   }

   // wrappers for functions that are used multiple places / by DNPPicker
   //
   public static int checkGetPrinterPortNum(byte[] data) throws Exception {
      return check("c13",DNP.GetPrinterPortNum(data));
   }
   public static String checkGetSerialNo(int port) throws Exception {
      return checkString("c14",DNP.GetSerialNo(port));
   }
   public static String checkGetMedia(int port) throws Exception {
      return checkString("c15",DNP.GetMedia(port));
   }

   private static int check(String key, int result) throws Exception {
      if (result < 0) {
         throw new Exception(Text.get(FormatDNP.class,"e2",new Object[] { Text.get(FormatDNP.class,key), Convert.fromInt(result) }));
      }
      return result; // sometimes it's not just a result code
   }

   private static String checkString(String key, String result) throws Exception {
      if (result == null) {
         throw new Exception(Text.get(FormatDNP.class,"e3",new Object[] { Text.get(FormatDNP.class,key) }));
      }
      return result;
   }

   private static void checkStatus(int result) throws Exception {
      String key = null;

      // I don't care about the groups, just go straight to the statuses
      switch (result) {

      case DNP.STATUS_NORMAL_IDLE:
      case DNP.STATUS_NORMAL_PRINTING:
      case DNP.STATUS_NORMAL_STANDSTILL: // not in the specs or in Trey's code, but let's accept it
      case DNP.STATUS_NORMAL_COOLING:
      case DNP.STATUS_NORMAL_MOTCOOLING:
         return;

      //   DNP.STATUS_NORMAL_SHOOTING    // these are tower-only, shouldn't happen here
      //   DNP.STATUS_NORMAL_BACKPRINT

      case DNP.STATUS_NORMAL_PAPER_END:      key = "st1";  break;
      case DNP.STATUS_NORMAL_RIBBON_END:     key = "st2";  break;

      // other cases where it seems polite to have a custom message
      //
      case DNP.STATUS_SETTING_COVER_OPEN:    key = "st3";  break;
      case DNP.STATUS_SETTING_PAPER_JAM:     key = "st4";  break;
      case DNP.STATUS_SETTING_RIBBON_ERR:    key = "st5";  break;
      case DNP.STATUS_SETTING_PAPER_ERR:     key = "st6";  break;
      case DNP.STATUS_SETTING_DATA_ERR:      key = "st7";  break;
      case DNP.STATUS_SETTING_SCRAPBOX_ERR:  key = "st8";  break;
      }

      String status;
      if (key != null) {
         status = Text.get(FormatDNP.class,key);
      } else {
         String s = Integer.toHexString(result).toUpperCase();
         while (s.length() < 8) s = "0" + s;
         s = s.substring(0,4) + " " + s.substring(4,8); // for readability
         status = Text.get(FormatDNP.class,"st0",new Object[] { s });
      }

      throw new Exception(Text.get(FormatDNP.class,"e7",new Object[] { status }));
   }

   private static int findPort(int count, byte[] data, int printerType, int printerIDType, String printerID) throws Exception {
      int match = -1;
      for (int i=0; i<count; i++) {
         if (              data[2*i  ] == printerType
              && isMatch(i,data[2*i+1],printerIDType,printerID) ) {
            if (match != -1) throw new Exception(Text.get(FormatDNP.class,"e10"));
            match = i;
         }
      }
      if (match == -1) throw new Exception(Text.get(FormatDNP.class,"e4"));
      return match;
   }

   private static boolean isMatch(int port, byte datum, int printerIDType, String printerID) throws Exception {

      // we could factor out Convert.toInt and other things,
      // but there are probably at most two printers of any
      // given type, not worth messing with.

      switch (printerIDType) {
      case DNPConfig.ID_TYPE_SINGLE:
         return true;
      case DNPConfig.ID_TYPE_ID:
         return (datum == Convert.toInt(printerID));
      case DNPConfig.ID_TYPE_SERIAL:
         String s1 = checkGetSerialNo(port);
         return s1.equals(printerID);
      case DNPConfig.ID_TYPE_MEDIA:
         String s2 = DNPMedia.getShortCode(checkGetMedia(port));
         return s2.equals(DNPMedia.findMediaByDescription(printerID).scode);
         // scode is known to be non-null, but it doesn't matter because it's
         // on the other side of equals.
      default:
         throw new IllegalArgumentException();
      }
   }

   private static void incrementDoneQuantity(Job.Ref ref) {
      ref.doneQuantity = new Integer(ref.getDoneQuantity()+1);
   }

// --- completion ---

   public boolean isCompleteWithoutDirectory(String property, Special special) throws Exception {
      return (property != null);
      // not null means job should complete immediately
   }

}

