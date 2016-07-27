/*
 * Thumbnail.java
 */

package com.lifepics.neuron.gui;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.table.View;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 * A class that displays a thumbnail version of an image file.
 */

public class Thumbnail extends JLabel implements PropertyChangeListener, Runnable {

   // this seems like a weird design to me now.  why store the state in the image buffer,
   // when we could just have some kind of loading-loaded-errored flag and draw the text
   // or image on the fly?  still, it's done and it works, so I won't mess with it.
   // maybe I thought rotation was expensive, and maybe it was, but not so much any more.
   // or, maybe I didn't know how easy it was to override paintComponent.
   // see the "image" part of ThumbnailGrid.Unit.paintComponent for what it might look like.

// --- fields ---

   private int thumbWidth;
   private int thumbHeight;

   private BufferedImage buffer;
   private Graphics graphics;
   private AffineTransform[] rotationTransform;

   private ThumbnailLoader loader; // null means blank thumbnail ...
   private int rotation;           // ... in which case rotation is irrelevant

// --- construction ---

   public Thumbnail(int thumbWidth, int thumbHeight) {

      this.thumbWidth  = thumbWidth;
      this.thumbHeight = thumbHeight;

      buffer = new BufferedImage(thumbWidth,thumbHeight,BufferedImage.TYPE_INT_RGB);
      graphics = buffer.getGraphics();
      // this isn't great ... the graphics object doesn't get disposed of promptly,
      // it just waits until garbage-collection time.  on the other hand, until now
      // I allocated one every time I drew, and also didn't dispose of it promptly.
      // so, this has to be an improvement.
      // now that there's just one object, we do need to be careful not to make any
      // permanent changes to the state .. but that's how it often is with graphics.
      erase();

      // precompute rotation transforms, since they depend only on the thumbnail size.
      // the behavior when width != height is to rotate about the center and clip the
      // edges.  that's not ideal, so I recommend not using both features at once.
      //
      rotationTransform = ThumbnailUtil.getRotationTransform(thumbWidth,thumbHeight);

      // loader and rotation start out blank

      setIcon(new ImageIcon(buffer));
      setBorder(BorderFactory.createLoweredBevelBorder());
   }

// --- drawing routines ---

   private void erase() {
      graphics.setColor(getBackground());
      graphics.fillRect(0,0,thumbWidth,thumbHeight);
   }

   private static final String textLoading = Text.get(Thumbnail.class,"s1");
   private static final String textError   = Text.get(Thumbnail.class,"s2");

   private void drawLoading() { drawText(textLoading); }
   private void drawError  () { drawText(textError  ); }

   private void drawText(String s) {
      erase();
      ThumbnailUtil.drawText (thumbWidth,thumbHeight,graphics,getForeground(),s);
   }

   private void drawImage() { // caller must check the loader is usable
      erase();
      ThumbnailUtil.drawImage(thumbWidth,thumbHeight,graphics,loader.getScaledImage(),rotationTransform[rotation]);
   }

// --- ThumbnailLoader callback ---

   public void run() {

      // about setting the loader to null ... if the synchronization
      // in ThumbnailLoader is correct, as I think it is, then the
      // finish function, which runs the runnable, can never be called
      // after stop has been called.  however ... it runs the runnable
      // using EventQueue.invokeLater, so actually this function can
      // run arbitrarily late.  maybe the loader will have been cleared
      // and set to null, or maybe it will be a different object.
      //
      // so, ideally, we should add code to verify that the loader
      // is the same, and ignore the call if not.  that's a pain,
      // though, so I'll take the easy way of just handling the case
      // where the loader is null, to avoid NullPointerException.
      //
      // the result?  if the notification is delayed until the loader
      // is null, we correctly ignore it.  if the notification is
      // so late that another loader has been set, there are two cases.
      // (1) the other loader is ready; we'll draw twice.
      // (2) the other loader is not ready; we'll draw an error
      //     briefly, then draw the image at next notification.

      if (loader == null) return;

      if (loader.isUsable()) drawImage(); else drawError();
      repaint();
   }

// --- methods ---

   /**
    * Clear the thumbnail display.
    */
   public void clear() {

      if (loader != null) {
         loader.stop();
         loader = null;
         erase();
         repaint();
      }
      // else already blank
   }

