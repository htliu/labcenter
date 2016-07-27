/*
 * Query.java
 */

package com.lifepics.neuron.net;

import com.lifepics.neuron.object.Obfuscate;

import java.io.IOException;
import java.net.URLEncoder;

/**
 * A utility class for building HTTP URL query strings.
 */

public class Query {

// --- fields ---

   private StringBuffer buffer; // starts out null

   private boolean hasCleartext;
   private int i1;
   private int i2;

   public Query() { hasCleartext = false; }

// --- methods ---

   public void add(String key, String value) throws IOException {
      addImpl(key,URLEncoder.encode(value,"UTF-8"));
      // note that UTF-8 transforms string bytes with values over 0x80
      // into multibyte patterns ... for example, 80 becomes E2 82 AC.
      // so, leaving out the argument might be better ... but it's not
      // broken, don't fix it.
   }

   public void addPasswordObfuscate(String key, String value) {
      addImpl(key,encode(Obfuscate.hideAlternate(value)));
   }

   /**
    * It's not great to send passwords as cleartext, and we try to avoid it
    * on new transactions, but it's harmless if the connection is secure.
    * However, it's not harmless to write them to the log file in debug mode,
    * so we need a bit of special code to handle that.
    */
   public void addPasswordCleartext(String key, String value) throws IOException {

      if (hasCleartext) throw new IllegalStateException(); // multiple cleartext not supported

      String encoded = URLEncoder.encode(value,"UTF-8");
      addImpl(key,encoded);

      i2 = buffer.length();
      i1 = i2 - encoded.length();
      hasCleartext = true;
   }

   private void prepare() {
      if (buffer == null) {
         buffer = new StringBuffer();
      } else {
         buffer.append('&');
      }
   }

   private void addImpl(String key, String value) {
      prepare();
      buffer.append(key); // assume no special chars here
      buffer.append('=');
      buffer.append(value);
   }

   public void add(Query query) {

      if (hasCleartext && query.hasCleartext) throw new IllegalStateException(); // multiple cleartext not supported

      if (query.buffer != null) {
         prepare();
         if (query.hasCleartext) {
            hasCleartext = true;
            i1 = buffer.length() + query.i1;
            i2 = buffer.length() + query.i2;
         }
         buffer.append(query.buffer);
      }
   }

   public String getWithPrefix_Censored() {
      return (buffer == null) ? "" : ("?" + censoredBuffer());
   }

   public String getWithPrefix() {
      return (buffer == null) ? "" : ("?" + buffer.toString());
   }

   public String getWithoutPrefix() {
      return (buffer == null) ? "" : buffer.toString();
   }

   private String censoredBuffer() {
      return hasCleartext ? buffer.substring(0,i1) + "XXX" + buffer.substring(i2)
                          : buffer.toString();
   }

   /**
    * An alternate encoder that takes a byte array.
    * The result encode(b) is equal to the (non-UTF-8) result
    * URLEncoder.encode(new String(b)) <i>except</i> in five
    * cases where new String(b) maps a byte greater than 0x80
    * onto 0x3F because it's the same character symbol.
    * Here's the complete list of cases:  81 8D 8F 90 9D.
    */
   public static String encode(byte[] b) {
      StringBuffer buffer = new StringBuffer();

      // handle all byte codes greater than 0x80 specially,
      // since they're easy ... let URLEncoder do the rest.
      int base = 0;
      int i;
      for (i=0; i<b.length; i++) {
         if (b[i] >= 0) continue;
         if (base < i) buffer.append(URLEncoder.encode(new String(b,base,i-base)));
         buffer.append('%' + Obfuscate.toHex(new byte[] { b[i] }));
         base = i+1;
      }
      if (base < i) buffer.append(URLEncoder.encode(new String(b,base,i-base)));

      return buffer.toString();
   }

}

