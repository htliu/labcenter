/*
 * AutoCompleteDialog.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.dendron.AutoCompleteConfig;
import com.lifepics.neuron.gui.Blob;
import com.lifepics.neuron.gui.EditDialog;
import com.lifepics.neuron.gui.Field;
import com.lifepics.neuron.gui.GridBagHelper;
import com.lifepics.neuron.gui.IntervalField;
import com.lifepics.neuron.misc.StoreHours;
import com.lifepics.neuron.net.MerchantConfig;

import java.util.LinkedList;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A subdialog for editing autocomplete settings.
 */

public class AutoCompleteDialog extends EditDialog {

// --- fields ---

   private AutoCompleteConfig acc;
   private LinkedList storeHours;
   private String storeHoursURL;
   private MerchantConfig merchantConfig;

   private IntervalField autoCompleteDelay;
   private JCheckBox restrictToStoreHours;
   private IntervalField deltaOpen;
   private IntervalField deltaClose;
   private JComboBox deltaOpenSign;
   private JComboBox deltaCloseSign;
   private JComponent hoursDisplay;

// --- construction ---

   public AutoCompleteDialog(Dialog owner, AutoCompleteConfig acc, LinkedList storeHours, String storeHoursURL, MerchantConfig merchantConfig) {
      super(owner,Text.get(AutoCompleteDialog.class,"s1"));

      this.acc = acc;
      this.storeHours = storeHours;
      this.storeHoursURL = storeHoursURL;
      this.merchantConfig = merchantConfig;

      construct(constructFields(),/* readonly = */ false);
   }

// --- combo boxes ---

   private static Object[] signNames = new Object[] { Text.get(AutoCompleteDialog.class,"ds0"),
                                                      Text.get(AutoCompleteDialog.class,"ds1")  };
   private static int[] signValues = new int[] { -1, 1 };

// --- methods ---

   private JPanel constructFields() {

      autoCompleteDelay = new IntervalField(IntervalField.MINUTES,IntervalField.HOURS);
      restrictToStoreHours = new JCheckBox(Text.get(this,"s4"));
      deltaOpen = new IntervalField(IntervalField.MINUTES,IntervalField.HOURS);
      deltaClose = new IntervalField(IntervalField.MINUTES,IntervalField.HOURS);
      deltaOpenSign = new JComboBox(signNames);
      deltaCloseSign = new JComboBox(signNames);
      hoursDisplay = Blob.makeScrollBlob(StoreHours.describe(storeHours),Text.getInt(this,"h1"),Text.getInt(this,"w1"));

      JButton buttonRefresh = new JButton(Text.get(this,"s10"));
      buttonRefresh.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doRefresh(); } });

      JPanel fields = new JPanel();
      GridBagHelper helper = new GridBagHelper(fields);

      int y = 0;

      int d1 = Text.getInt(this,"d1");

      helper.add(0,y,Box.createVerticalStrut(Text.getInt(this,"d2")));
      y++;
      // whitespace at top is nonstandard, but it looks good here

      helper.add(0,y,new JLabel(Text.get(this,"s2") + ' '));
      helper.add(1,y,autoCompleteDelay);
      helper.addSpan(2,y,6,new JLabel(' ' + Text.get(this,"s3")));
      y++;

      helper.add(0,y,Box.createVerticalStrut(Text.getInt(this,"d3")));
      y++;

      helper.addSpan(0,y,3,restrictToStoreHours);
      helper.add(3,y,new JLabel(Text.get(this,"s5") + ' '));
      helper.add(4,y,deltaOpen);
      helper.add(5,y,Box.createHorizontalStrut(d1));
      helper.add(6,y,deltaOpenSign);
      helper.add(7,y,new JLabel(' ' + Text.get(this,"s6")));
      y++;

      helper.add(3,y,new JLabel(Text.get(this,"s7") + ' '));
      helper.add(4,y,deltaClose);
      helper.add(5,y,Box.createHorizontalStrut(d1));
      helper.add(6,y,deltaCloseSign);
      helper.add(7,y,new JLabel(' ' + Text.get(this,"s8")));
      y++;

      helper.add(0,y,Box.createVerticalStrut(Text.getInt(this,"d4")));
      y++;

      JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
      panel.add(new JLabel(Text.get(this,"s9")));
      panel.add(Box.createHorizontalStrut(Text.getInt(this,"d5")));
      panel.add(buttonRefresh);
      //
      helper.addSpan(0,y,8,panel);
      y++;

      helper.add(0,y,Box.createVerticalStrut(Text.getInt(this,"d6")));
      y++;

      helper.addSpanFill(0,y,8,hoursDisplay);
      y++;

      return fields;
   }

   protected void put() {

      autoCompleteDelay.put(acc.autoCompleteDelay);
      Field.put(restrictToStoreHours,acc.restrictToStoreHours);
      putSigned(deltaOpen,deltaOpenSign,acc.deltaOpen);
      putSigned(deltaClose,deltaCloseSign,acc.deltaClose);
   }

   protected void getAndValidate() throws ValidationException {

      acc.autoCompleteDelay = autoCompleteDelay.get();
      acc.restrictToStoreHours = Field.get(restrictToStoreHours);
      acc.deltaOpen = getSigned(deltaOpen,deltaOpenSign);
      acc.deltaClose = getSigned(deltaClose,deltaCloseSign);

      acc.validate();

      // do cross-validation now too
      acc.validateHours(storeHours);
   }

   // eventually we might want to standardize signed combos,
   // but this is good enough for now.
   // a new class seems like overkill, and also I'm not sure that
   // "before" and "after" would be the right words to use in all
   // contexts.

   private void putSigned(IntervalField f, JComboBox sign, long delta) {
      int value = 1;
      if (delta <= 0) { delta = -delta; value = -1; } // zero could be either, but "before" is most useful
      f.put(delta);
      Field.put(sign,signValues,value);
   }

   private long getSigned(IntervalField f, JComboBox sign) throws ValidationException {
      return f.get() * Field.get(sign,signValues);
   }

   private void doRefresh() {
      if (StoreHoursTransaction.refresh(this,storeHoursURL,merchantConfig,storeHours)) {
         Blob.updateScrollBlob(hoursDisplay,StoreHours.describe(storeHours));
      }
   }

}

