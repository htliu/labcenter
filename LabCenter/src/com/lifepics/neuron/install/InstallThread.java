/*
 * InstallThread.java
 */

package com.lifepics.neuron.install;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Log;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.misc.AppUtil;
import com.lifepics.neuron.misc.FileUtil;
import com.lifepics.neuron.misc.TransferTracker;
import com.lifepics.neuron.net.DefaultHandler;
import com.lifepics.neuron.net.DescribeHandler;
import com.lifepics.neuron.net.DiagnoseConfig;
import com.lifepics.neuron.net.DiagnoseHandler;
import com.lifepics.neuron.net.DownloadTransaction;
import com.lifepics.neuron.net.GetTransaction;
import com.lifepics.neuron.net.Handler;
import com.lifepics.neuron.net.NetUtil;
import com.lifepics.neuron.net.PauseAdapter;
import com.lifepics.neuron.net.PauseCallback;
import com.lifepics.neuron.object.XML;
import com.lifepics.neuron.table.AlternatingFile;
import com.lifepics.neuron.thread.StoppableThread;
import com.lifepics.neuron.thread.ThreadStatus;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * A thread that downloads and installs various files.
 */

public class InstallThread extends StoppableThread {

// --- fields ---

   private ThreadStatus threadStatus;
   private TransferTracker tracker;
   private AppUtil.ControlInterface control;
   private File baseDir;
   private File mainDir;
   private String installURL;
   private File installFile;
   private String installName;

   private InstallConfig config;
   private DiagnoseHandler diagnoseHandler;
   private Handler handler;
   private PauseAdapter pauseAdapter;

// --- construction ---

   public InstallThread(ThreadStatus threadStatus, TransferTracker tracker, AppUtil.ControlInterface control, File baseDir, File mainDir, String installURL, File installFile, String installName) {
      super(Text.get(InstallThread.class,"s1"));

      this.threadStatus = threadStatus;
      this.tracker = tracker;
      this.control = control;
      this.baseDir = baseDir;
      this.mainDir = mainDir;
      this.installURL = installURL;
      this.installFile = installFile;
      this.installName = installName;

      // config starts out null
      // diagnoseHandler, ditto
      // handler, ditto
      pauseAdapter = new PauseAdapter(threadStatus);
   }

// --- interface for thread fields ---

   protected void doInit() throws Exception {
   }

   protected void doRun() throws Exception {
      try {

         // unlike most threads, this one doesn't loop, except in network retries.
         // it just does its thing and either errors out or succeeds and exits.

         install();

      } catch (Exception e) {
         if ( ! isStopping() ) threadStatus.fatal(e);
         throw e;
      }
   }

   protected void doExit() {
   }

   protected void doStop() {
   }

// --- main code ---

   private void install() throws Exception {

      // in LC, the state is stored in objects that exist even when the thread
      // isn't running, so if you stop and restart the thread, there's no read
      // from disk, it just uses the same object.  the only reason I did it
      // differently here is, it wasn't convenient to store the InstallConfig
      // anywhere outside.

      // one unfortunate thing is, if we get powered down while writing to the
      // config file, on restart we have no way to distinguish that from a new
      // start where we never got a complete download .. so, we'll re-download
      // and start from scratch.  I think it should be OK, though, because
      // the installer isn't something that runs unattended for days at a time;
      // power failures and other rare events shouldn't matter much.

   // if the file exists and is readable, continue where we left off last time

      AlternatingFile af = new AlternatingFile(installFile);
      if (af.exists()) {
         try {
            loadConfig();
         } catch (Exception e) {
            // ignore, leave the config null

            // except, we do need to delete it, because it'd be untidy
            // to download into the base file when an alternate exists.
            //
            af.delete();
         }
      }

      if (isStopping()) return;

   // if not, go get it from the install URL

      initNetworking();

      if (config == null) {

         Log.log(Level.INFO,this,"i1");

         DownloadFile t = new DownloadFile(installURL,installFile,/* verifySize = */ null,/* verifyXML = */ true,/* checksum = */ null,/* callback = */ null);
         if ( ! handler.run(t,pauseAdapter) ) return; // stopping

         loadConfig();

         // this is different than the order download behavior,
         // but better I think.  the question is, what happens
         // if we receive valid XML that's not a valid config?
         // order download would re-download, but here we fail.
         // you can try again by restarting the thread.
      }

      if (isStopping()) return;

   // work through the files

      String setupApp = config.getFile(InstallFile.TYPE_SETUP_APP).getAppName();
      // go ahead and check this now so we don't waste time if it's not there

      reinitNetworking();

      boolean complete = false;
      try {
         tracker.groupBegin1(config.files);
         tracker.groupBegin2(config.files);
         tracker.timeBegin();

         if ( ! installItems() ) return; // stopping

         complete = true;
      } finally {
         tracker.timeEnd(complete);
         tracker.groupEnd();
      }

   // done

      Log.log(Level.INFO,this,"i4");

      control.restart(setupApp);

      // there isn't any thread status for normal completion,
      // but it doesn't matter, we can leave the light green.
      // the app is going to exit anyway.
   }

