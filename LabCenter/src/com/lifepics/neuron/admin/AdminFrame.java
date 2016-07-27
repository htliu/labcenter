/*
 * AdminFrame.java
 */

package com.lifepics.neuron.admin;

import com.lifepics.neuron.app.AboutDialog;
import com.lifepics.neuron.app.AutoConfig;
import com.lifepics.neuron.app.Config;
import com.lifepics.neuron.app.ConfigDialog;
import com.lifepics.neuron.app.ConfigDialogPro;
import com.lifepics.neuron.axon.ProMode;
import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Pop;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.gui.EditDialog;
import com.lifepics.neuron.gui.Graphic;
import com.lifepics.neuron.gui.Grid;
import com.lifepics.neuron.gui.GridColumn;
import com.lifepics.neuron.gui.GridUtil;
import com.lifepics.neuron.gui.MenuUtil;
import com.lifepics.neuron.gui.MinimumSize;
import com.lifepics.neuron.gui.ScrollUtil;
import com.lifepics.neuron.gui.ViewHelper;
import com.lifepics.neuron.meta.FieldComparator;
import com.lifepics.neuron.meta.NaturalComparator;
import com.lifepics.neuron.misc.AppUtil;
import com.lifepics.neuron.object.XML;
import com.lifepics.neuron.table.ListView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * The main frame window for the LabCenter Admin application.
 */

public class AdminFrame extends JFrame {

// --- fields ---

   private AppUtil.ControlInterface control;
   private File mainDir;
   private AdminConfig config;

   private ArrayList rows;
   private ListView view;
   private ViewHelper viewHelper;
   private GridUtil.Sorter sorter;
   private JTable tableFixed;

   private SnapshotFile snapshotFile;

// --- construction ---

