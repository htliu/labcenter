/*
 * RollManager.java
 */

package com.lifepics.neuron.axon;

import com.lifepics.neuron.core.ChainedException;
import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Log;
import com.lifepics.neuron.core.Pop;
import com.lifepics.neuron.core.Reportable;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.misc.FileUtil;
import com.lifepics.neuron.misc.KioskLog;
import com.lifepics.neuron.misc.Purge;
import com.lifepics.neuron.net.DefaultHandler;
import com.lifepics.neuron.net.Email;
import com.lifepics.neuron.net.Handler;
import com.lifepics.neuron.net.MerchantConfig;
import com.lifepics.neuron.net.PauseCallback;
import com.lifepics.neuron.net.Transaction;
import com.lifepics.neuron.object.CopyUtil;
import com.lifepics.neuron.table.LockException;
import com.lifepics.neuron.table.Table;
import com.lifepics.neuron.thread.StopDialog;
import com.lifepics.neuron.thread.Subsystem;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.logging.Level;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import org.w3c.dom.Document;

/**
 * A utility class for manipulating rolls
 * at the table and file levels simultaneously.
 */

public class RollManager {

// --- fields ---

   private Table table;
   private MerchantConfig merchantConfig;
   private File dataDir;
   private String tryPurgeLocalImageURL;

// --- construction ---

   public RollManager(Table table, MerchantConfig merchantConfig, File dataDir, String tryPurgeLocalImageURL) {
      this.table = table;
      this.merchantConfig = merchantConfig;
      this.dataDir = dataDir;
      this.tryPurgeLocalImageURL = tryPurgeLocalImageURL;
   }

   public synchronized void reinit(MerchantConfig merchantConfig, File dataDir, String tryPurgeLocalImageURL) {
      this.merchantConfig = merchantConfig;
      this.dataDir = dataDir;
      this.tryPurgeLocalImageURL = tryPurgeLocalImageURL;
   }

// --- accessors ---

   public Table getTable() {
      return table;
   }

// --- roll creation (manual) ---

   private Roll startManual() {

   // set up the source-dependent fields
   // (and also the rollID, which is needed for editing in the UI)

      Roll roll = new Roll();

      roll.rollID = -1; // shows as "NEW", will be auto-numbered later
      roll.source = Roll.SOURCE_MANUAL;
      roll.email = "";

      if (ProMode.isProOld()) roll.transformType = new Integer(Roll.TRANSFORM_TYPE_REGULAR);

      return roll;
   }

   private void finishManual(Roll roll, Frame owner) throws Exception {
      createHaveItems(roll,METHOD_COPY,null,owner);
   }

// --- roll creation (Pakon) ---

   public void createPakon(String uploadDirectory, String bagID, int totalBytesInDirectory, String email) throws Exception {

      // this still has a bagID argument because Pakon sends it,
      // but we don't even store it in the roll object any more.

   // check parameters

      File uploadDir = new File(uploadDirectory);

      if ( ! uploadDir.exists() ) {
         throw new IOException(Text.get(this,"e1",new Object[] { uploadDirectory }));
      }

      if ( ! uploadDir.isDirectory() ) {
         throw new IOException(Text.get(this,"e2",new Object[] { uploadDirectory }));
      }

      File[] files = uploadDir.listFiles(new FileFilter() { public boolean accept(File file) {
         return ( ! file.isDirectory() );
      } });
      // there will be a subdirectory for low resolution versions of the images

      if (files.length == 0) {
         throw new IOException(Text.get(this,"e4"));
      }

      long totalSize = FileUtil.getTotalSize(files);
      if (    totalBytesInDirectory != totalSize
           && totalBytesInDirectory != totalSize-1 ) { // scanner currently misreports by 1, but don't require that
         throw new IOException(Text.get(this,"e3",new Object[] { Convert.fromInt(totalBytesInDirectory), Convert.fromLong(totalSize) }));
      }
      // although file sizes are normally reported as longs rather than ints,
      // totalBytesInDirectory really is an int, because that's how it's passed
      // in the interface to the Pakon DLL.

      // now that we've checked the total bytes, we can forget about it

   // set up the source-dependent fields

      Roll roll = new Roll();

      // rollID is auto-numbered
      roll.source = Roll.SOURCE_PAKON;
      roll.email = email;

      createMakeItems(roll,files);
   }

// --- roll creation (general) ---

