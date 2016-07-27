/*
 * MainFrame.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.axon.HistoryDialog;
import com.lifepics.neuron.axon.ProMode;
import com.lifepics.neuron.core.ChainedException;
import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Pop;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.dendron.BackupDialog;
import com.lifepics.neuron.dendron.DendronHistoryDialog;
import com.lifepics.neuron.dendron.JobHistoryDialog;
import com.lifepics.neuron.dendron.GroupPanel;
import com.lifepics.neuron.dendron.Queue;
import com.lifepics.neuron.dendron.QueueList;
import com.lifepics.neuron.gui.Browser;
import com.lifepics.neuron.gui.EditDialog;
import com.lifepics.neuron.gui.EditSKUInterface;
import com.lifepics.neuron.gui.FrameReference;
import com.lifepics.neuron.gui.Graphic;
import com.lifepics.neuron.gui.HTMLEditorPane;
import com.lifepics.neuron.gui.MenuUtil;
import com.lifepics.neuron.gui.MinimumSize;
import com.lifepics.neuron.gui.ScrollUtil;
import com.lifepics.neuron.gui.Style;
import com.lifepics.neuron.gui.TabControl;
import com.lifepics.neuron.gui.User;
import com.lifepics.neuron.gui.ViewMonitor;
import com.lifepics.neuron.misc.ShellUtil;
import com.lifepics.neuron.net.Query;
import com.lifepics.neuron.struct.SKU;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.*;

/**
 * The main frame window for the whole application.
 */

public class MainFrame extends JFrame implements TabControl, EditSKUInterface, ChangeListener, HyperlinkListener, User.FrameAlert {

// --- fields ---

   private Global global;
   private File baseDir;
   private int proMode;

   private FrameReference orderHistory;
   private FrameReference jobHistory;
   private FrameReference rollHistory;

   private URL newsFile;
   private URL nullFile;
   private HTMLEditorPane editorPane;

   private Component cpsJMenuItem;

   // help menu items starting with index = helpIndex
   // were dynamically generated and can be refreshed
   private JMenu helpMenu;
   private int helpIndex;

   private JTabbedPane tabbedPane;
   private SummaryPanel summaryTab;
   private DendronPanel downloadTab;
   private AxonPanel uploadTab;
   private OtherPanel otherTab;
   private GroupPanel queueTab;

   private SubsystemControl subsystemControl;

   private MinimumSize minimumSize;

// --- construction ---

