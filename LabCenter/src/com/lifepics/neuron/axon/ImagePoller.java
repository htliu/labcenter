/*
 * ImagePoller.java
 */

package com.lifepics.neuron.axon;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.misc.ExtensionFileFilter;
import com.lifepics.neuron.misc.FileUtil;

import java.io.File;
import java.io.FileFilter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A poller that accepts single images and organizes them into folders.
 */

public class ImagePoller implements Poller {

   // some of this is similar to RenamePoller, but the whole getCore/getName system
   // would make us recalculate filename stuff way too often

// --- constants ---

   private static final String DIR_TEMP = "tmp";
   private static final String DIR_DONE = "uploaded";

   private static final char CHAR_DASH = '-';

   public static final long DEFAULT_DELAY_INTERVAL = 300000; // five minutes

   private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

// --- static config ---

   // the default will always be overwritten, but it's nice to start with a valid number
   private static long delayInterval = DEFAULT_DELAY_INTERVAL;
   public static void setDelayInterval(long delayInterval) { ImagePoller.delayInterval = delayInterval; }

// --- FileFilter subclass ---

   // use a class here so that we don't accept some files in a cluster and
   // not others because the system clock changed partway through the scan

   private static class TimeFileFilter implements FileFilter {

      private long time;
      public TimeFileFilter(long time) { this.time = time; }

      public boolean accept(File file) {

         if (file.isDirectory()) return false;
         if (file.lastModified() > time) return false;

         String name = file.getName();
         if ( ! ExtensionFileFilter.hasExtension(name,ItemUtil.extensions) ) return false;

         int i = name.lastIndexOf(CHAR_DASH); // note that none of the extensions has one
         if (i == -1) return false;

         return true;
      }
   }

// --- utility functions ---

   protected static boolean endsWithIgnoreCase(String s, String suffix) {
      int index = s.length() - suffix.length();
      if (index < 0) return false;
      return (s.substring(index).equalsIgnoreCase(suffix));
   }

// --- implementation of Poller ---

   public FileFilter getFileFilter() { return new TimeFileFilter(System.currentTimeMillis() - delayInterval); }

   public void process(File file, PollCallback callback) throws Exception {

   // derive some stuff from the filename

      String name = file.getName();

      int i1 = name.lastIndexOf(CHAR_DASH); // not -1
      String code1 = name.substring(0,i1); // barcode

      int i2 = name.indexOf(CHAR_DASH); // also not -1
      String code2 = name.substring(0,i2); // first barcode

   // first renaming

      File dirT = new File(file.getParentFile(),DIR_TEMP);
      if ( ! dirT.exists() && ! dirT.mkdir() ) throw new Exception(Text.get(ImagePoller.class,"e5"));

      File temp = new File(dirT,name);
      if ( ! file.renameTo(temp) ) throw new Exception(Text.get(ImagePoller.class,"e1"));

   // actual processing

      Roll.validateAlbum(code1); // should be fine, it's from a file name, but if not, we want to error out

      Roll roll = new Roll();

      roll.source = Roll.SOURCE_HOT_IMAGE;
      roll.email = "";
      roll.album = code1;
      roll.claimNumber = code1; // apparently not used, but keep in case we fix that in the future
      roll.jobNumber = code2;

      File[] files = new File[] { temp };

      callback.submit(roll,files);

   // second renaming

      File dir1 = new File(file.getParentFile(),DIR_DONE);
      if ( ! dir1.exists() && ! dir1.mkdir() ) throw new Exception(Text.get(ImagePoller.class,"e2"));

      String dateString = dateFormat.format(new Date()); // too bad there's no scanDate
      File dir2 = new File(dir1,dateString);
      if ( ! dir2.exists() && ! dir2.mkdir() ) throw new Exception(Text.get(ImagePoller.class,"e6",new Object[] { dateString }));

      File dir3 = new File(dir2,code1);
      if ( ! dir3.exists() && ! dir3.mkdir() ) throw new Exception(Text.get(ImagePoller.class,"e3",new Object[] { code1 }));

      File done = new File(dir3,FileUtil.disambiguate(dir3,name));
      if ( ! temp.renameTo(done) ) throw new Exception(Text.get(ImagePoller.class,"e4"));
   }

}

