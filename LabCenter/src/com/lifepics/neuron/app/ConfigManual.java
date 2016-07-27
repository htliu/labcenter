/*
 * ConfigManual.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.dendron.ManualConfig;
import com.lifepics.neuron.gui.Field;
import com.lifepics.neuron.gui.GridBagHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A panel class for editing manual settings.
 */

public class ConfigManual extends ConfigFormat {

// --- fields ---

   private ManualConfig config;

   private JTextField dataDir;
   private JCheckBox autoCreate;

// --- construction ---

   public ConfigManual(Dialog owner, boolean border, ManualConfig config) {
      this(owner,border,config,Text.get(ConfigManual.class,"s1"),Text.get(ConfigManual.class,"s2"));
   }

   public ConfigManual(Dialog owner, boolean border, ManualConfig config, String title, String label) {
      super(owner);
      this.config = config;

      dataDir = constructDataDir();
      autoCreate = new JCheckBox(Text.get(this,"s3"));

      if (border) setBorder(BorderFactory.createTitledBorder(title));

      GridBagHelper helper = new GridBagHelper(this);

      int y = 0;

      y = addDataDir(helper,y,label,dataDir);

      helper.addSpan(1,y,3,autoCreate);
      y++;
   }

   // as another option, you can pass in null for the config object,
   // call this to get hold of the text field, and access it yourself.
   // just don't call getFormatConfig, put, or get, or it'll crash.

   public JTextField asTextField() { return dataDir; }

// --- implementation of ConfigFormat ---

   public Object getFormatConfig() { return config; }

   public void put() {
      if (config.dataDir != null) // happens at loadDefault
         Field.put(dataDir,Convert.fromFile(config.dataDir));
      Field.put(autoCreate,config.autoCreate);
   }

   public void get() throws ValidationException {
      config.dataDir = Convert.toFile(Field.get(dataDir));
      config.autoCreate = Field.get(autoCreate);
   }

}

