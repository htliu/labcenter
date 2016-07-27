/*
 * FormatHotFolder.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.misc.Op;
import com.lifepics.neuron.table.AlternatingFileAutoNumber;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Implementation of the hot folder format.
 */

public class FormatHotFolder extends Format {

// --- constants ---

   private static final String SEQUENCE_FILE = "sequence.txt";
   private static final int    SEQUENCE_NDIGITS = 6;
   private static final int    SEQUENCE_LIMIT = 990000; // see fix function for explanation

   private static final String TEMP_SUFFIX = ".tmp";

// --- subclass hooks ---

   public String getShortName() { return Text.get(this,"s1"); }

   public int[] getAllowedCompletionModes() { return new int[] { COMPLETION_MODE_MANUAL, COMPLETION_MODE_AUTO }; }
   public int   getCompletionMode(Object formatConfig) { return ((HotFolderConfig) formatConfig).completeImmediately ? COMPLETION_MODE_AUTO : COMPLETION_MODE_MANUAL; }
   public void  setCompletionMode(Object formatConfig, int mode) { ((HotFolderConfig) formatConfig).completeImmediately = (mode == COMPLETION_MODE_AUTO); }

// --- format function ---

   public void format(Job job, Order order, Object formatConfig) throws Exception {

      HotFolderConfig config = (HotFolderConfig) formatConfig;
      // because of validation, the cast is guaranteed to work

      require(config.dataDir);

   // (1) plan the operation

      LinkedList ops1 = new LinkedList();
      LinkedList ops2 = new LinkedList();

      AlternatingFileAutoNumber autoNumber = new AlternatingFileAutoNumber(new File(config.dataDir,SEQUENCE_FILE),0);
      int next = fix(autoNumber.getNextInteger());

      Iterator i = job.refs.iterator();
      while (i.hasNext()) {
         Job.Ref ref = (Job.Ref) i.next();
         Order.Item item = order.getItem(ref);

         int quantity = job.getOutputQuantity(item,ref);
         while (quantity-- > 0) {

            addFile(config.dataDir,order,ops1,ops2,next++,item.filename);
            // put this in a subfunction in case we ever want
            // to add support for single-sided creative products

            // no need to check for existence or disambiguate,
            // the sequence number takes care of that.
            // if there does happen to be a collision, we'll just
            // roll back and error out, that's fine.
         }
      }

      ops1.addAll(ops2); // all renames after all copies

      autoNumber.advanceTo(fix(next));
      // I don't want to try and make this an atomic operation,
      // so just go ahead and advance it now; no harm in using
      // up a few sequence numbers.

   // (2) alter files

      Op.transact(ops1);

   // (3) alter object

      if (config.completeImmediately) job.property = "";
   }

   private static int fix(int next) {

      if (next < 0 || next >= SEQUENCE_LIMIT) next = 0;

      // the first case only happens if someone hacks the sequence file.

      // the second case is the interesting one.  I've set the sequence limit
      // to 10000 below the digit threshold, so in normal operation,
      // the job that hits the limit will continue over the limit but not over
      // the digit threshold, and then the next job will start over at zero.
      // thus, the filenames in each job will be in order, and if the jobs are
      // separated in time, and the hot folder uses file name order, the files
      // will print in the desired order.  best we can do.
      //
      // if a job contains more than 10000 prints, that's not great but OK.
      // Convert.fromIntNDigit will wrap the first job around to zero, so
      // it will have files out of order, and then the next job will start over
      // at zero, and so might be delayed due to filename collision.
      //
      // if a job contains more than 1000000 prints, it could self-collide,
      // but that's not a problem I'm worried about.

      return next;
   }

   private static void addFile(File dataDir, Order order, LinkedList ops1, LinkedList ops2, int next, String filename) throws Exception {

      String goalname = Convert.fromIntNDigit(SEQUENCE_NDIGITS,next) + "-" + filename;
      String tempname = goalname + TEMP_SUFFIX; // two suffixes is OK

      File goalfile = new File(dataDir,goalname);
      File tempfile = new File(dataDir,tempname);

      ops1.add(new Op.Copy(tempfile,order.getPath(filename)));
      ops2.add(new Op.Move(goalfile,tempfile));
   }

// --- completion ---

   public boolean isCompleteWithoutDirectory(String property, Special special) throws Exception {
      return (property != null);
      // not null means job should complete immediately
   }

}

