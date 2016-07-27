/*
 * AppUtil.java
 */

package com.lifepics.neuron.misc;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Log;
import com.lifepics.neuron.core.Pop;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.launch.Parse;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.UIManager;

/**
 * A utility class for common application setup code.
 * It contains some code directly, and some comments
 * telling you where to find the other code you need.
 */

public class AppUtil {

// --- notes about static objects ---

   // in the library code (core, launch, and eight others now)
   // there are a few places that have nasty static objects.
   // here's a list so you can watch out for them and handle
   // them appropriately.

   // * Log.setReportInterface
   // this is how the low-level log information gets back up
   // to the report subsystem that sends them to the server.
   // why I didn't implement this as another handler on the
   // base logger, who knows, but that's how it is.  if you
   // don't set the variables, the logging code ignores them,
   // so there's no problem here, just an optional feature.

   // * Style.setStyle
   // this is mostly just a relic from the LC 4.0 conversion,
   // although it does also handle the QDPC UI modifications.
   // if you don't set it, you get nasty old classic style,
   // so it's hard to miss.  if you don't want to make the
   // style configurable, as it is in LC, just call setStyle
   // with value getDefaultStyle, that'll do.

   // * User.setConfig and setFrame
   // this is a bad one -- if you don't set them, you'll get
   // a NPE.  the frame might be safe, but it's still nasty.
   // the solution is, don't invoke this code.  so, no calls
   // into User, or into SubsystemMonitor, which does a tell
   // about other processes.
   //
   // if for some reason you do want the User code, you'll
   // probably want to call User.terminate at app exit too.
   //
   // also, if you're calling User, you'll either need to
   // make sure sounds are off or include the sound files
   // in the application jar file.

   // * Print.setConfig
   // this is another bad one, and again the solution is,
   // don't invoke this code.  so, no calls into Print
   // or into HTMLViewer, which invokes it.  eventually we
   // may want to allow HTMLViewer in a non-printing mode,
   // but that's what we've got for now.

// --- notes about external links ---

   // one other thing to watch out for is ShellUtil.
   // it's well-behaved and won't crash, but it also won't
   // work without the related DLLs, which won't be there.

   // another is the email validator in net.Email.
   // it's also well-behaved, but it won't work without the
   // file mail.jar.  I list that as a DLS jar now,
   // but it's really a standard Java one that I'm reusing.

   // image handling capabilities are also slightly reduced
   // because the imageio jar won't be there.

// --- code in other places ---

   // there are a couple of functions in NetUtil that you'll
   // also want to call to set things up.  I didn't put them
   // here because misc is supposed to be a lower-level
   // package than net, and also because I want all the code
   // that uses HTTPClient to be in the net package.

// --- code here ---

   /**
    * If your main class isn't inheriting from AppUtil.Stub,
    * you should execute this as static code at the very start
    * of your main class, so that it runs as soon as possible.
    */
   public static void setDefaultLocale() {
      Locale.setDefault(Locale.US);
   }

   /**
    * This is more of a GUI function, but it's fine here too,
    * not worth making a whole 'nother class in that package.
    * Keep the code static so that non-stubs can call it too.
    */
   public static void initLookAndFeel() {
      try {
         UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      } catch (Exception e) {
         // ignore and continue
      }
   }

// --- UI view of stub ---

   public interface ControlInterface {

      void exit();
      void restart(String version);
   }

// --- stub class ---

   // it's easiest for this setup code to go in a class
   // because that way we can use some member variables.

   public static abstract class Stub implements ControlInterface {

      static { setDefaultLocale(); }
      //
      // this runs before anything else, including static initializers in other files.
      // do it because (a) it fixes a problem with DecimalFormat in Convert,
      // and (b) the rest of the program isn't localized, so there's no sense in e.g.
      // having the message boxes have Oui and Non buttons.
      //
      // this even runs before static initializers in Main, because parent classes are
      // initialized before child classes!  this only works for Stub, not for AppUtil.

   // --- constants ---

      // only ones used by multiple applications should go here

      protected static final String CONFIG_FILE  = "config.xml";
      protected static final String INSTALL_FILE = "install.xml";

      protected static final String NAME_INSTALL = "Install";

   // --- cross-thread utility ---

      // this could easily be factored out into a stand-alone class

