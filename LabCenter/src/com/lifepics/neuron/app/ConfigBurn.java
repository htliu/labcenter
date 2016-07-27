/*
 * ConfigBurn.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.dendron.BurnConfig;
import com.lifepics.neuron.gui.Field;
import com.lifepics.neuron.gui.GridBagHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A panel class for editing media burn settings.
 */

public class ConfigBurn extends ConfigFormat {

// --- fields ---

   private BurnConfig config;

   private JTextField dataDir;
   private JTextField command;
   private JTextField workingDir;

// --- construction ---

   public ConfigBurn(Dialog owner, BurnConfig config) {
      super(owner);
      this.config = config;

      dataDir = constructDataDir();
      command = new JTextField(Text.getInt(this,"w1"));
      workingDir = constructDataDir();

      GridBagHelper helper = new GridBagHelper(this);

      int y = 0;

      y = addDataDir(helper,y,Text.get(this,"s1"),dataDir);

      helper.addSpan(0,y,4,new JLabel(Text.get(this,"s2")));
      y++;
      helper.addSpan(1,y,3,command);
      y++;

      y = addDataDir(helper,y,Text.get(this,"s3"),workingDir);
   }

// --- implementation of ConfigFormat ---

   public Object getFormatConfig() { return config; }

   public void put() {
      if (config.dataDir != null) // happens at loadDefault
         Field.put(dataDir,Convert.fromFile(config.dataDir));
      Field.putNullable(command,config.command);
      Field.putNullable(workingDir,Convert.fromNullableFile(config.workingDir));
   }

   public void get() throws ValidationException {
      config.dataDir = Convert.toFile(Field.get(dataDir));
      config.command = Field.getNullable(command);
      config.workingDir = Convert.toNullableFile(Field.getNullable(workingDir));
   }

}

