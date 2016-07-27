/*
 * ThumbnailGrid.java
 */

package com.lifepics.neuron.gui;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.table.View;
import com.lifepics.neuron.table.ViewListener;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TooManyListenersException;

import javax.swing.*;
import java.awt.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;

/**
 * A class that displays a grid of thumbnails.
 */

public class ThumbnailGrid extends JPanel implements Scrollable, ViewListener, MouseListener, MouseMotionListener, DropTargetListener {

// --- constants ---

   private static int margin   = Text.getInt(ThumbnailGrid.class,"d1");
   private static int frame    = Text.getInt(ThumbnailGrid.class,"d2");
   private static int labelGap = Text.getInt(ThumbnailGrid.class,"d3");

   private static Color colorMarginNormal   = Color.white;
   private static Color colorMarginSelected = new JTable().getSelectionBackground();
   private static Color colorFrame          = Color.black;
   private static Color colorLabelNormal    = Color.black;
   private static Color colorLabelSelected  = Color.white;

// --- fields ---

   private View view;
   private ThumbnailUtil.Adapter adapter;
   private ThumbnailUtil.Source source;
   private int rows; // only for scroll presentation size
   private int columns;
   private int thumbWidth;
   private int thumbHeight;

   private boolean dragEnabled;
   private MouseEvent dragEvent;
   private boolean dragResponse;

   private int frameWidth;
   private int frameHeight;
   private int cellWidth;
   private int cellHeight;
   private int labelCenterX;
   private int labelCenterY;
   private AffineTransform[] rotationTransform;

   private JPanel grid;

// --- construction ---

   public ThumbnailGrid(View view, ThumbnailUtil.Adapter adapter, ThumbnailUtil.Source source,
                        int rows, int columns,
                        int thumbWidth, int thumbHeight) {

      this.view = view;
      this.adapter = adapter;
      this.source = source;
      this.rows = rows;
      this.columns = columns;
      this.thumbWidth = thumbWidth;
      this.thumbHeight = thumbHeight;

      dragEnabled = false;
      // dragEvent null unless drag in progress
      // dragResponse scope is within listener

   // compute layout stuff

      FontMetrics fm = getFontMetrics(getFont());
      int ascent = fm.getAscent();
      int descent = fm.getDescent();

      frameWidth  = thumbWidth  + 2*frame;
      frameHeight = thumbHeight + 2*frame;

      cellWidth  = frameWidth  + 2*margin;
      cellHeight = frameHeight + 2*margin + labelGap + ascent + descent;

      labelCenterX = margin + frameWidth / 2;
      labelCenterY = margin + frameHeight + labelGap + ascent;

      rotationTransform = ThumbnailUtil.getRotationTransform(thumbWidth,thumbHeight);

   // finish up

      grid = new JPanel(new GridLayout(0,columns));

      Spacer spacer = new Spacer();

      grid  .setBackground(colorMarginNormal);
      spacer.setBackground(colorMarginNormal);
             setBackground(colorMarginNormal);
      // this last one is just a formality, it never appears.
      // I thought I should set it, since the viewport background
      // gets set to the same color (that also never appears).

      GridBagHelper helper = new GridBagHelper(this);
      helper.add(0,0,grid);
      helper.add(0,1,spacer);
      helper.setRowWeight(0,1);
      helper.setColumnWeight(0,1);

      // the point of all this is to make the whole viewport responsive to drag-drop,
      // or in particular, to make it possible to drop files into an empty dest grid.

      view.addListener(this);
      reportChange();
      // else no initialization
   }

   public void setDragEnabled(boolean dragEnabled) {
      this.dragEnabled = dragEnabled;
   }

   /**
    * If drag-drop is enabled, call this <i>after</i> the transfer handler is set.
    * Setting the transfer handler is what creates the drop target.
    * The units don't have transfer handlers, so they don't have drop targets,
    * that's why we must listen for drag events at the panel level.
    */
   public void enableDragResponse() {
      try {
         getDropTarget().addDropTargetListener(this);
      } catch (TooManyListenersException e) {
         // shouldn't happen, but if it does, oh well, no drag response
      }
   }

// --- spacer ---

