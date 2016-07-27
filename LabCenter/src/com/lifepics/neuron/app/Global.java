/*
 * Global.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.axon.LocalImageSubsystem;
import com.lifepics.neuron.axon.PollSubsystem;
import com.lifepics.neuron.axon.RollManager;
import com.lifepics.neuron.axon.RollPurgeSubsystem;
import com.lifepics.neuron.axon.ScanSubsystem;
import com.lifepics.neuron.axon.UploadSubsystem;
import com.lifepics.neuron.dendron.AutoCompleteSubsystem;
import com.lifepics.neuron.dendron.CompletionSubsystem;
import com.lifepics.neuron.dendron.DownloadSubsystem;
import com.lifepics.neuron.dendron.InvoiceSubsystem;
import com.lifepics.neuron.dendron.JobManager;
import com.lifepics.neuron.dendron.JobPurgeSubsystem;
import com.lifepics.neuron.dendron.LocalSubsystem;
import com.lifepics.neuron.dendron.OrderManager;
import com.lifepics.neuron.dendron.OrderPurgeSubsystem;
import com.lifepics.neuron.dendron.SpawnSubsystem;
import com.lifepics.neuron.misc.AppUtil;
import com.lifepics.neuron.table.DerivedTable;
import com.lifepics.neuron.table.Table;
import com.lifepics.neuron.thread.ErrorWindow;

import java.io.IOException;
import java.util.LinkedList;

/**
 * A structure that holds global objects that the UI is aware of.
 */

public class Global {

// --- fields ---

   public Table orderTable;
   public Table jobTable;
   public Table rollTable;

   public DerivedTable groupTable;

   public OrderManager orderManager;
   public JobManager jobManager;
   public RollManager rollManager;

   public DownloadSubsystem downloadSubsystem;
   public UploadSubsystem uploadSubsystem;

   public InvoiceSubsystem invoiceSubsystem;
   public SpawnSubsystem spawnSubsystem;
   public LinkedList formatSubsystems; // list of ThreadDefinition, actually
   public CompletionSubsystem completionSubsystem;
   public JobPurgeSubsystem jobPurgeSubsystem;
   public AutoCompleteSubsystem autoCompleteSubsystem;
   public OrderPurgeSubsystem orderPurgeSubsystem;
   public LocalSubsystem localSubsystem;

   public ScanSubsystem scanSubsystem;
   public PollSubsystem pollSubsystem;
   public LocalImageSubsystem localImageSubsystem;
   public RollPurgeSubsystem rollPurgeSubsystem;

   public ErrorWindow errorWindowDownload;
   public ErrorWindow errorWindowUpload;
   public ErrorWindow errorWindowAutoComplete;

   public Control control;

// --- control interface ---

   public interface Control extends AppUtil.ControlInterface, RollManager.MemoryInterface {

      Config getConfig();
      void setConfig(Config config             ) throws IOException;
      void setConfig(Config config, Config auth) throws IOException;

      AutoUpdateConfig getServerAUC() throws Exception;
   }

}