   public AdminFrame(AppUtil.ControlInterface control, File mainDir, AdminConfig config) {
      super(Text.get(AdminFrame.class,"s1"));

      this.control = control;
      this.mainDir = mainDir;
      this.config = config;

      setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
      Graphic.setFrameIcon(this);
      addWindowListener(new WindowAdapter() { public void windowClosing(WindowEvent e) { doExit(); } });

   // menus

      JMenuBar menuBar = new JMenuBar();
      setJMenuBar(menuBar);

      JMenu menu;

      menu = MenuUtil.makeMenu(menuBar,this,"s3");
      MenuUtil.makeItem(menu,this,"s4",new ActionListener() { public void actionPerformed(ActionEvent e) { doNew(); } });
      MenuUtil.makeItem(menu,this,"s5",new ActionListener() { public void actionPerformed(ActionEvent e) { doOpen(); } }).setEnabled(false);
      MenuUtil.makeItem(menu,this,"s12",new ActionListener() { public void actionPerformed(ActionEvent e) { doRefresh(/* adjust = */ false); } });
      MenuUtil.makeItem(menu,this,"s16",new ActionListener() { public void actionPerformed(ActionEvent e) { doRefresh(/* adjust = */ true); } });
      MenuUtil.makeItem(menu,this,"s6",new ActionListener() { public void actionPerformed(ActionEvent e) { doSave(); } }).setEnabled(false);
      MenuUtil.makeItem(menu,this,"s7",new ActionListener() { public void actionPerformed(ActionEvent e) { doSaveAs(); } }).setEnabled(false);
      menu.addSeparator();
      MenuUtil.makeItem(menu,this,"s8",new ActionListener() { public void actionPerformed(ActionEvent e) { doExit(); } });

      menu = MenuUtil.makeMenu(menuBar,this,"s13");
      MenuUtil.makeItem(menu,this,"s27",new ActionListener() { public void actionPerformed(ActionEvent e) { doEditConfig(/* disableOK = */ true ); } });
      MenuUtil.makeItem(menu,this,"s14",new ActionListener() { public void actionPerformed(ActionEvent e) { doEditConfig(/* disableOK = */ false); } });
      MenuUtil.makeItem(menu,this,"s20",new ActionListener() { public void actionPerformed(ActionEvent e) { doEditVersion(); } });
      menu.addSeparator();
      MenuUtil.makeItem(menu,this,"s23",new ActionListener() { public void actionPerformed(ActionEvent e) { doExport(); } });

      menu = MenuUtil.makeMenu(menuBar,this,"s9");
      MenuUtil.makeItem(menu,this,"s10",new ActionListener() { public void actionPerformed(ActionEvent e) { doHelpAbout(); } });

   // columns

      GridColumn[] colsFixed = new GridColumn[] {

            InstanceUtil.colLineType,
            InstanceUtil.colLineID,
            InstanceUtil.colLocLocationID,
            InstanceUtil.colLocName,
            InstanceUtil.colLastStatusReport
         };

      GridColumn[] cols = new GridColumn[] {

            // continuation of fixed columns
            InstanceUtil.colLocCity,
            InstanceUtil.colLocState,
            InstanceUtil.colLocPhone,

            // instance properties
            InstanceUtil.colPasscode,
            InstanceUtil.colRevisionNumber,
            InstanceUtil.colDisplayVersion,
            InstanceUtil.colReportedLabCenterVersion,
            InstanceUtil.colReportedJavaVersion,
            InstanceUtil.colReportedDL,
            InstanceUtil.colReportedUL,
            InstanceUtil.colReportedIntegrations,
            InstanceUtil.colLastConfigRead,
            InstanceUtil.colLastConfigUpdate,

            // merchant properties
            InstanceUtil.colMerMerchantID,
            InstanceUtil.colMerName,

            // location properties
            InstanceUtil.colLocIsDeleted,
            InstanceUtil.colLocIsPickup,
            InstanceUtil.colLocMerchantLocationID,
            InstanceUtil.colLocFranchiseNumber,
            InstanceUtil.colLocInstallerLookup,
            InstanceUtil.colFftDisplayName,
            InstanceUtil.colFftConfigFileName,

            // wholesaler properties
            InstanceUtil.colWhsWholesalerID,
            InstanceUtil.colWhsName,

            // invoice information
            InstanceUtil.colCmpSwvName,
            InstanceUtil.colMerSwvName,
            InstanceUtil.colLocSwvName,
            InstanceUtil.colWhsSwvName,
            InstanceUtil.colSwvName
         };

   // the grid

      // EditSKUDialog is the model for most of the rest of this,
      // see comments there for details

      rows = new ArrayList();

      int c1 = Text.getInt(this,"c1");
      view = new ListView(rows,null);
      viewHelper = new ViewHelper(view,c1,cols,null);

      viewHelper.getTable().setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
      GridUtil.setShowGrid(viewHelper.getTable());
      viewHelper.getScrollPane().setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      viewHelper.fixHeaderBackground();

      Dimension d = viewHelper.getTable().getPreferredScrollableViewportSize(); // see EditSKUDialog for expl.
      viewHelper.getTable().setPreferredScrollableViewportSize(new Dimension(Text.getInt(this,"d1"),d.height));

      sorter = viewHelper.makeSortable(view);

   // the fixed area

      Grid gridFixed = new Grid(view,colsFixed);

      tableFixed = new JTable(gridFixed) {
         public Dimension getPreferredScrollableViewportSize() {
            return new Dimension(getPreferredSize().width,preferredViewportSize.height);
         }
      };
      GridUtil.setPreferredSize(tableFixed,gridFixed,c1);

      tableFixed.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
      tableFixed.setColumnSelectionAllowed(false);
      tableFixed.getTableHeader().setReorderingAllowed(false);
      GridUtil.setShowGrid(tableFixed);

      GridUtil.addToExistingSorter(sorter,tableFixed,gridFixed);

      viewHelper.getScrollPane().setRowHeaderView(tableFixed);
      viewHelper.getScrollPane().setCorner(JScrollPane.UPPER_LEFT_CORNER,tableFixed.getTableHeader());

      tableFixed.setSelectionModel(viewHelper.getTable().getSelectionModel());

      viewHelper.getScrollPane().getRowHeader().setBackground(tableFixed.getBackground());

   // finish up

      getContentPane().add(viewHelper.getScrollPane(),BorderLayout.CENTER);

      pack();
      new MinimumSize(this);
      ScrollUtil.setSizeToMedium(this);
      setLocationRelativeTo(null); // center on screen
   }

// --- helpers ---

   /**
    * Get a snapshot from the server, or report error to user and return null.
    */
   private Snapshot getSnapshot(Parameters p) {
      try {
         SnapshotTransaction t = new SnapshotTransaction(config.baseURL,p);
         t.runInline();
         return t.result;
      } catch (Exception e) {
         Pop.error(this,e,Text.get(this,"s15"));
         return null;
      }
   }

   private void switchTo(SnapshotFile temp) {
      snapshotFile = temp;

      Snapshot snapshot = temp.snapshot;
      snapshot.index();
      snapshot.bind();
      view.repoint(snapshot.getRows());
      sorter.reset();
   }

// --- commands ---

