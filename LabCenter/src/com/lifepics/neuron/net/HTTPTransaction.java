/*
 * HTTPTransaction.java
 */

package com.lifepics.neuron.net;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Log;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.misc.Base64EncoderOutputStream;
import com.lifepics.neuron.misc.FileUtil;

import HTTPClient.HTTPConnection;
import HTTPClient.HttpOutputStream;
import HTTPClient.HTTPResponse;
import HTTPClient.ModuleException;
import HTTPClient.NVPair;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.logging.Level;
import java.util.Random;

/**
 * A class that knows how to perform a complete HTTP request-response transaction ...
 * how to build and send the request, and how to receive and process the response.
 * This is a mid-level superclass.  Its direct subclasses represent particular kinds
 * of transactions (GET, POST), and further subclasses represent actual transactions.
 */

public abstract class HTTPTransaction extends Transaction {

// --- subclass hooks ---

   /**
    * Get the fixed URL for the request, that the parameters will be attached to.
    */
   protected abstract String getFixedURL();

   /**
    * Get the parameters for the request, if there are any.
    */
   protected void getParameters(Query query) throws IOException {}

   /**
    * Send the header to the server, by calling http.Get or http.Post.
    */
   protected abstract HTTPResponse send(HTTPConnection http, String urlFile, PauseCallback callback) throws Exception;

   /**
    * Receive data from the server.
    *
    * @return True if the transaction was performed successfully.
    *         False if the thread is stopping.
    *         If there's an error, an exception should be thrown.
    */
   protected abstract boolean receive(InputStream inputStream) throws Exception;

// --- the run function ---

   /**
    * Attempt to perform the transaction, assuming that there's no reporting and no stopping.
    * The point is to provide an alternative to calling run and then not checking the result.
    */
   public void runInline() throws Exception {
      if ( ! run(null) ) throw new Exception(Text.get(HTTPTransaction.class,"e1"));
   }

   /**
    * Attempt to perform the transaction.  If the transaction fails, the function
    * may be called again, depending on the handler and the situation,
    * so the function code must be written so that it works in that case.
    *
    * @return True if the transaction was performed successfully.
    *         False if the thread is stopping.
    *         If there's an error, an exception should be thrown.
    */
   public boolean run(PauseCallback callback) throws Exception {

   // figure out URL

      String fixedURL = getFixedURL();

      Query query = new Query();
      getParameters(query);

      URL url = new URL(fixedURL + query.getWithPrefix());

      Log.log(Level.FINER,HTTPTransaction.class,"i2",new Object[] { fixedURL + query.getWithPrefix_Censored() });
         // reserve FINE for things that are intermittent

   // send and receive

      HTTPConnection http = new HTTPConnection(url);
      http.setDefaultHeaders(new NVPair[] { new NVPair("Connection","close") }); // see note below
      // default timeout
      http.setAllowUserInteraction(false);

      // since I create a new HTTPConnection object for every request,
      // connections wouldn't be reused anyway, but this way we don't
      // wait for sixty seconds before HTTPClient auto-closes it.

      HTTPStopAction hsa = new HTTPStopAction(http);
      try {
         if (hsa.set()) return false; // set returns isStopping

         HTTPResponse response = send(http,url.getFile(),callback);
         checkResponseCode(response);
         return receive(getInputStream(response));

      } catch (Exception e) {
         if (hsa.caused(e)) return false; else throw e;
      } finally {
         hsa.clear();
         http.stop(); // see comment below
      }

      // the HTTPClient documentation says to use response.getInputStream().close().
      // that would be necessary if we had other requests on the same connection,
      // but we don't, and using stop instead of close takes care of one minor problem.
      //
      // (1) if the connection times out before the header loads,
      //     getInputStream waits (again) for the header to load,
      //     and times out, and then the socket doesn't get closed.
   }

// --- utility functions ---

   public static String combine(String baseURL, String page) {
      boolean add = ! baseURL.endsWith("/");
      return baseURL + (add ? "/" : "") + page;
   }
   // in most places in the system where you enter a URL,
   // it's for a file, not a directory.
   // so, it's not obvious whether we should expect the base URL
   // to end in '/' or not .... be nice and handle it either way.

