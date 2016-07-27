/*
 * ConfigDP2.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.dendron.DP2Config;
import com.lifepics.neuron.gui.Field;
import com.lifepics.neuron.gui.GridBagHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A panel class for editing DP2 settings.
 */

public class ConfigDP2 extends ConfigFormat {

// --- fields ---

   private DP2Config config;

   private JTextField requestDir;
   private JTextField imageDir;
   private JTextField mapImageDir;

   private JComboBox includeFile;
   private JTextField prefix;
   private JTextField customerID;
   private JComboBox status;
   private JTextField rollName;

   private JRadioButton enableRealNo;
   private JRadioButton enableRealYes;
   private JTextField prefixRealID;

   private JCheckBox customIntegration;
   private JTextField customerType;
   private JCheckBox includeShipCompany;

// --- construction ---

   public ConfigDP2(Dialog owner, DP2Config config) {
      super(owner);
      this.config = config;

   // fields

      requestDir = constructDataDir();
      imageDir = constructDataDir();
      mapImageDir = constructDataDir();

      includeFile = new JComboBox(DP2Config.includeSet);
      includeFile.setEditable(true);
      prefix = new JTextField(Text.getInt(this,"w1"));
      customerID = new JTextField(Text.getInt(this,"w2"));
      status = new JComboBox(DP2Config.statusSet);
      status.setEditable(true);
      rollName = new JTextField(Text.getInt(this,"w3"));

      enableRealNo  = new JRadioButton();
      enableRealYes = new JRadioButton();
      prefixRealID = new JTextField(Text.getInt(this,"w5"));

      ButtonGroup group = new ButtonGroup();
      group.add(enableRealNo);
      group.add(enableRealYes);

      customIntegration = new JCheckBox(Text.get(this,"s10"));
      customerType = new JTextField(Text.getInt(this,"w4"));
      includeShipCompany = new JCheckBox();

   // subpanels

      JPanel panel = new JPanel();
      GridBagHelper helper = new GridBagHelper(panel);

      helper.add    (0,0,  enableRealNo);
      helper.addSpan(1,0,2,customerID);
      helper.add    (3,0,  new JLabel(' ' + Text.get(this,"s12")));

      helper.add    (0,1,  enableRealYes);
      helper.add    (1,1,  prefixRealID);
      helper.addSpan(2,1,2,new JLabel(' ' + Text.get(this,"s13")));

      JPanel panel2 = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
      panel2.add(new JLabel(Text.get(this,"s14")));
      panel2.add(includeShipCompany);

   // layout

      helper = new GridBagHelper(this);

      int y = 0;

      y = addDataDir(helper,y,Text.get(this,"s1"),requestDir);
      y = addDataDir(helper,y,Text.get(this,"s2"),imageDir);

      helper.addSpan(0,y,4,new JLabel(Text.get(this,"s3")));
      y++;
      helper.add(1,y,mapImageDir); // can't use picker for path from DP2
      helper.addSpan(2,y,2,new JLabel(' ' + Text.get(this,"s4")));
      y++;

      helper.add(0,y,Box.createVerticalStrut(Text.getInt(this,"d1")));
      y++;

      helper.add(0,y,new JLabel(Text.get(this,"s9") + ' '));
      helper.addSpan(1,y,3,includeFile);
      y++;

      helper.add(0,y,new JLabel(Text.get(this,"s5") + ' '));
      helper.addSpan(1,y,3,prefix);
      y++;

      helper.add(0,y,new JLabel(Text.get(this,"s6") + ' '),GridBagHelper.alignTopLeft);
      helper.addSpan(1,y,3,panel);
      y++;

      helper.add(0,y,new JLabel(Text.get(this,"s7") + ' '));
      helper.addSpan(1,y,3,status);
      y++;

      helper.add(0,y,new JLabel(Text.get(this,"s8") + ' '));
      helper.addSpan(1,y,3,rollName);
      y++;

      helper.add(0,y,Box.createVerticalStrut(Text.getInt(this,"d2")));
      y++;

      helper.addSpan(0,y,4,customIntegration);
      y++;

      helper.add(0,y,new JLabel(Text.get(this,"s11") + ' '));
      helper.addSpan(1,y,3,customerType);
      y++;

      helper.addSpan(0,y,4,panel2);
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

      includeFile.setSelectedItem(config.includeFile);
      Field.put(prefix,config.prefix);
      Field.put(customerID,config.customerID);
      status.setSelectedItem(config.status);
      Field.put(rollName,config.rollName);

      if (config.enableRealID) enableRealYes.setSelected(true);
      else                     enableRealNo .setSelected(true);
      Field.put(prefixRealID,config.prefixRealID);

      Field.put(customIntegration,config.customIntegration);
      Field.put(customerType,config.customerType);
      Field.put(includeShipCompany,config.includeShipCompany);
   }

   public void get() throws ValidationException {

      config.requestDir = Convert.toFile(Field.get(requestDir));
      config.imageDir = Convert.toFile(Field.get(imageDir));
      config.mapImageDir = Convert.toNullableFile(Field.getNullable(mapImageDir));

      config.includeFile = (String) includeFile.getSelectedItem();
      config.prefix = Field.get(prefix);
      config.customerID = Field.get(customerID);
      config.status = (String) status.getSelectedItem();
      config.rollName = Field.get(rollName);

      config.enableRealID = enableRealYes.isSelected();
      config.prefixRealID = Field.get(prefixRealID);

      config.customIntegration = Field.get(customIntegration);
      config.customerType = Field.get(customerType);
      config.includeShipCompany = Field.get(includeShipCompany);
   }

}

