/*
 * SubsystemDialog.java
 */

package com.lifepics.neuron.gui;

import com.lifepics.neuron.core.ChainedException;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.thread.SubsystemController;
import com.lifepics.neuron.thread.ThreadStopException;

import java.util.Iterator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A dialog for displaying errors from a subsystem.
 */

public class SubsystemDialog extends JDialog {

// --- fields ---

   private SubsystemController subsystem;

// --- construction ---

   public SubsystemDialog(Frame owner, SubsystemController subsystem) {
      super(owner,Text.get(SubsystemDialog.class,"s1"),/* modal = */ true);

      this.subsystem = subsystem;

      setDefaultCloseOperation(DISPOSE_ON_CLOSE);
      setResizable(false);

   // errors

      StringBuffer s = new StringBuffer();
      boolean first = true;

      Iterator i = subsystem.getErrors().iterator();
      while (i.hasNext()) {
         if (!first) s.append("\n\n");
         first = false;

         Throwable t = (Throwable) i.next();
         if (t instanceof ThreadStopException) {
            String hint = ((ThreadStopException) t).getHint();
            if (hint != null) {
               s.append(hint);
               s.append(Text.get(this,"s4"));
            }
         }
         s.append(ChainedException.format(t));
      }

      JComponent blob = Blob.makeScrollBlob(s.toString(),Text.getInt(this,"h1"),Text.getInt(this,"w1"));

   // buttons

      JButton buttonOK = new JButton(Text.get(this,"s2"));
      buttonOK.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { SubsystemDialog.this.subsystem.clearErrors(); dispose(); } });

      JButton buttonCancel = new JButton(Text.get(this,"s3"));
      buttonCancel.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { dispose(); } });

      JPanel buttons = ButtonHelper.makeButtons(buttonOK,buttonCancel);

   // finish up

      ButtonHelper.doLayout(this,blob,buttons,buttonCancel);
      pack();
      setLocationRelativeTo(getOwner());
   }

}

