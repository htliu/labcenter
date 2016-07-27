/*
 * PrintScaledImage.java
 */

package com.lifepics.neuron.gui;

import com.lifepics.neuron.core.Log;
import com.lifepics.neuron.core.Text;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.util.logging.Level;

/**
 * An implementation of {@link Printable} for scaled images.
 * The main differences with {@link PrintScaled} are,
 * the source is an image not a GUI component, we auto-rotate
 * and center crop instead of scaling to fit, and there's not
 * any footer.
 */

public class PrintScaledImage implements Printable {

// --- fields ---

   private ImageLoader imageLoader;
   private boolean rotateCW;
   private double pageTranslateX;
   private double pageTranslateY;
   private double goalWidth;
   private double goalHeight;
   private boolean flip;

   private boolean logReceived; // setLogReceived

   private boolean tileEnable;  // setTileParameters
   private int tilePixels;

// --- construction ---

   public PrintScaledImage(ImageLoader imageLoader, boolean rotateCW,
                           PageFormat pageFormat) {
      this(imageLoader,rotateCW,
           pageFormat.getImageableX(),pageFormat.getImageableY(),pageFormat.getImageableWidth(),pageFormat.getImageableHeight(),/* flip = */ false);
   }

   public PrintScaledImage(ImageLoader imageLoader, boolean rotateCW,
                           double pageTranslateX, double pageTranslateY, double goalWidth, double goalHeight, boolean flip) {
      this.imageLoader = imageLoader;
      this.rotateCW = rotateCW;
      this.pageTranslateX = pageTranslateX;
      this.pageTranslateY = pageTranslateY;
      this.goalWidth = goalWidth;
      this.goalHeight = goalHeight;
      this.flip = flip;

      logReceived = false;

      tileEnable = false;
      // tilePixels only applies when enabled
   }

   private AffineTransform getTransform(BufferedImage image) {

      int w = image.getWidth();
      int h = image.getHeight();

      AffineTransform rotation = null;

      if (    w > h && goalWidth < goalHeight
           || w < h && goalWidth > goalHeight ) {

         rotation = rotateCW ? Rotation.corner270(w,h) : Rotation.corner90(w,h);

         int temp = w;
         w = h;
         h = temp;
      }

      double scaleX = goalWidth  / w;
      double scaleY = goalHeight / h;

      double translateX = pageTranslateX;
      double translateY = pageTranslateY;

      // use larger scale, center in other direction
      if (scaleX > scaleY) {
         scaleY = scaleX;
         translateY += (goalHeight - h*scaleY) / 2; // subtracting, actually
      } else {
         scaleX = scaleY;
         translateX += (goalWidth  - w*scaleX) / 2;
      }

      if (flip) {
         translateX += w*scaleX;
         scaleX = -scaleX;
      }

      AffineTransform transform = new AffineTransform();
      transform.translate(translateX,translateY);
      transform.scale(scaleX,scaleY);
      if (rotation != null) transform.concatenate(rotation);
      // the order seems odd, but as Graphics.transform explains,
      // the way transform works is last-specified-first-applied.

      return transform;
   }

   public void setLogReceived() {
      logReceived = true;
   }

   public void setTileParameters(boolean tileEnable, int tilePixels) {
      this.tileEnable = tileEnable;
      this.tilePixels = tilePixels;
   }

// --- implementation of Printable ---

   public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {

      if (pageIndex < 0 || pageIndex >= imageLoader.getImageCount()) return NO_SUCH_PAGE;

      if (logReceived && pageIndex == 0) {

         // assume format is same for all pages, if multiple,
         // but I want to see the values from both the
         // initial setup call and the actual printing call.

         Log.log(Level.FINE,this,"i1",new Object[] { Print.format(pageFormat) });
      }

      Graphics2D g = (Graphics2D) graphics;

      // RenderingHints.KEY_ANTIALIASING only applies to shape boundaries, so,
      // at most to the edges of the image rectangle, maybe not even that.
      // it's slightly academic since we're rotating by a multiple of 90 degrees,
      // and even more academic since the image edges should be off the paper.

      // RenderingHints.KEY_INTERPOLATION?  I tried all the different options
      // and found no difference in times with the Kodak, which we're probably using,
      // so probably it's just ignored.  that being the case, take my best guess for
      // the one we really want.  this is consistent with UploadTransform.
      // there's also no difference in the print quality as far as I can tell.
      //
      g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);

