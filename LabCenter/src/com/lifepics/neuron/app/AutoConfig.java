/*
 * AutoConfig.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Log;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.misc.Compress;
import com.lifepics.neuron.misc.FileUtil;
import com.lifepics.neuron.net.CompressUploadTransaction;
import com.lifepics.neuron.net.GetTransaction;
import com.lifepics.neuron.net.Handler;
import com.lifepics.neuron.net.PauseCallback;
import com.lifepics.neuron.net.Query;
import com.lifepics.neuron.net.ServerException;
import com.lifepics.neuron.object.XML;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.logging.Level;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Static code that handles auto-config behavior,
 * i.e., that keeps the local config info
 * in sync with the config info on the server.
 *
 * Compare the name to AutoUpdate, not to XyzConfig.
 */

public class AutoConfig {

// --- interfaces ---

   public interface PushConfig {

      void pushConfig(Config config) throws Exception;

      /**
       * @param callback A function that should be called atomically with the push.
       */
      void pushServer(Config config, PushCallback callback) throws Exception;
   }

   public interface PushCallback {
      void run(File target) throws Exception;
   }

// --- validation helpers ---

   public static void validateConfig(Config config) throws ValidationException {
      if (config.autoUpdateConfig.instanceID     != null) throw new ValidationException(Text.get(AutoConfig.class,"e4"));
      if (config.autoUpdateConfig.passcode       != null) throw new ValidationException(Text.get(AutoConfig.class,"e17"));
      if (config.autoUpdateConfig.revisionNumber != null) throw new ValidationException(Text.get(AutoConfig.class,"e5"));
   }

   public static void validateServer(Config server) throws ValidationException {
      if (server.autoUpdateConfig.instanceID     == null) throw new ValidationException(Text.get(AutoConfig.class,"e2"));
      if (server.autoUpdateConfig.passcode       == null) throw new ValidationException(Text.get(AutoConfig.class,"e16"));
      if (server.autoUpdateConfig.revisionNumber == null) throw new ValidationException(Text.get(AutoConfig.class,"e3"));
   }

   /**
    * For most config fields, we know how to distribute changes to the right places,
    * so no restart is needed.  For a few, though, it's too hard to distribute them.
    *
    * Note that whether a field is edited in the UI has nothing to do with anything.
    * If it's edited, it must be redistributed, but so what?
    * We want to redistribute even unedited fields so that auto-config will work well.
    */
   public static boolean equalsUndistributed(Config c1, Config c2) {

      return (    c1.proMode == c2.proMode
               && c1.style.equals(c2.style)
               && c1.lockPort == c2.lockPort

               && c1.logCount == c2.logCount
               && c1.logSize == c2.logSize
               // logLevel is distributed now
               && c1.reportLevel.equals(c2.reportLevel)
               && c1.reportQueueSize == c2.reportQueueSize
               && c1.kioskLogCount == c2.kioskLogCount
               && c1.kioskLogSize == c2.kioskLogSize   );
   }

// --- wrapper layer ---

   // this should really be unified with the diagnosing code,
   // but it'll do for now.  only wrapper constructor needed.
   //
   // putting the try-catch around the handler call is too broad,
   // since it covers parseable but wrong/invalid XML structures,
   // but, again, it'll do for now.

   private static class TemporaryException extends Exception {
      public TemporaryException(String message, Throwable cause) { super(message,cause); }
   }

   private static class Flag {
      public boolean value;
      public Flag() { value = false; }
   }

