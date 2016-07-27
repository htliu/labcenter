/*
 * EditSKUData.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.dendron.MappingConfig;
import com.lifepics.neuron.dendron.MappingUtil;
import com.lifepics.neuron.dendron.ProductConfig;
import com.lifepics.neuron.struct.SKU;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * A data record that contains exactly what {@link EditSKUDialog} needs,
 * and that can validate itself.  It's similar to, but slightly smaller
 * than, a {@link QueueList}.<p>
 *
 * It needs to be a separate record so that invalid data poked into the
 * main queue list by a failed OK doesn't stop the subscreen from OKing
 * (and so that the subscreen doesn't poke invalid data itself).
 */

public class EditSKUData {

// --- fields ---

   public ProductConfig productConfig;

   // same as in QueueList
   public LinkedList mappings;
   public String defaultQueue;

   // subset of queues
   public LinkedList mappingQueues;

   // fields for SKU transfer only
   public SKU jpegSKU;
   public LinkedList dueSkus;

// --- queues ---

   public static class MappingQueue {

      // read-only fields
      public int format;
      public String queueID;
      public String name;
      public MappingConfig mappingConfig; // see diagram below
      public Object adapter;              //

      public LinkedList mappings;
   }

   // real config object     <--->     EditSKUData  <--->  EditSKUDialog hash maps
   //                    mappingConfig             adapter

// --- methods ---

   public void validate() throws ValidationException {

      // this is a strange subset of QueueList validation.
      // I'm not sure what cases the first two calls
      // were meant to catch.  the loop is important, though,
      // because that's where column data validation happens.

      productConfig.validate();

      MappingUtil.validate(mappings);

      Iterator i = mappingQueues.iterator();
      while (i.hasNext()) {
         MappingQueue mq = (MappingQueue) i.next();

         MappingUtil.validate(mq.mappings);
      }
   }

}

