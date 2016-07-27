/*
 * ConfigLucidiom.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.dendron.LucidiomConfig;
import com.lifepics.neuron.gui.Field;
import com.lifepics.neuron.gui.GridBagHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A panel class for editing Lucidiom settings.
 */

public class ConfigLucidiom extends ConfigFormat {

// --- fields ---

   private LucidiomConfig config;

   private JTextField dataDir;
   private JTextField apmID;
   private JTextField brand;
   private JCheckBox glossy;

// --- construction ---

   public ConfigLucidiom(Dialog owner, LucidiomConfig config) {
      super(owner);
      this.config = config;

      dataDir = constructDataDir();

      int w1 = Text.getInt(this,"w1");
      apmID = new JTextField(w1);
      brand = new JTextField(w1);
      glossy = new JCheckBox();

      GridBagHelper helper = new GridBagHelper(this);

      int y = 0;

      y = addDataDir(helper,y,Text.get(this,"s1"),dataDir);

      helper.add(0,y,new JLabel(Text.get(this,"s2") + ' '));
      helper.addSpan(1,y,3,apmID);
      y++;

      helper.add(0,y,new JLabel(Text.get(this,"s3") + ' '));
      helper.addSpan(1,y,3,brand);
      y++;

      helper.add(0,y,new JLabel(Text.get(this,"s4") + ' '));
      helper.addSpan(1,y,3,glossy);
      y++;
   }

// --- implementation of ConfigFormat ---

   public Object getFormatConfig() { return config; }

   public void put() {
      if (config.dataDir != null) // happens at loadDefault
         Field.put(dataDir,Convert.fromFile(config.dataDir));
      Field.put(apmID,config.apmID); // these two are null at loadDefault, but that's fine
      Field.put(brand,config.brand);
      Field.put(glossy,config.glossy);
   }

   public void get() throws ValidationException {
      config.dataDir = Convert.toFile(Field.get(dataDir));
      config.apmID = Field.get(apmID);
      config.brand = Field.get(brand);
      config.glossy = Field.get(glossy);
   }

}

