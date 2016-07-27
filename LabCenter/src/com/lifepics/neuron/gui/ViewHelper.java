/*
 * ViewHelper.java
 */

package com.lifepics.neuron.gui;

import com.lifepics.neuron.meta.Sortable;
import com.lifepics.neuron.table.View;
import com.lifepics.neuron.table.ViewListener;

import java.util.Comparator;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.*;

/**
 * A class that helps show the objects in a {@link View}.
 * It only extends {@link JPanel} so that
 * the subclass {@link ViewPanel} can be a panel.
 */

public class ViewHelper extends JPanel implements GridUtil.ClickListener {

// --- callback interfaces ---

   public interface DoubleClick {
      void run(Object o);
   }

   public interface ButtonPress {
      void run(Object[] o);
   }

// --- fields ---

   private View view;
   private DoubleClick doubleClick;

   private Grid grid;
   private JTable table;
   private JScrollPane scroll;

// --- construction ---

   public ViewHelper(View view, int rows, GridColumn[] cols, DoubleClick doubleClick) {
      this(view,rows,cols,doubleClick,new Grid(view,cols));
   }
   public ViewHelper(View view, int rows, GridColumn[] cols, DoubleClick doubleClick, Grid grid) {

      this.view = view;
      this.doubleClick = doubleClick;

   // table

      this.grid = grid;

      table = new JTable(grid);
      GridUtil.setPreferredSize(table,grid,rows);

      scroll = new JScrollPane(table,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                     JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
      GridUtil.setProperties(table,scroll);

      if (doubleClick != null) GridUtil.addDoubleClickListener(table,this);

      // the scroll pane isn't added to anything yet
   }

   public JScrollPane getScrollPane() { return scroll; }

// --- colorization ---

   public interface Colorizer {
      Color get(Object o);
   }
   // basically just an accessor that returns a color

   private class ColorRenderer extends DefaultTableCellRenderer {
      private Colorizer colorizer;
      public ColorRenderer(Colorizer colorizer) { this.colorizer = colorizer; }

      public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
         setBackground(colorizer.get(view.get(row)));
         return super.getTableCellRendererComponent(table,value,isSelected,hasFocus,row,column);
      }
   }

   public void colorize(Colorizer colorizer) {
      table.setDefaultRenderer(Object.class,new ColorRenderer(colorizer));
      // note this doesn't work for non-string column classes
   }

// --- event handling ---

   public void click(int row) {
      doubleClick.run(view.get(row));
   }

   public ActionListener getAdapter(ButtonPress buttonPress) {
      return new Adapter(buttonPress);
   }

   private class Adapter implements ActionListener {

      private ButtonPress buttonPress;
      public Adapter(ButtonPress buttonPress) { this.buttonPress = buttonPress; }

      public void actionPerformed(ActionEvent e) {
         buttonPress.run(getSelectedObjects());
      }
   }

   public Object[] getSelectedObjects() {

      int[] sel = table.getSelectedRows();
      // rest works correctly even when sel.length == 0

      Object[] o = new Object[sel.length];
      for (int i=0; i<sel.length; i++) {
         o[i] = view.get(sel[i]);
      }

      return o;
   }

// --- accessors ---

   public JTable getTable() { return table; }

// --- semi-pass-through functions ---

   public GridUtil.Sorter makeSortable(Sortable sortable) { return GridUtil.makeSortable(table,grid,sortable); }
   public void setView(View view) { this.view = view; grid.setView(view); }

   public void setColumns(GridColumn[] columns) {
      grid.setColumns(columns);
      GridUtil.setPreferredWidths(table,grid);
   }

   /**
    * Dialogs with editable grids should call this at the start of getAndValidate,
    * and at the start of any program-controlled access (R/W) to the grid contents.
    */
   public void stopEditing() {
      stopEditing(table);
   }
   public static void stopEditing(JTable table) {
      CellEditor editor = table.getCellEditor();
      if (editor != null) editor.stopCellEditing();
      // without this, the data being edited does not go through
   }

// --- pass-through functions ---

   public void viewAddListener(ViewListener listener) { view.addListener(listener); }
   public void viewRemoveListener(ViewListener listener) { view.removeListener(listener); }
   public int viewSize() { return view.size(); }
   public Object viewGet(int i) { return view.get(i); }
   public void viewSort(Comparator comparator) { view.sort(comparator); }

   public void tableSelectAll() { table.selectAll(); }
   public int tableGetSelectedRow() { return table.getSelectedRow(); }
   public int tableGetSelectedColumn() { return table.getSelectedColumn(); }

// --- magic header ---

   public void fixHeaderBackground() {
      table.setTableHeader(new MagicHeader(table.getColumnModel(),table.getBackground()));
   }
   private static class MagicHeader extends JTableHeader {

      private Color color;
      public MagicHeader(TableColumnModel model, Color color) {
         super(model);
         this.color = color;
      }

      protected void paintComponent(Graphics g) {
         super.paintComponent(g);

         // by default, the header row background color is gray,
         // which looks OK if you think of the header row as a
         // fixed entity attached to the top of the scroll pane,
         // but I just don't like it.
         // unfortunately, if you change the background it also
         // changes the background of the column headers.
         // so, do all this work just to make it a little nicer.

         Rectangle r = getBounds();
         int w = columnModel.getTotalColumnWidth();
         if (r.width > w) {
            g.setColor(color);
            g.fillRect(r.x+w,r.y,r.width-w,r.height);
         }
      }
   }

}