   private class Spacer extends JPanel {

      public Dimension getMinimumSize() { return getPreferredSize(); }
      public Dimension getMaximumSize() { return getPreferredSize(); }

      public Dimension getPreferredSize() {

         int actualRows  = (view.size() + (columns-1)) / columns;
         int virtualRows = (actualRows >= rows) ? 0 : (rows - actualRows);

         return new Dimension(columns*cellWidth,virtualRows*cellHeight);

         // Q: why does the spacer have width as well as height,
         //    since the grid layout already sets it?
         // A: the grid layout has width zero if there are no components.
      }
   }

// --- implementation of Scrollable ---

   public Dimension getPreferredScrollableViewportSize() {
      return new Dimension(columns*cellWidth,rows*cellHeight);
   }

   public boolean getScrollableTracksViewportHeight() { return false; }
   public boolean getScrollableTracksViewportWidth()  { return false; }

   public int getScrollableUnitIncrement (Rectangle visibleRect, int orientation, int direction) {
      return (orientation == SwingConstants.HORIZONTAL) ? cellWidth : cellHeight;
   }
   public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
      return (orientation == SwingConstants.HORIZONTAL) ? cellWidth : cellHeight;
   }

// --- implementation of ViewListener ---

   public void reportInsert(int j, Object o) {
      grid.add(new Unit(view.get(j),/* preload = */ false),j);
      revalidate();
   }

   public void reportUpdate(int i, int j, Object o) {
      Component co = grid.getComponent(i);
      if (j == i) {
         co.repaint();
      } else {
         grid.remove(i);
         grid.add(co,j);
         revalidate();
      }
   }

   public void reportDelete(int i) {

      repaint(); // do this before removing any of the units,
      // otherwise if the view gets smaller, the area at the
      // end is not repainted, and shows nonexistent units.

      grid.remove(i);
      revalidate();
   }

   public void reportChange() {
      source.cancel(); // must come before unit construction

      repaint(); // do this before removing any of the units,
      // otherwise if the view gets smaller, the area at the
      // end is not repainted, and shows nonexistent units.

      int visible = rows*columns;

      grid.removeAll();
      for (int i=0; i<view.size(); i++) {
         grid.add(new Unit(view.get(i),/* preload = */ (i < visible)));
      }
      revalidate();
   }

