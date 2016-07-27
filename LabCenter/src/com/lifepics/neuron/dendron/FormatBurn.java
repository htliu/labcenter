/*
 * FormatBurn.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Log;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.misc.Op;
import com.lifepics.neuron.misc.StreamLogger;
import com.lifepics.neuron.misc.ZipUtil;
import com.lifepics.neuron.object.XML;
import com.lifepics.neuron.struct.SKU;
import com.lifepics.neuron.table.AlternatingFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Implementation of the media burn format.
 */

public class FormatBurn extends Format {

// --- constants ---

   private static String RESULT_FILE = "results.txt";

   private static String RESULT_SUCCESS = "success";
   private static String RESULT_FAILURE = "failure";

   private static String SETTINGS_FILE = "settings.xml";

// --- subclass hooks ---

   public String getShortName() { return Text.get(this,"s1"); }

   public int[] getAllowedCompletionModes() { return new int[] { COMPLETION_MODE_DETECT }; }
   public int   getCompletionMode(Object formatConfig) { return COMPLETION_MODE_DETECT; }
   public void  setCompletionMode(Object formatConfig, int mode) {}

// --- format function ---

   private static HashSet deployedTo = new HashSet(); // only deploy once per directory per LabCenter run
   private static HashSet allowEdits = new HashSet();
   static {
      allowEdits.add(SETTINGS_FILE);
   }

   public void format(Job job, Order order, Object formatConfig) throws Exception {}
   //
   public boolean formatStoppable(Job job, Order order, Object formatConfig, FormatCallback fc) throws Exception {

      BurnConfig config = (BurnConfig) formatConfig;
      // because of validation, the cast is guaranteed to work

      autoCreate(config.dataDir); // requireOrAutoCreate is better, but I can't be bothered with a config setting

      if (config.workingDir != null) {
         autoCreate(config.workingDir);

         if ( ! deployedTo.contains(config.workingDir) ) { // don't add right away, we want retry on fail
            try {
               File burnerFile = fc.getBurnerFile();
               if (burnerFile == null) throw new Exception(Text.get(this,"e8"));
               ZipUtil.refreshAll(burnerFile,config.workingDir,allowEdits);
               deployedTo.add(config.workingDir);
            } catch (Exception e) {
               throw new Exception(Text.get(this,"e6"),e); // wrap for readability
            }
         }
         // in Agfa we could condition on empty directory, but here we're going
         // to be sending updates some day, and also apparently there were some
         // manual installs before LC 7.3.1.
         //
         // note, it's important that refreshAll does everything atomically, because
         // it's possible the CD burner is already running and some of the files are
         // locked and in use.  moving stale files to a temp directory before delete
         // should catch that case.
         //
         // notes:
         // * the app doesn't modify any of its own files as it runs, otherwise we'd
         //   have to add more things to allowEdits.
         // * the current zip has no subdirectories.  if that changes, we'll need to
         //   make sure it has entries for the subdirectories, because LC won't auto-
         //   create.  or we could change LC at that time.
         // * partly the deploy is here because it's the easiest place to trigger it,
         //   but partly it's because this is private to RIMG / Fred Meyer.  it only
         //   deploys if someone knows to create the hidden CD integration type.

         try {
            updateSettings(config); // push dataDir to settings.xml if needed
         } catch (Exception e) {
            // ignore it so that the update can't break any existing installs
            // but do log it, otherwise we have no idea why it's not updating
            Log.log(Level.WARNING,this,"e7",e);
         }
      }

      if (job.refs.isEmpty()) throw new Exception(Text.get(this,"e1"));
      SKU sku = ((Job.Ref) job.refs.getFirst()).sku;
      BurnMapping m = (BurnMapping) MappingUtil.getMapping(config.mappings,sku);
      if (m == null) missingChannel(sku,order.getFullID());

      File root = new File(config.dataDir,order.getFullID()); // use full ID because manual
      root = vary(root);
      LinkedList files = new LinkedList();

   // (1) plan the operation

      LinkedList ops = new LinkedList();
      ops.add(new Op.Mkdir(root));

      HashSet fset = new HashSet();
      // fset probably isn't needed here -- items should be unique by filename and product type, and
      // jobs are split by product type, but there's no harm in it, and that's how FormatFlat did it.

      int quantity = 1; // quantity is validated to be positive, and we have at least one item, so this is fine

      Iterator i = job.refs.iterator();
      while (i.hasNext()) {
         Job.Ref ref = (Job.Ref) i.next();

         if ( ! ref.sku.equals(sku) ) throw new Exception(Text.get(this,"e2",new Object[] { sku.toString(), ref.sku.toString() }));
         // check against actual SKU, just in case

         Order.Item item = order.getItem(ref);

         int temp = job.getOutputQuantity(item,ref);
         if (temp > quantity) quantity = temp;

         List filenames = item.isMultiImage() ? item.filenames : Collections.singletonList(item.filename);
         Iterator j = filenames.iterator();
         while (j.hasNext()) {
            String filename = (String) j.next();
            if (fset.add(filename)) {
               String file = filename; // no subdirectory
               files.add(file);
               ops.add(new Op.Copy(new File(root,file),order.getPath(filename)));
            }
         }
      }

      if (config.command != null) {
         // workingDir is also not null in this case.  the Runtime.exec call (*) could handle null,
         // but it would complicate the auto-deploy logic, let's not get into it.

         ops.add(new Launch(config.command,config.workingDir,root.getName(),m.productType,quantity));

         files.add(RESULT_FILE);
         // go ahead and own the result file even though it doesn't exist yet.
         // if the job gets purged before it exists, you get a warning in the
         // log file, that's all.
      }

   // (2) alter files

      Op.transact(ops);

   // (3) alter object

      job.dir = root;
      job.files = files;

      return true;
   }

// --- settings file ---