   /**
    * Run through the standard auto-config procedure.
    *
    * @param config The current local config.
    * @param server The last reported server config, or null if no server config is available.
    * @param ir The instance information, newly-allocated if server is null.
    * @param pushConfig An interface to set the config with.  We can't just return the config
    *                   because we need the transaction to commit while we're running in here.
    *
    * @return True if a restart is needed.
    */
   public static boolean execute(Config config, Config server, AutoUpdate.InstanceRecord ir, LinkedList softwareList,
                                 PushConfig pushConfig,
                                 Handler handler, PauseCallback callback, File baseDir, File mainDir) {
      Flag restart = new Flag();
      try {
         String key = (server != null) ? "i6b" : "i6a";
         String description = Text.get(AutoConfig.class,key,new Object[] { Convert.fromInt(ir.instanceID.intValue()) }); // known not null

         Log.log(Level.INFO,AutoConfig.class,"i1",new Object[] { description });

         executeFlag(config,server,ir,softwareList,pushConfig,handler,callback,baseDir,mainDir,restart);

         Log.log(Level.INFO,AutoConfig.class,"i5");
      } catch (TemporaryException e) {
         Log.log(Level.SEVERE,AutoConfig.class,"e19",e);
         // I think the theory here was, we shouldn't report errors on network conditions
         // because they might just be temporary.  but, then again, they might not ...
         // anyway we're going to try logging them as severe rather than just warning.
      } catch (Exception e) {
         Log.log(Level.SEVERE,AutoConfig.class,"e15",e);
      }

      // not sure where to put this comment ... normally when a handler returns false,
      // you're supposed to just stop and exit quietly, without throwing exceptions.
      // here (below), we do throw exceptions, but only because we know it will get us
      // to the right case up here.  we know we're going to catch and log it, and
      // we don't want to say that the auto-config was a success, because it wasn't.

      return restart.value;
      // the reason for this flag business is, it's possible that the auto-config process
      // will require a restart *and* throw an exception.  (it happens when there are
      // both local and remote changes, and the remote changes get merged in, but the write
      // to the server fails.)  so, the flag state has to be outside the exception handler.
   }

// --- main ---