      try {

         BufferedImage image = imageLoader.getImage(pageIndex);
         // for multi-image items this is inefficient, loading each image
         // once for the setup call and once for the actual printing call,
         // but it's the right way.  the images can be large and we don't
         // want to have two in memory at once.  the important thing is,
         // in the very common single-image case we only load the image one time.

         boolean tile = false;
         if (tileEnable) {

            int w = image.getWidth();
            int h = image.getHeight();

            tile = (w > tilePixels || h > tilePixels) && (w > 1) && (h > 1);
            // sizes must be at least 2 for tile math to work.
            // no worries about tilePixels, it's validated to be large.
         }

         AffineTransform transform = getTransform(image);

         if (tile) { // the new way
            tileDrawImage(g,image,transform,tilePixels);
         } else { // the old way
            g.drawImage(image,transform,null);
         }

      } catch (Throwable t) {
         throw (PrinterException) new PrinterException(Text.get(this,"e1")).initCause(t);
      }
      // this has been observed to throw java.lang.InternalError (at Meijer,
      // when printing canvas prints).  the Print class would catch it,
      // but I want to convert it to PrinterException instead in hopes that
      // the layers in between will handle it better.
      //
      // Caused by: java.lang.InternalError: Problem in WPrinterJob_drawDIBImage
      //    at sun.awt.windows.WPrinterJob.drawDIBImage(Native Method)
      //    at sun.awt.windows.WPrinterJob.drawDIBImage(Unknown Source)
      //    at sun.awt.windows.WPathGraphics.drawImageToPlatform(Unknown Source)
      //    at sun.print.PathGraphics.drawImage(Unknown Source)
      //
      // this draws the complete image, even the parts that are out of bounds,
      // but it keeps the logic simpler and doesn't seem to cause any trouble.
      // also, all the drawImage functions that clip the source image take ints,
      // so there's no guarantee we could get the exact aspect ratio we want.

      return PAGE_EXISTS;
   }

// --- tile functions ---

   // the problem this solves is that LC thought it was sending images to HP
   // successfully, but the output was all black.  we don't understand why
   // this fixes it, since some sizes fail and some larger sizes work, but it does.
   //
   // of course this is like what UploadTransform does, but there are differences:
   //
   // * here we only get one try.
   //
   // * we still read the entire image all at once.  it would be great to read
   //   piece by piece, but the read wasn't the problem, and changing it would
   //   add a lot more complication.
   //
   // * in UploadTransform it was OK to use the same width / height for all fragments,
   //   but here the getSubimage call would complain.

   private static void tileDrawImage(Graphics2D g, BufferedImage image, AffineTransform transform, int tilePixels) {

      int w0 = image.getWidth();
      int h0 = image.getHeight();

      int nx = getTileCount(w0,tilePixels);
      int ny = getTileCount(h0,tilePixels);
      // tilePixels isn't used after this, everything just
      // depends on nx and ny

      // equalize the tile sizes now that we know how many
      int w1 = getTileSize(w0,nx);
      int h1 = getTileSize(h0,ny);

      AffineTransform temp = new AffineTransform();

      for (int ix=0; ix<nx; ix++) {
         for (int iy=0; iy<ny; iy++) {

            int x = ix*(w1-1);
            int y = iy*(h1-1);

            int w = (x+w1 > w0) ? w0-x : w1;
            int h = (y+h1 > h0) ? h0-y : h1;

            BufferedImage subimage = image.getSubimage(x,y,w,h);

            temp.setTransform(transform);
            temp.translate(x,y);
            // subimages always have zero corner coordinates,
            // so we have to translate to get them in the right place.
            // use a temp object instead of translate / undo
            // to guarantee that there's no FP hysteresis over time.

            g.drawImage(subimage,temp,null);
         }
      }
   }

   // these both come from the equation for 1-pixel overlap,
   // which is, tileCount * (tilePixels-1) >= (size-1)
   // the second one is just UploadTransform.getFragmentSize.

   private static int getTileCount(int size, int tilePixels) {
      return (int) Math.ceil((size-1)/(double) (tilePixels-1));
   }

   private static int getTileSize(int size, int tileCount) {
      return 1 + (int) Math.ceil((size-1)/(double) tileCount);
   }

}