   private boolean installItems() throws Exception {

      // this is a lot like Op.transact except that we never roll back,
      // we just keep track of what's been done and proceed from there.

      boolean started = false;

      Iterator i = config.files.iterator();
      while (i.hasNext()) {
         InstallFile file = (InstallFile) i.next();

         if (file.getDone()) {

            if (started) tracker.fileSkip(file,config.files); // skipped by branching
            // because we validate done flags, we can detect branches this simple way
            //
            // it's a little unfortunate that we skip the files one at a time instead
            // of all at once, but it'll still happen very quickly in the UI, and all
            // the other approaches I could think of were too hairy.

            continue;
         }
         started = true;

         String key = InstallFile.descriptionTable[file.method];
         Log.log(Level.INFO,InstallFile.class,key,new Object[] { file.getShortName() });

         try {
            tracker.fileBegin1(file);
            tracker.fileBegin2(file,config.files,/* sizeActual = */ null);

            if ( ! execute(file) ) {
               tracker.fileEndIncomplete();
               return false; // stopping
            }

            tracker.fileEndComplete();

         } catch (Exception e) {

            tracker.fileEndIncomplete();
            throw e;
         }

         file.done = Boolean.TRUE;
         saveConfig();

         if (isStopping()) return false;
      }

      return true;
   }

// --- config helpers ---

   private void loadConfig() throws IOException, ValidationException {
      config = (InstallConfig) AlternatingFile.load(installFile,new InstallConfig(),installName);
   }

   /**
    * We never want to hear about errors from this ...
    * if it goes wrong, just hope we can finish up
    * using the config object in memory.
    *
    * You might think this should be a serious error
    * like TableException.  The difference is, here,
    * if we don't save the config, don't finish, and restart,
    * we'll do some things over ... but we have to support
    * doing things over anyway for various reinstall cases.
    */
   private void saveConfig() {
      try {
         AlternatingFile.store(installFile,config,installName);
      } catch (IOException e) {
         // ignore
      }
   }

// --- network helpers ---

   private void initNetworking() {

      DiagnoseConfig diagnoseConfig = (DiagnoseConfig) new DiagnoseConfig().loadDefault(
         /* downRetriesBeforeNotification = */ 0,
         /* downRetriesEvenIfNotDown      = */ 5  );
      // the theory here is, installation is an interactive process,
      // not something we should keep retrying for hours on end.
      // so, show warnings immediately, and only retry for five minutes
      // before failing.  the user can restart by hand if necessary.

      diagnoseHandler = new DiagnoseHandler(new DefaultHandler(),diagnoseConfig);
      handler = new DescribeHandler(diagnoseHandler);

      NetUtil.setDefaultTimeout(60000); // hardcoded constant
   }

   private void reinitNetworking() {

      diagnoseHandler.reinit(config.diagnoseConfig);

      NetUtil.setDefaultTimeout(config.defaultTimeoutInterval);
   }

// --- execute - layer 1 ---

