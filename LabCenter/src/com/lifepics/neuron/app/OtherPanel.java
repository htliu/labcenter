/*
 * OtherPanel.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.dendron.ThreadDefinition;
import com.lifepics.neuron.gui.GridBagHelper;
import com.lifepics.neuron.gui.Monitor;
import com.lifepics.neuron.gui.Style;
import com.lifepics.neuron.gui.SubsystemMonitor;
import com.lifepics.neuron.gui.SubsystemPanel;
import com.lifepics.neuron.thread.ErrorWindow;
import com.lifepics.neuron.thread.SubsystemController;

import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A panel that displays information about other processes.
 */

public class OtherPanel extends JPanel {

   private Frame owner;
   private SubsystemControl subsystemControl;
   private int renotifyInterval;

   private LinkedList monitors;
   private JPanel cluster;

   public OtherPanel(Global global, Frame owner, SubsystemControl subsystemControl, int renotifyInterval) {
      this.owner = owner;
      this.subsystemControl = subsystemControl;
      this.renotifyInterval = renotifyInterval;

      monitors = new LinkedList();

      boolean newStatusLayout = Style.style.newStatusLayout();

      // normally should use dp between panels, too, but not here
      int dp = Style.style.getPanelGap();
      int d2 = newStatusLayout ? Text.getInt(this,"d2b")
                               : Text.getInt(this,"d2a");

      setBorder(BorderFactory.createEmptyBorder(dp,dp,dp,dp));

      GridBagHelper helper = new GridBagHelper(this);

      int y = 0;

      if ( ! newStatusLayout ) {
         helper.add (0,y++,Box.createVerticalStrut(d2));
      }
      helper.addFill(0,y++,create(global.invoiceSubsystem,2));
      helper.add    (0,y++,Box.createVerticalStrut(d2));
      helper.addFill(0,y++,create(global.spawnSubsystem,3));
      helper.add    (0,y++,Box.createVerticalStrut(d2));
      //
      createFormatSubsystems(global.formatSubsystems);
      helper.addFill(0,y++,cluster);
      //
      helper.add    (0,y++,Box.createVerticalStrut(d2));
      helper.addFill(0,y++,create(global.completionSubsystem,5));
      helper.add    (0,y++,Box.createVerticalStrut(d2));
      helper.addFill(0,y++,create(global.jobPurgeSubsystem,6));
      helper.add    (0,y++,Box.createVerticalStrut(d2));
      helper.addFill(0,y++,create(global.autoCompleteSubsystem,8,global.errorWindowAutoComplete,null));
      helper.add    (0,y++,Box.createVerticalStrut(d2));
      helper.addFill(0,y++,create(global.orderPurgeSubsystem,10));
      helper.add    (0,y++,Box.createVerticalStrut(d2));
      helper.addFill(0,y++,create(global.localSubsystem,11));
      helper.add    (0,y++,Box.createVerticalStrut(d2));
      helper.addFill(0,y++,create(global.pollSubsystem,7));
      helper.add    (0,y++,Box.createVerticalStrut(d2));
      helper.addFill(0,y++,create(global.rollPurgeSubsystem,9));
      int yWeight = y;
      helper.add    (0,y++,new JLabel());
      if ( ! newStatusLayout ) {
         helper.add (0,y++,Box.createVerticalStrut(d2));
      }

      helper.setRowWeight(yWeight,1);
      helper.setColumnWeight(0,1);
   }

   public void rebuildFormatSubsystems(LinkedList formatSubsystems) {

      removeFormatSubsystems();
      createFormatSubsystems(formatSubsystems);

      // notes:
      //
      // we could try to be smart, match SubsystemPanel objects
      // to threads and only create and remove the ones we need
      // to, but this is a rare change, not worth optimizing.
      //
      // fine to reuse same cluster object,
      // and avoids any need for re-layout.
      //
      // not trying to preserve subsystem order in the monitor
      // and SubsystemControl lists, because it doesn't matter.
   }

   private void createFormatSubsystems(LinkedList formatSubsystems) {
      Iterator i = formatSubsystems.iterator();
      while (i.hasNext()) {
         ThreadDefinition t = (ThreadDefinition) i.next();

         SubsystemPanel panel = create(t.formatSubsystem,4,null,t.threadName);
         if (cluster == null) {
            cluster = panel.createCluster();
         } else {
            panel.addToCluster(cluster);
         }
      }
   }

   private void removeFormatSubsystems() {
      Component[] c = cluster.getComponents();
      for (int i=0; i<c.length; i++) {
         SubsystemPanel panel = (SubsystemPanel) c[i];
         subsystemControl.unregister(panel);
         removeSubsystemMonitor(panel.getSubsystem());
      }
      cluster.removeAll();
   }

   public void reinit(int renotifyInterval) {

      if (renotifyInterval != this.renotifyInterval) {
         this.renotifyInterval = renotifyInterval;

         Iterator i = monitors.iterator();
         while (i.hasNext()) {
            ((Monitor) i.next()).reinit(renotifyInterval);
         }
      }
   }

   private SubsystemPanel create(SubsystemController subsystem, int n) {
      return create(subsystem,n,null,null);
   }
   private SubsystemPanel create(SubsystemController subsystem, int n, ErrorWindow errorWindow, String threadName) {
      String suffix = Convert.fromInt(n);

      String name = (threadName != null) ? threadName : Text.get(this,"s" + suffix);

      SubsystemPanel panel = new SubsystemPanel(owner,subsystem,name,errorWindow);
      subsystemControl.register(panel);

      SubsystemMonitor monitor = new SubsystemMonitor(subsystem,Text.get(this,"e" + suffix),renotifyInterval);
      monitors.add(monitor);
      // originally this was to prevent garbage collection, but now we use it for reinit too

      return panel;
   }

   public void addManagedMonitor(Monitor monitor) {
      monitors.add(monitor);
      // allow reuse of existing monitor tracking code
   }

   private void removeSubsystemMonitor(SubsystemController subsystem) {
      Iterator i = monitors.iterator();
      while (i.hasNext()) {
         Monitor monitor = (Monitor) i.next();
         if (monitor instanceof SubsystemMonitor) {
            SubsystemMonitor subsystemMonitor = (SubsystemMonitor) monitor;
            if (subsystemMonitor.getSubsystem() == subsystem) {
               i.remove();
               subsystemMonitor.stop(); // this is important!
               return;
            }
         }
      }
      // shouldn't happen, but throwing exceptions won't help
   }

}

