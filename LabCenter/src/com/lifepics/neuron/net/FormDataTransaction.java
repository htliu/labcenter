/*
 * FormDataTransaction.java
 */

package com.lifepics.neuron.net;

import HTTPClient.HTTPConnection;
import HTTPClient.HTTPResponse;
import HTTPClient.NVPair;

import java.io.IOException;

/**
 * A class that represents a generic HTTP POST transaction with form data.
 * This differs from PostDataTransaction in that (1) the content type is set
 * to form-encoded, and (2) a query object is used as a convenience.
 * Subclasses implement describe, getFixedURL, getParameters (optional),
 * getFormData, and receive.
 *
 * Usually you won't want to send query parameters and form data fields
 * at the same time, but it's technically possible, and I allow it here.
 */

public abstract class FormDataTransaction extends HTTPTransaction {

// --- more subclass hooks ---

   /**
    * Get the form data for the request.
    */
   protected abstract void getFormData(Query query) throws IOException;

// --- implementation ---

   /**
    * Send the header to the server, by calling http.Get or http.Post.
    */
   protected HTTPResponse send(HTTPConnection http, String urlFile, PauseCallback callback) throws Exception {

      Query query = new Query();
      getFormData(query);

      NVPair[] headers = new NVPair[1];
      headers[0] = new NVPair("Content-Type","application/x-www-form-urlencoded");

      HTTPResponse response = http.Post(urlFile,query.getWithoutPrefix(),headers);
      if (callback != null) callback.unpaused(); // we have a connection
      return response;
   }

}

