/*
 * ConfigKonica.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.dendron.KonicaConfig;
import com.lifepics.neuron.gui.Field;
import com.lifepics.neuron.gui.GridBagHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A panel class for editing Konica settings.
 */

public class ConfigKonica extends ConfigFormat {

// --- fields ---

   private KonicaConfig config;

   private JTextField dataDir;
   private ConfigBackprint backprint;

// --- construction ---

   public ConfigKonica(Dialog owner, boolean border, KonicaConfig config) {
      super(owner);
      this.config = config;

      dataDir = constructDataDir();
      backprint = new ConfigBackprint(config.backprints);

      if (border) setBorder(BorderFactory.createTitledBorder(Text.get(this,"s45")));

      GridBagHelper helper = new GridBagHelper(this);

      int y = 0;

      y = addDataDir(helper,y,Text.get(this,"s46"),dataDir);
      y = backprint.add(helper,y);
   }

// --- implementation of ConfigFormat ---

   public Object getFormatConfig() { return config; }

   public void put() {
      if (config.dataDir != null) // happens at loadDefault
         Field.put(dataDir,Convert.fromFile(config.dataDir));
      backprint.put();
   }

   public void get() throws ValidationException {
      config.dataDir = Convert.toFile(Field.get(dataDir));
      backprint.get();
   }

}

