/*
 * ConfigHotFolder.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.dendron.HotFolderConfig;
import com.lifepics.neuron.gui.Field;
import com.lifepics.neuron.gui.GridBagHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A panel class for editing hot folder settings.
 */

public class ConfigHotFolder extends ConfigFormat {

// --- fields ---

   private HotFolderConfig config;

   private JTextField dataDir;

// --- construction ---

   public ConfigHotFolder(Dialog owner, HotFolderConfig config) {
      super(owner);
      this.config = config;

      dataDir = constructDataDir();

      GridBagHelper helper = new GridBagHelper(this);

      int y = 0;

      y = addDataDir(owner,helper,y,Text.get(this,"s1"),dataDir,/* pack = */ true);
   }

// --- implementation of ConfigFormat ---

   public Object getFormatConfig() { return config; }

   public void put() {
      if (config.dataDir != null) // happens at loadDefault
         Field.put(dataDir,Convert.fromFile(config.dataDir));
   }

   public void get() throws ValidationException {
      config.dataDir = Convert.toFile(Field.get(dataDir));
   }

}