   private static void executeFlag(Config config, Config server, AutoUpdate.InstanceRecord ir, LinkedList softwareList,
                                   PushConfig pushConfig,
                                   Handler handler, PauseCallback callback, File baseDir, File mainDir, Flag restart) throws Exception {

   // 1. check parameter consistency

      Integer revisionNumber = null;

      AutoUpdate.SoftwareRecord sr = AutoUpdate.findByName(softwareList,AutoUpdate.SOFTWARE_CONFIG);
      if (sr != null) {
         revisionNumber = new Integer(Convert.toInt(sr.version));
         // downloadURL should be empty, and size should be zero,
         // but we won't require that

         if (revisionNumber.intValue() == -1) revisionNumber = null;
         // cut the server some slack, it'll want to do this
      }

      // because we're careful to make the server file write
      // atomic with the put transaction, these should never
      // have different nullness.

      if (server != null) {
         if (revisionNumber == null) throw new Exception(Text.get(AutoConfig.class,"e6"));
      } else {
         if (revisionNumber != null) throw new Exception(Text.get(AutoConfig.class,"e7"));
      }

      // FYI, we already know that the config file doesn't have an instanceID
      // or revision number, and that the server file does have both of them.
      // these things are validated in Main, when the files are read off disk.

   // 2. make sure we have an instance ID

      // actually the instance ID is allocated in auto-update now

      // it would be nice to save the instance ID somewhere,
      // so that if we fail to send the initial file,
      // we don't request another new instance ID next time ...
      // but there's not really anywhere good to put it.
      //
      // related note: the reason the instance ID needs to be null
      // in the config file is, the user is allowed to modify that.
      // manual editing, reverting from backup, even copying
      // from one install to another ... everything is allowed.

      // semi-related note: if you search for instanceID and passcode,
      // you'll see that they always appear in pairs, doing the exact
      // same things.  so what's the point of having both?
      // the point is to prevent one dealer from messing up another!
      // the instance ID is just a serial key, and a user could
      // easily edit a server file to give it a different valid ID,
      // with disastrous results.  the passcode should prevent that.
      //
      // it doesn't prevent copying a LC installation, but then
      // nothing we do can prevent that; in fact sometimes it's
      // exactly what we want (e.g., if moving to new computer)

   // 3a. set up

      Config remote = null; // the update from the server
      Config update = config.copy(); // the possibly-updated local config

   // 3b. refresh some fields from server

      // baseDir not currently used here, but it could be in the future

      AutoUpdate.updateSoftware(mainDir,update.autoUpdateConfig.invoice,AutoUpdate.SOFTWARE_INVOICE,softwareList,handler,callback);
      AutoUpdate.clean(mainDir,update.autoUpdateConfig.invoice);
      //
      // notes on invoice auto-update:
      // (1) the standard invoice is still the most common case, we'll keep building that one into the LabCenter file.
      // (2) even when there's a custom invoice in the software list, we always keep the standard invoice.xsl file up
      // to date too, mostly just because it was easier to code that way.
      // (3) the old customInvoiceXsl field is being removed.  as of LC 7.1.0, if it's false, we set it to null, and
      // if it's true, we set it to null and archive off the custom invoice.xsl just in case.
      // (4) one issue I didn't deal with, what if we need to be able to release a new LC with new invoice at exactly
      // the same time?  in theory we update the DB and it all works, but if there's network trouble we could have an
      // inconsistent set.  something to come back to if necessary.

      AutoUpdate.updateSoftware(mainDir,update.autoUpdateConfig.burner,AutoUpdate.SOFTWARE_BURNER,softwareList,handler,callback);
      AutoUpdate.clean(mainDir,update.autoUpdateConfig.burner);

      AutoRefresh.refresh(update);

      // as far as I know, it doesn't matter whether we do this
      // before or after the next step

   // 3c. merge in server changes, if any

      if (server != null) {

         // note, server not null implies revisionNumber not null; we checked just above
         int delta = (revisionNumber.intValue() - server.autoUpdateConfig.revisionNumber.intValue());

         if (delta <  0) throw new Exception(Text.get(AutoConfig.class,"e8"));
         //  delta == 0  means nothing new
         if (delta >  0) {

            // ok, the server has something for us, go get it

            Log.log(Level.INFO,AutoConfig.class,"i3");

            GetDataTransaction t = new GetDataTransaction(config.autoUpdateConfig.autoConfigURL,ir.instanceID.intValue(),ir.passcode.intValue());
            try {
               if ( ! handler.run(t,callback) ) throw new Exception(Text.get(AutoConfig.class,"e13")); // better luck next time
            } catch (Exception e) {
               throw new TemporaryException(Text.get(AutoConfig.class,"e20"),e);
            }
            remote = t.result;

            validateServer(remote); // prevent nulls

            // instance ID and passcode must match
            if (remote.autoUpdateConfig.instanceID.intValue() != server.autoUpdateConfig.instanceID.intValue()) throw new Exception(Text.get(AutoConfig.class,"e9"));
            if (remote.autoUpdateConfig.passcode  .intValue() != server.autoUpdateConfig.passcode  .intValue()) throw new Exception(Text.get(AutoConfig.class,"e18"));

            // revision number must be at least as large as reported
            // (could be larger if update just hit)
            if (remote.autoUpdateConfig.revisionNumber.intValue() < revisionNumber.intValue()) throw new Exception(Text.get(AutoConfig.class,"e10"));
            //
            // the error message here is "the received revision number has gone backward",
            // which is not quite correct ... the number has gone backward relative to
            // what we were told on the auto-update page, not necessarily relative to what
            // the local server file says.

            // ok, merge (yes, we always merge, even if there are no local changes)

            update.merge(server,remote);

            update.autoUpdateConfig.instanceID     = null; // it already is, but I like to be sure
            update.autoUpdateConfig.passcode       = null;
            update.autoUpdateConfig.revisionNumber = null; // this one we really are setting to null
            // if you add more fields here, also add them to Restore.getConfig
         }
      }

   // 3d. finish

      if (remote != null || ! update.equals(config)) {

         try {
            update.validate();
         } catch (ValidationException e) {
            throw new Exception(Text.get(AutoConfig.class,"e11"),e);
         }
         // when fields depend on one another (for example, one has to be larger
         // than another) separately valid changes may not be valid when merged.
         // right now, the best way to fix this error is to edit the local config
         // to a compatible state, let the merge happen, then re-edit.

         if (remote != null) {

            // update server.xml and config.xml simultaneously
            //
            final Config updateF = update;
            final PushConfig pushConfigF = pushConfig;
            //
            pushConfig.pushServer(remote,new PushCallback() { public void run(File target) throws Exception {
               pushConfigF.pushConfig(updateF);
            } });

         } else { // ! update.equals(config)

            // just update config.xml
            //
            pushConfig.pushConfig(update);
         }

         // we could have a third case for remote != null but update.equals(config),
         // where we update the server file only, but it's not worth messing with.
      }

      // as of now, the updated local config is the official config.
      // in practice, the one effect is that if we're sending local changes
      // back to the server, we'll use the updated auto-config URL to do it

   // 4. figure out what else we should do

      // so, what do we have?  up to four config objects.  some may be null,
      // which is confusing, but at least the objects are always distinct.
      // here's one way of looking at them, with local and remote as parallel
      // vertical histories ... or you can tilt it to get a merge diamond.
      //
      //    update <-- remote
      //       ^          ^
      //       |          |
      //    config <-- server
      //
      // another good way to look at it is case by case.
      //
      //                              config  update  server  remote
      //                              ------  ------  ------  ------
      // not persisted on server yet  *       *       null    null
      // server had nothing           *       *       *       null
      // server had new revision      *       *       *       *
      //
      // what we do next depends on two different comparisons.
      //
      // * to see if we need to send an update to the server, we want to compare
      //   the latest local config (update) to the latest remote config
      //   (remote or server).  here we want to look at everything except the three
      //   auto-config fields that aren't present in local config files.
      //
      // * to see if we need to restart, we want to compare update to config,
      //   but look only at certain specific fields that force a restart.
      //
      // note that there's no exception-throwing code between the config save,
      // above, and the restart computation here.  in fact, there's *no* code.
      // so, if a restart is called for, we'll definitely say so.

      restart.value = ! equalsUndistributed(update,config);
      boolean send;
      Integer sendRevisionNumber;

      Config ptrLocal  =  update;
      Config ptrRemote = (remote != null) ? remote : server; // null if server is

      if (server == null) {

         // not worth unifying with other cases
         send = true;
         sendRevisionNumber = new Integer(0);

      } else {

         Integer saveInstanceID     = ptrRemote.autoUpdateConfig.instanceID;
         Integer savePasscode       = ptrRemote.autoUpdateConfig.passcode;
         Integer saveRevisionNumber = ptrRemote.autoUpdateConfig.revisionNumber;
         ptrRemote.autoUpdateConfig.instanceID     = null;
         ptrRemote.autoUpdateConfig.passcode       = null;
         ptrRemote.autoUpdateConfig.revisionNumber = null;

         send = ! ptrLocal.equals(ptrRemote);
         sendRevisionNumber = new Integer(saveRevisionNumber.intValue() + 1);

         ptrRemote.autoUpdateConfig.instanceID     = saveInstanceID;
         ptrRemote.autoUpdateConfig.passcode       = savePasscode;
         ptrRemote.autoUpdateConfig.revisionNumber = saveRevisionNumber;

         // actually we know the instance ID and passcode, but I like the clarity
      }

   // 5. send back local changes, if any

      // originally I thought that if we had changes on both sides,
      // we'd end up reconfiguring twice ... but now that
      // we don't keep the revision number in the local config file,
      // that doesn't happen!

      if (send) {

         // we're done with the update and config objects now,
         // so we can just write the server fields onto ptrLocal
         //
         ptrLocal.autoUpdateConfig.instanceID     = ir.instanceID;
         ptrLocal.autoUpdateConfig.passcode       = ir.passcode;
         ptrLocal.autoUpdateConfig.revisionNumber = sendRevisionNumber;

         Log.log(Level.INFO,AutoConfig.class,"i4");

         // update server.xml and web server simultaneously
         //
         final Config ptrLocalF = ptrLocal;
         final Handler handlerF = handler;
         final PauseCallback callbackF = callback;
         //
         pushConfig.pushServer(ptrLocal,new PushCallback() { public void run(File target) throws Exception {
            PutDataTransaction t = new PutDataTransaction(ptrLocalF,target);
            try {
               if ( ! handlerF.run(t,callbackF) ) throw new Exception(Text.get(AutoConfig.class,"e14"));
            } catch (Exception e) {
               throw new TemporaryException(Text.get(AutoConfig.class,"e21"),e);
            }
         } });

         // if someday we want to make the code super-fancy, we could
         // catch ServerException and look for code 1, which means
         // that the revision number is no good, i.e., somebody else
         // updated the config before us.  not sure if we'd want
         // to loop back and merge the new change on top of the old,
         // or instead just scrap everything and start over.
         //
         // the two merge methods can give different results, but it's
         // not a big deal ... the same thing happens in other cases too.
         // if we change a field on the server from A to B, then later
         // back from B to A, what happens on the client depends on whether
         // or not it sees the intermediate state.  if it does,
         // the server change will overwrite and then overwrite back to A;
         // if not, the client setting will be left at its original value.
         //
         // anyway, as it is, we'll just wait and try again next cycle.
      }
   }

// --- HTTP transactions ---