   public MainFrame(Global global, File baseDir, File newsFile, int proMode, boolean preventStopStart, boolean showScanThread, boolean showKioskThread, boolean showCpsCommand, LinkedList helpItems, boolean showQueueTab, boolean showGrandTotal, String frameTitleSuffix, int renotifyInterval) {
      super(constructTitle(frameTitleSuffix,proMode));

      this.global = global;
      this.baseDir = baseDir;
      this.proMode = proMode;

      orderHistory = new FrameReference();
      jobHistory   = new FrameReference();
      rollHistory  = new FrameReference();

      try {
         this.newsFile = newsFile.toURL();
         this.nullFile = new URL("file:"); // this is the most pleasing null page I've found
      } catch (Exception e) {
         this.newsFile = null; // doesn't happen
         this.nullFile = null;
      }

      setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
      Graphic.setFrameIcon(this);
      addWindowListener(new WindowAdapter() {
         public void windowClosing(WindowEvent e) { doWindowClose(); }
         public void windowDeiconified(WindowEvent e) { doWindowUnminimize(); }
      });

   // menus

      JMenuBar menuBar = new JMenuBar();
      setJMenuBar(menuBar);

      JMenu menu;

      menu = MenuUtil.makeMenu(menuBar,this,"s2");
      MenuUtil.makeItem(menu,this, "s3",new ActionListener() { public void actionPerformed(ActionEvent e) { doSetup(/* forceSKU = */ null); } });
      MenuUtil.makeItem(menu,this,"s24",new ActionListener() { public void actionPerformed(ActionEvent e) { doSetupAutostart(); } });
      menu.addSeparator();
      //
      MenuUtil.makeItem(menu,this,"s45",new ActionListener() { public void actionPerformed(ActionEvent e) { doChangePrinterStatus(); } });
      cpsJMenuItem = menu.getMenuComponent(menu.getItemCount()-1); // getItemCount is same as getMenuComponentCount
      //
      MenuUtil.makeItem(menu,this,"s48",new ActionListener() { public void actionPerformed(ActionEvent e) { doChangeAutoPrint(); } });
      menu.addSeparator();
      MenuUtil.makeItem(menu,this,"s33",new ActionListener() { public void actionPerformed(ActionEvent e) { doRestore(); } });
      menu.addSeparator();
      MenuUtil.makeItem(menu,this, "s4",new ActionListener() { public void actionPerformed(ActionEvent e) { doExit(); } });

      menu = MenuUtil.makeMenu(menuBar,this,"s17");
      if ( ! ProMode.isPro(proMode) ) MenuUtil.makeItem(menu,this,"s18",new ActionListener() { public void actionPerformed(ActionEvent e) { doOrderHistory(); } });
      MenuUtil.makeItem(menu,this,"s19",new ActionListener() { public void actionPerformed(ActionEvent e) { doRollHistory(); } });

      menu = MenuUtil.makeMenu(menuBar,this,"s5");
      String s6 = Text.get(this,"s6",new Object[] { Style.style.getBrandName() });
      MenuUtil.makeItem(menu,       s6 ,new ActionListener() { public void actionPerformed(ActionEvent e) { doHelpAbout(); } });
      MenuUtil.makeItem(menu,this,"s43",new ActionListener() { public void actionPerformed(ActionEvent e) { doCheckUpdate(); } });

      menu = MenuUtil.makeMenu(menuBar,this,"s22");
      Style.style.adjustMenu_LiveHelp(menu);
      MenuUtil.makeItem(menu,this,"s21",new ActionListener() { public void actionPerformed(ActionEvent e) { doHelpChat(); } });
   // menu.addSeparator();
   // MenuUtil.makeItem(menu,this,"s31",new ActionListener() { public void actionPerformed(ActionEvent e) { doHelpManual(); } });
   // disabled until we have a manual there
      menu.addSeparator();
      MenuUtil.makeItem(menu,this,"s39",new ActionListener() { public void actionPerformed(ActionEvent e) { doHelpAdmin("M"); } });
      MenuUtil.makeItem(menu,this,"s40",new ActionListener() { public void actionPerformed(ActionEvent e) { doHelpAdmin("L"); } });
      helpMenu = menu;
      helpIndex = menu.getItemCount(); // dynamic items added by reinit1

      Style.style.adjustMenuBar(menuBar);

   // news area

      editorPane = new HTMLEditorPane(this.newsFile);
      editorPane.addHyperlinkListener(this); // must be read-only for this to work

      // re extraPanel, see HTMLViewer.java
      JPanel extraPanel = new JPanel();
      extraPanel.setLayout(new BorderLayout());
      extraPanel.add(editorPane);

      JScrollPane scroll = new JScrollPane(extraPanel,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                      JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      scroll.setPreferredSize(new Dimension(0,Text.getInt(this,"d1")));
      scroll.getVerticalScrollBar()  .setUnitIncrement(Text.getInt(this,"d2"));
      scroll.getHorizontalScrollBar().setUnitIncrement(Text.getInt(this,"d3"));
      Style.style.adjustScroll_Other(scroll);
      //
      // if the client implements Scrollable, the scroll pane will get
      // the increments from there.  the extra panel prevents that,
      // but it doesn't seem like a bad idea, maybe we should call the
      // Scrollable functions on editorPane directly?  actually, no ...
      // it's JTextComponent that implements it, and all it does is take
      // 10% of the visible rectangle.

      JPanel newsPanel = new JPanel();
      newsPanel.setBorder(Style.style.createSidebarBorder(Text.get(this,"s29")));
      newsPanel.setLayout(new BorderLayout());
      newsPanel.add(scroll);

      // it's a bit strange to construct and manage the news here,
      // then move the news area into the summary panel.
      // but, there's no harm in it, and it's much easier than
      // moving all the code there.

   // overall layout

      subsystemControl = new SubsystemControl();

      if (ProMode.isPro(proMode)) {

         summaryTab  = new SummaryPanel(global,this,null);
         uploadTab   = new AxonPanel   (global,this,subsystemControl,newsPanel);
         // tabbedPane, downloadTab, and otherTab remain null

         reinit1(preventStopStart,showScanThread,showKioskThread,showCpsCommand,helpItems,showQueueTab,showGrandTotal,null);
         // keep this for consistency, but it does nothing here
         // since showScanThread is forced to false in pro mode.
         // actually, this is what hides the scan status!

         // summaryTab not displayed, not adjusted
         getContentPane().add(Style.style.adjustTab(uploadTab));

      } else { // normal mode

         tabbedPane = new JTabbedPane();

         summaryTab  = new SummaryPanel(global,this,newsPanel);
         downloadTab = new DendronPanel(global,this,subsystemControl);
         uploadTab   = new AxonPanel   (global,this,subsystemControl,null);
         otherTab    = new OtherPanel  (global,this,subsystemControl,renotifyInterval);

         otherTab.addManagedMonitor(new ViewMonitor(downloadTab.getViewPrint(),User.CODE_ORDERS_READY,Text.get(this,"e9"),renotifyInterval));

         tabbedPane.addTab(Text.get(this,"s7"), Style.style.adjustTab(summaryTab ));
         tabbedPane.addTab(Text.get(this,"s8"), Style.style.adjustTab(downloadTab));
         tabbedPane.addTab(Text.get(this,"s9"), Style.style.adjustTab(uploadTab  ));
         tabbedPane.addTab(Text.get(this,"s10"),Style.style.adjustTab(otherTab   ));

         reinit1(preventStopStart,showScanThread,showKioskThread,showCpsCommand,helpItems,showQueueTab,showGrandTotal,null);

         Style.style.adjustTabbedPane (tabbedPane);
         Style.style.refreshTabbedPane(tabbedPane);
         tabbedPane.addChangeListener(this);

         getContentPane().add(tabbedPane);
      }

   // finish up

      pack();
      minimumSize = new MinimumSize(this);
      ScrollUtil.setSizeToMedium(this);
      setLocationRelativeTo(null); // center on screen
   }

   public StatusNews.Counts getCounts() {
      return summaryTab.getCounts();
   }

   public void reloadNews() {
      // if you just set the same page, it won't reload
      editorPane.setPageSafe(nullFile);
      editorPane.setPageSafe(newsFile);
   }

   public void hyperlinkUpdate(HyperlinkEvent e) {
      // I'm not supporting frames here
      if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
         Browser.launch(this,e.getURL().toString(),Text.get(this,"s30"));
      }
   }

