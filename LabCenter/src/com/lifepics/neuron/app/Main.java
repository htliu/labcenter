/*
 * Main.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.axon.AbstractRollDialog;
import com.lifepics.neuron.axon.ImagePoller;
import com.lifepics.neuron.axon.LocalImageSubsystem;
import com.lifepics.neuron.axon.PakonSubsystem;
import com.lifepics.neuron.axon.PakonThread;
import com.lifepics.neuron.axon.PollSubsystem;
import com.lifepics.neuron.axon.ProMode;
import com.lifepics.neuron.axon.RollAdapter;
import com.lifepics.neuron.axon.RollManager;
import com.lifepics.neuron.axon.RollPurgeSubsystem;
import com.lifepics.neuron.axon.RollRetrySubsystem;
import com.lifepics.neuron.axon.RollUtil;
import com.lifepics.neuron.axon.ScanSubsystem;
import com.lifepics.neuron.axon.ScanThread;
import com.lifepics.neuron.axon.UploadSubsystem;
import com.lifepics.neuron.axon.Wholesale;
import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Log;
import com.lifepics.neuron.core.Nullable;
import com.lifepics.neuron.core.ReportQueue;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.dendron.AutoCompleteSubsystem;
import com.lifepics.neuron.dendron.CompletionSubsystem;
import com.lifepics.neuron.dendron.DownloadSubsystem;
import com.lifepics.neuron.dendron.DueSubsystem;
import com.lifepics.neuron.dendron.FormatSubsystem;
import com.lifepics.neuron.dendron.FormatThread;
import com.lifepics.neuron.dendron.GroupDerivation;
import com.lifepics.neuron.dendron.InvoiceSubsystem;
import com.lifepics.neuron.dendron.JobAdapter;
import com.lifepics.neuron.dendron.JobManager;
import com.lifepics.neuron.dendron.JobPurgeSubsystem;
import com.lifepics.neuron.dendron.JobUtil;
import com.lifepics.neuron.dendron.LocalStatus;
import com.lifepics.neuron.dendron.LocalSubsystem;
import com.lifepics.neuron.dendron.OrderAdapter;
import com.lifepics.neuron.dendron.OrderDialog;
import com.lifepics.neuron.dendron.OrderManager;
import com.lifepics.neuron.dendron.OrderPurgeSubsystem;
import com.lifepics.neuron.dendron.OrderRetrySubsystem;
import com.lifepics.neuron.dendron.OrderUtil;
import com.lifepics.neuron.dendron.ProductConfig;
import com.lifepics.neuron.dendron.QueueList;
import com.lifepics.neuron.dendron.SpawnSubsystem;
import com.lifepics.neuron.dendron.StaticDLS;
import com.lifepics.neuron.dendron.ThreadDefinition;
import com.lifepics.neuron.gui.Print;
import com.lifepics.neuron.gui.Style;
import com.lifepics.neuron.gui.User;
import com.lifepics.neuron.misc.AppUtil;
import com.lifepics.neuron.misc.FileMapper;
import com.lifepics.neuron.misc.FileUtil;
import com.lifepics.neuron.misc.KioskLog;
import com.lifepics.neuron.misc.ProcessException;
import com.lifepics.neuron.misc.PurgeConfig;
import com.lifepics.neuron.net.DefaultHandler;
import com.lifepics.neuron.net.Handler;
import com.lifepics.neuron.net.NetUtil;
import com.lifepics.neuron.net.PauseCallback;
import com.lifepics.neuron.net.RetryHandler;
import com.lifepics.neuron.net.ServerThread;
import com.lifepics.neuron.object.CopyUtil;
import com.lifepics.neuron.object.XML;
import com.lifepics.neuron.table.AlternatingFile;
import com.lifepics.neuron.table.AlternatingFileAutoNumber;
import com.lifepics.neuron.table.AlternatingFileStorage;
import com.lifepics.neuron.table.AutoNumber;
import com.lifepics.neuron.table.Storage;
import com.lifepics.neuron.table.Table;
import com.lifepics.neuron.table.TableAdapter;
import com.lifepics.neuron.thread.ErrorWindow;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.Socket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TimeZone;
import java.util.logging.Level;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.imageio.ImageIO;
import javax.swing.Timer;

/**
 * The main class for the whole application.
 */

public class Main extends AppUtil.Stub implements ScanThread.Callback, Global.Control, ServerThread.Callback {

// --- library list ---

   private static final String[] libraries = new String[] {

      /* DLS   */ "activation.jar", "KIASClientNew.jar", "mail.jar", "soap.jar", "xerces.jar", "jnicos.dll", "msvcr71.dll",
      /* shell */ "jnishell.dll", "msvcrt40.dll",
      /* Agfa  */ "agfa.jar", "ConnectionKit.dll", "GenOsse.dll", "PfdfApe.dll", "PfdfComm.dll", "PfdfDriver.dll",
      /* TIFF  */ "jai_imageio_11.jar",
      /* DNP   */ "jnicsp.dll", "cspstat.dll"
   };

   private static final String[] imageLib = new String[] {
      "ImageLib.exe", "LifePics.Core.BE.dll", "LifePics.Core.BL.dll", "LifePics.Core.DAL.dll", "LifePics.Opie.Util.dll"
   };

// --- main ---

   public static void main(String[] args) {
      main(args,/* launched = */ false); // ignore result
   }

   public static String main(String[] args, boolean launched) {
      return new Main().execute(args,launched);
   }

// --- main functions ---

   private Global global;

   protected String init(String causeKey, String[] args, boolean launched) throws ProcessException {
      global = new Global();
      global.control = this;

      // the order is only partly determined by dependencies

      AppUtil.initLookAndFeel(); // do first because we show dialog on exception
      NetUtil.initNetwork();

      initLogging1();
      initMainDir(args);
      initConfig();
      Style.setStyle(config.style);
      initMemory1();
      if ( ! initLock() ) throw new MultipleInstanceException(); // already running
      initLogging2(config.logCount,config.logSize,config.logLevel);
      initLogging3();
      KioskLog.init(mainDir,config.kioskLogCount,config.kioskLogSize);

      initTimeout();

      Log.log(Level.INFO,this,causeKey,new Object[] { AboutDialog.getVersion(), describeMerchant(config) }); // starting

      // special cases
      boolean storeConfigAtEnd = false;

      if (config.versionLoaded < 12) { // pre 2.0.4 ?

         // need to change these for all users
         //
         // the normal way to do these kinds of things is to bump the base version
         // on the individual fields, and set new default values.
         // I guess when I wrote this code, I hadn't figured out that approach yet.

         config.downloadConfig.listURL   = "https://services.lifepics.com/LabCenter/GetOrderList.asp";
         config.downloadConfig.statusURL = "https://services.lifepics.com/LabCenter/UpdateOrderStatus.asp";
      }

      if (config.versionLoaded <= 15) { // 2.1.7 or earlier ?

         try {
            storeMemory((Memory) new Memory().loadDefault(config.manualImageDir));
         } catch (IOException e) {
            throw new ProcessException(Text.get(this,"e15"),e);
         }

         storeConfigAtEnd = true;
      }

      boolean isRitz =    config.autoUpdateConfig.autoUpdateURL.equals("https://services.lifepics.com/getupdateinforitz.asp")
                       || config.autoUpdateConfig.autoUpdateURL.equals("https://services.lifepics.com/getupdateinforitzbeta.asp");

      if (config.versionLoaded < 29) { // Ritz adjustment

         // detect Ritz stores using the auto-update URL
         // since we don't have profiles available yet.
         // there are two variants so that they can test
         // at some stores without affecting all of them.
         //
         if (isRitz) {

            // this is sloppy ... mkdirs could leave a partial chain
            // of directories, or the config update could fail and
            // leave empty directories with nothing pointing at them.
            // it would also be pleasing to have the order and roll
            // changes linked into a single transaction, so that we
            // move both or neither.  in practice, though, it's all fine.

            // some stores have already been moved, so check for C drive
            // and don't move anything that's not there.
            File cDrive = new File("C:\\"); // equals allows lowercase
            File target = new File("D:\\LifePics Temporary Files");

            if (FileMapper.getRoot(config.orderDataDir).equals(cDrive)) {

               File dir = new File(target,"orders");
               if (dir.exists() || dir.mkdirs()) {
                  config.orderDataDir = dir;
               }
               // else just leave it alone
            }

            if (FileMapper.getRoot(config.rollDataDir).equals(cDrive)) {

               File dir = new File(target,"rolls");
               if (dir.exists() || dir.mkdirs()) {
                  config.rollDataDir = dir;
               }
               // else just leave it alone
            }
         }

         // even if we didn't change anything, write to config file
         // so that we'll know not to try adjusting again next time.
         storeConfigAtEnd = true;
      }

      if (config.versionLoaded < 32) { // new Ritz adjustment
         if (isRitz) {
            config.printConfig.markPay = true;
            config.printConfig.markPro = true;
            config.printConfig.markProNotMerchantID = new Integer(997); // Ritz of course
         }
         // since this adjustment has no side-effects, no need to save config
      }

      if (config.versionLoaded < 33) {
         if (isRitz) {
            config.printConfig.markPayAddMessage = "Ring as 289";
         }
      }

      if (config.versionLoaded < 34) {
         config.autoUpdateConfig.autoUpdateURL = "https://services.lifepics.com/autoupdate.asp";
         // this makes isRitz useless .. but it's fine, that's exactly what auto-config is for.
         // because of the storeConfig bug, this got lost in some cases, so I've copied it into
         // AutoUpdateConfig.  that way, even if an old config file surfaces, we won't be stuck
         // with the bad URL ever again.
      }

      if (storeConfigAtEnd) {
         try {
            storeConfig(config);
         } catch (IOException e) {
            throw new ProcessException(Text.get(this,"e13"),e);
         }
      }
      // the problem with storing the config in the middle was, we hadn't applied all the updates,
      // yet we were writing out a config file with the current version number on it.
      // so, if we shut down without ever writing the config file again, which is easily possible,
      // on restart we'd have a config with some updates absent, notably the auto-update one.

      // time-sensitive code, version was 41
      try {
         resetServer();
      } catch (IOException e) { // not a big deal, just log it
         Log.log(Level.WARNING,this,"e22",e);
      }

      // ** probably we shouldn't add any more version conditionals here **
      //
      // the trouble is, what if the file gets updated through ConfigCenter?
      // in that case, the changes won't ever be applied.  (also LC will
      // probably jam up because of the config file version, but then they'll
      // update to the new version and not have the conditional changes.)
      //
      // Q: why haven't we seen this problem in the field?
      // A: you can't update through ConfigCenter unless the config file is
      // on the server, and it doesn't go on the server unless you're using
      // LC 4.0 or higher, which is version 34.

      if (launched) {
         String result = initAutoUpdate();
         if (result != null) return result;
      }
      // else we're running in development, and these things are not useful.
      // check for auto-updates?  just a nuisance that interferes with launch.
      // extract libraries from the current jar?  there may not even be a jar.

      initMemory2();
      initImageIO(); // must come after auto-update and before upload thread

      config.queueList.precomputeBackupList();

      initOrderTable();
      initJobTable();
      initRollTable();

      // if there are no jobs at all, then when we create the first one,
      // Job.Ref loads before Job, and Ref.sd appears to be null.
      // as a temporary fix, just go ahead and force Job to load now.
      { Object o = com.lifepics.neuron.dendron.Job.sd; }

      initOrderManager();
      initJobManager();
      initRollManager();

      initAxonGlobal();
      initDendronGlobal();
      initPrint();
      initUser();
      initDLSBusy();
      initLocal1();

      initReporting();
      initRestart();

      initDownload();
      initUpload();
      initInvoice();
      initSpawn();
      initFormat();
      initCompletion();
      initJobPurge();
      initAutoComplete();
      initOrderPurge();

      initLocal2();
      initDue();
      initPakon();
      initScan();
      initPoll();
      initLocalImage();
      initRollPurge();
      initOrderRetry();
      initRollRetry();

      initErrorWindow();
      initFrame();
      initTimers();

      return null;
   }

