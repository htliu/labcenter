/*
 * BackupDialog.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Nullable;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.gui.EditDialog;
import com.lifepics.neuron.gui.GridBagHelper;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A dialog for controlling which queues are switched to backup.
 */

public class BackupDialog extends EditDialog {

// --- setup ---

   // these must come before construction in case there are no eligible queues

   private static HashSet calcHasBackup(QueueList queueList) {
      HashSet hasBackup = new HashSet();
      Iterator i = queueList.mappings.iterator();
      while (i.hasNext()) {
         QueueMapping m = (QueueMapping) i.next();
         if (m.backupQueueID != null) hasBackup.add(m.queueID);
      }
      return hasBackup;
   }

   private static LinkedList calcCanSwitch(QueueList queueList, HashSet hasBackup) {
      LinkedList canSwitch = new LinkedList();
      Iterator i = queueList.queues.iterator();
      while (i.hasNext()) {
         Queue q = (Queue) i.next();
         if (Nullable.nbToB(q.switchToBackup) || hasBackup.contains(q.queueID)) canSwitch.add(q);
      }
      return canSwitch;
   }

   public static Queue[] getQueueArray(QueueList queueList) {
      HashSet hasBackup = calcHasBackup(queueList);
      LinkedList canSwitch = calcCanSwitch(queueList,hasBackup);
      return (Queue[]) canSwitch.toArray(new Queue[canSwitch.size()]);
   }

// --- fields ---

   private Queue[] queue;

   private JRadioButton[] queueUp;
   private JRadioButton[] queueDown;

// --- construction ---

   public BackupDialog(Frame owner, Queue[] queue) {
      super(owner,Text.get(BackupDialog.class,"s1"));
      this.queue = queue;
      construct(constructFields(),/* readonly = */ false);
   }

// --- methods ---

   private JPanel constructFields() {

      String textUp   = Text.get(this,"s2");
      String textDown = Text.get(this,"s3");
      String noteUp   = Text.get(this,"s4");
      String noteDown = Text.get(this,"s5");

      int d1 = Text.getInt(this,"d1");

      JPanel fields = new JPanel(new GridLayout(0,1));

      queueUp   = new JRadioButton[queue.length];
      queueDown = new JRadioButton[queue.length];

      for (int i=0; i<queue.length; i++) { // allocation loop

         queueUp  [i] = new JRadioButton(textUp  );
         queueDown[i] = new JRadioButton(textDown);

         ButtonGroup group = new ButtonGroup();
         group.add(queueUp  [i]);
         group.add(queueDown[i]);
      }

      for (int i=0; i<queue.length; i++) { // layout loop

         JPanel panel = new JPanel();
         GridBagHelper helper = new GridBagHelper(panel);

         helper.add(0,0,Box.createHorizontalStrut(d1));

         helper.add(1,0,queueUp  [i]);
         helper.add(1,1,queueDown[i]);

         helper.add(2,0,new JLabel(noteUp  ));
         helper.add(2,1,new JLabel(noteDown));

         helper.add(3,0,Box.createHorizontalStrut(d1));

         panel.setBorder(BorderFactory.createTitledBorder(queue[i].name));
         fields.add(panel);
      }

      return fields;
   }

   protected void put() {
      for (int i=0; i<queue.length; i++) {
         boolean down = Nullable.nbToB(queue[i].switchToBackup);
         if (down) queueDown[i].setSelected(true);
         else      queueUp  [i].setSelected(true);
      }
   }

   protected void getAndValidate() throws ValidationException {
      for (int i=0; i<queue.length; i++) {
         queue[i].switchToBackup = Nullable.bToNb(queueDown[i].isSelected());
      }
      // no validation needed
   }

}