   // config.workingDir isn't really a free parameter.  I thought the CD
   // burner app would read from the working directory, but no, the path
   // is hard-coded to this.
   // C:\LifePics\CdBurner\settings.xml

   // this code fails in Java 1.4 because the XML parser can't handle the
   // three-byte prefix in settings.xml, but it works in Java 1.6,
   // and everyone's using that by now.  the output file may not have the
   // prefix, but I checked that the CD burner can still parse it.

   private static void updateSettings(BurnConfig config) throws Exception {

      File file = new File(config.workingDir,SETTINGS_FILE);
      Document doc = XML.readFile(file);
      Node node = XML.getElement(doc,"settings");
      Node rdir = XML.getElement(node,"rootDirectory");
      String rval = XML.getAttribute(rdir,"value");

      String goal = Convert.fromFile(config.dataDir) + File.separatorChar;
      // separator at end is necessary, the app just uses a string concat

      if ( ! rval.equals(goal) ) {

         XML.setAttribute(rdir,"value",goal);
         // normally I only use this to create new attributes, but it can
         // also be used to update them, and it's not a misuse of the API

         AlternatingFile af = new AlternatingFile(file);
         try {
            OutputStream outputStream = af.beginWrite();
            XML.writeStream(outputStream,doc);
            af.commitWrite();
         } finally {
            af.endWrite();
         }
         // the CD burner doesn't understand about alternating files,
         // so no point in using one to read, but at least we can do
         // a nice atomic write.
      }
   }

// --- launch ---

   private static class Launch extends Op {

      private String command;  // not null here
      private File workingDir; // could be null
      private String dataSubdir;
      private String productType;
      private int quantity;

      public Launch(String command, File workingDir, String dataSubdir, String productType, int quantity) {
         this.command = command;
         this.workingDir = workingDir;
         this.dataSubdir = dataSubdir;
         this.productType = productType;
         this.quantity = quantity;
      }

      public void dodo() throws IOException {

         // note, only need subdir because the CD burner app has data dir in its config file.
         // not really necessary to place quotes around subdir, but it's a string, good form.
         command += " \"";
         command += dataSubdir;
         command += "\" \"";
         command += productType;
         command += "\" ";
         command += Convert.fromInt(quantity);

         Process p;
         try {
            p = Runtime.getRuntime().exec(command,null,workingDir); // (*)
         } catch (IOException e) {
            throw (IOException) new IOException(Text.get(FormatBurn.class,"e3")).initCause(e);
         }

         p.getOutputStream().close();  // better close the output stream
         // so that the external process can't lock up trying to read it.

         new StreamLogger(FormatBurn.class,"i1",p.getInputStream()).start();
         new StreamLogger(FormatBurn.class,"i2",p.getErrorStream()).start();
      }

      public void undo() {
      }
      // no way to undo this, the operation must come last in list
   }

// --- completion ---

   public File isCompleteOrError(File dir, String property) throws Exception {

      File file = new File(dir,RESULT_FILE);
      if ( ! file.exists() ) return null; // not complete

      FileReader fr = null;
      BufferedReader br = null;
      String line = null;
      try {
         fr = new FileReader(file);
         br = new BufferedReader(fr);
         line = br.readLine();
      } catch (Exception e) {
         if (br != null) try { br.close(); } catch (Exception e2) {}
         if (fr != null) try { fr.close(); } catch (Exception e2) {}
         throw new Exception(Text.get(this,"e4"),e);
      }

      if (line == null) line = ""; // don't put "null" into error message; also this makes the other code easier
      String lineSave = line;
      line = line.toLowerCase();
      if (line.equals(RESULT_SUCCESS)) {
         return dir; // complete (and directory not renamed)
      } else {
         throw new Exception(Text.get(this,line.equals(RESULT_FAILURE) ? "e5a" : "e5b",new Object[] { lineSave }));
      }
   }

}

