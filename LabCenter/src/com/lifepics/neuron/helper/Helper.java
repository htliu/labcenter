/*
 * Helper.java
 */

package com.lifepics.neuron.helper;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.install.Context;
import com.lifepics.neuron.install.InstallFile;
import com.lifepics.neuron.misc.StreamLogger;
import com.lifepics.neuron.net.Handler;
import com.lifepics.neuron.net.PauseCallback;
import com.lifepics.neuron.object.XML;

import java.io.File;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.LinkedList;

import org.w3c.dom.Document;

/**
 * Utility class that provides common install helper functions.
 */

public class Helper {

   /**
    * Read install-directory entries from a config file,
    * and create them.  The point of using this instead
    * of MKDIR is that the set of directories can depend
    * on the configuration.
    */
   public static String makeInstallDirs(String[] args, Context c, File baseDir, File mainDir, Handler handler, PauseCallback callback) throws Exception {
      if (args.length != 1) throw new IllegalArgumentException();

      File configFile = InstallFile.getFile(args[0],baseDir,mainDir);
      Document doc = XML.readFile(configFile);
      LinkedList dirs = XML.getStringList(doc,"Config","InstallDir");

      // like the MKDIR command, except can vary with the config

      Iterator i = dirs.iterator();
      while (i.hasNext()) {
         File dest = InstallFile.getFile((String) i.next(),baseDir,mainDir);

         if (dest.exists()) {
            if ( ! dest.isDirectory() ) throw new Exception(Text.get(Helper.class,"e1",new Object[] { dest }));
         } else {
            if ( ! dest.mkdirs     () ) throw new Exception(Text.get(Helper.class,"e2",new Object[] { dest }));
         }
      }

      return null;
   }

   public static String execWithoutDir(String[] args, Context c, File baseDir, File mainDir, Handler handler, PauseCallback callback) throws Exception {
      return exec(args,null);
   }
   public static String execWithDir   (String[] args, Context c, File baseDir, File mainDir, Handler handler, PauseCallback callback) throws Exception {
      if (args.length < 1) throw new IllegalArgumentException();

      File dir = InstallFile.getFile(args[0],baseDir,mainDir);

      String[] rest = new String[args.length-1];
      for (int i=0; i<rest.length; i++) {
         rest[i] = args[i+1];
      }
      // cf. start of Launcher

      return exec(rest,dir);
   }

   /**
    * Execute an arbitrary process.  The first argument has to be executable,
    * so it can't be a DOS command, but it *can* be the name of a batch file.
    */
   private static String exec(String[] args, File dir) throws Exception {
      Process p = Runtime.getRuntime().exec(args,null,dir);

      new StreamLogger(Helper.class,"i1",p.getInputStream()).start();
      new StreamLogger(Helper.class,"i2",p.getErrorStream()).start();
      // if there's output, waitFor won't return until it's read

      int result = p.waitFor();
      if (result != 0) throw new Exception(Text.get(Helper.class,"e3",new Object[] { args[0], Convert.fromInt(result) }));
      return null;
   }

   public static String set(String[] args, Context c, File baseDir, File mainDir, Handler handler, PauseCallback callback) throws Exception {
      if (args.length != 1) throw new IllegalArgumentException();
      return args[0];
      // the point of this is, the input argument gets evaluated
   }

   public static String exitIfExists(String[] args, Context c, File baseDir, File mainDir, Handler handler, PauseCallback callback) throws Exception {
      if (args.length != 1) throw new IllegalArgumentException();
      File file = InstallFile.getFile(args[0],baseDir,mainDir);
      if (file.exists()) System.exit(0); // only place outside AppUtil where we do this
         // app exit leaves this item marked incomplete, so it will run again next time
      return null;
   }

   public static String toUpper(String[] args, Context c, File baseDir, File mainDir, Handler handler, PauseCallback callback) throws Exception {
      if (args.length != 1) throw new IllegalArgumentException();
      return args[0].toUpperCase();
   }

   public static String toLower(String[] args, Context c, File baseDir, File mainDir, Handler handler, PauseCallback callback) throws Exception {
      if (args.length != 1) throw new IllegalArgumentException();
      return args[0].toLowerCase();
   }

   public static String getHostName(String[] args, Context c, File baseDir, File mainDir, Handler handler, PauseCallback callback) throws Exception {
      if (args.length > 0) throw new IllegalArgumentException();
      return InetAddress.getLocalHost().getHostName();
   }

   public static String getHostAddress(String[] args, Context c, File baseDir, File mainDir, Handler handler, PauseCallback callback) throws Exception {
      if (args.length > 0) throw new IllegalArgumentException();
      return InetAddress.getLocalHost().getHostAddress();
   }

   /**
    * Transform an IP address in a slightly flexible way.
    */
   public static String transform(String[] args, Context c, File baseDir, File mainDir, Handler handler, PauseCallback callback) throws Exception {
      if (args.length != 4) throw new IllegalArgumentException();

      int[] ip = toIP(args[0]);           // toIP parsing is strict enough
      String op = args[1];                // enumeration checked below
      int index = Convert.toInt(args[2]); // if out of range, we'll get an out of bounds exception
      int value = Convert.toInt(args[3]); // if out of range, no problem, it'll get masked anyway

      if      (op.equals("SET")) ip[index]  = value;
      else if (op.equals("ADD")) ip[index] += value;
      else if (op.equals("AND")) ip[index] &= value;
      else if (op.equals("OR" )) ip[index] |= value;
      else if (op.equals("XOR")) ip[index] %= value;
      else throw new Exception(Text.get(Helper.class,"e6",new Object[] { op }));

      return fromIP(ip);
   }

   /**
    * Strict parser that makes sure everything is just right.
    *
    * Why write my own?  Comment copied from SetupFrame.isValidAddress.
    * There are too many uncertainties involved with java.net.InetAddress,
    * for example it will let through numbers without any dots at all.
    * So, let's just validate for the simple format that we expect to see.
    */
   private static int[] toIP(String s) throws ValidationException {

      String[] t = s.split("\\.",-1); // -1 to stop weird default behavior
      if (t.length != 4) throw new ValidationException(Text.get(Helper.class,"e4",new Object[] { s }));

      int[] ip = new int[t.length];
      for (int i=0; i<t.length; i++) {
         ip[i] = Convert.toInt(t[i]);
         if (ip[i] < 0 || ip[i] > 255) throw new ValidationException(Text.get(Helper.class,"e5",new Object[] { s }));
      }

      return ip;
   }

   /**
    * Lax formatter that allows any array length and masks down to bytes.
    */
   private static String fromIP(int[] ip) {

      StringBuffer b = new StringBuffer();
      for (int i=0; i<ip.length; i++) {
         if (i != 0) b.append('.');
         b.append(Convert.fromInt(ip[i] & 0xFF));
      }

      return b.toString();
   }

}

