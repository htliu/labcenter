/*
 * Setup.java
 */

package com.lifepics.neuron.setup;

import com.lifepics.neuron.core.Log;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.gui.Style;
import com.lifepics.neuron.gui.User;
import com.lifepics.neuron.install.InstallConfig;
import com.lifepics.neuron.install.InstallFile;
import com.lifepics.neuron.misc.AppUtil;
import com.lifepics.neuron.misc.ProcessException;
import com.lifepics.neuron.object.XML;
import com.lifepics.neuron.table.AlternatingFile;

import java.io.File;
import java.util.LinkedList;
import java.util.logging.Level;

/**
 * The main class for the setup application.
 */

public class Setup extends AppUtil.Stub {

// --- main ---

   public static void main(String[] args) {
      main(args,/* launched = */ false); // ignore result
   }

   public static String main(String[] args, boolean launched) {
      return new Setup().execute(args,launched);
   }

// --- main functions ---

   protected String init(String causeKey, String[] args, boolean launched) throws ProcessException {

      // same order as in app.Main, just for consistency.
      // in general you want the NetUtil ones too,
      // but this particular app doesn't do any networking.

      AppUtil.initLookAndFeel();
      // NetUtil.initNetwork();
      initLogging1();
      initMainDir(args);
      Style.setStyle(Style.getDefaultStyle());
      initLogging2();
      // NetUtil.setDefaultTimeout(n);

      Log.log(Level.INFO,this,causeKey);

      initConfig();
      initStoreList();

      initFrame();

      return null;
   }

   protected void run() {
      frame.setVisible(true);
      if (minimizeCommand) {
         User.iconify(frame); // see comments in app/Main.java
      }
      // minimize doesn't make sense because we're interactive ...
      // so don't use the command line switch!
   }

   protected void exit(String causeKey) {
      exitFrame();
      Log.log(Level.INFO,this,causeKey);
      exitLogging();
   }

// --- constants ---

   private static final String NAME_STORES = "StoreList";

// --- config ---

   private File storeFile;
   private LinkedList templates;
   private String mainApp;

   private void initConfig() throws ProcessException {
      try {

         InstallConfig config = (InstallConfig) AlternatingFile.load(new File(baseDir,INSTALL_FILE),new InstallConfig(),NAME_INSTALL);
         // we use AlternatingFile to write into the install file
         // in the installer app, so we should use it to read too.

         storeFile = config.getFile (InstallFile.TYPE_SETUP_STORE_LIST).getDestFile(baseDir,mainDir);
         templates = config.getFiles(InstallFile.TYPE_SETUP_TEMPLATE,/* allowZero = */ false);
         mainApp   = config.getFile (InstallFile.TYPE_MAIN_APP).getAppName();
         // don't even store the config object itself, this is all the information that we need

      } catch (Exception e) {
         throw new ProcessException(Text.get(this,"e3"),e);
      }
   }

// --- store list ---

   private StoreList storeList;

   private void initStoreList() throws ProcessException {
      try {
         storeList = (StoreList) XML.loadFile(storeFile,new StoreList(),NAME_STORES);
      } catch (Exception e) {
         throw new ProcessException(Text.get(this,"e2"),e);
      }
   }

// --- frame ---

   private SetupFrame frame;

   private void initFrame() {
      frame = new SetupFrame(this,baseDir,mainDir,templates,mainApp,storeList,new File(mainDir,CONFIG_FILE));
   }

   private void exitFrame() {
      if (frame != null) frame.dispose();
   }

}

