/*
 * DefaultHandler.java
 */

package com.lifepics.neuron.net;

/**
 * A default handler that just runs transactions directly.  All chains end in one of these.
 */

public class DefaultHandler extends Handler {

   /**
    * Run a transaction.
    */
   public boolean run(Transaction t, PauseCallback callback) throws Exception {
      return t.run(callback);
   }

}

