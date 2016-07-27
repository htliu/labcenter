/*
 * ConfigNoritsu.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.dendron.NoritsuConfig;
import com.lifepics.neuron.gui.Field;
import com.lifepics.neuron.gui.GridBagHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A panel class for editing Noritsu settings.
 */

public class ConfigNoritsu extends ConfigFormat {

// --- fields ---

   private NoritsuConfig config;

   private JTextField dataDir;
   private JRadioButton strictFalse;
   private JRadioButton strictTrue;
   private JCheckBox limitEnable;
   private JTextField limitLength;
   private JCheckBox printSingleSided;
   private JCheckBox collateInReverse;
   private ConfigBackprint backprint;

// --- construction ---

   public ConfigNoritsu(Dialog owner, boolean border, NoritsuConfig config) {
      super(owner);
      this.config = config;

      dataDir = constructDataDir();
      strictFalse = new JRadioButton(Text.get(this,"s34"));
      strictTrue  = new JRadioButton(Text.get(this,"s36"));
      limitEnable = constructLimitEnable();
      limitLength = constructLimitLength();
      printSingleSided = new JCheckBox(Text.get(this,"s38"));
      collateInReverse = new JCheckBox();
      backprint = new ConfigBackprint(config.backprints);

      ButtonGroup group = new ButtonGroup();
      group.add(strictFalse);
      group.add(strictTrue );

      if (border) setBorder(BorderFactory.createTitledBorder(Text.get(this,"s31")));

   // mini panel

      JPanel panel = new JPanel();
      GridBagHelper helper = new GridBagHelper(panel);

      helper.add(0,0,strictFalse);
      helper.add(1,0,new JLabel(Text.get(this,"s35")));
      helper.add(0,1,strictTrue );
      helper.add(1,1,new JLabel(Text.get(this,"s37")));

   // main panel

      helper = new GridBagHelper(this);

      int y = 0;

      y = addDataDir(helper,y,Text.get(this,"s32"),dataDir);

      y = addLengthLimit(helper,y,limitEnable,limitLength);

      helper.addSpan(0,y,4,new JLabel(Text.get(this,"s33")));
      y++;

      helper.addSpan(1,y,3,panel);
      y++;

      helper.addSpan(0,y,4,printSingleSided);
      y++;

      panel = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
      panel.add(new JLabel(Text.get(this,"s39")));
      panel.add(collateInReverse);
      panel.add(new JLabel(Text.get(this,"s40")));

      helper.addSpan(1,y,3,panel);
      y++;

      y = backprint.add(helper,y,/* spacer = */ true);
   }

// --- implementation of ConfigFormat ---

   public Object getFormatConfig() { return config; }

   public void put() {
      if (config.dataDir != null) // happens at loadDefault
         Field.put(dataDir,Convert.fromFile(config.dataDir));
      if (config.strict) strictTrue .setSelected(true);
      else               strictFalse.setSelected(true);
      Field.put(limitEnable,config.limitEnable);
      Field.put(limitLength,Convert.fromInt(config.limitLength));
      Field.put(printSingleSided,config.printSingleSided);
      Field.put(collateInReverse,config.collateInReverse);
      backprint.put();
   }

   public void get() throws ValidationException {
      config.dataDir = Convert.toFile(Field.get(dataDir));
      config.strict = strictTrue.isSelected();
      config.limitEnable = Field.get(limitEnable);
      config.limitLength = Convert.toInt(Field.get(limitLength));
      config.printSingleSided = Field.get(printSingleSided);
      config.collateInReverse = Field.get(collateInReverse);
      backprint.get();
   }

}

