/*
 * Diagnose.java
 */

package com.lifepics.neuron.net;

import HTTPClient.HTTPConnection;
import HTTPClient.HTTPResponse;
import HTTPClient.NVPair;

import java.net.URL;
import java.util.Collections;
import java.util.LinkedList;
import java.util.ListIterator;

/**
 * A utility class for diagnosing the condition of the network.
 */

public class Diagnose {

// --- main function ---

   public static final int RESULT_NET_UP_SITE_UP = 0;
   public static final int RESULT_NET_UP_SITE_DOWN = 1;
   public static final int RESULT_NET_DOWN = 2;
   public static final int RESULT_STOPPED = 3;

   private static class StopException extends Exception {}

   /**
    * @return One of the enumerated result codes above.
    */
   public static int diagnose(DiagnoseConfig config) {
      try {
         return diagnoseImpl(config);
      } catch (StopException e) {
         return RESULT_STOPPED;
      }
   }

   private static int diagnoseImpl(DiagnoseConfig config) throws StopException {
      PollResult result;

      // the tests for empty lists guarantee that the requirements of
      // the poll function will be met.  also, the list sizes work out,
      // so the result will never be indefinite
      //
      // netFailLimit is not zero, because of validation

   // check the site

      boolean siteUp;
      if (config.siteURLs.isEmpty()) {
         siteUp = true;
      } else {
         result = poll(config.siteURLs,config.siteURLs.size(),1,config.timeoutInterval);
         siteUp = (result.code == CODE_SUCCESS);
      }

      if (siteUp) return RESULT_NET_UP_SITE_UP;

   // check the net

      boolean netUp;
      if (config.netURLs.isEmpty()) {
         netUp = true;
      } else {
         result = poll(config.netURLs,1,Math.min(config.netFailLimit,config.netURLs.size()),config.timeoutInterval);
         netUp = (result.code == CODE_SUCCESS);
      }

      if (netUp) return RESULT_NET_UP_SITE_DOWN;

   // done

      return RESULT_NET_DOWN;
   }

// --- poll functions ---

   private static final int CODE_SUCCESS = 0;
   private static final int CODE_FAILURE = 1;
   private static final int CODE_INDEFINITE = 2;

   private static class PollResult {
      public int code;
      public int successCount;
      public int failCount;
   }

   /**
    * Poll the URLs in a list until either the success limit or the fail limit
    * is achieved or the list runs out of elements.
    */
   private static PollResult poll(LinkedList URLs, int successLimit, int failLimit, int timeoutInterval) throws StopException {
      PollResult result = new PollResult();

      result.successCount = 0;
      result.failCount = 0;

      LinkedList list = new LinkedList(URLs); // copy before shuffle
      Collections.shuffle(list);

      ListIterator li = list.listIterator();
      while (    li.hasNext()
              && result.successCount < successLimit
              && result.failCount    < failLimit    ) {

         String s = (String) li.next();
         if (poll(s,timeoutInterval)) result.successCount++; else result.failCount++;
      }

      if      (result.successCount == successLimit) result.code = CODE_SUCCESS;
      else if (result.failCount    == failLimit   ) result.code = CODE_FAILURE;
      else                                          result.code = CODE_INDEFINITE;

      // the loop ends when one or more of the conditions is met.
      // each single condition corresponds to a code.
      // here is an analysis of how I am thinking about
      // the cases where multiple conditions are met.
      //
      // I assume that the list is not empty,
      // and that the limits are not zero.
      // that leaves only two cases with multiple conditions.
      //
      //  * end of list + success limit --> success
      //  * end of list + fail limit    --> failure
      //
      // if the list has more than (successLimit-1) + (failLimit-1) elements,
      // the result code is guaranteed not to be CODE_INDEFINITE

      return result;
   }

   private static boolean poll(String s, int timeoutInterval) throws StopException {
      try {

         URL url = new URL(s);

         HTTPConnection http = new HTTPConnection(url);
         http.setDefaultHeaders(new NVPair[] { new NVPair("Connection","close") }); // see note in HTTPTransaction
         http.setTimeout(timeoutInterval);
         http.setAllowUserInteraction(false);

         HTTPStopAction hsa = new HTTPStopAction(http);
         try {
            if (hsa.set()) throw new StopException(); // set returns isStopping

            HTTPResponse response = http.Head(url.getFile());
            response.getStatusCode();
            // ignore result, we only care if the connection succeeded

         } catch (Exception e) {
            if (hsa.caused(e)) throw new StopException(); else throw e;
         } finally {
            hsa.clear();
            http.stop(); // see comment in HTTPTransaction
         }

         return true;

      } catch (StopException e) {
         throw e;
      } catch (Exception e) {
         return false;
      }
   }

}

