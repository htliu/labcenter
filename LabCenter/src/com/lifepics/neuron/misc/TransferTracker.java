/*
 * TransferTracker.java
 */

package com.lifepics.neuron.misc;

import java.io.IOException;
import java.util.Iterator;

/**
 * An object that keeps track of file transfers and reports to listeners.
 * It's a complicated object with several inbound interfaces (OK, six),
 * but I couldn't figure out any way to break it up into simpler pieces.
 * Most of the interfaces are just concepts to keep in mind, not actual
 * Java interfaces.  They could be, but I didn't see what it added.
 *
 * The main interfaces are the one called by the transfer threads, plus
 * FileUtil.Callback.  The protocol on those interfaces is quite limited;
 * here's a grammar thingy.
 *
 *    <overall> = <group>*
 *    <group>   = GB1 {GB2 <time>@} GE -- here "@" means "0 or 1 repetitions"
 *    <time>    = TB       <file>*  TE
 *    <file>    = FB1 {FB2 setSizeActual@ copied*} FE | FS
 *
 * In other words, if there's a GB1, there must be a GE, and similarly
 * for TB-TE and FB1-FE, and everything has to be in the correct order.
 * It <i>is</i> possible to have a successful run with no files, for example
 * in an empty upload.  FS (fileSkip) replaces the others in InstallThread
 * branching constructs, a weird special case.
 *
 * Another way to think about it is in terms of states, with the rule that
 * once you go below #3, you have to go down to #0 before going up again.
 *
 *      groupBegin1   groupBegin2   timeBegin   fileBegin1   fileBegin2
 *    0             1             2           3            4            5 copied
 *     ^groupEnd----+------------+ ^timeEnd--+ ^fileEnd----+-----------+
 *
 * The states view is especially useful here because we do actually keep
 * an explicit state variable that tells which data fields are valid.
 *
 * The third interface is setListener.  It's not much of an interface,
 * only called once, but it does need to be synchronized like the others
 * because the transfer threads start running before the UI is complete.
 *
 * The fourth interface is the ping function, which only does things when
 * we're in state #3 or higher, but is called all the time for simplicity.
 *
 * The fifth interface is getKbpsLast, used for status reporting to the
 * main LifePics server.
 *
 * The sixth interface is getTotalSizeGoal, used by the upload thread to
 * send the total upload size to the server.  The server probably just
 * totally ignores it, since it's never complained about transformed versus
 * untransformed values, but I'll keep sending it anyway.  It should only
 * be called in state #2 or higher.
 *
 * All interface entry points are synchronized to avoid any thread issues.
 * Interfaces 1, 2, and 6 are called from the transfer thread, 4 and 5 from
 * the UI thread, and 3 from the main app thread during frame construction.
 */

public class TransferTracker implements FileUtil.Callback {

// --- adapter interface ---

   public interface Adapter {

      String getGroupID(Object group);
      Iterator getItems(Object group);

      String getFilename(Object item);
      long getSize(Object item, Object group) throws IOException;
      boolean isComplete(Object item);
   }

// --- fields ---

   private Adapter adapter;
   private TransferListener listener;

   private int state;
   private Double kbpsLast; // (*)

   private long lastSize;

   // state 1
   private String groupID;

   // state 2
   private int count;
   private int countGoal;
   private long totalSizeBase; // base for current file
   private long totalSize;
   private long totalSizeGoal;
   private double fraction;
   private int percent;

   // state 3
   private long startMillis;  // start of timed section
   private long totalSizeAtStart;
   private long totalSizeActual;
   private long timeMillis;
   private Long estimatedMillis; // (*)
   private Double kbpsCurrent;   // (*)

   private long accumulator;
   private boolean averageReady;
   private int iAverage;
   private long[] averageBytes;
   private long[] averageMillis;

   // state 4
   private String filename;

   // state 5
   private long size;
   private long sizeGoal;
   private long sizeActual;
   private Long sizeActualGoal; // null means no size override, not scaled

   // (*) the marked fields can be set or clear independent of the current
   // state, so we need to make them nullable to track that

// --- construction ---

   private static final int CYCLE = 5; // cycle length for rolling average

   public TransferTracker() {

      // adapter set later
      listener = new NullTransferListener(); // this avoids if statements

      state = 0;
      kbpsLast = null;

      // lastSize is handled totally within the copied function

      averageBytes  = new long[CYCLE];
      averageMillis = new long[CYCLE];
   }

