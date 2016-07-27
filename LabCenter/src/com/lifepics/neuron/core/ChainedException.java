/*
 * ChainedException.java
 */

package com.lifepics.neuron.core;

/**
 * A utility class for concatenating messages from chained exceptions.
 */

public class ChainedException {

   private static String delimiter = Text.get(ChainedException.class,"s1") + ' ';

   /**
    * Build a message by concatenating messages along the exception chain.
    *
    * @param t The start of the exception chain.
    */
   public static String format(Throwable t) {
      return format(null,t);
   }

   /**
    * Build a message by concatenating messages along the exception chain.
    *
    * @param message A message to be considered as a top-level exception message.
    *                The calls format(message,t) and format(new Throwable(message,t))
    *                produce exactly the same result.
    * @param t The start of the exception chain.
    */
   public static String format(String message, Throwable t) {
      Throwable tSave = t;

      StringBuffer buffer = new StringBuffer();
      boolean first = true;

      if (message != null) {
         buffer.append(message);
         first = false;
      }

      for ( ; t != null; t = t.getCause()) {

         String s = t.getMessage();
         if (s == null || s.length() == 0) continue;

         if ( ! first ) {
            // auto-remove final periods in text
            int len = buffer.length();
            if (len > 0 && buffer.charAt(len-1) == '.') buffer.setLength(len-1);

            buffer.append(delimiter);
         }

         buffer.append(s);
         first = false;
      }

      // all exceptions in LabCenter take a message argument except the following:
      //
      //  * subclasses of RuntimeException
      //  * Java exceptions that don't take a message argument (e.g. UnsupportedFlavorException)
      //  * signaling exceptions that are caught and discarded
      //  * pure wrapper exceptions (e.g. ToldException, NormalOperationException, LocalException)
      //  * exceptions in the launcher where Text isn't available
      //
      // we do want to skip exceptions with no message, see the list above.
      // but, if there's no message at all, we definitely want *something*.
      //
      if (first) buffer.append(Text.get(ChainedException.class,"s2",new Object[] { tSave.getClass().getName() }));

      return buffer.toString();
   }

}

