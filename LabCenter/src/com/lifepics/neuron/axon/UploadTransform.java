/*
 * UploadTransform.java
 */

package com.lifepics.neuron.axon;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Log;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.gui.ImageUtil;
import com.lifepics.neuron.gui.Rotation;
import com.lifepics.neuron.misc.FileUtil;

import java.io.File;
import java.util.Iterator;
import java.util.logging.Level;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import javax.imageio.*;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.FileImageOutputStream;

/**
 * A utility class that transforms image files prior to upload.
 */

public class UploadTransform {

// --- fields ---

   // allocated resources
   private FileImageInputStream  fiis;
   private FileImageOutputStream fios;
   private ImageReader reader;
   private ImageWriter writer;

   // arguments
   private TransformConfig config;
   private int rotation;
   // files not widely used, so not included

   // calculation results
   private int widthIn,  heightIn;
   private int widthOut, heightOut;

// --- main line ---

   /**
    * @return True if output was produced, false if not.
    *         False is not an error condition, it means that
    *         the file didn't need to be transformed.
    *         Actual errors are indicated by exceptions.
    */
   public static boolean transform(TransformConfig config, File input, int rotation, File output) throws Exception {
      UploadTransform ut = new UploadTransform(config,rotation);
      try {
         return ut.transform(input,output);
      } finally {
         ut.cleanup();
      }
   }

   private boolean transform(File input, File output) throws Exception {

   // first calculation

      String suffix = ImageUtil.getSuffix(input);
      // note, it's important that we determine the file type by looking
      // at the suffix rather than the data, since that's the only thing
      // that guarantees we won't upload files with non-JPEG names.

      boolean jpeg = suffix.equals("jpg") || suffix.equals("jpeg");
         // empirical fact, these are the ones imageio recognizes

   // first test

      // if we can tell that none of the transform conditions apply,
      // we can save some effort here by not reading the image file.
      // this does change behavior slightly, since a bad image file
      // that doesn't need to be transformed won't be detected.

      if (      jpeg
           && ! config.enableLimit
           && ! config.alwaysCompress
           &&   rotation == 0         ) return false;

   // second calculation

      createReader(input,suffix);

      calcWidthHeightIn();
      calcWidthHeightOut();

      double scale = config.enableLimit ? getScale() : 0;

   // second test

      // now we check the real conditions for transforming.
      // it's easier to understand if you switch the logic ...
      // we transform if
      //    (1) the file isn't a JPEG
      //    (2) limiting is enabled, and the file is large
      //    (3) "always compress" is on
      // or (4) the user has applied a rotation.

      if (      jpeg
           &&   scale == 0
           && ! config.alwaysCompress
           &&   rotation == 0         ) return false;

   // set up

      AffineTransformOp op = getTransformOp(scale);

   // normal transform

      try {
         write(output,normalTransform(op,rotation,scale));
         return true;
      } catch (Exception e) {

         // handle special errors that we want to recover from.  so far, they all come
         // from Java being too picky (or buggy) to read certain unusual JPEG formats.
         //
         // the actual observed message is "Invalid icc profile: bad sequence number".
         // apparently the colon isn't from my code, it's all on one exception object.
         //
         // the second one had message "Can't load standard profile: PYCC.pf",
         // and again, it was all on one exception.  Google suggests there are other
         // similar IllegalArgument ones, so generalize and handle them all at once.
         // I'd worry about being over-broad, but IllegalArgument should really never
         // happen ... it's a mistake on Sun's part that they used it for this.

         if (    (e instanceof IIOException             && e.getMessage().startsWith("Invalid icc profile"     ))
              || (e instanceof IllegalArgumentException && e.getMessage().toLowerCase().indexOf("profile") != -1) ) {

            // cf. last test, way below (*)
            if (      jpeg
                 &&   rotation == 0 ) { Log.log(Level.INFO,this,"i3",e); return false; }
         }

         throw e;

      } catch (OutOfMemoryError e) {
         // that didn't work, fall through and try fragmenting
      }
      // order doesn't matter: Exception and OutOfMemoryError are independent,
      // and you can't fall through from one exception block into another.

      // except in this case ... fragment transform doesn't work
      // on 1 x n images (because they produce fragment size 1,
      // and in that case the algorithm doesn't iterate even once).
      // this case is *not* covered by the minSize test,
      // since the other dimension might be over the minimum size.
      //
      if (widthIn == 1 || heightIn == 1) throw new Exception(Text.get(this,"e6"));

   // fragment transform

      // note that if a fragment transform is going to fail, it will
      // probably fail on the first fragment, because all the buffers
      // are allocated during that pass.  the only exception is the
      // actual write to the output file.  that uses more memory, but
      // also takes place after the transform is complete, so there
      // should be plenty of freed-up memory available.
      // the point is, we shouldn't waste a lot of time trying different
      // fragmentations, because ones that don't work will fail quickly.

      int a = widthIn;
      int b = heightIn;
      // basically, we start with these and divide by two each time

      int minSize = 100;
      // an arbitrary limit to prevent ridiculously small fragments

      for (int fragments=2; fragments<=8; fragments*=2) {

         if (a < minSize && b < minSize) break;
         // make sure at least one of the fragment sizes will change

         if (a >= minSize) a = getFragmentSize(widthIn, fragments);
         if (b >= minSize) b = getFragmentSize(heightIn,fragments);

         Log.log(Level.INFO,this,"i1",new Object[] { Convert.fromInt(fragments) });

         try {
            // clean up after previous failed attempt
            destroyWriter();
            resetReader(input,suffix);

            write(output,fragmentTransform(op,a,b));
            return true;
         } catch (OutOfMemoryError e) {
            // that didn't work, fall through and try again
         }
      }

      // if breaking into 8^2 = 64 fragments didn't help, probably
      // we're close to the memory limit for other reasons, and we
      // should just give up instead of trying over and over.
      // also, reading the image file more than 64 times starts to
      // become very time-consuming.

   // last test (*)

      // if we couldn't transform, there are some cases where it's OK
      // to go ahead and upload the original file after all.
      // rotation is required, obviously; scaling and compression are
      // pretty clearly optional; so the only question is non-JPEGs,
      // and the answer there is that the transform should be required.

      if (      jpeg
           &&   rotation == 0 ) { Log.log(Level.INFO,this,"i2"); return false; }

   // error out

      throw new Exception(Text.get(this,"e7"));
   }

