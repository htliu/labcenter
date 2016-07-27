/*
 * DendronSearchDialog.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.gui.EditDialog;
import com.lifepics.neuron.gui.Field;
import com.lifepics.neuron.gui.GridBagHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A dialog for editing search information.
 */

public class DendronSearchDialog extends EditDialog {

// --- fields ---

   private DendronSearchSelector selector;

   private JComboBox searchField;
   private JComboBox searchMode;
   private JTextField searchValue;

   private JTextField fromDate;
   private JTextField toDate;

// --- combo boxes ---

   private static Object[] searchFieldNames = new Object[] { Text.get(DendronSearchDialog.class,"f0"),
                                                             Text.get(DendronSearchDialog.class,"f1"),
                                                             Text.get(DendronSearchDialog.class,"f2"),
                                                             Text.get(DendronSearchDialog.class,"f3"),
                                                             Text.get(DendronSearchDialog.class,"f4")  };

   private static int[] searchFieldValues = new int[] { DendronSearchSelector.FIELD_NONE,
                                                        DendronSearchSelector.FIELD_ORDER_ID,
                                                        DendronSearchSelector.FIELD_NAME,
                                                        DendronSearchSelector.FIELD_EMAIL,
                                                        DendronSearchSelector.FIELD_PHONE };

   private static Object[] searchModeNames = new Object[] { Text.get(DendronSearchDialog.class,"m0"),
                                                            Text.get(DendronSearchDialog.class,"m1"),
                                                            Text.get(DendronSearchDialog.class,"m2"),
                                                            Text.get(DendronSearchDialog.class,"m3")  };

   private static int[] searchModeValues = new int[] { DendronSearchSelector.MODE_EQUALS,
                                                       DendronSearchSelector.MODE_CONTAINS,
                                                       DendronSearchSelector.MODE_STARTS_WITH,
                                                       DendronSearchSelector.MODE_ENDS_WITH };

// --- construction ---

   /**
    * @param selector An object that will be modified by the dialog.
    */
   public DendronSearchDialog(Frame owner, DendronSearchSelector selector) {
      super(owner,Text.get(DendronSearchDialog.class,"s1"));

      this.selector = selector;
      construct(constructFields(),/* readonly = */ false);
   }

// --- methods ---

   private JPanel constructFields() {

   // fields

      int w1 = Text.getInt(this,"w1");
      int w2 = Text.getInt(this,"w2");

      searchField = new JComboBox(searchFieldNames);
      searchMode = new JComboBox(searchModeNames);
      searchValue = new JTextField(w1);

      fromDate = new JTextField(w2);
      toDate = new JTextField(w2);

   // field-mode-value panel

      int d1 = Text.getInt(this,"d1");

      JPanel panel = new JPanel();
      panel.setLayout(new FlowLayout(FlowLayout.LEFT,d1,0));

      panel.add(searchField);
      panel.add(searchMode);
      panel.add(searchValue);

   // overall

      int d2 = Text.getInt(this,"d2");
      int d3 = Text.getInt(this,"d3");

      JPanel fields = new JPanel();
      GridBagHelper helper = new GridBagHelper(fields);

      helper.addSpan(0,0,4,new JLabel(Text.get(this,"s5")));

      helper.add(0,1,Box.createVerticalStrut(d2));
      helper.addSpan(0,2,4,panel);
      helper.add(0,3,Box.createVerticalStrut(d2));

      helper.add(1,4,Box.createHorizontalStrut(d3));
      helper.addCenter(2,4,new JLabel(Text.get(this,"s2")));

      helper.add(0,5,new JLabel(Text.get(this,"s3") + ' '));
      helper.add(2,5,fromDate);

      helper.add(0,6,new JLabel(Text.get(this,"s4") + ' '));
      helper.add(2,6,toDate);

      return fields;
   }

   protected void put() {

      Field.put(searchField,searchFieldValues,selector.searchField);
      Field.put(searchMode,searchModeValues,selector.searchMode);
      Field.put(searchValue,selector.searchValue);

      Field.put(fromDate,Convert.fromDateStart(selector.fromDate));
      Field.put(toDate,  Convert.fromDateEnd  (selector.toDate  ));
   }

   protected void getAndValidate() throws ValidationException {

      selector.searchField = Field.get(searchField,searchFieldValues);
      selector.searchMode = Field.get(searchMode,searchModeValues);
      selector.searchValue = Field.get(searchValue);

      selector.fromDate = Convert.toDateStart(Field.get(fromDate));
      selector.toDate   = Convert.toDateEnd  (Field.get(toDate  ));

      selector.validate();
   }

}

