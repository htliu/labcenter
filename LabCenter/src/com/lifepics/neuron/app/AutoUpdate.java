/*
 * AutoUpdate.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Log;
import com.lifepics.neuron.core.Nullable;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.misc.FileUtil;
import com.lifepics.neuron.net.DownloadTransaction;
import com.lifepics.neuron.net.GetTransaction;
import com.lifepics.neuron.net.Handler;
import com.lifepics.neuron.net.MerchantConfig;
import com.lifepics.neuron.net.PauseCallback;
import com.lifepics.neuron.net.Query;
import com.lifepics.neuron.net.ServerException;
import com.lifepics.neuron.object.Obfuscate;
import com.lifepics.neuron.object.XML;
import com.lifepics.neuron.table.AlternatingFile;
import com.lifepics.neuron.thread.StopDialog;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * A utility class that handles auto-update behavior.
 */

public class AutoUpdate {

// --- software constants ---

   public static final String SOFTWARE_LABCENTER = "LabCenter";
   public static final String SOFTWARE_CONFIG = "config.xml";
   public static final String SOFTWARE_INVOICE = "invoice.xsl";
   public static final String SOFTWARE_BURNER = "CdBurner.zip";

   // the config and invoice ones happen to match the file names,
   // but they're really different constants with different uses

// --- main functions ---

   /**
    * Check for a new version and preload it so that it'll be there at restart.
    * @return The new version, or null if there was no preload.
    *         (The config object doesn't change in any case, unlike in update.)
    */
   public static String preload(File baseDir, AutoUpdateConfig config, MerchantConfig merchantConfig,
                                InstanceRecord ir, LinkedList softwareList,
                                Handler handler, PauseCallback callback) throws Exception {

      // note, here we want to throw exceptions rather than log,
      // because the caller might choose to show them in the UI.

      SoftwareRecord desired;
      try {
         GetSoftwareList t = new GetSoftwareList(config,merchantConfig,ir,softwareList);
         if ( ! handler.run(t,callback) ) return null;
         desired = findByNameRequired(softwareList,SOFTWARE_LABCENTER);
      } catch (Exception e) {
         throw new Exception(Text.get(AutoUpdate.class,"e7"),e);
      }

      if (desired.version.equals(config.lc.currentVersion)) return null;

      File file = new File(baseDir,desired.version);
      if ( ! match(file,desired) ) {

         try {
            DownloadVersion t = new DownloadVersion(file,desired);
            if ( ! handler.run(t,callback) ) return null;
         } catch (Exception e) {
            file.delete(); // ignore result
            throw new Exception(Text.get(AutoUpdate.class,"e8"),e);
         }
      }

      // here we don't shift versions in the config file, or do any of the
      // other things that the startup-time auto-update does.  instead,
      // we just preload the new jar file and exit, and let the normal auto-update happen.
      // that requires one more server query, but keeps the code simple.

      return desired.version;
   }

   /**
    * Update to a new version, if possible.
    * @return The new version, or null if there was no update.
    *         (If there is a new version, the config object will have changed.)
    */
   public static String update(File baseDir, AutoUpdateConfig config, MerchantConfig merchantConfig,
                               InstanceRecord ir, LinkedList softwareList,
                               Handler handler, PauseCallback callback) {

   // determine desired state

      SoftwareRecord desired;
      try {
         GetSoftwareList t = new GetSoftwareList(config,merchantConfig,ir,softwareList);
         if ( ! handler.run(t,callback) ) throw new Exception(Text.get(AutoUpdate.class,"e11"));
         desired = findByNameRequired(softwareList,SOFTWARE_LABCENTER);
      } catch (Exception e) {
         Log.log(Level.SEVERE,AutoUpdate.class,"e3",e);
         return null;
      }

   // see if there's a new version

      if (desired.version.equals(config.lc.currentVersion)) return null;

   // download file if necessary

      File file = new File(baseDir,desired.version);
      if ( ! match(file,desired) ) {
         //
         // if the file comes through at the wrong size, we keep it for viewing.
         // so, we do need to check the size.
         //
         // normally I'd use FileUtil.getSize, but it's a pain that it throws an
         // exception.  and, if you look at the code, it's just file.length with
         // an exception in the case where the file doesn't exist.

         try {

            // if we ever set up a more general auto-update scheme,
            // the thread should move out to encompass everything that
            // could take a long time

            DownloadThread downloadThread = new DownloadThread(file,desired,handler,callback);
            downloadThread.start();
            StopDialog.joinSafe(downloadThread,null);
            downloadThread.rethrow();

         } catch (Exception e) {
            file.delete(); // ignore result
            Log.log(Level.SEVERE,AutoUpdate.class,"e4",e);
            return null;
         }
      }

   // shift versions in config file

      advance(config.lc,desired.version);

   // done

      return desired.version;
   }

