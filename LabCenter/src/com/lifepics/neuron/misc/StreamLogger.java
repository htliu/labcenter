/*
 * StreamLogger.java
 */

package com.lifepics.neuron.misc;

import com.lifepics.neuron.core.Log;
import com.lifepics.neuron.misc.FileUtil;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.logging.Level;

/**
 * A utility class for absorbing streams from external processes,
 * adapted from this helpful article.
 * http://www.javaworld.com/javaworld/jw-12-2000/jw-1229-traps.html
 */

public class StreamLogger extends Thread {

   // Q: why make this a daemon thread?
   // A: because this is the only kind of thread in the whole application
   // that we don't track and have no way of shutting down (except by
   // making sure the external process exits, which is what we do for ImageLib).
   // the underlying problem is, as long as a non-daemon thread is active,
   // Java won't exit (unless you call System.exit, which we do, but it's
   // bad form to rely on that)
   //
   // as I said in StatusNews, most threads fall into one of two categories.
   // (1) threads that run indefinitely and are tracked by the system
   // (2) threads that the UI uses to show a wait dialog on a long operation
   //
   // we can break the first category down like so.
   // (1a) threads that are connected to standard subsystems (which may or
   //      may not be connected to a panel in the UI)
   // (1b) threads that aren't standard but that we do track
   //
   // here are all the latter as of right now.
   // * ServerThread  - used for the lock port and for Pakon
   // * PakonThread   - subsystem is nonstandard since there are many threads,
   //                   one ServerThread plus one PakonThread per connection
   // * StatusNews    - not sure why this is like it is, but at least it's tracked
   // * TimeoutStream - the thread object isn't tracked, but we can communicate
   //                   with it and shut it down

   private Object o;
   private String key;
   private InputStream stream;

   public StreamLogger(Object o, String key, InputStream stream) {
      setDaemon(true);
      this.o = o;
      this.key = key;
      this.stream = stream;
   }

   public void run() {
      try {

         ByteArrayOutputStream buffer = new ByteArrayOutputStream();
         FileUtil.copy(buffer,stream);
         String s = buffer.toString(); // cf. HTTPTransaction.getText

         if (s.length() > 0) {
            Log.log(Level.FINE,o,key + "a",new Object[] { s });
         }
         // shouldn't be much, but if there is, log it, might be useful

      } catch (Exception e) {
         Log.log(Level.WARNING,o,key + "b",e);
         // in this case we'll probably hang too,
         // but log so we have some hint of what's going on
      }
   }

}

