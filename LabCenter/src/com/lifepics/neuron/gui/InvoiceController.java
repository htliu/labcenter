/*
 * InvoiceController.java
 */

package com.lifepics.neuron.gui;

import com.lifepics.neuron.thread.SubsystemController;

/**
 * An extension of SubsystemController, to allow the GUI
 * to detect InvoiceSubsystem and add special components.
 * This is ugly, but not as ugly as adding GUI functions
 * to SubsystemController.
 */

public interface InvoiceController extends SubsystemController {

   /**
    * Start the subsystem's thread with temporary options.
    */
   void start(boolean skipInvoice, boolean skipLabel);

}

