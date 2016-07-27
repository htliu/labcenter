/*
 * PostTransaction.java
 */

package com.lifepics.neuron.net;

import HTTPClient.HTTPConnection;
import HTTPClient.HTTPResponse;

/**
 * A class that represents a generic HTTP POST transaction without data uploading.
 * Subclasses implement describe, getFixedURL, getParameters (optional), and receive.
 */

public abstract class PostTransaction extends HTTPTransaction {

   /**
    * Send the header to the server, by calling http.Get or http.Post.
    */
   protected HTTPResponse send(HTTPConnection http, String urlFile, PauseCallback callback) throws Exception {
      HTTPResponse response = http.Post(urlFile);
      if (callback != null) callback.unpaused(); // we have a connection
      return response;
   }

}

