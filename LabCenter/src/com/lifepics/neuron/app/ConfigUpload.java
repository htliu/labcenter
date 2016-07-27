/*
 * ConfigUpload.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.axon.TransformConfig;
import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.gui.Field;
import com.lifepics.neuron.gui.GridBagHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A helper class for editing upload settings.
 */

public class ConfigUpload extends JPanel {

// --- fields ---

   private boolean includeCheckboxes;

   private JCheckBox enableLimit;
   private JTextField dpi;
   private JTextField xInches;
   private JTextField yInches;
   private JCheckBox alwaysCompress;
   private JTextField compression;

   private JCheckBox lockdownEnabled;
   private JCheckBox watermarkEnabled;
   private JCheckBox exclusiveEnabled;
   private JCheckBox claimEnabled;

// --- construction ---

   public ConfigUpload(boolean includeCheckboxes) {
      this.includeCheckboxes = includeCheckboxes;

   // fields

      enableLimit = new JCheckBox();
      dpi = new JTextField(Text.getInt(this,"w12"));
      int w10 = Text.getInt(this,"w10");
      xInches = new JTextField(w10);
      yInches = new JTextField(w10);
      alwaysCompress = new JCheckBox();
      compression = new JTextField(Text.getInt(this,"w11"));

      if (includeCheckboxes) {
         lockdownEnabled = new JCheckBox();
         watermarkEnabled = new JCheckBox(Text.get(this,"s245"));
         exclusiveEnabled = new JCheckBox(Text.get(this,"s246"));
         claimEnabled = new JCheckBox(Text.get(this,"s244"));
      }

   // layout

      GridBagHelper helper;

      JPanel sub1 = new JPanel();
      helper = new GridBagHelper(sub1);
      helper.add(0,0,enableLimit);
      helper.add(1,0,new JLabel(Text.get(this,"s134") + ' '));
      helper.add(2,0,xInches);
      helper.add(3,0,new JLabel(' ' + Text.get(this,"s135") + ' '));
      helper.add(4,0,yInches);
      helper.add(5,0,new JLabel(' ' + Text.get(this,"s136") + ' '));
      helper.add(6,0,dpi);
      helper.add(7,0,new JLabel(' ' + Text.get(this,"s142")));
      helper.add(0,1,alwaysCompress);
      helper.addSpan(1,1,7,new JLabel(Text.get(this,"s138")));

      JPanel sub2 = new JPanel();
      helper = new GridBagHelper(sub2);
      helper.add(0,1,new JLabel(Text.get(this,"s137") + ' '));
      helper.add(1,1,compression);
      helper.add(2,1,Box.createHorizontalStrut(Text.getInt(this,"d10")));
      helper.add(3,0,new JLabel(Convert.fromInt(TransformConfig.COMPRESSION_MIN_USEFUL)),GridBagHelper.alignRight);
      helper.add(3,1,new JLabel(Convert.fromInt(TransformConfig.COMPRESSION_DEFAULT   )),GridBagHelper.alignRight);
      helper.add(3,2,new JLabel(Convert.fromInt(TransformConfig.COMPRESSION_MAX_USEFUL)),GridBagHelper.alignRight);
      helper.add(4,0,new JLabel(' ' + Text.get(this,"s139")));
      helper.add(4,1,new JLabel(' ' + Text.get(this,"s140")));
      helper.add(4,2,new JLabel(' ' + Text.get(this,"s141")));

      JPanel sub4 = null;
      if (includeCheckboxes) {
         sub4 = new JPanel();
         helper = new GridBagHelper(sub4);
         helper.add(0,0,lockdownEnabled);
         helper.add(1,0,new JLabel(Text.get(this,"s237")));
         helper.add(1,1,new JLabel(Text.get(this,"s238")));
         helper.add(1,2,new JLabel(Text.get(this,"s239")));
      }

      setBorder(BorderFactory.createTitledBorder(Text.get(this,"s20")));

      helper = new GridBagHelper(this);

      int d11 = Text.getInt(this,"d11");
      helper.add(0,0,sub1);
      helper.add(0,1,Box.createVerticalStrut(d11));
      helper.add(0,2,sub2);

      if (includeCheckboxes) {
         helper.add(0,3,Box.createVerticalStrut(d11));
         helper.add(0,4,sub4);
         helper.add(0,5,watermarkEnabled);
         helper.add(0,6,exclusiveEnabled);
         helper.add(0,7,claimEnabled);
      }
   }

// --- data transfer ---

   public void put(Config config) {

      Field.put(enableLimit,config.uploadConfig.transformConfig.enableLimit);
      Field.put(dpi,Convert.fromInt(config.uploadConfig.transformConfig.dpi));
      Field.put(xInches,Convert.fromDouble(config.uploadConfig.transformConfig.xInches));
      Field.put(yInches,Convert.fromDouble(config.uploadConfig.transformConfig.yInches));
      Field.put(alwaysCompress,config.uploadConfig.transformConfig.alwaysCompress);
      Field.put(compression,Convert.fromInt(config.uploadConfig.transformConfig.compression));

      if (includeCheckboxes) {
         Field.put(lockdownEnabled,config.uploadConfig.lockdownEnabled);
         Field.put(watermarkEnabled,config.uploadConfig.watermarkEnabled);
         Field.put(exclusiveEnabled,config.uploadConfig.exclusiveEnabled);
         Field.put(claimEnabled,config.claimEnabled);
      }
   }

   public void get(Config config) throws ValidationException {

      config.uploadConfig.transformConfig.enableLimit = Field.get(enableLimit);
      config.uploadConfig.transformConfig.dpi = Convert.toInt(Field.get(dpi));
      config.uploadConfig.transformConfig.xInches = Convert.toDouble(Field.get(xInches));
      config.uploadConfig.transformConfig.yInches = Convert.toDouble(Field.get(yInches));
      config.uploadConfig.transformConfig.alwaysCompress = Field.get(alwaysCompress);
      config.uploadConfig.transformConfig.compression = Convert.toInt(Field.get(compression));

      if (includeCheckboxes) {
         config.uploadConfig.lockdownEnabled = Field.get(lockdownEnabled);
         config.uploadConfig.watermarkEnabled = Field.get(watermarkEnabled);
         config.uploadConfig.exclusiveEnabled = Field.get(exclusiveEnabled);
         config.claimEnabled = Field.get(claimEnabled);
      }
   }

}

