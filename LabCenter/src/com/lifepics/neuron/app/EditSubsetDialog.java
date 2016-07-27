/*
 * EditSubsetDialog.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.dendron.ProductCallback;
import com.lifepics.neuron.dendron.ProductConfig;
import com.lifepics.neuron.dendron.SKUComparator;
import com.lifepics.neuron.gui.EditDialog;
import com.lifepics.neuron.gui.GridBagHelper;
import com.lifepics.neuron.gui.GridColumn;
import com.lifepics.neuron.gui.GridUtil;
import com.lifepics.neuron.gui.IntervalField;
import com.lifepics.neuron.gui.ViewHelper;
import com.lifepics.neuron.meta.Accessor;
import com.lifepics.neuron.meta.EditAccessor;
import com.lifepics.neuron.meta.FieldComparator;
import com.lifepics.neuron.struct.SKU;
import com.lifepics.neuron.table.ListView;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A dialog for editing the SKU subset for notification.
 */

public class EditSubsetDialog extends EditDialog {

// --- fields ---

   private LinkedList skus;
   private LinkedList rows;
   private ViewHelper viewHelper;

// --- construction ---

   /**
    * @param productConfig The product definitions (not edited).
    * @param skus          The subset of SKUs      (    edited).
    */
   public EditSubsetDialog(Dialog owner, ProductConfig productConfig, LinkedList skus) {
      super(owner,Text.get(EditSubsetDialog.class,"s1"));

      // productConfig is only saved indirectly, in row objects
      this.skus = skus;

      // build a list of products that have due intervals
      rows = new LinkedList();
      productConfig.iterate(new ProductCallback() { public void f(SKU sku, String description, Long dueInterval) {
         if (dueInterval != null) rows.add(new Row(sku,description,dueInterval));
      } });
      Collections.sort(rows,new FieldComparator(skuAccessorRaw,SKUComparator.displayOrder));
      // the rows should mostly be in order, we just have to combine the old and new SKUs

      construct(constructFields(),/* readonly = */ false,/* resizable = */ true);
   }

// --- rows ---

   // these row objects are a lot like old-style products,
   // except the SKU is an object instead of a string.
   // having a checked flag makes everything a lot simpler.

   private static class Row {

      public boolean checked;
      public SKU sku;
      public String description;
      public Long dueInterval;

      public Row(SKU sku, String description, Long dueInterval) {
         this.checked = false;
         this.sku = sku;
         this.description = description;
         this.dueInterval = dueInterval;
      }
   }

   private static Row findRowBySKU(LinkedList rows, SKU sku) {
      Iterator i = rows.iterator();
      while (i.hasNext()) {
         Row row = (Row) i.next();
         if (row.sku.equals(sku)) return row;
      }
      return null;
   }

// --- columns ---

   private static GridColumn makeColumn(String suffix, Accessor accessor, boolean editable) {
      GridColumn col = new GridColumn(Text.get   (EditSubsetDialog.class,"n" + suffix),
                                      Text.getInt(EditSubsetDialog.class,"w" + suffix),accessor,null);
      if (editable) col.editable = true;
      return col;
   }

   private static Accessor notifyAccessor = new EditAccessor() {
      public Class getFieldClass() { return Boolean.class; }
      public Object get(Object o) { return new Boolean(((Row) o).checked); }
      public void put(Object o, Object value) { ((Row) o).checked = ((Boolean) value).booleanValue(); }
   };

   private static Accessor skuAccessorRaw = new Accessor() {
      public Class getFieldClass() { return SKU.class; }
      public Object get(Object o) { return ((Row) o).sku; }
   };

   private static Accessor skuAccessor = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return ((Row) o).sku.toString(); }
   };

   private static Accessor descriptionAccessor = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return ((Row) o).description; }
   };

   private static Accessor dueIntervalAccessor = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return IntervalField.format(((Row) o).dueInterval.longValue(),IntervalField.MINUTES,IntervalField.WEEKS); }
   };

// --- methods ---

   private JPanel constructFields() {

      GridColumn[] cols = new GridColumn[] {
            makeColumn("1",notifyAccessor,true),
            makeColumn("2",skuAccessor,false),
            makeColumn("3",descriptionAccessor,false),
            makeColumn("4",dueIntervalAccessor,false)
         };

      ListView view = new ListView(rows,null);
      viewHelper = new ViewHelper(view,Text.getInt(this,"c1"),cols,null);

      GridUtil.setShowGrid(viewHelper.getTable()); // different look for editable data

      JPanel fields = new JPanel();
      GridBagHelper helper = new GridBagHelper(fields);

      helper.add(0,0,viewHelper.getScrollPane(),GridBagHelper.fillBoth);

      helper.setRowWeight(0,1);
      helper.setColumnWeight(0,1);

      // the helper may look like overkill, but without it,
      // the window resize doesn't cause a grid resize.

      return fields;
   }

   protected void put() {

      Iterator i = skus.iterator();
      while (i.hasNext()) {
         SKU sku = (SKU) i.next();

         Row row = findRowBySKU(rows,sku);
         if (row != null) {
            row.checked = true;
         }
         // else the SKU is left over from an old product,
         // and we'll correctly drop it from the SKU list
         // when the user accepts the changes.
      }
   }

   protected void getAndValidate() {

      viewHelper.stopEditing(); // not necessary, since check boxes
      // don't go into editing mode, but it's definitely good form

      skus.clear();

      Iterator i = rows.iterator();
      while (i.hasNext()) {
         Row row = (Row) i.next();

         if (row.checked) skus.add(row.sku);
      }
   }

}

