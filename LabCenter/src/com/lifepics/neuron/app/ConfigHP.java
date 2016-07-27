/*
 * ConfigHP.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.dendron.HPConfig;
import com.lifepics.neuron.gui.Field;
import com.lifepics.neuron.gui.GridBagHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A panel class for editing HP settings.
 */

public class ConfigHP extends ConfigFormat {

// --- fields ---

   private HPConfig config;

   private JTextField dataDir;
   private JComboBox priority;
   private JTextField prefix;
   private JTextField useDigits;
   private JTextField dealerID;
   private JTextField dealerName;

   private ConfigBackprint backprint;

// --- combo boxes ---

   private static Object[] priorityNames = new Object[] { Text.get(ConfigHP.class,"pr0"),
                                                          Text.get(ConfigHP.class,"pr1"),
                                                          Text.get(ConfigHP.class,"pr2"),
                                                          Text.get(ConfigHP.class,"pr3"),
                                                          Text.get(ConfigHP.class,"pr4"),
                                                          Text.get(ConfigHP.class,"pr5")  };
   private static int[] priorityValues = new int[] { HPConfig.PRIORITY_DEFAULT,
                                                     HPConfig.PRIORITY_FIRST,
                                                     HPConfig.PRIORITY_HIGH,
                                                     HPConfig.PRIORITY_NORMAL,
                                                     HPConfig.PRIORITY_LOW,
                                                     HPConfig.PRIORITY_HOLD };

// --- construction ---

   public ConfigHP(Dialog owner, HPConfig config) {
      super(owner);
      this.config = config;

   // fields

      dataDir = constructDataDir();
      priority = new JComboBox(priorityNames);
      prefix = new JTextField(Text.getInt(this,"w2"));
      useDigits = new JTextField(Text.getInt(this,"w3"));
      dealerID = new JTextField(Text.getInt(this,"w4"));
      dealerName = new JTextField(Text.getInt(this,"w5"));

      backprint = new ConfigBackprint(config.backprints);

   // layout

      GridBagHelper helper = new GridBagHelper(this);

      int y = 0;

      y = addDataDir(helper,y,Text.get(this,"s1"),dataDir);

      helper.add(0,y,Box.createVerticalStrut(Text.getInt(this,"d1")));
      y++;

      helper.add(0,y,new JLabel(Text.get(this,"s3") + ' '));
      helper.addSpan(1,y,3,priority);
      y++;

      JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
      panel.add(prefix);
      panel.add(new JLabel(' ' + Text.get(this,"s5") + ' '));
      panel.add(useDigits);
      panel.add(new JLabel(' ' + Text.get(this,"s6")));

      helper.add(0,y,new JLabel(Text.get(this,"s4") + ' '));
      helper.addSpan(1,y,3,panel);
      y++;

      helper.add(0,y,new JLabel(Text.get(this,"s7") + ' '));
      helper.addSpan(1,y,3,dealerID);
      y++;

      helper.add(0,y,new JLabel(Text.get(this,"s8") + ' '));
      helper.addSpan(1,y,3,dealerName);
      y++;

      y = backprint.add(helper,y);
   }

// --- implementation of ConfigFormat ---

   public Object getFormatConfig() { return config; }

   public void put() {

      Field.put(dataDir,Convert.fromFile(config.dataDir));
      Field.put(priority,priorityValues,config.priority);
      Field.put(prefix,config.prefix);
      Field.put(useDigits,Convert.fromInt(config.useDigits));
      Field.put(dealerID,config.dealerID);
      Field.put(dealerName,config.dealerName);

      backprint.put();
   }

   public void get() throws ValidationException {

      config.dataDir = Convert.toFile(Field.get(dataDir));
      config.priority = Field.get(priority,priorityValues);
      config.prefix = Field.get(prefix);
      config.useDigits = Convert.toInt(Field.get(useDigits));
      config.dealerID = Field.get(dealerID);
      config.dealerName = Field.get(dealerName);

      backprint.get();
   }

}