   protected void run() {
      frame.setVisible(true);
      User.setFrame(frame);
      RestartDialog.setFrame(frame,this);

      if (minimizeCommand || config.minimizeAtStart) {
         User.iconify(frame); // should wrap in invokeLater, but this seems to work fine
      }
      // because this comes after setVisible, LC becomes visible
      // for a moment before iconifying ... at least, a frame outline does.
      // that's not great, but if we swap the calls, LC starts iconified
      // but grabs the focus off whoever had it before; so, this is better.
      // (it still grabs the focus, but it gives it back when it iconifies)
   }

   protected void exit(String causeKey) {

      exitUser();
      exitTimers();

      exitRollRetry();
      exitOrderRetry();
      exitRollPurge();
      exitLocalImage();
      exitPoll();
      exitScan();
      exitPakon();
      exitDue();
      exitLocal();

      exitOrderPurge();
      exitAutoComplete();
      exitJobPurge();
      exitCompletion();
      exitFormat();
      exitSpawn();
      exitInvoice();
      exitUpload();
      exitDownload();

      exitRestart();
      exitReporting();

      exitFrame(); // need frame for subsystem exit

      Log.log(Level.INFO,this,causeKey); // stopping

      KioskLog.exit();
      exitLogging();
      exitLock();
   }

// --- constants ---

   // directory structure
   private static final String SERVER_FILE = "server.xml";
   private static final String MEMORY_FILE = "memory.xml";
   private static final String STATUS_FILE = "status.xml";
   private static final String NEWS_FILE = "news.html";
   private static final String NEWS_TEMP = "news.tmp";

   private static final String BACKUP_DIR  = "backup";
   private static final String BACKUP_PREFIX = "config-";
   private static final String BACKUP_SUFFIX = ".xml";

   private static final String ORDER_QUEUE_DIR = "queue/order";
   private static final String JOB_QUEUE_DIR = "queue/job";
   private static final String ROLL_QUEUE_DIR = "queue/roll";
   private static final String QUEUE_SUFFIX = "xml";
   private static final String JOB_AUTO_NUMBER_FILE = "queue/nextjob";
   private static final String ROLL_AUTO_NUMBER_FILE = "queue/nextroll";
   private static final String STYLESHEET_FILE = "invoice.xsl"; // but see next section!
   private static final String STYLESHEET_ARCHIVE = "invoice-custom.xsl";
   private static final String STYLESHEET_POSTAGE = "postage.xsl";

   private static final String TRANSFORM_FILE = "transform.jpg";

   private static final String NAME_CONFIG = "Config";
   private static final String NAME_SERVER = "Server"; // (*)
   private static final String NAME_MEMORY = "Memory";

   // (*) the content of the config and server files is the same,
   // but I want to discourage people from dropping config files
   // on top of server files, since that would cause misbehavior.

// --- file helpers ---

   // the stylesheet file ends up in two places, the Invoice constructor and
   // OrderParser.writePrefix.  in writePrefix I think we don't want to use
   // custom invoice file names, it's too weird to have those embedded there.
   // and, I really doubt anyone double-clicks the XML any more to see their
   // invoices.  if so, standard is good enough.

   private File getStylesheetFileStandard() {
      return new File(mainDir,STYLESHEET_FILE);
   }

   private File getStylesheetFile(Config config) {
      String currentVersion = config.autoUpdateConfig.invoice.currentVersion;
      return (currentVersion != null) ? new File(mainDir,currentVersion) : getStylesheetFileStandard();
   }

   private File getBurnerFile(Config config) {
      String currentVersion = config.autoUpdateConfig.burner.currentVersion;
      return (currentVersion != null) ? new File(mainDir,currentVersion) : null;
   }

// --- lock ---

   // init: main dir - lock
   // init: config - lock

   private ServerThread lockServer;

   /**
    * Create a lock to prevent multiple copies in the same configuration.
    */
   private boolean initLock() throws ProcessException {

      ServerThread temp = new ServerThread(config.lockPort,this);

      boolean bound;
      try {
         bound = temp.bind();
      } catch (SocketException e) {
         bound = false;
      } catch (IOException e) { // this doesn't happen
         throw new ProcessException(Text.get(this,"e4"),e);
      }
      // on Windows 7 we get SocketException instead of false,
      // so catch and handle that.  (not BindException!)
      // maybe I should just do the same for all IOExceptions,
      // but let's keep the two separate for now.
      // if I ever want to make it more specific, the message
      // in the UI is, "Unrecognized Windows Sockets error: 0:
      // JVM_Bind".  I don't have direct evidence that it's
      // a SocketException, and the colons could be from ChainedException,
      // but I get plenty of Google hits on that exact text.

      if ( ! bound ) try { // LabCenter is already running, try to bring to front
         new Socket("127.0.0.1",config.lockPort).close();
         return false;
         // the string constant is ugly, but InetAddress.getLocalHost has some
         // overhead where it looks up the host name -- not worth messing with
      } catch (IOException e) {
         throw new ProcessException(Text.get(this,"e3")); // fall back on message
      }

      // don't set the real variable until the lock is active.
      // this isn't a big deal, since stopNice wouldn't crash,
      // but this is the correct way.
      //
      lockServer = temp;
      lockServer.start();
      return true;
   }

   public void accepted(Socket socket) {
      try {
         socket.close();
      } catch (Exception e) {
         // ignore
      }

      if (frame != null) {
         EventQueue.invokeLater(new Runnable() { public void run() {
            User.toFront(frame,/* alert = */ false);
         } });
         // shouldn't call toFront from inside server thread
      }
   }

   private void exitLock() {
      if (lockServer != null) lockServer.stopNice();
   }

// --- logging ---

   private ReportQueue reportQueue;

   private void initLogging3() {
      reportQueue = new ReportQueue(config.reportQueueSize);
      Log.setReportInterface(config.reportLevel,reportQueue);
      Log.setLocalReportInterface(new LocalStatus());
   }

// --- config ---

   // init: main dir - config

   private File configFile;
   private Config config;
   private File serverFile;
   private File backupDir;

   private void initConfig() throws ProcessException {

      configFile = new File(mainDir,CONFIG_FILE);
      try {
         config = loadConfig();
      } catch (Exception e) {
         throw new ProcessException(Text.get(this,"e5"),e);
      }

      // make relative paths be relative to location of config file,
      // rather than relative to the current working directory
      //
      config.makeRelativeTo(mainDir);

      serverFile = new File(mainDir,SERVER_FILE);
      backupDir = new File(mainDir,BACKUP_DIR);
   }

// --- memory ---

   // init: main dir - memory

   private File memoryFile;
   private Memory memory;

   private void initMemory1() {
      memoryFile = new File(mainDir,MEMORY_FILE);
   }

   private void initMemory2() throws ProcessException {

      try {
         memory = loadMemory();
      } catch (Exception e) {
         throw new ProcessException(Text.get(this,"e14"),e);
      }

      memory.makeRelativeTo(mainDir);
   }

// --- timeout ---

   // init: config - timeout

   private void initTimeout() {
      NetUtil.setDefaultTimeout(config.defaultTimeoutInterval);
   }

   private void reinitTimeout() { initTimeout(); }

// --- auto-update ---

   // init: main dir - auto update
   // init: config - auto update
   // init: timeout - auto update

   private AutoUpdate.InstanceRecord irSave = new AutoUpdate.InstanceRecord(); // preallocate in case no initAutoUpdate

