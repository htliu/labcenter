/*
 * RotatedIcon.java
 */

package com.lifepics.neuron.gui;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import javax.swing.Icon;

/**
 * An icon that wraps another icon and displays it in rotated form.
 * This is different from what a thumbnail buffer does,
 * because here the icon size varies with the rotation.
 * So, I think the two aren't worth unifying, at least for now.
 */

public class RotatedIcon implements Icon {

   private Icon icon;
   private int width;
   private int height;
   private AffineTransform transform;

   public RotatedIcon(Icon icon, int rotation) {
      this.icon = icon;

      width  = icon.getIconWidth ();
      height = icon.getIconHeight();
      transform = Rotation.corner(rotation,width,height);

      if (Rotation.isOdd(rotation)) {
         int temp = width;
         width    = height;
         height   = temp;
      }
   }

   public int getIconWidth () { return width;  }
   public int getIconHeight() { return height; }

   public void paintIcon(Component c, Graphics g, int x, int y) {

      Graphics2D g2 = (Graphics2D) g;

      AffineTransform saveTransform = g2.getTransform();
      g2.translate(x,y);
      g2.transform(transform);
      g2.translate(-x,-y);
      // the translations keep the point (x,y) fixed

      icon.paintIcon(c,g,x,y);
      // as long as the wrapped icon doesn't access any properties
      // of the component c that depend on coordinates, we're fine.

      g2.setTransform(saveTransform);
   }
}

