/*
 * Graphic.java
 */

package com.lifepics.neuron.gui;

import com.lifepics.neuron.misc.Resource;

import java.util.HashMap;

import java.awt.Dimension;
import java.awt.Image;
import javax.swing.ImageIcon;
import javax.swing.JFrame;

/**
 * A utility class for retrieving standard graphic elements.
 */

public class Graphic {

// --- key class ---

   private static class Key {

      private String name;
      private Dimension size; // nullable

      public Key(String name, Dimension size) {
         this.name = name;
         this.size = size;
      }

      public int hashCode() {
         int result = name.hashCode();
         if (size != null) result += size.hashCode();
         return result;
      }

      public boolean equals(Object o) {
         if ( ! (o instanceof Key) ) return false;
         Key k = (Key) o;

         if ( ! name.equals(k.name) ) return false;
         return (size == null) ? (k.size == null)
                               : size.equals(k.size);
      }
   }

// --- functions ---

   private static HashMap iconMap = new HashMap();

   public static ImageIcon getIcon(String name) {
      return getIcon(name,null);
   }
   public static ImageIcon getIcon(String name, Dimension size) {
      Key key = new Key(name,size);

      ImageIcon icon = (ImageIcon) iconMap.get(key);
      if (icon == null) {

         if (size == null) {
            icon = new ImageIcon(Resource.getResource(Graphic.class,name));
         } else {
            icon = getIcon(name,null); // recurse to cache original too
            Image image = icon.getImage();
            image = image.getScaledInstance(size.width,size.height,Image.SCALE_FAST);
            icon = new ImageIcon(image);
            //
            // getScaledInstance isn't the preferred way to scale things:
            //
            // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6196792
            // http://today.java.net/pub/a/today/2007/04/03/perils-of-image-getscaledinstance.html
            //
            // this isn't an industrial-strength part of the code, though,
            // so it'll be fine.  it's only used for a few tiny GUI icons.
         }
         iconMap.put(key,icon);
      }

      return icon;
   }

   public static ImageIcon getIcon(String name, int scaleHeight) {
      ImageIcon icon = getIcon(name,null);
      double scale = scaleHeight / (double) icon.getIconHeight();
      int scaleWidth = (int) (icon.getIconWidth() * scale);
      return getIcon(name,new Dimension(scaleWidth,scaleHeight));
   }

   public static void setFrameIcon(JFrame frame) {
      frame.setIconImage(getIcon("wave.gif").getImage());
   }

   // as far as I can see, you can't set the icon on a JDialog.
   // if the dialog is resizable, it uses the owner's icon;
   // if not, it doesn't; that doesn't seem to be configurable.

}

