/*
 * FormDataUploadTransaction.java
 */

package com.lifepics.neuron.net;

import com.lifepics.neuron.misc.FileUtil;

import HTTPClient.HTTPConnection;
import HTTPClient.HTTPResponse;
import HTTPClient.NVPair;

import java.io.File;
import java.io.IOException;

/**
 * Another HTTP POST transaction class.  See package notes for comparison.
 * Subclasses implement describe, getFixedURL, getParameters (optional),
 * and receive, which are standard, and also the additional three functions
 * getFormData, getFile, and getCallback (optional).<p>
 *
 * Usually you won't want to send query parameters and form data fields
 * at the same time, but it's technically possible, and I allow it here.
 */

public abstract class FormDataUploadTransaction extends HTTPTransaction {

// --- more subclass hooks ---

   /**
    * Get the form data for the request.  This must include an empty
    * parameter at the end of the list for the file to go into!
    */
   protected abstract void getFormData(Query query) throws IOException;

   /**
    * Get the file to be uploaded.
    */
   protected abstract File getFile();

   /**
    * Get a callback to be used for copy progress reporting.
    */
   protected FileUtil.Callback getCallback() { return null; }

// --- implementation ---

   /**
    * Send the header to the server, by calling http.Get or http.Post.
    */
   protected HTTPResponse send(HTTPConnection http, String urlFile, PauseCallback callback) throws Exception {

      Query query = new Query();
      getFormData(query);

      byte[] blob = query.getWithoutPrefix().getBytes();

      NVPair[] headers = new NVPair[1];
      headers[0] = new NVPair("Content-Type","application/x-www-form-urlencoded");

      return sendStream(http,urlFile,callback,getFile(),getCallback(),
                        headers,blob,null,/* base64 = */ true);
   }

}