      // we have a flag in addition to an object in case the send
      // happens before the receive.

      private Object crossThreadObject;
      private boolean crossThreadFlag;
      private String crossThreadValue;

      protected Stub() {
         crossThreadObject = new Object();
         crossThreadFlag = false;
         crossThreadValue = null;
      }

      private void crossThreadSend(String value) {
         crossThreadValue = value;
         synchronized (crossThreadObject) { crossThreadFlag = true; crossThreadObject.notify(); }
      }

      private String crossThreadReceive() {
         try {
            synchronized (crossThreadObject) { if ( ! crossThreadFlag ) crossThreadObject.wait(); }
         } catch (InterruptedException e) {
            // won't happen
         }
         return crossThreadValue;
      }

   // --- subclass hooks ---

      // the subclass also needs to define a set of text constants.
      // most of these are sent back down to the subclass as causeKey ...
      // i1 in init, i2-i6 in exit.  there are also strings s1 and e1
      // that are used for error reporting.

      /**
       * Init the application.
       * @return Null to continue on to the UI phase, non-null to launch a different jar file.
       */
      protected abstract String init(String causeKey, String[] args, boolean launched) throws ProcessException;

      /**
       * Start the UI phase.
       */
      protected abstract void run();

      /**
       * Exit the application.  This is called exactly once regardless of
       * how far the init process went, so it has to check whether things
       * were initialized or not.
       */
      protected abstract void exit(String causeKey);

   // --- helper class ---

      /**
       * An exception class used to report multiple instances.
       * AppUtil.Stub subclasses can throw this during init
       * to produce an exit without showing any error message.
       */
      public static class MultipleInstanceException extends ProcessException {}

   // --- execute ---

      // unfortunately there doesn't seem to be any way to factor out
      // the static main function(s), you just have to copy and paste.

      // there are five possible control flows here:
      //
      // (1) multiple instances detected                i4
      // (2) other exception during initialization      i2
      // (3) restart request during initialization      i5
      // (4a) UI launches, UI menu exit                 i3
      // (4b) UI launches, restart request from thread  i6
      //
      // the causeKey values are just historical accidents

      public String execute(String[] args, boolean launched) {

         String version = null;
         try {

            version = init("i1",args,launched);

         } catch (MultipleInstanceException e) { // (1) multiple instances detected

            exit("i4");
            System.exit(0);

            // the shutdown probably isn't needed,
            // since UI not started, but be safe

            // this log message goes nowhere, since lock detection
            // happens before file logging starts ...
            // which is good since the other LC is already logging.

         } catch (ProcessException e) { // (2) other exception during initialization

            Log.log(Level.SEVERE,this,"e1",e); // may not go anywhere, depending on how far we got
            Pop.error(null,e,Text.get(this,"s1"));
            //
            // as far as I know, this is the only place I use a null owner ...
            // which is good, because it creates an unkillable frame within Java
            // to be the owner, so we can't do any more hot restarts because the
            // UI thread won't exit.  here, though, it's OK, because we know
            // we're going to shut down immediately after the dialog is finished.

            exit("i2");
            System.exit(0);
         }

         if (version != null) { // (3) restart request during initialization

            exit("i5");
            return version;
         }

         // (4) UI launches

         run();

         // now we need to lock up the invoking thread
         // so that we can return a version string after a live auto-update.
         // if the user exits LC, this thread will be killed by System.exit.

         version = crossThreadReceive();
         return version;

         // one nice side effect of this way of doing things: the restart thread
         // starts running during init, but even if it decides fairly quickly
         // that a restart is needed, we still don't restart at some random time.
         // init and run are guaranteed to complete.
      }

      // I would have liked to make exit call crossThreadSend(null) and then
      // push the exit(causeKey) and System.exit(0) code back into execute,
      // for symmetry with the other three control flows, but that doesn't work ...
      // once the UI is launched, it's important that the exit code execute in
      // the UI thread, not the main thread.  for example, we might want to
      // show a stop dialog inside StoppableThread.stopNice.  it's possible we
      // could work around that, or prove it safe, but it's not worth it just
      // for symmetry.

      public void exit() { // (4a) UI launches, UI menu exit
         exit("i3");
         System.exit(0);
      }

