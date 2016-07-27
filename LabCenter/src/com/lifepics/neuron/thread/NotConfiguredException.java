/*
 * NotConfiguredException.java
 */

package com.lifepics.neuron.thread;

import com.lifepics.neuron.core.Text;

/**
 * An exception that means a thread is not configured correctly
 * and should never have been started.  For now it's just
 * a convenient way to tag the points where this happens and get
 * a standard error message, but maybe later we'll check for it.
 */

public class NotConfiguredException extends ThreadStopException {

   public NotConfiguredException(String message) {
      this(new Exception(message));
   }

   public NotConfiguredException(Throwable cause) {
      super(Text.get(NotConfiguredException.class,"e1"),cause);
   }

}

