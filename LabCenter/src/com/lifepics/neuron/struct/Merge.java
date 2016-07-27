/*
 * Merge.java
 */

package com.lifepics.neuron.struct;

import com.lifepics.neuron.meta.Accessor;
import com.lifepics.neuron.object.Copyable;
import com.lifepics.neuron.object.CopyUtil;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * A utility class for various merge-related functions.
 */

public class Merge {

// --- constants ---

   public static final int EQUALITY = 0;
   public static final int IDENTITY = 1;
   public static final int POSITION = 2;
   public static final int NO_MERGE = 3;

// --- by equality ---

   /**
    * Merge two lists of objects using only the concept of equality.
    * So, elements can be added and removed, but not edited,
    * and they don't have to appear in any particular order.
    * Duplicates are handled, although that shouldn't happen in practice.
    *
    * @param dest This argument is special ... if dest turns out to be
    *             the result of the merge, it's returned without copying.
    * @param copy True if the elements are mutable and need to be copied.
    */
   public static LinkedList mergeByEquality(LinkedList dest, LinkedList base, LinkedList src, boolean copy) {

   // avoid messing up the order, if possible

      if (src.equals(dest)) return dest; // end results agree, use dest
      if (src.equals(base)) return dest; // no changes to src, use dest
      if (dest.equals(base)) return copy ? CopyUtil.copyList(src) : new LinkedList(src);
                                         // no changes to dest, use src

   // OK, there are changes on both sides, give up on preserving order

      LinkedList result = new LinkedList();

      LinkedList temp1 = new LinkedList(dest);
      LinkedList temp2 = new LinkedList(src);
      //
      // we need copies of the lists so we can alter them,
      // but they don't need to be deep copies,
      // we'll take care of that on an item-by-item basis.

   // first deal with elements that exist in the base.
   // if removed on one side, the change goes through;
   // if removed on both .. well, they agree about it!

      Iterator i = base.iterator();
      while (i.hasNext()) {
         Object o = i.next();
         boolean in1 = temp1.remove(o);
         boolean in2 = temp2.remove(o);
         if (in1 && in2) result.add(copy ? CopyUtil.copy((Copyable) o) : o);
      }

   // now temp1 and temp2 contain only added elements;
   // just be sure to cover the case where they agree.

      while ( ! temp1.isEmpty() ) {
         Object o = temp1.removeFirst();
         temp2.remove(o); // can fail
         result.add(o); // no need to copy dest elements
      }

      while ( ! temp2.isEmpty() ) {
         Object o = temp2.removeFirst();
         // temp1 is known empty
         result.add(copy ? CopyUtil.copy((Copyable) o) : o);
      }

      return result;
   }

// --- by identity ---

   /**
    * Merge two structure lists.  The elements have a unique ID,
    * so we can think of the lists as being a bunch of NullableStructureFields,
    * one per possible ID value.  Then all we need to do is order the elements.
    *
    * @param dest This argument is special ... if dest turns out to be
    *             the result of the merge, it's returned without copying.
    * @param accessor The accessor for the unique ID.
    */
   public static LinkedList mergeByIdentity(LinkedList dest, LinkedList base, LinkedList src,
                                            StructureDefinition sd,
                                            Accessor accessor, Comparator comparator) {

   // handle the easy cases first

      if (sd.equalsElements(src,dest)) return dest; // end results agree, use dest
      if (sd.equalsElements(src,base)) return dest; // no changes to src, use dest
      if (sd.equalsElements(dest,base)) return CopyUtil.copyList(src);
                                                    // no changes to dest, use src

   // there are changes on both sides, use full algorithm
   //
   // (1) gather IDs

      HashSet set = new HashSet();

      load(dest,accessor,set);
      load(base,accessor,set);
      load(src, accessor,set);

   // (2) merge each ID

      LinkedList result = new LinkedList();

      Iterator i = set.iterator();
      while (i.hasNext()) {
         Object id = i.next();

   // (a) merge into valDest

         Object valDest = get(dest,accessor,id);
         Object valBase = get(base,accessor,id);
         Object valSrc  = get(src, accessor,id);
         if (valDest != null && valBase != null && valSrc != null) {
            sd.merge(valDest,valBase,valSrc);
         } else {
            if ( ! sd.equalsNullable(valSrc,valBase) ) valDest = copyNullable(valSrc);
         }
         // this is the null-pattern from NullableStructureField

   // (b) put valDest in result

         if (valDest != null) result.add(valDest);
      }

   // (3) sort the results

      Collections.sort(result,comparator);

      return result;
   }

// --- by position ---

   /**
    * Merge two structure lists, comparing elements in the same position.
    *
    * @param dest This argument is special ... if dest turns out to be
    *             the result of the merge, it's returned without copying.
    */
   public static LinkedList mergeByPosition(LinkedList dest, LinkedList base, LinkedList src,
                                            StructureDefinition sd) {

   // handle the easy cases first

      if (sd.equalsElements(src,dest)) return dest; // end results agree, use dest
      if (sd.equalsElements(src,base)) return dest; // no changes to src, use dest
      if (sd.equalsElements(dest,base)) return CopyUtil.copyList(src);
                                         // no changes to dest, use src

   // there are changes on both sides, use full algorithm

      LinkedList result = new LinkedList();

      Iterator iDest = dest.iterator();
      Iterator iBase = base.iterator();
      Iterator iSrc  = src .iterator();

      // we could leave base out of the max-size calculation,
      // since extra base elements are deleted on both sides,
      // but I like the uniformity.
      //
      int n = Math.max(dest.size(),Math.max(base.size(),src.size()));
      while (n-- > 0) {

   // (a) merge into valDest

         Object valDest = iDest.hasNext() ? iDest.next() : null;
         Object valBase = iBase.hasNext() ? iBase.next() : null;
         Object valSrc  = iSrc .hasNext() ? iSrc .next() : null;
         if (valDest != null && valBase != null && valSrc != null) {
            sd.merge(valDest,valBase,valSrc);
         } else {
            if ( ! sd.equalsNullable(valSrc,valBase) ) valDest = copyNullable(valSrc);
         }
         // this is the null-pattern from NullableStructureField

   // (b) put valDest in result

         if (valDest != null) result.add(valDest);
      }

      return result;
   }

// --- utilities ---

   /**
    * Load the IDs of the objects into the hash set.
    */
   private static void load(LinkedList list, Accessor accessor, HashSet set) {
      Iterator i = list.iterator();
      while (i.hasNext()) {
         Object o = i.next();
         set.add(accessor.get(o));
      }
   }

   /**
    * Find the element, if any, with the same ID.
    */
   private static Object get(LinkedList list, Accessor accessor, Object id) {
      Iterator i = list.iterator();
      while (i.hasNext()) {
         Object o = i.next();
         if (accessor.get(o).equals(id)) return o;
      }
      return null;
   }

   /**
    * Make a copy of an object that might be null.
    */
   private static Object copyNullable(Object o) {
      return (o == null) ? null : CopyUtil.copy((Copyable) o);
   }

}

