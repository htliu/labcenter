/*
 * ServerException.java
 */

package com.lifepics.neuron.net;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.object.XML;

import org.w3c.dom.Node;

/**
 * An exception class that's also a data carrier for server errors.
 * In spite of the name, it is not related to {@link ServerThread}.
 *
 * Note, the error codes here are totally transaction-dependent!
 */

public class ServerException extends Exception {

// --- the basic class ---

   private int code;
   private String originalMessage; // avoid conflict with Exception.getMessage

   public ServerException(int code, String message) {
      super(Text.get(ServerException.class,"e1",new Object[] { Convert.fromInt(code), message }));
      this.code = code;
      this.originalMessage = message;
   }

   public int getCode() { return code; }
   public String getOriginalMessage() { return originalMessage; }

// --- parse utilities ---

   private static final String NAME_ERROR = "Error";

   /**
    * @param required True if the error structure is always present.
    * @return An exception, or null if no error structure was found.
    *         If required is set, the result is guaranteed not null.
    */
   public static ServerException parseStandard(Node parent, boolean required) throws ValidationException {

      Node node;
      if (required) {
         node = XML.getElement   (parent,NAME_ERROR);
      } else {
         node = XML.getElementTry(parent,NAME_ERROR);
         if (node == null) return null;
      }

      String codeString = XML.getElementText(node,"Code");
      int codeInt = Convert.toInt(codeString);
      String message = XML.getElementText(node,"Message");

      return new ServerException(codeInt,message);
   }

}

