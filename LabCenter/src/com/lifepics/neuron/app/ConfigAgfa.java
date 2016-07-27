/*
 * ConfigAgfa.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.dendron.AgfaConfig;
import com.lifepics.neuron.gui.EditDialog;
import com.lifepics.neuron.gui.Field;
import com.lifepics.neuron.gui.GridBagHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A panel class for editing Agfa settings.
 */

public class ConfigAgfa extends ConfigFormat {

// --- fields ---

   private AgfaConfig config;

   private JTextField databaseDir;
   private JTextField host;
   private JTextField service;
   private JTextField sourceDeviceID;
   private JComboBox  targetDeviceID;
   private ConfigBackprint backprint;

// --- combo boxes ---

   private static Object device(int number) {
      return new LabeledNumber(number,Text.get(ConfigAgfa.class,"dev" + Convert.fromInt(number)));
   }

   private static Object[] deviceSet = new Object[] { "",
                                                      device(80),
                                                      device(70),
                                                      device(60)  };
   // these are both names and values in the ordinary terminology

// --- construction ---

   public ConfigAgfa(Dialog owner, boolean border, AgfaConfig config) {
      super(owner);
      this.config = config;

      int w5 = Text.getInt(this,"w5");

      databaseDir = constructDataDir();
      host = new JTextField(Text.getInt(this,"w1"));
      service = new JTextField(Text.getInt(this,"w2"));
      sourceDeviceID = new JTextField(w5);
      targetDeviceID = new JComboBox(deviceSet);
      targetDeviceID.setEditable(true);
      backprint = new ConfigBackprint(config.backprints);

      if (border) setBorder(BorderFactory.createTitledBorder(Text.get(this,"s1")));

   // subpanel

      JPanel subpanel = new JPanel();
      GridBagHelper helper = new GridBagHelper(subpanel);

      int d1 = Text.getInt(this,"d1");

      helper.add(0,0,Box.createVerticalStrut(d1));

      JButton button = new JButton(Text.get(this,"s13"));
      button.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doAdvancedDialog(); } });
      helper.addSpan(1,1,2,button);
      ConfigFormat.addSpacer(helper,1);

      helper.add(0,2,Box.createVerticalStrut(d1));

      helper.addSpan(0,3,2,new JLabel(Text.get(this,"s8") + ' '));
      helper.add    (2,3,  sourceDeviceID);

      helper.addSpan(0,4,2,new JLabel(Text.get(this,"s9") + ' '));
      helper.add    (2,4,  targetDeviceID);

      helper.setColumnWeight(2,1);

   // main panel

      helper = new GridBagHelper(this);

      int y = 0;

      y = addDataDir(helper,y,Text.get(this,"s10"),databaseDir);

      helper.add(0,y,new JLabel(Text.get(this,"s11") + ' '));
      helper.addSpan(1,y,3,host);
      y++;

      helper.add(0,y,new JLabel(Text.get(this,"s12") + ' '));
      helper.addSpan(1,y,3,service);
      y++;

      helper.addSpanFill(0,y,4,subpanel);
      y++;

      y = backprint.add(helper,y);
   }

// --- implementation of ConfigFormat ---

   public Object getFormatConfig() { return config; }

   public void put() {

      // all these nulls happen only at loadDefault
      if (config.databaseDir != null)
         Field.put(databaseDir,Convert.fromFile(config.databaseDir));
      if (config.host != null)
         Field.put(host,config.host);
      if (config.service != null)
         Field.put(service,config.service);

      Field.put(sourceDeviceID,Convert.fromInt(config.sourceDeviceID));
      targetDeviceID.setSelectedItem(fromNumber(deviceSet,config.targetDeviceID));

      backprint.put();
   }

   public void get() throws ValidationException {

      config.databaseDir = Convert.toFile(Field.get(databaseDir));
      config.host = Field.get(host);
      config.service = Field.get(service);
      config.sourceDeviceID = Convert.toInt(Field.get(sourceDeviceID));
      config.targetDeviceID = toNumber(targetDeviceID.getSelectedItem());

      backprint.get();
   }

// --- subdialog ---

   private void doAdvancedDialog() {
      new AdvancedDialog().run();
      // changes will be put into the config object, not committed until top-level OK
   }

   private class AdvancedDialog extends EditDialog {

   // --- fields ---

      private JTextField dealerNumber;
      private JTextField dealerName;
      private JTextField operatorNumber;
      private JTextField operatorName;

   // --- construction ---

      public AdvancedDialog() {
         super(ConfigAgfa.this.getOwner(),Text.get(ConfigAgfa.class,"s14"));
         construct(constructFields(),/* readonly = */ false);
      }

   // --- methods ---

      private String get(String key) { return Text.get(ConfigAgfa.class,key); }

      private JPanel constructFields() {

         int w3 = Text.getInt(ConfigAgfa.class,"w3");
         int w4 = Text.getInt(ConfigAgfa.class,"w4");

         dealerNumber = new JTextField(w3);
         dealerName = new JTextField(w4);
         operatorNumber = new JTextField(w3);
         operatorName = new JTextField(w4);

         JPanel fields = new JPanel();
         GridBagHelper helper = new GridBagHelper(fields);

         helper.add(0,0,new JLabel(get("s2") + ' '));
         helper.add(1,0,dealerNumber);
         helper.add(2,0,new JLabel(' ' + get("s4") + ' '));
         helper.add(3,0,dealerName);

         helper.add(0,1,new JLabel(get("s5") + ' '));
         helper.add(1,1,operatorNumber);
         helper.add(2,1,new JLabel(' ' + get("s7") + ' '));
         helper.add(3,1,operatorName);

         return fields;
      }

      protected void put() {

         Field.put(dealerNumber,config.dealerNumber);
         Field.put(dealerName,config.dealerName);
         Field.put(operatorNumber,Convert.fromInt(config.operatorNumber));
         Field.put(operatorName,config.operatorName);
      }

      protected void getAndValidate() throws ValidationException {

         int temp = Convert.toInt(Field.get(operatorNumber));
         //
         // do this first, so that error followed by cancellation
         // doesn't leave garbage written into the config object

         config.dealerNumber = Field.get(dealerNumber);
         config.dealerName = Field.get(dealerName);
         config.operatorNumber = temp;
         config.operatorName = Field.get(operatorName);
      }
   }

}

