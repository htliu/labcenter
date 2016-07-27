/*
 * ConfigPixel.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.dendron.PixelConfig;
import com.lifepics.neuron.gui.Field;
import com.lifepics.neuron.gui.GridBagHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A panel class for editing Pixel Magic settings.
 */

public class ConfigPixel extends ConfigFormat {

// --- fields ---

   private PixelConfig config;

   private JTextField dataDir;
   private JTextField mapDataDir;
   private JTextField baseURL;
   private JCheckBox separatorSheet;
   private JCheckBox allowMultiPane;
   private JComboBox deviceGroup;
   private JCheckBox printSingleSided;
   private JTextField cdOverflowLimit;
   private JTextField cdOverflowBytes;
   private JComboBox cdOverflowSplit;

// --- combo boxes ---

   private static Object group(int number) {
      return new LabeledNumber(number,Text.get(ConfigPixel.class,"dg" + Convert.fromInt(number)));
   }

   private static Object[] groupSet = new Object[] { "",
                                                     group(3),
                                                     group(5),
                                                     group(10),
                                                     group(12)  };
   // these are both names and values in the ordinary terminology

   private static Object[] splitNames = new Object[] { Text.get(ConfigPixel.class,"sp0"),
                                                       Text.get(ConfigPixel.class,"sp1")  };
   private static int[] splitValues = new int[] { 0, 1 };

// --- construction ---

   public ConfigPixel(Dialog owner, PixelConfig config) {
      super(owner);
      this.config = config;

      dataDir = constructDataDir();
      mapDataDir = constructDataDir();
      baseURL = new JTextField(Text.getInt(this,"w1"));
      separatorSheet = new JCheckBox();
      allowMultiPane = new JCheckBox();
      deviceGroup = new JComboBox(groupSet);
      deviceGroup.setEditable(true);
      printSingleSided = new JCheckBox(Text.get(this,"s7"));
      int w2 = Text.getInt(this,"w2");
      cdOverflowLimit = new JTextField(w2);
      cdOverflowBytes = new JTextField(w2);
      cdOverflowSplit = new JComboBox(splitNames);

   // limit panel

      JPanel panelSplit = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
      panelSplit.add(new JLabel(Text.get(this,"s12") + ' '));
      panelSplit.add(cdOverflowSplit);

      JPanel panelLimit = new JPanel();
      GridBagHelper helper = new GridBagHelper(panelLimit);

      helper.addSpan(0,0,3,new JLabel(Text.get(this,"s9")));

      helper.add(0,1,Box.createHorizontalStrut(Text.getInt(this,"d4")));

      helper.add(1,1,cdOverflowLimit);
      helper.add(2,1,new JLabel(' ' + Text.get(this,"s10")));

      helper.add(1,2,cdOverflowBytes);
      helper.add(2,2,new JLabel(' ' + Text.get(this,"s11")));

      helper.addSpan(0,3,3,panelSplit);

   // main panel

      helper = new GridBagHelper(this);

      int y = 0;

      helper.add    (0,y,  new JLabel(Text.get(this,"s1") + ' '));
      helper.addSpan(1,y,2,baseURL);
      y++;

      helper.addSpan(0,y,2,new JLabel(Text.get(this,"s2") + ' '));
      helper.add    (2,y,  separatorSheet);
      y++;

      helper.addSpan(0,y,2,new JLabel(Text.get(this,"s6") + ' '));
      helper.add    (2,y,  allowMultiPane);
      y++;

      helper.add    (0,y,  Box.createVerticalStrut(Text.getInt(this,"d1")));
      y++;

      helper.addSpan(0,y,3,new JLabel(Text.get(this,"s3")));
      y++;
      helper.addSpan(1,y,2,dataDir);
      y++;

      helper.addSpan(0,y,3,new JLabel(Text.get(this,"s4")));
      y++;
      helper.addSpan(1,y,2,mapDataDir);
      y++;

      helper.add    (0,y,  Box.createVerticalStrut(Text.getInt(this,"d2")));
      y++;

      helper.addSpan(0,y,3,new JLabel(Text.get(this,"s5")));
      y++;
      helper.addSpan(1,y,2,deviceGroup);
      y++;

      helper.addSpan(0,y,3,printSingleSided);
      y++;
      helper.addSpan(1,y,2,new JLabel(Text.get(this,"s8")));
      y++;

      helper.add    (0,y,  Box.createVerticalStrut(Text.getInt(this,"d3")));
      y++;

      helper.addSpan(0,y,3,panelLimit);
      y++;
   }

// --- implementation of ConfigFormat ---

   public Object getFormatConfig() { return config; }

   public void put() {

      if (config.dataDir != null) // happens at loadDefault
         Field.put(dataDir,Convert.fromFile(config.dataDir));
      Field.putNullable(mapDataDir,Convert.fromNullableFile(config.mapDataDir));
      if (config.baseURL != null)
         Field.put(baseURL,config.baseURL);
      Field.put(separatorSheet,config.separatorSheet);
      Field.put(allowMultiPane,config.allowMultiPane);
      deviceGroup.setSelectedItem(fromNumber(groupSet,config.deviceGroup));
      Field.put(printSingleSided,config.printSingleSided);

      Integer limit = (config.cdOverflowLimit != 0) ? new Integer(config.cdOverflowLimit) : null;
      Field.putNullable(cdOverflowLimit,Convert.fromNullableInt(limit));

      Long bytes = (config.cdOverflowBytes != 0) ? new Long(config.cdOverflowBytes) : null;
      Field.putNullable(cdOverflowBytes,Convert.fromNullableLong(bytes));

      int split = config.cdOverflowSplit ? 1 : 0;
      Field.put(cdOverflowSplit,splitValues,split);
   }

   public void get() throws ValidationException {

      config.dataDir = Convert.toFile(Field.get(dataDir));
      config.mapDataDir = Convert.toNullableFile(Field.getNullable(mapDataDir));
      config.baseURL = Field.get(baseURL);
      config.separatorSheet = Field.get(separatorSheet);
      config.allowMultiPane = Field.get(allowMultiPane);
      config.deviceGroup = toNumber(deviceGroup.getSelectedItem());
      config.printSingleSided = Field.get(printSingleSided);

      Integer limit = Convert.toNullableInt(Field.getNullable(cdOverflowLimit));
      config.cdOverflowLimit = (limit != null) ? limit.intValue() : 0;

      Long bytes = Convert.toNullableLong(Field.getNullable(cdOverflowBytes));
      config.cdOverflowBytes = (bytes != null) ? bytes.longValue() : 0;

      int split = Field.get(cdOverflowSplit,splitValues);
      config.cdOverflowSplit = (split != 0);
   }

}