// --- drawing unit ---

   private static final String textLoading = Text.get(ThumbnailGrid.class,"s1");
   private static final String textError   = Text.get(ThumbnailGrid.class,"s2");
   //
   // right now these happen to be the same as in Thumbnail,
   // but in the future, we might want them to be different.

   private class Unit extends JPanel implements Runnable {

      private Object o;
      private ThumbnailUtil.Result result;
      private boolean selected;
      private boolean saveSelected;

      public Unit(Object o, boolean preload) {
         this.o = o;
         if (preload) load(); // else result starts out null, see (*)
         selected = false;

         setOpaque(true);

         Dimension size = new Dimension(cellWidth,cellHeight);
         setMinimumSize(size);
         setMaximumSize(size);
         setPreferredSize(size);

         addMouseListener(ThumbnailGrid.this);
         addMouseMotionListener(ThumbnailGrid.this);
      }

      private void load() {
         result = source.get(adapter.getFile(o),this);
      }

      public Object getObject() {
         return o;
      }

      public boolean isSelected() {
         return selected;
      }

      public void setSelected(boolean selected) {
         if (selected != this.selected) {
            this.selected = selected;
            repaint();
         }
      }

      public void saveState() { saveSelected = selected; }
      public void restoreState() { setSelected(saveSelected); }

      public void run() {
         repaint();
         // result now has final value
      }

      protected void paintComponent(Graphics g) {
         Color saveColor = g.getColor();

         // the font is already set to component's font
         // rely on double-buffering to prevent flicker

      // frame

         g.setColor(selected ? colorMarginSelected : colorMarginNormal);
         g.fillRect(0,0,cellWidth,cellHeight);

         g.setColor(colorFrame);
         g.drawRect(margin,margin,thumbWidth+1,thumbHeight+1);

         if (selected) {
            g.setColor(colorMarginNormal);
            g.fillRect(margin+1,margin+1,thumbWidth,thumbHeight);
         }

      // image

         int border = margin + frame;
         g.translate(border,border);

         if (result == null) load(); // (*)
         //
         // units off screen are not painted right away,
         // so by delaying the load call until now,
         // we can load just the units that we actually need.
         // however ... for whatever reason,
         // painting occurs from last to first, and it looks
         // weird to see the images load in that order.
         // as a compromise, I'll make the initially-visible
         // ones preload, so they'll appear in correct order.

         if ( ! result.isDone() ) {
            ThumbnailUtil.drawText(thumbWidth,thumbHeight,g,getForeground(),textLoading);
         } else {
            Image image = result.getImage();
            if (image == null) {
               ThumbnailUtil.drawText(thumbWidth,thumbHeight,g,getForeground(),textError);
            } else {
               ThumbnailUtil.drawImage(thumbWidth,thumbHeight,g,image,rotationTransform[adapter.getRotation(o)]);
            }
         }

         g.translate(-border,-border);

      // label

         String label = adapter.getName(o);

         FontMetrics fm = g.getFontMetrics();
         int width = fm.stringWidth(label);

         if (width > frameWidth) {

            // assume dots by themselves will fit
            String dots = Text.get(ThumbnailGrid.class,"s3");
            width = fm.stringWidth(dots);

            int i;
            for (i=0; i<label.length(); i++) {
               int charw = fm.charWidth(label.charAt(i));
               width += charw;
               if (width > frameWidth) { width -= charw; break; }
            }

            label = label.substring(0,i) + dots;
         }

         g.setColor(selected ? colorLabelSelected : colorLabelNormal);
         g.drawString(label,labelCenterX - width/2,labelCenterY);

      // done

         g.setColor(saveColor);
      }
   }

// --- implementation of MouseListener ---

   // some of this is in the next section

   public void mouseEntered(MouseEvent e) {}
   public void mouseMoved  (MouseEvent e) {}
   public void mouseExited (MouseEvent e) {}

   public void mouseClicked(MouseEvent e) {

      Unit unit = (Unit) e.getComponent();

      switch (e.getClickCount()) {
      case 1:
         if      (e.isShiftDown  ()) extend(unit);
         else if (e.isControlDown()) alter (unit);
         else                        select(unit);
         break;
      case 2:
         doubleClick(unit);
         break;
      }
      // else way too many clicks
   }

   /**
    * @param unit The unit to select, or null to deselect everything.
    */
   private void select(Unit unit) {
      Component[] c = grid.getComponents();
      for (int i=0; i<c.length; i++) {
         Unit u = (Unit) c[i];
         u.setSelected(u == unit); // doesn't change unless necessary
      }
   }

   private void alter(Unit unit) {
      unit.setSelected( ! unit.isSelected() );
   }

   private void extend(Unit unit) {
      if (unit.isSelected()) return; // nothing to do

   // scan to find the unit and any adjacent selected units

      int iUnit = -1;
      int iPrev = -1;
      int iNext = -1;

      Component[] c = grid.getComponents();
      for (int i=0; i<c.length; i++) {
         Unit u = (Unit) c[i];
         if (u == unit) {
            iUnit = i;
         } else if (u.isSelected()) {
            if (iUnit == -1) { // earlier selected unit
               iPrev = i;
            } else {             // later selected unit
               iNext = i;
               break; // no need to scan the rest
            }
         }
      }

   // decide what to do, assuming iUnit != -1

      int iFirst, iLast;

      if (iPrev != -1) { // an earlier selection, extend forward from there
         iFirst = iPrev+1;
         iLast  = iUnit;
      } else if (iNext != -1) { // no earlier selection, but a later, extend backward from there
         iFirst = iUnit;
         iLast  = iNext-1;
      } else { // no selection except maybe the unit itself, just select it
         iFirst = iUnit;
         iLast  = iUnit;
      }

      for (int i=iFirst; i<=iLast; i++) {
         Unit u = (Unit) c[i];
         u.setSelected(true);
      }
   }

   private void doubleClick(Unit unit) {
      ThumbnailUtil.viewFullSize((Window) this.getTopLevelAncestor(),adapter,unit.getObject());
      // not a great cast, top-level ancestor could in theory be an applet
   }