   private void write(File output, BufferedImage imageOut) throws Exception {

      createWriter(output,"jpg");

      writer.write(null,new IIOImage(imageOut,null,null),getParam());
      fios.flush(); // ImageIO.write does this, seems reasonable
   }

   /**
    * Transform in the normal way, by reading the whole image.
    * This is very simple, but sometimes uses too much memory.
    */
   private BufferedImage normalTransform(AffineTransformOp op, int rotation, double scale) throws Exception {

      // rotation doesn't actually need to be an argument,
      // since it's already a member variable, but I like
      // having it there for clarity.

   // read

      BufferedImage imageIn = reader.read(0);

   // transform

      BufferedImage imageOut;
      if (rotation != 0 || scale != 0) {

         imageIn = colorConvert(imageIn,null); // result is really imageUse,
            // but it doesn't matter here, because we're done with imageIn

         imageOut = new BufferedImage(widthOut,heightOut,imageIn.getType());
         op.filter(imageIn,imageOut);

      } else if (hasTooManyComponents(imageIn)) {

         imageOut = colorConvert(imageIn,null);
         // the example to keep in mind here is CMYK TIFF, which needs to be
         // converted to three components so that we don't write a CMYK JPEG.
         // see (**) for more detail.

      } else {

         imageOut = imageIn;
         // no real transform needed, we're just rewriting the file
         // in a different format and/or with different compression
      }

      return imageOut;
   }

