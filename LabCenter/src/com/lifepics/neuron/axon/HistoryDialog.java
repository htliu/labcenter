/*
 * HistoryDialog.java
 */

package com.lifepics.neuron.axon;

import com.lifepics.neuron.core.Convert;
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
import java.util.LinkedList;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A dialog for viewing and manipulating the roll history.
 */

public class HistoryDialog extends JFrame implements Sortable {

// --- fields ---

   private RollManager rollManager;
   private RollManager.MemoryInterface mem;

   private SearchSelector selector; // this is not always the one that's in use
   private Comparator comparator;
   private ViewHelper viewHelper;

   private JButton buttonShowAll;

// --- construction ---

   public HistoryDialog(Frame owner, RollManager rollManager, RollManager.MemoryInterface mem) {
      super(Text.get(HistoryDialog.class,"s1"));

      this.rollManager = rollManager;
      this.mem = mem;

      setDefaultCloseOperation(DISPOSE_ON_CLOSE);
      Graphic.setFrameIcon(this);

   // build table

      selector = new SearchSelector(); // shows all
      comparator = RollUtil.orderRollID;

      View view = rollManager.getTable().select(selector,comparator,true,true);
         // need dynamic updates to see rebuild and purge work

      LinkedList cols = new LinkedList();

      cols.add(RollUtil.colRollID);

      if (ProMode.isProOld()) {
         cols.add(RollUtil.colAlbum);
         cols.add(RollUtil.colEventCode);
      } else if (ProMode.isProNew()) {
         cols.add(RollUtil.colEmail);
         cols.add(RollUtil.colAlbum);
         cols.add(RollUtil.colEventCode);
      } else if (ProMode.isMerged()) {
         cols.add(RollUtil.colEmail);
         cols.add(RollUtil.colSource);
         cols.add(RollUtil.colLocalImageID); // (*)
         cols.add(RollUtil.colAlbum);
         cols.add(RollUtil.colEventCode);
      } else {
         cols.add(RollUtil.colEmail);
         cols.add(RollUtil.colSource);
         cols.add(RollUtil.colLocalImageID); // (*)
      }
      // (*) not helpful unless this is a kiosk installation,
      // but it's not worth threading that down here.
      // also I'm going to make a search option that uses it.

      cols.add(RollUtil.colItemCount);
      cols.add(RollUtil.colStatusHold);
      cols.add(RollUtil.colRecmodDate);
      cols.add(RollUtil.colPurgeDate);
      cols.add(RollUtil.colRollDir);

      GridColumn[] colsArray = (GridColumn[]) cols.toArray(new GridColumn[cols.size()]);

      ViewHelper.DoubleClick doubleClick = openRoll();
      viewHelper = new ViewHelper(view,Text.getInt(this,"n1"),colsArray,doubleClick);
      viewHelper.makeSortable(this);

   // build buttons

      buttonShowAll = new JButton(Text.get(this,"s3"));
      buttonShowAll.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doShowAll(); } });
      buttonShowAll.setEnabled(false);

      JPanel buttons = new ButtonHelper()
         .addButton(Text.get(this,"s2"),new ActionListener() { public void actionPerformed(ActionEvent e) { doSearch(); } })
         .addStrut()
         .add(buttonShowAll)
         .addGlue()
         .addButton(Text.get(this,"s8"),viewHelper.getAdapter(recoverRolls()))
         .addStrut()
         .addButton(Text.get(this,"s4"),viewHelper.getAdapter(rebuildRolls()))
         .addStrut()
         .addButton(Text.get(this,"s5"),viewHelper.getAdapter(purgeRolls()))
         .addStrut()
         .addButton(Text.get(this,"s6"),new ActionListener() { public void actionPerformed(ActionEvent e) { dispose(); } })
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

   public ViewHelper.DoubleClick openRoll() {
      return new ViewHelper.DoubleClick() { public void run(Object o) { rollManager.doOpen(HistoryDialog.this,(Roll) o,mem); } };
   }

   public ViewHelper.ButtonPress recoverRolls() {
      return new ViewHelper.ButtonPress() { public void run(Object[] o) { rollManager.doRecover(HistoryDialog.this,o); } };
   }

   public ViewHelper.ButtonPress rebuildRolls() {
      return new ViewHelper.ButtonPress() { public void run(Object[] o) { rollManager.doRebuild(HistoryDialog.this,o); } };
   }

   public ViewHelper.ButtonPress purgeRolls() {
      return new ViewHelper.ButtonPress() { public void run(Object[] o) { rollManager.doPurge(HistoryDialog.this,o); } };
   }

// --- user commands ---

   private void requery(Selector selector) {
      View view = rollManager.getTable().select(selector,comparator,true,true);
      viewHelper.setView(view);
   }

   private void doSearch() {
      SearchSelector temp = selector.copy();
      if (new SearchDialog(this,temp).run()) {
         selector = temp;
         requery(selector);
         buttonShowAll.setEnabled(true);
      }
   }

   private void doShowAll() {
      // leave selector filled in with previous values, as a user convenience
      requery(new SearchSelector());
      buttonShowAll.setEnabled(false);
   }

}