// --- drag initiation ---

   // all cribbed from Sun code, including the 5-pixel threshold.
   // the code ... there's some in the drag-drop tutorial,
   // or you can look at javax.swing.plaf.basic.BasicDragGestureRecognizer.

   public void mousePressed(MouseEvent e) {
      if ( ! dragEnabled ) return;

      Unit unit = (Unit) e.getComponent();
      if ( ! unit.isSelected() ) return;

      if (e.isShiftDown() || e.isControlDown()) return;

      dragEvent = e;
      e.consume();

      // if the user's clicked on a selected unit,
      // and there are no modifiers, then normally
      // the click would deselect everything else ...
      // not so useful.  so, this is a good chance
      // to capture and convert to a drag.
      //
      // actually, if a drag isn't started, the click
      // goes through, but it's still useful to block out
      // the shift and control cases, because there
      // the user is clearly trying to do something else.
   }

   public void mouseDragged(MouseEvent e) {
      if (dragEvent == null) return;

      e.consume();

      int dx = Math.abs(e.getX() - dragEvent.getX());
      int dy = Math.abs(e.getY() - dragEvent.getY());

      if (dx > 5 || dy > 5) {

         getTransferHandler().exportAsDrag(this,dragEvent,TransferHandler.MOVE);
         // use the panel as source, rather than the unit, not that it matters

         dragEvent = null; // we're done with it
      }
   }

   public void mouseReleased(MouseEvent e) {
      dragEvent = null;
   }

// --- drag response ---

   // adapted from javax.swing.plaf.basic.BasicDropTargetListener,
   // with a few pieces from the concrete subclass in BasicTreeUI.

   public void dragEnter(DropTargetDragEvent e) {
      dragResponse = getTransferHandler().canImport(this,e.getCurrentDataFlavors());
      if ( ! dragResponse ) return;

      // save state
      Component[] c = grid.getComponents();
      for (int i=0; i<c.length; i++) {
         ((Unit) c[i]).saveState();
      }
   }

   public void dragOver(DropTargetDragEvent e) {
      if ( ! dragResponse ) return;

      // select
      Component c = grid.getComponentAt(e.getLocation());
      if (c instanceof Unit) {
         select((Unit) c);
      } else {
         select(null); // so that drop will go at end
      }
   }

   public void dragExit(DropTargetEvent e) {
      if ( ! dragResponse ) return;

      // restore state
      Component[] c = grid.getComponents();
      for (int i=0; i<c.length; i++) {
         ((Unit) c[i]).restoreState();
      }
   }

   public void drop(DropTargetDropEvent e) {}
   public void dropActionChanged(DropTargetDragEvent e) {}

// --- selection functions ---

   public void clearSelection() {
      select(null);
   }

   public LinkedList getSelectedObjectsList() {
      LinkedList list = new LinkedList();

      Component[] c = grid.getComponents();
      for (int i=0; i<c.length; i++) {
         Unit u = (Unit) c[i];
         if (u.isSelected()) list.add(u.getObject());
      }

      return list;
   }

   /**
    * @return A list of Integer objects.
    */
   public LinkedList getSelectedIndicesList() {
      LinkedList list = new LinkedList();

      Component[] c = grid.getComponents();
      for (int i=0; i<c.length; i++) {
         Unit u = (Unit) c[i];
         if (u.isSelected()) list.add(new Integer(i));
      }

      return list;
   }

   public Object[] getSelectedObjects() {
      LinkedList list = getSelectedObjectsList();
      return list.toArray(new Object[list.size()]);
   }

   public int[] getSelectedIndices() {
      LinkedList list = getSelectedIndicesList();
      int[] array = new int[list.size()];

      Iterator i = list.iterator();
      int j = 0;
      while (i.hasNext()) array[j++] = ((Integer) i.next()).intValue();

      return array;
   }

}

