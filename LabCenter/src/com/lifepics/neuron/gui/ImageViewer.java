/*
 * ImageViewer.java
 */

package com.lifepics.neuron.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A minimal dialog for viewing an image.
 */

public class ImageViewer extends JDialog {

// --- construction ---

   public ImageViewer(Frame owner, String title, Icon icon, int rotation) {
      super(owner,title,/* modal = */ true);
      construct(icon,rotation);
   }

   public ImageViewer(Dialog owner, String title, Icon icon, int rotation) {
      super(owner,title,/* modal = */ true);
      construct(icon,rotation);
   }

   public static ImageViewer construct(Window owner, String title, Icon icon, int rotation) {
      if (owner instanceof Frame ) return new ImageViewer((Frame ) owner,title,icon,rotation);
      if (owner instanceof Dialog) return new ImageViewer((Dialog) owner,title,icon,rotation);
      throw new IllegalArgumentException();
   }

   private void construct(Icon icon, int rotation) {

      setDefaultCloseOperation(DISPOSE_ON_CLOSE);

   // build scroll pane

      JLabel label = new JLabel(new RotatedIcon(icon,rotation));
      JScrollPane scroll = new JScrollPane(label,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                 JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      getContentPane().add(scroll);

   // finish up

      ScrollUtil.setSizeToPreferred(scroll,this);
      setLocationRelativeTo(getOwner());
   }

// --- methods ---

   /**
    * Run the dialog.
    */
   public void run() {
      setVisible(true); // does the modal thing
   }

}

