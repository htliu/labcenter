/*
 * DownloadSubsystem.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Nullable;
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
 * The download subsystem of the Dendron application.
 */

public class DownloadSubsystem extends TransferSubsystem {

// --- config class ---

   public static class Config extends TransferSubsystem.Config implements Copyable {

      public Table table;
      public OrderManager orderManager;
      public MerchantConfig merchantConfig;
      public DownloadConfig downloadConfig;
      public File dataDir;
      public File stylesheet;
      public boolean holdInvoice;
      public ProductConfig productConfig;
      public LinkedList coverInfos;
      public boolean enableItemPrice;
      public File localShareDir;

      public Object clone() throws CloneNotSupportedException {
         Config c = (Config) super.clone();

         c.merchantConfig = merchantConfig.copy(); // don't share
         c.downloadConfig = downloadConfig.copy();
         c.productConfig = productConfig.copy();
         c.coverInfos = CopyUtil.copyList(coverInfos);

         return c;
      }

      public boolean equals(Object o) {
         if ( ! (o instanceof Config) ) return false;
         if ( ! super.equals(o) ) return false;
         Config c = (Config) o;

         return (    table == c.table
                  && orderManager == c.orderManager
                  && merchantConfig.equals(c.merchantConfig)
                  && downloadConfig.equals(c.downloadConfig)
                  && dataDir.equals(c.dataDir)
                  && stylesheet.equals(c.stylesheet)
                  && holdInvoice == c.holdInvoice
                  && productConfig.equals(c.productConfig)
                  && coverInfos.equals(c.coverInfos)
                  && enableItemPrice == c.enableItemPrice
                  && Nullable.equals(localShareDir,c.localShareDir) );
      }
   }

// --- construction ---

   private static final long SIZE_ESTIMATE = 1048576; // nice large number, not visible to user
   private static long adjust(Long size) { return (size != null) ? size.longValue() : SIZE_ESTIMATE; }

   public DownloadSubsystem(Copyable config) {
      super(config);

      tracker.setAdapter(new TransferTracker.Adapter() {

         public String getGroupID(Object group) { return ((OrderStub) group).getFullID(); }
         public Iterator getItems(Object group) { return ((Order) group).files.iterator(); }

         public String getFilename(Object item) { return ((Order.OrderFile) item).filename; } // preferred over originalFilename
         public long getSize(Object item, Object group) throws IOException { return adjust(((Order.OrderFile) item).size); }
         public boolean isComplete(Object item) { return ( ((Order.OrderFile) item).status == Order.STATUS_ITEM_RECEIVED ); }
      });
   }

// --- subclass hook ---

   protected StoppableThread makeThread() {
      Config c = (Config) this.config;
      return new DownloadThread(c.table,
                                c.orderManager,
                                c.merchantConfig,
                                c.downloadConfig,
                                c.dataDir,
                                c.stylesheet,
                                c.holdInvoice,
                                c.productConfig,
                                c.coverInfos,
                                c.enableItemPrice,
                                c.localShareDir,
                                handler,
                                tracker,
                                makeThreadStatus());
   }

}