   private String initAutoUpdate() throws ProcessException {

   // (0) grab server config if available

      boolean autoConfig = true;
      LinkedList softwareList = new LinkedList();
      Config server = null;
      AutoUpdate.InstanceRecord ir = irSave;

      try {
         server = loadServer();
         if (server != null) {
            ir.instanceID = server.autoUpdateConfig.instanceID;
            ir.passcode   = server.autoUpdateConfig.passcode;
         }
         // else just leave all three null, auto-update will allocate
      } catch (Exception e) {
         // server file is corrupt, do not auto-config (which would overwrite)
         autoConfig = false;
         Log.log(Level.SEVERE,this,"e17",e); // call for help
      }

   // (1) handle auto-update

      Config copy = config.copy();

      Handler handler = new RetryHandler(new DefaultHandler(),copy.diagnoseConfig.startupRetries,copy.diagnoseConfig.startupRetryInterval);
      PauseCallback callback = null;

      String result = AutoUpdate.update(baseDir,copy.autoUpdateConfig,copy.merchantConfig,ir,softwareList,handler,callback);
      boolean cleaned1 = AutoUpdate.clean(baseDir,copy.autoUpdateConfig.lc);

      if (softwareList.isEmpty()) autoConfig = false; // empty list means server read failed

   // (2) set up for next steps

      String useName;
      boolean overwriteInvoice;

      if (result != null) {
         useName = result;
         overwriteInvoice = true;
      } else {
         useName = copy.autoUpdateConfig.lc.currentVersion;
         overwriteInvoice = false;
      }

      File useFile = new File(baseDir,useName);

   // (2 1/4) deal with old customInvoiceXsl field

      // best way to think about this is, write out a chart of all the cases,
      // updating or not (result != null) x three values of customInvoiceXsl,
      // and see what you want to happen in each case.  archive off?  extract?
      // set field to null?  store config?  the first time the code runs will
      // be after the update to 7.1.0, with result == null, but we still want
      // to handle all cases correctly.

      boolean cleaned2 = false;

      // ought to use copy instead of config until we store it
      if (copy.customInvoiceXsl != null) {

         if (copy.customInvoiceXsl.booleanValue()) {

            // archive off the custom invoice.xsl just in case
            File before = getStylesheetFileStandard();
            if (before.exists()) { // really should
               File after = new File(mainDir,FileUtil.disambiguate(mainDir,STYLESHEET_ARCHIVE));
               // doesn't exist, that's why we called disambiguate
               before.renameTo(after); // ignore failure, see next comment
            }
            // set this.  if the archive step was successful it won't matter,
            // since we always extract when there's no file on disk,
            // but if the archive step failed we do want to try to overwrite.
            overwriteInvoice = true;
         }

         copy.customInvoiceXsl = null;
         cleaned2 = true;
         // note, the point of allowing null as a value and using that instead of false
         // is just to make it less likely that someone will go in and reset it to true
      }

   // (2 1/2) maybe store config

      if (result != null || cleaned1 || cleaned2) {
         try {
            storeConfig(copy);
            // if we can't store, let the exception pass upward
            // so that we warn the user and don't relaunch
         } catch (IOException e) {
            throw new ProcessException(Text.get(this,"e8"),e);
         }
         config = copy;
      }

   // (3) extract stylesheet (if auto-update or if not present)

      // we're going to be switching to a different version,
      // so try to extract the stylesheet that goes with it.

      AutoUpdate.extract(useFile,STYLESHEET_FILE,mainDir,overwriteInvoice);

   // (3 1/2) extract PhotoYCC profile (if not present)

      // the file PYCC.pf isn't included in the JRE install, only in the JDK,
      // simply because it's large and rarely needed in general applications.
      //
      // Java searches various places for .pf files (see the documentation
      // for java.awt.color.ICC_Profile.getInstance for the general idea,
      // or the source code for privilegedOpenProfile for useful specifics),
      // but the first one typically isn't set, and the second one doesn't
      // work because our classpath is LC.jar, and it isn't smart enough to
      // look inside.  so, putting it into the Java install is the only way,
      // and also a good way since it lets other people use it.
      //
      // in a JDK, the location is jre\lib\cmm inside the install directory.
      //
      // note, the system doesn't seem to cache the profiles, so we can just
      // add the file and not worry about restarting or anything.
      //
      // note, we have some PYCC TIFF images that just won't read under Java 1.4,
      // even when the .pf file is there.
      // the symptom is new IllegalArgumentException("Invalid ICC Profile Data"),
      // coming from ICC_Profile.java line 705, called by the TIFF reader.

      String javaDir = System.getProperty("java.home");
      if (javaDir != null) { // should always be true

         File pyccDir = new File(new File(javaDir,"lib"),"cmm");
         String pyccFile = "PYCC.pf";
         // constants are only used here, no need to factor out

         AutoUpdate.extract(useFile,pyccFile,pyccDir,/* overwrite = */ false);
      }

   // (4) extract libraries (if not present)

      // the theory is, give every different version of a library file
      // a different name, then old applications run just as before,
      // and there's no problem with collisions and overwriting at install.

      // when an auto-update introduces a new library, it might look like
      // the new libraries would get extracted here, and we'd only need to
      // restart once, but unfortunately that's not how it works ...
      // the list of libraries we're checking is the list from the old app.
      // we could extract all top-level files (including invoice.xsl again),
      // or extract a list of what to extract, but it's not worth it now.

      boolean extracted = false;

      for (int i=0; i<libraries.length; i++) {
         extracted |= AutoUpdate.extract(useFile,libraries[i],baseDir,/* overwrite = */ false);
         // if the target already exists, calling with no overwrite immediately returns false
      }

      // extract without auto-update is mainly for new installs.
      // in any case, restart so Java will link in the new jars.
      //
      if (extracted && result == null) result = useName; // equals lc.currentVersion

   // (4 1/2) extract ImageLib libraries

      // not sure I can change the names of these, so try a different plan.
      // also, unlike other libraries, we don't link to ImageLib statically,
      // so it can't happen that we've tried to link, failed, and now need
      // to restart.

      // I was planning to condition this on localShareEnabled != null, but
      // actually let's just extract all the time.  the libraries are small,
      // and this way I don't have to build some convoluted restart trigger
      // for when we enable kiosk mode.

      AutoUpdate.extractSet(useFile,imageLib,baseDir);

   // (5) check for auto-config

      if (autoConfig) {

         AutoConfig.PushConfig pushConfig = new AutoConfig.PushConfig() {

            public void pushConfig(Config config) throws Exception {

               logIfMerchantChanged(config,Main.this.config);

               storeConfig(config);
               Main.this.config = config.copy();
               // same as above, except here we have to copy too

               // here is the special redistribution code!
               // any config fields that are used before now,
               // but don't cause a restart, must be updated.
               reinitTimeout();
            }

            public void pushServer(Config config, AutoConfig.PushCallback callback) throws Exception {
               storeServer(config,callback);
            }
         };

         boolean restart = AutoConfig.execute(config.copy(),server,ir,softwareList,pushConfig,handler,callback,baseDir,mainDir);
         // we can pass in server file without copying since we're done with it

         if (restart && result == null) result = useName; // as above
      }

   // (6) done

      return result;
   }

// --- ImageIO ---

   // the issue here is that the TIFF plugin wasn't being detected.
   //
   // calling ImageIO.scanForPlugins by itself didn't fix it,
   // but if you look at the source code, that calls IIORegistry's
   // function registerApplicationClasspathSpis, which scans for
   // plugins in the thread's context class loader ... which, since
   // I haven't set it to anything else, is the system class loader.
   //
   // setting the context class loader permanently strikes me as
   // a bad idea, since it would probably have all kinds of unknown
   // magic effects, but setting it and then setting it back seems
   // safe enough ... basically just a way of giving a class loader
   // as an argument to the function.
   //
   // this must come after auto-update, because the ImageIO plugin registry
   // is a global object, and we don't want to have entries in it that
   // point back to the previous class loader.  I haven't actually observed
   // a problem there, I'm just trying to avoid one.
   //
   // it has to come before the upload thread, because that uses the
   // plugins to transform things.

   private void initImageIO() {
      Thread thread = Thread.currentThread();

      ClassLoader saveLoader = thread.getContextClassLoader();
      thread.setContextClassLoader(this.getClass().getClassLoader());

      ImageIO.scanForPlugins(); // (*)

      thread.setContextClassLoader(saveLoader);
   }

   // (*) this <i>should</i> be when ImageIO and IIORegistry
   // load for the first time; if not, then someone is using
   // it without the benefit of all the plugins.
   // so, calling scanForPlugins actually makes it scan twice.
   // however, if we just forced the class to load, and this
   // turned out <i>not</i> to be the first time, it would be
   // bad.  so, scan twice, who cares.

// --- order table ---

   // init: main dir - order table

   private void initOrderTable() throws ProcessException {

      File queueDir = new File(mainDir,ORDER_QUEUE_DIR);
      Storage storage = new AlternatingFileStorage(queueDir,QUEUE_SUFFIX);
      TableAdapter adapter = new OrderAdapter();
      try {
         global.orderTable = new Table(adapter,storage,null);
      } catch (Exception e) {
         throw new ProcessException(Text.get(this,"e9"),e);
      }

      global.groupTable = global.orderTable.derive(new GroupDerivation());
   }

// --- job table ---

   // init: main dir - job table

   private void initJobTable() throws ProcessException {

      File queueDir = new File(mainDir,JOB_QUEUE_DIR);
      File autoNumberFile = new File(mainDir,JOB_AUTO_NUMBER_FILE);

      // code to create job table for pre-1.5 installations.
      // this can go away as soon as everyone is updated.
      // (*) the argument "1", below, is part of this code.
      //
      if ( ! queueDir.exists() && ! queueDir.mkdir() ) throw new ProcessException(Text.get(this,"e12"));

      Storage storage = new AlternatingFileStorage(queueDir,QUEUE_SUFFIX);
      TableAdapter adapter = new JobAdapter();
      try {
         AutoNumber autoNumber = new AlternatingFileAutoNumber(autoNumberFile,1); // (*)
         global.jobTable = new Table(adapter,storage,autoNumber);
      } catch (Exception e) {
         throw new ProcessException(Text.get(this,"e11"),e);
      }
   }

// --- roll table ---

   // init: main dir - roll table

   private void initRollTable() throws ProcessException {

      File queueDir = new File(mainDir,ROLL_QUEUE_DIR);
      File autoNumberFile = new File(mainDir,ROLL_AUTO_NUMBER_FILE);
      Storage storage = new AlternatingFileStorage(queueDir,QUEUE_SUFFIX);
      TableAdapter adapter = new RollAdapter();
      try {
         AutoNumber autoNumber = new AlternatingFileAutoNumber(autoNumberFile);
         global.rollTable = new Table(adapter,storage,autoNumber);
      } catch (Exception e) {
         throw new ProcessException(Text.get(this,"e10"),e);
      }
   }

// --- order manager ---

   // init: config - order manager
   // init: order table - order manager

   private void initOrderManager() {

   // create order manager

      global.orderManager = new OrderManager(global.orderTable,config.merchantConfig,config.downloadConfig,config.orderDataDir,getStylesheetFile(config),new File(mainDir,STYLESHEET_POSTAGE),config.warnNotPrinted,config.carrierList.enableTracking);

   // init purge subsystem

      OrderUtil.setTimeZone(TimeZone.getTimeZone(config.printConfig.timeZone));
      OrderUtil.setPurgeConfig(config.purgeConfig);
      // global.orderManager.autoPurge(); // not needed, now that we have order purge subsystem
   }

// --- job manager ---

   // init: config - job manager
   // init: order table - job manager
   // init: job table - job manager

   private void initJobManager() {

   // create job manager

      global.jobManager = new JobManager(global.orderTable,global.jobTable,config.queueList);

   // init purge subsystem

      JobUtil.setPurgeConfig(config.purgeConfig);
      // global.jobManager.autoPurge(); // not needed, now that we have job purge subsystem

   // connect to order manager

      global.orderManager.jobManager = global.jobManager;
   }

// --- roll manager ---

   // init: config - roll manager
   // init: roll table - roll manager

   private void initRollManager() {

   // create roll manager

      global.rollManager = new RollManager(global.rollTable,config.merchantConfig,config.rollDataDir,config.purgeConfig.tryPurgeLocalImageURL);

   // init purge subsystem

      RollUtil.setPurgeConfig(config.purgeConfig);
      // global.rollManager.autoPurge(); // not needed, now that we have roll purge subsystem
   }

// --- axon global ---

   // init: config - axon global

   private void initAxonGlobal() {
      ProMode.setProMode(config.proMode);
      AbstractRollDialog.setConfig(config.proEmail,config.uploadConfig.transformConfig,config.claimEnabled,config.claimEmail,config.priceLists,config.dealers);
      Wholesale.setWholesale(config.merchantConfig.isWholesale);
      ImagePoller.setDelayInterval(config.pollConfig.imageDelayInterval);
   }

// --- dendron global ---

   // init: config - dendron global

   private void initDendronGlobal() {
      OrderDialog.setCarrierList(config.carrierList);
   }

// --- print ---

   // init: config - print

   private void initPrint() {
      Print.setConfig(config.printConfig,config.productBarcodeConfig);
   }

// --- user ---

   // init: config - user

   private void initUser() {
      User.setConfig(config.userConfig);
   }

