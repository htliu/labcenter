/*
 * Handler.java
 */

package com.lifepics.neuron.net;

/**
 * An abstract class that represents things capable of running a transaction.
 * This provides a way to add a wrapper layer uniformly around all different
 * kinds of transaction.
 */

public abstract class Handler {

   /**
    * Run a transaction.
    *
    * @return True if the transaction was performed successfully.
    *         False if the thread is stopping.
    *         If there's an error, an exception should be thrown.
    */
   public abstract boolean run(Transaction t, PauseCallback callback) throws Exception;

}