   /**
    * @param roll A partially-filled-in roll object.
    *             The source-dependent fields (source, email) must be filled in.
    *             The optional fields may or may not be filled in.
    *             The source-independent fields should not be filled in.
    *
    * @param files A list of files for the roll.
    *              This will be used to create a set of item objects.
    */
   public void createMakeItems(Roll roll, File[] files) throws Exception {

      FileUtil.sortByName(files);

      for (int i=0; i<files.length; i++) {
         roll.items.add(ItemUtil.makeItem(files[i]));
      }

      createHaveItems(roll,METHOD_COPY,null,null);
   }

   public static final int METHOD_COPY            = 0;
   public static final int METHOD_MOVE            = 1;
   public static final int METHOD_COPY_AND_DELETE = 2; // same result as move

   public void createHaveItems(Roll roll, int method, Document order, Frame owner) throws Exception {

      ItemUtil.disambiguate(roll.items);

   // set up the source-independent fields

      synchronized (this) { // data dir can change in other threads
         roll.rollDir = dataDir; // will change to include roll ID
      }

      roll.status = (roll.scans != null) ? Roll.STATUS_ROLL_SCANNED
                                         : Roll.STATUS_ROLL_PENDING;
      roll.hold = Roll.HOLD_ROLL_NONE; // may become email hold
      adjustHold(roll);

      // lastError starts out null

      roll.recmodDate = new Date();

   // create the record

      Object lock = table.insert(roll);
      // once this succeeds, we must either finish successfully or delete

      try {

   // copy files

         String key = Convert.fromInt(roll.rollID);
         roll.rollDir = new File(roll.rollDir,key);

         if (owner != null) { // UI thread, show wait dialog

            CopyThread copyThread = new CopyThread(roll.rollDir,roll.items,method,order);
            copyThread.start();
            StopDialog.joinSafe(copyThread,owner);
            copyThread.rethrow();

         } else { // non-UI thread, just do it

            ItemUtil.copyToNewDirectory(roll.rollDir,roll.items,method,order);
         }

         ItemUtil.replacePreitems(roll.items); // can't replace until after copy

   // finish up

         table.update(roll,lock);
         table.release(roll,lock);
         lock = null;

         Log.log(Level.INFO,this,"i7a",new Object[] { key, RollEnum.fromSource(roll.source), roll.email });

      } finally {
         if (lock != null) {
            table.delete(roll,lock);
            lock = null;
         }
      }
   }

// --- copy thread ---

   private static class CopyThread extends Thread {

      private File dir;
      private LinkedList items;
      private int method;
      private Document order;
      private Exception exception;

      public CopyThread(File dir, LinkedList items, int method, Document order) {
         super(Text.get(RollManager.class,"s5"));
         this.dir = dir;
         this.items = items;
         this.method = method;
         this.order = order;
         // exception starts out null
      }

      public void run() {
         try {
            ItemUtil.copyToNewDirectory(dir,items,method,order);
         } catch (Exception e) {
            exception = e;
         }
      }

      /**
       * Transfer exceptions back to main thread.
       */
      public void rethrow() throws Exception {
         if (exception != null) throw exception;
      }
   }

// --- roll creation (in place) ---

   /**
    * Same as createMakeItems, except that roll.rollDir is already filled in,
    * and the files already exist in that directory, and shouldn't be copied.
    */
   public void createInPlace(Roll roll, File[] files) throws Exception {

      FileUtil.sortByName(files);

      for (int i=0; i<files.length; i++) {
         roll.items.add(ItemUtil.makeItemScanned(files[i]));
      }

      createInPlaceHaveItems(roll);
   }