   /**
    * Transform by reading the image in a x b pixel fragments.
    */
   private BufferedImage fragmentTransform(AffineTransformOp op, int a, int b) throws Exception {

      // the coordinates increase by a-1 and b-1, not a and b,
      // so that there's a 1-pixel overlap between fragments.
      // otherwise, we might occasionally get floating-point error
      // that would leave a pixel not covered by any fragment.
      //
      // see below for more notes

      ImageReadParam param = reader.getDefaultReadParam();

      BufferedImage imageIn = null;
      BufferedImage imageUse = null;
      BufferedImage imageOut = null;

      // probably slightly more efficient to do rows together.
      for (int y=0; y<heightIn-1; y+=b-1) {
         for (int x=0; x<widthIn-1; x+=a-1) {

            param.setSourceRegion(new Rectangle(x,y,a,b)); // auto-clips at right and bottom
            param.setDestination(imageIn); // null the first time, otherwise reuse

            // we have seekForwardOnly set to true, but re-reads are allowed
            imageIn = reader.read(0,param);
            imageUse = colorConvert(imageIn,imageUse);

            if (imageOut == null) imageOut = new BufferedImage(widthOut,heightOut,imageUse.getType());
               // can't allocate until we know input type

            AffineTransform t = new AffineTransform(1,0,0,1,x,y); // translate
            t.preConcatenate(op.getTransform());
            new AffineTransformOp(t,op.getInterpolationType()).filter(imageUse,imageOut);
         }
      }

      return imageOut;
   }

   // because of floating-point error in the fragment coordinates,
   // the fragment transform probably won't produce the exact same
   // image as the normal transform.  it should be close, though.
   //
   // here's what I figured out about Java's bilinear interpolation.
   // think of the source image as looking like so, with the pixels
   // located at the center of their cells.
   //
   //    +---+---+---+---+
   //    | . | . | . | . |
   //    +---+---+---+---+
   //    | . | . | . | . |
   //    +---+---+---+---+
   //    | . | . | . | . |
   //    +---+---+---+---+
   //      a   b
   //
   // when you transform, you map this grid onto the dest image.
   // anything that's in the middle gets interpolated, no problem,
   // but anything in the 0.5-pixel border on each side doesn't
   // have anything to interpolate against, and is held constant.
   // so, if column a is more red, and column b is less red,
   // and you scale the source way up, you get this redness profile.
   //
   //      a   b
   //    ***   .
   //       *  .
   //        * .
   //         *.
   //          *
   //
   // so, because I just transform right into the output image,
   // these 0.5-pixel border regions get into the final output.
   // it's not ideal, because the interpolation is wrong,
   // but it's not terrible, because it's still pretty close,
   // and we're talking about less than one pixel at 300 DPI.
   //
   // one thing that helps is, the reason we need the fragment
   // transform is that we run out of memory on large images.
   // so, if the image is large, we'll be scaling down, and the
   // border region will be scaled from 0.5 pixels to even less.
   //
   // Q: how do you decide which dest pixels are inside
   //    the transformed grid?
   // A: you look at the pixel centers for that, too
   //
   // it's a little bit alarming that when we get to the edge
   // fragments, we don't erase or hide the old data in the
   // area that's outside the image, we just rely on the transform
   // to map it where it won't be seen.  but, this is roughly
   // the same problem as getting the normal transform to cover
   // the entire image reliably, and that does work.

   // if I understood how the tiling feature worked, I could
   // probably use that instead of this weird custom version.
   // or, if not, certainly the full JAI project covers it.

// --- allocated resources ---

   /**
    * Fills in the fiis and reader fields.
    */
   private void createReader(File input, String suffix) throws Exception {

      reader = ImageUtil.getReader(suffix);

      fiis = new FileImageInputStream(input);
      reader.setInput(fiis);
   }

   /**
    * Recover from an out-of-memory error during a previous pass.
    */
   private void resetReader(File input, String suffix) throws Exception {
      if (suffix.equals("png")) {
         destroyReader();
         createReader(input,suffix);
      } else {
         reader.reset();
         fiis.seek(0);
         reader.setInput(fiis);
      }

      // most readers seem to work if you do nothing here, but I think
      // it's reasonable to do some kind of reset just in case there's
      // garbage accumulating in the reader.
      //
      // also I really had to do something for the JPEG reader,
      // since if you don't reset it, you get this:
      // "Improper call to JPEG library in state 202"

      // unfortunately, there's no one thing you can do that works for
      // every reader.
      //
      // the reset doesn't work at all for the PNG reader.
      // apparently the reader flushes the input stream as it goes,
      // meaning that it's done with previous data, and then the
      // stream will throw an exception if you try to seek to zero.
      //
      // the destroy-create doesn't work well for the JPEG reader.
      // it must allocate large tables per reader, or something,
      // because the later fragmentation fallbacks don't work well.
   }