   /**
    * Update a software component to a new version, if possible.
    */
   public static void updateSoftware(File useDir, AutoUpdateConfig.VersionInfo info, String software,
                                     LinkedList softwareList,
                                     Handler handler, PauseCallback callback) {

      SoftwareRecord desired = findByName(softwareList,software);

      // handle the null case separately, simplest this way
      if (desired == null) {
         if (info.currentVersion != null) advance(info,null);
         return;
      }

      if (desired.version.equals(info.currentVersion)) return; // works even if currentVersion is null

      File file = new File(useDir,desired.version);
      if ( ! match(file,desired) ) {

         // this should be pretty fast, no need for thread and dialog.
         // also it would need to be switchable, since this is called
         // from both Main and RestartThread.

         try {
            DownloadVersion t = new DownloadVersion(file,desired);
            if ( ! handler.run(t,callback) ) return;
            // preload and update differ here, but I'll follow the idea
            // that thread stop shouldn't throw an exception
         } catch (Exception e) {
            file.delete(); // ignore result
            Log.log(Level.SEVERE,AutoUpdate.class,"e13",new Object[] { software },e);
            return;
         }
      }

      advance(info,desired.version);
   }

   private static boolean match(File file, SoftwareRecord desired) {
      return (file.exists() && file.length() == desired.size && match(file,desired.checksum));
   }

   private static boolean match(File file, Integer checksum) {
      if (checksum == null) return true; // no checksum requirement on file
      try {
         return (DownloadTransaction.getChecksum(file) == checksum.intValue());
      } catch (IOException e) {
         return false; // treat indeterminate checksum same as wrong checksum.
         // it could also be an error condition, but that's hard to deal with
         // in the update function.
      }
      // the checksum test is usually not needed because of the delete clause
      // in the try-catch blocks above, but in some cases we could be left
      // with a right-size wrong-checksum file on disk .. e.g., a thread stop
      // during preload, or a process killed at exactly the wrong moment.
   }

   /**
    * Advance to a new version.
    * @param newVersion The new version, not null for LC but can be in other cases.
    *                   Caller is responsible for checking that there was a change!
    */
   public static void advance(AutoUpdateConfig.VersionInfo info, String newVersion) {

      // for the LabCenter jar file, keep one backup version because
      // (a) we couldn't delete it if we wanted, the JVM is using it
      // (b) it makes rolling back an update a lot quicker
      //
      // for invoice.xsl, keep one backup version for no real reason.
      // there's a small chance the file might be open, but we could
      // move it to the stale list right away if we wanted

      // keep one real backup version, don't push nulls down the chain
      //
      if (info.currentVersion != null) {
         if (info.backupVersion != null) {
            info.staleVersions.add(info.backupVersion);
         }
         info.backupVersion = info.currentVersion;
      }
      info.currentVersion = newVersion;

      // make sure the current and backup versions aren't marked as stale
      // normally this would only happen if there's a rollback
      if (info.currentVersion != null) {
         while (info.staleVersions.remove(info.currentVersion)) ;
      }
      if (info.backupVersion != null) {
         while (info.staleVersions.remove(info.backupVersion)) ;
      }
   }

   /**
    * Clean up stale versions.
    * @return True if the config object was changed.
    */
   public static boolean clean(File useDir, AutoUpdateConfig.VersionInfo info) {
      boolean removed = false;

      ListIterator li = info.staleVersions.listIterator();
      while (li.hasNext()) {
         String version = (String) li.next();
         File file = new File(useDir,version);

         if ( (! file.exists()) || file.delete() ) {
            li.remove();
            removed = true;
         }
         // else leave the file in the list for later,
         // maybe it is in use by the current JVM
      }

      return removed;
   }

