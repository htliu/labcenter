/*
 * ThreadDialog.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.core.Nullable;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.dendron.Queue;
import com.lifepics.neuron.dendron.QueueList;
import com.lifepics.neuron.dendron.ThreadDefinition;
import com.lifepics.neuron.gui.EditDialog;

import java.util.Iterator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A subdialog for editing thread settings (FormatThread multithreading).
 */

public class ThreadDialog extends EditDialog {

// --- fields ---

   private QueueList queueList;

   private int bw;
   private int bh;
   private ThreadDefinition[] thread;
   private Queue[] queue;
   private JRadioButton[][] button;

// --- construction ---

   public ThreadDialog(Dialog owner, QueueList queueList) {
      super(owner,Text.get(ThreadDialog.class,"s1"));
      this.queueList = queueList;
      construct(constructFields(),/* readonly = */ false);
   }

// --- methods ---

   private JPanel constructFields() {
      Iterator i;
      int x, y;

   // construct bw and bh

      bw = queueList.threads.size()+1;
      bh = queueList.queues .size();

   // construct arrays

      ThreadDefinition tDefault = new ThreadDefinition();
      tDefault.threadID = null;
      tDefault.threadName = ThreadDefinition.DEFAULT_THREAD_NAME;

      thread = new ThreadDefinition[bw];
      x = 0;
      thread[x++] = tDefault;
      i = queueList.threads.iterator();
      while (i.hasNext()) {
         thread[x++] = (ThreadDefinition) i.next();
      }

      queue = new Queue[bh];
      y = 0;
      i = queueList.queues.iterator();
      while (i.hasNext()) {
         queue[y++] = (Queue) i.next();
      }

   // construct buttons

      button = new JRadioButton[bw][bh];
      for (y=0; y<bh; y++) {
         ButtonGroup group = new ButtonGroup();
         for (x=0; x<bw; x++) {
            JRadioButton b = new JRadioButton();
            group.add(b);
            button[x][y] = b;
         }
      }

   // do the layout

      int hgap = Text.getInt(this,"w1");
      JPanel fields = new JPanel(new GridLayout(bh+1,bw+1,hgap,0)); // yes, h before w

      fields.add(new JLabel());
      for (x=0; x<bw; x++) {
         fields.add(new JLabel(thread[x].threadName));
      }

      for (y=0; y<bh; y++) {
         fields.add(new JLabel(queue[y].name));
         for (x=0; x<bw; x++) {
            fields.add(button[x][y]);
         }
      }

      return fields;
   }

   private int getIndexByThreadID(String threadID) {
      for (int x=0; x<bw; x++) {
         if (Nullable.equals(thread[x].threadID,threadID)) return x;
      }
      throw new IllegalArgumentException();
   }

   private int getSelectedIndex(int y) {
      for (int x=0; x<bw; x++) {
         if (button[x][y].isSelected()) return x;
      }
      throw new IllegalArgumentException();
   }

   protected void put() {
      for (int y=0; y<bh; y++) {
         int x = getIndexByThreadID(queue[y].threadID);
         button[x][y].setSelected(true);
      }
   }

   protected void getAndValidate() {
      for (int y=0; y<bh; y++) {
         int x = getSelectedIndex(y);
         queue[y].threadID = thread[x].threadID;
      }
      // can't fail, OK to write on real config
   }

}

