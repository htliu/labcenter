/*
 * JobHistoryDialog.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Pop;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.gui.ButtonHelper;
import com.lifepics.neuron.gui.Graphic;
import com.lifepics.neuron.gui.GridColumn;
import com.lifepics.neuron.gui.MinimumSize;
import com.lifepics.neuron.gui.ViewHelper;
import com.lifepics.neuron.meta.CompoundComparator;
import com.lifepics.neuron.meta.ReverseComparator;
import com.lifepics.neuron.meta.Selector;
import com.lifepics.neuron.table.View;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A dialog for viewing and manipulating jobs.
 */

public class JobHistoryDialog extends JFrame {

// --- fields ---

   private JobManager jobManager;

// --- construction ---

   public JobHistoryDialog(Frame owner, JobManager jobManager) {
      super(Text.get(JobHistoryDialog.class,"s1"));

      this.jobManager = jobManager;

      setDefaultCloseOperation(DISPOSE_ON_CLOSE);
      Graphic.setFrameIcon(this);

   // build table

      Selector selector = new Selector() { public boolean select(Object o) { return true; } };

      CompoundComparator comparator = new CompoundComparator(JobUtil.orderStatusHold,new ReverseComparator(JobUtil.orderJobID));
      //
      // eventually we'll probably want to add more order clauses.
      // the status-hold order comes first so that the errors,
      // which are what the user will want to see, are at the top.

      View view = jobManager.getJobTable().select(selector,comparator,true,true);

      GridColumn[] cols = new GridColumn[] {
            JobUtil.colJobID,
            JobUtil.colOrderID,
            JobUtil.colStatusHold,
            JobUtil.colRecmodDate
         };

      ViewHelper.DoubleClick doubleClick = seeErrorDC();
      ViewHelper viewHelper = new ViewHelper(view,Text.getInt(this,"n1"),cols,doubleClick);
      viewHelper.makeSortable(view);

   // build buttons

      JPanel buttons = new ButtonHelper()
         .addButton(Text.get(this,"s2"),viewHelper.getAdapter(holdJobs()))
         .addStrut()
         .addButton(Text.get(this,"s3"),viewHelper.getAdapter(releaseJobs()))
         .addStrut() // not enough columns, need more spacing
         .addGlue()
         .addStrut()
         .addButton(Text.get(this,"s4"),viewHelper.getAdapter(seeError()))
         .addStrut()
         .addGlue()
         .addStrut()
         .addButton(Text.get(this,"s7"),viewHelper.getAdapter(forgetJobs()))
         .addStrut()
         .addButton(Text.get(this,"s8"),viewHelper.getAdapter(purgeJobs()))
         .addStrut()
         .addButton(Text.get(this,"s5"),new ActionListener() { public void actionPerformed(ActionEvent e) { dispose(); } })
         .end();

   // finish up

      ButtonHelper.doLayout(this,viewHelper.getScrollPane(),buttons,null,/* fieldsBorder = */ false);
      pack();
      new MinimumSize(this);
      setLocationRelativeTo(owner);
   }

// --- bindings ---

   public ViewHelper.DoubleClick seeErrorDC() {
      return new ViewHelper.DoubleClick() { public void run(Object o) { doSeeError(o); } };
   }

   public ViewHelper.ButtonPress seeError() {
      return new ViewHelper.ButtonPress() { public void run(Object[] o) { doSeeError(o); } };
   }

   public ViewHelper.ButtonPress holdJobs() {
      return new ViewHelper.ButtonPress() { public void run(Object[] o) { jobManager.doHold(JobHistoryDialog.this,o); } };
   }

   public ViewHelper.ButtonPress releaseJobs() {
      return new ViewHelper.ButtonPress() { public void run(Object[] o) { jobManager.doRelease(JobHistoryDialog.this,o); } };
   }

   public ViewHelper.ButtonPress forgetJobs() {
      return new ViewHelper.ButtonPress() { public void run(Object[] o) { jobManager.doForget(JobHistoryDialog.this,o); } };
   }

   public ViewHelper.ButtonPress purgeJobs() {
      return new ViewHelper.ButtonPress() { public void run(Object[] o) { jobManager.doPurge(JobHistoryDialog.this,o); } };
   }

// --- user commands ---

   private void doSeeError(Object[] o) {
      if (o.length == 0) { JobManager.pleaseSelect(this,false); return; }
      doSeeError(o[0]);
   }
   private void doSeeError(Object o) {

      Job job = (Job) o; // no need to get latest copy, just use this one
      if (job.lastError == null) return;

      Pop.error(this,job.lastError,Text.get(this,"s6"));
   }

}