   /**
    * Extract a single specific file from a jar file.
    *
    * @param jarFile The jar file.
    * @param entry   The entry name, which should not contain any path prefix.
    *                The entry name is also used as the target file name.
    * @param targetDir  The directory into which the given file should be extracted.
    * @param overwrite  A flag for whether to try to overwrite existing files.
    *
    * @return True if the file was successfully extracted.
    */
   public static boolean extract(File jarFile, String entry, File targetDir, boolean overwrite) {
      try {

         File target = new File(targetDir,entry);
         if ( ! overwrite && target.exists() ) return false;

         ZipFile zf = new ZipFile(jarFile);
         try {
            extractEntry(zf,getEntry(zf,entry),target);
         } finally {
            zf.close();
         }

         return true;

      } catch (Exception e) {
         Log.log(Level.WARNING,AutoUpdate.class,"e5",new Object[] { entry },e);
         return false;
      }
   }

   private static ZipEntry getEntry(ZipFile zf, String entry) throws Exception {

      ZipEntry ze = zf.getEntry(entry);
      if (ze == null) throw new Exception(Text.get(AutoUpdate.class,"e6",new Object[] { entry }));

      return ze;
   }

   private static void extractEntry(ZipFile zf, ZipEntry ze, File target) throws Exception {

      // use an alternating file so that there will never be a partial file,
      // at worst you'll just have an older version of whatever the file is.

      AlternatingFile af = new AlternatingFile(target);
      try {
         FileUtil.copy(af.beginWrite(),zf.getInputStream(ze));
         af.commitWrite();
      } finally {
         af.endWrite();
      }

      long t = ze.getTime();
      if (t != -1) target.setLastModified(t); // not critical, ignore result
   }

   /**
    * Extract a set of libraries unless they all exist and have the proper size.
    */
   public static void extractSet(File jarFile, String[] entries, File targetDir) {
      try {
         ZipFile zf = new ZipFile(jarFile);
         try {

         // set up

            // even if everything is already extracted, we still have to check
            // whether the files exist and whether the sizes match, so we need
            // all these objects.

            File[] target = new File[entries.length];
            ZipEntry[] ze = new ZipEntry[entries.length];

            for (int i=0; i<entries.length; i++) {
               target[i] = new File(targetDir,entries[i]);
               ze[i] = getEntry(zf,entries[i]);
            }

         // check

            boolean extract = false;

            for (int i=0; i<entries.length; i++) {
               if (target[i].exists() && target[i].length() == ze[i].getSize()) {
                  // fine
               } else {
                  extract = true;
                  break;
               }
            }

            if ( ! extract ) return;

         // extract

            for (int i=0; i<entries.length; i++) {
               extractEntry(zf,ze[i],target[i]);
            }
            // why is this not an atomic operation?!

         } finally {
            zf.close();
         }
      } catch (Exception e) {
         Log.log(Level.WARNING,AutoUpdate.class,"e20",e);
      }
   }

// --- download thread ---

   private static class DownloadThread extends Thread {

      private File dest;
      private SoftwareRecord desired;
      private Handler handler;
      private PauseCallback callback;
      private Exception exception;

      public DownloadThread(File dest, SoftwareRecord desired, Handler handler, PauseCallback callback) {
         super(Text.get(AutoUpdate.class,"s1"));
         this.dest = dest;
         this.desired = desired;
         this.handler = handler;
         this.callback = callback;
         // exception starts out null
      }

      public void run() {
         try {
            DownloadVersion t = new DownloadVersion(dest,desired);
            if ( ! handler.run(t,callback) ) throw new Exception(Text.get(AutoUpdate.class,"e12"));
         } catch (Exception e) {
            exception = e;
         }
      }

      /**
       * Transfer exception back to main thread.
       */
      public void rethrow() throws Exception {
         if (exception != null) throw exception;
      }
   }

// --- instance record ---

   // this is really more of an AutoConfig thing, but it's convenient to declare it here

   public static class InstanceRecord {
      public Integer instanceID;
      public Integer passcode;
   }

// --- software record ---

   public static class SoftwareRecord {
      public String name;
      public String version;
      public String downloadURL;
      public long size;
      public Integer checksum;
   }

   public static SoftwareRecord findByName(LinkedList softwareList, String name) {
      Iterator i = softwareList.iterator();
      while (i.hasNext()) {
         SoftwareRecord sr = (SoftwareRecord) i.next();
         if (sr.name.equals(name)) return sr;
      }
      return null;
   }

