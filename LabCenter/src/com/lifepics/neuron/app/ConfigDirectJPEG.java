/*
 * ConfigDirectJPEG.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.dendron.DirectJPEGConfig;
import com.lifepics.neuron.gui.Field;
import com.lifepics.neuron.gui.GridBagHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A panel class for editing direct-print JPEG settings.
 */

public class ConfigDirectJPEG extends ConfigFormat {

// --- fields ---

   private DirectJPEGConfig config;

   private ConfigSetup setup;
   private JRadioButton rotateCW;
   private JRadioButton rotateCCW;
   private JCheckBox tileEnable;
   private JTextField tilePixels;

// --- construction ---

   public ConfigDirectJPEG(Dialog owner, DirectJPEGConfig config) {
      super(owner);
      this.config = config;

   // fields

      setup = new ConfigSetup(owner,config.pageSetup);
      rotateCW = new JRadioButton(Text.get(this,"s2"));
      rotateCCW = new JRadioButton(Text.get(this,"s3"));
      tileEnable = new JCheckBox();
      tilePixels = new JTextField(Text.getInt(this,"d2"));

      ButtonGroup group = new ButtonGroup();
      group.add(rotateCW);
      group.add(rotateCCW);

   // layout

      GridBagHelper helper = new GridBagHelper(this);

      int y = 0;

      helper.addSpan(0,y,3,setup.getPanel(ConfigSetup.PANEL_BOTTOM));
      y++;

      helper.add(0,y,Box.createVerticalStrut(Text.getInt(this,"d1")));
      y++;

      helper.add(0,y,new JLabel(Text.get(this,"s1")));
      helper.add(1,y,rotateCW);
      helper.add(2,y,rotateCCW);
      y++;

      JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
      panel.add(tileEnable);
      panel.add(new JLabel(Text.get(this,"s4") + ' '));
      panel.add(tilePixels);
      panel.add(new JLabel(' ' + Text.get(this,"s5")));
      helper.addSpan(0,y,3,panel);
   }

// --- implementation of ConfigFormat ---

   public Object getFormatConfig() { return config; }

   public void put() {

      setup.put();

      if (config.rotateCW) rotateCW .setSelected(true);
      else                 rotateCCW.setSelected(true);

      Field.put(tileEnable,config.tileEnable);
      Field.put(tilePixels,Convert.fromInt(config.tilePixels));
   }

   public void get() throws ValidationException {

      setup.get();

      config.rotateCW = rotateCW.isSelected();

      config.tileEnable = Field.get(tileEnable);
      config.tilePixels = Convert.toInt(Field.get(tilePixels));
   }

   public JComboBox accessPrinterCombo() { return setup.accessPrinterCombo(); }

}

