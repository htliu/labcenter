/*
 * PollThread.java
 */

package com.lifepics.neuron.axon;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Log;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.misc.ExtensionFileFilter;
import com.lifepics.neuron.net.Email;
import com.lifepics.neuron.net.MerchantConfig;
import com.lifepics.neuron.table.Table;
import com.lifepics.neuron.thread.StoppableThread;
import com.lifepics.neuron.thread.ThreadStatus;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Level;

import org.w3c.dom.Document;

/**
 * A thread that polls for new rolls from various sources.
 */

public class PollThread extends StoppableThread implements PollCallback {

// --- fields ---

   private PollConfig config;
   private MerchantConfig merchantConfig;
   private LinkedList dealers;
   private RollManager rollManager;
   private ThreadStatus threadStatus;

// --- construction ---

   public PollThread(PollConfig config, MerchantConfig merchantConfig, LinkedList dealers, RollManager rollManager, ThreadStatus threadStatus) {
      super(Text.get(PollThread.class,"s1"));

      this.config = config;
      this.merchantConfig = merchantConfig;
      this.dealers = dealers;
      this.rollManager = rollManager;
      this.threadStatus = threadStatus;
   }

// --- interface for thread fields ---

   protected void doInit() throws Exception {
   }

   protected void doRun() throws Exception {
      try {
         while ( ! isStopping() ) {
            poll();
            sleepNice(config.pollInterval);
         }
      } catch (Exception e) {
         if ( ! isStopping() ) threadStatus.fatal(e); // StoppableThread will log
         throw e;
      }
   }

   protected void doExit() {
   }

   protected void doStop() {
   }

// --- implementation of PollCallback ---

   private void applyDefaultEmail(Roll roll) {

      // this is arbitrarily restricted to Fuji right now.
      // it could be moved up to roll manager level,
      // but I like knowing it won't apply to manual rolls.
      // actually, the image hot folder uses it too.

      if (    (roll.source == Roll.SOURCE_HOT_FUJI || roll.source == Roll.SOURCE_HOT_IMAGE)
           && config.defaultEmail != null ) {

         // this handles both empty and invalid email addresses
         try {
            Email.validate(roll.email);
         } catch (ValidationException e) {
            roll.email = config.defaultEmail;
         }
      }
   }

   public int submit(Roll roll, File[] files) throws Exception {
      applyDefaultEmail(roll);
      rollManager.createMakeItems(roll,files);
      return roll.rollID;
   }

   public int submitWithItems(Roll roll, int method, Document order) throws Exception {
      applyDefaultEmail(roll);
      rollManager.createHaveItems(roll,method,order,null);
      return roll.rollID;
   }

   public int submitInPlace(Roll roll, File[] files) throws Exception {
      applyDefaultEmail(roll);
      rollManager.createInPlace(roll,files);
      return roll.rollID;
   }

   public void throttle() throws Exception {

      boolean paused = false;

      Table table = rollManager.getTable();
      while (table.count() >= config.throttleCount) { // goal is to sit at throttleCount

         if ( ! paused ) { threadStatus.pausedWait(Text.get(this,"s2")); paused = true; }
         // here we can ignore the previous-state token, since we know we were running

         sleepNice(config.throttleInterval);

         if (isStopping()) throw new Exception(Text.get(this,"e8"));
         // this is normal operation, so it's not really polite to throw an exception,
         // but it would be a pain to make provision for stopping all the way through.
      }

      if (paused) threadStatus.unpaused();
      // no need for a try-finally ... if we're stopping, we'll go
      // to stopped status; no need to go back to normal one first
   }

   public Dealer getDealerForLocation(String locationID) throws Exception {

      // here the names get tangled up -- both merchantConfig.merchant
      // and locationID are really Merchant Location Reference Numbers.

      if (merchantConfig.isWholesale) {
         Dealer dealer = Dealer.findByID(dealers,locationID);
         if (dealer == null) throw new Exception(Text.get(this,"e10",new Object[] { locationID }));
         return dealer;
      } else {
         if (Convert.toInt(locationID) != merchantConfig.merchant) throw new Exception(Text.get(this,"e11",new Object[] { locationID }));
         return null;
      }
   }

   public boolean ignore(String filename) {

      // eventually we might have more than two modes -- e.g., an intermediate level
      // of ignoring specific types and allowing non-understood types to fall through
      // and error out -- and we might have callers other than XMLPoller, but for now
      // this is all we need.

      if ( ! config.ignoreMode ) return false; // not enabled, keep

      return ! ExtensionFileFilter.hasExtension(filename,ItemUtil.extensions);
   }

// --- main functions ---

   // this thread is a bit unusual ... if there are no targets,
   // it won't do anything even when enabled.
   // all other threads always potentially do something, I think.
   // the only reason I don't make it not run in that case
   // is that it looks weird in the UI to have a non-green light.

   private void poll() throws Exception {

      Iterator i = config.targets.iterator();
      while (i.hasNext() && ! isStopping()) {
         PollConfig.Target target = (PollConfig.Target) i.next();

         Poller poller = getPoller(target.source);

         if ( ! target.directory.exists() ) throw new Exception(Text.get(this,"e2",new Object[] { Convert.fromFile(target.directory) }));

         File[] file = target.directory.listFiles(poller.getFileFilter());
         if (file == null) throw new Exception(Text.get(this,"e3",new Object[] { Convert.fromFile(target.directory) }));

         for (int j=0; j<file.length && ! isStopping(); j++) {
            processTryCatch(poller,file[j]);
         }
      }
   }

   private void processTryCatch(Poller poller, File file) {
      try {

         Log.log(Level.INFO,this,"i1",new Object[] { Convert.fromFile(file) });

         poller.process(file,this);

         Log.log(Level.INFO,this,"i2",new Object[] { Convert.fromFile(file) });

      } catch (Exception e) {

         e = new Exception(Text.get(this,"e6",new Object[] { Convert.fromFile(file) }),e);
            // want file name to show up in the UI as well as in the log

         Log.log(Level.SEVERE,this,"e7",e);
         threadStatus.error(e);
      }
   }

// --- poller look-up ---

   // pollers have no state, so static instances are fine

   private static Poller simplePoller = new SimplePoller();
   private static Poller xmlPoller = new XMLPollerLarge(); // uses XMLPoller indirectly
   private static Poller fujiPoller = new FujiPoller();
   private static Poller orderPoller = new OrderPoller();
   private static Poller emailPoller = new EmailPoller();
   private static Poller imagePoller = new ImagePoller();

   private static Poller getPoller(int source) throws Exception {

      switch (source) {
      case Roll.SOURCE_HOT_SIMPLE:  return simplePoller;
      case Roll.SOURCE_HOT_XML:     return xmlPoller;
      case Roll.SOURCE_HOT_FUJI:    return fujiPoller;
      case Roll.SOURCE_HOT_ORDER:   return orderPoller;
      case Roll.SOURCE_HOT_EMAIL:   return emailPoller;
      case Roll.SOURCE_HOT_IMAGE:   return imagePoller;
      default:  throw new Exception(Text.get(PollThread.class,"e1",new Object[] { RollEnum.fromSource(source) }));
         // this is a thread-stopper, because it's not inside the block that recovers from exceptions
      }

      // before, I passed the target source field down to the poller object,
      // which put it into the roll.  now, though, the pollers just put in
      // whatever they want, so the target source is a totally independent thing,
      // used only for this lookup here (and potentially in the UI someday).
   }

}

