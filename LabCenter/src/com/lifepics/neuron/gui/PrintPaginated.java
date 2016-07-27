/*
 * PrintPaginated.java
 */

package com.lifepics.neuron.gui;

import com.lifepics.neuron.core.Text;

import java.util.LinkedList;
import java.util.ListIterator;

import java.awt.*;
import java.awt.print.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;

/**
 * An implementation of {@link Printable} for paginated objects (invoices).
 */

public class PrintPaginated implements Printable {

// --- fields ---

   // received fields
   private Component c;
   private int copies;

   // computed fields
   private double translateX, translateY;
   private double scale;
   private int c_width; // this and breaK are screen coordinates, so they can be ints
   private int[] breaK; // breaK.length = pages+1, with breaK[0] = 0 and breaK[pages] = c_height
   private int pages;
   private PrintFooter printFooter;

// --- construction ---

   public PrintPaginated(Component c, int copies, PageFormat pageFormat) {

      this.c = c;
      this.copies = copies;

   // translate

      // the PageFormat object is passed to the print function,
      // but I thought I'd cache these just so it's clear that
      // the values all come from the same object.

      translateX = pageFormat.getImageableX();
      translateY = pageFormat.getImageableY();

   // scale

      Dimension size = c.getPreferredSize();

      // this is what the coordinates will be multiplied by,
      // when the component is drawing itself in the window.
      //
      scale = pageFormat.getImageableWidth() / size.width;

      // so, this is the page height, in component terms
      //
      int pageHeight = (int) Math.floor( (pageFormat.getImageableHeight()-PrintFooter.getHeight()) / scale );

   // c_width

      c_width = size.width;

   // break array

      try {
         breaK = breakText((JTextComponent) c,size.height,pageHeight);
      } catch (Exception e) {
         breaK = breakGlob(c,size.height,pageHeight);
      }

      // the exception block handles BadLocationException (which can't happen)
      // and also the ClassCastException you get if the component isn't text.
      // in the latter case we want to fall back on the previous glob algorithm.

   // pages

      pages = breaK.length - 1;

   // footer

      printFooter = new PrintFooter(pages,Text.get(this,"s1"),pageFormat);
   }

// --- implementation of Printable ---

   public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) {

   // set pageIndex

      if (pageIndex < 0 || pageIndex >= pages*copies) return NO_SUCH_PAGE;

      pageIndex %= pages; // collated
      // pageIndex /= copies; // uncollated

      printFooter.print(graphics,pageIndex); // before we mess up the coordinates

   // set up graphics

      Graphics2D g = (Graphics2D) graphics;

      g.translate(translateX,translateY);

      // the component is in a scroll view, or unattached,
      // so we don't need to untranslate for its location

      g.scale(scale,scale);
      g.translate(0,-breaK[pageIndex]);

      g.setClip(0,breaK[pageIndex],c_width,breaK[pageIndex+1]-breaK[pageIndex]);
      // else the component would probably draw extra stuff

   // draw

      c.print(g);
      //
      // unfortunately, this draws the whole component,
      // not just the part that's in view,
      // but I can't see that there's anything I can do about that.
      // maybe it's smart enough to look at the clip rectangle.

   // done

      return PAGE_EXISTS;
   }

// --- page breaks ---

   // the c_height argument is a convenience, equals c.getPreferredSize().height

   private static int[] breakGlob(Component c, int c_height, int pageHeight) {

      int pages = (int) Math.ceil( c_height / (double) pageHeight );

      int[] breaK = new int[pages+1];

      for (int i=0; i<pages; i++) breaK[i] = i*pageHeight;
      breaK[pages] = c_height;

      return breaK;
   }

   private static int[] breakText(JTextComponent c, int c_height, int pageHeight) throws BadLocationException {

   // gather row information

      LinkedList rows = new LinkedList();
      int y = 0;
      int h = 0;

      int len = c.getDocument().getLength();
      for (int i=0; i<=len; i++) {
         Rectangle r = c.modelToView(i);

         // filter out duplicates -- we do this again later,
         // but it's good to do a quick fast test now,
         // because the duplicates are usually in one bunch.
         //
         if (r.y == y && r.height == h) continue;
         y = r.y;
         h = r.height;

         // ignore the initial entry with zero height (and any others)
         if (h <= 0) continue;

         addRow(rows,new Row(y,h));
      }

   // remove overlaps

      // at this point, each list entry represents a row that shouldn't be broken.
      // however, we can also think about the entries in another way.
      // if we assume (correctly) that JTextComponent doesn't make any whitespace
      // between rows, then the row starts are also the only possible break points.

      int base = 1; // if there's a row with y = 0 (or y < 0), this will remove it

      ListIterator li = rows.listIterator();
      while (li.hasNext()) {
         Row row = (Row) li.next();

         if (row.y >= base) { // valid break point, retain
            base = row.y + row.h;
         } else { // invalid, remove, possibly extend base
            li.remove();
            base = Math.max(base,row.y + row.h);
         }
      }

      // at this point the h values no longer matter

   // find breaks

      LinkedList breaks = new LinkedList();
      breaks.add(new Row(0,0));

      li = rows.listIterator();

      int max = pageHeight;
      while (max < c_height) {
         Row found = find(li,max);
         if (found == null) found = new Row(max,0); // force break
         breaks.add(found);
         max = found.y + pageHeight;
      }

      breaks.add(new Row(c_height,0));

   // construct array

      int[] breaK = new int[breaks.size()];
      int i = 0;

      li = breaks.listIterator();
      while (li.hasNext()) {
         breaK[i++] = ((Row) li.next()).y;
      }

      return breaK;
   }

// --- utilities for page breaks ---

   private static class Row {
      public int y;
      public int h;
      public Row(int y, int h) { this.y = y; this.h = h; }
   }

   private static void addRow(LinkedList list, Row add) {

      // in general, this insertion sort would be very inefficient,
      // but here the y values are pretty much monotone increasing.
      // that's why we scan from the end of the list.

      ListIterator li = list.listIterator(list.size());
      while (li.hasPrevious()) {
         Row row = (Row) li.previous();

         if (row.y >  add.y) continue;
         if (row.y == add.y) { // only allow one entry per y
            if (add.h > row.h) row.h = add.h; // take maximum
            return;
         } else {
            li.next();
            break; // found insertion point
         }
      }

      li.add(add);
   }

   /**
    * Find the row with the largest value of row.y <= max,
    * and leave the iterator just after that row.
    * If there's no suitable row, leave the iterator alone and return null.
    */
   private static Row find(ListIterator li, int max) {

      Row found = null;

      while (li.hasNext()) {
         Row row = (Row) li.next();

         if (row.y <= max) found = row;
         else { li.previous(); break; }
      }

      return found;
   }

}