   private void doNew() {

      Parameters p = new Parameters();
      if ( ! new ParametersDialog(this,p).run() ) return;

      Snapshot snapshot = getSnapshot(p);
      if (snapshot == null) return;

      SnapshotFile temp = new SnapshotFile();
      temp.parameters = p;
      temp.snapshot = snapshot;

      switchTo(temp);
   }

   private void doOpen() {
   }

   private void doRefresh(boolean adjust) {
      if (snapshotFile == null) return;

      Parameters p = snapshotFile.parameters;
      if (adjust) {
         p = p.copy();
         if ( ! new ParametersDialog(this,p).run() ) return;
      }

      Snapshot snapshot = getSnapshot(p);
      if (snapshot == null) return;

      snapshotFile.parameters = p;
      snapshotFile.snapshot = snapshot;

      switchTo(snapshotFile);
   }

   private void doSave() {
   }

   private void doSaveAs() {
   }

   private Instance getSelectedInstance() throws Exception {
      // we could do some magic with ViewHelper.getAdapter,
      // but it doesn't seem worth it
      Object[] o = viewHelper.getSelectedObjects();
      if (o.length == 0) return null;
      if ( ! (o[0] instanceof Instance) ) throw new Exception(Text.get(this,"e2"));
      return (Instance) o[0];
   }

   private void doEditConfig(boolean disableOK) {
      try {

         Instance instance = getSelectedInstance();
         if (instance == null) return;
         if (instance.passcode == null) throw new Exception(Text.get(this,"e3")); // shouldn't happen

         File f1 = new File(mainDir,"temp1.xml");
         File f2 = new File(mainDir,"temp2.xml");

         new AutoConfig.DownloadDataTransaction(config.autoConfigURL,instance.instanceID,instance.passcode.intValue(),f1).runInline();

         Config c = (Config) XML.loadFile(f1,new Config(),"Server");
         AutoConfig.validateServer(c);

         // there are four different versions to consider.
         //
         // (1) the assigned version on the server (based on labCenterVersion, useDefaultVersion, and defaultLabCenterVersion)
         // (2) the last reported version on the server
         // (3) the current version in the config file
         // (4) the version of LC that's actually running
         //
         // we really want to know (4), but none of the others are exactly equal to it.
         // (1) can be different if there's a problem with the auto-update, or even if
         // it just hasn't happened yet.  (2) is flimsy, especially since the table is
         // still by location not instance.  (3) only tells the version at the time of
         // the last config write, but since we rewrite the file when the version changes,
         // it's pretty close.
         //
         // on the other hand, it's really (1) that matters, not (4).  we won't read
         // the updated config file until auto-update time so it should all work out.
         //
         // might want to use inequalities someday, like, if the reported version is X,
         // maybe LC is on a later version, but it can still handle a version X config.

         int tMin = 711;
         int t = 0;

         String versionName = DisplayVersion.get(instance).getVersionName(snapshotFile.snapshot.defaultLabCenterVersion);
         if (versionName == null) { // version is invalid
            Pop.warning(this,Text.get(this,"e5"),Text.get(this,"s22"));
            disableOK = true;
         } else {
            t = AppVersion.versionToInt(versionName);
            if (t < tMin) {
               Pop.warning(this,Text.get(this,"e4",new Object[] { AppVersion.versionFromInt(t),
                                                                  AppVersion.versionFromInt(tMin) }),Text.get(this,"s19"));
               disableOK = true;
            }
            // if t > current LC Admin version, no problem since we can apparently read the file
         }

         EditDialog dialog = ProMode.isPro(c.proMode) ? (EditDialog) new ConfigDialogPro(this,c,disableOK)
                                                      : (EditDialog) new ConfigDialog   (this,c,disableOK,/* forceSKU = */ null);
         if ( ! dialog.run() ) return;

         int r = c.autoUpdateConfig.revisionNumber.intValue();
         c.autoUpdateConfig.revisionNumber = new Integer(r+1);

         // make very sure we didn't break anything
         c.validate();
         AutoConfig.validateServer(c);

         XML.tstoreFile(f2,t,c,"Server");
         new AutoConfig.PutDataTransaction(c,f2).runInline();

         archive(f1,instance.instanceID,r,  "b"); // before
         archive(f2,instance.instanceID,r+1,"a"); // after

      } catch (Exception e) {
         Pop.error(this,e,Text.get(this,"s18"));
      }
   }