   private void exitUser() {
      User.terminate();
   }
   // these are not really parallel functions, in case you haven't noticed

// --- DLS busy ---

   // init: config - DLS busy

   private void initDLSBusy() {
      StaticDLS.configure(config.dlsBusyPollInterval,config.dlsBusyRetries);
   }

// --- reporting ---

   // closely related to logging, but has its own thread, so goes here

   // init: config - reporting
   // init: logging2 - reporting
   // exit: reporting - frame

   private ReportSubsystem.Config rsc;
   private ReportSubsystem reportSubsystem;

   private void initReporting() {

      rsc = new ReportSubsystem.Config();
      rsc.reportQueue = reportQueue;
      rsc.reportURL = config.reportURL;
      rsc.idlePollInterval = 10000;
      rsc.merchantConfig = config.merchantConfig;
      rsc.diagnoseConfig = config.diagnoseConfig;

      reportSubsystem = new ReportSubsystem(rsc);
      reportSubsystem.init(true);
   }

   private void exitReporting() {
      if (reportSubsystem != null) {
         reportSubsystem.exit(frame);
      }
   }

// --- restart ---

   // closely related to auto-update

   // init: main dir - restart
   // init: config - restart
   // exit: restart - frame

   private RestartSubsystem.Config rssc;
   private RestartSubsystem restartSubsystem;

   private void initRestart() {

      rssc = new RestartSubsystem.Config();
      rssc.baseDir = baseDir;
      rssc.mainDir = mainDir;
      rssc.autoUpdateConfig = config.autoUpdateConfig;
      rssc.merchantConfig = config.merchantConfig;
      rssc.diagnoseConfig = config.diagnoseConfig;
      rssc.reconfig = getReconfigInterface(); // goes with config locking

      restartSubsystem = new RestartSubsystem(rssc);
      restartSubsystem.init(config.autoUpdateConfig.enableRestart);

      // to be parallel with the auto-update check at startup,
      // we ought to prevent the thread from running if we weren't launched.
      // however, that rule is just a convenience for development,
      // and here the most convenient thing is to have it run normally.
   }

   private void exitRestart() {
      if (restartSubsystem != null) {
         restartSubsystem.exit(frame);
      }
   }

// --- download ---

   // init: config - download
   // init: order table - download
   // init: order manager - download
   // exit: download - frame

   private DownloadSubsystem.Config dsc;

   private void initDownload() {

      dsc = new DownloadSubsystem.Config();
      dsc.table = global.orderTable;
      dsc.orderManager = global.orderManager;
      dsc.merchantConfig = config.merchantConfig;
      dsc.downloadConfig = config.downloadConfig; // ok to share
      dsc.dataDir = config.orderDataDir;
      dsc.stylesheet = getStylesheetFileStandard();
      dsc.holdInvoice = config.holdInvoice;
      dsc.productConfig = config.productConfig;
      dsc.coverInfos = config.coverInfos;
      dsc.enableItemPrice = config.enableItemPrice;
      dsc.localShareDir = Nullable.nbToB(config.localShareEnabled) ? config.localShareDir : null;
      dsc.diagnoseConfig = config.diagnoseConfig;

      global.downloadSubsystem = new DownloadSubsystem(dsc);
      global.downloadSubsystem.init(config.downloadEnabled && ! ProMode.isPro(config.proMode));
   }

   private void exitDownload() {
      if (global.downloadSubsystem != null) {
         global.downloadSubsystem.exit(frame);
      }
   }

// --- upload ---

   // init: config - upload
   // init: roll table - upload
   // exit: upload - frame

   private UploadSubsystem.Config usc;

   private void initUpload() {

      usc = new UploadSubsystem.Config();
      usc.table = global.rollTable;
      usc.merchantConfig = config.merchantConfig;
      usc.uploadConfig = config.uploadConfig; // ok to share
      usc.transformFile = new File(mainDir,TRANSFORM_FILE);
      usc.dealers = config.dealers;
      usc.prioritizeEnabled = Nullable.nbToB(config.localShareEnabled) && ! ProMode.isPro(config.proMode);
         // this is one of the few subsystems that can run in pro mode,
         // so we need the pro mode check here in the config field instead
      usc.rollReceivedPurgeInterval = config.purgeConfig.rollReceivedPurgeInterval;
      usc.diagnoseConfig = config.diagnoseConfig;

      global.uploadSubsystem = new UploadSubsystem(usc);
      global.uploadSubsystem.init(config.uploadEnabled);
   }

   private void exitUpload() {
      if (global.uploadSubsystem != null) {
         global.uploadSubsystem.exit(frame);
      }
   }

// --- invoice ---

   // init: config - invoice
   // init: order table - invoice
   // init: job manager - invoice
   // init: print - invoice
   // exit: invoice - frame

   private InvoiceSubsystem.Config isc;

   private void initInvoice() {

      isc = new InvoiceSubsystem.Config();
      isc.table = global.orderTable;
      isc.idlePollInterval = config.downloadConfig.idlePollInterval; // reuse config
      isc.stylesheet = getStylesheetFile(config);
      isc.postageXSL = new File(mainDir,STYLESHEET_POSTAGE);
      isc.jobManager = global.jobManager;
      isc.printerInvoice = config.printConfig.setupInvoice.printer;
      isc.printerLabel = config.printConfig.setupLabel.printer;

      global.invoiceSubsystem = new InvoiceSubsystem(isc);
      global.invoiceSubsystem.init( ! ProMode.isPro(config.proMode) );
   }

   private void exitInvoice() {
      if (global.invoiceSubsystem != null) {
         global.invoiceSubsystem.exit(frame);
      }
   }

// --- spawn ---

   // init: config - spawn
   // init: order table - spawn
   // init: job manager - spawn
   // exit: spawn - frame

   private SpawnSubsystem.Config ssc;

   private void initSpawn() {

      ssc = new SpawnSubsystem.Config();
      ssc.table = global.orderTable;
      ssc.idlePollInterval = config.downloadConfig.idlePollInterval; // reuse config
      ssc.autoSpawn = config.autoSpawn;
      ssc.autoSpawnSpecial = config.autoSpawnSpecial;
      ssc.autoSpawnPrice = config.autoSpawnPrice;
      ssc.jobManager = global.jobManager;

      global.spawnSubsystem = new SpawnSubsystem(ssc);
      global.spawnSubsystem.init( ! ProMode.isPro(config.proMode) );
   }

   private void exitSpawn() {
      if (global.spawnSubsystem != null) {
         global.spawnSubsystem.exit(frame);
      }
   }

// --- format ---

   // init: config - format
   // init: order table - format
   // init: job table - format
   // exit: format - frame

   private FormatSubsystem.Config fsc;

   private void initFormat() {

      fsc = new FormatSubsystem.Config();
      fsc.jobTable = global.jobTable;
      fsc.idlePollInterval = config.downloadConfig.idlePollInterval; // reuse config
      fsc.orderTable = global.orderTable;
      // queues, isQueueSubset, and dueMap handled by updateFormatSubsystems
      fsc.burnerFile = getBurnerFile(config);

      global.formatSubsystems = new LinkedList();
      updateFormatSubsystems(config.queueList,config.productConfig);
   }

   private void exitFormat() {
      if (global.formatSubsystems != null) {
         Iterator i = global.formatSubsystems.iterator();
         while (i.hasNext()) {
            ThreadDefinition tOld = (ThreadDefinition) i.next();
            tOld.formatSubsystem.exit(frame);
         }
         // not worth combining with updateFormatSubsystems
      }
   }

   /**
    * Given a list of ThreadDefinition with formatSubsystem set to null,
    * rebuild the list in Global (which has formatSubsystem not null),
    * calling init, reinit, and exit as appropriate.  Existing subsystems
    * that continue to exist will always receive a reinit call.
    */
   private boolean updateFormatSubsystems(QueueList queueList, ProductConfig productConfig) {

   // set up

      LinkedList goal = CopyUtil.copyList(queueList.threads);
      // take ownership so we can modify and keep as global.formatSubsystems

      ThreadDefinition tDefault = new ThreadDefinition();
      tDefault.threadID = null;
      tDefault.threadName = null;

      goal.addFirst(tDefault);

      // the due map is medium-expensive to construct, and usually not needed,
      // so let's skip it if possible
      HashMap dueMap = FormatThread.usesDueMap(queueList.queues) ? FormatThread.buildDueMap(productConfig) : null;

   // first pass, exit

      Iterator i = global.formatSubsystems.iterator();
      while (i.hasNext()) {
         ThreadDefinition tOld = (ThreadDefinition) i.next();
         ThreadDefinition tNew = QueueList.findThreadByID(goal,tOld.threadID);

         if (tNew == null) {
            tOld.formatSubsystem.exit(frame);
         }
      }

   // second pass, init/reinit

      i = goal.iterator();
      while (i.hasNext()) {
         ThreadDefinition tNew = (ThreadDefinition) i.next();
         ThreadDefinition tOld = QueueList.findThreadByID(global.formatSubsystems,tNew.threadID);

         boolean enabled = ! ProMode.isPro(config.proMode);

         if (goal.size() > 1) {

            fsc.queues = queueList.getQueueSubset(tNew.threadID);
            fsc.isQueueSubset = true;
            fsc.dueMap = (dueMap != null && FormatThread.usesDueMap(fsc.queues)) ? dueMap : null;
            // check dueMap first to avoid unnecessary subtests

            enabled &= (fsc.queues.size() > 0);

         } else {

            fsc.queues = queueList.queues;
            fsc.isQueueSubset = false;
            fsc.dueMap = dueMap;
         }
         // because subsystems make copies of their configs,
         // we can reuse the same fsc for all subsystems
         // and poke in the right queues at the last minute

         if (tOld == null) {
            tNew.formatSubsystem = new FormatSubsystem(fsc);
            tNew.formatSubsystem.init(enabled);
         } else {
            tNew.formatSubsystem = tOld.formatSubsystem;
            tNew.formatSubsystem.reinit(enabled,fsc,frame);
         }
         // so, tNew.formatSubsystem is set in any case
      }

      fsc.queues = null; // not necessary but tidy
      fsc.dueMap = null;

   // done

      boolean rebuildFS = ! ThreadDefinition.sd.equalsElements(goal,global.formatSubsystems);
      // the equals function doesn't include the formatSubsystem field on the object,
      // but it would be harmless if it did, since equal threadID <=> equal formatSubsystem.

      global.formatSubsystems = goal;

      return rebuildFS;
   }

// --- completion ---

   // init: config - completion
   // init: job table - completion
   // init: job manager - completion
   // exit: completion - frame

   private CompletionSubsystem.Config csc;

   private void initCompletion() {

      csc = new CompletionSubsystem.Config();
      csc.jobTable = global.jobTable;
      csc.scanInterval = config.scanInterval;
      csc.jobManager = global.jobManager;

      global.completionSubsystem = new CompletionSubsystem(csc);
      global.completionSubsystem.init( ! ProMode.isPro(config.proMode) );
   }

