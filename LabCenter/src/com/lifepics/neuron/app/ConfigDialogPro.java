/*
 * ConfigDialogPro.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.axon.PriceListTransaction;
import com.lifepics.neuron.axon.ProMode;
import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.gui.EditDialog;
import com.lifepics.neuron.gui.Field;
import com.lifepics.neuron.gui.GridBagHelper;
import com.lifepics.neuron.gui.IntervalField;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A dialog for editing configuration information.
 */

public class ConfigDialogPro extends EditDialog {

// --- fields ---

   private Config config;

   private JTabbedPane tabbedPane;

   // user - upload
   private ConfigUpload configUpload;

   // user - purge
   private JCheckBox autoPurgeManual;
   private IntervalField rollPurgeInterval;

   // user - refresh
   // no fields here

   // system
   private JTextField proEmail;
   private JTextField merchant;
   private JPasswordField password;

   // network
   private ConfigNetwork configNetwork;

// --- construction ---

   private static final String titleStatic = Text.get(ConfigDialogPro.class,"s1");

   /**
    * @param config An object that will be modified by the dialog.
    */
   public ConfigDialogPro(Frame owner, Config config) {
      this(owner,config,/* disableOK = */ false);
   }
   public ConfigDialogPro(Frame owner, Config config, boolean disableOK) {
      super(owner,titleStatic);

      this.config = config;

      construct(constructFields(),/* readonly = */ false,/* resizable = */ false,/* altOK = */ true,disableOK);
   }

   public ConfigDialogPro(Dialog owner, Config config) {
      super(owner,titleStatic);

      this.config = config;

      construct(constructFields(),/* readonly = */ false,/* resizable = */ false,/* altOK = */ true,/* disableOK = */ false);
   }

// --- memory ---

   private static Integer lastTab;

   public void dispose() {

      lastTab = new Integer(tabbedPane.getSelectedIndex());

      super.dispose();
   }

// --- methods ---

   private JComponent constructFields() {
      GridBagHelper helper;

   // fields

      configUpload = new ConfigUpload(/* includeCheckboxes = */ ProMode.isProNew(config.proMode));

      autoPurgeManual = new JCheckBox();
      rollPurgeInterval = new IntervalField(IntervalField.HOURS,IntervalField.DAYS);

      if (ProMode.isProOld(config.proMode)) {
         proEmail = new JTextField(Text.getInt(this,"w13"));
      }
      merchant = new JTextField(Text.getInt(this,"w2"));
      password = new JPasswordField(Text.getInt(this,"w3"));

      configNetwork = new ConfigNetwork(/* includeStartOverInterval = */ false);

   // user - purge

      JPanel panelPurge = new JPanel();
      panelPurge.setBorder(BorderFactory.createTitledBorder(Text.get(this,"s22")));

      helper = new GridBagHelper(panelPurge);

      helper.add(0,0,autoPurgeManual);
      helper.add(1,0,new JLabel(Text.get(this,"s23") + ' '));
      helper.add(2,0,rollPurgeInterval);

   // user - refresh

      JPanel panelRefresh = null;
      if (ProMode.isProOld(config.proMode)) {

         panelRefresh = new JPanel();
         panelRefresh.setBorder(BorderFactory.createTitledBorder(Text.get(this,"s146")));

         helper = new GridBagHelper(panelRefresh);

         JButton button = new JButton(Text.get(this,"s147"));
         button.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doRefresh(); } });
         helper.add(0,0,button);
      }

   // system

      JPanel panelSystem = new JPanel();
      panelSystem.setBorder(BorderFactory.createTitledBorder(Text.get(this,"s144")));

      helper = new GridBagHelper(panelSystem);

      if (ProMode.isProOld(config.proMode)) {

         helper.add(0,0,new JLabel(Text.get(this,"s145") + ' '));
         int d11 = Text.getInt(this,"d11");
         helper.add(0,1,Box.createVerticalStrut(d11));

         helper.add(1,0,proEmail);
      }

      helper.add(0,2,new JLabel(Text.get(this,"s7a")));
      helper.add(0,3,new JLabel(Text.get(this,"s7b") + ' '),GridBagHelper.alignRight);
      helper.add(0,4,new JLabel(Text.get(this,"s8") + ' '));

      helper.add(1,3,merchant);
      helper.add(1,4,password);

   // user tab

      JPanel tabUser = new JPanel();
      helper = new GridBagHelper(tabUser);

      helper.addFill(0,0,configUpload);
      helper.addFill(0,1,panelPurge);
      if (ProMode.isProOld(config.proMode)) helper.addFill(0,2,panelRefresh);
      helper.add    (0,3,new JLabel());

      helper.setRowWeight(3,1);
      helper.setColumnWeight(0,1);

   // system tab

      JPanel tabSystem = new JPanel();
      helper = new GridBagHelper(tabSystem);

      helper.addFill(0,0,panelSystem);
      helper.add    (0,1,new JLabel());

      helper.setRowWeight(1,1);
      helper.setColumnWeight(0,1);

   // network tab

      JPanel tabNetwork = new JPanel();
      helper = new GridBagHelper(tabNetwork);

      helper.addFill(0,0,configNetwork);
      helper.add    (0,1,new JLabel());

      helper.setRowWeight(1,1);
      helper.setColumnWeight(0,1);

   // overall

      tabbedPane = new JTabbedPane();

      tabbedPane.addTab(Text.get(this,"s28"),tabUser);
      tabbedPane.addTab(Text.get(this,"s30"),tabSystem);
      tabbedPane.addTab(Text.get(this,"s90"),tabNetwork);

      if (lastTab != null) tabbedPane.setSelectedIndex(lastTab.intValue());

      return tabbedPane;
   }

   protected void put() {

      configUpload.put(config);

      Field.put(autoPurgeManual,config.purgeConfig.autoPurgeManual);
      rollPurgeInterval.put(config.purgeConfig.rollPurgeInterval);

      if (ProMode.isProOld(config.proMode)) {
         Field.put(proEmail,config.proEmail);
      }
      Field.put(merchant,Convert.fromInt(config.merchantConfig.merchant));
      Field.put(password,config.merchantConfig.password);

      configNetwork.put(config);
   }

   private void getMerchantConfig() throws ValidationException {
      config.merchantConfig.merchant = Convert.toInt(Field.get(merchant));
      config.merchantConfig.password = Field.get(password);
   }

   protected void getAndValidate() throws ValidationException {

      configUpload.get(config);

      config.purgeConfig.autoPurgeManual = Field.get(autoPurgeManual);
      config.purgeConfig.rollPurgeInterval = rollPurgeInterval.get();

      if (ProMode.isProOld(config.proMode)) {
         config.proEmail = Field.get(proEmail);
      }
      getMerchantConfig();

      configNetwork.get(config);

      config.validate();
   }

// --- commands ---

   private void doRefresh() {

      try {
         getMerchantConfig();
      } catch (ValidationException e) {
      }
      // like ConfigDialog.doEditSKU; see comments there

      config.priceLists = PriceListTransaction.refresh(this,config.priceListURL,config.merchantConfig,config.priceLists);
      // the refresh function handles the UI and everything
   }

}

