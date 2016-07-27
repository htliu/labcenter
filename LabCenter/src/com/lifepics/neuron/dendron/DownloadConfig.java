/*
 * DownloadConfig.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.net.BandwidthConfig;
import com.lifepics.neuron.object.CopyUtil;
import com.lifepics.neuron.struct.*;

/**
 * An object that holds configuration information for the download subsystem.
 */

public class DownloadConfig extends Structure {

// --- fields ---

   public String listURL;
   public String orderURL;
   public String opieURL;
   public boolean opieFlag;
   public String statusURL;

   /**
    * The (short) polling interval for local orders that can be tried, in milliseconds.
    */
   public long idlePollInterval; // millis

   /**
    * The (long) polling interval for new orders on the remote server, in milliseconds.
    */
   public long listPollInterval; // millis

   public boolean startOverEnabled;

   /**
    * When an order isn't downloading, purge and start over after this interval.
    */
   public long startOverInterval; // millis;

   /**
    * When this flag is set, higher-priority orders download first
    * (with priority determined by the product due intervals).
    * This requires not only picking the highest-priority order from
    * the list, but also getting the list after every download
    * and interrupting every download after we have the order XML.
    */
   public boolean prioritizeEnabled;

   /**
    * When this flag is set, we back-convert from new to old SKU codes in
    * the invoice file when possible.  This is for the very few dealers
    * that ring up orders by hand-entering codes off the existing invoice.
    */
   public boolean conversionEnabled;

   public BandwidthConfig bandwidthConfig;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      DownloadConfig.class,
      0,7,
      new AbstractField[] {

         new StringField("listURL","ListURL"),
         new StringField("orderURL","OrderURL"),
         new StringField("opieURL","OpieURL",5,"https://opie.lifepics.com/OPService.asmx/GetOrderLC"),
         new BooleanField("opieFlag","OpieFlag",6,true),
         new StringField("statusURL","StatusURL",7,"https://services.lifepics.com/net/Orders/UpdateOrderStatus.aspx"),
         new LongField("idlePollInterval","IdlePollInterval-Millis"),
         new LongField("listPollInterval","ListPollInterval-Millis"),
         new BooleanField("startOverEnabled","StartOverEnabled",1,true),
         new LongField("startOverInterval","StartOverInterval-Millis",1,10800000), // 3 hours
         new BooleanField("prioritizeEnabled","PrioritizeEnabled",2,true),
         new BooleanField("conversionEnabled","ConversionEnabled",3,false),

         new StructureField("bandwidthConfig","BandwidthConfig",BandwidthConfig.sd,4)
      });

   protected StructureDefinition sd() { return sd; }

// --- copy function ---

   public DownloadConfig copy() { return (DownloadConfig) CopyUtil.copy(this); }

// --- validation ---

   public void validate() throws ValidationException {

      if (idlePollInterval < 1) throw new ValidationException(Text.get(this,"e1"));
      if (listPollInterval < 1) throw new ValidationException(Text.get(this,"e2"));
      if (startOverInterval < 1) throw new ValidationException(Text.get(this,"e3"));

      bandwidthConfig.validate();
   }

}

