/*
 * CopyUtil.java
 */

package com.lifepics.neuron.object;

import com.lifepics.neuron.core.Text;

import java.util.LinkedList;
import java.util.ListIterator;

/**
 * A class containing utilities related to cloning and copying.
 * The functions are named copy rather than clone
 * because they don't throw exceptions; see {@link Copyable}.
 */

public class CopyUtil {

   /**
    * A clone function that throws an Error (unchecked)
    * instead of a CloneNotSupportedException (checked).
    *
    * @param o The object to be cloned, which must implement {@link Copyable}.
    */
   public static Object copy(Copyable o) {
      try {
         return o.clone();
      } catch (CloneNotSupportedException e) {
         throw new Error(Text.get(CopyUtil.class,"e1"),e); // programmer error
      }
   }

   /**
    * A clone function for lists that clones the list elements
    * (unlike LinkedList.clone, which produces a shallow copy).
    *
    * @param list The list to be cloned, the elements of which must implement {@link Copyable}.
    */
   public static LinkedList copyList(LinkedList list) {
      LinkedList result = new LinkedList();

      ListIterator li = list.listIterator();
      while (li.hasNext()) {
         result.add( copy((Copyable) li.next()) );
      }

      return result;
   }

}