   /**
    * Show the given file in the thumbnail display.
    */
   public void show(File file) { show(file,/* rotation = */ 0); }
   public void show(File file, int rotation) {

      boolean newFile = (loader == null || ! file.equals(loader.getFile()));
      boolean newRotation = (rotation != this.rotation);

      this.rotation = rotation; // go ahead and set, don't need old value

      if (newFile) { // new file, make a new loader

         if (loader != null) {
            loader.stop();
            loader = null;
         }
         drawLoading(); // may be overwritten immediately, oh well
         repaint();
         loader = new ThumbnailLoader(file,thumbWidth,thumbHeight,this); // leads to run call

      } else if (newRotation && loader.isUsable()) { // same file, new rotation, loader usable

         drawImage();
         repaint();
         // n.b. we know loader isn't null because newFile is false
      }
      // else there are several cases.
      // if the rotation isn't new, everything is exactly the same, no need to do anything.
      // if the loader isn't usable, that means one of two things.
      // * it's still working, and the new rotation will apply automatically after it finishes
      // * it errored out, and we can't show the image at any rotation
   }

// --- file chooser accessory methods ---

   /**
    * Add the thumbnail display to a file chooser.<p>
    *
    * The implementation isn't perfect -- to stop thumbnail image processing,
    * you have to clear the thumbnail manually after the window has closed.
    */
   public void addTo(JFileChooser chooser, int margin) {

      JPanel panel = new JPanel();
      panel.setLayout(new BoxLayout(panel,BoxLayout.X_AXIS));
      panel.add(Box.createHorizontalStrut(margin));
      panel.add(this);

      // without the panel, the display is too close to the file area,
      // and also it is mis-sized, it expands to be non-rectangular

      chooser.setAccessory(panel);
      chooser.addPropertyChangeListener(this);
   }

   public void propertyChange(PropertyChangeEvent e) {
      if (e.getPropertyName().equals(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY)) {
         File file = (File) e.getNewValue();

         if (file != null) show(file);
         else              clear();
      }
   }

// --- table accessory methods ---

   /**
    * Bind the thumbnail to a JTable so that it displays the current selection.<p>
    *
    * The implementation isn't perfect -- to stop thumbnail image processing,
    * you have to clear the thumbnail manually after the window has closed.
    * Also, because of how GridUtil is set up, a multiple selection shows
    * the image for the first row ... but what method would be better, there?<p>
    *
    * The thumbnail used not to clear when you deselected rows programmatically,
    * but that should be fixed now that I'm using ExtendedClickListener.
    *
    * @return A runnable object that makes the thumbnail refresh (e.g., if rotation has changed).
    */
   public Runnable bindTo(Dialog owner, JTable table, View view, ThumbnailUtil.Adapter adapter) {
      return new Binder(owner,table,view,adapter);
   }

   // this has to be a whole class in order to remember all the variables we need
   private class Binder implements Runnable {

      private Dialog owner;
      private JTable table;
      private View view;
      private ThumbnailUtil.Adapter adapter;

      private GridUtil.ClickListener ecl;

      public Binder(Dialog owner, JTable table, View viewx, ThumbnailUtil.Adapter adapter) {

         // purpose of the name "viewx" is to avoid ambiguity in nested classes

         this.owner = owner;
         this.table = table;
         this.view = viewx;
         this.adapter = adapter;

         ecl = new GridUtil.ExtendedClickListener() { public void click(int row) { doThumbnail(view.get(row)); } public void unclick() { doClear(); } };
         GridUtil.addSingleClickListener(table,ecl);
         GridUtil.addDoubleClickListener(table,new GridUtil.ClickListener() { public void click(int row) { doView(view.get(row)); } });
      }

      public void run() {
         GridUtil.onSingleClick(table,ecl);
      }

      private void doThumbnail(Object o) {

         File file = adapter.getFile(o);
         if (file != null) {
            /* thumbnail. */ show(file,adapter.getRotation(o));
         } else {
            /* thumbnail. */ clear(); // not downloaded yet
         }
      }

      private void doClear() {
         /* thumbnail. */ clear();
      }

      private void doView(Object o) {
         ThumbnailUtil.viewFullSize(owner,adapter,o);
      }
   }

}

