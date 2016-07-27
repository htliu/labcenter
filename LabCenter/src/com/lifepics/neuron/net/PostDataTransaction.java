/*
 * PostDataTransaction.java
 */

package com.lifepics.neuron.net;

import com.lifepics.neuron.object.XML;

import HTTPClient.HTTPConnection;
import HTTPClient.HTTPResponse;

import java.io.ByteArrayOutputStream;

import org.w3c.dom.Document;

/**
 * A class that represents a generic HTTP POST transaction <i>with</i> data uploading.
 * This differs from UploadTransaction in that the data is small and non-MIME.
 * Subclasses implement describe, getFixedURL, getParameters (optional), and receive,
 * and also one of getByteData and getXMLData.
 */

public abstract class PostDataTransaction extends HTTPTransaction {

// --- more subclass hooks ---

   // this is a shortcut ... really you should have two subclasses,
   // one with abstract getByteData, and one that implements it in
   // terms of abstract getXMLData.

   // note that String.getBytes() is a good source of byte data

   protected byte[] getByteData() throws Exception {
      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      XML.writeStream(buffer,getXMLData());
      return buffer.toByteArray();
      // could write XML directly to HttpOutputStream, but I think
      // that adds needless complexity ... not least because
      // it's much better to know the length of the data in advance.
   }

   protected Document getXMLData() throws Exception {
      // not abstract, so that you can override getByteData by itself
      return null;
   }

// --- implementation ---

   /**
    * Send the header to the server, by calling http.Get or http.Post.
    */
   protected HTTPResponse send(HTTPConnection http, String urlFile, PauseCallback callback) throws Exception {
      HTTPResponse response = http.Post(urlFile,getByteData());
      if (callback != null) callback.unpaused(); // we have a connection
      return response;
   }

}

