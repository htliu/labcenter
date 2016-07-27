/*
 * HTTPException.java
 */

package com.lifepics.neuron.net;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;

import java.io.IOException;

/**
 * An exception class used to report an unexpected HTTP response code.
 */

public class HTTPException extends IOException {

   private int code;
   private String originalMessage;

   public HTTPException(int code, String message) {
      super(Text.get(HTTPException.class,"e1",new Object[] { Convert.fromInt(code), message }));
      this.code = code;
      this.originalMessage = message;
   }

   public int getCode() { return code; }
   public String getOriginalMessage() { return originalMessage; }

}