   public void createInPlaceHaveItems(Roll roll) throws Exception {

      // no need to disambiguate, since everything already exists in directory,
      // plus it wouldn't do anything since there are no preitems in the list.

   // set up the source-independent fields

      roll.status = (roll.scans != null) ? Roll.STATUS_ROLL_SCANNED
                                         : Roll.STATUS_ROLL_PENDING;
      roll.hold = Roll.HOLD_ROLL_NONE; // may become email hold
      adjustHold(roll);

      // lastError starts out null

      roll.recmodDate = new Date();

   // create the record

      Object lock = table.insert(roll);
      // once this succeeds, we must either finish successfully or delete

      try {

         String key = Convert.fromInt(roll.rollID);

         table.release(roll,lock);
         lock = null;

         if (roll.source == Roll.SOURCE_LOCAL) {
            KioskLog.log(this,"i9",new Object[] { roll.getLocalImageID(), key });
         } else {
            Log.log(Level.INFO,this,"i7b",new Object[] { key, RollEnum.fromSource(roll.source), roll.email });
         }

      } finally {
         if (lock != null) {
            table.delete(roll,lock);
            lock = null;
         }
      }
      // the delete block is overkill, since anything that makes release fail
      // will also make delete fail .. but keep the block for good style.
   }

// --- purge functions ---

   // lock object used by LocalImageThread to synchronize and avoid
   // the part of the purge when the file exists but the roll doesn't
   private Object purgeLock = new Object();
   public Object getPurgeLock() { return purgeLock; }

   private static final Handler defaultHandler = new DefaultHandler();
   // note, we can only use a static handler because a default handler has no state!

   /**
    * Purge a set of rolls, with user prompting and error reporting.
    */
   public void purgeMultiple(Window owner, Object[] o) {

   // confirm before purging

      String s = Text.get(this,"e7",new Object[] { new Integer(o.length), Convert.fromInt(o.length) });
      boolean confirmed = Pop.confirm(owner,s,Text.get(this,"s2"));
      if ( ! confirmed ) return;

   // ok, now purge

      boolean warn = false;

      for (int i=0; i<o.length; i++) {
         try {
            warn |= (purgeSingle(getKey(o[i]),null,null,Purge.REASON_MANUAL,(Reportable) o[i],/* verify = */ true,defaultHandler,null) == Purge.RESULT_INCOMPLETE);
            // note, we can't get RESULT_STOPPING in the UI thread, but even if we do, it's just ignored, no harm done
         } catch (Exception e) {
            // this is silent in other cases, but here we need to present the info because of the TryPurgeLocalImage call
            // wrap to include ID
            e = new Exception(Text.get(this,"e17",new Object[] { getKey(o[i]) }),e); // this is a lot like e5, by the way
            Pop.error(owner,e,Text.get(this,"s12"));
         }
      }

   // warn if purge not completely successful

      if (warn) {
         Pop.warning(owner,Text.get(this,"e8"),Text.get(this,"s3"));
      }
   }

   /**
    * Pass in both roll and lock if the object is locked.
    * Pass in key in every case.
    *
    * @param reason A value from the Purge.REASON_X enumeration.
    * @return       A value from the Purge.RESULT_X enumeration, but with RESULT_FAILED replaced by an exception.
    */
   private int purgeSingle(String key, Roll roll, Object lock, int reason, Reportable reportable, boolean verify, Handler handler, PauseCallback callback) throws Exception {
      synchronized (purgeLock) {

      boolean own = (lock == null);

      // used to be an outer try-catch block, see below
         if (own) lock = table.lock(key);
         try {

            if (own) roll = (Roll) table.get(key); // get latest copy

            if (verify && ! roll.isPurgeableStatus()) throw new Exception(Text.get(this,"e16"));

            // for local images, we need to check with the server before
            // purging, since we might want to use the image in an order
            if (roll.source == Roll.SOURCE_LOCAL) {
               Roll.Item item = (Roll.Item) roll.items.getFirst(); // by validation there's exactly one item
               Transaction t = new UploadThread.TryPurgeLocalImage(merchantConfig,tryPurgeLocalImageURL,roll,item);
               if ( ! handler.run(t,callback) ) return Purge.RESULT_STOPPING;
               // note, in the rare case that we get past this step and then fail later, it's
               // OK to call TPLI twice, it'll just reply with code 3 and let the purge occur
            }

            table.delete(key,lock);
            if (own) lock = null; // a successful delete releases the lock

            String suffix = Purge.getReasonSuffix(reason);
            if (roll.source == Roll.SOURCE_LOCAL) {
               KioskLog.log(this,"i10" + suffix,new Object[] { roll.getLocalImageID() });
            } else {
               Log.log(Level.INFO,this,"i8" + suffix,new Object[] { key });
            }

         } finally {
            if (own && lock != null) table.release(key,lock);
         }
      // used to be an outer try-catch block, see below

      // compare this to OrderManager and JobManager.  there, we catch the exception, log it,
      // and return a result code.  here, we pass it up the stack to EntityThread or the GUI.
      // so, logging would be redundant (and was, until I got rid of it).

      return FileManager.purgeFiles(roll) ? Purge.RESULT_COMPLETE : Purge.RESULT_INCOMPLETE;
      }
   }

