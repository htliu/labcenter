/*
 * ConfigPurus.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.dendron.PurusConfig;
import com.lifepics.neuron.gui.Field;
import com.lifepics.neuron.gui.GridBagHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A panel class for editing Purus settings.
 */

public class ConfigPurus extends ConfigFormat {

// --- fields ---

   private PurusConfig config;

   private JTextField dataDir;
   private JTextField statusDir;
   private JTextField mandator;
   private JTextField dealer;
   private JTextField prefix;
   private JTextField orderDigits;
   private JTextField reprintDigits;
   private JCheckBox swap;

// --- construction ---

   public ConfigPurus(Dialog owner, PurusConfig config) {
      super(owner);
      this.config = config;

      int w1 = Text.getInt(this,"w1");
      int w2 = Text.getInt(this,"w2");

      dataDir = constructDataDir();
      statusDir = constructDataDir();
      mandator = new JTextField(w1);
      dealer = new JTextField(w1);
      prefix = new JTextField(w1);
      orderDigits = new JTextField(w2);
      reprintDigits = new JTextField(w2);
      swap = new JCheckBox(Text.get(this,"s9"));

      GridBagHelper helper = new GridBagHelper(this);

      int y = 0;

      y = addDataDir(helper,y,Text.get(this,"s1"),dataDir);
      y = addDataDir(helper,y,Text.get(this,"s2"),statusDir);

      helper.add(0,y,Box.createVerticalStrut(Text.getInt(this,"d1")));
      y++;

      helper.add(0,y,new JLabel(Text.get(this,"s3") + ' '));
      helper.addSpan(1,y,3,mandator);
      y++;

      helper.add(0,y,new JLabel(Text.get(this,"s4") + ' '));
      helper.addSpan(1,y,3,dealer);
      y++;

      helper.add(0,y,Box.createVerticalStrut(Text.getInt(this,"d2")));
      y++;

      helper.addSpan(0,y,4,new JLabel(Text.get(this,"s5")));
      y++;

      helper.add(0,y,new JLabel(Text.get(this,"s6") + ' '));
      helper.addSpan(1,y,3,prefix);
      y++;

      helper.add(0,y,new JLabel(Text.get(this,"s7") + ' '));
      helper.addSpan(1,y,3,orderDigits);
      y++;

      helper.add(0,y,new JLabel(Text.get(this,"s8") + ' '));
      helper.addSpan(1,y,3,reprintDigits);
      y++;

      helper.addSpan(0,y,4,swap);
      y++;
   }

// --- implementation of ConfigFormat ---

   public Object getFormatConfig() { return config; }

   public void put() {
      if (config.dataDir != null) // happens at loadDefault
         Field.put(dataDir,Convert.fromFile(config.dataDir));
      if (config.statusDir != null) // happens at loadDefault
         Field.put(statusDir,Convert.fromFile(config.statusDir));
      Field.put(mandator,config.mandator);
      Field.put(dealer,config.dealer);
      Field.put(prefix,config.prefix);
      Field.put(orderDigits,Convert.fromInt(config.orderDigits));
      Field.put(reprintDigits,Convert.fromInt(config.reprintDigits));
      Field.put(swap,config.swap);
   }

   public void get() throws ValidationException {
      config.dataDir = Convert.toFile(Field.get(dataDir));
      config.statusDir = Convert.toFile(Field.get(statusDir));
      config.mandator = Field.get(mandator);
      config.dealer = Field.get(dealer);
      config.prefix = Field.get(prefix);
      config.orderDigits = Convert.toInt(Field.get(orderDigits));
      config.reprintDigits = Convert.toInt(Field.get(reprintDigits));
      config.swap = Field.get(swap);
   }

}

