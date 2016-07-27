/*
 * ConfigDLS.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.dendron.DLSConfig;
import com.lifepics.neuron.gui.Field;
import com.lifepics.neuron.gui.GridBagHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A panel class for editing DLS settings.
 */

public class ConfigDLS extends ConfigFormat {

// --- fields ---

   private DLSConfig config;

   private JTextField host;
   private JTextField userName;
   private JTextField password;
   private ConfigBackprint backprint;

// --- construction ---

   public ConfigDLS(Dialog owner, boolean border, DLSConfig config) {
      super(owner);
      this.config = config;

      int w1 = Text.getInt(this,"w1");

      host = new JTextField(w1);
      userName = new JTextField(w1);
      password = new JTextField(w1);
      backprint = new ConfigBackprint(config.backprints);

      if (border) setBorder(BorderFactory.createTitledBorder(Text.get(this,"s1")));

      GridBagHelper helper = new GridBagHelper(this);

      int y = 0;

      helper.add(0,y,new JLabel(Text.get(this,"s2") + ' '));
      helper.addSpan(1,y,3,host);
      y++;

      helper.add(0,y,new JLabel(Text.get(this,"s3") + ' '));
      helper.addSpan(1,y,3,userName);
      y++;

      helper.add(0,y,new JLabel(Text.get(this,"s4") + ' '));
      helper.addSpan(1,y,3,password);
      y++;

      y = backprint.add(helper,y);
   }

// --- implementation of ConfigFormat ---

   public Object getFormatConfig() { return config; }

   public void put() {

      if (config.host != null) // happens at loadDefault
         Field.put(host,config.host);
      if (config.userName != null)
         Field.put(userName,config.userName);
      if (config.password != null)
         Field.put(password,config.password);

      backprint.put();
   }

   public void get() throws ValidationException {

      config.host = Field.get(host);
      config.userName = Field.get(userName);
      config.password = Field.get(password);

      backprint.get();
   }

}

