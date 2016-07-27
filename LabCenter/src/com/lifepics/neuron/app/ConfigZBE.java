/*
 * ConfigZBE.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.dendron.ZBEConfig;
import com.lifepics.neuron.gui.Field;
import com.lifepics.neuron.gui.GridBagHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A panel class for editing ZBE settings.
 */

public class ConfigZBE extends ConfigFormat {

// --- fields ---

   private ZBEConfig config;

   private JTextField requestDir;
   private JTextField imageDir;
   private JTextField mapImageDir;
   private JTextField productRoot;

   private JTextField prefix;
   private JCheckBox includeCustomer;
   private JComboBox submitOnHold;
   private JComboBox submitForReview;
   private JComboBox units;

   private ConfigBackprint backprint;

// --- combo boxes ---

   private static Object[] unitsNames = new Object[] { Text.get(ConfigZBE.class,"un0"),
                                                       Text.get(ConfigZBE.class,"un1"),
                                                       Text.get(ConfigZBE.class,"un2")  };
   private static int[] unitsValues = new int[] { ZBEConfig.UNITS_INCH,
                                                  ZBEConfig.UNITS_CM,
                                                  ZBEConfig.UNITS_300DPI };

// --- construction ---

   public ConfigZBE(Dialog owner, ZBEConfig config) {
      super(owner);
      this.config = config;

   // fields

      requestDir = constructDataDir();
      imageDir = constructDataDir();
      mapImageDir = constructDataDir();
      productRoot = constructDataDir();

      prefix = new JTextField(Text.getInt(this,"w1"));
      includeCustomer = new JCheckBox();
      submitOnHold = new JComboBox(nullableBooleanNames);
      submitForReview = new JComboBox(nullableBooleanNames);
      units = new JComboBox(unitsNames);

      backprint = new ConfigBackprint(config.backprints);

   // subpanel

      JPanel panel = new JPanel();
      GridBagHelper helper = new GridBagHelper(panel);

      helper.addSpan(0,0,2,new JLabel(Text.get(this,"s5") + ' '));
      helper.add(2,0,prefix);

      helper.add(0,1,includeCustomer);
      helper.addSpan(1,1,2,new JLabel(Text.get(this,"s6")));
      helper.addSpan(0,2,3,new JLabel(Text.get(this,"s7")));

      helper.add(3,0,Box.createHorizontalStrut(Text.getInt(this,"d2")));

      helper.addSpan(4,0,2,new JLabel(Text.get(this,"s8") + ' '));
      helper.add(6,0,submitOnHold);

      helper.addSpan(4,1,2,new JLabel(Text.get(this,"s9") + ' '));
      helper.add(6,1,submitForReview);

      GridBagConstraints constraints = new GridBagConstraints();
      constraints.gridwidth = 2;
      constraints.anchor = GridBagConstraints.EAST;

      helper.add(4,2,new JLabel(Text.get(this,"s10") + ' '));
      helper.add(5,2,units,constraints);

   // layout

      helper = new GridBagHelper(this);

      int y = 0;

      y = addDataDir(helper,y,Text.get(this,"s1"),requestDir);
      y = addDataDir(helper,y,Text.get(this,"s2"),imageDir);

      helper.addSpan(0,y,4,new JLabel(Text.get(this,"s3")));
      y++;
      helper.add(1,y,mapImageDir); // can't use picker for remote path
      helper.addSpan(2,y,2,new JLabel(' ' + Text.get(this,"s4")));
      y++;

      helper.addSpan(0,y,4,new JLabel(Text.get(this,"s11")));
      y++;
      helper.add(1,y,productRoot); // can't use picker for remote path
      helper.addSpan(2,y,2,new JLabel(' ' + Text.get(this,"s12")));
      y++;

      helper.add(0,y,Box.createVerticalStrut(Text.getInt(this,"d1")));
      y++;

      helper.addSpan(0,y,4,panel);
      y++;

      y = backprint.add(helper,y);
   }

// --- implementation of ConfigFormat ---

   public Object getFormatConfig() { return config; }

   public void put() {

      if (config.requestDir != null)
         Field.put(requestDir,Convert.fromFile(config.requestDir));
      if (config.imageDir != null)
         Field.put(imageDir,Convert.fromFile(config.imageDir));
      Field.putNullable(mapImageDir,Convert.fromNullableFile(config.mapImageDir));
      Field.putNullable(productRoot,Convert.fromNullableFile(config.productRoot));

      Field.put(prefix,config.prefix);
      Field.put(includeCustomer,config.includeCustomer);
      Field.put(submitOnHold,   nullableBooleanValues,nbToInt(config.submitOnHold   ));
      Field.put(submitForReview,nullableBooleanValues,nbToInt(config.submitForReview));
      Field.put(units,unitsValues,config.units);

      backprint.put();
   }

   public void get() throws ValidationException {

      config.requestDir = Convert.toFile(Field.get(requestDir));
      config.imageDir = Convert.toFile(Field.get(imageDir));
      config.mapImageDir = Convert.toNullableFile(Field.getNullable(mapImageDir));
      config.productRoot = Convert.toNullableFile(Field.getNullable(productRoot));

      config.prefix = Field.get(prefix);
      config.includeCustomer = Field.get(includeCustomer);
      config.submitOnHold    = intToNb(Field.get(submitOnHold,   nullableBooleanValues));
      config.submitForReview = intToNb(Field.get(submitForReview,nullableBooleanValues));
      config.units = Field.get(units,unitsValues);

      backprint.get();
   }

}

