/*
 * Channel.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.*;

/**
 * A relic from before we had mapping objects.  It used to be that
 * everything except the SKU code was packed into a channel string.
 */

public class Channel extends Structure {

// --- fields ---

   public String channel;
   public String sku;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      Channel.class,
      // no version
      new AbstractField[] {

         new StringField("channel","Channel"),
         new StringField("sku","SKU")
      });

   protected StructureDefinition sd() { return sd; }

// --- validation ---

   public void validate() throws ValidationException {

      // channel objects are only created as temporaries by MappingUtil.migrate,
      // they never stick around until validation occurs.
   }

}

