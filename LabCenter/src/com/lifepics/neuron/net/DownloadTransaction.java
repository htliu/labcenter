/*
 * DownloadTransaction.java
 */

package com.lifepics.neuron.net;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.misc.FileUtil;
import com.lifepics.neuron.object.Obfuscate;

import HTTPClient.HTTPConnection;
import HTTPClient.HTTPResponse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.Adler32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.Checksum;

/**
 * A class that represents a generic HTTP GET transaction, but with the received data
 * going into a file.  Subclasses implement describe, getFixedURL, and getParameters (optional),
 * which are standard, and also two more functions, getFile and getCallback (optional).
 */

public abstract class DownloadTransaction extends GetTransaction {

   private boolean overwrite;
   private long expectedSize;
   private long acceptedSize; // a size that's not correct but that we accept anyway
   private Integer expectedChecksum;

   public DownloadTransaction(boolean overwrite) {
      this(overwrite,-1,-1);
   }
   public DownloadTransaction(boolean overwrite, long expectedSize) {
      this(overwrite,expectedSize,-1);
   }
   public DownloadTransaction(boolean overwrite, Long expectedSize) {
      this(overwrite,(expectedSize != null) ? expectedSize.longValue() : -1,-1);
   }
   public DownloadTransaction(boolean overwrite, Long expectedSize, Long acceptedSize) {
      this(overwrite,(expectedSize != null) ? expectedSize.longValue() : -1,
                     (acceptedSize != null) ? acceptedSize.longValue() : -1);
   }
   public DownloadTransaction(boolean overwrite, long expectedSize, long acceptedSize) {
      this.overwrite = overwrite;
      this.expectedSize = expectedSize;
      this.acceptedSize = acceptedSize;
      this.expectedChecksum = null;
   }

   public void initChecksum(int expectedChecksum) {
      this.expectedChecksum = new Integer(expectedChecksum);
   }
   public void initChecksum(Integer expectedChecksum) {
      this.expectedChecksum = expectedChecksum;
   }
   // easier than dealing with multiple constructors

// --- more subclass hooks ---

   /**
    * Get the file location to be downloaded into.
    */
   protected abstract File getFile();

   /**
    * Get a callback to be used for copy progress reporting.
    */
   protected FileUtil.Callback getCallback() { return null; }

   protected void writePrefix(OutputStream dest) throws IOException {}
   protected void writeSuffix(OutputStream dest) throws IOException {}

// --- implementation ---

