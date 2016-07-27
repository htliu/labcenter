/*
 * TransferListener.java
 */

package com.lifepics.neuron.misc;

/**
 * A callback interface for reporting file transfer status information.
 * The interface is used for both rolls (upload) and orders (download).
 */

public interface TransferListener {

   // the fields in each category aren't always set at the same moment,
   // and sometimes they get updated independently, but they're always
   // cleared together.

// --- group fields ---

   void setGroupID(String groupID);
   void setCount(int count);
   void setCountGoal(int countGoal);
   void setTotalSize(long totalSize);
   void setTotalSizeGoal(long totalSizeGoal);

   /**
    * The progress may be computable from the total size and total size goal,
    * depending on how we're doing it, but I'll let the tracker be in charge.
    */
   void setProgress(double fraction, int percent);

   void clearGroup();

// --- time fields ---

   void setTime(long timeMillis);
   void setEstimated(Long estimatedMillis); // null means no estimate
   void setKbps(Double kbps);               // null means no kbps yet

   void clearTime();

// --- file fields ---

   void setFilename(String filename);
   void setSize(long size);
   void setSizeGoal(long sizeGoal);

   void clearFile();

}

