/*
 * Grid.java
 */

package com.lifepics.neuron.gui;

import com.lifepics.neuron.meta.EditAccessor;
import com.lifepics.neuron.table.View;
import com.lifepics.neuron.table.ViewListener;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.ListIterator;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

/**
 * An object that combines row information (the objects in a {@link View})
 * with column information (an array of {@link GridColumn}) to produce a {@link TableModel}.
 */

public class Grid implements TableModel, ViewListener {

// --- fields ---

   private View view;
   private GridColumn[] columns;

   private LinkedList listeners;

// --- construction ---

   public Grid(View view, GridColumn[] columns) {
      this.view = view;
      this.columns = columns;

      listeners = new LinkedList();

      view.addListener(this);
   }

   public void setView(View view) {

      // it looks like there might be a possible bug here, but really there isn't.
      // the idea is, there might be an update waiting in the event queue
      // when the view is changed, and the update might be applied to the new view.
      // however, that's wrong on two levels.
      // first, all we do here is send notifications, not apply updates,
      // so at worst we would send an invalid notification.
      // second, even that can't happen, because the connection between
      // the view and here takes place completely in the UI thread.
      // there might be a queued-up update, but by the time the old view
      // receives it, we will be removed from its listener list.

      this.view.removeListener(this);
      this.view = view;
      view.addListener(this);

      tableChanged(new TableModelEvent(this)); // complete change
   }

   public void setColumns(GridColumn[] columns) {
      this.columns = columns;
      tableChanged(new TableModelEvent(this,TableModelEvent.HEADER_ROW)); // complete structure change
   }

// --- listeners ---

   public void addTableModelListener(TableModelListener listener) {
      listeners.add(listener);
   }

   public void removeTableModelListener(TableModelListener listener) {
      listeners.remove(listener); // ignore result
   }

   private void tableChanged(TableModelEvent e) {
      ListIterator li = listeners.listIterator();
      while (li.hasNext()) {
         ((TableModelListener) li.next()).tableChanged(e);
      }
   }

// --- implementation of ViewListener ---

   /**
    * Report that an object has been inserted.
    */
   public void reportInsert(int j, Object o) {
      tableChanged(new TableModelEvent(this,j,j,TableModelEvent.ALL_COLUMNS,TableModelEvent.INSERT));
   }

   /**
    * Report that an object has been updated.
    */
   public void reportUpdate(int i, int j, Object o) {
      if (i == j) {
         tableChanged(new TableModelEvent(this,i)); // row update
      } else {
         tableChanged(new TableModelEvent(this,i,i,TableModelEvent.ALL_COLUMNS,TableModelEvent.DELETE));
         tableChanged(new TableModelEvent(this,j,j,TableModelEvent.ALL_COLUMNS,TableModelEvent.INSERT));

         // the idea is, sending two events is better than sending
         // one event that says all the intervening rows changed
      }
   }

   /**
    * Report that an object has been deleted.
    */
   public void reportDelete(int i) {
      tableChanged(new TableModelEvent(this,i,i,TableModelEvent.ALL_COLUMNS,TableModelEvent.DELETE));
   }

   /**
    * Report that the view has completely changed.
    */
   public void reportChange() {
      tableChanged(new TableModelEvent(this)); // complete change
   }

// --- rows ---

   public int getRowCount() {
      return view.size();
   }

// --- columns ---

   public int getColumnCount() {
      return columns.length;
   }

   public String getColumnName(int columnIndex) {
      return columns[columnIndex].name;
   }

   public int getColumnWidth(int columnIndex) {
      return columns[columnIndex].width;
   }

   public Class getColumnClass(int columnIndex) {
      return columns[columnIndex].accessor.getFieldClass();
   }

   public Comparator getColumnComparator(int columnIndex) {
      return columns[columnIndex].comparator;
   }

// --- cells ---

   public Object getValueAt(int rowIndex, int columnIndex) {
      return columns[columnIndex].accessor.get(view.get(rowIndex));
   }

   public boolean isCellEditable(int rowIndex, int columnIndex) {
      return columns[columnIndex].editable;
   }

   public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
      ((EditAccessor) columns[columnIndex].accessor).put(view.get(rowIndex),aValue);
   }

}

