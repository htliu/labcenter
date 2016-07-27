/*
 * AutoPrintDialog.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.gui.EditDialog;
import com.lifepics.neuron.gui.GridBagHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A dialog for controlling auto-print settings.
 */

public class AutoPrintDialog extends EditDialog {

// --- fields ---

   private JRadioButton autoPrintYes;
   private JRadioButton autoPrintNo;

   public boolean autoPrint; // input and output

// --- construction ---

   public AutoPrintDialog(Frame owner, boolean autoPrint) {
      super(owner,Text.get(AutoPrintDialog.class,"s1"));
      this.autoPrint = autoPrint;
      construct(constructFields(),/* readonly = */ false);
   }

// --- methods ---

   private JPanel constructFields() {

      autoPrintYes = new JRadioButton(Text.get(this,"s3"));
      autoPrintNo  = new JRadioButton(Text.get(this,"s4"));

      ButtonGroup group = new ButtonGroup();
      group.add(autoPrintYes);
      group.add(autoPrintNo );

      int d1 = Text.getInt(this,"d1");

      JPanel fields = new JPanel(new GridLayout(0,1));
      // keep this layout feature from BackupDialog in case we want to allow per-queue control someday

      JPanel panel = new JPanel();
      GridBagHelper helper = new GridBagHelper(panel);

      helper.add(0,0,Box.createHorizontalStrut(d1));

      helper.add(1,0,autoPrintYes);
      helper.add(1,1,autoPrintNo );

      helper.add(2,0,new JLabel(Text.get(this,"s5")));
      helper.add(2,1,new JLabel(Text.get(this,"s6")));

      helper.add(3,0,Box.createHorizontalStrut(d1));

      panel.setBorder(BorderFactory.createTitledBorder(Text.get(this,"s2")));
      fields.add(panel);

      return fields;
   }

   protected void put() {
      if (autoPrint) autoPrintYes.setSelected(true);
      else           autoPrintNo .setSelected(true);
   }

   protected void getAndValidate() throws ValidationException {
      autoPrint = autoPrintYes.isSelected();
   }

}