   /**
    * @return A purge result, or null if purge not attempted.
    */
   public Integer autoPurge(Date now, Roll roll, Object lock, Handler handler, PauseCallback callback) throws Exception {

      Date purgeDate = RollUtil.getPurgeDate(roll);
      if (    purgeDate != null
           && purgeDate.before(now) ) {

         String key = Convert.fromInt(roll.rollID);
         int result = purgeSingle(key,roll,lock,Purge.REASON_AUTOMATIC,roll,/* verify = */ false,handler,callback);
         if (result == Purge.RESULT_INCOMPLETE) {
            Log.log(roll,Level.WARNING,this,"e6",new Object[] { key });
         }

         return new Integer(result);
      } else {
         return null;
      }
   }

// --- helpers ---

   public static String getKey(Object o) {
      return Convert.fromInt( ((Roll) o).rollID );
   }

   public static String[] getKeys(Object[] o) {

      String[] keys = new String[o.length];
      for (int i=0; i<o.length; i++) {
         keys[i] = getKey(o[i]);
      }

      return keys;
   }

   private static boolean isAdjustableHold(int hold) {
      return (    hold == Roll.HOLD_ROLL_NONE
               || hold == Roll.HOLD_ROLL_EMAIL
               || hold == Roll.HOLD_ROLL_DEALER );
   }

   public static void adjustHold(Roll roll) {

      // other statuses remain unchanged

      if (    (    roll.status == Roll.STATUS_ROLL_PENDING
                || roll.status == Roll.STATUS_ROLL_SENDING ) // normally we can't lock sending -- if we can, it's like pending
           && isAdjustableHold(roll.hold) ) {

         // email hold takes precedence, then dealer hold.

         // note that isWholesale can change at runtime,
         // which means that the dealer hold is not reliable.
         // because of that, and because it's tidy code,
         // the upload thread also checks dealer correctness.
         // we check here because we can catch 99.9% of them
         // and make them into nice holds rather than errors.

         try {
            boolean allowBlank = (roll.source == Roll.SOURCE_HOT_ORDER) || (roll.source == Roll.SOURCE_LOCAL);
            if (allowBlank && roll.email.length() == 0) {
               // allow blank email address, it means it's a guest account or a local-image upload
               // a non-blank address still gets validated, just as before.
            } else {
               Email.validate(roll.email);
            }

            if (Wholesale.isWholesale() && roll.dealer == null) {

               roll.hold = Roll.HOLD_ROLL_DEALER;
               roll.lastError = Text.get(RollManager.class,"e14");

            } else {

               roll.hold = Roll.HOLD_ROLL_NONE;
               roll.lastError = null;
            }

         } catch (ValidationException e) {

            roll.hold = Roll.HOLD_ROLL_EMAIL;
            roll.lastError = ChainedException.format(e);
         }
      }
   }

// --- roll creation (manual, top level) ---

   public void createManual(Frame owner, MemoryInterface mem) {

      Roll roll = startManual();

      AbstractRollDialog dialog = AbstractRollDialog.create(owner,roll,AbstractRollDialog.MODE_CREATE,mem.getManualImageDir(),mem.getAddressList());
      if (dialog == null) return; // just as if canceled

      boolean ok = dialog.run();

      // save directory and addresses even if roll was canceled
      mem.setBoth(dialog.getCurrentDir(),dialog.getAddressList());

      if (ok) {
         try {
            finishManual(roll,owner);
         } catch (Exception e) {
            Pop.error(owner,e,Text.get(this,"s7"));
         }
      }
   }

// --- view function ---

   public interface MemoryInterface {

      File getManualImageDir();
      void setManualImageDir(File dir);

      LinkedList getAddressList();
      void       setAddressList(LinkedList list);

      void setBoth(File dir, LinkedList list);
   }