   private static final String NAME_REMOTE = "Server"; // must be same as Main.NAME_SERVER
      // because in PutDataTransaction we just send the already-built server XML file

   public static class GetDataTransaction extends GetTransaction {

      public Config result;

      private String autoConfigURL;
      private int instanceID;
      private int passcode;
      public GetDataTransaction(String autoConfigURL, int instanceID, int passcode) {
         this.autoConfigURL = autoConfigURL;
         this.instanceID = instanceID;
         this.passcode = passcode;
      }

      public String describe() { return Text.get(AutoConfig.class,"s2"); }
      protected String getFixedURL() { return combine(autoConfigURL,"GetData.asp"); }
      protected void getParameters(Query query) throws IOException {
         query.add("instance",Convert.fromInt(instanceID));
         query.add("passcode",Convert.fromInt(passcode));
      }

      protected boolean receive(InputStream inputStream) throws Exception {
         Document doc = XML.readStream(Compress.wrapInput(inputStream));

         // with most transactions, the result format is fixed up front,
         // but here it's just whatever we choose to send.
         // so, as the config structure changes, so will the transaction.

         try {
            result = (Config) XML.loadDoc(doc,new Config(),NAME_REMOTE);
         } catch (ValidationException e) {
            // not a config, must be an error structure
            throw ServerException.parseStandard(doc,/* required = */ true);
         }

         return true;
      }
   }

