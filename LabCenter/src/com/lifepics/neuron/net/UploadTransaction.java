/*
 * UploadTransaction.java
 */

package com.lifepics.neuron.net;

import com.lifepics.neuron.misc.FileUtil;

import HTTPClient.HTTPConnection;
import HTTPClient.HTTPResponse;
import HTTPClient.NVPair;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.PrintWriter;

/**
 * A class that represents a generic HTTP POST transaction <i>with</i> data uploading.
 * Subclasses implement describe, getFixedURL, and getParameters (optional),
 * which are standard, and also three more functions, getFilename, getFile, and getCallback (optional).<p>
 *
 * By default, this expects no response, but you can override that if you want.
 */

public abstract class UploadTransaction extends HTTPTransaction {

// --- more subclass hooks ---

   /**
    * Get the filename to be used in the MIME header.
    */
   protected abstract String getFilename();

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

      String boundary = generateBoundary();
      NVPair[] headers = new NVPair[1];
      headers[0] = new NVPair("Content-Type","multipart/form-data; boundary=" + boundary);

   // build blobs so we can measure length

      ByteArrayOutputStream blobStream1 = new ByteArrayOutputStream();
      PrintWriter writer1 = new PrintWriter(blobStream1);

      writer1.println("--" + boundary);
      writer1.println("Content-Disposition: form-data; name=\"file\"; filename=\"" + getFilename() + "\"");
      writer1.println("Content-Type: application/octet-stream");
      writer1.println();
      writer1.close();

      ByteArrayOutputStream blobStream2 = new ByteArrayOutputStream();
      PrintWriter writer2 = new PrintWriter(blobStream2);

      writer2.println();
      writer2.println("--" + boundary + "--");
      writer2.close();

      byte[] blob1 = blobStream1.toByteArray();
      byte[] blob2 = blobStream2.toByteArray();

   // send

      return sendStream(http,urlFile,callback,getFile(),getCallback(),
                        headers,blob1,blob2,/* base64 = */ false);
   }

   /**
    * Receive data from the server.
    */
   protected boolean receive(InputStream inputStream) throws Exception {
      return true;
   }

}