   public void setAdapter(Adapter adapter) {
      this.adapter = adapter;
   }

// --- interface 1 : transfer thread ---

   // Q: why two functions groupBegin1 and groupBegin2?
   // A: during download, we don't know the item set until we get the order file

   public synchronized void groupBegin1(Object group) {

      groupID = adapter.getGroupID(group);

      state = 1;

      listener.setGroupID(groupID);
      listener.setKbps(null);
   }

   public synchronized void groupBegin2(Object group) throws IOException {

      count = 0;
      countGoal = 0;
      totalSize = 0;
      totalSizeGoal = 0;

      Iterator i = adapter.getItems(group);
      while (i.hasNext()) {
         Object item = i.next();

         long size = adapter.getSize(item,group);

         countGoal += 1;
         totalSizeGoal += size;

         if (adapter.isComplete(item)) {
            count += 1;
            totalSize += size;
         }
      }

      totalSizeBase = totalSize;
      recalcProgress();

      state = 2;

      listener.setCount(count);
      listener.setCountGoal(countGoal);
      listener.setTotalSize(totalSize);
      listener.setTotalSizeGoal(totalSizeGoal);
      listener.setProgress(fraction,percent);
   }

   public synchronized void groupEnd() {

      state = 0;

      listener.clearGroup();
      listener.setKbps(kbpsLast);
   }

   public synchronized void timeBegin() {

      startMillis = System.currentTimeMillis();
      totalSizeAtStart = totalSize;
      totalSizeActual = 0;

      timeMillis = 0;
      estimatedMillis = null;
      kbpsCurrent = null;

      accumulator = 0;
      averageReady = false;
      iAverage = 0;
      // take first sample now so that we don't lose bytes before first ping
      averageBytes [iAverage] = accumulator;
      averageMillis[iAverage] = startMillis;
      iAverage++;
      // I assume CYCLE is greater than one

      state = 3;

      listener.setTime(timeMillis);
      listener.setEstimated(estimatedMillis);
      listener.setKbps(kbpsCurrent);
   }

   public synchronized void timeEnd(boolean complete) {

      // maybe set kbpsLast ...
      //
      // only measure transfers that completed successfully,
      // not ones that errored out or were thread-stopped.
      // there are several (weak) reasons for this approach.
      //
      // * if the transfer didn't complete successfully,
      // then we spent some time transferring bytes that
      // didn't make it into the total.
      //
      // * if we errored out, presumably we spent some time
      // thrashing at the end without making any progress.
      //
      // * either way, the computed speed is less accurate.
      //
      // * also, the point of measuring the speed is so that
      // users can see when something is wrong, and if there
      // are errors, they'll already know that ...
      // no need to also report a low, inaccurate speed.
      //
      // but, note that we're only checking that the timed section
      // completed successfully.  if the final step of reporting
      // to the server fails, we still got a good kbps measurement
      // out of it.
      //
      if (complete) {

         long dt = System.currentTimeMillis() - startMillis;

         // zero bytes is possible ... maybe it was an account-creating upload
         // with no images, or maybe we just finished downloading an
         // order that earlier had errored out on the "mark complete" step.
         // the point is, don't set kbpsLast if we didn't transfer anything.
         //
         // zero dt is possible if the file fits into the network buffer.
         // on the other hand, we never count a transfer as complete
         // until we hear back from the server, so even if the whole
         // file fits into the network buffer, we still have to wait.
         //
         // negative dt might be possible if the user adjusted the clock?
         //
         if (totalSizeActual > 0 && dt > 0) {

            kbpsLast = new Double( ((double) totalSizeActual) * 8 / dt ); // (**)
            // bits per millisecond = kilobits per second
            // Wikipedia convinced me that in this context, kilo is metric 10^3
         }
      }

      state = 2;

      listener.clearTime();
   }

   // Q: why two functions fileBegin1 and fileBegin2?
   // A: during upload, we don't know the file size until we've transformed it

   public synchronized void fileBegin1(Object item) {

      filename = adapter.getFilename(item);

      state = 4;

      listener.setFilename(filename);
   }

