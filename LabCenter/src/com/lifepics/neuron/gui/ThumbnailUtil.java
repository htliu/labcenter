/*
 * ThumbnailUtil.java
 */

package com.lifepics.neuron.gui;

import com.lifepics.neuron.core.Pop;
import com.lifepics.neuron.core.Text;

import java.io.File;
import java.net.MalformedURLException;

import javax.swing.ImageIcon;
import java.awt.*;
import java.awt.geom.AffineTransform;

/**
 * Miscellaneous functions related to thumbnail processing.
 */

public class ThumbnailUtil {

// --- adapter interface ---

   /**
    * An interface for providing thumbnail information about arbitrary objects.
    */
   public interface Adapter {

      /**
       * Get the image file.
       * @return The file, or null if it's not there yet (e.g., isn't downloaded).
       */
      File getFile(Object o);

      /**
       * Get the name -- this is used as the window title
       * when the image is brought up in an image viewer.
       */
      String getName(Object o);

      /**
       * Get the rotation to apply, 0-3 for 0-270 degrees of CCW rotation.
       */
      int getRotation(Object o);
   }

// --- result interface ---

   /**
    * A result structure that can exist in three different states.
    *
    *   isDone   getImage   state
    *   ------   --------   -----
    *   false        null   not loaded yet
    *   false    not null   (n/a)
    *   true         null   error occurred
    *   true     not null   loaded
    */
   public interface Result {

      boolean isDone();
      Image getImage();
   }

// --- source interface ---

   public interface Source {

      /**
       * Get a thumbnail.  If the result is not done, the runnable will be called later,
       * unless cancel is called.  The runnable may still be called after that, however.
       * When the runnable is called, the original result object will have been changed!
       */
      Result get(File file, Runnable r);

      /**
       * Cancel all pending requests for thumbnails made via this instance.
       */
      void cancel();
   }

// --- drawing utilities ---

   public static AffineTransform[] getRotationTransform(int thumbWidth, int thumbHeight) {
      return new AffineTransform[] {
         Rotation.identity(),
         Rotation.center90 (thumbWidth,thumbHeight),
         Rotation.center180(thumbWidth,thumbHeight),
         Rotation.center270(thumbWidth,thumbHeight)
      };
   }

   public static void drawText(int thumbWidth, int thumbHeight, Graphics g1, Color color, String s) {

      g1.setColor(color);

      FontMetrics fm = g1.getFontMetrics();
      int ascent = fm.getAscent();
      int descent = fm.getDescent();
      int width = fm.stringWidth(s);

      int x = (thumbWidth - width) / 2;             // TW/2 - W/2
      int y = (thumbHeight + ascent - descent) / 2; // TH/2 - (A+D)/2 + A

      g1.drawString(s,x,y);
   }

   public static void drawImage(int thumbWidth, int thumbHeight, Graphics g1, Image image, AffineTransform transform) {

      int x = (thumbWidth  - image.getWidth (null)) / 2;
      int y = (thumbHeight - image.getHeight(null)) / 2;

      Graphics2D g2 = (Graphics2D) g1;

      AffineTransform saveTransform = g2.getTransform();
      g2.transform(transform);

      g2.drawImage(image,x,y,null);

      g2.setTransform(saveTransform);
   }

// --- viewing utilities ---

   public static void viewFullSize(Window owner, ThumbnailUtil.Adapter adapter, Object o) {

      File file = adapter.getFile(o);
      if (file == null) return; // order image not downloaded yet

      if (file.getName().toLowerCase().endsWith(".pdf")) { // like ImageUtil.getSuffix without exceptions

         // toURL produces "file:/C:/etc" instead of "file:///C:/etc", technically wrong,
         // but it's all we've got, and it seems to work in all the browsers I've tested

         try {
            Browser.launch(owner,file.toURL().toString(),Text.get(ThumbnailUtil.class,"s2"));
         } catch (MalformedURLException e) {
            // ignore
         }
         return;
      }

      try {

         Image image = ImageLoader.getFullSize(file); // BufferedImage, so fully loaded
         ImageIcon icon = new ImageIcon(image);
         ImageViewer.construct(owner,adapter.getName(o),icon,adapter.getRotation(o)).run();

      } catch (Exception e) {
         // user will usually know there's a problem because the thumbnail says N/A,
         // but maybe we'll run out of memory for the full image, or who knows what.
         // so, report it.
         Pop.error(owner,e,Text.get(ThumbnailUtil.class,"s1"));
      }
   }

}