   public void open(Window owner, Roll roll, MemoryInterface mem) {
      String key = getKey(roll);

      try {
         Object lock = table.lockTry(key);
         try {

            boolean readonly;

            if (lock != null) {
               roll = (Roll) table.get(key); // get latest copy

               // note, normally we can't lock sending -- if we can, it's like pending.
               boolean okStatus = ! roll.isPurgeableStatus();

               boolean okScans = (roll.scans == null); // no edit if still copying from scanner
               // here we could equally well test the status,
               // but that doesn't work in the recover function, and I want them to be the same.

               boolean okUpload = (roll.upload == null); // no edit once upload has started
                                                         // this dominates the status check, but do both anyway
               // note, roll.upload stays null for local images, but the next line covers that

               boolean okSource = (roll.source != Roll.SOURCE_HOT_ORDER) && (roll.source != Roll.SOURCE_LOCAL);

               readonly = ! (okStatus && okScans && okUpload && okSource);

            } else {
               readonly = true;
               // if we can't lock the roll, show the cached copy in read-only mode
            }

            int mode = readonly ? AbstractRollDialog.MODE_VIEW
                                : AbstractRollDialog.MODE_EDIT;

            LinkedList itemsBefore = CopyUtil.copyList(roll.items);

            AbstractRollDialog dialog = AbstractRollDialog.create(owner,roll,mode,mem.getManualImageDir(),mem.getAddressList());
            if (dialog == null) return; // just as if canceled

            boolean ok = dialog.run();

            // save directory and addresses even if roll was canceled
            mem.setBoth(dialog.getCurrentDir(),dialog.getAddressList());

            if (ok) { // won't happen in readonly mode

               ItemUtil.disambiguate(roll.items);
               // subtle point: the items are not disambiguated with respect to itemsBefore.
               // the code only works in that case because the delete comes before the copy.

               adjustHold(roll);
               roll.recmodDate = new Date();

               LinkedList itemsAfter = CopyUtil.copyList(roll.items);
               // must copy because the alter thread will modify it

               AlterThread alterThread = new AlterThread(roll.rollDir,itemsBefore,itemsAfter);
               alterThread.start();
               StopDialog.joinSafe(alterThread,owner);
               alterThread.rethrow();

               ItemUtil.replacePreitems(roll.items);

               table.update(roll,lock);

               Log.log(Level.INFO,this,"i6",new Object[] { key });
            }

         } finally {
            if (lock != null) table.release(key,lock);
         }
      } catch (Exception e) {
         Pop.error(owner,e,Text.get(this,"s1"));
      }
   }

// --- alter thread ---

   // directory structure -- see also Axon.java
   private static final String TEMP_DIR = "temp";

   private static class AlterThread extends Thread {

      private File dir;
      private LinkedList itemsBefore;
      private LinkedList itemsAfter;
      private Exception exception;

      public AlterThread(File dir, LinkedList itemsBefore, LinkedList itemsAfter) {
         super(Text.get(RollManager.class,"s6"));
         this.dir = dir;
         this.itemsBefore = itemsBefore;
         this.itemsAfter = itemsAfter;
         // exception starts out null
      }

      public void run() {
         try {

            ItemUtil.removeMatchingItems(itemsBefore,itemsAfter);

            // the original list contained only items,
            // the dialog only adds preitems,
            // and we've removed all matching items,
            // so, now, itemsBefore is all items,
            // and itemsAfter is all preitems

            if (itemsBefore.isEmpty() && itemsAfter.isEmpty()) return;

            String[] removeFiles = ItemUtil.itemsToStrings(itemsBefore);
            LinkedList addItems = itemsAfter; // just for clarity

            ItemUtil.alter(dir,new File(dir,TEMP_DIR),removeFiles,addItems);

         } catch (Exception e) {
            exception = e;
         }
      }

      /**
       * Transfer exceptions back to main thread.
       */
      public void rethrow() throws Exception {
         if (exception != null) throw exception;
      }
   }

// --- rebuild function ---

   /**
    * Rebuild a set of rolls, with user confirmation.
    */
   public void rebuild(Window owner, String[] keys) {

   // confirm

      String s = Text.get(this,"e9",new Object[] { new Integer(keys.length), Convert.fromInt(keys.length) });
      boolean confirmed = Pop.confirm(owner,s,Text.get(this,"s4"));
      if ( ! confirmed ) return;

   // rebuild

      for (int i=0; i<keys.length; i++) {
         rebuild(keys[i]);
      }
   }

