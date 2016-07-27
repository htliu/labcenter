/*
 * SummaryPanel.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.axon.RollUtil;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.dendron.JobUtil;
import com.lifepics.neuron.dendron.LightCluster;
import com.lifepics.neuron.dendron.OrderUtil;
import com.lifepics.neuron.gui.Counter;
import com.lifepics.neuron.gui.GridBagHelper;
import com.lifepics.neuron.gui.IndicatorLight;
import com.lifepics.neuron.gui.MiniLight;
import com.lifepics.neuron.gui.Style;
import com.lifepics.neuron.gui.TabControl;
import com.lifepics.neuron.meta.Selector;
import com.lifepics.neuron.table.View;
import com.lifepics.neuron.thread.SubsystemController;

import java.util.LinkedList;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.*;

/**
 * A panel that displays a summary of the application status.
 */

public class SummaryPanel extends JPanel {

// --- fields ---

   private TabControl tabControl;

   private View pausedOrders;
   private View pausedJobs;
   private View pausedRolls;

   private LightCluster cluster;

// --- construction ---

   /**
    * @param newsPanel The news panel, or null if we don't want one on here.
    */
   public SummaryPanel(Global global, TabControl tabControl, JPanel newsPanel) {
      this.tabControl = tabControl;

   // special indicator lights

      // all the subsystem argument does is add the light as a listener.
      // so, for the networking subsystems, pass null for the subsystem,
      // then add the light as a listener to the error window instead.

      IndicatorLight indicatorLightDownload = new IndicatorLight(null,Style.LIGHT_SUBSYSTEM_LARGE);
      IndicatorLight indicatorLightUpload   = new IndicatorLight(null,Style.LIGHT_SUBSYSTEM_LARGE);
      IndicatorLight indicatorLightAutoComplete = new IndicatorLight(null,Style.LIGHT_SUBSYSTEM_THIN);

      global.errorWindowDownload.addListener(indicatorLightDownload);
      global.errorWindowUpload  .addListener(indicatorLightUpload  );
      global.errorWindowAutoComplete.addListener(indicatorLightAutoComplete);

   // download

      GridPanel panelDownload = new GridPanel(Text.get(this,"s2"),2,3,TabControl.TARGET_DOWNLOAD);

      panelDownload.addGrid(Style.style.adjustLine1(new JLabel(Text.get(this,"s5"))));
      panelDownload.addGrid(twoLineLabel("s6"));
      panelDownload.addGrid(twoLineLabel("s7"));

      panelDownload.addGrid(indicatorLightDownload.getComponent());
      panelDownload.addGrid(miniLightGroup(orderSelect(global,Selectors.orderOpen),
                            pausedOrders = orderSelect(global,Selectors.orderOpenHold),
                                           global.downloadSubsystem),false);
      panelDownload.addGrid(Style.style.adjustCounter(new Counter(orderSelect(global,Selectors.orderPrint))));

   // upload

      GridPanel panelUpload = new GridPanel(Text.get(this,"s3"),2,3,TabControl.TARGET_UPLOAD);

      panelUpload.addGrid(Style.style.adjustLine1(new JLabel(Text.get(this,"s8"))));
      panelUpload.addGrid(twoLineLabel("s9"));
      panelUpload.addGrid(new JLabel());

      panelUpload.addGrid(indicatorLightUpload.getComponent());
      panelUpload.addGrid(miniLightGroup(rollSelect(global,Selectors.rollOpen),
                           pausedRolls = rollSelect(global,Selectors.rollOpenHold),
                                         global.uploadSubsystem),false);
      panelUpload.addGrid(new JLabel(),0,1);

   // light group for job panel

      JPanel group = new JPanel();
      GridBagHelper helper = new GridBagHelper(group);
      helper.add(0,0,new MiniLight(pausedJobs = jobSelect(global,Selectors.jobHold),null,Style.LIGHT_VIEW_HUGE).getComponent());

   // job

      GridPanel panelJob = new GridPanel(Text.get(this,"s11"),2,1,TabControl.TARGET_JOB);

      panelJob.addGrid(Style.style.adjustLine1(new JLabel(Text.get(this,"s12"))));

      panelJob.addGrid(group);

   // light group for other panel

      JComponent[] lights = new JComponent[] {
            new IndicatorLight(global.invoiceSubsystem,   Style.LIGHT_SUBSYSTEM_THIN).getComponent(),
            new IndicatorLight(global.spawnSubsystem,     Style.LIGHT_SUBSYSTEM_THIN).getComponent(),
            cluster = new LightCluster(global.formatSubsystems,Style.LIGHT_SUBSYSTEM_THIN), // (*)
            new IndicatorLight(global.completionSubsystem,Style.LIGHT_SUBSYSTEM_THIN).getComponent(),
            new IndicatorLight(global.jobPurgeSubsystem,  Style.LIGHT_SUBSYSTEM_THIN).getComponent(),
                indicatorLightAutoComplete.getComponent(),
            new IndicatorLight(global.orderPurgeSubsystem,Style.LIGHT_SUBSYSTEM_THIN).getComponent(),
            new IndicatorLight(global.localSubsystem,     Style.LIGHT_SUBSYSTEM_THIN).getComponent(),
            new IndicatorLight(global.pollSubsystem,      Style.LIGHT_SUBSYSTEM_THIN).getComponent(),
            new IndicatorLight(global.rollPurgeSubsystem, Style.LIGHT_SUBSYSTEM_THIN).getComponent()
         };
      // (*) doesn't work in classic style, but nobody knows or cares about that now

      group = Style.style.getLightLayout(lights);

   // other

      GridPanel panelOther = new GridPanel(Text.get(this,"s4"),2,1,TabControl.TARGET_OTHER);

      panelOther.addGrid(Style.style.adjustLine1(new JLabel(Text.get(this,"s10"))));

      panelOther.addGrid(group);

   // overall

      int dp = Style.style.getPanelGap();

      helper = new GridBagHelper(this);

      helper.add(1,1,Style.style.adjustPanel(panelDownload),GridBagHelper.fillBoth);
      helper.add(1,3,Style.style.adjustPanel(panelUpload),  GridBagHelper.fillBoth);

      helper.add(3,1,Style.style.adjustPanel(panelJob),     GridBagHelper.fillBoth);
      helper.add(3,3,Style.style.adjustPanel(panelOther),   GridBagHelper.fillBoth);

      if (newsPanel != null) helper.addSpanFill(1,5,3,Style.style.adjustPanel(newsPanel));

      helper.add(0,0,Box.createRigidArea(new Dimension(dp,dp)));

      helper.add(2,0,Box.createHorizontalStrut(dp));
      helper.add(4,0,Box.createHorizontalStrut(dp));

      helper.add(0,2,Box.createVerticalStrut(dp));
      helper.add(0,4,Box.createVerticalStrut(dp));
      if (newsPanel != null) helper.add(0,6,Box.createVerticalStrut(dp));

      helper.setRowWeight(1,1);
      helper.setRowWeight(3,1);
      helper.setColumnWeight(1,5);
      helper.setColumnWeight(3,1);
   }

