/*
 * Lookup.java
 */

package com.lifepics.neuron.lookup;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.install.Context;
import com.lifepics.neuron.install.InstallFile;
import com.lifepics.neuron.net.Handler;
import com.lifepics.neuron.net.PauseCallback;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Utility class that looks up the kiosk K-number.
 */

public class Lookup {

   private static void loadProperties(File file, Properties p) throws IOException {
      InputStream inputStream = new FileInputStream(file);
      try {
         p.load(inputStream);
      } finally {
         inputStream.close();
      }
   }

   private static String getPropertyOrElse(Properties p, String key) throws Exception {
      String s = p.getProperty(key);
      if (s == null) throw new Exception(Text.get(Lookup.class,"e1",new Object[] { key }));
      return s;
   }

   public static String readProperty(String[] args, Context c, File baseDir, File mainDir, Handler handler, PauseCallback callback) throws Exception {
      if (args.length != 2) throw new IllegalArgumentException();
      File file = InstallFile.getFile(args[0],baseDir,mainDir); // normally absolute path, but allow others
      Properties p = new Properties();
      loadProperties(file,p);
      return getPropertyOrElse(p,args[1]); // K-number plus country code
   }

   /**
    * Make this into a separate operation so we can remove it at the install script level if we need to.
    */
   public static String removeCountryCode(String[] args, Context c, File baseDir, File mainDir, Handler handler, PauseCallback callback) throws Exception {
      if (args.length != 1) throw new IllegalArgumentException();
      int i = args[0].lastIndexOf('-');
      if (i == -1) throw new Exception(Text.get(Lookup.class,"e2"));
      return args[0].substring(0,i);
   }

}

