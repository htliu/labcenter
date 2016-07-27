/*
 * PrintFooter.java
 */

package com.lifepics.neuron.gui;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;

import java.text.MessageFormat;

import java.awt.*;
import java.awt.print.PageFormat;

/**
 * A helper class for printing footers.
 */

public class PrintFooter { // could implement Printable, code is similar but not quite the same

// --- fields ---

   private int pageIndexMax;
   private String footer;
   private int footerX, footerY;

// --- construction ---

   public PrintFooter(int pageIndexMax, String footer, PageFormat pageFormat) {

      this.pageIndexMax = pageIndexMax;
      this.footer = footer;

      footerX = (int) (pageFormat.getImageableX() + pageFormat.getImageableWidth()/2 );
      footerY = (int) (pageFormat.getImageableY() + pageFormat.getImageableHeight()  );

      // could use double, but the documentation says something about a bug with drawString
   }

// --- methods ---

   // the units are points for both d1 and d2
   private static final Font footerFont   = new Font("Serif",Font.PLAIN,Text.getInt(PrintFooter.class,"d1"));
   private static final int  footerHeight = Text.getInt(PrintFooter.class,"d2");

   public static int getHeight() { return footerHeight; }
   public static int getHeight(String footer) { return (footer == null) ? 0 : footerHeight; }

   public void print(Graphics g, int pageIndex) {
      if (footer == null) return;

      g.setColor(Color.black);
      g.setFont(footerFont);

      String s = MessageFormat.format(footer,new Object[] { Convert.fromInt(pageIndex+1), Convert.fromInt(pageIndexMax) });

      FontMetrics fm = g.getFontMetrics();
      int width = fm.stringWidth(s);
      int descent = fm.getDescent();

      g.drawString(s,footerX - width/2,footerY - descent);
   }

}

