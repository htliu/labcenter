/*
 * GridUtil.java
 */

package com.lifepics.neuron.gui;

import com.lifepics.neuron.meta.ReverseComparator;
import com.lifepics.neuron.meta.Sortable;

import java.util.Comparator;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.*;

/**
 * A utility class for dealing with the aspects of {@link JTable}
 * that aren't handled by {@link Grid} (which is a {@link javax.swing.table.TableModel}).
 */

public class GridUtil {

// --- properties ---

   public static void setProperties(JTable table, JScrollPane scrollPane) {
      table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
      table.setColumnSelectionAllowed(false);
      table.getTableHeader().setReorderingAllowed(false);
      table.setRowMargin(0);
      table.getColumnModel().setColumnMargin(0);
      table.setShowGrid(false);
      if (scrollPane != null) scrollPane.getViewport().setBackground(table.getBackground());
   }

   public static void setShowGrid(JTable table) {
      table.setRowMargin(1);
      table.getColumnModel().setColumnMargin(1);
      table.setShowGrid(true);
      // bring margins back to defaults here
   }

// --- column sizes ---

   private static class TextField extends JTextField {
      public int getColumnWidth() { return super.getColumnWidth(); } // evade protected attribute
   }

   public static int getScale() {
      TextField field = new TextField();
      return field.getColumnWidth();
   }

   public static int setPreferredWidths(JTable table, Grid grid) {

   // determine constants

      int scale = getScale();

      // I used to include row and column margins here,
      // but it turns out the margins are subtracted
      // from the preferred sizes, not added to them.
      // another interesting fact, if you set the column
      // margin to N, you get N/2 on each side, with the
      // extra going to the right margin (for the grid).

   // set up columns

      int widthTotal = 0;

      for (int i=0; i<grid.getColumnCount(); i++) {
         int width = grid.getColumnWidth(i) * scale;
         widthTotal += width;

         TableColumn tc = table.getColumnModel().getColumn(i);
         tc.setPreferredWidth(width);
      }

      return widthTotal;
   }

   public static void setPreferredSize(JTable table, Grid grid, int rows) {

      int widthTotal = setPreferredWidths(table,grid);
      int heightTotal = rows * table.getRowHeight();

      table.setPreferredScrollableViewportSize(new Dimension(widthTotal,heightTotal));
   }

// --- click interface ---

   public interface ClickListener {
      void click(int i);
   }

   public interface ExtendedClickListener extends ClickListener {
      void unclick();
   }

// --- single-clicks ---

   private static class SingleClickAdapter implements ListSelectionListener {

      private JTable table;
      private ClickListener listener;

      public SingleClickAdapter(JTable table, ClickListener listener) {
         this.table = table;
         this.listener = listener;
      }

      public void valueChanged(ListSelectionEvent e) {

         if (e.getValueIsAdjusting()) return;
         // this means another event will follow soon, so we should ignore it

         onSingleClick(table,listener);
      }
   }

   public static void onSingleClick(JTable table, ClickListener listener) {

      int row = table.getSelectionModel().getMinSelectionIndex();
      if (row != -1 && row < table.getRowCount()) { // (*)
         listener.click(row);
      } else if (listener instanceof ExtendedClickListener) {
         ((ExtendedClickListener) listener).unclick();
      }

      // (*) if you create a new roll and tab into the file list
      // before adding any files, you get a selection with row = 0.
      // that's why the second condition above is there.
   }

   public static void addSingleClickListener(JTable table, ClickListener listener) {
      ListSelectionModel model = table.getSelectionModel();
      model.addListSelectionListener(new SingleClickAdapter(table,listener));
   }

// --- double-clicks ---

   private static class DoubleClickAdapter extends MouseAdapter {

      private JTable table;
      private ClickListener listener;

      public DoubleClickAdapter(JTable table, ClickListener listener) {
         this.table = table;
         this.listener = listener;
      }

      public void mouseClicked(MouseEvent e) {
         if (e.getClickCount() == 2) {
            int row = table.rowAtPoint(e.getPoint());
            if (row != -1) {
               listener.click(row);
            }
         }
      }
   }

   public static void addDoubleClickListener(JTable table, ClickListener listener) {
      table.addMouseListener(new DoubleClickAdapter(table,listener));
   }

// --- header clicks ---

   private static class HeaderClickAdapter extends MouseAdapter {

      private JTable table;
      private ClickListener listener;

      public HeaderClickAdapter(JTable table, ClickListener listener) {
         this.table = table;
         this.listener = listener;
      }

      public void mouseClicked(MouseEvent e) {
         if (e.getClickCount() == 1) {
            int col = table.columnAtPoint(e.getPoint());

            // handle reordered columns, even though we don't use them
            col = table.convertColumnIndexToModel(col);

            if (col != -1) {
               listener.click(col);
            }
         }
      }
   }

   public static void addHeaderClickListener(JTable table, ClickListener listener) {
      table.getTableHeader().addMouseListener(new HeaderClickAdapter(table,listener));
   }

// --- sorting ---

   // nearly always, sortGrid will be constant.
   // it's useful only in the weird case where
   // you have two grids with one row model.

   public static class Sorter {

      private Sortable sortable;
      private Grid sortGrid;
      private int sortCol;
      private boolean sortAscending;

      public Sorter(Sortable sortable) {
         this.sortable = sortable;
         reset();
      }

      /**
       * Call this function if you've resorted the grid programmatically,
       * to reset the sorter state.  The only effect is to make a future
       * click on the old sortCol produce ascending rather than the
       * reverse of whatever it was before ... but that's still worthwhile.
       */
      public void reset() {
         sortGrid = null;
         sortCol = -1;
         sortAscending = true; // not that it matters
      }

      public void click(Grid grid, int col) {

      // figure out desired column and direction

         if (grid == sortGrid && col == sortCol) {
            sortAscending = ! sortAscending;
         } else {
            sortGrid = grid;
            sortCol = col;
            sortAscending = true;
         }

      // see if the column has a comparator

         Comparator comparator = sortGrid.getColumnComparator(sortCol);
         if (comparator == null) return; // leave previous sort in place

      // sort using the comparator or its reverse

         if ( ! sortAscending ) comparator = new ReverseComparator(comparator);
         sortable.sort(comparator);
      }
   }

   public static Sorter makeSortable(JTable table, Grid grid, Sortable sortable) {
      Sorter sorter = new Sorter(sortable);
      addToExistingSorter(sorter,table,grid);
      return sorter;
   }

   public static void addToExistingSorter(final Sorter sorter, JTable table, final Grid grid) {
      addHeaderClickListener(table,new ClickListener() { public void click(int col) { sorter.click(grid,col); } });
   }

}

