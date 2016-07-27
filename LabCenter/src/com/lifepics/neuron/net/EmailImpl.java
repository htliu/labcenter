/*
 * EmailImpl.java
 */

package com.lifepics.neuron.net;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

/**
 * A utility class for validating email addresses.
 */

public class EmailImpl {

   // *** WARNING!  This class requires an external jar file!
   // *** Callers must catch and handle NoClassDefFoundError!

   public static void validate(String email) throws ValidationException {
      try {
         new InternetAddress(email); // default is strict, and this way works with javamail 1.2
      } catch (AddressException e) {
         throw new ValidationException(Text.get(EmailImpl.class,"e1"),e);
      }
   }

}