   // a fair amount of duplication with Op, but not worth unifying I think

   private boolean execute(InstallFile file) throws Exception {
      switch (file.method) {
      case InstallFile.METHOD_DOWNLOAD:     return executeDownload(file);
      case InstallFile.METHOD_MKDIR:        executeMkDir     (file);  break;
      case InstallFile.METHOD_RMDIR:        executeRmDir     (file);  break;
      case InstallFile.METHOD_MOVE:         executeMove      (file);  break;
      case InstallFile.METHOD_COPY:         executeCopy      (file);  break;
      case InstallFile.METHOD_MKEMPTY:      executeMkEmpty   (file);  break;
      case InstallFile.METHOD_DELETE:       executeDelete    (file);  break;
      case InstallFile.METHOD_EXTRACT_ONE:  executeExtractOne(file);  break;
      case InstallFile.METHOD_EXTRACT_ALL:  executeExtractAll(file);  break;
      case InstallFile.METHOD_QUERY:        return executeQuery(file);
      case InstallFile.METHOD_IMPORT:       executeImport    (file);  break;
      case InstallFile.METHOD_EXPORT:       executeExport    (file);  break;
      case InstallFile.METHOD_INVOKE:       executeInvoke    (file);  break;
      case InstallFile.METHOD_BRANCH:       executeBranch    (file);  break;
      default:  throw new IllegalArgumentException();
      }
      return true; // most methods have no path that allows stopping
   }

// --- execute - layer 2 ---

   private Exception fail0(String key) {
      return new Exception(Text.get(this,key));
   }

   private Exception fail1(String key, File dest) {
      return new Exception(Text.get(this,key,new Object[] { Convert.fromFile(dest) }));
   }

   private Exception fail2(String key, File src, File dest, Exception e) {
      return new Exception(Text.get(this,key,new Object[] { Convert.fromFile(src), Convert.fromFile(dest) }),e);
      // note, constructor Exception(message,/* cause = */ null) produces the same result as Exception(message)
   }

// --- execute - layer 3 ---

   // the theory here is, everything has to work correctly
   // even if we're doing it for the second time.
   // as part of that, we have to check whether everything
   // is a file or a directory, because if we accidentally
   // move a directory, we can't do that again.
   //
   // in some common cases you can use these utility functions
   // to do everything all at once, but in the rarer cases you
   // just have to check it all by hand.
   //
   // a slightly strange result of this strategy is,
   // mkdir has to work if the directory is already there,
   // but rmdir can fail if the directory is already gone,
   // and similarly for mkempty and delete.

   /**
    * Make sure the source file exists and is a file.
    */
   private File checkSrcFile(File src) throws Exception {
      if ( ! src.exists() ) throw fail1("e1",src);
      if ( ! src.isFile() ) throw fail1("e2",src);
      // isFile includes exists, but I want descriptive errors
      return src;
   }

   /**
    * Make sure the dest file doesn't exist.
    * This can include deleting it if there's a file there.
    */
   private File checkDestFile(File dest) throws Exception {
      if (dest.exists()) {
         if ( ! dest.isFile() ) throw fail1("e3",dest);
         if ( ! dest.delete() ) throw fail1("e4",dest);
      }
      return dest;
   }

// --- execute - layer 4 ---

   private File fs(InstallFile file) { return file.getSrcFile (baseDir,mainDir); }
   private File fd(InstallFile file) { return file.getDestFile(baseDir,mainDir); }

   private File fsc(InstallFile file) throws Exception { return checkSrcFile (fs(file)); }
   private File fdc(InstallFile file) throws Exception { return checkDestFile(fd(file)); }

// --- execute - layer 5 ---

   private boolean executeDownload(InstallFile file) throws Exception {
      String url = config.context.eval(file.src);
      File dest = fdc(file);
      return handler.run(new DownloadFile(url,dest,file.verifySize,file.getVerifyXML(),file.getChecksum(),tracker),pauseAdapter);
      // no need to wrap exceptions from run, the describe handler does that
   }

