/*
 * ConfigDKS3.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.dendron.DKS3Config;
import com.lifepics.neuron.gui.Field;
import com.lifepics.neuron.gui.GridBagHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A panel class for editing DKS3 settings.
 */

public class ConfigDKS3 extends ConfigFormat {

// --- fields ---

   private DKS3Config config;

   private JTextField dataDir;
   private JTextField prefix;
   private JComboBox priority;
   private JCheckBox autoArchive;
   private ConfigBackprint backprint;

// --- combo boxes ---

   private static Object[] priorityNames = new Object[] { Text.get(ConfigDKS3.class,"pr2"),
                                                          Text.get(ConfigDKS3.class,"pr1"),
                                                          Text.get(ConfigDKS3.class,"pr0")  };
   private static int[] priorityValues = new int[] { DKS3Config.PRIORITY_HIGH,
                                                     DKS3Config.PRIORITY_NORMAL,
                                                     DKS3Config.PRIORITY_LOW     };

// --- construction ---

   public ConfigDKS3(Dialog owner, DKS3Config config) {
      super(owner);
      this.config = config;

   // fields

      dataDir = constructDataDir();
      prefix = new JTextField(Text.getInt(this,"w1"));
      priority = new JComboBox(priorityNames);
      autoArchive = new JCheckBox();
      backprint = new ConfigBackprint(config.backprints);

   // layout

      GridBagHelper helper = new GridBagHelper(this);

      int y = 0;

      y = addDataDir(helper,y,Text.get(this,"s1"),dataDir);

      helper.add(0,y,Box.createVerticalStrut(Text.getInt(this,"d1")));
      y++;

      helper.add(0,y,new JLabel(Text.get(this,"s2") + ' '));
      helper.addSpan(1,y,3,prefix);
      y++;

      helper.add(0,y,new JLabel(Text.get(this,"s3") + ' '));
      helper.addSpan(1,y,3,priority);
      y++;

      helper.add(0,y,new JLabel(Text.get(this,"s4") + ' '));
      helper.addSpan(1,y,3,autoArchive);
      y++;

      y = backprint.add(helper,y);
   }

// --- implementation of ConfigFormat ---

   public Object getFormatConfig() { return config; }

   public void put() {
      Field.put(dataDir,Convert.fromFile(config.dataDir));
      Field.put(prefix,config.prefix);
      Field.put(priority,priorityValues,config.priority);
      Field.put(autoArchive,config.autoArchive);
      backprint.put();
   }

   public void get() throws ValidationException {
      config.dataDir = Convert.toFile(Field.get(dataDir));
      config.prefix = Field.get(prefix);
      config.priority = Field.get(priority,priorityValues);
      config.autoArchive = Field.get(autoArchive);
      backprint.get();
   }

}

