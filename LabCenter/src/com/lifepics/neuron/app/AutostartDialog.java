/*
 * AutostartDialog.java
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
 * A dialog for editing the autostart setting.
 */

public class AutostartDialog extends EditDialog {

// --- fields ---

   public boolean autostart;

   public JRadioButton autostartYes;
   public JRadioButton autostartNo;

// --- construction ---

   public AutostartDialog(Frame owner, boolean autostart) {
      super(owner,Text.get(AutostartDialog.class,"s1"));

      this.autostart = autostart;

      construct(constructFields(),/* readonly = */ false);
   }

// --- methods ---

   private JPanel constructFields() {

      JPanel fields = new JPanel();
      GridBagHelper helper = new GridBagHelper(fields);

      autostartYes = new JRadioButton(Text.get(this,"s3"));
      autostartNo  = new JRadioButton(Text.get(this,"s4"));

      ButtonGroup group = new ButtonGroup();
      group.add(autostartYes);
      group.add(autostartNo);

      int y = 0;

      helper.add(0,y++,new JLabel(Text.get(this,"s2")));
      helper.add(0,y++,autostartYes);
      helper.add(0,y++,autostartNo);

      return fields;
   }

   protected void put() {
      if (autostart) autostartYes.setSelected(true);
      else           autostartNo .setSelected(true);
   }

   protected void getAndValidate() throws ValidationException {
      autostart = autostartYes.isSelected();
   }

}

