/*
 * QueueList.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Nullable;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.meta.NoCaseComparator;
import com.lifepics.neuron.struct.*;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

import org.w3c.dom.Node;

/**
 * An object that holds configuration information for all the print queues.
 */

public class QueueList extends Structure {

// --- fields ---

   public LinkedList queues; // kept in order by name
   public LinkedList mappings;
   public String defaultQueue; // nullable
   public int nextQueueID;
   public boolean enableBackup;
   public LinkedList threads;
   public Integer nextThreadID;

   // fields that are not real
   public LinkedList backupList; // Main calls precomputeBackupList before queueList is distributed

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      QueueList.class,
      0,new History(new int[] {2,757,3,769,4}),
      new AbstractField[] {

         new StructureListField("queues","Queue",Queue.sd,Merge.IDENTITY).with(Queue.idAccessor,Queue.nameComparator),

         new StructureListField("mappings","Mapping",QueueMapping.sd,Merge.IDENTITY,2,0) {
            protected void loadSpecial(Node node, Object o, int version) throws ValidationException {
               MappingUtil.migrate(tget(o),node,QueueMapping.sd);
            }
         }.with(MappingUtil.skuAccessor,MappingUtil.skuComparator),

         new NullableStringField("defaultQueue","DefaultQueue"),
         new IntegerField("nextQueueID","NextQueueID",1,0) {
            public void loadDefault(Object o) {
               tset(o,((QueueList) o).computeNextQueueID()); // note, queues already loaded
            }
         },
         new BooleanField("enableBackup","EnableBackup",3,false),
         new StructureListField("threads","Thread",ThreadDefinition.sd,Merge.IDENTITY,4,0).with(ThreadDefinition.idAccessor,ThreadDefinition.nameComparator),
         new NullableIntegerField("nextThreadID","NextThreadID",4,null)
      });

   protected StructureDefinition sd() { return sd; }

// --- queue find functions ---

   public static Queue findQueueByID(LinkedList queues, String queueID) {
      Iterator i = queues.iterator();
      while (i.hasNext()) {
         Queue q = (Queue) i.next();
         if (q.queueID.equals(queueID)) return q;
      }
      return null;
   }

   public Queue findQueueByID(String queueID) {
      return findQueueByID(queues,queueID);
   }

   public Queue findQueueByName(String name) {
      Iterator i = queues.iterator();
      while (i.hasNext()) {
         Queue q = (Queue) i.next();
         if (q.name.equals(name)) return q;
      }
      return null;
   }

   public String findQueueIDBySKU(SKU sku) {
      QueueMapping m = (QueueMapping) MappingUtil.getMapping(mappings,sku);
      if (enableBackup) {
         // check queueID, backupQueueID, and defaultQueue, in that order.
         // if a queue is defined, and not switched to backup, we're done.
         if (m != null) {
            if (/* m.queueID never null */ ! backupList.contains(m.queueID      )) return m.queueID;
            if (m.backupQueueID != null && ! backupList.contains(m.backupQueueID)) return m.backupQueueID;
         }
         if (defaultQueue != null && ! backupList.contains(defaultQueue)) return defaultQueue;
         return null;
      } else {
         return (m == null) ? defaultQueue : m.queueID;
         // the result may still be null
      }
      // it looks like I'm doing two different things, but actually
      // the enableBackup test is just a tiny optimization to avoid
      // an unnecessary call to backupList.contains.
      // if enableBackup is false, then all switchToBackup are false
      // by validation, which means backupList is empty, and in that
      // case the first block reduces to the second.
      //
      // by the way, the point of backupList is to avoid having
      // to search for the queue object to check switchToBackup.
      // 99% of the time it will be an empty list, and
      // 99% of the rest of the time it will have only one element.
      // and, 99% of the time backup won't even be enabled!
   }

   public Queue findQueueBySKU(SKU sku) {
      String id = findQueueIDBySKU(sku);
      return (id == null) ? null : findQueueByID(id);
   }

