/*
 * ConfigFujiNew.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Pop;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.dendron.FujiNewConfig;
import com.lifepics.neuron.gui.EditDialog;
import com.lifepics.neuron.gui.Field;
import com.lifepics.neuron.gui.GridBagHelper;
import com.lifepics.neuron.gui.IntervalField;

import java.io.File;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A panel class for editing Fuji PIC 2.6 settings.
 */

public class ConfigFujiNew extends ConfigFormat {

// --- fields ---

   private FujiNewConfig config;

   private File tempRequestDir;
   private File tempImageDir;
   private File tempMapImageDir;
   private String tempPrefix;
   private boolean tempUseOrderSource;
   private boolean tempUseOrderTypeIn;
   private String tempOrderSource;
   private String tempOrderTypeIn;
   private boolean tempProMode;
   private long tempProModeWaitInterval;
   private File tempListDirectory;

   // fields for simple configuration
   private JTextField requestDirRoot;
   private String     requestDirRest;
   private JTextField mapImageDirRoot;
   private String     mapImageDirRest;

   private JCheckBox limitEnable;
   private JTextField limitLength;

// --- construction ---

   public ConfigFujiNew(Dialog owner, boolean border, FujiNewConfig config) {
      super(owner);
      this.config = config;

      requestDirRoot = constructDataDir();
      mapImageDirRoot = constructDataDir();
      limitEnable = constructLimitEnable();
      limitLength = constructLimitLength();

      if (border) setBorder(BorderFactory.createTitledBorder(Text.get(this,"s1")));

      GridBagHelper helper = new GridBagHelper(this);

      int y = 0;

      int d1 = Text.getInt(this,"d1");

      y = addLengthLimit(helper,y,limitEnable,limitLength);

      helper.addSpan(0,y,4,new JLabel(Text.get(this,"s2")));
      y++;
      helper.add(1,y,requestDirRoot);
      y++;
      helper.addSpan(1,y,3,new JLabel(Text.get(this,"s3")));
      y++;

      helper.add(0,y,Box.createVerticalStrut(d1));
      y++;

      helper.addSpan(0,y,4,new JLabel(Text.get(this,"s4")));
      y++;
      helper.add(1,y,mapImageDirRoot);
      y++;
      helper.addSpan(1,y,3,new JLabel(Text.get(this,"s5")));
      y++;

      helper.add(0,y,Box.createVerticalStrut(d1));
      y++;

      JButton button = new JButton(Text.get(this,"s6"));
      button.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doAdvancedDialog(); } });
      helper.add(1,y,button);
      y++;

      ConfigFormat.addSpacer(helper,y);
   }

// --- transfer primitives ---

   private void tempToConfig() {
      config.requestDir = tempRequestDir;
      config.imageDir = tempImageDir;
      config.mapImageDir = tempMapImageDir;
      config.prefix = tempPrefix;
      config.useOrderSource = tempUseOrderSource;
      config.useOrderTypeIn = tempUseOrderTypeIn;
      config.orderSource = tempOrderSource;
      config.orderTypeIn = tempOrderTypeIn;
      config.proMode = tempProMode;
      config.proModeWaitInterval = tempProModeWaitInterval;
      config.listDirectory = tempListDirectory;
   }

   private void configToTemp() {
      tempRequestDir = config.requestDir;
      tempImageDir = config.imageDir;
      tempMapImageDir = config.mapImageDir;
      tempPrefix = config.prefix;
      tempUseOrderSource = config.useOrderSource;
      tempUseOrderTypeIn = config.useOrderTypeIn;
      tempOrderSource = config.orderSource;
      tempOrderTypeIn = config.orderTypeIn;
      tempProMode = config.proMode;
      tempProModeWaitInterval = config.proModeWaitInterval;
      tempListDirectory = config.listDirectory;
   }

   private void tempToSimple() {
      Field.putNullable(requestDirRoot,ConfigFuji.splitRoot(tempRequestDir));
      requestDirRest = ConfigFuji.splitRest(tempRequestDir);
      Field.putNullable(mapImageDirRoot,ConfigFuji.splitRoot(tempMapImageDir));
      mapImageDirRest = ConfigFuji.splitRest(tempMapImageDir);

      // neither field is nullable, but split and join use nulls as signals.
      // the nulls are caught during local validation, below.
   }

   private void simpleToTemp() throws ValidationException {
      tempRequestDir = ConfigFuji.join(Field.getNullable(requestDirRoot),requestDirRest);
      tempMapImageDir = ConfigFuji.join(Field.getNullable(mapImageDirRoot),mapImageDirRest);
   }

