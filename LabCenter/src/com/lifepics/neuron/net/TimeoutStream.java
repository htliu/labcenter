/*
 * TimeoutStream.java
 */

package com.lifepics.neuron.net;

import HTTPClient.HTTPConnection;

import java.io.IOException;
import java.io.OutputStream;

/**
 * A filter stream that detects write timeout and aborts transfer.
 */

public class TimeoutStream extends OutputStream implements Runnable {

   // design notes for the write timeout:
   //
   // the timeout in HTTPClient is only for reads.  it applies to both
   // header reads and body reads ... the JavaDoc for setTimeout says
   // it doesn't apply to the body, but that was changed in V0.3-3, as
   // explained in the advanced info for HTTPClient.dontTimeoutRespBody.
   // the timeout applies even to reading the response to an upload.
   //
   // anyway, after exhausting other options, I added a write timeout.
   // it applies only to the body ... but TCP/IP absorbs the first 32K
   // of data into buffers, so the headers are pretty much guaranteed
   // not to block.  the timeout is maybe not quite the same in detail
   // as the HTTPClient one ... it requires that any given write call
   // finish within the timeout interval.  so, if you write a block of
   // 1024 bytes, and it gets sent through at a regular 1 byte/second,
   // it will take 17+ minutes for the call to complete, and if you've
   // set the timeout to less than that, it will fail.  you could fix
   // that by sending only one byte at a time, but I don't think it will
   // matter ... the problem we're trying to solve is that uploads get
   // completely locked up, not that they go slowly but surely.
   // I mean, the dealers should all have substantial bandwidth, right?

   // Q: Why not use java.io.FilterOutputStream as the base class?
   // A: Because I want something that is completely transparent,
   // and in spite of what the documentation says about it, FOS isn't.
   //
   //    The class FilterOutputStream itself simply overrides
   //    all methods of OutputStream with versions that
   //    pass all requests to the underlying output stream.
   //
   // If you look at the code, it's true that each request produces
   // <i>a</i> request on the underlying stream, but it's not the
   // <i>same</i> request, and other things are added in some cases.

// --- fields ---

   private OutputStream out;
   private HTTPConnection http;

   // synchronized variables
   private long activeTime;
   private boolean timedOut;
   private boolean stopping;

// --- construction ---

   public TimeoutStream(OutputStream out, HTTPConnection http) {

      this.out = out;
      this.http = http;

      setActiveTime();
      timedOut = false;
      stopping = false;
   }

// --- implementation of OutputStream ---

   public void write(int b) throws IOException {
      out.write(b);
      setActiveTime();
   }
   public void write(byte[] b) throws IOException {
      out.write(b);
      setActiveTime();
   }
   public void write(byte[] b, int off, int len) throws IOException {
      out.write(b,off,len);
      setActiveTime();
   }
   public void flush() throws IOException {
      out.flush();
      setActiveTime();
   }
   public void close() throws IOException {
      out.close();
      setActiveTime();
   }

   private synchronized void setActiveTime() {
      activeTime = System.currentTimeMillis();
   }

// --- thread control ---

   public void start() {
      new Thread(this).start(); // thread object not tracked
   }

   public synchronized void stop() {
      stopping = true;
      notify(); // don't check state, no harm in extra notify
   }

   public synchronized boolean isTimedOut() {
      return timedOut;
   }

// --- thread implementation ---

   public void run() {

      int timeout = HTTPConnection.getDefaultTimeout();
      if (timeout <= 0) return; // no timeout
      //
      // currently timeout <= 0 is prevented by validation,
      // but HTTPClient accepts 0 as valid ... maybe we'll
      // want to allow that some day.

      long delay;
      while (true) {

         // check if timed out or stopping
         synchronized (this) {

            if (stopping) break;
            // as a result of this line, if stopping is set, timedOut can't be.
            // that's not the purpose, though ... all that matters is that the
            // thread exits promptly once stopping is set.
            // and, in fact it is possible to set them both in the other order.

            delay = activeTime + timeout - System.currentTimeMillis();
            if (delay <= 0) timedOut = true;
         }

         // keep possibly-slow stream close outside synchronization
         if (delay <= 0) { http.stop(); break; }

         // wait until anticipated timeout
         synchronized (this) {

            if (stopping) break;
            // important to check stopping flag before entering the wait state!

            try {
               wait(delay);
            } catch (InterruptedException e) {
               // won't happen
            }
         }
      }

      // fall through and exit
   }
}

