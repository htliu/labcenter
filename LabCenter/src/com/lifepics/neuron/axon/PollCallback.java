/*
 * PollCallback.java
 */

package com.lifepics.neuron.axon;

import java.io.File;

import org.w3c.dom.Document;

/**
 * An interface that a {@link Poller} can use to do things.
 */

public interface PollCallback {

   /**
    * Submit a roll.  The source and email address must be filled in,
    * along with any optional fields you want.
    *
    * @return The roll ID.  This is just a convenience, it's also in roll.rollID.
    */
   int submit(Roll roll, File[] files) throws Exception;

   /**
    * Submit a roll.  Same as regular submit, except that
    * the item list must be filled in too (with preitems).
    */
   int submitWithItems(Roll roll, int method, Document order) throws Exception;

   /**
    * Submit a roll in place.  Same as regular submit, except that
    * roll.rollDir must be filled in, and the files are not copied.
    */
   int submitInPlace(Roll roll, File[] files) throws Exception;

   /**
    * Wait until the number of rolls drops to a suitable level.
    * This can throw an exception if the thread stops,
    * so don't call it when you're in a transition state.
    * Usually you want to call it right before submit.
    */
   void throttle() throws Exception;

   /**
    * Get dealer information for a roll.  If in wholesale mode,
    * we look up the dealer record, otherwise we just validate
    * the location ID and return null.
    */
   Dealer getDealerForLocation(String locationID) throws Exception;

   /**
    * Check whether we should ignore a file (because it's not an image).
    */
   boolean ignore(String filename);

}