// --- implementation of ConfigFormat ---

   public Object getFormatConfig() { return config; }

   public void put() {

      configToTemp();
      tempToSimple();

      Field.put(limitEnable,config.limitEnable);
      Field.put(limitLength,Convert.fromInt(config.limitLength));
   }

   public void get() throws ValidationException {

      simpleToTemp();
      tempToConfig();
      if (config.requestDir  == null) throw new ValidationException(Text.get(this,"e1"));
      if (config.mapImageDir == null) throw new ValidationException(Text.get(this,"e2"));
      if (config.listDirectory == null) throw new ValidationException(Text.get(this,"e3"));

      config.limitEnable = Field.get(limitEnable);
      config.limitLength = Convert.toInt(Field.get(limitLength));
   }

// --- subdialog ---

   private void doAdvancedDialog() {

      try {
         simpleToTemp();
      } catch (ValidationException e) {
         Pop.error(this,e,Text.get(this,"s7"));
         return;
      }

      if (new AdvancedDialog().run()) {
         tempToSimple();
      }
   }

   // non-static so it can mess with the temp fields.
   // there is no nullness validation at this level.

   private class AdvancedDialog extends EditDialog {

   // --- fields ---

      private JTextField requestDir;
      private JTextField imageDir;
      private JTextField mapImageDir;
      private JTextField prefix;
      private JCheckBox useOrderSource;
      private JCheckBox useOrderTypeIn;
      private JTextField orderSource;
      private JTextField orderTypeIn;
      private JCheckBox proMode;
      private IntervalField proModeWaitInterval;
      private JTextField listDirectory;

   // --- construction ---

      public AdvancedDialog() {
         super(ConfigFujiNew.this.getOwner(),Text.get(ConfigFujiNew.class,"s8"));
         construct(constructFields(),/* readonly = */ false);
      }

   // --- methods ---

      private String get(String key) { return Text.get(ConfigFujiNew.class,key); }

      private JPanel constructFields() {

         int w1 = Text.getInt(ConfigFujiNew.class,"w1");
         int w2 = Text.getInt(ConfigFujiNew.class,"w2");

         requestDir = constructDataDir();
         imageDir = constructDataDir();
         mapImageDir = constructDataDir();
         prefix = new JTextField(w1);
         useOrderSource = new JCheckBox(get("s21"));
         useOrderTypeIn = new JCheckBox(get("s22"));
         orderSource = new JTextField(w2);
         orderTypeIn = new JTextField(w2);
         proMode = new JCheckBox(get("s23"));
         proModeWaitInterval = new IntervalField(IntervalField.MILLIS,IntervalField.SECONDS);
         listDirectory = constructDataDir();

         JPanel fields = new JPanel();
         GridBagHelper helper = new GridBagHelper(fields);

         int y = 0;

         int d3 = Text.getInt(ConfigFujiNew.class,"d3");
         int d4 = Text.getInt(ConfigFujiNew.class,"d4");

         JLabel header = new JLabel(get("s9"));
         Font bold = header.getFont().deriveFont(Font.BOLD);
         header.setFont(bold);
         helper.addSpan(0,y,4,header);
         y++;

         helper.add(0,y,Box.createVerticalStrut(d3));
         y++;

         helper.add(0,y,new JLabel(get("s10") + ' '));
         helper.add(1,y,requestDir);
         helper.add(2,y,new JLabel(' ' + get("s11") + ' '));
         helper.add(3,y,new JLabel(get("s12")));
         y++;
         helper.add(0,y,new JLabel(get("s26") + ' '));
         helper.add(1,y,listDirectory);
         helper.add(3,y,new JLabel(get("s27")));
         y++;
         helper.add(0,y,new JLabel(get("s13") + ' '));
         helper.add(1,y,imageDir);
         helper.add(3,y,new JLabel(get("s14")));
         y++;

         helper.add(0,y,Box.createVerticalStrut(d4));
         y++;

         header = new JLabel(get("s15"));
         header.setFont(bold);
         helper.addSpan(0,y,4,header);
         y++;

         helper.add(0,y,Box.createVerticalStrut(d3));
         y++;

         helper.add(0,y,new JLabel(get("s16") + ' '));
         helper.add(1,y,mapImageDir);
         helper.add(3,y,new JLabel(get("s17")));
         y++;

         helper.add(0,y,Box.createVerticalStrut(d4));
         y++;

         header = new JLabel(get("s18"));
         header.setFont(bold);
         helper.addSpan(0,y,4,header);
         y++;

         helper.add(0,y,Box.createVerticalStrut(d3));
         y++;

         helper.add(0,y,new JLabel(get("s19") + ' '));
         helper.add(1,y,prefix);
         helper.addSpan(2,y,2,new JLabel(' ' + get("s20")));
         y++;
         helper.add(0,y,useOrderSource);
         helper.addSpan(1,y,3,orderSource);
         y++;
         helper.add(0,y,useOrderTypeIn);
         helper.addSpan(1,y,3,orderTypeIn);
         y++;

         JPanel panelPro = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
         panelPro.add(proMode);
         panelPro.add(new JLabel(get("s24") + ' '));
         panelPro.add(proModeWaitInterval);
         panelPro.add(new JLabel(' ' + get("s25")));

         helper.addSpan(0,y,4,panelPro);
         y++;

         return fields;
      }

      protected void put() {

         Field.putNullable(requestDir,Convert.fromNullableFile(tempRequestDir));
         Field.putNullable(imageDir,Convert.fromNullableFile(tempImageDir));
         Field.putNullable(mapImageDir,Convert.fromNullableFile(tempMapImageDir));
         Field.put(prefix,tempPrefix);
         Field.put(useOrderSource,tempUseOrderSource);
         Field.put(useOrderTypeIn,tempUseOrderTypeIn);
         Field.put(orderSource,tempOrderSource);
         Field.put(orderTypeIn,tempOrderTypeIn);
         Field.put(proMode,tempProMode);
         proModeWaitInterval.put(tempProModeWaitInterval);
         Field.putNullable(listDirectory,Convert.fromNullableFile(tempListDirectory));
      }

      protected void getAndValidate() throws ValidationException {

         // if there were any way these could fail,
         // we'd have to worry about partial data
         // getting stored into the non-simple fields.
         //
         // (that's why we have to allow null files
         //  and then run special validations later)

         tempRequestDir = Convert.toNullableFile(Field.getNullable(requestDir));
         tempImageDir = Convert.toNullableFile(Field.getNullable(imageDir));
         tempMapImageDir = Convert.toNullableFile(Field.getNullable(mapImageDir));
         tempPrefix = Field.get(prefix);
         tempUseOrderSource = Field.get(useOrderSource);
         tempUseOrderTypeIn = Field.get(useOrderTypeIn);
         tempOrderSource = Field.get(orderSource);
         tempOrderTypeIn = Field.get(orderTypeIn);
         tempProMode = Field.get(proMode);
         tempProModeWaitInterval = proModeWaitInterval.get();
         tempListDirectory = Convert.toNullableFile(Field.getNullable(listDirectory));
      }
   }

}

