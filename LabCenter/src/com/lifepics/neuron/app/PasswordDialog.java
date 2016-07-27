/*
 * PasswordDialog.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.gui.EditDialog;
import com.lifepics.neuron.gui.Field;
import com.lifepics.neuron.gui.GridBagHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A dialog for entering passwords.
 */

public class PasswordDialog extends EditDialog {

// --- fields ---

   private String requiredPassword;

   private JPasswordField password;

// --- construction ---

   public PasswordDialog(Frame owner, String requiredPassword, boolean alert) {
      super(owner,Text.get(PasswordDialog.class,"s1"));

      this.requiredPassword = requiredPassword;

      construct(constructFields(alert),/* readonly = */ false);
   }

// --- methods ---

   private JPanel constructFields(boolean alert) {

      JPanel fields = new JPanel();
      GridBagHelper helper = new GridBagHelper(fields);

      password = new JPasswordField(Text.getInt(this,"w1"));

      if (alert) {

         JLabel label = new JLabel(Text.get(this,"s4"));
         label.setFont(label.getFont().deriveFont(Font.BOLD));

         helper.add(0,0,Box.createVerticalStrut(Text.getInt(this,"d2")));
         helper.addCenter(0,1,label);
         helper.add(0,2,Box.createVerticalStrut(Text.getInt(this,"d3")));
      }

      helper.add(0,3,new JLabel(Text.get(this,"s2")));
      helper.add(0,4,Box.createVerticalStrut(Text.getInt(this,"d1")));
      helper.add(0,5,password);

      return fields;
   }

   protected void put() {
      // put nothing, field starts out blank
   }

   protected void getAndValidate() throws ValidationException {
      if ( ! Field.get(password).equals(requiredPassword) ) throw new ValidationException(Text.get(this,"s3"));
   }

}

