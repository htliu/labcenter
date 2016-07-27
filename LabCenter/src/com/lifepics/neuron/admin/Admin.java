/*
 * Admin.java
 */

package com.lifepics.neuron.admin;

import com.lifepics.neuron.app.AboutDialog;
import com.lifepics.neuron.core.Log;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.gui.Style;
import com.lifepics.neuron.misc.AppUtil;
import com.lifepics.neuron.misc.ProcessException;
import com.lifepics.neuron.net.NetUtil;
import com.lifepics.neuron.table.AlternatingFile;

import java.io.File;
import java.util.logging.Level;

/**
 * The main class for the LabCenter Admin application.
 */

public class Admin extends AppUtil.Stub { // stub is overkill but it'll do

// --- main ---

   public static void main(String[] args) {
      new Admin().execute(args,/* launched = */ false); // ignore result
   }

// --- main functions ---

   protected String init(String causeKey, String[] args, boolean launched) throws ProcessException {

      // same order as in app.Main, just for consistency.

      AppUtil.initLookAndFeel();
      NetUtil.initNetwork();
      initLogging1();
      initMainDir(args);
      initConfig();
      Style.setStyle(Style.getDefaultStyle());
      initLoggingC(/* createMain = */ false);
      initLogging2(config.logCount,config.logSize,config.logLevel);
      NetUtil.setDefaultTimeout(config.defaultTimeoutInterval);

      Log.log(Level.INFO,this,causeKey,new Object[] { AboutDialog.getVersion() });

      initFrame();

      return null;
   }

   protected void run() {
      frame.setVisible(true);
   }

   protected void exit(String causeKey) {
      exitFrame();
      Log.log(Level.INFO,this,causeKey);
      exitLogging();
   }

// --- constants ---

   private static final String ADMIN_CONFIG_FILE = "admin-config.xml";

   private static final String NAME_ADMIN_CONFIG = "AdminConfig";

// --- config ---

   private AdminConfig config;

   private void initConfig() throws ProcessException {

      File configFile = new File(mainDir,ADMIN_CONFIG_FILE);
      try {
         config = (AdminConfig) AlternatingFile.load(configFile,new AdminConfig(),NAME_ADMIN_CONFIG);
      } catch (Exception e) {
         throw new ProcessException(Text.get(this,"e2"),e);
      }
   }

// --- frame ---

   private AdminFrame frame;

   private void initFrame() {
      frame = new AdminFrame(this,mainDir,config);
   }

   private void exitFrame() {
      if (frame != null) frame.dispose();
   }

}

