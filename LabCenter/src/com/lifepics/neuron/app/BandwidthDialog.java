/*
 * BandwidthDialog.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.gui.EditDialog;
import com.lifepics.neuron.gui.Field;
import com.lifepics.neuron.gui.GridBagHelper;
import com.lifepics.neuron.net.BandwidthConfig;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A subdialog for editing bandwidth settings.
 */

public class BandwidthDialog extends EditDialog {

// --- fields ---

   private BandwidthConfig bc;
   private int n;
   private String suffix; // "d" or "u" for download or upload customization
   private boolean showRestrict;

   private JTextField[] startTime;
   private JComboBox[] bandwidthPercent;
   private JCheckBox[] restrict;

// --- construction ---

   public BandwidthDialog(Dialog owner, BandwidthConfig bc, String suffix, boolean showRestrict) {
      super(owner,Text.get(BandwidthDialog.class,"s1" + suffix));

      this.bc = bc;
      n = bc.schedules.size();
      this.suffix = suffix;
      this.showRestrict = showRestrict;

      construct(constructFields(),/* readonly = */ false);
   }

// --- combo boxes ---

   private static Object[] standardPercents = new Object[] { "0", "25", "50", "75", "100" };

   private static String copies(char c, int n) {
      char[] s = new char[n];
      for (int i=0; i<n; i++) s[i] = c;
      return new String(s);
   }

// --- methods ---

   private JPanel constructFields() {

      int w1 = Text.getInt(this,"w1");
      int w2 = Text.getInt(this,"w2");

      String prototype = copies('m',w2); // 'm' is what JTextField.getColumnWidth uses.
      //
      // note, w2 is different than w1 because the fixed distances
      // are larger for JComboBox (+2) than for JTextField (-4).
      // (that's compared to the 'm' width of 8 in the Windows L&F.)

      startTime = new JTextField[n];
      bandwidthPercent = new JComboBox[n];
      if (showRestrict) restrict = new JCheckBox[n];

      for (int i=0; i<n; i++) {

         startTime[i] = new JTextField(w1);

         bandwidthPercent[i] = new JComboBox(standardPercents);
         bandwidthPercent[i].setEditable(true);
         bandwidthPercent[i].setPrototypeDisplayValue(prototype);
         //
         // editable combos become wider than the model values,
         // not sure why, but setting the prototype fixes them.

         if (showRestrict) restrict[i] = new JCheckBox();
      }

      JPanel fields = new JPanel();
      GridBagHelper helper = new GridBagHelper(fields);

      int y = 0;

      helper.addCenter(0,y,new JLabel(Text.get(this,"s2")));
      helper.add(1,y,Box.createHorizontalStrut(Text.getInt(this,"d1")));
      helper.add(3,y,Box.createHorizontalStrut(Text.getInt(this,"d2")));
      helper.addCenter(4,y,new JLabel(Text.get(this,"s4")));
      if (showRestrict) helper.add(6,y,Box.createHorizontalStrut(Text.getInt(this,"d5")));
      if (showRestrict) helper.addCenter(7,y,new JLabel(Text.get(this,"s10")));
      y++;

      helper.addCenter(4,y,new JLabel(Text.get(this,"s5")));
      if (showRestrict) helper.addCenter(7,y,new JLabel(Text.get(this,"s11")));
      y++;

      helper.add(0,y,Box.createVerticalStrut(Text.getInt(this,"d3")));
      y++;

      String percent = ' ' + Text.get(this,"s9");

      for (int i=0; i<n; i++) {
         BandwidthConfig.Schedule schedule = (BandwidthConfig.Schedule) bc.schedules.get(i);
         // get is inefficient, but it's a short list

         helper.add(0,y,startTime[i]);
         helper.add(2,y,new JLabel(schedule.entryName));
         helper.add(4,y,bandwidthPercent[i]);
         helper.add(5,y,new JLabel(percent));
         if (showRestrict) helper.addCenter(7,y,restrict[i]);
         y++;
      }

      helper.add(0,y,Box.createVerticalStrut(Text.getInt(this,"d4")));
      y++;

      helper.addSpan(0,y,6,new JLabel(Text.get(this,"s6")));
      y++;

      helper.addSpan(0,y,6,new JLabel(Text.get(this,"s7")));
      y++;

      helper.addSpan(0,y,6,new JLabel(Text.get(this,"s8")));
      y++;

      return fields;
   }

   protected void put() {

      for (int i=0; i<n; i++) {
         BandwidthConfig.Schedule schedule = (BandwidthConfig.Schedule) bc.schedules.get(i);
         // get is inefficient, but it's a short list

         Field.put(startTime[i],Convert.fromTime(schedule.startTime));
         bandwidthPercent[i].setSelectedItem(Convert.fromInt(schedule.bandwidthPercent));
         if (showRestrict) Field.put(restrict[i],schedule.isRestricted());
      }
   }

   protected void getAndValidate() throws ValidationException {

      // as in BarcodeDialog, here the caller takes care of
      // copying after validation is complete, so we can
      // just get the fields in whatever order we choose to.

      for (int i=0; i<n; i++) {
         BandwidthConfig.Schedule schedule = (BandwidthConfig.Schedule) bc.schedules.get(i);
         // get is inefficient, but it's a short list

         schedule.startTime = Convert.toTime(Field.get(startTime[i]));
         schedule.bandwidthPercent = Convert.toInt((String) bandwidthPercent[i].getSelectedItem());
         if (showRestrict) schedule.setRestricted(Field.get(restrict[i]));
      }

      bc.validate();
   }

}

