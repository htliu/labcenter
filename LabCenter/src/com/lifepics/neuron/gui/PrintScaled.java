/*
 * PrintScaled.java
 */

package com.lifepics.neuron.gui;

import java.awt.*;
import java.awt.print.*;

/**
 * An implementation of {@link Printable} for scaled objects (labels).
 */

public class PrintScaled implements Printable {

// --- fields ---

   // received fields
   private Component c;
   private int copies;

   // computed fields
   private double translateX, translateY;
   private double scale;
   private PrintFooter printFooter;

// --- construction ---

   // here we could just as well do the calculations inside print( ... ),
   // but I'll keep them here just to be consistent with PrintPaginated.

   public PrintScaled(Component c, int copies, PageFormat pageFormat, String footer) {

      this.c = c;
      this.copies = copies;

   // translate

      translateX = pageFormat.getImageableX(); // adjusted below
      translateY = pageFormat.getImageableY();

   // scale

      Dimension size = c.getPreferredSize();

      double goalWidth  = pageFormat.getImageableWidth();
      double goalHeight = pageFormat.getImageableHeight()-PrintFooter.getHeight(footer);

      // component coordinate x scale = graphics coordinate

      double scaleX = goalWidth  / size.width;
      double scaleY = goalHeight / size.height;

      // use smaller scale, center in other direction
      if (scaleX < scaleY) {
         scale = scaleX;
         translateY += (goalHeight - size.height*scale) / 2;
      } else {
         scale = scaleY;
         translateX += (goalWidth  - size.width *scale) / 2;
      }

   // footer

      printFooter = new PrintFooter(copies,footer,pageFormat);
   }

// --- implementation of Printable ---

   public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) {

      if (pageIndex < 0 || pageIndex >= copies) return NO_SUCH_PAGE;

      printFooter.print(graphics,pageIndex); // before we mess up the coordinates

   // set up graphics

      Graphics2D g = (Graphics2D) graphics;

      g.translate(translateX,translateY);

      // the component is in a scroll view, or unattached,
      // so we don't need to untranslate for its location

      g.scale(scale,scale);

   // draw

      c.print(g);

   // done

      return PAGE_EXISTS;
   }

}

