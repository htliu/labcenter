/*
 * CompressUploadTransaction.java
 */

package com.lifepics.neuron.net;

import com.lifepics.neuron.misc.Compress;
import com.lifepics.neuron.misc.FileUtil;

import HTTPClient.HTTPConnection;
import HTTPClient.HTTPResponse;
import HTTPClient.HttpOutputStream;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A class that represents a generic HTTP POST transaction <i>with</i> data uploading.
 * This differs from UploadTransaction in that the data is sent raw, not as form data,
 * and from RawUploadTransaction in that the data is compressed and Base64-encoded.
 * Subclasses implement describe, getFixedURL, getParameters (optional), and getFile.
 *
 * By default, this expects no response, but you can override that if you want.
 */

public abstract class CompressUploadTransaction extends RawUploadTransaction {

   protected HTTPResponse send(HTTPConnection http, String urlFile, PauseCallback callback) throws Exception {

      // similar to RawUploadTransaction.send, but not worth unifying, I think

      File file = getFile();

      // measure the compressed stream -- inefficient, but what else can we do?
      CounterStream counterStream = new CounterStream();
      FileUtil.copy(Compress.wrapOutput(counterStream),file);

      HttpOutputStream stream = new HttpOutputStream((int) counterStream.getCount());
      HTTPResponse response = http.Post(urlFile,stream);
      if (callback != null) callback.unpaused(); // we have a connection

      FileUtil.copy(Compress.wrapOutput(stream),file);

      return response;
   }

   // slightly inefficient to use FileUtil.copy instead of just counting
   // as we read, but we need an OutputStream to produce the compression.

   private static class CounterStream extends OutputStream {
      private long count;
      public long getCount() { return count; }

      public void close() throws IOException {}
      public void flush() throws IOException {}
      public void write(byte[] b                  ) throws IOException { count += b.length; }
      public void write(byte[] b, int off, int len) throws IOException { count +=   len;    }
      public void write(int b                     ) throws IOException { count +=   1;      }
   }

}