   /**
    * Fills in the fios and writer fields.
    */
   private void createWriter(File output, String suffix) throws Exception {

      Iterator i = ImageIO.getImageWritersBySuffix(suffix);
      if ( ! i.hasNext() ) throw new Exception(Text.get(this,"e3",new Object[] { suffix }));

      writer = (ImageWriter) i.next(); // take first writer

      FileUtil.makeNotExists(output);
      // else ImageWriter will overwrite without truncating,
      // leaving unpredictable junk at the end of the file.

      fios = new FileImageOutputStream(output);
      writer.setOutput(fios);
   }

   private void destroyWriter() {

      if (writer != null) {
         writer.dispose();
         writer = null;
      }

      if (fios != null) {
         try { fios.close(); } catch (Exception e) {}
         fios = null;
      }
   }

   private void destroyReader() {

      if (reader != null) {
         reader.dispose();
         reader = null;
      }

      if (fiis != null) {
         try { fiis.close(); } catch (Exception e) {}
         fiis = null;
      }
   }

   /**
    * This is the destructor for the allocated resources.
    */
   private void cleanup() {

      destroyWriter();
      destroyReader();

      // if you use ImageIO.createImageInput/OutputStream, as the tutorial suggests,
      // then you don't have access to the stream close operations without casting.
      // and, if you don't close the output stream, the output file isn't closed promptly,
      // and the upload thread sees size zero.
      // the input stream didn't cause any particular problem, closing is just good form.
   }

// --- other utilities ---

   private UploadTransform(TransformConfig config, int rotation) {
      this.config = config;
      this.rotation = rotation;
   }

   private void calcWidthHeightIn() throws Exception {

      widthIn  = reader.getWidth (0);
      heightIn = reader.getHeight(0);

      if (widthIn <= 0 || heightIn <= 0) throw new Exception(Text.get(this,"e2"));
         // make sure we won't be dividing by zero; also catch negative numbers
   }

   private void calcWidthHeightOut() {

      if (Rotation.isEven(rotation)) {
         widthOut  = widthIn;
         heightOut = heightIn;
      } else {
         widthOut  = heightIn;
         heightOut = widthIn;
      }
   }

   /**
    * Also adjusts widthOut and heightOut.
    */
   private double getScale() {

      int xLimit = config.getXLimit();
      int yLimit = config.getYLimit();

      // flip limits to whichever way matches image;
      // note square images and square limits never flip
      //
      if (    widthOut > heightOut && xLimit < yLimit
           || widthOut < heightOut && xLimit > yLimit ) {
         int temp;
         temp   = xLimit;
         xLimit = yLimit;
         yLimit = temp;
      }

      // what do we have to scale by to fit the limits?
      double xScale = ((double) xLimit) / widthOut;
      double yScale = ((double) yLimit) / heightOut;

      // it's the <i>larger</i> of the scale values that matters,
      // because the other dimension will be cropped.
      // for example, if the limits are square, and you have a source image
      // that's 2x1, you don't scale to fit inside the square,
      // you scale to fit the height, with the ends sticking out.

      // as in ThumbnailLoader, break out into cases to avoid FP error,
      // otherwise image would sometimes be 1 pixel smaller than limit.
      //
      // it's debatable whether we should use truncation or rounding for
      // the other coordinate.  I thought truncation would be better,
      // less likely to produce a black pixel line along the image edge,
      // but AffineTransformOp seems to work fine with rounding, and if
      // we truncate, we actually lose one row of information.
      // using ceil is definitely out, I tried it and saw a black line.

      if (xScale > yScale) {
         if (xScale < 1) {
            widthOut  = xLimit;
            heightOut = (int) Math.round(heightOut * xScale);
            return xScale;
         }
      } else {
         if (yScale < 1) {
            widthOut  = (int) Math.round(widthOut  * yScale);
            heightOut = yLimit;
            return yScale;
         }
      }

      return 0; // it fits, no need to scale
   }

