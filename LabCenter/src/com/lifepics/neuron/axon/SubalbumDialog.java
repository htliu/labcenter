/*
 * SubalbumDialog.java
 */

package com.lifepics.neuron.axon;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.gui.EditDialog;
import com.lifepics.neuron.gui.Field;
import com.lifepics.neuron.gui.GridBagHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A helper dialog for the two "Create Subalbum" buttons.
 */

public class SubalbumDialog extends EditDialog {

// --- fields ---

   // these two are the results
   public String  r_name;
   public boolean r_createChild;

   private JTextField name;
   private JRadioButton createChild;
   private JRadioButton createSibling;

// --- construction ---

   public SubalbumDialog(Dialog owner, boolean childOnly) {
      super(owner,Text.get(SubalbumDialog.class,"s1"));

      construct(constructFields(childOnly),/* readonly = */ false);
   }

// --- methods ---

   private JPanel constructFields(boolean childOnly) {

   // fields

      name = new JTextField(Text.getInt(this,"w1"));

      createChild   = new JRadioButton(Text.get(this,"s4"));
      createSibling = new JRadioButton(Text.get(this,"s5"));

      ButtonGroup group = new ButtonGroup();
      group.add(createChild  );
      group.add(createSibling);

      if (childOnly) {
         createChild  .setSelected(true);
         createSibling.setEnabled(false);
         // leave createChild enabled, otherwise it doesn't read well
      } else {
         createSibling.setSelected(true);
      }

   // layout

      JPanel fields = new JPanel();
      GridBagHelper helper = new GridBagHelper(fields);

      int y = 0;

      helper.addSpan(0,y,2,new JLabel(Text.get(this,"s2") + ' '));
      helper.addSpan(2,y,3,name); // one col added to fix layout, as in ConfigDialog
      y++;

      helper.add(0,y,Box.createVerticalStrut(Text.getInt(this,"d1")));
      y++;

      helper.add(0,y,new JLabel(Text.get(this,"s3")));
      helper.addSpan(1,y,2,createChild);
      helper.add(3,y,new JLabel(Text.get(this,"s6")));
      y++;

      helper.addSpan(1,y,2,createSibling);
      y++;

      return fields;
   }

   protected void put() {
      // do nothing here, the only setup comes from childOnly, above
   }

   protected void getAndValidate() throws ValidationException {

      r_name = Field.get(name);
      r_createChild = createChild.isSelected();

      if (r_name.length() == 0) throw new ValidationException(Text.get(this,"e1"));
      // the user can make empty names by hand-editing later,
      // but let's not encourage it, it just gets validated at the end.
   }

}

