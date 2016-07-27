/*
 * Backprint.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * A shared subobject class for Noritsu and Konica formats, and maybe others in the future.
 */

public class Backprint extends Structure {

// --- fields ---

   public int type;
   public String message;

// --- n-digit values ---

   private static final int NDIGIT_SEQUENCE = 3;

// --- type enumeration ---

   public static final int TYPE_MIN = 0;

   public static final int TYPE_NOTHING  = 0;
   public static final int TYPE_FILENAME = 1;
   public static final int TYPE_CUSTOM   = 2;
   public static final int TYPE_ORDER_ID = 3;
   public static final int TYPE_NAME_LAST = 4;
   public static final int TYPE_NAME_FULL = 5;
   public static final int TYPE_ORDER_SEQ = 6;
   public static final int TYPE_WHOLESALE_1 = 7;
   public static final int TYPE_WHOLESALE_2 = 8;
   public static final int TYPE_WHOLESALE_3 = 9;
   public static final int TYPE_DEALER = 10;
   public static final int TYPE_COMMENTS_1 = 11;
   public static final int TYPE_COMMENTS_2 = 12;
   public static final int TYPE_ORDER_FN = 13;

   public static final int TYPE_MAX = 13;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      Backprint.class,
      // no version
      new AbstractField[] {

         new EnumeratedField("type","Type",OrderEnum.backprintTypeType,0,TYPE_NOTHING),
         new StringField("message","Message",0,"")
      });

   protected StructureDefinition sd() { return sd; }

// --- generation ---

   /**
    * @return Null if all backprints are turned off.
    */
   public static String generate(LinkedList backprints, String delimiter,
                                 Order order, Order.Item item, Sequence sequence) throws IOException {
      String s = null;

      Iterator i = backprints.iterator();
      while (i.hasNext()) {
         Backprint b = (Backprint) i.next();
         String bs = b.generate(order,item,sequence);

         if (bs == null) continue; // bs = "" is also plausible, but then result never null
         s = (s == null) ? bs : s + delimiter + bs;
      }

      return s;
   }

   public String generate(Order order, Order.Item item, Sequence sequence) throws IOException {
      switch (type) {

      case TYPE_NOTHING:   return null;
      case TYPE_FILENAME:  return item.isMultiImage() ? "" : order.findFileByFilename(item.filename).originalFilename; // (*)
      case TYPE_CUSTOM:    return message;
      case TYPE_ORDER_ID:  return order.getFullID();
      case TYPE_NAME_LAST: return order.getLastName();
      case TYPE_NAME_FULL: return order.getFullName();
      case TYPE_ORDER_SEQ: return order.getFullID() + " " + Convert.fromIntAtLeastNDigit(NDIGIT_SEQUENCE,sequence.get(item));

      case TYPE_WHOLESALE_1:  return (order.wholesale == null) ? order.getFullID() : Convert.fromInt(order.wholesale.merchantOrderID);
      case TYPE_WHOLESALE_2:  return (order.wholesale == null) ? order.getFullID() : Text.get(Backprint.class,"s1",new Object[] { order.wholesale.merchantName, Convert.fromInt(order.wholesale.merchantOrderID) });
      case TYPE_WHOLESALE_3:  return (order.wholesale == null) ? order.getFullID() : Text.get(Backprint.class,"s2",new Object[] { order.wholesale.merchantName, Convert.fromInt(order.wholesale.merchantOrderID), order.getFullID() });
                           // see also OrderStubDialog.describeID
                           // if a wholesaler has local orders, just show the local ID

      case TYPE_DEALER:    return (order.dealerName == null) ? "" : order.dealerName;

      case TYPE_COMMENTS_1:   return extract(item.comments,/* line = */ 0);
      case TYPE_COMMENTS_2:   return extract(item.comments,/* line = */ 1);

      case TYPE_ORDER_FN:  return order.getFullID() + " " + (item.isMultiImage() ? "" : order.findFileByFilename(item.filename).originalFilename); // (*)

      default:
         throw new IOException(Text.get(this,"e3"));
      }

      // (*) multi-image products typically won't have backprinting available, but if they do,
      // be sure not to send the multi-image description through.  it should be private to LC.
   }

   private static String extract(String comments, int line) {
      String result = ""; // backprint should only be null if type is set to nothing
      if (comments != null) {
         String[] s = comments.split("\\|",-1); // -1 to stop weird default behavior
         if (line < s.length) {
            result = s[line];
         }
      }
      return result;
   }

// --- minor utilities ---

   public boolean hasAnything() {
      return (type != TYPE_NOTHING);
   }

   public static boolean hasAnything(LinkedList backprints) {
      Iterator i = backprints.iterator();
      while (i.hasNext()) {
         if (((Backprint) i.next()).hasAnything()) return true;
      }
      return false;
   }

// --- print sequence ---

   public boolean hasSequence() {
      return (type == TYPE_ORDER_SEQ);
   }

   public static boolean hasSequence(LinkedList backprints) {
      Iterator i = backprints.iterator();
      while (i.hasNext()) {
         if (((Backprint) i.next()).hasSequence()) return true;
      }
      return false;
   }

   private static class Entry {
      public int sequence;
      public Entry(int sequence) { this.sequence = sequence; }
   }
   // basically just a mutable Integer

   public static class Sequence {
      private HashMap map;
      public Sequence() { map = new HashMap(); }

      private Entry getEntry(Order.Item item) {
         Entry entry = (Entry) map.get(item.sku);
         if (entry == null) {
            entry = new Entry(1);
            map.put(item.sku,entry);
         }
         return entry;
      }

      public int get(Order.Item item) { return getEntry(item).sequence; }
      public void advance(Order.Item item) { getEntry(item).sequence++; }

      // get and advance are separate so that everything still works
      // when more than one backprint line makes use of the sequence.
   }

// --- validation ---

   public static void validate(LinkedList backprints) throws ValidationException {
      Iterator i = backprints.iterator();
      while (i.hasNext()) {
         ((Backprint) i.next()).validate();
      }
   }

   public void validate() throws ValidationException {

      if (type < TYPE_MIN || type > TYPE_MAX) {
         throw new ValidationException(Text.get(Backprint.class,"e1",new Object[] { Convert.fromInt(type) }));
      }

      // allow non-null message even for types other than custom ...
      // it's nice to preserve stuff like that even when not used.

      if (message.indexOf('\"') != -1) throw new ValidationException(Text.get(Backprint.class,"e2"));
   }

}

