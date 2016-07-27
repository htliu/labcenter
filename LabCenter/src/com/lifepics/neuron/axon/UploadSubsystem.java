/*
 * UploadSubsystem.java
 */

package com.lifepics.neuron.axon;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Nullable;
import com.lifepics.neuron.misc.FileUtil;
import com.lifepics.neuron.misc.TransferTracker;
import com.lifepics.neuron.net.MerchantConfig;
import com.lifepics.neuron.net.TransferSubsystem;
import com.lifepics.neuron.object.Copyable;
import com.lifepics.neuron.object.CopyUtil;
import com.lifepics.neuron.table.Table;
import com.lifepics.neuron.thread.StoppableThread;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * The upload subsystem of the Axon application.
 */

public class UploadSubsystem extends TransferSubsystem {

// --- config class ---

   public static class Config extends TransferSubsystem.Config implements Copyable {

      public Table table;
      public MerchantConfig merchantConfig;
      public UploadConfig uploadConfig;
      public File transformFile;
      public LinkedList dealers;
      public boolean prioritizeEnabled;
      public Long rollReceivedPurgeInterval;

      public Object clone() throws CloneNotSupportedException {
         Config c = (Config) super.clone();

         c.merchantConfig = merchantConfig.copy(); // don't share
         c.uploadConfig = uploadConfig.copy();
         c.dealers = CopyUtil.copyList(dealers);

         return c;
      }

      public boolean equals(Object o) {
         if ( ! (o instanceof Config) ) return false;
         if ( ! super.equals(o) ) return false;
         Config c = (Config) o;

         return (    table == c.table
                  && merchantConfig.equals(c.merchantConfig)
                  && uploadConfig.equals(c.uploadConfig)
                  && transformFile.equals(c.transformFile)
                  && dealers.equals(c.dealers)
                  && prioritizeEnabled == c.prioritizeEnabled
                  && Nullable.equals(rollReceivedPurgeInterval,c.rollReceivedPurgeInterval) );
      }
   }

// --- construction ---

   public UploadSubsystem(Copyable config) {
      super(config);

      tracker.setAdapter(new TransferTracker.Adapter() {

         public String getGroupID(Object group) { return Convert.fromInt(((Roll) group).rollID); }
         public Iterator getItems(Object group) { return ((Roll) group).items.iterator(); }

         public String getFilename(Object item) { return ((Roll.Item) item).getOriginalFilename(); }
         public long getSize(Object item, Object group) throws IOException { return FileUtil.getSize(new File(((Roll) group).rollDir,((Roll.Item) item).filename)); }
         public boolean isComplete(Object item) { return ( ((Roll.Item) item).status == Roll.STATUS_FILE_SENT ); }
      });
   }

// --- subclass hook ---

   protected StoppableThread makeThread() {
      Config c = (Config) this.config;
      return new UploadThread(c.table,
                              c.merchantConfig,
                              c.uploadConfig,
                              c.transformFile,
                              c.dealers,
                              c.prioritizeEnabled,
                              c.rollReceivedPurgeInterval,
                              handler,
                              tracker,
                              makeThreadStatus());
   }

}

