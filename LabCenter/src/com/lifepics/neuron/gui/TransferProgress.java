/*
 * TransferProgress.java
 */

package com.lifepics.neuron.gui;

/**
 * A callback interface for reporting file transfer progress fraction.
 * This is used to connect a TransferPanel to a progress indicator
 * so that the TransferTracker only needs to have one output channel.
 */

public interface TransferProgress {

   /**
    * We have both values handy, so we might as well send them along.
    *
    * @param fraction The fraction complete, in the range 0-1.
    * @param percent  The percent  complete, in the range 0-100.
    */
   void setProgress(double fraction, int percent);

   /**
    * Currently this is the same as setProgress(0,0), but we could have
    * other UI designs in the future; also it avoids a false merge
    * into Double and Integer that we'd then have to resplit every time.
    */
   void clearProgress();

}