   /**
    * @param queueList During a real reinit, the queue list; during construction, null.
    */
   public void reinit1(boolean preventStopStart, boolean showScanThread, boolean showKioskThread, boolean showCpsCommand, LinkedList helpItems, boolean showQueueTab, boolean showGrandTotal, QueueList queueList) {
      final int INDEX_QUEUE_TAB = 2;

      subsystemControl.setPreventStopStart(preventStopStart);
      uploadTab.reinit(showScanThread,showKioskThread);

      cpsJMenuItem.setVisible(showCpsCommand);
      // this avoids the style issue that the help menu has

      MenuUtil.truncate(helpMenu,helpIndex);
      if (helpItems.size() > 0) {
         helpMenu.addSeparator();
         Iterator i = helpItems.iterator();
         while (i.hasNext()) {
            HelpItem item = (HelpItem) i.next();
            final String url = item.itemURL;
            MenuUtil.makeItem(helpMenu,MenuUtil.noMnemonic(item.itemText),new ActionListener() { public void actionPerformed(ActionEvent e) { doHelpItem(url); } });
         }
         Style.style.adjustMenu(helpMenu); // readjust top part is harmless
      }

      if (tabbedPane == null) return; // in pro mode, no tabbed pane, thus no queue tab
      if (showQueueTab) {

         if (queueTab == null) {
            int selected = tabbedPane.getSelectedIndex();

            queueTab = new GroupPanel(this,this,global.groupTable,global.jobManager,global.orderManager,showGrandTotal,global.formatSubsystems);
            tabbedPane.insertTab(Text.get(this,"s20"),null,Style.style.adjustTab(queueTab),null,INDEX_QUEUE_TAB);

            if (selected >= INDEX_QUEUE_TAB) selected++;
            tabbedPane.setSelectedIndex(selected);
            // amazingly, tabbed pane doesn't do this itself;
            // also this should cause a ChangeEvent to fire

         } else { // queue tab present, maybe update queue list
            if (queueList != null) {
               queueTab.reinit(queueList,showGrandTotal);
            }
         }

      } else { // don't show queue tab

         if (queueTab != null) {
            int selected = tabbedPane.getSelectedIndex();
            // this must come before removal, because the tabbed pane
            // does keep the selected index within the number of tabs

            queueTab = null;
            tabbedPane.removeTabAt(INDEX_QUEUE_TAB);

            if (selected > INDEX_QUEUE_TAB) selected--;
            tabbedPane.setSelectedIndex(selected); // see above

            Style.style.refreshTabbedPane(tabbedPane);
            // annoyance ... in the case that you're on the last tab,
            // the change event fires inside removeTabAt, <i>before</i>
            // the tab is actually removed.  so, the wrong tab gets
            // the highlight color, and then setSelectedIndex doesn't
            // do anything because that tab index is already selected.
            // so, just refresh again in all cases ...
            // it's not like tab removal is something that happens much.
         }
      }

      // the point of going to all this trouble, actually inserting and removing
      // instead of just showing and hiding, is to avoid the overhead of keeping
      // the queue tab updated for the vast majority of users who won't use it.
   }