   private void exitCompletion() {
      if (global.completionSubsystem != null) {
         global.completionSubsystem.exit(frame);
      }
   }

// --- job purge ---

   // init: config - job purge
   // init: job table - job purge
   // init: job manager - job purge
   // exit: job purge - frame

   private JobPurgeSubsystem.Config jpsc;

   private static long adjustedScanInterval(long scanInterval, long purgeInterval) {

      // compute how often to check for purge, which depends on the purge interval.
      // if the interval is measured in days, once an hour is fine;
      // if the interval is zero, we want to check often, at the default scan interval;
      // in fact that should be a lower bound for the result.
      // in between, we want some function, which I arbitrarily choose to be linear.

      // divide by 24 so that one day becomes one hour
      long fraction = purgeInterval / 24;
      if (fraction > 3600000     ) fraction = 3600000; // one hour
      if (fraction < scanInterval) fraction = scanInterval;
      return fraction;
   }

   private static long jobScanInterval(long scanInterval, PurgeConfig purgeConfig) {
      long purgeInterval = purgeConfig.jobPurgeInterval;
      if (purgeConfig.autoPurgeStale || purgeConfig.autoPurgeStaleLocal) {
         purgeInterval = Math.min(purgeInterval,purgeConfig.jobStalePurgeInterval);
      }
      return adjustedScanInterval(scanInterval,purgeInterval);
   }

   private void initJobPurge() {

      jpsc = new JobPurgeSubsystem.Config();
      jpsc.jobTable = global.jobTable;
      jpsc.scanInterval = jobScanInterval(config.scanInterval,config.purgeConfig);
      jpsc.jobManager = global.jobManager;
      jpsc.autoPurgeStale = config.purgeConfig.autoPurgeStale;
      jpsc.autoPurgeStaleLocal = config.purgeConfig.autoPurgeStaleLocal;

      global.jobPurgeSubsystem = new JobPurgeSubsystem(jpsc);
      global.jobPurgeSubsystem.init(config.purgeConfig.autoPurgeJobs && ! ProMode.isPro(config.proMode));
   }

   private void exitJobPurge() {
      if (global.jobPurgeSubsystem != null) {
         global.jobPurgeSubsystem.exit(frame);
      }
   }

// --- autocomplete ---

   // init: config - autocomplete
   // init: order table - autocomplete
   // init: order manager - autocomplete
   // exit: autocomplete - frame

   private AutoCompleteSubsystem.Config acsc;

   private void initAutoComplete() {

      acsc = new AutoCompleteSubsystem.Config();
      acsc.table = global.orderTable;
      acsc.scanInterval = config.scanInterval;
      acsc.orderManager = global.orderManager;
      acsc.autoCompleteConfig = config.autoCompleteConfig;
      acsc.storeHours = config.storeHours;
      acsc.diagnoseConfig = config.diagnoseConfig;
      acsc.enableTracking = config.carrierList.enableTracking;

      global.autoCompleteSubsystem = new AutoCompleteSubsystem(acsc);
      global.autoCompleteSubsystem.init(config.autoComplete && ! ProMode.isPro(config.proMode));
   }

   private void exitAutoComplete() {
      if (global.autoCompleteSubsystem != null) {
         global.autoCompleteSubsystem.exit(frame);
      }
   }

// --- order purge ---

   // init: config - order purge
   // init: order table - order purge
   // init: order manager - order purge
   // exit: order purge - frame

   private OrderPurgeSubsystem.Config opsc;

   private static long orderScanInterval(long scanInterval, PurgeConfig purgeConfig) {
      long purgeInterval = purgeConfig.orderPurgeInterval;
      if (purgeConfig.autoPurgeStale || purgeConfig.autoPurgeStaleLocal) {
         purgeInterval = Math.min(purgeInterval,purgeConfig.orderStalePurgeInterval);
      }
      return adjustedScanInterval(scanInterval,purgeInterval);
   }

   private void initOrderPurge() {

      opsc = new OrderPurgeSubsystem.Config();
      opsc.table = global.orderTable;
      opsc.scanInterval = orderScanInterval(config.scanInterval,config.purgeConfig);
      opsc.orderManager = global.orderManager;
      opsc.autoPurgeStale = config.purgeConfig.autoPurgeStale;
      opsc.autoPurgeStaleLocal = config.purgeConfig.autoPurgeStaleLocal;

      global.orderPurgeSubsystem = new OrderPurgeSubsystem(opsc);
      global.orderPurgeSubsystem.init(config.purgeConfig.autoPurgeOrders && ! ProMode.isPro(config.proMode));
   }

   private void exitOrderPurge() {
      if (global.orderPurgeSubsystem != null) {
         global.orderPurgeSubsystem.exit(frame);
      }
   }

// --- local ---

   // init: config - local
   // init: order table - local
   // exit: local - frame

   private LocalSubsystem.Config lsc;

   private void initLocal1() {
      LocalStatus.setConfig(config.localEnabled && ! ProMode.isPro(config.proMode),config.localConfig); // do early because other subsystems can cause local errors reports
   }

   private void initLocal2() {

      lsc = new LocalSubsystem.Config();
      lsc.localConfig = config.localConfig;
      lsc.table = global.orderTable;
      lsc.stylesheet = getStylesheetFileStandard();
      lsc.coverInfos = config.coverInfos;
      lsc.enableItemPrice = config.enableItemPrice;

      global.localSubsystem = new LocalSubsystem(lsc);
      global.localSubsystem.init(config.localEnabled && ! ProMode.isPro(config.proMode));
   }

   private void exitLocal() {
      if (global.localSubsystem != null) {
         global.localSubsystem.exit(frame);
      }
   }

// --- due ---

   // init: config - due
   // init: order table - due
   // exit: due - frame

   private DueSubsystem dueSubsystem;
   private DueSubsystem.Config esc;

   private void initDue() {

      esc = new DueSubsystem.Config();
      esc.table = global.orderTable;
      esc.scanInterval = config.scanInterval;
      esc.productConfig = config.productConfig;
      esc.soonInterval = config.userConfig.soonInterval;
      esc.skuDue = config.userConfig.skuDue;
      esc.skus = config.userConfig.skus;
      esc.timeZone = config.printConfig.timeZone;

      dueSubsystem = new DueSubsystem(esc);
      dueSubsystem.init( ! ProMode.isPro(config.proMode) );
   }

   private void exitDue() {
      if (dueSubsystem != null) {
         dueSubsystem.exit(frame);
      }
   }

// --- pakon ---

   // init: config - pakon
   // init: rollManager - pakon

   private PakonSubsystem pakonSubsystem;

   private void initPakon() {
      pakonSubsystem = new PakonSubsystem(config.pakonEnabled && ! ProMode.isPro(config.proMode),config.pakonPort,
         new PakonThread.Callback() {
            public void received(String uploadDirectory, String bagID, int totalBytesInDirectory, String email) throws Exception {
               global.rollManager.createPakon(uploadDirectory,bagID,totalBytesInDirectory,email);
            }
         });
   }

   private void exitPakon() {
      if (pakonSubsystem != null) pakonSubsystem.exit();
   }

// --- scan ---

   // init: config - scan
   // init: roll table - scan
   // init: roll manager - scan
   // exit: scan - frame

   private ScanSubsystem.Config nsc; // ssc already used

   private void initScan() {

      nsc = new ScanSubsystem.Config();
      nsc.table = global.rollTable;
      nsc.idlePollInterval = config.uploadConfig.idlePollInterval; // copy from upload side
      nsc.scannerPollInterval = config.scannerPollInterval;
      nsc.configDLS = config.scanConfigDLS;
      nsc.callback = this;
      nsc.rollManager = global.rollManager;

      global.scanSubsystem = new ScanSubsystem(nsc);
      global.scanSubsystem.init(config.scanEnabled && ! ProMode.isPro(config.proMode));
   }

   private void exitScan() {
      if (global.scanSubsystem != null) {
         global.scanSubsystem.exit(frame);
      }
   }

// --- poll ---

   // init: config - poll
   // init: roll manager - poll
   // exit: poll - frame

   private PollSubsystem.Config psc;

   private void initPoll() {

      psc = new PollSubsystem.Config();
      psc.pollConfig = config.pollConfig;
      psc.merchantConfig = config.merchantConfig;
      psc.dealers = config.dealers;
      psc.rollManager = global.rollManager;

      global.pollSubsystem = new PollSubsystem(psc);
      global.pollSubsystem.init(config.pollEnabled && ! ProMode.isPro(config.proMode));
   }

   private void exitPoll() {
      if (global.pollSubsystem != null) {
         global.pollSubsystem.exit(frame);
      }
   }

// --- local image ---

   // init: config - local image
   // init: roll manager - local image
   // exit: local image - frame

   private LocalImageSubsystem.Config lisc;

   private void initLocalImage() {

      lisc = new LocalImageSubsystem.Config();
      lisc.localShareDir = config.localShareDir;
      lisc.localImageConfig = config.localImageConfig;
      lisc.rollManager = global.rollManager;

      global.localImageSubsystem = new LocalImageSubsystem(lisc);
      global.localImageSubsystem.init(Nullable.nbToB(config.localShareEnabled) && ! ProMode.isPro(config.proMode));
   }

   private void exitLocalImage() {
      if (global.localImageSubsystem != null) {
         global.localImageSubsystem.exit(frame);
      }
   }

// --- roll purge ---

   // init: config - roll purge
   // init: roll table - roll purge
   // init: roll manager - roll purge
   // exit: roll purge - frame

   private RollPurgeSubsystem.Config rpsc;

   private static boolean autoPurgeRolls(PurgeConfig purgeConfig) {
      return (    purgeConfig.autoPurgeManual
               || purgeConfig.autoPurgePakon
               || purgeConfig.autoPurgeHot
               || purgeConfig.autoPurgeDLS );
      // getPurgeDate returns null for any incorrect types,
      // this is just a test whether we need to run at all.
   }

   private void initRollPurge() {

      rpsc = new RollPurgeSubsystem.Config();
      rpsc.table = global.rollTable;
      rpsc.scanInterval = adjustedScanInterval(config.scanInterval,config.purgeConfig.rollPurgeInterval);
      rpsc.rollManager = global.rollManager;
      rpsc.diagnoseConfig = config.diagnoseConfig;

      global.rollPurgeSubsystem = new RollPurgeSubsystem(rpsc);
      global.rollPurgeSubsystem.init(autoPurgeRolls(config.purgeConfig));
   }

   private void exitRollPurge() {
      if (global.rollPurgeSubsystem != null) {
         global.rollPurgeSubsystem.exit(frame);
      }
   }

// --- order retry ---

   // init: config - order retry
   // init: order table - order retry
   // exit: order retry - frame

   private OrderRetrySubsystem orderRetrySubsystem;
   private OrderRetrySubsystem.Config orsc;