// --- queue methods ---

   private int computeNextQueueID() {

      // find the current maximum ID in the list,
      // and consider that to be the last one allocated.
      // there can't be any jobs with larger IDs,
      // because there was no delete function prior to this version.

      // this is the only place where I use the fact
      // that the IDs are really integers.
      // "OK, here and in the XMLPollerLarge pos file." -- what??
      // pollers have nothing to do with queue IDs!

      int max = -1; // start with zero if list empty

      Iterator i = queues.iterator();
      while (i.hasNext()) {
         Queue q = (Queue) i.next();

         try {
            int id = Convert.toInt(q.queueID); // get weird message if not integer
            if (id > max) max = id;
         } catch (ValidationException e) {
            // ignore error ... the ID's not a number, so we won't collide with it
         }
      }

      return max+1;
   }

   private String getNextQueueID() throws ValidationException {

      if (nextQueueID == Integer.MAX_VALUE) throw new ValidationException(Text.get(this,"e7"));
      //
      // this is something that will never happen in practice,
      // but it's important in theory (at least to me)
      // because it guarantees that a job created in one queue
      // won't end up in another because of a reused queue ID.

      return Convert.fromInt(nextQueueID++);
      //
      // it's a little strange to increment the field even before commit,
      // but then that's the kind of thing that happens with autonumbers.
   }

   private int getQueueInsertionPoint(String name) {

      // this could be done nicer with NoCaseComparator, FieldComparator,
      // and Collections.binarySearch, but it's not worth thinking about.

      Comparator comparator = new NoCaseComparator();
      int pos =  0;

      ListIterator li = queues.listIterator(); // need list iterator for nextIndex
      while (li.hasNext()) {
         Queue q = (Queue) li.next();

         int ord = comparator.compare(name,q.name);
         if (ord  > 0) pos = li.nextIndex();
         //
         // the duplicate-name case is not handled carefully,
         // so the result may be wrong ... so don't do that.
      }

      return pos;
   }

   private void validateUniqueQueueName(String name) throws ValidationException {

      Queue.validateName(name);
      if (findQueueByName(name) != null) throw new ValidationException(Text.get(this,"e5"));
   }

   public Queue createQueue(String name) throws ValidationException {
      validateUniqueQueueName(name);

      Queue q = (Queue) new Queue().loadDefault(getNextQueueID(),name);
      queues.add(getQueueInsertionPoint(name),q);

      return q;
   }

   public void renameQueue(Queue q, String name) throws ValidationException {
      validateUniqueQueueName(name);
      // note this prevents "renaming" to the same name

      removeQueue(q);
      q.name = name;
      queues.add(getQueueInsertionPoint(name),q);
   }

   public void deleteQueue(Queue q) throws ValidationException {
      removeQueue(q);

      // remove mappings
      Iterator i = mappings.iterator();
      while (i.hasNext()) {
         QueueMapping m = (QueueMapping) i.next();
         if (m.queueID.equals(q.queueID)) {
            i.remove();
            // what about backupQueueID?  the ideal would be to keep a row with null queueID
            // and have it behave like no row, but that's a lot of work for an unlikely case,
            // so dropping the row is fine.  I don't like promoting backupQueueID to queueID,
            // that seems unexpected.
         } else if (m.backupQueueID != null && m.backupQueueID.equals(q.queueID)) {
            m.backupQueueID = null;
         }
      }

      // update default
      if (q.queueID.equals(defaultQueue)) defaultQueue = null;
      // order in equals reversed because defaultQueue can be null
   }

   private void removeQueue(Queue q) throws ValidationException {

      // can't use list remove, it calls equals function

      Iterator i = queues.iterator();
      while (i.hasNext()) {
         Queue q2 = (Queue) i.next();
         if (q2 == q) { i.remove(); return; }
      }
      throw new ValidationException(Text.get(this,"e6"));
   }

   public void precomputeBackupList() {

      backupList = new LinkedList();

      Iterator i = queues.iterator();
      while (i.hasNext()) {
         Queue q = (Queue) i.next();
         if (Nullable.nbToB(q.switchToBackup)) backupList.add(q.queueID);
      }
   }

   public LinkedList getQueueSubset(String threadID) {
      LinkedList queueSubset = new LinkedList();

      Iterator i = queues.iterator();
      while (i.hasNext()) {
         Queue q = (Queue) i.next();
         if (Nullable.equals(q.threadID,threadID)) queueSubset.add(q);
      }

      return queueSubset;
   }

