/*
 * ConfigBackprint.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.dendron.Backprint;
import com.lifepics.neuron.gui.DisableTextField;
import com.lifepics.neuron.gui.Field;
import com.lifepics.neuron.gui.GridBagHelper;

import java.util.LinkedList;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A helper class for ConfigDialog.
 */

public class ConfigBackprint {

// --- fields ---

   private LinkedList backprints;
   private int backprintBase;
   private int backprintCount;

   private JComboBox[] backprintType;
   private JTextField[] backprintMessage;

// --- combo boxes ---

   private static Object[] backprintTypeNames = new Object[] { Text.get(ConfigBackprint.class,"bt0"),
                                                               Text.get(ConfigBackprint.class,"bt1"),
                                                               Text.get(ConfigBackprint.class,"bt3"),
                                                               Text.get(ConfigBackprint.class,"bt13"),
                                                               Text.get(ConfigBackprint.class,"bt6"),
                                                               Text.get(ConfigBackprint.class,"bt4"),
                                                               Text.get(ConfigBackprint.class,"bt5"),
                                                               Text.get(ConfigBackprint.class,"bt10"),
                                                               Text.get(ConfigBackprint.class,"bt2"),
                                                               Text.get(ConfigBackprint.class,"bt11"),
                                                               Text.get(ConfigBackprint.class,"bt12"),
                                                               Text.get(ConfigBackprint.class,"bt7"),
                                                               Text.get(ConfigBackprint.class,"bt8"),
                                                               Text.get(ConfigBackprint.class,"bt9") };

   private static int[] backprintTypeValues = new int[] { Backprint.TYPE_NOTHING,
                                                          Backprint.TYPE_FILENAME,
                                                          Backprint.TYPE_ORDER_ID,
                                                          Backprint.TYPE_ORDER_FN,
                                                          Backprint.TYPE_ORDER_SEQ,
                                                          Backprint.TYPE_NAME_LAST,
                                                          Backprint.TYPE_NAME_FULL,
                                                          Backprint.TYPE_DEALER,
                                                          Backprint.TYPE_CUSTOM,
                                                          Backprint.TYPE_COMMENTS_1,
                                                          Backprint.TYPE_COMMENTS_2,
                                                          Backprint.TYPE_WHOLESALE_1,
                                                          Backprint.TYPE_WHOLESALE_2,
                                                          Backprint.TYPE_WHOLESALE_3 };

// --- methods ---

   public ConfigBackprint(LinkedList backprints) {
      this(backprints,1);
   }
   public ConfigBackprint(LinkedList backprints, int backprintBase) {

      this.backprints = backprints;
      this.backprintBase = backprintBase;
      backprintCount = backprints.size();

      backprintType = new JComboBox[backprintCount];
      backprintMessage = new JTextField[backprintCount];

      int w7 = Text.getInt(this,"w7"); // width for backprint messages

      for (int i=0; i<backprintCount; i++) {
         backprintType[i] = new JComboBox(backprintTypeNames);
         backprintMessage[i] = new DisableTextField(w7);

         ActionListener a = new BackprintListener(i);
         a.actionPerformed(null);
         backprintType[i].addActionListener(a);
      }
   }

   private class BackprintListener implements ActionListener {

      private int i;
      public BackprintListener(int i) { this.i = i; }

      public void actionPerformed(ActionEvent e) { // e is null for initial update
         int type = Field.get(backprintType[i],backprintTypeValues);
         backprintMessage[i].setEnabled(type == Backprint.TYPE_CUSTOM);

         // you can always get something into the message by enabling and disabling,
         // and we don't validate that the message is empty for non-custom type ...
         // this is more in the nature of a visual indicator that you're supposed to type there.
      }
   }

   public int add(GridBagHelper helper, int y) {
      return add(helper,y,/* spacer = */ true);
   }
   public int add(GridBagHelper helper, int y, boolean spacer) {

      int d2 = Text.getInt(this,"d2"); // spacing for backprint lines

      if (spacer) {
         helper.add(0,y,Box.createVerticalStrut(d2));
         y++;
      }

      helper.add(0,y,new JLabel(Text.get(this,"s33")));
      y++;

      for (int i=0; i<backprintCount; i++) {

         if (i > 0) {
            helper.add(0,y,Box.createVerticalStrut(d2));
            y++;
         }

         helper.add(0,y,new JLabel(Text.get(this,"s34",new Object[] { Convert.fromInt(i+backprintBase) }) + ' '));
         helper.addSpan(1,y,3,backprintType[i]);
         y++;

         helper.addSpan(1,y,3,backprintMessage[i]);
         y++;
      }

      return y;
   }

   public void put() {
      for (int i=0; i<backprintCount; i++) {
         Backprint b = (Backprint) backprints.get(i);
         Field.put(backprintType[i],backprintTypeValues,b.type);
         Field.put(backprintMessage[i],b.message);
      }
   }

   public void get() {
      for (int i=0; i<backprintCount; i++) {
         Backprint b = (Backprint) backprints.get(i);
         b.type = Field.get(backprintType[i],backprintTypeValues);
         b.message = Field.get(backprintMessage[i]);
      }
   }

}