   private void initOrderRetry() {

      orsc = new OrderRetrySubsystem.Config();
      orsc.table = global.orderTable;
      orsc.scanInterval = config.scanInterval;
      orsc.pauseRetryInterval = config.diagnoseConfig.pauseRetryInterval;

      orderRetrySubsystem = new OrderRetrySubsystem(orsc);
      orderRetrySubsystem.init( ! ProMode.isPro(config.proMode) );
   }

   private void exitOrderRetry() {
      if (orderRetrySubsystem != null) {
         orderRetrySubsystem.exit(frame);
      }
   }

// --- roll retry ---

   // init: config - roll retry
   // init: roll table - roll retry
   // exit: roll retry - frame

   private RollRetrySubsystem rollRetrySubsystem;
   private RollRetrySubsystem.Config rrsc;

   private void initRollRetry() {

      rrsc = new RollRetrySubsystem.Config();
      rrsc.table = global.rollTable;
      rrsc.scanInterval = config.scanInterval;
      rrsc.pauseRetryInterval = config.diagnoseConfig.pauseRetryInterval;

      rollRetrySubsystem = new RollRetrySubsystem(rrsc);
      rollRetrySubsystem.init(true);
   }

   private void exitRollRetry() {
      if (rollRetrySubsystem != null) {
         rollRetrySubsystem.exit(frame);
      }
   }

// --- error window ---

   // init: config - error window
   // init: download - error window
   // init: upload - error window
   // init: autocomplete - error window

   private void initErrorWindow() {

      // one for each network-connecting subsystem

      global.errorWindowDownload     = constructErrorWindow();
      global.errorWindowUpload       = constructErrorWindow();
      global.errorWindowAutoComplete = constructErrorWindow();

      global.downloadSubsystem    .addListener(global.errorWindowDownload);
      global.uploadSubsystem      .addListener(global.errorWindowUpload);
      global.autoCompleteSubsystem.addListener(global.errorWindowAutoComplete);
   }

   private void reinitErrorWindow() {

      reinitErrorWindow(global.errorWindowDownload);
      reinitErrorWindow(global.errorWindowUpload);
      reinitErrorWindow(global.errorWindowAutoComplete);
   }

   public void pingErrorWindow() {
      global.errorWindowDownload.ping();
      global.errorWindowUpload  .ping();
      global.errorWindowAutoComplete.ping();
   }

   private ErrorWindow constructErrorWindow() {
      return new ErrorWindow(config.errorWindowEnabled,
                             config.errorWindowInterval,
                             config.errorWindowPercent);
   }

   private void reinitErrorWindow(ErrorWindow errorWindow) {
      errorWindow.reinit(config.errorWindowEnabled,
                         config.errorWindowInterval,
                         config.errorWindowPercent);
   }

// --- frame ---

   // init: global - frame
   // init: main dir - frame

   private MainFrame frame;

   private void initFrame() {
      boolean showScanThread = config.scanEnabled && ! ProMode.isPro(config.proMode);
      boolean showKioskThread = Nullable.nbToB(config.localShareEnabled) && ! ProMode.isPro(config.proMode);
      boolean showCpsCommand = config.queueList.enableBackup && ! ProMode.isPro(config.proMode);
      frame = new MainFrame(global,baseDir,new File(mainDir,NEWS_FILE),config.proMode,config.preventStopStart,showScanThread,showKioskThread,showCpsCommand,config.helpItems,config.showQueueTab,config.showGrandTotal,config.frameTitleSuffix,config.userConfig.renotifyInterval);
      User.setEditSKUInterface(frame);
      OrderDialog.setEditSKUInterface(frame);
   }

   private void exitFrame() {

      // if (frame != null) frame.dispose();
      //
      // that's the idea, but we also need to close any history frames
      // that are open, so ...

      if (frame != null) Kill.kill();

      // most of the time this call is followed by a System.exit, so it
      // doesn't matter what we close, but if we're restarting, it does.
      // the test for null frame is nice because it's like the original
      // idea and because it avoids activating AWT unnecessarily.
      // (I'm not sure Frame.getFrames would activate it, but it might.)
   }

   private void doStatusNews() {
      StatusNews.initiate(config,irSave, // trust to read only (for both fields)
                          frame.getCounts(),
                          global.downloadSubsystem.getKbpsLast(),
                          global.uploadSubsystem  .getKbpsLast(),
                          new File(mainDir,STATUS_FILE),
                          new File(mainDir,NEWS_FILE),
                          new File(mainDir,NEWS_TEMP),
                          new Runnable() { public void run() { frame.reloadNews(); } });
   }

// --- timers ---

   // init: config - timers
   // init: frame - timers (because doStatusNews uses frame)

   private Timer timerErrorWindow;
   private Timer timerTrackers;
   private Timer timerStatusNews;

   private void initTimers() {

      timerErrorWindow = new Timer(60000,new ActionListener() { public void actionPerformed(ActionEvent e) { pingErrorWindow(); } });
      timerErrorWindow.start();

      timerTrackers = new Timer(1000,new ActionListener() { public void actionPerformed(ActionEvent e) { pingTrackers(); } });
      timerTrackers.start();

      timerStatusNews = new Timer(config.statusNewsInterval,new ActionListener() { public void actionPerformed(ActionEvent e) { doStatusNews(); } });
      timerStatusNews.setInitialDelay(0);
         // can't just call doStatusNews ourselves, it has to run in UI thread
      timerStatusNews.start();
   }

   private void reinitTimers() {

      // error window timer currently not configurable
      // same for tracker timer

      if (timerStatusNews.getDelay() != config.statusNewsInterval) {

         timerStatusNews.setDelay(config.statusNewsInterval);
         timerStatusNews.restart();
         // this produces an immediate ping, so don't do it unless needed
      }
   }

   private void exitTimers() {
      if (timerErrorWindow != null) timerErrorWindow.stop();
      if (timerTrackers    != null) timerTrackers   .stop();
      if (timerStatusNews  != null) timerStatusNews .stop();

      StatusNews.terminate(); // now that no more new threads
   }

   private void pingTrackers() {
      global.downloadSubsystem.ping();
      global.uploadSubsystem  .ping();
   }

// --- config helpers ---

   private Config loadConfig() throws IOException, ValidationException {
      AlternatingFile af = new AlternatingFile(configFile);
      try {
         InputStream inputStream = af.beginRead();
         Config c = (Config) XML.loadStream(inputStream,new Config(),NAME_CONFIG);
         AutoConfig.validateConfig(c); // check no instance ID or revision number
         return c;
      } finally {
         af.endRead();
      }
   }

   private void storeConfig(Config config) throws IOException {
      AlternatingFile af = new AlternatingFile(configFile);
      try {
         OutputStream outputStream = af.beginWrite();
         XML.storeStream(outputStream,config,NAME_CONFIG);
         if (config.backupCount == 0) {
            af.commitWrite();
         } else {
            af.commitWrite(getBackup());
            cleanBackup(config.backupCount);
         }
         // note a minor flaw here, if you set the count to zero,
         // existing backup files won't be cleaned up.
         // we could fix this, but then we'd either have to scan
         // the backup dir pointlessly at every save, or do some
         // fancy before-and-after config comparison.
      } finally {
         af.endWrite();
      }
   }

   private static SimpleDateFormat backupFormat = new SimpleDateFormat(Text.get(Main.class,"f1"));
      // the config is only saved in the UI thread, no sync needed

   private static FileFilter backupFilter = new FileFilter() {
      public boolean accept(File file) {
         if (file.isDirectory()) return false;
         String name = file.getName();
         return name.startsWith(BACKUP_PREFIX) && name.endsWith(BACKUP_SUFFIX);
      }
   };

   private File getBackup() {

      // this line isn't really needed ... (a) config file does always exist,
      // (b) even if it didn't, lastModified would return zero, no problem,
      // (c) the result wouldn't be used anyway, since there's no file to back up.
      // but, this is nice, do it anyway.  there's also a synchronization issue ...
      // what if the user deletes the config file between this line and the next?
      // it doesn't matter, for reasons (a) - (c).
      //
      if ( ! configFile.exists() ) return null;

      if ( ! backupDir.exists() ) backupDir.mkdir();
         // ignore result; if it fails, commitWrite(..) will just delete

      String date = backupFormat.format(new Date(configFile.lastModified()));
      return new File(backupDir,BACKUP_PREFIX + date + BACKUP_SUFFIX);

      // note, we use the last-modified time so that the time stamp tells
      // when the config was first used, not when it was replaced.
   }

   private void cleanBackup(int backupCount) {

      // have to pass in backupCount, since here config.backupCount
      // would mean the old count, not the new one

      File[] files = backupDir.listFiles(backupFilter);
      FileUtil.sortByName(files); // same as by date, intentionally

      int count = files.length - backupCount; // may be negative
      for (int i=0; i<count; i++) {
         files[i].delete(); // ignore result
      }
   }

   private static String describeMerchant(Config config) {
      String key = config.merchantConfig.isWholesale ? "i7b" : "i7a";
      return Text.get(Main.class,key,new Object[] { Convert.fromInt(config.merchantConfig.merchant) });
   }

   private static void logIfMerchantChanged(Config after, Config before) {
      if (    after.merchantConfig.merchant    != before.merchantConfig.merchant
           || after.merchantConfig.isWholesale != before.merchantConfig.isWholesale ) {
         Log.log(Level.INFO,Main.class,"i8",new Object[] { describeMerchant(before), describeMerchant(after) });
      }
   }

// --- server helpers ---

   /**
    * Unlike the config file, the server file may or may not exist.
    * If it doesn't exist, return null; if it's corrupt, throw an exception.
    */
   private Config loadServer() throws IOException, ValidationException {
      AlternatingFile af = new AlternatingFile(serverFile);
      if ( ! af.exists() ) return null;
      try {
         InputStream inputStream = af.beginRead();
         Config c = (Config) XML.loadStream(inputStream,new Config(),NAME_SERVER);
         AutoConfig.validateServer(c); // check instance ID and revision number
         return c;
      } finally {
         af.endRead();
      }
   }

   /**
    * It so happens that whenever we write into the server file,
    * we want the write to be atomic with some other operation.
    * This isn't quite atomic, but it's pretty good, only fails
    * if we can't delete the old server file.
    * I allow the callback to be null, but that's just a formality.
    */
   private void storeServer(Config config, AutoConfig.PushCallback callback) throws Exception {
      AlternatingFile af = new AlternatingFile(serverFile);
      try {
         OutputStream outputStream = af.beginWrite();
         XML.storeStream(outputStream,config,NAME_SERVER);
         outputStream.close(); // (*)
         if (callback != null) callback.run(af.getTarget());
         af.commitWrite();
      } finally {
         af.endWrite();
      }
      // (*) without this, the file size may report as zero
      // for a few seconds .. which is bad for the callback.
   }

   private void resetServer() throws IOException {

      final long T_RESET = 1225519200000L; // 2008-11-01 00:00:00 MDT
      // (under new daylight savings rules that Java 1.4 doesn't have)

      AlternatingFile af = new AlternatingFile(serverFile);
      if (    af.exists()
           && af.getLastModified() < T_RESET ) af.delete();

      // the server-side config database got corrupted, so all existing instances
      // can no longer sync up.  so, delete the local server files and start over.
   }

