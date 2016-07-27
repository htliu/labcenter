/*
 * OverrideDialog.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.gui.EditDialog;
import com.lifepics.neuron.gui.Field;
import com.lifepics.neuron.gui.GridBagHelper;
import com.lifepics.neuron.gui.InitialFocus;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A dialog for entering item override information.
 * (Currently only the quantity can be overridden.)
 */

public class OverrideDialog extends EditDialog {

// --- fields ---

   public Integer overrideQuantity;

   private JRadioButton quantityNormal;
   private JRadioButton quantityOverride;
   private JTextField   quantityValue;

// --- construction ---

   /**
    * Unlike most other edit dialogs, this one doesn't modify a passed-in object,
    * you just run it and (if not canceled) read the results from public fields.
    */
   public OverrideDialog(Dialog owner) {
      super(owner,Text.get(OverrideDialog.class,"s1"));

      construct(constructFields(),/* readonly = */ false);
   }

// --- methods ---

   private JPanel constructFields() {

   // fields

      int w1 = Text.getInt(this,"w1");

      quantityNormal = new JRadioButton(Text.get(this,"s3"));

      quantityOverride = new JRadioButton();
      quantityOverride.setSelected(true);

      quantityValue = new JTextField(w1);

      ButtonGroup group = new ButtonGroup();
      group.add(quantityNormal);
      group.add(quantityOverride);

   // layout

      JPanel fields = new JPanel();
      GridBagHelper helper = new GridBagHelper(fields);

      helper.add(0,0,new JLabel(Text.get(this,"s2") + ' '));
      helper.add(1,0,quantityOverride);
      helper.add(2,0,quantityValue);

      helper.addSpan(1,1,2,quantityNormal);

   // make the value get the initial focus
   // this method is overkill, but oh well ...

      setFocusTraversalPolicy(new InitialFocus(quantityValue));

      return fields;
   }

   protected void put() {
      Field.put(quantityValue,Convert.fromInt(1));
   }

   protected void getAndValidate() throws ValidationException {

      if (quantityNormal.isSelected()) {
         overrideQuantity = null;
      } else {
         int quantity = Convert.toInt(Field.get(quantityValue));
         Order.validateQuantity(quantity);
         overrideQuantity = new Integer(quantity);
      }
   }

}