   public void rebuildFormatSubsystems(LinkedList formatSubsystems) {
      cluster.rebuild(formatSubsystems);
   }

   public StatusNews.Counts getCounts() {
      StatusNews.Counts counts = new StatusNews.Counts();

      counts.pausedOrders = pausedOrders.size();
      counts.pausedJobs   = pausedJobs.size();
      counts.pausedRolls  = pausedRolls.size();

      return counts;
   }

// --- helpers ---

   private static View orderSelect(Global global, Selector selector) {
      return global.orderTable.select(selector,OrderUtil.orderOrderID,true,true);
   }

   private static View jobSelect(Global global, Selector selector) {
      return global.jobTable.select(selector,JobUtil.orderJobID,true,true);
   }

   private static View rollSelect(Global global, Selector selector) {
      return global.rollTable.select(selector,RollUtil.orderRollID,true,true);
   }

   private JComponent twoLineLabel(String key) {
      JPanel panel = new JPanel();
      GridBagHelper helper = new GridBagHelper(panel);
      helper.addCenter(0,0,Style.style.adjustLine1(new JLabel(Text.get(this,key + "a"))));
      helper.addCenter(0,1,Style.style.adjustLine2(new JLabel(Text.get(this,key + "b"))));
      return panel;
   }

   private JComponent miniLightGroup(View viewCount, View viewAlert, SubsystemController subsystem) {
      JPanel panel = new JPanel();
      panel.setLayout(new GridLayout(1,3,0,0));
      panel.add(new JLabel());
      panel.add(Style.style.adjustCounter(new Counter(viewCount)));
      panel.add(centered(new MiniLight(viewAlert,subsystem,Style.LIGHT_VIEW_LARGE).getComponent()));
      return panel;
   }

   private JComponent centered(JComponent c) {
      JPanel panel = new JPanel();
      panel.setLayout(new GridBagLayout()); // without weights, produces centering
      panel.add(c);
      return panel;
   }

// --- helper class ---

   private class GridPanel extends JPanel {

      private int target;
      private JPanel inside;

      public GridPanel(String title, int rows, int cols, int target) {
         this.target = target;

         int d1 = Text.getInt(SummaryPanel.class,"d1");
         int dd = Style.style.getDetailGap();

      // inside

         inside = new JPanel();

         inside.setLayout(new GridLayout(rows,cols,0,0));
         Border border = BorderFactory.createEmptyBorder(d1,d1,dd,d1);
         if (Style.style.showSummaryGrid()) border = BorderFactory.createCompoundBorder(border,BorderFactory.createMatteBorder(0,0,1,1,Color.black));
         inside.setBorder(border);

      // button

         JButton button = new JButton(Text.get(SummaryPanel.class,"s1"));
         Style.style.adjustButton_Details(button);
         button.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { tabControl.setSelectedTab(GridPanel.this.target); } });

      // this

         setBorder(Style.style.createMajorBorder(title));

         GridBagHelper helper = new GridBagHelper(this);
         helper.add(0,0,inside,GridBagHelper.fillBoth);
         helper.addCenter(0,1,button);
         helper.add(0,2,Box.createVerticalStrut(dd));

         helper.setRowWeight(0,1);
         helper.setColumnWeight(0,1);
      }

      public void addGrid(JComponent c)                    { addGrid(c,1,1,     true); }
      public void addGrid(JComponent c, int top, int left) { addGrid(c,top,left,true); }
      public void addGrid(JComponent c, boolean center)    { addGrid(c,1,1,     center); }

      public void addGrid(JComponent c, int top, int left, boolean center) {
         if (center) c = centered(c);
         if (Style.style.showSummaryGrid()) c.setBorder(BorderFactory.createMatteBorder(top,left,0,0,Color.black));
         inside.add(c);
      }
   }

}