   private static boolean hasTooManyComponents(BufferedImage imageIn) {
      return (imageIn.getColorModel().getColorSpace().getNumComponents() > 3);
   }

   /**
    * @param imageUse The old value of imageUse, possibly null.
    * @return         The new value of imageUse, never null.
    */
   private static BufferedImage colorConvert(BufferedImage imageIn, BufferedImage imageUse) {

      // see explanation at (**), below

      if (imageUse == null) {
         if (    imageIn.getType() == BufferedImage.TYPE_CUSTOM
              || hasTooManyComponents(imageIn) ) {

            imageUse = new BufferedImage(imageIn.getWidth(),imageIn.getHeight(),
                                         BufferedImage.TYPE_INT_RGB);
               // any image type with 24-bit color would work

         } else {
            imageUse = imageIn;
         }
      }

      if (imageUse != imageIn) {
         Graphics g = imageUse.getGraphics();
         g.drawImage(imageIn,0,0,null);
         g.dispose();
         // drawImage does color conversion ... it's very smart.
         // ColorConvertOp does the same thing, faster, but for
         // some unknown reason the quality is worse.
      }
      // else no color conversion needed

      return imageUse;
   }

   // (**) there are two cases where we need color conversion.
   //
   // (1) the TIFF case.  an affine transform requires two buffers
   // of the same type, but if the buffer type is TYPE_CUSTOM,
   // it's not easy to construct a matching one; probably not hard,
   // but I haven't done it.  TIFF seems to produce TYPE_CUSTOM.
   //
   // (2) the CMYK case.  the JPEG writer will happily write out
   // an image with four color components, but apparently that's
   // not a common format, and we don't want to produce it.
   // this has only been seen with CMYK TIFF files, but it could
   // be any image type, in theory.
   //
   // in fragmentTransform, we run everything through colorConvert,
   // so the logic in there is sufficient.  in normalTransform,
   // though, we want to skip the affine-transform step if possible,
   // so we need a special test for case (2) "too many components".
   // you might think that because colorConvert does nothing most of
   // the time, you could just call it in every case, but no ...
   // it's perfectly OK to send a three-component TYPE_CUSTOM image
   // straight to the JPEG writer without conversion.
   //
   // well, maybe not "perfectly OK", but it's worked so far.
   // the link at the bottom of the javax.imageio.plugins.jpeg package summary
   // has a good explanation of how the built-in JPEG reader and writer handle
   // different color spaces.  what I take away from that is, it's probably OK
   // to write out any three-component color space without conversion ...
   // the writer will include an ICC profile or do whatever else it needs to.

   private AffineTransformOp getTransformOp(double scale) {

      AffineTransform transform = Rotation.corner(rotation,widthIn,heightIn);
      int opType = AffineTransformOp.TYPE_NEAREST_NEIGHBOR;
      // nearest neighbor is best for pure rotation, since pixels should match up

      if (scale != 0) {
         transform.preConcatenate(AffineTransform.getScaleInstance(scale,scale));
         opType = AffineTransformOp.TYPE_BILINEAR;
      }

      return new AffineTransformOp(transform,opType);
   }

   private ImageWriteParam getParam() {

      // scale compression into range 0-1
      double c = ((double) (config.compression - TransformConfig.COMPRESSION_MIN))
            / (TransformConfig.COMPRESSION_MAX - TransformConfig.COMPRESSION_MIN);

      ImageWriteParam param = writer.getDefaultWriteParam();
      param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
      param.setCompressionType("JPEG");
      param.setCompressionQuality((float) c);

      return param;
   }

   private static int getFragmentSize(int size, int fragments) {
      return 1 + (int) Math.ceil((size-1)/(double) fragments);
   }
   // this function is well-behaved even for trivially small images.
   // some details, assuming fragments >= 2:
   //
   // size   return
   // ----   ------
   //   1        1
   //   2        2
   //   3        2
   //   4     3 for f=2,      2 otherwise
   //   5     3 for f=2 or 3, 2 otherwise
   //
   // the fragment transform doesn't run right with fragment size 1,
   // but that's not this function's fault.

}

