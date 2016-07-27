/*
 * Transaction.java
 */

package com.lifepics.neuron.net;

/**
 * A very abstract class that represents a transaction that a handler can perform.
 * In practice, all transactions are {@link HTTPTransaction}s; I'm distinguishing
 * the two so that it's clear what level of abstraction a handler operates at.
 */

public abstract class Transaction {

   /**
    * Get a string that describes the transaction for the user.
    *
    * @return The string, which should fit the template "Unable to {0}."
    */
   public abstract String describe();

   /**
    * Attempt to perform the transaction.  If the transaction fails, the function
    * may be called again, depending on the handler and the situation,
    * so the function code must be written so that it works in that case.
    *
    * @return True if the transaction was performed successfully.
    *         False if the thread is stopping.
    *         If there's an error, an exception should be thrown.
    */
   public abstract boolean run(PauseCallback callback) throws Exception;

}