   public synchronized void fileBegin2(Object item, Object group, Long sizeActualGoal) throws IOException {

      size = 0;
      sizeGoal = adapter.getSize(item,group);
      sizeActual = 0;
      this.sizeActualGoal = sizeActualGoal;

      state = 5;

      listener.setSize( (sizeActualGoal != null) ? sizeActual : size ); // both are zero, but do it right
      listener.setSizeGoal( (sizeActualGoal != null) ? sizeActualGoal.longValue() : sizeGoal);
   }

   public synchronized void setSizeActual(Long sizeActualGoal) {

      this.sizeActualGoal = sizeActualGoal;

      listener.setSize( (sizeActualGoal != null) ? sizeActual : size ); // no change yet, but do it right
      listener.setSizeGoal( (sizeActualGoal != null) ? sizeActualGoal.longValue() : sizeGoal);
   }

   // Q: why two fileEnd functions but only one timeEnd function?
   // A: that's just how it turned out, no great significance there

   public synchronized void fileEndComplete() {

      count += 1;
      totalSizeBase += sizeGoal;
      totalSizeActual += (sizeActualGoal != null) ? sizeActualGoal.longValue() : sizeGoal;

      totalSize = totalSizeBase; // should be no change
      recalcProgress();

      state = 3;

      listener.setCount(count);
      listener.setTotalSize(totalSize);
      listener.setProgress(fraction,percent);
      listener.clearFile();
   }

   public synchronized void fileEndIncomplete() {

      totalSize = totalSizeBase; // roll back to base
      recalcProgress();

      state = 3;

      listener.setTotalSize(totalSize);
      listener.setProgress(fraction,percent);
      listener.clearFile();
   }

   public synchronized void fileSkip(Object item, Object group) throws IOException {

      // exactly same effect as the sequence fileBegin1, fileBegin2,
      // copied(0), copied(sizeGoal), fileEndComplete,
      // except that we don't update accumulator or totalSizeActual.
      // those are used to compute transfer metrics, and for a skip
      // there's no actual transfer

      sizeGoal = adapter.getSize(item,group); // OK to use as scratch space

      count += 1;
      totalSizeBase += sizeGoal;

      totalSize = totalSizeBase; // roll forward
      recalcProgress();

      // state stays at 3

      listener.setCount(count);
      listener.setTotalSize(totalSize);
      listener.setProgress(fraction,percent);
   }

// --- interface 2 : FileUtil.Callback ---

   public synchronized void copied(long size) {

      if (size == 0) { // means start of a new transfer attempt
         lastSize = 0;
      } else {
         accumulator += (size - lastSize);
         lastSize = size;
      }
      // if we fail partway through a file, we don't know how much
      // of the last chunk got sent ... oh well.
      // so, we under-report bandwidth in some error situations.
      //
      // also note that because of retries, the accumulator can become larger
      // than totalSizeGoal and even wrap around to negative, at least in theory.
      // it doesn't matter, because all we do is take differences, and those
      // work correctly even across a wrap.  we don't even really need to init
      // the accumulator to zero.

      if (sizeActualGoal != null) {
         long goal = sizeActualGoal.longValue();

         // if sizeActualGoal is null, sizeActual isn't used, no update needed
         sizeActual = size;
         if (sizeActual > goal) sizeActual = goal; // clamp this one too

         if (goal != 0) {

            double fileFraction = ((double) size) / goal; // goal not zero!
            size = Math.round(fileFraction * sizeGoal);
            // we could avoid the floating-point math by taking (size * sizeGoal) / goal,
            // but that would be susceptible to overflow (at only 4 GB file size or so)

         } else { // don't divide by zero

            size = 0;
            // for a file of size zero, copied is invoked only at the start
            // of the file transfer, so the scaled size is zero of sizeGoal.
         }
      }

      if (size > sizeGoal) size = sizeGoal; // in case the download is too large,
         // or there's rounding error in the scaled value

      this.size = size;
      totalSize = totalSizeBase + size;
      recalcProgress();

      listener.setSize( (sizeActualGoal != null) ? sizeActual : size );
      listener.setTotalSize(totalSize);
      listener.setProgress(fraction,percent);
      //
      // you might think we'd want to have a single listener call, so that we don't
      // start a repaint with the first and then have to do a second repaint later
      // for the other two.  but, there's nothing magic about the number of function
      // calls ... the calls don't transfer to the UI thread, so even with one call,
      // the first field-set would start the repaint process.  or, more to the point,
      // even with three calls, they'll almost certainly all finish before the first
      // repaint goes anywhere, so it doesn't matter.
      //
      // if it does ever matter, this is the place to fix it, since this is by far
      // the most frequently used entry point.
   }

// --- interface 3 : setListener ---

