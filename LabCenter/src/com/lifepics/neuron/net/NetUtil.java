/*
 * NetUtil.java
 */

package com.lifepics.neuron.net;

import HTTPClient.CookieModule;
import HTTPClient.HTTPConnection;

import java.security.Security;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * A utility class for common network setup code,
 * including things related to HTTPS and cookies.
 */

public class NetUtil {

// --- certificates ---

   private static class NullTrustManager implements X509TrustManager {
      public void checkClientTrusted(X509Certificate[] chain, String authType) {}
      public void checkServerTrusted(X509Certificate[] chain, String authType) {}
      public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
   }

   /**
    * Replace the trust manager so that the server chain is not authenticated.
    * we only want encryption, and there is some problem with the authentication.
    */
   private static void ignoreServerCertificates() {
      try {

         SSLContext context = SSLContext.getInstance("SSL");
         context.init(null,new TrustManager[] { new NullTrustManager() },null);

         HTTPConnection.setDefaultSSLSocketFactory(context.getSocketFactory());

      } catch (Exception e) {
      }
   }

// --- helpers ---

   /**
    * Set up standard network behavior.
    */
   public static void initNetwork() {

      // turn off HTTPS server certificate check
      ignoreServerCertificates();

      // totally ignore all cookies
      HTTPConnection.removeDefaultModule(CookieModule.class);

      // turn off Java DNS cache (and rely on OS one)
      Security.setProperty("networkaddress.cache.ttl","0");
      //
      // the problem here is, the Java cache ignores the TTL values,
      // and an app that runs as long as LC does can't get away with that.
      //
      // note 1: there's a negative TTL, too, but it's a harmless
      // optimization, leave it at the default, which is 10 seconds.
      // you can find these in the java.net package documentation.
      //
      // note 2: the cache still seems to hold positive responses
      // for a few seconds, even though it shouldn't ... harmless.
      // it's not the negative setting at work, I checked that.
   }

   /**
    * Just a small wrapper to isolate rest from HTTPClient library.
    */
   public static int getDefaultTimeout() {
      return HTTPConnection.getDefaultTimeout();
   }

   /**
    * Just a small wrapper to isolate rest from HTTPClient library.
    */
   public static void setDefaultTimeout(int defaultTimeoutInterval) {
      HTTPConnection.setDefaultTimeout(defaultTimeoutInterval);
   }

}