   private void doEditVersion() {
      try {

         Instance instance = getSelectedInstance();
         if (instance == null) return;

         Snapshot snapshot = snapshotFile.snapshot; // instance not null implies snapshot not null

         IDisplayVersion dvOld = DisplayVersion.get(instance);
         Object[] dvAll = DisplayVersion.getAll(snapshot.labCenterVersions);

         EditVersionDialog dialog = new EditVersionDialog(this,dvOld,dvAll,snapshot.defaultLabCenterVersion.versionName);
         if ( ! dialog.run() ) return;
         IDisplayVersion dvNew = dialog.getResult();
         if (dvNew == dvOld) return;
         // note, in the case when dvNew and dvOld are the default version, we're not
         // checking that the instance's version ID equals the default version ID,
         // but it should be; we always maintain that condition when we update the DB.
         // in particular, when we set the version to the default, the server will
         // check that the snapshot default version is the database default version.

         int versionID = dvNew.getVersionID(snapshot.defaultLabCenterVersion);
         boolean useDefault = dvNew.getUseDefault();
         LabCenterVersion lcv = dvNew.getVersionObject(snapshot.defaultLabCenterVersion);

         updateInstance(instance,versionID,useDefault,lcv);

         /*
         Object[] multi = viewHelper.getSelectedObjects();
         for (int i=0; i<multi.length; i++) {
            if (multi[i] instanceof Instance) {
               updateInstance((Instance) multi[i],versionID,useDefault,lcv);
            }
         }
         */

      } catch (Exception e) {
         Pop.error(this,e,Text.get(this,"s21"));
      }
   }

   private void updateInstance(Instance instance, int versionID, boolean useDefault, LabCenterVersion lcv) throws Exception {

      new UpdateInstanceTransaction(config.baseURL,instance.instanceID,versionID,useDefault).runInline();
      // the fields in the database are nullable and independent,
      // but we only support setting them to (versionID,false) and (defaultLabCenterVersionID,true)

      // success, update the data in memory
      instance.labCenterVersionID = new Integer(versionID);
      instance.useDefaultVersion = new Boolean(useDefault);
      instance.labCenterVersion = lcv;

      // and refresh (the entire row, not sure if we can do cells)
      view.editNoSort(instance);
   }

   private static void archive(File f, int instanceID, int r, String suffix) throws Exception {
      // suffix should prevent all collisions; if not, oh well
      f.renameTo(new File(f.getParent(),Convert.fromInt(instanceID) + "-" + Convert.fromInt(r) + suffix + ".xml"));
   }

   private void doExport() { // export list of selected instances in ConfigCenter format

   // prepare instance list

      ArrayList instances;
      try {

         Object[] o = viewHelper.getSelectedObjects();
         instances = new ArrayList();
         for (int i=0; i<o.length; i++) {
            if (o[i] instanceof Instance) {
               Instance instance = (Instance) o[i];
               if (instance.passcode == null) throw new Exception(Text.get(this,"e6",new Object[] { Convert.fromInt(instance.instanceID) }));
               instances.add(instance);
            }
         }
         if (instances.size() == 0) throw new Exception(Text.get(this,"e7"));
         Collections.sort(instances,new FieldComparator(InstanceUtil.insInstanceID,new NaturalComparator()));

      } catch (Exception e) {
         Pop.error(this,e,Text.get(this,"s24"));
         return;
      }

   // get target file

      JFileChooser chooser = new JFileChooser(mainDir);
      if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
      // ought to remember the export directory independent of main directory
      File dest = chooser.getSelectedFile();
      if ( dest.exists() && ! Pop.confirm(this,Text.get(this,"e8",new Object[] { Convert.fromFile(dest) }),Text.get(this,"s25")) ) return;

   // export to file

      FileOutputStream fos = null;
      try {

         fos = new FileOutputStream(dest);
         PrintStream ps = new PrintStream(fos);
         Iterator i = instances.iterator();
         while (i.hasNext()) {
            Instance instance = (Instance) i.next();
            ps.println(Convert.fromInt(instance.instanceID) + "," + Convert.fromNullableInt(instance.passcode));
         }

      } catch (Exception e) {
         Pop.error(this,e,Text.get(this,"s26"));
      } finally {
         if (fos != null) try { fos.close(); } catch (Exception e) {}
      }
   }

   private void doHelpAbout() {
      Pop.info(this,Text.get(this,"i1",new Object[] { AboutDialog.getVersion() }),Text.get(this,"s11"));
   }

   private void doExit() {

      boolean confirmed = Pop.confirm(this,Text.get(this,"e1"),Text.get(this,"s2"));
      if ( ! confirmed ) return;

      control.exit();
   }

}

