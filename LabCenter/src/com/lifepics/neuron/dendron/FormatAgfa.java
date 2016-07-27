/*
 * FormatAgfa.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.misc.ZipUtil;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Implementation of the Agfa format.
 */

public class FormatAgfa extends Format {

// --- subclass hooks ---

   public String getShortName() { return Text.get(this,"s1"); }

   public int[] getAllowedCompletionModes() { return new int[] { COMPLETION_MODE_MANUAL }; }
   public int   getCompletionMode(Object formatConfig) { return COMPLETION_MODE_MANUAL; }
   public void  setCompletionMode(Object formatConfig, int mode) {}

// --- format function ---

   public void format(Job job, Order order, Object formatConfig) throws Exception {

      // we don't directly load any DLLs here, but there are DLLs involved,
      // and any UnsatisfiedLinkError would have to come through here.
      // it's unlikely, because if there's no ConnectionKit.dll there's probably
      // also no Agfa.jar, and that would produce a NoClassDefFoundError first;
      // but it's possible, so we ought to handle it.

      try {
         formatImpl(job,order,formatConfig);
      } catch (UnsatisfiedLinkError e) {
         throw new Exception(Text.get(this,"e3")); // no need for more detail
      } catch (NoClassDefFoundError e) {
         throw new Exception(Text.get(this,"e4"));
      }
   }

   private void formatImpl(Job job, Order order, Object formatConfig) throws Exception {

      // this one is unusual because there are no file operations involved.
      // the Agfa communication could be made into a transactable operation,
      // but there's no reason to, it's perfectly clear this way.

      AgfaConfig config = (AgfaConfig) formatConfig;
      // because of validation, the cast is guaranteed to work

      require(config.databaseDir);

      // if empty directory, auto-install a clean Agfa database
      String[] list = config.databaseDir.list();
      if (list != null && list.length == 0) ZipUtil.extractAllFromResource(config.databaseDir,this,"AgfaDatabase.jar");

      // check for missing mappings before bothering the server
      HashSet skus = new HashSet();
      Iterator i = job.refs.iterator();
      while (i.hasNext()) {
         Job.Ref ref = (Job.Ref) i.next();
         if (skus.add(ref.sku) && ! MappingUtil.existsMapping(config.mappings,ref.sku)) missingChannel(ref.sku,order.getFullID());
      }

      boolean started = false;
      boolean batched = false;
      int batch = 0; // for compiler

      try {

      // setup

         // have to check the connection explicitly ...
         // startCK will complete even if the server isn't available,
         // then createBatch will lock up until it becomes so,
         // which can take arbitrarily long.  this check isn't perfect,
         // but it's a great improvement.
         //
         if ( ! Agfa.checkConnection(config.host,config.service,5) ) throw new Exception(Text.get(this,"e1"));

         // interesting technical note:  the serialize subdirectory
         // contains saved batch and image number pool information.
         // if the server changes, that information should be erased.
         // fortunately, the documentation claims the connection kit
         // will do that by itself ... and there's evidence for that,
         // which is that the serialize file contains the host and service.
         //
         // the one case that doesn't handle is a reinstalled server.
         // in that case you must erase the files manually (they say),
         // or (I say) figure out how to start the server with the
         // right batch number, to avoid erasing on however-many devices.

         Agfa.setPathOptions(path(config,Agfa.SUBDIR_DATA_PATH),
                             path(config,Agfa.SUBDIR_DELETE_PATH),
                             path(config,Agfa.SUBDIR_PICTURE_PATH),
                             path(config,Agfa.SUBDIR_PROT_PATH),
                             path(config,Agfa.SUBDIR_RECV_PATH),
                             path(config,Agfa.SUBDIR_SEND_PATH),
                             path(config,Agfa.SUBDIR_SERIALIZE_PATH),
                             path(config,Agfa.SUBDIR_VERSION_PATH));

         Agfa.setConnectionOptions(config.host,config.service,config.batchNumberPoolsize,config.imageNumberPoolsize);

         Agfa.setClientOptions(config.sourceDeviceID,
                               Agfa.DEVICE_TYPE_CONNECTION_KIT,
                               "LifePics LabCenter",
                               config.operatorNumber,
                               config.operatorName,
                               config.dealerNumber,
                               config.dealerName,
                               true); // do delete local copy after send

         started = true; // supposed to call stop even if start fails
         Agfa.startCK();

      // start batch

         batch = Agfa.createBatch(Agfa.FILM_SIZE_MIXED,
                                  Agfa.FILM_CATEGORY_DIGITAL_IMAGE,
                                  Agfa.PHOTO_FINISHING_MODE_FIRST_RUN);
         batched = true;

         Agfa.setBatchAttyInt(batch,Agfa.ATTY_SourceDeviceType,
                                    Agfa.DEVICE_TYPE_CONNECTION_KIT);
         // this goes somewhere different than the one in setClientOptions

         Agfa.setOrderAttyStr(batch,Agfa.ATTY_OrderIDClient,
                                    config.refnumPrefix + Convert.fromInt(order.orderID));
         // named refnumPrefix because the real PfDF attribute is Order Reference #1

      // add images

         LinkedList images = new LinkedList(); // list of non-null Integer

         Backprint.Sequence sequence = new Backprint.Sequence();
         boolean hasSequence = Backprint.hasSequence(config.backprints);

         i = job.refs.iterator();
         while (i.hasNext()) {
            Job.Ref ref = (Job.Ref) i.next();
            addImages(batch,images,job,ref,order,config,sequence,hasSequence);
         }

      // finish batch

         Agfa.saveBatch(batch);

      // send

         i = images.iterator();
         while (i.hasNext()) {
            Agfa.sendImage(((Integer) i.next()).intValue());
         }

         Agfa.sendBatch(batch);
         batched = false;

      // done

         started = false; // don't try to stop twice
         Agfa.stopCK();

      } catch (Exception e) {

         if (batched) {
            try {
               Agfa.destroyBatch(batch);
            } catch (Exception e2) {
               // ignore
            }
         }
         // every batch should be either sent or destroyed, or there will be junk left over.
         //
         // interesting experimental fact, when you destroy a saved batch, the connection kit
         // allocates the next batch number and sends a fake batch to the server that refers
         // to all the images in the destroyed batch ... that's how the images are cleaned up
         // on the server side, if any were sent.
         //
         // so, if you destroy a batch while the network is down, the server may never get
         // cleaned up.  not much I can do about that ... best would be to check connectivity
         // before destroying the batch, and if it's not working, save the batch for later.
         // but then we'd have to check later, and who knows, maybe we'd be connected to a
         // different server by then.  better to keep the local database all clean, I think.

         if (started) {
            try {
               Agfa.stopCK();
            } catch (Exception e2) {
               // ignore
            }
         }

         throw e;
      }
   }

