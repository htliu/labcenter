/*
 * RawUploadTransaction.java
 */

package com.lifepics.neuron.net;

import com.lifepics.neuron.misc.FileUtil;

import HTTPClient.HTTPConnection;
import HTTPClient.HTTPResponse;
import HTTPClient.HttpOutputStream;

import java.io.File;
import java.io.InputStream;

/**
 * A class that represents a generic HTTP POST transaction <i>with</i> data uploading.
 * This differs from UploadTransaction in that the data is sent raw, not as form data.
 * Also, the upload should be small-ish, so the callback and timeout aren't needed.
 * Subclasses implement describe, getFixedURL, getParameters (optional), getFile, and GetCallback (optional).
 *
 * By default, this expects no response, but you can override that if you want.
 */

public abstract class RawUploadTransaction extends HTTPTransaction {

// --- more subclass hooks ---

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

      File file = getFile();

      HttpOutputStream stream = new HttpOutputStream((int) FileUtil.getSize(file));
      HTTPResponse response = http.Post(urlFile,stream);
      if (callback != null) callback.unpaused(); // we have a connection

      FileUtil.copy(stream,file,getCallback());

      return response;
   }

   /**
    * Receive data from the server.
    */
   protected boolean receive(InputStream inputStream) throws Exception {
      return true;
   }

}