// --- thread find functions ---

   // see comments on queue functions, this is mostly cloned from there

   public static ThreadDefinition findThreadByID(LinkedList threads, String threadID) {
      Iterator i = threads.iterator();
      while (i.hasNext()) {
         ThreadDefinition t = (ThreadDefinition) i.next();
         if (Nullable.equals(t.threadID,threadID)) return t;
      }
      return null;
   }

   public ThreadDefinition findThreadByID(String threadID) {
      return findThreadByID(threads,threadID);
   }

   public ThreadDefinition findThreadByName(String threadName) {
      Iterator i = threads.iterator();
      while (i.hasNext()) {
         ThreadDefinition t = (ThreadDefinition) i.next();
         if (Nullable.equals(t.threadName,threadName)) return t;
      }
      return null;
   }

// --- thread methods ---

   /**
    * Doesn't handle null threadID, don't call before the threads have been validated.
    */
   private int computeNextThreadID() {

      int max = -1;

      Iterator i = threads.iterator();
      while (i.hasNext()) {
         ThreadDefinition t = (ThreadDefinition) i.next();

         try {
            int id = Convert.toInt(t.threadID);
            if (id > max) max = id;
         } catch (ValidationException e) {
            // ignore error
         }
      }

      return max+1;
   }

   private String getNextThreadID() throws ValidationException {

      int id = (nextThreadID == null) ? computeNextThreadID() : nextThreadID.intValue();
      // allow null for nextThreadID so manual thread edits work in beta,
      // and also so all the people not using it don't have to see it in the config.

      if (id == Integer.MAX_VALUE) throw new ValidationException(Text.get(this,"e17"));

      nextThreadID = new Integer(id+1);
      return Convert.fromInt(id);
   }

   private int getThreadInsertionPoint(String threadName) {

      Comparator comparator = new NoCaseComparator();
      int pos =  0;

      ListIterator li = threads.listIterator();
      while (li.hasNext()) {
         ThreadDefinition t = (ThreadDefinition) li.next();

         int ord = comparator.compare(threadName,t.threadName);
         if (ord  > 0) pos = li.nextIndex();
      }

      return pos;
   }

   private void validateUniqueThreadName(String threadName) throws ValidationException {

      ThreadDefinition.validateThreadName(threadName);
      if (findThreadByName(threadName) != null) throw new ValidationException(Text.get(this,"e15"));
   }

   public ThreadDefinition createThread(String threadName) throws ValidationException {
      validateUniqueThreadName(threadName);

      ThreadDefinition t = new ThreadDefinition();
      t.threadID = getNextThreadID();
      t.threadName = threadName;

      threads.add(getThreadInsertionPoint(threadName),t);

      return t;
   }

   public void renameThread(ThreadDefinition t, String threadName) throws ValidationException {
      validateUniqueThreadName(threadName);

      removeThread(t);
      t.threadName = threadName;
      threads.add(getThreadInsertionPoint(threadName),t);
   }

   public void deleteThread(ThreadDefinition t) throws ValidationException {
      removeThread(t);

      // remove assignments
      Iterator i = queues.iterator();
      while (i.hasNext()) {
         Queue q = (Queue) i.next();
         if (Nullable.equals(q.threadID,t.threadID)) q.threadID = null;
      }
   }

   private void removeThread(ThreadDefinition t) throws ValidationException {
      Iterator i = threads.iterator();
      while (i.hasNext()) {
         ThreadDefinition t2 = (ThreadDefinition) i.next();
         if (t2 == t) { i.remove(); return; }
      }
      throw new ValidationException(Text.get(this,"e16"));
   }

   public String getThreadValidation() {
      StringBuffer buf = new StringBuffer();

      Iterator i = threads.iterator();
      while (i.hasNext()) {
         ThreadDefinition t = (ThreadDefinition) i.next();
         if (getQueueSubset(t.threadID).size() == 0) {
            buf.append(Text.get(this,"e14a",new Object[] { t.threadName }));
            buf.append("\n");
            // threadName is validated not null, but once there's a GUI
            // for thread management, we can get here before validation.
            // so, if something goes wrong, maybe threadName could be null,
            // but if so, it's only ugly, not harmful.
         }
      }

      if (buf.length() == 0) {
         return null;
      } else {
         buf.append(Text.get(this,"e14b"));
         return buf.toString();
      }
   }

