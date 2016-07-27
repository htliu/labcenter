/*
 * ConfigDirectPDF.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.dendron.DirectPDFConfig;
import com.lifepics.neuron.gui.GridBagHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A panel class for editing direct-print PDF settings.
 */

public class ConfigDirectPDF extends ConfigFormat {

// --- fields ---

   private DirectPDFConfig config;

   private ConfigPJL pjl;

// --- construction ---

   public ConfigDirectPDF(Dialog owner, DirectPDFConfig config) {
      super(owner);
      this.config = config;

      pjl = new ConfigPJL(owner,config);

   // layout

      GridBagHelper helper = new GridBagHelper(this);
      int y = 0;

      y = pjl.doLayout1(helper,y,/* showOrientation = */ true);
      y = pjl.doLayoutS(helper,y);
      y = pjl.doLayout2(helper,y);
   }

// --- implementation of ConfigFormat ---

   public Object getFormatConfig() { return config; }

   public void put() {
      pjl.put();
   }

   public void get() throws ValidationException {
      pjl.get();
   }

   public JComboBox accessPrinterCombo() { return pjl.accessPrinterCombo(); }

}

