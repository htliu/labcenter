/*
 * UploadLCVersion.java
 */

package com.lifepics.neuron.admin;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.misc.FileUtil;
import com.lifepics.neuron.net.NetUtil;
import com.lifepics.neuron.net.RawUploadTransaction;
import com.lifepics.neuron.net.Query;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * The transaction for uploading LabCenter jar files to the server.
 * This is a command-line utility, not a function that's available
 * within LC Admin, but it's an admin-like function that clearly
 * doesn't belong anywhere else in the code tree, let's put it here.
 *
 * No need for a reverse transaction to verify the upload, just use
 * the direct URL to the file system.
 * https://services.lifepics.com/LabCenterUpdates/
 */

public class UploadLCVersion extends RawUploadTransaction {

// --- fields ---

   private int versionID;
   private String auth;
   private File file;
   private boolean overwrite;

   public UploadLCVersion(int versionID, String auth, File file, boolean overwrite) {
      this.versionID = versionID;
      this.auth = auth;
      this.file = file;
      this.overwrite = overwrite;
   }

// --- transaction stuff ---

   // cf. UploadThread.UploadLocal transaction

   private static final String url = "http://api.lifepics.com/closed/UploadLCVersion.aspx";
   // a non-configured URL, should be rare

   public String describe() { return Text.get(UploadLCVersion.class,"s1"); }
   protected String getFixedURL() { return url; }
   protected void getParameters(Query query) throws IOException {

      query.add("versionID",Convert.fromInt(versionID));
      query.addPasswordCleartext("auth",auth); // do not build this in, it's secret
      query.add("filename",file.getName());
      if (overwrite) query.add("overwrite","1"); // optional param
   }

   protected File getFile() { return file; }

   protected boolean receive(InputStream inputStream) throws Exception {
      FileUtil.copy(System.out,inputStream); // don't bother wiring to UploadThread parse code
      return true;
   }

// --- main ---

   public static void main(String[] args) throws Exception {

      if (args.length < 3) {
         System.out.println(Text.get(UploadLCVersion.class,"s2"));
         return;
      }

      int versionID = Convert.toInt(args[0]);
      String auth = args[2];
      File file = new File("..",args[1]); // go up one level because we run in lib dir
      boolean overwrite = (args.length > 3);

      NetUtil.initNetwork(); // no effect since URL isn't HTTPS, but let's do it anyway
      // logging not set up

      new UploadLCVersion(versionID,auth,file,overwrite).runInline();
   }

}

