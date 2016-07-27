/*
 * InstallSubsystem.java
 */

package com.lifepics.neuron.install;

import com.lifepics.neuron.misc.AppUtil;
import com.lifepics.neuron.misc.TransferListener;
import com.lifepics.neuron.misc.TransferTracker;
import com.lifepics.neuron.object.Copyable;
import com.lifepics.neuron.thread.StoppableThread;
import com.lifepics.neuron.thread.Subsystem;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * The install subsystem, that downloads and installs various files.
 */

public class InstallSubsystem extends Subsystem {

// --- config class ---

   public static class Config implements Copyable {

      public AppUtil.ControlInterface control;
      public File baseDir;
      public File mainDir;
      public String installURL;
      public File installFile;
      public String installName;

      public Object clone() throws CloneNotSupportedException { return super.clone(); }

      public boolean equals(Object o) {
         if ( ! (o instanceof Config) ) return false;
         Config c = (Config) o;

         return (    control == c.control
                  && baseDir.equals(c.baseDir)
                  && mainDir.equals(c.mainDir)
                  && installURL.equals(c.installURL)
                  && installFile.equals(c.installFile)
                  && installName.equals(c.installName) );
      }
   }

// --- fields ---

   private TransferTracker tracker;

   // this is similar to TransferSubsystem, but not similar enough
   // to be worth inheriting from it.  the trouble is,
   // we use the DiagnoseHandler in a weird way ... we don't know
   // the config at subsystem construction time, and we reinit it
   // from inside the thread while we're running.

// --- construction ---

   public InstallSubsystem(Copyable config) {
      super(config);

      tracker = new TransferTracker();
      tracker.setAdapter(new TransferTracker.Adapter() {

         public String getGroupID(Object group) { return ""; } // not displayed
         public Iterator getItems(Object group) { return ((LinkedList) group).iterator(); }

         public String getFilename(Object item)         { return ((InstallFile) item).getShortName();  }
         public long getSize(Object item, Object group) { return ((InstallFile) item).getVerifySize(); }
         public boolean isComplete(Object item)         { return ((InstallFile) item).getDone();       }
      });
   }

// --- passthroughs ---

   public void setTransferListener(TransferListener listener) {
      tracker.setListener(listener);
   }

   public void ping() {
      tracker.ping();
   }

// --- subclass hook ---

   protected StoppableThread makeThread() {
      Config c = (Config) this.config;
      return new InstallThread(makeThreadStatus(),tracker,c.control,c.baseDir,c.mainDir,c.installURL,c.installFile,c.installName);
   }

}