// --- validation ---

   public void validateQueue(String queueID) throws ValidationException {
      if (findQueueByID(queueID) == null) throw new ValidationException(Text.get(this,"e1",new Object[] { queueID }));
   }
   // not sure why I did this instead of using the hash map I constructed

   public void validate() throws ValidationException {

   // threads

      HashSet tids   = new HashSet();
      HashSet tnames = new HashSet();

      Iterator i = threads.iterator();
      while (i.hasNext()) {
         ThreadDefinition t = (ThreadDefinition) i.next();
         t.validate();

         if ( ! tids  .add(t.threadID  ) ) throw new ValidationException(Text.get(this,"e11",new Object[] { t.threadID   }));
         if ( ! tnames.add(t.threadName) ) throw new ValidationException(Text.get(this,"e12",new Object[] { t.threadName }));
      }

      if (nextThreadID != null && nextThreadID.intValue() < computeNextThreadID()) throw new ValidationException(Text.get(this,"e13"));

      // you'd think we'd want every thread to have some queues,
      // but I think it's best for that to be a weak validation.
      // that way during early testing we can create the threads
      // by hand-editing the config, and if you want to delete
      // a queue without dealing with threads, you can do that too.

      // note that "threads" is a list of *additional* threads!
      // the default thread is always present, and queues
      // with null threadID will be sent to the default thread.

   // queues

      if (queues.isEmpty()) throw new ValidationException(Text.get(this,"e2"));
      // no queues would be fine, but it's hard to show in the UI.

      HashSet ids   = new HashSet();
      HashSet names = new HashSet();

      boolean switchToBackup = false;

      i = queues.iterator();
      while (i.hasNext()) {
         Queue q = (Queue) i.next();
         q.validate();

         // false result means didn't add because already present
         if ( ! ids  .add(q.queueID) ) throw new ValidationException(Text.get(this,"e3",new Object[] { q.queueID }));
         if ( ! names.add(q.name   ) ) throw new ValidationException(Text.get(this,"e4",new Object[] { q.name    }));

         switchToBackup |= Nullable.nbToB(q.switchToBackup);

         if (q.threadID != null && ! tids.contains(q.threadID)) throw new ValidationException(Text.get(this,"e10",new Object[] { q.threadID }));
      }

      if (switchToBackup && ! enableBackup) throw new ValidationException(Text.get(this,"e9"));
      // don't allow in mixed state, it's too confusing

      if (nextQueueID < computeNextQueueID()) throw new ValidationException(Text.get(this,"e8"));
      // I want to make sure this doesn't happen during an auto-config

   // mappings

      MappingUtil.validate(mappings);
      // the remaining validations can't be static

      i = mappings.iterator();
      while (i.hasNext()) {
         QueueMapping m = (QueueMapping) i.next();
         validateQueue(m.queueID);
         if (m.backupQueueID != null) validateQueue(m.backupQueueID);
      }
      // would be more efficient to load into a HashSet,
      // but who cares, there aren't that many mappings.

      if (defaultQueue != null) validateQueue(defaultQueue);
   }

}

