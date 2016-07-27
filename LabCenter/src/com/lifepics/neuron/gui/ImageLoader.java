/*
 * ImageLoader.java
 */

package com.lifepics.neuron.gui;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.misc.Exif;

import java.io.File;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;

/**
 * A utility class for loading full-size and thumbnail images.
 */

public class ImageLoader {

   /**
    * Get a full-size image from a file.
    */
   public static BufferedImage getFullSize(File src) throws Exception {

      ImageInputStream iis = null;
      ImageReader reader = null;
      try {

         String suffix = ImageUtil.getSuffix(src);
         reader = ImageUtil.getReader(suffix);

         iis = new FileImageInputStream(src);
         reader.setInput(iis);

         return reader.read(0);

      } catch (OutOfMemoryError e) {
         throw new Exception(Text.get(ImageLoader.class,"e1",new Object[] { src.getName() }));

      } finally {
         if (reader != null) reader.dispose();
         try {
            if (iis != null) iis.close();
         } catch (Exception e) {
            // ignore
         }
      }
   }

   /**
    * Get a thumbnail image from a file.  It may really be
    * a full-size image, so next you'll want to rescale it.
    */
   public static BufferedImage getThumbnail(File src) throws Exception {

      ImageInputStream iis = null;
      ImageReader reader = null;
      try {

         String suffix = ImageUtil.getSuffix(src);
         reader = ImageUtil.getReader(suffix);

         iis = new FileImageInputStream(src);
         reader.setInput(iis);

      // general cases

         if (reader.hasThumbnails(0)) return reader.readThumbnail(0,0);
         // as far as I know, this never happens,
         // but it could if someone ever created a JFIF thumbnail

      // special cases

         if (suffix.equals("tif") || suffix.equals("tiff")) {
            // for Exif-TIFF, image #1 is the thumbnail
            if (reader.getNumImages(true) > 1) return reader.read(1);
         }

         if (suffix.equals("jpg") || suffix.equals("jpeg")) {
            // for Exif-JPEG, use the custom reader code
            ImageInputStream iisThumbnail = null;
            try {
               iisThumbnail = Exif.getExifThumbnail(reader,0);
            } catch (Exception e) {
               // didn't work, fall through and try other things
            }
            if (iisThumbnail != null) { // (*)
               reader.reset(); // empirical, must come before close
               iis.close();
               iis = iisThumbnail; // so we remember to close it
               reader.setInput(iis);
               return reader.read(0);
            }
         }
         // in the block (*), there *is* a compressed Exif thumbnail,
         // no doubt about it.  so, we can proceed with reading that,
         // and if it errors out, show an error icon as our result.
         // we could fall back to reading the full image, but let's not,
         // at least for now.
         //
         // let's also not worry about the case where an Exif-JPEG
         // contains an uncompressed thumbnail, i.e., a TIFF image.
         // we really need to tap into that inside getExifThumbnail,
         // where we have the APP1 segment.  for now we'll let
         // getExifThumbnail throw an exception because compression
         // isn't set to JPEG.
         //
         // it's also an open question whether the 1.0 TIFF reader
         // can skip over the imageless 0th IFD to get to the TIFF
         // thumbnail in the 1st IFD.  I bet it can, but I have no
         // test data, so who knows.  see the comments at the start
         // of Exif.java for more info on JAI versions.
         //
         // minor detail: if we error out before saving iisThumbnail
         // into iis, it's no big deal; the thumbnail stream is just
         // a byte array, nothing that needs closing.

      // fall back to full size

         return reader.read(0);

      } catch (OutOfMemoryError e) {
         throw new Exception(Text.get(ImageLoader.class,"e2",new Object[] { src.getName() }));

      } finally {
         if (reader != null) reader.dispose();
         try {
            if (iis != null) iis.close();
         } catch (Exception e) {
            // ignore
         }
      }
   }

   /**
    * Rescale an image (to fit in a thumbnail display).
    */
   public static BufferedImage rescale(BufferedImage src, int thumbWidth, int thumbHeight) throws Exception {

      double xRatio = ((double) src.getWidth ()) / thumbWidth;
      double yRatio = ((double) src.getHeight()) / thumbHeight;

      // the ratios tell how much the image must be compressed to fit.
      // we must compress using the maximum ratio.
      // basically we are doing the following, but in a way that
      // avoids FP error on the dimension that limits the scale
      //
      // double ratio = Math.max(xRatio,yRatio);
      //
      // int w = (int) (src.getWidth () / ratio);
      // int h = (int) (src.getHeight() / ratio);

      int w, h;

      if (xRatio > yRatio) {
         w = thumbWidth;
         h = (int) (src.getHeight() / xRatio);
      } else {
         w = (int) (src.getWidth () / yRatio);
         h = thumbHeight;
      }

      BufferedImage dest;
      try {
         dest = new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);

      } catch (OutOfMemoryError e) {
         throw new Exception(Text.get(ImageLoader.class,"e3"));
      }
      // these are small, so this shouldn't happen much

      Graphics g = dest.getGraphics();
      g.drawImage(src,0,0,w,h,null);
      g.dispose();

      return dest; // and let src be cleaned up, usually
   }

// --- non-static code ---

   // this is basically a helper class for PrintScaledImage;
   // normally you should just use the static functions.
   // it holds one image at a time, a special kind of cache.

   private File[] srcArray;
   private int loadedIndex;
   private BufferedImage loadedImage;

   public ImageLoader(File[] srcArray) {
      this.srcArray = srcArray;
      unload();
   }

   public void unload() {
      loadedIndex = -1;
      loadedImage = null;
      // could be private, no reason for others to call this
   }

   public ImageLoader preload() throws Exception {
      getImage(0);
      return this;
      // mostly we want to do this, but let's not require it
   }

   public int getImageCount() {
      return srcArray.length;
   }

   public BufferedImage getImage(int index) throws Exception {
      if (index != loadedIndex) {

         // free up memory and get to valid state
         // in case the call to getFullSize fails
         unload();

         loadedImage = getFullSize(srcArray[index]);
         loadedIndex = index;
      }
      return loadedImage;
   }

}