   private static InputStream getInputStream(HTTPResponse response) throws Exception {
      InputStream inputStream = response.getInputStream();
      if (inputStream == null) throw new RetryableException(Text.get(HTTPTransaction.class,"e2"));
      return inputStream;
      // docs don't say anything about stream being null, but once in a while it is.
      // no idea why, probably something about the demux.  retry, but not infinitely.
   }

   private static void checkResponseCode(HTTPResponse response) throws Exception {
      int code = response.getStatusCode();
      if (code != HttpURLConnection.HTTP_OK) {

         String message = response.getReasonLine();

         Level level = null; // not null means yes, do full-text logging

         if (code == HttpURLConnection.HTTP_INTERNAL_ERROR) { // 500
            level = Level.FINE;
         } else if (code == HttpURLConnection.HTTP_BAD_REQUEST) { // 400
            level = Level.INFO;
            // I'd prefer fine, but this is an intermittent one that we
            // want to see in the field, and most people don't log fine
         }

         if (level != null) {
            String text = getText(getInputStream(response));
            Log.log(level,HTTPTransaction.class,"i1",new Object[] { Convert.fromInt(code), message, text });
         }

         throw new HTTPException(code,message);
      }
   }

   protected static String getText(InputStream inputStream) throws IOException {

      // this is the same idea as response.getText, but that function
      // is too picky, e.g., it requires that Content-Type be present.

      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      FileUtil.copy(buffer,inputStream);
      return buffer.toString();
   }

   private static Random random = new Random(System.currentTimeMillis());

   protected static String generateDigits(int n) {
      char[] c = new char[n];

      synchronized (random) {
         for (int i=0; i<n; i++) {
            c[i] = (char) ('0' + random.nextInt(10));
         }
      }

      return new String(c);
   }

   protected static String generateBoundary() {
      return "------------------------------" + generateDigits(15);
      // arbitrary choice, 30 dashes plus 15 digits
   }

   /**
    * Send a streamed file as the response, possibly using a prefix and suffix, possibly base-64 encoded.
    *
    * @param blob1 The prefix, or null for none.
    * @param blob2 The suffix, or null for none.
    */
   protected static HTTPResponse sendStream(HTTPConnection http, String urlFile, PauseCallback callback1,
                                            File file, FileUtil.Callback callback2,
                                            NVPair[] headers, byte[] blob1, byte[] blob2, boolean base64) throws Exception {

      int length = (int) FileUtil.getSize(file);
      if (base64) length = (int) FileUtil.inBase64(length);
      if (blob1 != null) length += blob1.length;
      if (blob2 != null) length += blob2.length;

      HttpOutputStream stream0 = new HttpOutputStream(length);
      HTTPResponse response = http.Post(urlFile,stream0,headers);
      if (callback1 != null) callback1.unpaused(); // we have a connection

      TimeoutStream stream = new TimeoutStream(stream0,http);
      try {
         stream.start();

         if (blob1 != null) stream.write(blob1);
         if (base64) {

            OutputStream os = new Base64EncoderOutputStream(stream);
            FileUtil.copyNoClose(os,file,callback2);
            os.flush(); // make sure we get the last few bytes out
            //
            // * the base-64 wrapper needs to be on the outside because the blob part
            //   is timed but not base-64 encoded.
            // * the copy operation is happening outside the encoding, so the callback
            //   sees unencoded size, not encoded size.  it also doesn't see the blob.

         } else {

            FileUtil.copyNoClose(stream,file,callback2);
         }
         if (blob2 != null) stream.write(blob2);
         stream.close();

      } catch (Exception e) {
         if (stream.isTimedOut() && HTTPStopAction.isStopException(e)) {
               // discard original exception, it's not informative
            e = new SocketTimeoutException(Text.get(HTTPTransaction.class,"e3"));
            // the class matters, it's what makes the exception retryable
         }
         throw e;
      } finally {
         stream.stop();
      }

      return response;
   }

}