   private void executeMkDir(InstallFile file) throws Exception {
      File dest = fd(file);
      if (dest.exists()) {
         if ( ! dest.isDirectory() ) throw fail1("e5",dest);
      } else {
         if ( ! dest.mkdirs     () ) throw fail1("e6",dest);
      }
   }

   private void executeRmDir(InstallFile file) throws Exception {
      File dest = fd(file);
      if ( ! dest.exists     () ) throw fail1("e7",dest);
      if ( ! dest.isDirectory() ) throw fail1("e8",dest);
      if ( ! dest.delete     () ) throw fail1("e9",dest);
   }

   private void executeMove(InstallFile file) throws Exception {
      File src  = fsc(file);
      File dest = fdc(file);
      if ( ! src.renameTo(dest) ) throw fail2("e10",src,dest,null);
   }

   private void executeCopy(InstallFile file) throws Exception {
      File src  = fsc(file);
      File dest = fdc(file);
      try {
         FileUtil.copy(dest,src);
      } catch (Exception e) {
         throw fail2("e11",src,dest,e);
      }
   }

   private void executeMkEmpty(InstallFile file) throws Exception {
      File dest = fdc(file);
      boolean created = false;
      try {
         created = dest.createNewFile(); // this can return false or throw exception
      } catch (IOException e) {
         // fall through and fail
      }
      if ( ! created ) throw fail1("e12",dest);
   }

   private void executeDelete(InstallFile file) throws Exception {
      File dest = fd(file);
      if ( ! dest.exists() ) throw fail1("e13",dest);
      if ( ! dest.isFile() ) throw fail1("e14",dest);
      if ( ! dest.delete() ) throw fail1("e15",dest);
      // checkDestFile would do the last two, but I want nice messages.
      // that's also why I broke out exists to be separate from isFile.
      // also I like this being parallel to rmdir.
   }

   // notes about zip files:
   //
   // one, there's similar code in AutoUpdate.extract and ZipUtil.extractAll.
   // however, I don't want to do the work to unify with them, and who knows,
   // maybe we'll want to have some small differences.
   //
   // two, it would be possible to define an "extract subdirectory" method,
   // but the implementation would be a nuisance, and we don't need it.
   // the nuisance is, if you say "extract abc\def to ghi", how do you test
   // for membership in abc\def in a nice way, and do you include that path
   // when you do the extraction or do you remove it somehow.
   //
   // three, I didn't bother with applying time stamps, since nothing else
   // that the installer does preserves them.
   //
   // four, I'm going to be strict about directories.  if you have a zip file
   // that doesn't include parent directories, and try to do a full extract,
   // it will fail.  the solution is, use a jar file instead.  those can be built
   // incorrectly if you try, but they can also be built correctly.

   private void executeExtractOne(InstallFile file) throws Exception {

      // use similar syntax to java.net.JarURLConnection to specify both
      // the jar file and the entry within the file.  note, the slash is
      // correct even on Windows .. zip format specifies a forward slash.

      final String delimiter = "!/";

      int i = file.src.indexOf(delimiter);
      if (i == -1) throw fail0("e16"); // no detail, not wrapped, should be rare

      String srcJar   = file.src.substring(0,i);
      String srcEntry = file.src.substring(i+delimiter.length());

      File src = checkSrcFile(InstallFile.getFile(srcJar,baseDir,mainDir));

      File dest = fdc(file);

      ZipFile zf = null;
      try {
         zf = new ZipFile(src);

         ZipEntry ze = zf.getEntry(srcEntry);
         if (ze == null) throw fail0("e17"); // no detail, wrapped

         FileUtil.copy(dest,zf.getInputStream(ze));

      } catch (Exception e) {
         throw new Exception(Text.get(this,"e18",new Object[] { srcEntry, Convert.fromFile(src), Convert.fromFile(dest) }),e);
      } finally {
         if (zf != null) zf.close();
      }
   }