   /**
    * Receive data from the server.
    *
    * @return True if the transaction was performed successfully.
    *         False if the thread is stopping.
    *         If there's an error, an exception should be thrown.
    */
   protected boolean receive(InputStream inputStream) throws Exception {

      // this is the same as FileUtil.copy(File,InputStream),
      // except that the prefix and suffix blocks are added.

      File file = getFile();
      if (overwrite) FileUtil. makeNotExists(file);
      else           FileUtil.checkNotExists(file);

      // the call to makeNotExists has to go here,
      // not outside the Transaction object,
      // so we can retry after a partial download

      Checksum checksum = null;

      OutputStream dest = null;
      try {
         dest = new FileOutputStream(file);

         OutputStream useDest = dest;
         if (expectedChecksum != null) {
            checksum = new Adler32();
            useDest = new CheckedOutputStream(dest,checksum);
         }

         writePrefix(dest);
         FileUtil.copyNoClose(useDest,inputStream,getCallback());
         writeSuffix(dest);

      } finally {
         try {
            if (dest != null) dest.close();
         } catch (IOException e) {
            // ignore
         }
      }

      // update #2: Opie does send a Content-Length header, so that takes care of that.
      // note, if we disconnect before getting that many bytes, we get an EOFException,
      // but if the server tries to send more bytes, HTTPClient will ignore them.
      // shouldn't be a problem in practice, but still it's a little bit unfortunate.
      //
      // update: with the new & improved retrying scheme, a different solution is better.
      // this code can throw a wrong-size exception even if the size is consistent,
      // and the diagnose handler can make wrong-size exceptions only finitely retryable.
      // so, if the network is transiently bad, there will be plenty of retries,
      // and if the file is transiently not found (which gives a consistent size of 80),
      // we'll come back to it in an hour and hopefully find it in place.
      // and, if the size is consistently wrong for a week, <i>then</i> we can error out.
      //
      // original:
      // here's the idea behind the expected size check
      //
      // if the network connection is closed prematurely by the server for some reason,
      // or if somehow a network failure looks like a closed socket (shouldn't happen),
      // we'll see an apparently complete file that has the wrong length.
      //
      // now, on the one hand, we want this to be a retryable condition;
      // on the other, if we're actually receiving the whole file from the server,
      // and it really is the wrong size, we don't want to download it repeatedly.
      //
      // so, the plan is, if the size is wrong the same way twice in a row,
      // throw a different kind of exception to indicate it's not retryable.
      //
      // the server currently doesn't send a Content-Length header, but if it did,
      // and the size were wrong, HTTPClient would throw a java.io.EOFException
      // with some message.  right now that's not treated as a retryable condition.
      // (but now it is)

      if (expectedSize != -1) { // do we know the expected size?

         long actualSize = FileUtil.getSize(file);

         if (    actualSize != expectedSize
              && actualSize != acceptedSize ) { // does the right thing if acceptedSize is -1

            String message = Text.get(DownloadTransaction.class,"e1",new Object[] { file.getName(), Convert.fromLong(expectedSize), Convert.fromLong(actualSize) });
            throw new WrongSizeException(message);
         }
         // in any case, leave the file around, the user might want to look at it
      }

      if (expectedChecksum != null) { // do we know the expected checksum?

         int actualChecksum = (int) checksum.getValue(); // also not null

         if (actualChecksum != expectedChecksum.intValue()) {

            String message = Text.get(DownloadTransaction.class,"e2",new Object[] { file.getName(), Obfuscate.fromChecksum(expectedChecksum.intValue()), Obfuscate.fromChecksum(actualChecksum) });;
            throw new WrongSizeException(message); // not really wrong size, but has same effect
         }
      }
      // note, the checksum option isn't compatible with the acceptedSize option.
      // we would need to fix the image downloader if we wanted to add checksums.

      return true;
   }

// --- content length handler ---

   public interface ContentLengthHandler {
      void handle(long contentLength);
   }

   private ContentLengthHandler contentLengthHandler;

   public void setContentLengthHandler(ContentLengthHandler contentLengthHandler) {
      this.contentLengthHandler = contentLengthHandler;
   }

   protected HTTPResponse send(HTTPConnection http, String urlFile, PauseCallback callback) throws Exception {
      HTTPResponse response = super.send(http,urlFile,callback);
      if (contentLengthHandler != null) {

         String header = response.getHeader("Content-Length");
         // I seem to remember something about the official name being "Content-length",
         // but this is what our servers use, and also the function is case-insensitive.
         // surprisingly, there's no string constant for this in HTTPClient!

         if (header == null) throw new Exception(Text.get(DownloadTransaction.class,"e3"));

         contentLengthHandler.handle(Convert.toLong(header)); // won't fail, HTTPClient has already examined it
      }
      return response;
   }

// --- checksum utility ---

   private static class NullOutputStream extends OutputStream {
      public void close() {}
      public void flush() {}
      public void write(byte[] b) {}
      public void write(byte[] b, int off, int len) {}
      public void write(int b) {}
   }

   public static int getChecksum(File file) throws IOException {
      Checksum checksum = new Adler32();
      FileUtil.copy(new CheckedOutputStream(new NullOutputStream(),checksum),file);
      return (int) checksum.getValue();
   }

   public static void main(String[] args) throws IOException {
      System.out.println(Obfuscate.fromChecksum(getChecksum(new File(args[0]))));
   }

}

