/*
 * AutoRefresh.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.axon.DealerTransaction;
import com.lifepics.neuron.axon.PriceListTransaction;
import com.lifepics.neuron.axon.ProMode;
import com.lifepics.neuron.core.Log;
import com.lifepics.neuron.dendron.Conversion;
import com.lifepics.neuron.dendron.Mapping;
import com.lifepics.neuron.dendron.MappingConfig;
import com.lifepics.neuron.dendron.MappingUtil;
import com.lifepics.neuron.dendron.PatternUtil;
import com.lifepics.neuron.dendron.ProductCallback;
import com.lifepics.neuron.dendron.ProductConfig;
import com.lifepics.neuron.dendron.Queue;
import com.lifepics.neuron.dendron.QueueMapping;
import com.lifepics.neuron.dendron.SKUComparator;
import com.lifepics.neuron.meta.SortUtil;
import com.lifepics.neuron.net.MerchantConfig;
import com.lifepics.neuron.object.CopyUtil;
import com.lifepics.neuron.struct.SKU;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Level;

/**
 * Static code for the auto-refresh function, which does the same thing
 * as the various Refresh buttons in ConfigDialog and ConfigDialogPro,
 * only automatically of course.  This is invoked from inside AutoConfig.
 */

public class AutoRefresh {

// --- main function ---

   /**
    * @param config A modifiable config object.
    */
   public static void refresh(Config config) {

      if (ProMode.isProOld(config.proMode)) {

         config.priceLists = PriceListTransaction.refresh(null,config.priceListURL,config.merchantConfig,config.priceLists);

      } else if (ProMode.isProNew(config.proMode)) {

         // refresh nothing

      } else { // normal LC

         Refresh.refresh(new DirectRefreshInterface(config)); // product refresh

         if ( ! config.merchantConfig.isWholesale ) {
            StoreHoursTransaction.refresh(null,config.storeHoursURL,config.merchantConfig,config.storeHours);
         }

         if (config.merchantConfig.isWholesale) {
            config.dealers = DealerTransaction.refresh(null,config.dealerURL,config.merchantConfig,config.dealers);
         }
      }
   }

// --- refresh interface ---

   private static class DirectRefreshInterface implements Refresh.RefreshInterface {

      private Config config;
      public DirectRefreshInterface(Config config) { this.config = config; }

      public void line(String s) {
         Log.log(Level.INFO,AutoRefresh.class,"i1",new Object[] { s });
      }

      public void blank() {}            // UI only, ignore here
      public void result(String key) {} // UI only, ignore here

      public MerchantConfig getMerchantConfig() { return config.merchantConfig; }
      public String getOldProductURL() { return config.skuRefreshURL; }
      public String getNewProductURL() { return config.newProductURL; }
      public String getConversionURL() { return config.conversionURL; }

      public ProductConfig getProductConfig() { return config.productConfig; }
      public void setProductConfig(ProductConfig pc) { config.productConfig = pc; }

      public Collection recomputeSKUs(ProductConfig pc) {
         final HashSet skus = new HashSet();

         pc.iterate(new ProductCallback() { public void f(SKU sku, String description, Long dueInterval) {
            skus.add(sku);
         } });

         Conversion.addAll(pc.conversions,skus);
         // this is less important that the corresponding call in EditSKUDialog,
         // but it's still marginally useful since it affects the default queue
         // conversion step.  same goes for the ESD call, too.

         PatternUtil.gather(config.queueList,skus);

         return skus; // UI needs sorted, but refresh doesn't
      }

      public void transfer(SKU skuNew, SKU skuOld) {
         transferImpl(skuNew,skuOld,config.queueList.mappings);

         Iterator i = config.queueList.queues.iterator();
         while (i.hasNext()) {
            Queue q = (Queue) i.next();
            if ( ! (q.formatConfig instanceof MappingConfig) ) continue;

            transferImpl(skuNew,skuOld,((MappingConfig) q.formatConfig).getMappings());
            // we don't call putMappings, we just write directly into the list
         }

         if (skuOld.equals(config.printConfig.jpegSKU)) config.printConfig.jpegSKU = skuNew;
         // reverse order because skuOld is definitely not null

         LinkedList dueSkus = config.userConfig.skus;
         if (dueSkus.contains(skuOld) && ! dueSkus.contains(skuNew)) {
            SortUtil.addInSortedOrder(dueSkus,skuNew,SKUComparator.displayOrder);
         }
         // the "already contains skuNew" case is kind of academic, but it could happen.
      }

      public String getSingleQueue() {
         LinkedList queues = config.queueList.queues;
         return (queues.size() == 1) ? ((Queue) queues.getFirst()).queueID : null;
      }

      public void setIfNotSet(SKU sku, String queueID) {
         LinkedList mappings = config.queueList.mappings;
         QueueMapping m = (QueueMapping) MappingUtil.getMapping(mappings,sku);
         if (m == null) {
            m = new QueueMapping();
            m.psku = sku;
            m.queueID = queueID;
            SortUtil.addInSortedOrder(mappings,m,MappingUtil.skuComparator);
         }
      }

      public String getDefaultQueue() { return config.queueList.defaultQueue; }
      public void clearDefaultQueue() { config.queueList.defaultQueue = null; }

      public void clean  () {} // UI only, ignore here
      public void showAll() {} // UI only, ignore here
   }

// --- helpers ---

   private static void transferImpl(SKU skuNew, SKU skuOld, LinkedList mappings) {

      Mapping mNew = (Mapping) MappingUtil.getMapping(mappings,skuNew);
      if (mNew != null) return;
      // is there a mapping or pattern that handles the new SKU?

      Mapping mOld = (Mapping) MappingUtil.getExactMapping(mappings,skuOld);
      if (mOld == null) return;
      // is there a specific individual mapping for the old SKU?

      // use getExactMapping in the second case so that if you rename a
      // book product with a general rule, you don't get a ton of
      // specific-SKU copies of the rule.  ideally we'd copy and modify
      // the pattern, but that'll have to wait for the future.
      // we can't use getExactMapping in the first case since then we'd
      // create specific mappings on top of a rule.

      mNew = (Mapping) CopyUtil.copy(mOld);
      mNew.setPSKU(skuNew);
      // I'm not super-excited about the copy and the SKU change being separate steps,
      // but this way has less code duplication.  if we had a mapping superclass that
      // held the SKU variable, we could push the two steps into there.
      SortUtil.addInSortedOrder(mappings,mNew,MappingUtil.skuComparator);
   }

}

