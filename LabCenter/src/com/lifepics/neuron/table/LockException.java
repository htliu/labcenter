/*
 * LockException.java
 */

package com.lifepics.neuron.table;

/**
 * An exception class used to report inability to acquire a lock.
 */

public class LockException extends TableException {

   private Thread lockThread;

   public LockException(Thread lockThread, String message) {
      super(message);
      this.lockThread = lockThread;
   }

   public LockException(Thread lockThread, String message, Throwable cause) {
      super(message,cause);
      this.lockThread = lockThread;
   }

   public Thread getLockThread() { return lockThread; }

}