   private void executeExtractAll(InstallFile file) throws Exception {

      File src = fsc(file);

      File dest = fd(file);
      if ( ! dest.exists     () ) throw fail1("e19",dest); // fail2?  I don't think so
      if ( ! dest.isDirectory() ) throw fail1("e20",dest);

      ZipFile zf = null;
      try {
         zf = new ZipFile(src);

         Enumeration e = zf.entries();
         while (e.hasMoreElements()) {
            ZipEntry ze = (ZipEntry) e.nextElement();

            File target = new File(dest,ze.getName());
            if (ze.isDirectory()) { // like executeMkDir

               if (target.exists()) {
                  if ( ! target.isDirectory() ) throw fail1("e21",target);
               } else {
                  if ( ! target.mkdir      () ) throw fail1("e22",target);
               }

            } else { // like executeCopy

               checkDestFile(target);
               FileUtil.copy(target,zf.getInputStream(ze));
            }
         }

      } catch (Exception e) {
         throw fail2("e23",src,dest,e);
      } finally {
         if (zf != null) zf.close();
      }
   }

   private boolean executeQuery(InstallFile file) throws Exception {

      // the dest is for display only, not used in execution;
      // see comment at start of InstallFile helpers section
      String shortName = file.getShortName();
      String url = config.context.eval(file.src);

      QueryServer query = new QueryServer(shortName,url);
      if ( ! handler.run(query,pauseAdapter) ) return false;
      // no need to wrap exceptions from run, the describe handler does that

      doImport(file.binds,query.result);
      return true;
   }

   private void executeImport(InstallFile file) throws Exception {
      File dest = checkSrcFile(fd(file));
      Document doc = doAlternatingRead(dest);
      doImport(file.binds,doc);
   }

   private void executeExport(InstallFile file) throws Exception {
      File dest = checkSrcFile(fd(file));
      Document doc = doAlternatingRead(dest);
      doExport(file.binds,doc);
      doAlternatingWrite(dest,doc);
   }

   private void executeInvoke(InstallFile file) throws Exception {
      File dest = checkSrcFile(fd(file));
      // dest doesn't need to be in base directory, here; that's just a launcher restriction

      int j = file.target.lastIndexOf('.');
      if (j == -1) throw fail1("e25",dest);
      String targetClass  = file.target.substring(0,j);
      String targetMethod = file.target.substring(j+1);

      String[] args = new String[file.paramIn.size()]; // zero is possible
      j = 0;
      Iterator i = file.paramIn.iterator();
      while (i.hasNext()) {
         args[j++] = config.context.eval((String) i.next());
      }

      String nameOut = (file.paramOut == null) ? null : Context.unwrap(file.paramOut);

      Class [] classes = new Class [] { args.getClass(), Context.class, File.class, File.class, Handler.class, PauseCallback.class };
      Object[] objects = new Object[] { args, config.context, baseDir, mainDir, handler, pauseAdapter };

      String result;
      try {

         URLClassLoader loader = new URLClassLoader(new URL[] { dest.toURL() },InstallThread.class.getClassLoader());
         // use nested loader so that invoke jar can be tiny.
         // the tradeoff is, for any class that's in the installer, invoke has to use that version of the class.

         Class c = loader.loadClass(targetClass);

         Method m = c.getMethod(targetMethod,classes);
         if (m.getReturnType() != String.class) throw new IllegalArgumentException(); // note, runtime exception
         // we could also check it's static, or we can just get an exception

         result = (String) m.invoke(null,objects);

      } catch (Throwable t) {
         // catch throwable, not just exception, because of my IllegalArgumentException.
         // invoke could throw that too, if we passed the wrong arguments.
         // more importantly, we get a NullPointerException if the method's not static.

         String key = "e26";
         if (t instanceof InvocationTargetException) {
            key = "e27";
            t = ((InvocationTargetException) t).getTargetException(); // unwrap before we wrap it up again
         }

         throw new Exception(Text.get(this,key,new Object[] { Convert.fromFile(dest), t.getClass().getName() }),t);
         // include exception class, otherwise message is often way too cryptic
      }
      // note, none of the method arguments is capable of storing references
      // to unknown classes, so the class loader should be garbage collected.
      //
      // invoke jars can use Text as long as they don't overlap the installer packages.
      // see Text.java for details.
      //
      // I wanted to include a frame argument so that the method could pop up dialogs,
      // but that had all kinds of problems.  it potentially tied up garbage collection,
      // and also would have had to run in the GUI thread instead of the install thread,
      // so we'd need some interlocking wait mechanism that could be canceled.
      // so, if you want to pop a dialog, you really need to use a setup jar after all.
      //
      // there's also no use in sending the transfer tracker ... the invoke step
      // has a file size (verifySize) of zero, which will be applied as an upper bound.

      if (nameOut != null && result != null) config.context.set(nameOut,result);
      //
      // could be stricter and say that the two must be null or not null together,
      // but sometimes it's useful to ignore a result.  the other case, where you
      // expect but don't get a setting, maybe not so useful.
   }