      public void restart(String version) { // (4b) UI launches, restart request from thread
         exit("i6");
         crossThreadSend(version);

         // now the event thread is supposed to detect that there aren't any more windows and
         // shut itself down, but that's not guaranteed.  even the Sun documentation says so ...
         //
         // from http://java.sun.com/j2se/1.4.2/docs/api/java/awt/doc-files/AWTThreadIssues.html:
         // Note, that while an application following these recommendations will exit cleanly
         // under normal conditions, it is not guaranteed that it will exit cleanly in all cases.
         //
         // that's disturbing, but what can we do except be as thorough as possible?
         // case in point: apparently if you've played a sound, the app will never exit by itself.
         //
         // on the other hand, the event thread seems not to be associated with a ClassLoader,
         // so obviously I don't have a good understanding of what's going on.
         // if you want to investigate, how about this:  without exitTimers, the timers carry
         // forward into the next version and continue running!
      }

   // --- main dir ---

      // the idea is, the base directory defines the base for everything;
      // it completely replaces the working directory.
      // all "new File" calls should have two arguments except for it.
      // there's one exception that I'm too lazy to correct right now,
      // which is in ConfigFormat ... when the file picker encounters
      // a blank string, it uses the working directory.
      // apart from that, the base directory really is used everywhere.

      protected File mainDir;
      protected File baseDir;
      protected boolean minimizeCommand;

      // academic now that baseDir is never null: new File(null,x) == new File(x)

      protected void initMainDir(String[] args) throws ProcessException {
         int i = 0;

         baseDir = Parse.getBaseDir(args);
         if (baseDir != null) i = Parse.getBaseDirOffset();

         // until just now, I was allowing baseDir to be a relative path,
         // but at the same time I had code in FileMapper.getRoot
         // (and maybe other places) that assumed all paths were absolute.
         // this is the simplest way to make it all behave correctly.
         //
         if (baseDir == null) baseDir = new File("");
         if ( ! baseDir.isAbsolute() ) baseDir = baseDir.getAbsoluteFile();

         if (i < args.length && args[i].equals("-min")) {
            minimizeCommand = true;
            i++;
         } else {
            minimizeCommand = false;
         }

         if (i != args.length-1) throw new ProcessException(Text.get(AppUtil.class,"e2"));
         mainDir = new File(baseDir,args[i]);
      }

   // --- logging ---

      private static final String LOGGER = "com.lifepics.neuron";

      private static final String LOG_DIR     = "log";
      private static final String LOG_PATTERN = "log/log%g.txt";

      private Logger logger;
      private FileHandler handler;

      protected void initLogging1() {

         logger = Logger.getLogger(LOGGER);

         // stop console logging
         logger.setUseParentHandlers(false);
      }

      /**
       * A helper that creates the directories.
       * Only the installer app should do this!
       */
      protected void initLoggingC() {
         initLoggingC(/* createMain = */ true);
      }
      protected void initLoggingC(boolean createMain) {

         File logDir = new File(mainDir,LOG_DIR);

         if (createMain && ! mainDir.exists()) mainDir.mkdir();
         if ( ! logDir .exists() ) logDir .mkdir();

         // no need to report failure, initLogging2 will handle it
      }

      /**
       * Init with reasonable defaults when there's no config file.
       */
      protected void initLogging2() throws ProcessException {
         initLogging2(/* logCount = */ 3,/* logSize = */ 100000,/* logLevel = */ Level.FINE);
      }

      protected void initLogging2(int logCount, int logSize, Level logLevel) throws ProcessException {

         File logPattern = new File(mainDir,LOG_PATTERN);
         try {
            handler = new FileHandler(Convert.fromFile(logPattern),logSize,logCount);
         } catch (IOException e) {
            throw new ProcessException(Text.get(AppUtil.class,"e6"),e);
         }
         handler.setFormatter(new LineFormatter());
         logger.addHandler(handler);
         logger.setLevel(logLevel);
      }

      protected void reinitLogging(Level logLevel) {
         if ( ! logLevel.equals(logger.getLevel()) ) {
            logger.setLevel(logLevel);
            Log.log(Level.SEVERE,AppUtil.class,"e7",new Object[] { Convert.fromLevel(logLevel) });
         }
      }

      protected void exitLogging() {
         if (logger != null && handler != null) {
            logger.removeHandler(handler);
            handler.close();
         }
      }
   }

}

