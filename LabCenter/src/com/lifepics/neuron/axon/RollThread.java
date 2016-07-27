/*
 * RollThread.java
 */

package com.lifepics.neuron.axon;

import com.lifepics.neuron.table.Table;
import com.lifepics.neuron.table.TableException;
import com.lifepics.neuron.thread.EntityThread;
import com.lifepics.neuron.thread.Manipulator;
import com.lifepics.neuron.thread.ThreadStatus;

import java.util.Comparator;
import java.util.Date;

/**
 * A superclass for threads that operate on rolls.
 */

public abstract class RollThread extends EntityThread {

// --- subclass hooks ---

   protected abstract boolean doRoll() throws Exception;

// --- fields ---

   protected Roll roll;
   protected Roll.Item item; // convenience, not used here

// --- construction ---

   public RollThread(String name,
                     Table table,
                     Manipulator manipulator,
                     boolean scanFlag,
                     long idlePollInterval,
                     ThreadStatus threadStatus) {

      super(name,table,manipulator,RollUtil.orderRollID,scanFlag,idlePollInterval,threadStatus);
   }

   public RollThread(String name,
                     Table table,
                     Manipulator manipulator,
                     Comparator comparator,
                     boolean scanFlag,
                     long idlePollInterval,
                     ThreadStatus threadStatus) {

      super(name,table,manipulator,comparator,scanFlag,idlePollInterval,threadStatus);
   }

// --- methods ---

   protected boolean doEntity() throws Exception {
      roll = (Roll) entity;
      return doRoll();
   }

// --- update helper ---

   protected void setFileStatus(int status) throws TableException {
      item.status = status;
      roll.recmodDate = new Date();
      table.update(roll,lock);
   }

}