   private void executeBranch(InstallFile file) throws Exception {
      ListIterator i;

      // the dest is for display only, not used in execution;
      // see comment at start of InstallFile helpers section

   // determine targets

      String target1; // primary
      String target2; // default

      int j = file.src.lastIndexOf('?');
      if (j != -1) {
         target1 = file.src.substring(0,j);
         target2 = file.src.substring(j+1);
      } else {
         target1 = file.src;
         target2 = null; // no default
      }

      target1 = config.context.eval(target1);
      // target2 should be constant, no eval

   // scan for indices

      // combined scan is slightly more efficient

      int index0 = -1;
      int index1 = -1;
      int index2 = -1;

      i = config.files.listIterator();
      while (i.hasNext()) {
         InstallFile scan = (InstallFile) i.next();

         if (                   scan == file                 ) index0 = i.previousIndex();
         if (                   scan.labels.contains(target1)) index1 = i.previousIndex();
         if (target2 != null && scan.labels.contains(target2)) index2 = i.previousIndex();
         //
         // in theory we might call previousIndex several times, slightly inefficient,
         // but in practice the targets will be different from the branch source
         // and also from each other since there's no point in adding other labels to
         // the default target.
         //
         // no need to worry about multiple hits, that's prevented by validation.
      }

   // see if they make sense

      // sanity check
      if (index0 == -1) throw new IllegalArgumentException(); // file was not in list

      // get the actual target into index1
      if (index1 != -1) {
         // primary found, we're already all set
      } else {
         if (target2 == null) {
            // primary not found, and no default, fail
            throw new Exception(Text.get(this,"e28",new Object[] { target1 }));
         } else {
            // primary not found but there's a default
            if (index2 != -1) {
               // default found, move to index1
               index1 = index2;
            } else {
               // default not found either, fail
               throw new Exception(Text.get(this,"e29",new Object[] { target1, target2 }));
            }
         }
      }

      // when there's a default, we could validate at load time whether
      // the label exists, but it's not worth duplicating all the logic.
      // similarly for primary targets that don't have evaluation.

      // debatable whether we should error out if the primary is found
      // but the default is specified and not found.  it should really
      // always be there.

      // make sure it's forward
      if (index1 <= index0) throw fail0("e30"); // no detail, should be rare

   // mark things done

      // if the target is the next thing in the list, we won't actually
      // mark anything done here.

      // could optimize by using listIterator(int), but it would have to do
      // the same scan internally, and it would make my code more confusing.

      i = config.files.listIterator();
      while (i.hasNext()) {
         InstallFile scan = (InstallFile) i.next();

         int index = i.previousIndex();
         if (index0 < index && index < index1) scan.done = Boolean.TRUE;
         // index0 will get marked done when we return,
         // and index1 should stay undone so that we'll execute it.
      }

      // there's no exception-throwing code between here and the part
      // where index0 gets marked done, so the whole thing is atomic.
      // if it weren't, the config file could become invalid because of
      // a bad done-flag transition at index0.

   // design notes

      // it might seem tempting to try and use the installItems loop
      // to scan forward and mark things done until we hit the label,
      // but it really doesn't work.  what about stop and restart in
      // the middle of a scan?  what about label not found then restart?
      // you could sort of make it work by adding a "scanning for label"
      // field to the config file, but even that fails once you allow
      // two potential targets.

      // I couldn't see how to make backward reference work with the
      // done flags, so that's not allowed.

      // as it turns out, this data-driven branch operation can handle
      // everything I want to do, but if you want to get back to what
      // I was originally imagining, the classic "if A equals B goto C",
      // just define a new function in Helper.jar that returns "true"
      // or "false", or maybe "eq" or "ne", then do a branch afterward
      // to "line13-[result]", say.  think assembly language!

      // when there's one target and the label isn't found, falling
      // through might seem useful, but it would make it impossible
      // to have a "goto constant" that was guaranteed to work.
      // if you want falling through, add a default target and make
      // it point to the next line.
   }

// --- import/export helpers ---