   /**
    * @return The AutoUpdateConfig from the server file, or null if the file doesn't exist.
    */
   public AutoUpdateConfig getServerAUC() throws Exception {
      Config config = loadServer();
      return (config == null) ? null : config.autoUpdateConfig;
      // note, the nullable integers in the AUC can't be null
      // because of the validateServer call inside loadServer
   }

// --- memory helpers ---

   // some notes on why the memory data is in a separate file.
   //  * the point of having config backups is to be able to see the last
   //    few config changes, not the last few times a roll was created.
   //  * in other words ... the config data changes much more slowly than
   //    the memory data, so they belong in separate files.
   //  * the config data now changes only when the user explicitly OKs it,
   //    the memory data changes through other user actions.
   //  * I want to protect against email addresses, which should be local,
   //    getting caught up and sent around in a standard config file.

   // some notes on why the memory data isn't backed up.
   //  * I'm lazy
   //  * in the config case, backup was desirable because the users could
   //    easily lose lots of data by changing the queue integration type.
   //    in the memory case, (a) there's no scenario where the users lose
   //    data without knowing it, and (b) the data should be convenience
   //    information, nothing critical or hard to reconstruct.

   private Memory loadMemory() throws IOException, ValidationException {
      return (Memory) AlternatingFile.load(memoryFile,new Memory(),NAME_MEMORY);
   }

   private void storeMemory(Memory memory) throws IOException {
      AlternatingFile.store(memoryFile,memory,NAME_MEMORY);
   }

   private void saveMemory() {
      try {
         storeMemory(memory); // exception would occur here
      } catch (IOException e) {
         // ignore
      }
   }

// --- implementation of ScanThread.Callback ---

   // these and the rest of the memory functions have to be synchronized,
   // because these are called from ScanThread, and the rest from the UI.
   // load, store, and saveMemory are internal functions called by these,
   // no need for sync there, and everything before that runs before the
   // initScan call, when there's also no need for sync.

   // note, there's no problem with the UI thread overwriting the scan thread,
   // or vice versa, because they write into completely different fields.
   // all we have to do is make sure that the writes are serialized properly.

   public synchronized LinkedList getDLSOrderIDs() {
      return new LinkedList(memory.dlsOrderIDs);
   }

   public synchronized void setDLSOrderIDs(LinkedList dlsOrderIDs) throws Exception {

      // this shouldn't be called when there's no change, but check anyway
      if ( ! dlsOrderIDs.equals(memory.dlsOrderIDs) ) {

         memory.dlsOrderIDs = new LinkedList(dlsOrderIDs);
         storeMemory(memory);
         // in this case it's very important to know that the memory worked,
         // so use storeMemory, not saveMemory, and don't catch exceptions
      }
   }

// --- implementation of RollManager.MemoryInterface ---

   // memory interface doesn't copy the whole object,
   // just copies individual fields as needed.

   public synchronized File getManualImageDir() {
      return memory.manualImageDir;
   }

   public synchronized void setManualImageDir(File dir) {
      if ( ! dir.equals(memory.manualImageDir) ) {
         memory.manualImageDir = dir; // keep even if we can't store!
         saveMemory();
      }
   }

   public synchronized LinkedList getAddressList() {
      return new LinkedList(memory.addressList);
   }

   public synchronized void setAddressList(LinkedList list) {
      if ( ! list.equals(memory.addressList) ) {
         memory.addressList = new LinkedList(list);
         saveMemory();
      }
   }

   public synchronized void setBoth(File dir, LinkedList list) {
      if (    ! dir .equals(memory.manualImageDir)
           || ! list.equals(memory.addressList)    ) {
         memory.manualImageDir = dir;
         memory.addressList = new LinkedList(list);
         saveMemory();
      }
   }

// --- config locking code ---

   // all getConfig-setConfig calls come from the UI, so they're
   // put in order that way, and there's never a conflict.
   // that policy should be sustainable - any background thread
   // should be notified of config changes, and so have no need
   // to be messing with the config.
   //
   // the complication is, the auto-config process.
   // it needs to do two things:
   //
   // * block all config writes temporarily, because it does
   //   a read on the actual config file.
   //   the block needs to last from pullConfig to done.
   //
   // * force a config write itself.  it may or may not do one;
   //   if it does, any getConfig-setConfig in progress should
   //   be blocked permanently.
   //
   // the first part is easy, we just have a blocked flag.
   // the second part is trickier.  in theory, it'd be sufficient
   // to have a flag that says "has the UI called getConfig",
   // and set it when that happens, and clear it when auto-config
   // calls pushConfig.  but, if something goes wrong with
   // the order of calls in the UI, that fails badly, writing an
   // old config on top of whatever we received from auto-config.
   // so, instead of a boolean flag, we'll keep a weak reference
   // to the last vended object, and only that object can be used
   // for setConfig.  that shouldn't cause any trouble in the UI,
   // and if it does, it's not a bad failure, just an error message
   // for the user, who can then re-do whatever it was (on top of
   // the updated config)

   // synchronization analysis:  as usual, the ideal is that any
   // synced block should finish quickly without waiting on anything.
   // getConfig, pullConfig, and done all do that.
   // but, setConfig and pushConfig don't, so we have to think more.
   //
   // a deadlock happens if we get into one sync block,
   // wait for something to happen, and that something
   // requires getting into another sync block.
   //
   // can we get into a deadlock if pushConfig is running?  no.
   // the proof is, the only threads that hit these sync blocks
   // are the UI thread and restart thread, and if we get into
   // the pushConfig sync block, we know what both threads are doing.
   // (we've also fixed it so setConfigImpl won't wait for the
   // restart thread to exit, although that's a different kind of deadlock.)
   //
   // so, what about if setConfig is running?  it's in the UI thread,
   // and can happen any time the user saves a config change.
   // can the restart thread hit a sync block?  yes, definitely.
   // can the UI thread wait on the restart thread?  also yes.
   // however, the restart thread can't be just anywhere ...
   // pushConfig and done are only called when blocked is set,
   // and in that case, setConfig will exit without waiting.
   // so the problem case is, the user saves a config change
   // that affects the restart thread, and just at that moment
   // restart thread makes a call to pullConfig and locks up.
   // the only solution I can see is, move the restart thread wait
   // outside the sync block ... so do that.
   //
   // by the way, according to a quick experiment I did, sync waits
   // are not interruptible even by Thread.interrupt.

   private Object configSync = new Object();
   private boolean blocked = false;
   private WeakReference configLock = null;
   private boolean pushed = false;

   public Config getConfig() {
      synchronized (configSync) {
         Config c = getConfigImpl();
         configLock = new WeakReference(c);
         return c;
      }
   }

   public void setConfig(Config config) throws IOException {
      setConfig(config,config);
   }

   /**
    * @param auth The config object you received from getConfig,
    *             which authorizes the setConfig call.
    *             Normally we write onto this config object and
    *             pass it as the first argument too, but that's
    *             not required.
    */
   public void setConfig(Config config, Config auth) throws IOException {
      synchronized (configSync) {
         if (blocked) throw new IOException(Text.get(this,"e18"));
         if (configLock == null) throw new IOException(Text.get(this,"e19"));
         if (configLock.get() != auth) throw new IOException(Text.get(this,"e20"));
         setConfigImpl(config);
         // can you call setConfig again with the same object?  I guess so
      }
      restartSubsystem.reinit(config.autoUpdateConfig.enableRestart,rssc,frame);
   }

   private RestartThread.Reconfig getReconfigInterface() {
      return new RestartThread.Reconfig() {

         public Config pullConfig() {
            synchronized (configSync) {
               blocked = true;
               pushed = false;
               return getConfigImpl();
            }
         }

         public void pushConfig(Config config) throws Exception {
            final Config configF = config;
            EventQueue.invokeAndWait(new Runnable() { public void run() {
               try {
                  pushConfigImpl(configF);
               } catch (Exception e) {
                  throw new RuntimeException(Text.get(Main.class,"e21"),e);
                  // this comes over as an InvocationTargetException
               }
            } });
         }
         private void pushConfigImpl(Config config) throws Exception {
            synchronized (configSync) {
               configLock = null;
               setConfigImpl(config);
               pushed = true;
               // important that flag is set only if call completes successfully
            }
         }
         // the setConfigImpl code was written with the assumption that
         // it was running in the UI thread, and it's just too alarming
         // to think about trying to change that.  for example ...
         // * the MainFrame reinit can create and destroy UI components
         // * the config info is copied into all kinds of static fields
         //   without any interlocks.  the only reason that works is,
         //   the fields are never accessed from outside the UI thread.
         // * if subsystems stop and start, we might bring up a thread
         //   stop dialog, which naturally assumes we're in the UI thread.

         public void done() {
            synchronized (configSync) {
               blocked = false;
               if (pushed) {
                  EventQueue.invokeLater(new Runnable() { public void run() {
                     restartSubsystem.reinit(config.autoUpdateConfig.enableRestart,rssc,frame);
                  } });
                  pushed = false; // unnecessary but pleasing
               }
               // setConfigImpl no longer reinits the restart subsystem.
               // so, if we make a successful call to it, we set a flag
               // and do the reinit ourselves later.
               // without that, any config change that affected the restart subsystem
               // would make the UI thread block waiting for the restart thread
               // to exit, while the restart thread would be blocked waiting for the
               // call into the UI thread to return.
               // so, by the way, this can't be an invokeAndWait, for the same reason.
               //
               // the reinit isn't guaranteed to be timely, so in theory we could
               // get a config change from the UI in between ... but no matter,
               // extra reinits aren't bad, there just needs to be one at the end.
            }
         }

         public Config pullServer() throws Exception {
            return loadServer();
         }
         public void pushServer(Config config, AutoConfig.PushCallback callback) throws Exception {
            storeServer(config,callback);
         }
         // the server functions are essentially static,
         // and can run in the restart thread with no problem.
         // there's never a collision on the server file
         // because the only other place it's used is in the
         // startup auto-update sequence, which is completed
         // before the restart thread starts.
      };
   }

// --- implementation of Global.Control ---

   private Config getConfigImpl() {
      return config.copy(); // so caller can modify
   }

