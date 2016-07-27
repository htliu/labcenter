/*
 * FormatDirectPDF.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Text;

import java.util.LinkedList;

/**
 * Implementation of the direct-print PDF format.
 */

public class FormatDirectPDF extends Format {

// --- subclass hooks ---

   public String getShortName() { return Text.get(this,"s1"); }

   public int[] getAllowedCompletionModes() { return new int[] { COMPLETION_MODE_MANUAL, COMPLETION_MODE_AUTO }; }
   public int   getCompletionMode(Object formatConfig) { return ((DirectPDFConfig) formatConfig).completeImmediately ? COMPLETION_MODE_AUTO : COMPLETION_MODE_MANUAL; }
   public void  setCompletionMode(Object formatConfig, int mode) { ((DirectPDFConfig) formatConfig).completeImmediately = (mode == COMPLETION_MODE_AUTO); }

// --- format function ---

   public void format(Job job, Order order, Object formatConfig) throws Exception {

      DirectPDFConfig config = (DirectPDFConfig) formatConfig;
      // because of validation, the cast is guaranteed to work

   // get the one item

      if (job.refs.size() != 1) throw new Exception(Text.get(this,"e1")); // shouldn't happen
      Job.Ref ref = (Job.Ref) job.refs.getFirst();

      if ( ! ref.filename.toLowerCase().endsWith(".pdf") ) throw new Exception(Text.get(this,"e3"));
      // go ahead and check this before we do too much work

      Order.Item item = order.getItem(ref);

      // SKU code is totally ignored, a weird behavior of this queue

   // generate PJL commands

      LinkedList commands = new LinkedList();

      config.addStandardCommand(commands,job.getOutputQuantity(item,ref),config.orientation);
      config.addLanguageCommand(commands,"PDF");

   // send to printer

      config.sendToPrinter(commands,order.getPath(ref.filename),order.getFullID());

   // done

      if (config.completeImmediately) job.property = "";
   }

// --- completion ---

   public boolean isCompleteWithoutDirectory(String property, Special special) throws Exception {
      return (property != null);
      // not null means job should complete immediately
   }

}

