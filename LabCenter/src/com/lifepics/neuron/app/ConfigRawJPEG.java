/*
 * ConfigRawJPEG.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.dendron.RawJPEGConfig;
import com.lifepics.neuron.gui.Field;
import com.lifepics.neuron.gui.GridBagHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A panel class for editing direct-print JPEG settings.
 */

public class ConfigRawJPEG extends ConfigFormat {

// --- fields ---

   private RawJPEGConfig config;

   private ConfigPJL pjl;
   private JComboBox language;
   private JComboBox scaling;

// --- combo boxes ---

   private static Object[] scalingNames = new Object[] { Text.get(ConfigRawJPEG.class,"sc0"),
                                                         Text.get(ConfigRawJPEG.class,"sc1"),
                                                         Text.get(ConfigRawJPEG.class,"sc2")  };
   private static int[] scalingValues = new int[] { 0, 1, -1 };

// --- construction ---

   public ConfigRawJPEG(Dialog owner, RawJPEGConfig config) {
      super(owner);
      this.config = config;

      pjl = new ConfigPJL(owner,config);
      language = new JComboBox(new Object[] { RawJPEGConfig.LANGUAGE_AUTO, RawJPEGConfig.LANGUAGE_JPEG });
      language.setEditable(true);
      scaling = new JComboBox(scalingNames);

   // layout

      GridBagHelper helper = new GridBagHelper(this);
      int y = 0;

      y = pjl.doLayout1(helper,y,/* showOrientation = */ false);

      helper.add(0,y,new JLabel(Text.get(this,"s1") + ' '));
      helper.add(1,y,language);
      y++;

      helper.add(0,y,new JLabel(Text.get(this,"s2") + ' '));
      helper.add(1,y,scaling);
      y++;

      y = pjl.doLayoutS(helper,y);
      y = pjl.doLayout2(helper,y);
   }

// --- implementation of ConfigFormat ---

   public Object getFormatConfig() { return config; }

   public void put() {
      pjl.put();
      language.setSelectedItem(config.language);

      int temp = config.scaleEnable ? (config.scaleRoundDown ? -1 : 1) : 0;
      Field.put(scaling,scalingValues,temp);
   }

   public void get() throws ValidationException {
      pjl.get();
      config.language = ((String) language.getSelectedItem()).trim();

      int temp = Field.get(scaling,scalingValues);
      config.scaleEnable    = (temp != 0);
      config.scaleRoundDown = (temp <  0);
   }

   public JComboBox accessPrinterCombo() { return pjl.accessPrinterCombo(); }

}