   private void addImages(int batch, LinkedList images, Job job, Job.Ref ref, Order order, AgfaConfig config, Backprint.Sequence sequence, boolean hasSequence) throws Exception {

      // in theory the PfDF format supports multiple jobs on a single image,
      // but in practice it doesn't right now.  so, no need to sort the refs
      // into groups by filename, just generate one image object per ref ...
      // or, if there's a backprint sequence, one image object per quantity (ugh).

      Order.Item item = order.getItem(ref);

      AgfaMapping m = (AgfaMapping) MappingUtil.getMapping(config.mappings,ref.sku);

      Iterator k = new ChunkIterator(job.getOutputQuantity(item,ref),Integer.MAX_VALUE,Integer.MAX_VALUE,hasSequence);
      while (k.hasNext()) {
         int quantity = ((Integer) k.next()).intValue();

         int image = Agfa.addImage(batch,Convert.fromFile(order.getPath(item.filename)),
                                         "", // no thumbnail
                                         "", // no share
                                         Agfa.SCHEME_PDM,
                                         Agfa.INPUT_MEDIA_NETWORK,
                                         false); // don't delete the original

         int imjob = Agfa.addImageJob(image,Agfa.WORK_TYPE_SERVICE_PRINT,
                                            m.surface,
                                            m.printWidth,
                                            quantity);

         if (m.printLength != null) {
            Agfa.setJobAttyInt(imjob,Agfa.ATTY_PrintLength,
                                     m.printLength.intValue());
         }

         if (m.rotation != null) {
            Agfa.setJobAttyInt(imjob,Agfa.ATTY_RotationInstructions,
                                     m.rotation.intValue());
         }

         if (m.imageFill != null) {
            Agfa.setJobAttyInt(imjob,Agfa.AGFA_ATTY_ImageFillFlag,
                                     m.imageFill.intValue());
         }

         if (m.borderWidth != null) {
            Agfa.setJobAttyInt(imjob,Agfa.AGFA_ATTY_WhiteBorderWidth,
                                     m.borderWidth.intValue());
         }

         if (config.targetDeviceID != null) {
            Agfa.setJobAttyInt(imjob,Agfa.ATTY_TargetDeviceID,
                                     config.targetDeviceID.intValue());
         }

         String s = Backprint.generate(config.backprints,"\r",order,item,sequence);
         if (s != null) {
            Agfa.setJobAttyStr(imjob,Agfa.ATTY_BackprintText,s);
         }

         images.add(new Integer(image));
         sequence.advance(item);
      }
   }

// --- utilities ---

   private static String path(AgfaConfig config, String subdir) {
      return Convert.fromFile(new File(config.databaseDir,subdir));
   }

}

