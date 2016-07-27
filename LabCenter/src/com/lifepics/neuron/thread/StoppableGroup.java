/*
 * StoppableGroup.java
 */

package com.lifepics.neuron.thread;

import com.lifepics.neuron.core.Text;

/**
 * A thread group that can be stopped in a nice way.
 */

public class StoppableGroup extends ThreadGroup {

// --- construction ---

   public StoppableGroup() {
      super(Text.get(StoppableGroup.class,"s1"));
   }

// --- methods ---

   /**
    * Stop the thread group in a nice way.<p>
    *
    * Before calling this, be sure to stop any threads
    * that might create new threads within the group.
    */
   public void stopNice() {
      Thread[] threads = new Thread[activeCount()];
      int count = enumerate(threads); // count can be less than activeCount if some stop
      for (int i=0; i<count; i++) {
         if (threads[i] instanceof StoppableThread) {
            ((StoppableThread) threads[i]).stopNice();
         }
         // else it's a HTTPClient SocketTimeout thread,
         // which will exit when the sockets are closed.
         // if a thread in a stoppable group opens the
         // first connection, that's where it shows up.
      }
   }

}