   // reinit1 is called during construction and at reinit time,
   // reinit2 is called only at reinit time.
   // so, for any given parameter, if it's easy to construct and
   // reinit in the same way, drop it in reinit1, otherwise here.
   //
   // if called, rebuildFormatSubsystems is called before reinit1 so that
   // the changes in reinit1 and reinit2 happen to the correct subsystems
   //
   public void reinit2(String frameTitleSuffix, int renotifyInterval) {
      setTitle(constructTitle(frameTitleSuffix,proMode));
      if (otherTab != null) otherTab.reinit(renotifyInterval);
   }

   public void rebuildFormatSubsystems() {

      // the first two uses don't depend on the thread names,
      // but rebuild them every time anyway.  it's not worth
      // the added complexity.
      /* never null */    summaryTab.rebuildFormatSubsystems(global.formatSubsystems);
      if (queueTab != null) queueTab.rebuildFormatSubsystems(global.formatSubsystems);

      // the test for null here is nice, but is it necessary?
      // technically yes.  even in pro mode, the subsystems
      // still exist.  there's no way to add or remove them through the UI,
      // but in theory a remote config change could cause a rebuild.
      if (otherTab != null) otherTab.rebuildFormatSubsystems(global.formatSubsystems);

      Dimension size = getSize();
      pack();
      minimumSize.update();
      setSize(size);
      // OtherPanel gets taller when threads are added,
      // and SummaryPanel and GroupPanel can get wider,
      // at least in theory.
   }

   private static String constructTitle(String frameTitleSuffix, int proMode) {
      // must be static, I think, since it's called in the superclass constructor
      String key = (frameTitleSuffix == null) ? "s1a" : "s1b";
      String pro;
      if      (ProMode.isProOld(proMode)) pro = " " + Text.get(MainFrame.class,"s1c");
      else if (ProMode.isProNew(proMode)) pro = " " + Text.get(MainFrame.class,"s1d");
      else pro = "";
      return Text.get(MainFrame.class,key,new Object[] { Style.style.getBrandName(), frameTitleSuffix, pro });
   }

// --- implementation of TabControl ---

