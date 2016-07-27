/*
 * ConfigBeaufort.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.dendron.BeaufortConfig;
import com.lifepics.neuron.gui.Field;
import com.lifepics.neuron.gui.GridBagHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A panel class for editing Beaufort settings.
 */

public class ConfigBeaufort extends ConfigFormat {

// --- fields ---

   private BeaufortConfig config;

   private JTextField dataDir;

// --- construction ---

   public ConfigBeaufort(Dialog owner, BeaufortConfig config) {
      super(owner);
      this.config = config;

      dataDir = constructDataDir();

      GridBagHelper helper = new GridBagHelper(this);

      int y = 0;

      y = addDataDir(helper,y,Text.get(this,"s1"),dataDir);
   }

// --- implementation of ConfigFormat ---

   public Object getFormatConfig() { return config; }

   public void put() {
      Field.put(dataDir,Convert.fromFile(config.dataDir));
   }

   public void get() throws ValidationException {
      config.dataDir = Convert.toFile(Field.get(dataDir));
   }

}

