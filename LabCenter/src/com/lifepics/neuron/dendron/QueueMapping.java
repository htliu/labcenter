/*
 * QueueMapping.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.*;

/**
 * An object that maps a SKU to a queue.
 */

public class QueueMapping extends Structure implements Mapping {

// --- fields ---

   public PSKU getPSKU() { return psku; }
   public void setPSKU(PSKU psku) { this.psku = psku; }

   public PSKU psku;
   public String queueID;
   public String backupQueueID;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      QueueMapping.class,
      // no version
      new AbstractField[] {

         new PSKUField("psku","A","SKU","Rule"),
         new StringField("queueID","QueueID"),
         new NullableStringField("backupQueueID","BackupQueueID")
      });

   static { sd.setAttributed(); }
   protected StructureDefinition sd() { return sd; }

// --- validation ---

   public void validate() throws ValidationException {

      if (queueID.length() == 0) throw new ValidationException(Text.get(this,"e3"));
      if (backupQueueID != null && backupQueueID.length() == 0) throw new ValidationException(Text.get(this,"e4"));
      // could check that they're not the same queue, but it's harmless stupidity
   }

// --- migration ---

   public void migrate(Channel c) throws ValidationException {

      psku = new OldSKU(c.sku);
      queueID = c.channel;
      // backupQueueID stays null
   }

}