   public void rebuild(String key) {
      try {
         Object lock = table.lock(key);
         try {
            Roll roll = (Roll) table.get(key); // get latest copy

            // check scans field instead of status,
            // since the user can delete a scan before it copies.
            // for normal rolls,
            // we could require that an upload record be present,
            // but it doesn't really matter,
            // since we're just going to clear it anyway.

            if (roll.scans == null) { // roughly, roll.status >= Roll.STATUS_ROLL_PENDING

               boolean clearImageID = (roll.source != Roll.SOURCE_LOCAL);
               // for local ones we know the ID in advance, don't clear

               roll.upload = null;
               roll.status = Roll.STATUS_ROLL_PENDING;
               roll.hold = Roll.HOLD_ROLL_USER;

               ListIterator li = roll.items.listIterator();
               while (li.hasNext()) {
                  Roll.Item item = (Roll.Item) li.next();
                  item.status = Roll.STATUS_FILE_PENDING;
                  if (clearImageID) item.imageID = null;
                  // this is the only place the status can roll back,
                  // so this is the only place we clear the imageID.
                  // logically the image ID is part of the upload record.
               }

               roll.recmodDate = new Date();
               table.update(roll,lock);

               Log.log(Level.INFO,this,"i5",new Object[] { key });
            }
         } finally {
            table.release(key,lock);
         }
      } catch (Exception e) {
         // ignore, user will see result
      }
   }

// --- single-roll functions ---

   /**
    * @return If the hold failed because of a lock, the thread that owns (or owned) the lock.
    */
   public Thread hold(String key) {
      try {
         Object lock = table.lock(key);
         try {
            Roll roll = (Roll) table.get(key); // get latest copy

            // we want to allow converting email holds into user holds,
            // so that you can correct an email address without uploading.
            // allow the same for error holds, just for uniformity.
            // note, normally we can't lock sending -- if we can, it's like pending.

            if (    ! roll.isPurgeableStatus()
                 && !(roll.hold == Roll.HOLD_ROLL_USER) ) {

               roll.hold = Roll.HOLD_ROLL_USER;

               roll.recmodDate = new Date();
               table.update(roll,lock);

               Log.log(Level.INFO,this,"i1",new Object[] { key });
            }
         } finally {
            table.release(key,lock);
         }
      } catch (LockException e) {
         return e.getLockThread();
      } catch (Exception e) {
         // ignore, user will see result
      }
      return null;
   }

   public void release(String key) {
      try {
         Object lock = table.lock(key);
         try {
            Roll roll = (Roll) table.get(key); // get latest copy

            if ( ! isAdjustableHold(roll.hold) ) { // not held, or hold depends on data and can't just be released

               roll.hold = Roll.HOLD_ROLL_NONE; // may convert to HOLD_ROLL_EMAIL below
               roll.lastError = null;

               adjustHold(roll);
               roll.recmodDate = new Date();
               table.update(roll,lock);

               Log.log(Level.INFO,this,"i2",new Object[] { key });
            }
         } finally {
            table.release(key,lock);
         }
      } catch (Exception e) {
         // ignore, user will see result
      }
   }

   public void delete(String key) {
      try {
         Object lock = table.lock(key);
         try {
            Roll roll = (Roll) table.get(key); // get latest copy

            // this is just a list of all the statuses that appear in the open table,
            // but it acts as a nice check that we aren't deleting a non-open roll.
            // note, normally we can't lock sending -- if we can, it's like pending.

            if ( ! roll.isPurgeableStatus() ) {

               roll.status = Roll.STATUS_ROLL_DELETED;
               roll.hold = Roll.HOLD_ROLL_NONE;

               roll.recmodDate = new Date();
               table.update(roll,lock);

               if (roll.source == Roll.SOURCE_LOCAL) {
                  KioskLog.log(this,"i11",new Object[] { roll.getLocalImageID() });
               } else {
                  Log.log(Level.INFO,this,"i3",new Object[] { key });
               }
            }
         } finally {
            table.release(key,lock);
         }
      } catch (Exception e) {
         // ignore, user will see result
      }
   }

