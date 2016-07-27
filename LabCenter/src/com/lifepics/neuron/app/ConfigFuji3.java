/*
 * ConfigFuji3.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.dendron.Fuji3Config;
import com.lifepics.neuron.gui.Field;
import com.lifepics.neuron.gui.GridBagHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A panel class for editing Fuji JobMaker 1.4 settings.
 */

public class ConfigFuji3 extends ConfigFormat {

// --- fields ---

   private Fuji3Config config;

   private JTextField requestDir;
   private JTextField imageDir;
   private JTextField mapImageDir;
   private JTextField printer;
   private JComboBox autoCorrect;
   private ConfigBackprint backprint;

// --- construction ---

   public ConfigFuji3(Dialog owner, Fuji3Config config) {
      super(owner);
      this.config = config;

   // fields

      requestDir = constructDataDir();
      imageDir = constructDataDir();
      mapImageDir = constructDataDir();
      printer = new JTextField(Text.getInt(this,"w1"));
      autoCorrect = new JComboBox(nullableBooleanNames);
      backprint = new ConfigBackprint(config.backprints,2); // we can't control line 1,
         // and it would confuse people because some printers don't have two lines

   // layout

      GridBagHelper helper = new GridBagHelper(this);

      int y = 0;

      y = addDataDir(helper,y,Text.get(this,"s1"),requestDir);
      y = addDataDir(helper,y,Text.get(this,"s2"),imageDir);

      helper.addSpan(0,y,4,new JLabel(Text.get(this,"s3")));
      y++;
      helper.add(1,y,mapImageDir); // can't use picker for remote path
      helper.addSpan(2,y,2,new JLabel(' ' + Text.get(this,"s6")));
      y++;

      helper.add(0,y,Box.createVerticalStrut(Text.getInt(this,"d1")));
      y++;

      helper.add(0,y,new JLabel(Text.get(this,"s4") + ' '));
      helper.add(1,y,printer);
      helper.addSpan(2,y,2,new JLabel(' ' + Text.get(this,"s7")));
      y++;

      helper.add(0,y,new JLabel(Text.get(this,"s5") + ' '));
      helper.addSpan(1,y,3,autoCorrect);
      y++;

      y = backprint.add(helper,y);
   }

// --- implementation of ConfigFormat ---

   public Object getFormatConfig() { return config; }

   public void put() {

      // normally we'd check for null on the first two, but here we have defaults
      Field.put(requestDir,Convert.fromFile(config.requestDir));
      Field.put(imageDir,Convert.fromFile(config.imageDir));
      Field.putNullable(mapImageDir,Convert.fromNullableFile(config.mapImageDir));
      Field.putNullable(printer,config.printer);
      Field.put(autoCorrect,nullableBooleanValues,nbToInt(config.autoCorrect));

      backprint.put();
   }

   public void get() throws ValidationException {

      config.requestDir = Convert.toFile(Field.get(requestDir));
      config.imageDir = Convert.toFile(Field.get(imageDir));
      config.mapImageDir = Convert.toNullableFile(Field.getNullable(mapImageDir));
      config.printer = Field.getNullable(printer);
      config.autoCorrect = intToNb(Field.get(autoCorrect,nullableBooleanValues));

      backprint.get();
   }

}

