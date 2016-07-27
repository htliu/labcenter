/*
 * NullTransferListener.java
 */

package com.lifepics.neuron.misc;

/**
 * A {@link TransferListener} object that does nothing;
 * it's just a convenience for {@link TransferTracker}.
 */

public class NullTransferListener implements TransferListener {

// --- group fields ---

   public void setGroupID(String groupID) {}
   public void setCount(int count) {}
   public void setCountGoal(int countGoal) {}
   public void setTotalSize(long totalSize) {}
   public void setTotalSizeGoal(long totalSizeGoal) {}
   public void setProgress(double fraction, int percent) {}

   public void clearGroup() {}

// --- time fields ---

   public void setTime(long timeMillis) {}
   public void setEstimated(Long estimatedMillis) {}
   public void setKbps(Double kbps) {}

   public void clearTime() {}

// --- file fields ---

   public void setFilename(String filename) {}
   public void setSize(long size) {}
   public void setSizeGoal(long sizeGoal) {}

   public void clearFile() {}

}