   // not called in pro mode

   public void setSelectedTab(int target) {
      JPanel tab = null;

      switch (target) {
      case TabControl.TARGET_DOWNLOAD:  tab = downloadTab;  break;
      case TabControl.TARGET_UPLOAD:    tab = uploadTab;    break;
      case TabControl.TARGET_OTHER:     tab = otherTab;     break;

      case TabControl.TARGET_JOB:
         if ( ! jobHistory.activate() ) jobHistory.run(new JobHistoryDialog(this,global.jobManager));
         break;
         // tab stays null
      }
      // else ignore -- programmer error

      if (tab != null) tabbedPane.setSelectedComponent(tab);
   }

// --- implementation of EditSKUInterface ---

   // not called in pro mode

   public void editSKU(SKU sku) { doSetup(/* forceSKU = */ sku); }

// --- implementation of ChangeListener ---

   // not called in pro mode

   public void stateChanged(ChangeEvent e) {
      Style.style.refreshTabbedPane(tabbedPane);
   }

// --- password utility ---

   private static final int CP_OPERATION_EXIT  = 0;
   private static final int CP_OPERATION_SETUP = 1;
   private static final int CP_OPERATION_UNMINIMIZE = 2;

   private static final int CP_RESULT_NOT_REQUIRED = 0;
   private static final int CP_RESULT_RECEIVED     = 1;
   private static final int CP_RESULT_CANCELED     = 2;

   private int checkPassword(int operation) {
      Config config = global.control.getConfig();

   // is password required?

      boolean required;
      String requiredPassword = config.merchantConfig.password;
      boolean hide = false;

      switch (operation) {
      case CP_OPERATION_EXIT:
         required = config.passwordForExit;
         break;
      case CP_OPERATION_SETUP:
         required = config.passwordForSetup;
         break;
      case CP_OPERATION_UNMINIMIZE:
         required = config.passwordForUnminimize;
         requiredPassword = config.unminimizePassword; // not null by validation if required
         hide = true;
         break;
      default:
         throw new IllegalArgumentException();
      }

      if ( ! required ) return CP_RESULT_NOT_REQUIRED;

   // does user have it?

      // it might be cleaner to hide using the glass pane or layered pane,
      // but I can't make them work well.

      Component c = getContentPane().getComponent(0); // tabbed pane
      if (hide) c.setVisible(false);
      boolean received = new PasswordDialog(this,requiredPassword,alert).run();
      if (hide) c.setVisible(true);
      return received ? CP_RESULT_RECEIVED : CP_RESULT_CANCELED;
   }

// --- commands ---

   private void doSetup(SKU forceSKU) {

      try {
         if (checkPassword(CP_OPERATION_SETUP) == CP_RESULT_CANCELED) return;

         Config config = global.control.getConfig(); // get copy
         EditDialog dialog = ProMode.isPro(proMode) ? (EditDialog) new ConfigDialogPro(this,config)
                                                    : (EditDialog) new ConfigDialog   (this,config,forceSKU);
         if (dialog.run()) {
            try {
               global.control.setConfig(config);
            } catch (IOException e) {
               Pop.error(this,e,Text.get(this,"s16"));
            }
         }
      } catch (Throwable t) {
         Pop.diagnostics(this,Text.get(this,"s38"),t);
      }
   }

   private void doChangePrinterStatus() {

      // password not required, that's sort of the point here

      Config config = global.control.getConfig(); // get copy

      Queue[] queue = BackupDialog.getQueueArray(config.queueList);
      if (queue.length == 0) {
         Pop.error(this,Text.get(this,"e11"),Text.get(this,"s46"));
         return;
      }

      if (new BackupDialog(this,queue).run()) {
         try {
            global.control.setConfig(config);
         } catch (IOException e) {
            Pop.error(this,e,Text.get(this,"s47"));
         }
      }
      // I thought about having switchToBackup be stored in memory.xml
      // instead of config.xml.  it's not really a configuration setting,
      // since it can change during normal operation of LC.  but,
      // it changes so rarely, and it would be such a big hassle to have
      // it stored separately!  but the clearest deciding factor is,
      // if we stored it in memory.xml, we couldn't make changes remotely.
   }

