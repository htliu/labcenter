/*
 * Launcher.java
 */

package com.lifepics.neuron.launch;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

/**
 * A small class that launches an application inside a URLClassLoader,
 * so that a new version can be swapped in without restarting the JVM.
 */

public class Launcher {

// --- main ---

   public static void main(String[] args) throws Exception {

   // split out first argument

      if (args.length < 1) throw new IllegalArgumentException();

      String indirectionName = args[0];

      String[] rest = new String[args.length-1];
      for (int i=0; i<rest.length; i++) {
         rest[i] = args[i+1];
      }

      args = rest; // avoid confusion later

      File baseDir = Parse.getBaseDir(args);
      File indirectionFile = new File(baseDir,indirectionName);

   // loop and run

      String jarName = read(indirectionFile);

      while (true) {
         String result = launch(new File(baseDir,jarName),args);
         if (result == null) break;

         jarName = result;
         write(indirectionFile,jarName);
      }

      // here's the original idea:
      //
      // if the result is a string, that means a re-launch;
      // if it's null, that means the launch was successful,
      // and the invoking thread can go ahead and exit.
      //
      // nowadays we don't return null for normal operation any more,
      // since we use daily auto-updates and might need to restart.
      // we could return null at exit, but then who would call System.exit?
      //
      // I thought there was no way to shut down the event dispatcher,
      // so that System.exit was required, but it turns out that was
      // just bugs ... JDK 1.4.1 will do it pretty reliably, and I'm sure
      // the client ones are better.  however, I don't trust it,
      // so I'm going to continue using System.exit for right now.
      //
      // I already found one case where the event dispatcher doesn't exit,
      // which is when you've created a JOptionPane with no parent.
      // that creates a default parent frame that's never disposed of.
   }

// --- launch function ---

   private static String launch(File jarFile, String[] args) throws Exception {

   // get main class name from JAR file

      String mainClass = new JarFile(jarFile).getManifest()
                                             .getMainAttributes()
                                             .getValue(Attributes.Name.MAIN_CLASS);
   // load main class

      URLClassLoader loader = new URLClassLoader(new URL[] { jarFile.toURL() });
      Class c = loader.loadClass(mainClass);

   // call main function

      // the method has a different signature than the regular main function,
      // so that the object can tell whether it was launched from a launcher,
      // and hence whether a re-launch attempt will be productive

      Method m = c.getMethod("main",new Class[] { args.getClass(), Boolean.TYPE });
      if (m.getReturnType() != String.class) throw new IllegalArgumentException();

      return (String) m.invoke(null,new Object[] { args, Boolean.TRUE });
   }

// --- I/O helpers ---

   private static String read(File file) throws IOException {
      FileReader reader = new FileReader(file);
      try {
         return readAll(reader,200); // arbitrary limit
      } finally {
         reader.close();
      }
   }

   private static void write(File file, String s) throws IOException {
      FileWriter writer = new FileWriter(file);
      try {
         writer.write(s);
      } finally {
         writer.close();
      }
   }

   private static String readAll(FileReader reader, int max) throws IOException {

      char[] c = new char[max];
      int off = 0;
      int len = max;

      // read until we reach EOF, which is good,
      // or the buffer is full, which is an error

      while (true) {
         int count = reader.read(c,off,len);
         if (count == -1) break;
         off += count;
         len -= count;
         if (len == 0) throw new IOException();
      }

      return new String(c,0,off);
   }

}