   public synchronized void setListener(TransferListener listener) {
      this.listener = listener;

      // send current status, assuming listener starts from blank state

      if (state == 0) listener.setKbps(kbpsLast);
      // when we're totally idle, we display the last kbps reading if we have one,
      // and when we're in state 3 or higher, we display the current kbps reading
      // if we have one, but in states 1 and 2 we don't show anything at all.

      if (state >= 1) {
         listener.setGroupID(groupID);
      }

      if (state >= 2) {
         listener.setCount(count);
         listener.setCountGoal(countGoal);
         listener.setTotalSize(totalSize);
         listener.setTotalSizeGoal(totalSizeGoal);
         listener.setProgress(fraction,percent);
      }

      if (state >= 3) {
         listener.setTime(timeMillis);
         listener.setEstimated(estimatedMillis);
         listener.setKbps(kbpsCurrent);
      }

      if (state >= 4) {
         listener.setFilename(filename);
      }

      if (state >= 5) {
         listener.setSize( (sizeActualGoal != null) ? sizeActual : size );
         listener.setSizeGoal( (sizeActualGoal != null) ? sizeActualGoal.longValue() : sizeGoal);
      }

      // we could reverse the order and use a case with no breaks,
      // but I like setting them in the correct order.
   }

// --- interface 4 : ping ---

   public synchronized void ping() {

      if (state < 3) return;

      // it's possible we could update the kbps and estimated time
      // when data comes in (through FileUtil.Callback), but there's
      // really no need to refresh them more than once a second, and
      // the calculations are moderately complicated.
      // so, update all the time-dependent fields here and only here

      long currentMillis = System.currentTimeMillis();
      timeMillis = currentMillis - startMillis;
      if (timeMillis < 0) timeMillis = 0; // protect formatter from nonsense; could happen if clock set

      // for the estimation, we could try to do something with the kbps value,
      // but it's not worth it.  if, for example, we're getting 50% packet loss,
      // the kbps rate might oscillate, but the estimate should figure out what
      // the average is and stabilize on that.  and, the way to do that is
      // also the easiest thing to write -- just extrapolate based on totalSize
      //
      //     estimatedMillis            is to  timeMillis
      // as  totalSizeGoal - totalSize  is to  totalSize - totalSizeAtStart
      //
      // in combination with the fact that totalSize is scaled from actual size
      // for transformed images, this does some pretty good estimation.

      if (totalSize == totalSizeAtStart) {
         estimatedMillis = null; // nothing copied, can't extrapolate
      } else {
         double ratio = ((double) (totalSizeGoal - totalSize)) / (totalSize - totalSizeAtStart);
         long temp = Math.round(ratio * timeMillis);
         if (temp < 0) temp = 0; // again, protect formatter; but can't happen
         estimatedMillis = new Long(temp);
      }

      long currentBytes = accumulator;

      if (averageReady) {

         long bytes = currentBytes  - averageBytes [iAverage];
         long dt    = currentMillis - averageMillis[iAverage];
         // dt should be about CYCLE seconds, but don't assume

         // zero bytes is allowed and normal here
         if (dt > 0) {
            kbpsCurrent = new Double( ((double) bytes) * 8 / dt ); // (**)
         } else {
            kbpsCurrent = null; // bizarro case, just blank out
         }
      }
      // else leave kbpsCurrent null

      averageBytes [iAverage] = currentBytes;
      averageMillis[iAverage] = currentMillis;

      if (++iAverage == CYCLE) {
         iAverage = 0;
         averageReady = true;
      }

      listener.setTime(timeMillis);
      listener.setEstimated(estimatedMillis);
      listener.setKbps(kbpsCurrent);
      // setKbps not always needed, but mostly
   }

// --- interface 5 : getKbpsLast ---

   public synchronized Double getKbpsLast() {
      return kbpsLast;
   }

// --- interface 6 : getTotalSizeGoal ---

   public synchronized long getTotalSizeGoal() {
      return totalSizeGoal;
   }

// --- utilities ---

   private void recalcProgress() {
      fraction = (totalSizeGoal == 0) ? 0 : ( ((double) totalSize) / totalSizeGoal );
      percent = (int) Math.round(100 * fraction);
   }

}