   private Document doAlternatingRead(File file) throws IOException {
      Document doc;
      AlternatingFile af = new AlternatingFile(file);
      try {
         InputStream inputStream = af.beginRead();
         doc = XML.readStream(inputStream);
      } finally {
         af.endRead();
      }
      return doc;
   }

   private void doAlternatingWrite(File file, Document doc) throws IOException {
      AlternatingFile af = new AlternatingFile(file);
      try {
         OutputStream outputStream = af.beginWrite();
         XML.writeStream(outputStream,doc);
         af.commitWrite();
      } finally {
         af.endWrite();
      }
   }

   private void doImport(LinkedList binds, Document doc) throws Exception {
      Context c = new Context();

      Iterator i = binds.iterator();
      while (i.hasNext()) {
         InstallFile.Bind b = (InstallFile.Bind) i.next();
         Node node = XML.getElementPath(doc,b.path);

         String value = XML.getText(node);
         c.set(b.name,value);
      }

      config.context.set(c); // set all at once, in one transaction
      // slightly academic, because we won't get past the import
      // until we get a successful read of all the bindings at once,
      // overwriting any leftover partials, but this way is cleaner.
   }

   private void doExport(LinkedList binds, Document doc) throws Exception {

      Iterator i = binds.iterator();
      while (i.hasNext()) {
         InstallFile.Bind b = (InstallFile.Bind) i.next();
         Node node = XML.getElementPath(doc,b.path);

         String value = config.context.get(b.name);
         XML.replaceElementText(node.getParentNode(),node,node.getNodeName(),value);
      }
   }

// --- transactions ---

   private static class DownloadFile extends DownloadTransaction {

      private String url;
      private File file;
      private boolean verifyXML;
      private FileUtil.Callback callback;

      public DownloadFile(String url, File file, Long verifySize, boolean verifyXML, Integer checksum, FileUtil.Callback callback) {
         super(/* overwrite = */ true,verifySize);

         this.url = url;
         this.file = file;
         this.verifyXML = verifyXML;
         initChecksum(checksum);
         this.callback = callback;
      }

      public String describe() { return Text.get(InstallThread.class,"s2",new Object[] { Convert.fromFile(file) }); }
      protected String getFixedURL() { return url; }
      protected File getFile() { return file; }
      protected FileUtil.Callback getCallback() { return callback; }

      protected boolean receive(InputStream inputStream) throws Exception {
         if ( ! super.receive(inputStream) ) return false;

         if (verifyXML) XML.readFile(file); // ignore result, we just want to see it work

         return true;
      }
   }

   private static class QueryServer extends GetTransaction {
      public Document result;

      private String shortName;
      private String url;
      public QueryServer(String shortName, String url) { this.shortName = shortName; this.url = url; }

      public String describe() { return Text.get(InstallThread.class,"s3",new Object[] { shortName }); }
      protected String getFixedURL() { return url; }

      protected boolean receive(InputStream inputStream) throws Exception {
         result = XML.readStream(inputStream);
         // so, basically verifyXML is built into this kind of transaction
         return true;
      }
   }

}

