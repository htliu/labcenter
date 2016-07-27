/*
 * ConfigFuji.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Pop;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.dendron.FujiConfig;
import com.lifepics.neuron.gui.EditDialog;
import com.lifepics.neuron.gui.Field;
import com.lifepics.neuron.gui.GridBagHelper;
import com.lifepics.neuron.misc.FileMapper;

import java.io.File;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A panel class for editing Fuji settings.
 */

public class ConfigFuji extends ConfigFormat {

// --- fields ---

   private FujiConfig config;

   private File tempRequestDir;
   private File tempImageDir;
   private File tempMapRequestDir;
   private File tempMapImageDir;

   // fields for simple configuration
   private JTextField requestDirRoot;
   private String     requestDirRest;
   private JTextField mapImageDirRoot;
   private String     mapImageDirRest;

   private JCheckBox limitEnable;
   private JTextField limitLength;

// --- construction ---

   public ConfigFuji(Dialog owner, boolean border, FujiConfig config) {
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

// --- split and join functions ---

   // the root is really a file, not a string,
   // but here the file always gets converted
   // to a string, might as well do it once.

   public static String splitRoot(File file) {
      return (file != null) ? Convert.fromFile(FileMapper.getRoot(file)) : null;
   }

   // the rest is never null, simpler that way.

   public static String splitRest(File file) {
      return (file != null) ? FileMapper.getRest(file) : "";
   }

   public static File join(String root, String rest) throws ValidationException {

      if (root == null) return null;
      // if the root has been set to null, it means the user has cleared the field
      // in simple configuration ... and in that case the rest should be discarded.

      // make sure user doesn't enter non-root stuff into a root field
      File temp = new File(root);
      if ( ! temp.equals(FileMapper.getRoot(temp)) ) throw new ValidationException(Text.get(ConfigFuji.class,"e2"));

      return new File(root,rest);
   }

// --- transfer primitives ---

   private void tempToConfig() {
      config.requestDir = tempRequestDir;
      config.imageDir = tempImageDir;
      config.mapRequestDir = tempMapRequestDir;
      config.mapImageDir = tempMapImageDir;
   }

   private void configToTemp() {
      tempRequestDir = config.requestDir;
      tempImageDir = config.imageDir;
      tempMapRequestDir = config.mapRequestDir;
      tempMapImageDir = config.mapImageDir;
   }

   private void tempToSimple() {

      Field.putNullable(requestDirRoot,splitRoot(tempRequestDir));
      requestDirRest = splitRest(tempRequestDir);
      // null requestDir isn't valid; but it occurs after loadDefault

      Field.putNullable(mapImageDirRoot,splitRoot(tempMapImageDir));
      mapImageDirRest = splitRest(tempMapImageDir);
   }

   private void simpleToTemp() throws ValidationException {

      tempRequestDir = join(Field.getNullable(requestDirRoot),requestDirRest);

      tempMapImageDir = join(Field.getNullable(mapImageDirRoot),mapImageDirRest);
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

      if (config.requestDir == null) throw new ValidationException(Text.get(this,"e1"));
      // everywhere else we've treated requestDir as nullable,
      // but we have to catch it before it gets out into the real world

      config.limitEnable = Field.get(limitEnable);
      config.limitLength = Convert.toInt(Field.get(limitLength));
   }

// --- subdialog ---

   private void doAdvancedDialog() {

      try {
         simpleToTemp();
      } catch (ValidationException e) {
         Pop.error(this,e,Text.get(this,"s19"));
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
      private JTextField mapRequestDir;
      private JTextField mapImageDir;

   // --- construction ---

      public AdvancedDialog() {
         super(ConfigFuji.this.getOwner(),Text.get(ConfigFuji.class,"s7"));
         construct(constructFields(),/* readonly = */ false);
      }

   // --- methods ---

      private String get(String key) { return Text.get(ConfigFuji.class,key); }

      private JPanel constructFields() {

         requestDir = constructDataDir();
         imageDir = constructDataDir();
         mapRequestDir = constructDataDir();
         mapImageDir = constructDataDir();

         JPanel fields = new JPanel();
         GridBagHelper helper = new GridBagHelper(fields);

         int y = 0;

         int d3 = Text.getInt(ConfigFuji.class,"d3");
         int d4 = Text.getInt(ConfigFuji.class,"d4");

         JLabel header = new JLabel(get("s8"));
         Font bold = header.getFont().deriveFont(Font.BOLD);
         header.setFont(bold);
         helper.addSpan(0,y,4,header);
         y++;

         helper.add(0,y,Box.createVerticalStrut(d3));
         y++;

         helper.add(0,y,new JLabel(get("s9") + ' '));
         helper.add(1,y,requestDir);
         helper.add(2,y,new JLabel(' ' + get("s10") + ' '));
         helper.add(3,y,new JLabel(get("s11")));
         y++;

         helper.add(0,y,new JLabel(get("s12") + ' '));
         helper.add(1,y,imageDir);
         helper.add(3,y,new JLabel(get("s13")));
         y++;

         helper.add(0,y,Box.createVerticalStrut(d4));
         y++;

         header = new JLabel(get("s14"));
         header.setFont(bold);
         helper.addSpan(0,y,4,header);
         y++;

         helper.add(0,y,Box.createVerticalStrut(d3));
         y++;

         helper.add(0,y,new JLabel(get("s15") + ' '));
         helper.add(1,y,mapRequestDir);
         helper.add(3,y,new JLabel(get("s16")));
         y++;

         helper.add(0,y,new JLabel(get("s17") + ' '));
         helper.add(1,y,mapImageDir);
         helper.add(3,y,new JLabel(get("s18")));
         y++;

         return fields;
      }

      protected void put() {

         Field.putNullable(requestDir,Convert.fromNullableFile(tempRequestDir));
         Field.putNullable(imageDir,Convert.fromNullableFile(tempImageDir));
         Field.putNullable(mapRequestDir,Convert.fromNullableFile(tempMapRequestDir));
         Field.putNullable(mapImageDir,Convert.fromNullableFile(tempMapImageDir));
      }

      protected void getAndValidate() throws ValidationException {

         // if there were any way these could fail,
         // we'd have to worry about partial data
         // getting stored into the non-simple fields.

         tempRequestDir = Convert.toNullableFile(Field.getNullable(requestDir));
         tempImageDir = Convert.toNullableFile(Field.getNullable(imageDir));
         tempMapRequestDir = Convert.toNullableFile(Field.getNullable(mapRequestDir));
         tempMapImageDir = Convert.toNullableFile(Field.getNullable(mapImageDir));
      }
   }

}

