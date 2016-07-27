/*
 * ServiceUtil.java
 */

package com.lifepics.neuron.net;

import com.lifepics.neuron.core.Log;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.object.XML;

import java.util.logging.Level;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * A utility class for dealing with the standard result format that my web services use.
 * Compare {@link ServerException}, which shows a nicer way to do the same kind of thing.
 * The reason I don't do the same thing here is, here we don't have any numeric codes,
 * and without those we can't reliably recognize any of the actual causes of exceptions.
 */

public class ServiceUtil {

   /**
    * Parse the result node.  It should be either an error message or a result of known type.
    */
   public static Node parseResult(Document doc, String type) throws Exception {

      Node res = XML.getElement(doc,"Result");

      Node error = XML.getElementTry(res,"Error");
      if (error != null) {
         String errType = XML.getElementText(error,"ErrType");
         String message = XML.getElementText(error,"Message");
         String details = XML.getElementText(error,"Details");

         Log.log(Level.FINE,ServiceUtil.class,"i1",new Object[] { errType, message, details });
         throw new Exception(Text.get(ServiceUtil.class,"e2",new Object[] { errType, message }));
      }

      String resType = XML.getAttribute(res,"xsi:type");
      if ( ! resType.equals(type) ) {
         throw new Exception(Text.get(ServiceUtil.class,"e1",new Object[] { resType, type }));
      }
      // type isn't returned in error case, so don't check for it until now

      return res;
   }

}

