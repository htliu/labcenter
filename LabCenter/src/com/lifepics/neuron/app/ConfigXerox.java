/*
 * ConfigXerox.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.dendron.XeroxConfig;
import com.lifepics.neuron.gui.Field;
import com.lifepics.neuron.gui.GridBagHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A panel class for editing Xerox settings.
 */

public class ConfigXerox extends ConfigFormat {

// --- fields ---

   private XeroxConfig config;

   private JTextField requestDir;
   private JTextField imageDir;
   private JTextField mapImageDir;

   private JTextField prefix;
   private JTextField useDigits;

// --- construction ---

   public ConfigXerox(Dialog owner, XeroxConfig config) {
      super(owner);
      this.config = config;

   // fields

      requestDir = constructDataDir();
      imageDir = constructDataDir();
      mapImageDir = constructDataDir();

      prefix = new JTextField(Text.getInt(this,"w1"));
      useDigits = new JTextField(Text.getInt(this,"w2"));

   // layout

      GridBagHelper helper = new GridBagHelper(this);

      int y = 0;

      y = addDataDir(helper,y,Text.get(this,"s1"),requestDir);

      helper.addSpan(1,y,3,new JLabel(Text.get(this,"s8")));
      y++;
      helper.add(0,y,Box.createVerticalStrut(Text.getInt(this,"d1")));
      y++;

      y = addDataDir(helper,y,Text.get(this,"s2"),imageDir);

      helper.addSpan(0,y,4,new JLabel(Text.get(this,"s3")));
      y++;
      helper.add(1,y,mapImageDir); // can't use picker for remote path
      helper.addSpan(2,y,2,new JLabel(' ' + Text.get(this,"s4")));
      y++;

      helper.addSpan(1,y,3,new JLabel(Text.get(this,"s9")));
      y++;
      helper.add(0,y,Box.createVerticalStrut(Text.getInt(this,"d2")));
      y++;

      JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
      panel.add(prefix);
      panel.add(new JLabel(' ' + Text.get(this,"s6") + ' '));
      panel.add(useDigits);
      panel.add(new JLabel(' ' + Text.get(this,"s7")));

      helper.add(0,y,new JLabel(Text.get(this,"s5") + ' '));
      helper.addSpan(1,y,3,panel);
      y++;
   }

// --- implementation of ConfigFormat ---

   public Object getFormatConfig() { return config; }

   public void put() {

      if (config.requestDir != null)
         Field.put(requestDir,Convert.fromFile(config.requestDir));
      if (config.imageDir != null)
         Field.put(imageDir,Convert.fromFile(config.imageDir));
      Field.putNullable(mapImageDir,Convert.fromNullableFile(config.mapImageDir));

      Field.put(prefix,config.prefix);
      Field.put(useDigits,Convert.fromInt(config.useDigits));
   }

   public void get() throws ValidationException {

      config.requestDir = Convert.toFile(Field.get(requestDir));
      config.imageDir = Convert.toFile(Field.get(imageDir));
      config.mapImageDir = Convert.toNullableFile(Field.getNullable(mapImageDir));

      config.prefix = Field.get(prefix);
      config.useDigits = Convert.toInt(Field.get(useDigits));
   }

}

