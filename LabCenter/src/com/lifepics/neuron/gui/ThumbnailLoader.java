/*
 * ThumbnailLoader.java
 */

package com.lifepics.neuron.gui;

import java.io.File;

import java.awt.EventQueue;
import java.awt.Image;

/**
 * A class that asynchronously loads and rescales an image.
 * It's a one-shot class ... it starts running when you construct it,
 * sends a notification when it's done what you asked for,
 * and that's that, you can't restart it or make it do anything else.
 * So, if you decide you want a different operation performed,
 * you should tell the existing loader to stop, and make a new one.
 */

public class ThumbnailLoader {

// --- fields ---

   private File file;
   private Image scaledImage;

// --- construction ---

   public ThumbnailLoader(File file, int thumbWidth, int thumbHeight, Runnable done) {

      this.file = file; // remember, just as a convenience for the caller

      try {
         scaledImage = ImageLoader.rescale(ImageLoader.getThumbnail(file),thumbWidth,thumbHeight);
      } catch (Exception e) {
         // discard and leave scaledImage null
      }

      EventQueue.invokeLater(done);
      //
      // the "load the image" part is no longer asynchronous, but the notification
      // needs to stay as it is, since callers rely on it not happening right away.
   }

// --- control interface ---

   /**
    * A convenience function that returns the associated file.
    */
   public File getFile() { return file; }

   /**
    * Stop asynchronous processing and cancel any future notifications.
    */
   public void stop() {}
   // this no longer does anything, but maybe we'll want the interface
   // again in the future, so let's keep it.

   /**
    * Check whether the image is usable, i.e., completely loaded with no errors.
    * You can call this before receiving the completed notification if you want.
    */
   public boolean isUsable() { return (scaledImage != null); }

   /**
    * Get the scaled image.  You should check isUsable=true before calling this.
    */
   public Image getScaledImage() { return scaledImage; }

}

