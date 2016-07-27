/*
 * Email.java
 */

package com.lifepics.neuron.net;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;

/**
 * A utility class for validating email addresses.
 */

public class Email {

   public static void validate(String email) throws ValidationException {

   // RFC 822 validation

      try {
         EmailImpl.validate(email);
      } catch (NoClassDefFoundError e) {
         throw new ValidationException(Text.get(Email.class,"e1"));
      }

      // even strict RFC 822 still allows things like "john" and "John <john@abc.com>"
      // possibly we should use InternetAddress.getAddress to pull out the <...> part,
      // but really we don't even want to allow those addresses.

   // extra rules until we have DNS validation

      int i = email.lastIndexOf('@');
      if (i == -1) throw new ValidationException(Text.get(Email.class,"e2"));

      String domain = email.substring(i+1);
      int j = domain.lastIndexOf('.');
      if (j == -1) throw new ValidationException(Text.get(Email.class,"e3",new Object[] { domain }));

      // String tld = domain.substring(j+1);
   }

}

