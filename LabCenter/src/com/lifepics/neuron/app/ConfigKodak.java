/*
 * ConfigKodak.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.dendron.KodakConfig;
import com.lifepics.neuron.gui.Field;
import com.lifepics.neuron.gui.GridBagHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A panel class for editing Kodak kiosk settings.
 */

public class ConfigKodak extends ConfigFormat {

// --- fields ---

   private KodakConfig config;

   private JTextField dataDir;
   private JComboBox beginThermal;
   private JComboBox beginHold;
   private JRadioButton claimedYes;
   private JRadioButton claimedNo;
   private JCheckBox allowSingleSided;
   private JCheckBox collateInReverse;

// --- combo boxes ---

   private static final int BEGIN_LAB     = 0;
   private static final int BEGIN_THERMAL = 1;

   private static final int BEGIN_HOLD    = 0;
   private static final int BEGIN_RELEASE = 1;

   private static Object[] beginThermalNames = new Object[] { Text.get(ConfigKodak.class,"bt0"),
                                                              Text.get(ConfigKodak.class,"bt1")  };
   private static int[] beginThermalValues = new int[] { BEGIN_LAB,
                                                         BEGIN_THERMAL };

   private static Object[] beginHoldNames = new Object[] { Text.get(ConfigKodak.class,"bh0"),
                                                           Text.get(ConfigKodak.class,"bh1")  };
   private static int[] beginHoldValues = new int[] { BEGIN_HOLD,
                                                      BEGIN_RELEASE };

// --- construction ---

   public ConfigKodak(Dialog owner, boolean border, KodakConfig config) {
      super(owner);
      this.config = config;

      dataDir = constructDataDir();
      beginThermal = new JComboBox(beginThermalNames);
      beginHold = new JComboBox(beginHoldNames);
      claimedYes = new JRadioButton(Text.get(this,"s7a"));
      claimedNo = new JRadioButton(Text.get(this,"s8a"));
      allowSingleSided = new JCheckBox(Text.get(this,"s10"));
      collateInReverse = new JCheckBox();

      ButtonGroup group = new ButtonGroup();
      group.add(claimedYes);
      group.add(claimedNo);

      if (border) setBorder(BorderFactory.createTitledBorder(Text.get(this,"s1")));

   // subpanels

      JPanel panel1 = new JPanel();
      GridBagHelper helper = new GridBagHelper(panel1);

      helper.add(0,0,claimedYes);
      helper.add(1,0,new JLabel(Text.get(this,"s7b")));
      helper.add(0,1,claimedNo);
      helper.add(1,1,new JLabel(Text.get(this,"s8b")));

      JPanel panel2 = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
      panel2.add(new JLabel(Text.get(this,"s11")));
      panel2.add(collateInReverse);
      panel2.add(new JLabel(Text.get(this,"s12")));

      JPanel panel3 = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
      panel3.add(beginHold);
      panel3.add(new JLabel(' ' + Text.get(this,"s13")));

   // main panel

      helper = new GridBagHelper(this);

      int y = 0;

      y = addDataDir(helper,y,Text.get(this,"s2"),dataDir);

      helper.add(0,y,Box.createVerticalStrut(Text.getInt(this,"d1")));
      y++;

      helper.add(0,y,new JLabel(Text.get(this,"s9")));
      y++;

      helper.add(0,y,new JLabel(Text.get(this,"s3") + ' '));
      helper.addSpan(1,y,3,beginThermal);
      y++;

      helper.add(0,y,new JLabel(Text.get(this,"s4") + ' '));
      helper.addSpan(1,y,3,panel3);
      y++;

      helper.add(0,y,Box.createVerticalStrut(Text.getInt(this,"d2")));
      y++;

      helper.addSpan(0,y,4,new JLabel(Text.get(this,"s5")));
      y++;
      helper.addSpan(0,y,4,new JLabel(Text.get(this,"s6")));
      y++;
      helper.addSpan(0,y,4,panel1);
      y++;

      helper.add(0,y,Box.createVerticalStrut(Text.getInt(this,"d3")));
      y++;

      helper.addSpan(0,y,4,allowSingleSided);
      y++;

      helper.addSpan(1,y,3,panel2);
      y++;
   }

// --- implementation of ConfigFormat ---

   public Object getFormatConfig() { return config; }

   public void put() {
      Field.put(dataDir,Convert.fromFile(config.dataDir));

      int value = config.beginThermal ? BEGIN_THERMAL : BEGIN_LAB;
      Field.put(beginThermal,beginThermalValues,value);

      value = config.beginHold ? BEGIN_HOLD : BEGIN_RELEASE;
      Field.put(beginHold,beginHoldValues,value);

      if (config.claimedIsComplete) claimedYes.setSelected(true);
      else                          claimedNo .setSelected(true);

      Field.put(allowSingleSided,config.allowSingleSided);
      Field.put(collateInReverse,config.collateInReverse);
   }

   public void get() throws ValidationException {
      config.dataDir = Convert.toFile(Field.get(dataDir));

      int value = Field.get(beginThermal,beginThermalValues);
      config.beginThermal = (value == BEGIN_THERMAL);

      value = Field.get(beginHold,beginHoldValues);
      config.beginHold = (value == BEGIN_HOLD);

      config.claimedIsComplete = claimedYes.isSelected();

      config.allowSingleSided = Field.get(allowSingleSided);
      config.collateInReverse = Field.get(collateInReverse);
   }

}