   public static SoftwareRecord findByNameRequired(LinkedList softwareList, String name) throws Exception {
      SoftwareRecord sr = findByName(softwareList,name);
      if (sr == null) throw new Exception(Text.get(AutoUpdate.class,"e2",new Object[] { name }));
      return sr;
   }

// --- analyze function ---

   public static class AnalysisRecord {
      public String message;

      public String curLabCenter;
      public String curConfig;
      public String curInvoice;
      public String curBurner;

      public String newLabCenter;
      public String newConfig;
      public String newInvoice;
      public String newBurner;

      public boolean showBurner;
   }

   public static AnalysisRecord analyze(Global.Control control) {
      AnalysisRecord ar = new AnalysisRecord();

      // the idea is, unknown means something is wrong somewhere

      String unknown = Text.get(AutoUpdate.class,"s4");
      String none = Text.get(AutoUpdate.class,"s5");
      String notApplicable = Text.get(AutoUpdate.class,"s6");

   // read from config file

      Config config = control.getConfig();
      ar.curLabCenter = config.autoUpdateConfig.lc.currentVersion;
      ar.curInvoice = config.autoUpdateConfig.invoice.currentVersion;
      if (ar.curInvoice == null) ar.curInvoice = none;
      ar.curBurner = config.autoUpdateConfig.burner.currentVersion;
      if (ar.curBurner != null) ar.showBurner = true;
      if (ar.curBurner == null) ar.curBurner = none; // order matters

      // the most common values
      ar.curConfig = unknown;
      ar.newLabCenter = unknown;
      ar.newConfig = unknown;
      ar.newInvoice = unknown;
      ar.newBurner = unknown;

   // read from server file

      InstanceRecord ir;
      {
         AutoUpdateConfig auc;
         try {
            auc = control.getServerAUC();
         } catch (Exception e) { // corrupt server file
            ar.message = Text.get(AutoUpdate.class,"e14");
            return ar;
         }

         if (auc == null) { // no server file
            ar.message = Text.get(AutoUpdate.class,"e15");
            ar.curConfig = notApplicable;
            ar.newLabCenter = notApplicable;
            ar.newConfig = notApplicable;
            ar.newInvoice = notApplicable;
            ar.newBurner = notApplicable;
            return ar;
         }

         ir = new InstanceRecord();
         ir.instanceID = auc.instanceID;
         ir.passcode   = auc.passcode;

         ar.curConfig = Convert.fromNullableInt(auc.revisionNumber); // only formally nullable
      }
      // done with auc, use a block to keep it confined here

   // talk to the server

      // we could do this without an instance ID, but then we'd keep generating more IDs
      // in the server table.  so, let's not allow that -- no big deal, it's a rare case.

      LinkedList softwareList = new LinkedList();
      try {
         new GetSoftwareList(config.autoUpdateConfig,config.merchantConfig,ir,softwareList).runInline();
      } catch (Exception e) {
         ar.message = Text.get(AutoUpdate.class,"e16");
         return ar;
      }

   // finish up

      int err = 0;
      int dif = 0;

      SoftwareRecord sr = findByName(softwareList,SOFTWARE_LABCENTER);
      if (sr == null) {
         err++; // leave unknown
      } else {
         ar.newLabCenter = sr.version;
         if ( ! ar.newLabCenter.equals(ar.curLabCenter) ) dif++;
      }

      sr = findByName(softwareList,SOFTWARE_CONFIG);
      if (sr == null || sr.version.equals("-1")) { // -1 is equivalent to null
         err++; // leave unknown
         // this is always an error here because we know we have a server file
      } else {
         ar.newConfig = sr.version;
         if ( ! ar.newConfig.equals(ar.curConfig) ) dif++;
      }

      sr = findByName(softwareList,SOFTWARE_INVOICE);
      ar.newInvoice = (sr == null) ? none : sr.version; // missing is allowed here
      if ( ! ar.newInvoice.equals(ar.curInvoice) ) dif++;
      // a bit ugly to be string-comparing none to invoice file names, but oh well

      sr = findByName(softwareList,SOFTWARE_BURNER);
      if (sr != null) ar.showBurner = true;
      ar.newBurner = (sr == null) ? none : sr.version; // missing is allowed here
      if ( ! ar.newBurner.equals(ar.curBurner) ) dif++;
      // no way to get dif++ without also getting showBurner = true

      if      (err > 0) ar.message = Text.get(AutoUpdate.class,"e17");
      else if (dif > 0) ar.message = Text.get(AutoUpdate.class,"e18");
      else              ar.message = Text.get(AutoUpdate.class,"e19");
      return ar;
   }

// --- HTTP transactions ---