   private void doChangeAutoPrint() {

      // password not required, that's sort of the point here

      Config config = global.control.getConfig(); // get copy

      AutoPrintDialog dialog = new AutoPrintDialog(this,config.autoSpawn);
      if (dialog.run()) {
         if (dialog.autoPrint != config.autoSpawn) { // optimization
            config.autoSpawn = dialog.autoPrint;
            try {
               global.control.setConfig(config);
            } catch (IOException e) {
               Pop.error(this,e,Text.get(this,"s49"));
            }
         }
      }
   }

   private void doRestore() {

      if (checkPassword(CP_OPERATION_SETUP) == CP_RESULT_CANCELED) return;

      Integer thisInstanceID = null;
      try {
         AutoUpdateConfig auc = global.control.getServerAUC();
         if (auc != null) thisInstanceID = auc.instanceID;
      } catch (Exception e) {
         Pop.error(this,ChainedException.format(Text.get(this,"e7"),e),Text.get(this,"s36"));
         return;
      }

      Config config = global.control.getConfig();
      // we only need this for the transaction parameters!
      // and a few fields that aren't restored, see below

      LinkedList instances = new LinkedList();
      try {
         new Restore.GetInstances(config.restoreURL,config.merchantConfig,instances).runInline();
         // there's no way to do a runInline with a DescribeHandler,
         // so just add the wrapper text to the ChainedException call, below
      } catch (Exception e) {
         Pop.error(this,ChainedException.format(Text.get(this,"e5"),e),Text.get(this,"s34"));
         return;
      }

      // a dialog with no instances would be confusing, so just block it
      if (instances.size() == 0) {
         Pop.info(this,Text.get(this,"e6"),Text.get(this,"s35"));
         return;
      }

      Config restore = new Restore.RestoreDialog(this,instances,thisInstanceID,config.autoUpdateConfig.autoConfigURL).run();
      if (restore == null) return; // canceled

      // everything else in the config file seems fine,
      // but I just don't think I should change these
      restore.autoUpdateConfig.lc = config.autoUpdateConfig.lc; // OK to share structure
      restore.autoUpdateConfig.invoice = config.autoUpdateConfig.invoice;
      restore.autoUpdateConfig.burner = config.autoUpdateConfig.burner;

      // actually these shouldn't change either ...
      // properties of the installation, not the config
      restore.orderDataDir = config.orderDataDir;
      restore.rollDataDir  = config.rollDataDir;
      // other directories should be restored, see email

      try {

         boolean restart = ! AutoConfig.equalsUndistributed(restore,config);
         // probably no need to precalculate

         global.control.setConfig(restore,config);

         if (restart) RestartDialog.activate(config.autoUpdateConfig.lc.currentVersion,(int) config.autoUpdateConfig.restartWaitInterval,/* isNewVersion = */ false);

      } catch (Exception e) {
         Pop.error(this,e,Text.get(this,"s37"));
      }
   }

