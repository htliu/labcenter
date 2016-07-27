/*
 * SearchDialog.java
 */

package com.lifepics.neuron.axon;

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

public class SearchDialog extends EditDialog {

// --- fields ---

   private SearchSelector selector;

   private JComboBox searchField;
   private JComboBox searchMode;
   private JTextField searchValue;

   private JTextField fromDate;
   private JTextField toDate;

// --- combo boxes ---

   private static String nameNone      = Text.get(SearchDialog.class,"f0");
   private static String nameRollID    = Text.get(SearchDialog.class,"f1");
   private static String nameEmail     = Text.get(SearchDialog.class,"f2");
   private static String nameAlbum     = Text.get(SearchDialog.class,"f3");
   private static String nameEventCode = Text.get(SearchDialog.class,"f4");
   private static String nameImageID   = Text.get(SearchDialog.class,"f5");

   private static Object[] searchFieldNames = new Object[] { nameNone,
                                                             nameRollID,
                                                             nameEmail,
                                                             nameImageID };

   private static int[] searchFieldValues = new int[] { SearchSelector.FIELD_NONE,
                                                        SearchSelector.FIELD_ROLL_ID,
                                                        SearchSelector.FIELD_EMAIL,
                                                        SearchSelector.FIELD_IMAGE_ID };

   private static Object[] searchFieldNamesProOld = new Object[] { nameNone,
                                                                   nameRollID,
                                                                   nameAlbum,
                                                                   nameEventCode };

   private static int[] searchFieldValuesProOld = new int[] { SearchSelector.FIELD_NONE,
                                                              SearchSelector.FIELD_ROLL_ID,
                                                              SearchSelector.FIELD_ALBUM,
                                                              SearchSelector.FIELD_EVENT_CODE };

   private static Object[] searchFieldNamesProNew = new Object[] { nameNone,
                                                                   nameRollID,
                                                                   nameEmail,
                                                                   nameAlbum,
                                                                   nameEventCode };

   private static int[] searchFieldValuesProNew = new int[] { SearchSelector.FIELD_NONE,
                                                              SearchSelector.FIELD_ROLL_ID,
                                                              SearchSelector.FIELD_EMAIL,
                                                              SearchSelector.FIELD_ALBUM,
                                                              SearchSelector.FIELD_EVENT_CODE };

   private static Object[] searchFieldNamesMerged = new Object[] { nameNone,
                                                                   nameRollID,
                                                                   nameEmail,
                                                                   nameImageID,
                                                                   nameAlbum,
                                                                   nameEventCode };

   private static int[] searchFieldValuesMerged = new int[] { SearchSelector.FIELD_NONE,
                                                              SearchSelector.FIELD_ROLL_ID,
                                                              SearchSelector.FIELD_EMAIL,
                                                              SearchSelector.FIELD_IMAGE_ID,
                                                              SearchSelector.FIELD_ALBUM,
                                                              SearchSelector.FIELD_EVENT_CODE };

   private static Object[] searchModeNames = new Object[] { Text.get(SearchDialog.class,"m0"),
                                                            Text.get(SearchDialog.class,"m1"),
                                                            Text.get(SearchDialog.class,"m2"),
                                                            Text.get(SearchDialog.class,"m3")  };

   private static int[] searchModeValues = new int[] { SearchSelector.MODE_EQUALS,
                                                       SearchSelector.MODE_CONTAINS,
                                                       SearchSelector.MODE_STARTS_WITH,
                                                       SearchSelector.MODE_ENDS_WITH };

   private static Object[] getSearchFieldNames() {
      if      (ProMode.isProOld()) return searchFieldNamesProOld;
      else if (ProMode.isProNew()) return searchFieldNamesProNew;
      else if (ProMode.isMerged()) return searchFieldNamesMerged;
      else                         return searchFieldNames;
   }

   private static int[] getSearchFieldValues() {
      if      (ProMode.isProOld()) return searchFieldValuesProOld;
      else if (ProMode.isProNew()) return searchFieldValuesProNew;
      else if (ProMode.isMerged()) return searchFieldValuesMerged;
      else                         return searchFieldValues;
   }

// --- construction ---

   /**
    * @param selector An object that will be modified by the dialog.
    */
   public SearchDialog(Frame owner, SearchSelector selector) {
      super(owner,Text.get(SearchDialog.class,"s1"));

      this.selector = selector;
      construct(constructFields(),/* readonly = */ false);
   }

// --- methods ---

   private JPanel constructFields() {

   // fields

      int w1 = Text.getInt(this,"w1");
      int w2 = Text.getInt(this,"w2");

      searchField = new JComboBox(getSearchFieldNames());
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

      Field.put(searchField,getSearchFieldValues(),selector.searchField);
      Field.put(searchMode,searchModeValues,selector.searchMode);
      Field.put(searchValue,selector.searchValue);

      Field.put(fromDate,Convert.fromDateStart(selector.fromDate));
      Field.put(toDate,  Convert.fromDateEnd  (selector.toDate  ));
   }

   protected void getAndValidate() throws ValidationException {

      selector.searchField = Field.get(searchField,getSearchFieldValues());
      selector.searchMode = Field.get(searchMode,searchModeValues);
      selector.searchValue = Field.get(searchValue);

      selector.fromDate = Convert.toDateStart(Field.get(fromDate));
      selector.toDate   = Convert.toDateEnd  (Field.get(toDate  ));

      selector.validate();
   }

}

