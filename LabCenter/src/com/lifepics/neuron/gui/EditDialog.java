/*
 * EditDialog.java
 */

package com.lifepics.neuron.gui;

import com.lifepics.neuron.core.Pop;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A dialog superclass for editing things.
 */

public abstract class EditDialog extends JDialog {

// --- subclass hooks ---

   protected void setCustomSize() {}

   protected abstract void put();
   protected abstract void getAndValidate() throws ValidationException;

   protected boolean weakValidate() { return true; }

// --- fields ---

   private boolean result;

// --- construction ---

   public EditDialog(Frame owner, String title) {
      super(owner,title,/* modal = */ true);
   }

   public EditDialog(Dialog owner, String title) {
      super(owner,title,/* modal = */ true);
   }

   protected void construct(JComponent fields, boolean readonly) {
      construct(fields,readonly,/* resizable = */ false,/* altOK = */ false,/* disableOK = */ false);
   }
   protected void construct(JComponent fields, boolean readonly, boolean resizable) {
      construct(fields,readonly,resizable,/* altOK = */ false,/* disableOK = */ false);
   }
   protected void construct(JComponent fields, boolean readonly, boolean resizable, boolean altOK, boolean disableOK) {

      ButtonInfo bi = new ButtonInfo();
      if (readonly) {

         // here, OK button has cancel effect

         JButton buttonOK = new JButton(Text.get(EditDialog.class,"s4"));
         buttonOK.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
            doCancel();
         } });

         bi.buttons = ButtonHelper.makeButtons(buttonOK);
         bi.buttonDefault = buttonOK;

      } else {

         JButton buttonOK = new JButton(Text.get(EditDialog.class,altOK ? "s5" : "s1"));
         buttonOK.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
            doOK();
         } });
         if (disableOK) buttonOK.setEnabled(false); // just a more emphatic way of saying readonly

         JButton buttonCancel = new JButton(Text.get(EditDialog.class,"s2"));
         buttonCancel.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
            doCancel();
         } });

         bi.buttons = ButtonHelper.makeButtons(buttonOK,buttonCancel);
         bi.buttonDefault = buttonOK;
      }

      construct(fields,bi,resizable);
   }

   public static class ButtonInfo {
      public JPanel buttons;
      public JButton buttonDefault;
   }

   protected void construct(JComponent fields, ButtonInfo bi, boolean resizable) {

      setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
      addWindowListener(new WindowAdapter() { public void windowClosing(WindowEvent e) { doCancel(); } });

      setResizable(resizable);
      //
      // by the way, calling setResizable(false) makes the dialog icon go away.
      // there's nothing you can do about it, it's a design decision they made
      // in Java.  it seems like a bad syllogism to me:
      //
      // * dialogs shouldn't have icons
      // * dialogs shouldn't be resizable
      // * therefore, resizable dialogs should have icons
      //
      // bonus: dialogs default to being resizable.
      //
      // http://bugs.java.com/bugdatabase/view_bug.do?bug_id=4094035

      ButtonHelper.doLayout(this,fields,bi.buttons,bi.buttonDefault);
      pack(); // pack before put, so that contents don't affect layout
      if (resizable) new MinimumSize(this);
      setCustomSize();
      setLocationRelativeTo(getOwner());

      put();
   }

// --- methods ---

   protected void doOK() {
      try {
         getAndValidate();
      } catch (ValidationException e) {
         Pop.error(this,e,Text.get(EditDialog.class,"s3"));
         return; // do not dispose, continue editing
      }

      if ( ! weakValidate() ) return; // continue editing

      result = true;
      dispose();
   }

   protected void doCancel() {
      result = false;
      dispose();
   }

   /**
    * Run the dialog.
    */
   public boolean run() {
      setVisible(true); // does the modal thing
      return result;
   }

}