   public void recover(String key) {
      try {
         Object lock = table.lock(key);
         try {
            Roll roll = (Roll) table.get(key); // get latest copy

            if (roll.status == Roll.STATUS_ROLL_DELETED) {

               // recover into scanned status if there are still scans
               roll.status = (roll.scans != null) ? Roll.STATUS_ROLL_SCANNED
                                                  : Roll.STATUS_ROLL_PENDING;
               roll.hold = Roll.HOLD_ROLL_USER;

               roll.recmodDate = new Date();
               table.update(roll,lock);

               Log.log(Level.INFO,this,"i4",new Object[] { key });
            }
         } finally {
            table.release(key,lock);
         }
      } catch (Exception e) {
         // ignore, user will see result
      }
   }

// --- UI commands ---

   public static void pleaseSelect(Window owner, boolean multi) {
      Pop.info(owner,Text.get(RollManager.class,multi ? "e15a" : "e15b"),Text.get(RollManager.class,"s11"));
   }

   public void doNew(Frame owner, MemoryInterface mem) {
      createManual(owner,mem);
   }

   public void doOpen(Window owner, Object o, MemoryInterface mem) {
      open(owner,(Roll) o,mem);
   }

   public void doHold(Frame owner, Object[] o, Subsystem uploadSubsystem) {
      if (o.length == 0) { pleaseSelect(owner,true); return; }

      int saveCount = 0;
      String saveKey = null;

      String[] keys = getKeys(o);
      for (int i=0; i<keys.length; i++) {
         Thread lockThread = hold(keys[i]);
         if (lockThread instanceof UploadThread) { // BTW null is not an instance of anything
            saveCount++;
            saveKey = keys[i];
         }
      }

      if (saveCount == 1) { // if greater, something weird is going on, better not get involved
         if (Pop.confirm(owner,Text.get(this,"e19",new Object[] { saveKey }),Text.get(this,"s13"))) {
            boolean wasRunning = uploadSubsystem.stop(owner); // true, else why is there a lock?
            hold(saveKey);
            if (wasRunning) uploadSubsystem.start();
            // cf. the Subsystem reinit function and the stop and start buttons in the GUI
         }
      }
      // see also comments in OrderManager.doHold
   }

   public void doRelease(Window owner, Object[] o) {
      if (o.length == 0) { pleaseSelect(owner,true); return; }
      String[] keys = getKeys(o);
      for (int i=0; i<keys.length; i++) {
         release(keys[i]);
      }
   }

   public void doDelete(Window owner, Object[] o) {
      if (o.length == 0) { pleaseSelect(owner,true); return; }

      // check for kiosk images
      boolean kiosk = false;
      for (int i=0; i<o.length; i++) {
         Roll roll = (Roll) o[i];
         if (roll.source == Roll.SOURCE_LOCAL) { kiosk = true; break; }
      }

      // confirm before deleting
      String s = Text.get(this,"e11",new Object[] { new Integer(o.length), Convert.fromInt(o.length) });
      if (kiosk) s += "\n\n" + Text.get(this,"e18");
      boolean confirmed = Pop.confirm(owner,s,Text.get(this,"s8"));
      if ( ! confirmed ) return;

      String[] keys = getKeys(o);
      for (int i=0; i<keys.length; i++) {
         delete(keys[i]);
      }
   }

   public void doRecover(Window owner, Object[] o) {
      if (o.length == 0) { pleaseSelect(owner,true); return; }

      // prevent ineffective recover commands
      for (int i=0; i<o.length; i++) {
         Roll roll = (Roll) o[i];
         if (roll.status != Roll.STATUS_ROLL_DELETED) {
            Pop.error(owner,Text.get(this,"e12"),Text.get(this,"s9"));
            return;
         }
      }

      String[] keys = getKeys(o);
      for (int i=0; i<keys.length; i++) {
         recover(keys[i]);
      }
   }

   public void doRebuild(Window owner, Object[] o) {
      if (o.length == 0) { pleaseSelect(owner,true); return; }

      // allow ineffective rebuild commands, they are harmless

      rebuild(owner,getKeys(o));
   }

   public void doPurge(Window owner, Object[] o) {
      if (o.length == 0) { pleaseSelect(owner,true); return; }

      // prevent ineffective purge commands
      for (int i=0; i<o.length; i++) {
         Roll roll = (Roll) o[i];
         if ( ! roll.isPurgeableStatus() ) {
            Pop.error(owner,Text.get(this,"e13"),Text.get(this,"s10"));
            return;
         }
      }

      purgeMultiple(owner,o);
   }

}