   private static class GetSoftwareList extends GetTransaction {

      private AutoUpdateConfig config;
      private MerchantConfig merchantConfig;
      private InstanceRecord ir;
      private LinkedList softwareList;
      public GetSoftwareList(AutoUpdateConfig config, MerchantConfig merchantConfig, InstanceRecord ir, LinkedList softwareList) {
         this.config = config;
         this.merchantConfig = merchantConfig;
         this.ir = ir;
         this.softwareList = softwareList;
      }

      public String describe() { return Text.get(AutoUpdate.class,"s2"); }
      protected String getFixedURL() { return config.autoUpdateURL; }
      protected void getParameters(Query query) throws IOException {
         if (merchantConfig.isWholesale) {
            query.add("wholesalerID",Convert.fromInt(merchantConfig.merchant));
         } else {
            query.add("merchant",Convert.fromInt(merchantConfig.merchant));
         }
         query.addPasswordObfuscate("encpassword",merchantConfig.password);
         query.addPasswordCleartext("password",merchantConfig.password);
         if (ir.instanceID != null) query.add("instance",Convert.fromInt(ir.instanceID.intValue()));
         if (ir.passcode   != null) query.add("passcode",Convert.fromInt(ir.passcode  .intValue()));
      }

      protected boolean receive(InputStream inputStream) throws Exception {

         Document doc = XML.readStream(inputStream);

         ServerException e = ServerException.parseStandard(doc,/* required = */ false);
         if (e != null) throw e;

         Node n1 = XML.getElement(doc,"Configuration");

      // instance record

         Integer instanceID = new Integer(Convert.toInt(XML.getAttribute(n1,"InstanceID")));
         Integer passcode   = new Integer(Convert.toInt(XML.getAttribute(n1,"PassCode"  )));
         // can't use toNullableInteger because we don't want to allow null.

         // it's the caller's responsibility to make sure that we don't have
         // one field null, the other not.  here we just do the stupid thing.

         if (ir.instanceID == null) {
            ir.instanceID = instanceID;
         } else {
            if (instanceID.intValue() != ir.instanceID.intValue()) throw new Exception(Text.get(AutoUpdate.class,"e9",new Object[] { Convert.fromInt(instanceID.intValue()), Convert.fromInt(ir.instanceID.intValue()) }));
         }

         if (ir.passcode == null) {
            ir.passcode = passcode;
         } else {
            if (passcode.intValue() != ir.passcode.intValue()) throw new Exception(Text.get(AutoUpdate.class,"e10")); // don't display passcodes publicly, just in case someone's trying to hack (and server is lax)
         }

      // software records

         LinkedList temp = new LinkedList();

         Iterator i = XML.getElements(n1,"Software");
         while (i.hasNext()) {
            Node node = (Node) i.next();

            SoftwareRecord sr = new SoftwareRecord();
            sr.name = XML.getElementText(node,"Name");
            sr.version = XML.getElementText(node,"Version");
            sr.downloadURL = XML.getElementText(node,"DownloadURL");
            sr.size = Convert.toLong(XML.getElementText(node,"Size"));

            String s = XML.getNullableText(node,"Checksum");
            sr.checksum = (s == null) ? null : new Integer(Obfuscate.toChecksum(s));

            temp.add(sr);
         }

         softwareList.clear(); // could be a retry
         softwareList.addAll(temp);
         // do this last so that we never return a partial list

         return true;
      }
   }

   private static class DownloadVersion extends DownloadTransaction {

      private String url;
      private File file;
      public DownloadVersion(File file, SoftwareRecord desired) {
         super(/* overwrite = */ true,desired.size);
         this.url = desired.downloadURL + desired.version;
         this.file = file;
         initChecksum(desired.checksum);
      }

      public String describe() { return Text.get(AutoUpdate.class,"s3"); }
      protected String getFixedURL() { return url; }
      protected File getFile() { return file; }
   }

}