   private void doSetupAutostart() {

      if ( ! ShellUtil.isAvailable() ) {
         Pop.info(this,Text.get(this,"e2"),Text.get(this,"s25"));
         return;
      }

      if (checkPassword(CP_OPERATION_SETUP) == CP_RESULT_CANCELED) return;

      String folder = ShellUtil.getFolderPath(ShellUtil.CSIDL_STARTUP);
      File shortcut = new File(folder,"LabCenter.lnk");

      boolean autostartActual = shortcut.exists();
      AutostartDialog dialog = new AutostartDialog(this,autostartActual);

      if ( ! dialog.run() ) return;

      boolean autostartDesired = dialog.autostart;
      if (autostartDesired == autostartActual) return;

      try {

         if (autostartDesired) { // create shortcut

            File workDir = new File(baseDir,"");
            // note, use this form instead of just baseDir
            // because it handles the baseDir == null case

            final String jar1 = "BaseLauncher.jar";
            final String jar2 = "Launcher.jar";
            String jar;
            if      (new File(workDir,jar1).exists()) jar = jar1;
            else if (new File(workDir,jar2).exists()) jar = jar2;
            else throw new Exception(Text.get(this,"e10",new Object[] { jar1, jar2 }));

            String arguments = "-jar " + jar + " labcenter_version run";

            int result = ShellUtil.createShortcut(Convert.fromFile(shortcut),
                                                  "javaw.exe",
                                                  arguments,
                                                  Text.get(this,"s27"),
                                                  Convert.fromFile(workDir),
                                                  null,0);
            if (result < 0) throw new Exception(Text.get(this,"e3"));

         } else { // remove shortcut

            boolean result = shortcut.delete();
            if ( ! result ) throw new Exception(Text.get(this,"e4"));
         }

      } catch (Exception e) {
         Pop.error(this,e,Text.get(this,"s26"));
      }
   }

   private void doCheckUpdate() {
      AutoUpdate.AnalysisRecord ar = AutoUpdate.analyze(global.control);
      if (new CheckUpdateDialog(this,ar).run()) {
         global.control.restart(ar.curLabCenter); // don't have new one!
      }
      // kind of stupid since most config and all invoice updates can
      // be processed without a restart, but I don't want to get into
      // all the complicated issues around that.
   }

   private void doOrderHistory() {
      if ( ! orderHistory.activate() ) orderHistory.run(new DendronHistoryDialog(this,global.orderManager));
   }

   private void doRollHistory() {
      if ( ! rollHistory.activate() ) rollHistory.run(new HistoryDialog(this,global.rollManager,global.control));
   }

   private void doHelpAbout() {
      new AboutDialog(this).run();
   }

   private void doHelpChat() {
      Browser.launch(this,global.control.getConfig().helpURL,Text.get(this,"s23"));
   }

   private void doHelpManual() {
      Browser.launch(this,global.control.getConfig().manualURL,Text.get(this,"s32"));
   }

   private void doHelpItem(String url) {
      Browser.launch(this,url,Text.get(this,"s44"));
   }

   private void doHelpAdmin(String type) {
      try {

         Config config = global.control.getConfig();
         if (config.merchantConfig.isWholesale) throw new Exception(Text.get(this,"e8"));

         Query query = new Query();
         query.add("mlrfnbr",Convert.fromInt(config.merchantConfig.merchant));
         query.add("type",type);
         Browser.launch(this,config.adminURL + query.getWithPrefix(),Text.get(this,"s42"));

      } catch (Exception e) {
         Pop.error(this,e,Text.get(this,"s41"));
      }
   }

   private void doWindowClose() {
      if (global.control.getConfig().minimizeAtClose) {
         User.iconify(this);
      } else {
         doExit();
      }
   }

   // flag that means the next unmininize is a programmatic alert (from User.tell)
   private boolean alert = false;
   public void setAlert() { alert = true; }

   private void doWindowUnminimize() {
      // we get called after un-minimize, so we need to do something
      // if the password check fails, not if it succeeds
      if (checkPassword(CP_OPERATION_UNMINIMIZE) == CP_RESULT_CANCELED) User.iconify(this);
      else User.deiconifySuccessful(); // show saved messages, if there are any
      alert = false;
   }

   private void doExit() {

      int cp = checkPassword(CP_OPERATION_EXIT);
      if (cp == CP_RESULT_CANCELED) return;

      if (cp == CP_RESULT_NOT_REQUIRED) { // still need some kind of prompt before exit

         boolean confirmed = Pop.confirm(this,Text.get(this,"e1"),Text.get(this,"s15"));
         if ( ! confirmed ) return;
      }

      global.control.exit();
   }

}