   public static class DownloadDataTransaction extends GetDataTransaction { // subclass of the one above!

      // equivalent to DownloadTransaction with overwrite set, except that we
      // call Compress.wrapInput.  not worth merging into DownloadTransaction.

      // note, this doesn't verify that the file contains valid config file data
      // instead of, say, an error structure!  you could group it with a parser
      // like we did in DownloadThread, but the error structure isn't likely to change,
      // might as well just parse the file in a separate step afterward.

      private File file;
      public DownloadDataTransaction(String autoConfigURL, int instanceID, int passcode, File file) {
         super(autoConfigURL,instanceID,passcode);
         this.file = file;
      }

      public String describe() { return Text.get(AutoConfig.class,"s4"); }

      protected boolean receive(InputStream inputStream) throws Exception {
         FileUtil.makeNotExists(file);
         FileUtil.copy(file,Compress.wrapInput(inputStream));
         return true;
      }
   }

   public static class PutDataTransaction extends CompressUploadTransaction {

      // the config object contains all other fields we need

      private Config config;
      private File file;
      public PutDataTransaction(Config config, File file) { this.config = config; this.file = file; }

      public String describe() { return Text.get(AutoConfig.class,"s3"); }
      protected String getFixedURL() { return combine(config.autoUpdateConfig.autoConfigURL,"PutData.asp"); }
      protected void getParameters(Query query) throws IOException {

         // these three are known not null
         query.add("instance",Convert.fromInt(config.autoUpdateConfig.instanceID    .intValue()));
         query.add("passcode",Convert.fromInt(config.autoUpdateConfig.passcode      .intValue()));
         query.add("revision",Convert.fromInt(config.autoUpdateConfig.revisionNumber.intValue()));

         if (config.merchantConfig.isWholesale) {
            query.add("wholesalerID",Convert.fromInt(config.merchantConfig.merchant));
         } else {
            query.add("mlrfnbr",Convert.fromInt(config.merchantConfig.merchant));
         }
         // the server will ignore the wholesaler field for now, just show null mlrfnbr
      }

      protected File getFile() { return file; }

      protected boolean receive(InputStream inputStream) throws Exception {
         Document doc = XML.readStream(inputStream);

         ServerException e = ServerException.parseStandard(doc,/* required = */ true);
         if (e.getCode() != 0) throw e;
         // here the result is always an error structure

         return true;
      }
   }

}

