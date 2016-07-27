/*
 * AgfaException.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;

/**
 * An exception that carries extra Agfa information (if anyone cares).
 */

public class AgfaException extends Exception {

   private String function;
   private int errorCode;

   public AgfaException(String function, int errorCode) {
      super(Text.get(AgfaException.class,"e1",new Object[] { function, Convert.fromInt(errorCode) }));
      this.function = function;
      this.errorCode = errorCode;
   }

   public String getFunction() { return function; }
   public int getErrorCode() { return errorCode; }

}

