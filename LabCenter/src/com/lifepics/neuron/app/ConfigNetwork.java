/*
 * ConfigNetwork.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.gui.Field;
import com.lifepics.neuron.gui.GridBagHelper;
import com.lifepics.neuron.gui.IntervalField;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A helper class for editing diagnostic settings and related things.
 */

public class ConfigNetwork extends JPanel {

// --- fields ---

   private boolean includeStartOverInterval;

   private IntervalField defaultTimeoutInterval; // not strictly a diagnose field
   private IntervalField downPollInterval;
   private JTextField downRetriesBeforeNotification;
   private JTextField downRetriesEvenIfNotDown;
   private JCheckBox pauseRetry;
   private IntervalField pauseRetryInterval;
   private IntervalField pauseRetryLimit;
   private JRadioButton errorShowRed; // these three are also not diagnose fields
   private JRadioButton errorShowYellow;
   private IntervalField errorWindowInterval;

   private JCheckBox startOverEnabled;
   private IntervalField startOverInterval;

// --- construction ---

   public ConfigNetwork(boolean includeStartOverInterval) {
      this.includeStartOverInterval = includeStartOverInterval;

   // fields

      int w6 = Text.getInt(this,"w6");

      defaultTimeoutInterval = new IntervalField(IntervalField.SECONDS,IntervalField.MINUTES);
      downPollInterval = new IntervalField(IntervalField.SECONDS,IntervalField.HOURS);
      downRetriesBeforeNotification = new JTextField(w6);
      downRetriesEvenIfNotDown = new JTextField(w6);
      pauseRetry = new JCheckBox();
      pauseRetryInterval = new IntervalField(IntervalField.MINUTES,IntervalField.DAYS);
      pauseRetryLimit = new IntervalField(IntervalField.MINUTES,IntervalField.WEEKS);
      errorShowRed    = new JRadioButton(Text.get(this,"s97"));
      errorShowYellow = new JRadioButton(Text.get(this,"s98"));
      errorWindowInterval = new IntervalField(IntervalField.MINUTES,IntervalField.HOURS);

      ButtonGroup group = new ButtonGroup();
      group.add(errorShowRed);
      group.add(errorShowYellow);

      if (includeStartOverInterval) {
         startOverEnabled = new JCheckBox();
         startOverInterval = new IntervalField(IntervalField.MINUTES,IntervalField.WEEKS);
      }

   // layout

      setBorder(BorderFactory.createTitledBorder(Text.get(this,"s78")));

      GridBagHelper helper = new GridBagHelper(this);

      int d6 = Text.getInt(this,"d6");

      int y = 0;

      helper.addSpan(0,y,5,new JLabel(Text.get(this,"s79") + ' '));
      helper.add    (5,y,  defaultTimeoutInterval);
      y++;

      helper.add(0,y,Box.createVerticalStrut(d6));
      y++;

      helper.addSpan(0,y,6,new JLabel(Text.get(this,"s80")));
      y++;

      helper.add    (0,y,  Box.createHorizontalStrut(Text.getInt(this,"d7")));
      helper.addSpan(1,y,3,new JLabel(Text.get(this,"s81") + ' '));
      helper.addSpan(4,y,2,downPollInterval);
      y++;

      helper.add    (1,y,  new JLabel(Text.get(this,"s82") + ' '));
      helper.add    (2,y,  downRetriesBeforeNotification);
      helper.addSpan(3,y,5,new JLabel(' ' + Text.get(this,"s83"))); // two cols added, see (*)
      y++;

      helper.addSpan(1,y,5,errorShowRed);
      y++;

      helper.addSpan(1,y,4,errorShowYellow);
      helper.add    (5,y,  errorWindowInterval);
      y++;

      helper.add    (1,y,  new JLabel(Text.get(this,"s84") + ' '));
      helper.add    (2,y,  downRetriesEvenIfNotDown);
      helper.addSpan(3,y,5,new JLabel(' ' + Text.get(this,"s85"))); // two cols added, see (*)
      y++;

      helper.add(0,y,Box.createVerticalStrut(d6));
      y++;

      helper.addSpan(0,y,6,new JLabel(Text.get(this,"s86")));
      y++;

      helper.addSpan(1,y,4,new JLabel(Text.get(this,"s87") + ' '));
      helper.add    (5,y,  pauseRetry);
      y++;

      helper.addSpan(1,y,3,new JLabel(Text.get(this,"s88") + ' '));
      helper.addSpan(4,y,2,pauseRetryInterval);
      y++;

      helper.addSpan(1,y,4,new JLabel(Text.get(this,"s89") + ' '));
      helper.add    (5,y,  pauseRetryLimit);
      y++;

      if (includeStartOverInterval) {

         helper.add(0,y,Box.createVerticalStrut(d6));
         y++;

         helper.addSpan(0,y,6,new JLabel(Text.get(this,"s100")));
         y++;

         helper.addSpan(1,y,4,new JLabel(Text.get(this,"s101") + ' '));
         helper.add    (5,y,  startOverEnabled);
         y++;

         helper.addSpan(1,y,4,new JLabel(Text.get(this,"s102") + ' '));
         helper.add    (5,y,  startOverInterval);
         y++;
      }
   }

// --- data transfer ---

   public void put(Config config) {

      defaultTimeoutInterval.put(config.defaultTimeoutInterval);
      downPollInterval.put(config.diagnoseConfig.downPollInterval);
      Field.put(downRetriesBeforeNotification,Convert.fromInt(config.diagnoseConfig.downRetriesBeforeNotification));
      Field.put(downRetriesEvenIfNotDown,Convert.fromInt(config.diagnoseConfig.downRetriesEvenIfNotDown));
      Field.put(pauseRetry,config.diagnoseConfig.pauseRetry);
      pauseRetryInterval.put(config.diagnoseConfig.pauseRetryInterval);
      pauseRetryLimit.put(config.diagnoseConfig.pauseRetryLimit);

      if (config.errorWindowEnabled) errorShowYellow.setSelected(true);
      else                           errorShowRed   .setSelected(true);
      errorWindowInterval.put(config.errorWindowInterval);

      if (includeStartOverInterval) {
         Field.put(startOverEnabled,config.downloadConfig.startOverEnabled);
         startOverInterval.put(config.downloadConfig.startOverInterval);
      }
   }

   public void get(Config config) throws ValidationException {

      config.defaultTimeoutInterval = (int) defaultTimeoutInterval.get();
      config.diagnoseConfig.downPollInterval = downPollInterval.get();
      config.diagnoseConfig.downRetriesBeforeNotification = Convert.toInt(Field.get(downRetriesBeforeNotification));
      config.diagnoseConfig.downRetriesEvenIfNotDown = Convert.toInt(Field.get(downRetriesEvenIfNotDown));
      config.diagnoseConfig.pauseRetry = Field.get(pauseRetry);
      config.diagnoseConfig.pauseRetryInterval = pauseRetryInterval.get();
      config.diagnoseConfig.pauseRetryLimit = pauseRetryLimit.get();

      config.errorWindowEnabled = errorShowYellow.isSelected();
      config.errorWindowInterval = errorWindowInterval.get();

      if (includeStartOverInterval) {
         config.downloadConfig.startOverEnabled = Field.get(startOverEnabled);
         config.downloadConfig.startOverInterval = startOverInterval.get();
      }
   }

}

