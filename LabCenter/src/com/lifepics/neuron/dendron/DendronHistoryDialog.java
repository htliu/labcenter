/*
 * DendronHistoryDialog.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.gui.ButtonHelper;
import com.lifepics.neuron.gui.Graphic;
import com.lifepics.neuron.gui.GridColumn;
import com.lifepics.neuron.gui.MinimumSize;
import com.lifepics.neuron.gui.ViewHelper;
import com.lifepics.neuron.meta.Selector;
import com.lifepics.neuron.meta.Sortable;
import com.lifepics.neuron.table.View;

import java.util.Comparator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A dialog for viewing and manipulating the order history.
 */

public class DendronHistoryDialog extends JFrame implements Sortable {

// --- fields ---

   private OrderManager orderManager;

   private DendronSearchSelector selector; // this is not always the one that's in use
   private Comparator comparator;
   private ViewHelper viewHelper;

   private JButton buttonShowAll;

// --- construction ---

   public DendronHistoryDialog(Frame owner, OrderManager orderManager) {
      super(Text.get(DendronHistoryDialog.class,"s1"));

      this.orderManager = orderManager;

      setDefaultCloseOperation(DISPOSE_ON_CLOSE);
      Graphic.setFrameIcon(this);

   // build table

      selector = new DendronSearchSelector(); // shows all
      comparator = OrderUtil.orderOrderID;

      View view = orderManager.getTable().select(selector,comparator,true,true);

      GridColumn[] cols = new GridColumn[] {
            OrderUtil.colOrderID,
            OrderUtil.colName,
            OrderUtil.colEmail,
            OrderUtil.colItemCount,
            OrderUtil.colStatusHold,
            OrderUtil.colRecmodDate,
            OrderUtil.colPurgeDate,
            OrderUtil.colOrderDir
         };

      ViewHelper.DoubleClick doubleClick = openOrder();
      viewHelper = new ViewHelper(view,Text.getInt(this,"n1"),cols,doubleClick);
      viewHelper.makeSortable(this);
      viewHelper.colorize(new DueColorizer(null));

   // build buttons

      buttonShowAll = new JButton(Text.get(this,"s3"));
      buttonShowAll.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doShowAll(); } });
      buttonShowAll.setEnabled(false);

      JPanel buttons = new ButtonHelper()
         .addButton(Text.get(this,"s2"),new ActionListener() { public void actionPerformed(ActionEvent e) { doSearch(); } })
         .addStrut()
         .add(buttonShowAll)
         .addGlue()
         .addButton(Text.get(this,"s6"),viewHelper.getAdapter(viewInvoice()))
         .addStrut()
         .addButton(Text.get(this,"s7"),viewHelper.getAdapter(viewLabel()))
         .addGlue()
         .addButton(Text.get(this,"s5"),viewHelper.getAdapter(purgeOrders()))
         .addStrut()
         .addButton(Text.get(this,"s4"),new ActionListener() { public void actionPerformed(ActionEvent e) { dispose(); } })
         .end();

   // finish up

      ButtonHelper.doLayout(this,viewHelper.getScrollPane(),buttons,null,/* fieldsBorder = */ false);
      pack();
      new MinimumSize(this);
      setLocationRelativeTo(owner);
   }

// --- methods ---

   public void sort(Comparator comparator) {
      this.comparator = comparator;
      viewHelper.viewSort(comparator);

      // this indirection serves two purposes:
      // first, it lets us keep track of the current comparator,
      // so that we can use it when we requery;
      // second, it allows the view object to be changed (on requery).
   }

// --- bindings ---

   public ViewHelper.DoubleClick openOrder() {
      return new ViewHelper.DoubleClick() { public void run(Object o) { orderManager.doOpen(DendronHistoryDialog.this,o); } };
   }

   public ViewHelper.ButtonPress viewInvoice() {
      return new ViewHelper.ButtonPress() { public void run(Object[] o) { orderManager.doInvoice(DendronHistoryDialog.this,o); } };
   }

   public ViewHelper.ButtonPress viewLabel() {
      return new ViewHelper.ButtonPress() { public void run(Object[] o) { orderManager.doLabel(DendronHistoryDialog.this,o); } };
   }

   public ViewHelper.ButtonPress purgeOrders() {
      return new ViewHelper.ButtonPress() { public void run(Object[] o) { orderManager.doPurge(DendronHistoryDialog.this,o); } };
   }

// --- user commands ---

   private void requery(Selector selector) {
      View view = orderManager.getTable().select(selector,comparator,true,true);
      viewHelper.setView(view);
   }

   private void doSearch() {
      DendronSearchSelector temp = selector.copy();
      if (new DendronSearchDialog(this,temp).run()) {
         selector = temp;
         requery(selector);
         buttonShowAll.setEnabled(true);
      }
   }

   private void doShowAll() {
      // leave selector filled in with previous values, as a user convenience
      requery(new DendronSearchSelector());
      buttonShowAll.setEnabled(false);
   }

}

