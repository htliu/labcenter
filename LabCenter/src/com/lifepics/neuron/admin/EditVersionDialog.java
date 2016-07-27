/*
 * EditVersionDialog.java
 */

package com.lifepics.neuron.admin;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.gui.EditDialog;
import com.lifepics.neuron.gui.GridBagHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A dialog for editing the LabCenter version of an instance.
 */

public class EditVersionDialog extends EditDialog {

// --- fields ---

   private IDisplayVersion dvOld;
   private IDisplayVersion dvNew;
   private Object[] dvAll;
   private String defaultVersionName;

   private JComboBox version;

// --- construction ---

   public EditVersionDialog(Frame owner, IDisplayVersion dvOld, Object[] dvAll, String defaultVersionName) {
      super(owner,Text.get(EditVersionDialog.class,"s1"));

      this.dvOld = dvOld;
      this.dvAll = dvAll;
      this.defaultVersionName = defaultVersionName;

      construct(constructFields(),/* readonly = */ false);
   }

// --- methods ---

   private JPanel constructFields() {

      version = new JComboBox(dvAll);

      JPanel fields = new JPanel();
      GridBagHelper helper = new GridBagHelper(fields);

      helper.add(0,0,new JLabel(Text.get(this,"s2") + ' '));
      helper.add(1,0,version);

      helper.add(0,1,Box.createVerticalStrut(Text.getInt(this,"d1")));

      helper.addSpan(0,2,2,new JLabel(Text.get(this,"s3",new Object[] { defaultVersionName })));
      helper.addSpan(0,3,2,new JLabel(Text.get(this,"s4")));

      return fields;
   }

   protected void put() {
      version.setSelectedItem(dvOld);
   }

   protected void getAndValidate() throws ValidationException {
      dvNew = (IDisplayVersion) version.getSelectedItem();
      if (dvNew != dvOld && ! dvNew.isValid()) throw new ValidationException(Text.get(this,"e1"));
   }

   public IDisplayVersion getResult() {
      return dvNew;
   }

}