   private void setConfigImpl(Config config) throws IOException {

      if (config != this.config && config.equals(this.config)) return;
      //
      // don't trap config == this.config, because sometimes we modify
      // the config object in place and still want to write it to disk.
      // actually I'm not sure that's true any more, but keep the code.

      // merchant fields are not modified in place
      logIfMerchantChanged(config,this.config);

      // fields that cause refresh are not modified in place
      boolean refreshTimeZone = ! config.printConfig.timeZone.equals(this.config.printConfig.timeZone);
      boolean refreshPurge    = ! config.purgeConfig.equals(this.config.purgeConfig);

      storeConfig(config); // exception would occur here
      if (config != this.config) this.config = config.copy();

      config.queueList.precomputeBackupList();

   // reconfigure

      // note, not all subsystem config fields need to be updated

      rsc.reportURL = config.reportURL;
      rsc.merchantConfig = config.merchantConfig;
      rsc.diagnoseConfig = config.diagnoseConfig;

      rssc.autoUpdateConfig = config.autoUpdateConfig;
      rssc.merchantConfig = config.merchantConfig;
      rssc.diagnoseConfig = config.diagnoseConfig;

      dsc.merchantConfig = config.merchantConfig;
      dsc.downloadConfig = config.downloadConfig;
      dsc.dataDir = config.orderDataDir;
      dsc.holdInvoice = config.holdInvoice;
      dsc.productConfig = config.productConfig;
      dsc.coverInfos = config.coverInfos;
      dsc.enableItemPrice = config.enableItemPrice;
      dsc.localShareDir = Nullable.nbToB(config.localShareEnabled) ? config.localShareDir : null;
      dsc.diagnoseConfig = config.diagnoseConfig;

      usc.merchantConfig = config.merchantConfig;
      usc.uploadConfig = config.uploadConfig;
      usc.dealers = config.dealers;
      usc.prioritizeEnabled = Nullable.nbToB(config.localShareEnabled) && ! ProMode.isPro(config.proMode);
      usc.rollReceivedPurgeInterval = config.purgeConfig.rollReceivedPurgeInterval;
      usc.diagnoseConfig = config.diagnoseConfig;

      isc.idlePollInterval = config.downloadConfig.idlePollInterval;
      isc.stylesheet = getStylesheetFile(config);
      isc.printerInvoice = config.printConfig.setupInvoice.printer;
      isc.printerLabel = config.printConfig.setupLabel.printer;

      ssc.idlePollInterval = config.downloadConfig.idlePollInterval;
      ssc.autoSpawn = config.autoSpawn;
      ssc.autoSpawnSpecial = config.autoSpawnSpecial;
      ssc.autoSpawnPrice = config.autoSpawnPrice;

      fsc.idlePollInterval = config.downloadConfig.idlePollInterval;
      // queues, isQueueSubset, and dueMap handled by updateFormatSubsystems
      fsc.burnerFile = getBurnerFile(config);

      csc.scanInterval = config.scanInterval;

      jpsc.scanInterval = jobScanInterval(config.scanInterval,config.purgeConfig);
      jpsc.autoPurgeStale = config.purgeConfig.autoPurgeStale;
      jpsc.autoPurgeStaleLocal = config.purgeConfig.autoPurgeStaleLocal;

      acsc.scanInterval = config.scanInterval;
      acsc.autoCompleteConfig = config.autoCompleteConfig;
      acsc.storeHours = config.storeHours;
      acsc.diagnoseConfig = config.diagnoseConfig;
      acsc.enableTracking = config.carrierList.enableTracking;

      opsc.scanInterval = orderScanInterval(config.scanInterval,config.purgeConfig);
      opsc.autoPurgeStale = config.purgeConfig.autoPurgeStale;
      opsc.autoPurgeStaleLocal = config.purgeConfig.autoPurgeStaleLocal;

      lsc.localConfig = config.localConfig;
      lsc.coverInfos = config.coverInfos;
      lsc.enableItemPrice = config.enableItemPrice;

      esc.scanInterval = config.scanInterval;
      esc.productConfig = config.productConfig;
      esc.soonInterval = config.userConfig.soonInterval;
      esc.skuDue = config.userConfig.skuDue;
      esc.skus = config.userConfig.skus;
      esc.timeZone = config.printConfig.timeZone;

      nsc.idlePollInterval = config.uploadConfig.idlePollInterval;
      nsc.scannerPollInterval = config.scannerPollInterval;
      nsc.configDLS = config.scanConfigDLS;

      psc.pollConfig = config.pollConfig;
      psc.merchantConfig = config.merchantConfig;
      psc.dealers = config.dealers;

      lisc.localShareDir = config.localShareDir;
      lisc.localImageConfig = config.localImageConfig;

      rpsc.scanInterval = adjustedScanInterval(config.scanInterval,config.purgeConfig.rollPurgeInterval);
      rpsc.diagnoseConfig = config.diagnoseConfig;

      orsc.scanInterval = config.scanInterval;
      orsc.pauseRetryInterval = config.diagnoseConfig.pauseRetryInterval;

      rrsc.scanInterval = config.scanInterval;
      rrsc.pauseRetryInterval = config.diagnoseConfig.pauseRetryInterval;

   // adjust subsystems

      reinitLogging(config.logLevel);

      reinitTimeout();

      global.orderManager.reinit(config.merchantConfig,config.downloadConfig,config.orderDataDir,getStylesheetFile(config),config.warnNotPrinted,config.carrierList.enableTracking);
      OrderUtil.setTimeZone(TimeZone.getTimeZone(config.printConfig.timeZone));
      OrderUtil.setPurgeConfig(config.purgeConfig);
      if (refreshTimeZone || refreshPurge) global.orderTable.refresh();

      global.jobManager.reinit(config.queueList);
      JobUtil.setPurgeConfig(config.purgeConfig);
      if (refreshPurge) global.jobTable.refresh();

      global.rollManager.reinit(config.merchantConfig,config.rollDataDir,config.purgeConfig.tryPurgeLocalImageURL);
      RollUtil.setPurgeConfig(config.purgeConfig);
      if (refreshPurge) global.rollTable.refresh();

      // pro mode doesn't change
      AbstractRollDialog.setConfig(config.proEmail,config.uploadConfig.transformConfig,config.claimEnabled,config.claimEmail,config.priceLists,config.dealers);
      Wholesale.setWholesale(config.merchantConfig.isWholesale); // (*) see note below
      ImagePoller.setDelayInterval(config.pollConfig.imageDelayInterval);
      OrderDialog.setCarrierList(config.carrierList);
      Print.setConfig(config.printConfig,config.productBarcodeConfig);
      User.setConfig(config.userConfig);
      StaticDLS.configure(config.dlsBusyPollInterval,config.dlsBusyRetries);

      reportSubsystem.reinit(rsc,frame);
      // restartSubsystem.reinit(config.autoUpdateConfig.enableRestart,rssc,frame);
      // can't do this in line, see synchronization analysis

      // note, the subsystems that have "init( ! ProMode.isPro(config.proMode) )"
      // don't need the same thing passed to reinit,
      // because reinit defaults to the current subsystem state.

      global.downloadSubsystem.reinit(config.downloadEnabled && ! ProMode.isPro(config.proMode),dsc,frame);
      global.uploadSubsystem.reinit(config.uploadEnabled,usc,frame);
      global.invoiceSubsystem.reinit(isc,frame);
      global.spawnSubsystem.reinit(ssc,frame);
      boolean rebuildFS = updateFormatSubsystems(config.queueList,config.productConfig);
      global.completionSubsystem.reinit(csc,frame);
      global.jobPurgeSubsystem.reinit(config.purgeConfig.autoPurgeJobs && ! ProMode.isPro(config.proMode),jpsc,frame);
      global.autoCompleteSubsystem.reinit(config.autoComplete && ! ProMode.isPro(config.proMode),acsc,frame);
      global.orderPurgeSubsystem.reinit(config.purgeConfig.autoPurgeOrders && ! ProMode.isPro(config.proMode),opsc,frame);

      LocalStatus.setConfig(config.localEnabled && ! ProMode.isPro(config.proMode),config.localConfig); // try to synchronize with subsystem config change
      global.localSubsystem.reinit(config.localEnabled && ! ProMode.isPro(config.proMode),lsc,frame);
      dueSubsystem.reinit(esc,frame);
      pakonSubsystem.reinit(config.pakonEnabled && ! ProMode.isPro(config.proMode),config.pakonPort);
      global.scanSubsystem.reinit(config.scanEnabled && ! ProMode.isPro(config.proMode),nsc,frame);
      global.pollSubsystem.reinit(config.pollEnabled && ! ProMode.isPro(config.proMode),psc,frame);
      global.localImageSubsystem.reinit(Nullable.nbToB(config.localShareEnabled) && ! ProMode.isPro(config.proMode),lisc,frame);
      global.rollPurgeSubsystem.reinit(autoPurgeRolls(config.purgeConfig),rpsc,frame);
      orderRetrySubsystem.reinit(orsc,frame);
      rollRetrySubsystem.reinit(rrsc,frame);

      reinitErrorWindow();
      if (rebuildFS) frame.rebuildFormatSubsystems(); // must come first so reinits hit the new subsystems
      boolean showScanThread = config.scanEnabled && ! ProMode.isPro(config.proMode);
      boolean showKioskThread = Nullable.nbToB(config.localShareEnabled) && ! ProMode.isPro(config.proMode);
      boolean showCpsCommand = config.queueList.enableBackup && ! ProMode.isPro(config.proMode);
      frame.reinit1(config.preventStopStart,showScanThread,showKioskThread,showCpsCommand,config.helpItems,config.showQueueTab,config.showGrandTotal,config.queueList);
      frame.reinit2(config.frameTitleSuffix,config.userConfig.renotifyInterval);
      reinitTimers();

      // the subsystems are responsible for doing nothing if nothing has changed
   }

   // (*) about isWholesale, mostly it's distributed through solid normal channels
   // as part of merchantConfig, but there are some cases where I cheated a little.
   // these are slightly less academic now that we have auto-config capability.
   // the question is, what happens when isWholesale is changed asynchronously by the UI thread?
   //
   // 1. in RollManager.adjustHold, we look at Wholesale.isWholesale to decide
   // whether to put rolls on dealer hold.  this function is called indirectly
   // in all kinds of threads, so basically the value is unpredictable.
   // however, it's not a big deal, for two reasons.  one is explained in the comments there ...
   // the value already changed asynchronously, so we didn't rely on the dealer hold.
   // the other is, we only use it once, so whatever value we see, we'll use it consistently
   //
   // 2. in RollDialog, we test Wholesale.isWholesale in several places.
   // that was fine before, because there was no way to reconfigure
   // while a roll dialog was up, but now there is, and if the values aren't
   // the same, we could easily access control fields that were never even
   // constructed.  so, I fixed the dialog to cache the value once.
   // the dialog may be inconsistent with the system state at save time,
   // but that's fine, the worst that can happen is a wrong dealer hold.
   //
   // 3. in OrderManager.updateStatusWithLock, we call StatusTransaction
   // using a static merchantConfig that we own.  that's fine for the three callers
   // that run in the UI thread, since they lock it up until they complete,
   // but the fourth caller, AutoCompleteThread, is really not nice at all.
   // we pass in a merchantConfig, so the value won't change while we're running,
   // but it bears no particular relationship to the value seen by the thread.
   // the only reason I'm not fixing it, besides that it's hard, is that the thread
   // doesn't actually look at isWholesale at all, so there's no inconsistency.
   // the other bad thing is, the merchantConfig and downloadConfig could come from
   // totally different configs, at least in theory.

}

