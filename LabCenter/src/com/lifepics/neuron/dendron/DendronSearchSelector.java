/*
 * DendronSearchSelector.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.meta.Selector;
import com.lifepics.neuron.object.Copyable;
import com.lifepics.neuron.object.CopyUtil;

import java.util.Date;

/**
 * A selector for searching for orders with various parameters.
 */

public class DendronSearchSelector implements Selector, Copyable {

// --- fields ---

   public int searchField;
   public int searchMode;
   public String searchValue;

   public Date fromDate; // inclusive
   public Date toDate;   // exclusive

// --- constants ---

   public static final int FIELD_NONE = 0;
   public static final int FIELD_ORDER_ID = 1;
   public static final int FIELD_NAME = 2;
   public static final int FIELD_EMAIL = 3;
   public static final int FIELD_PHONE = 4;

   public static final int MODE_EQUALS = 0;
   public static final int MODE_CONTAINS = 1;
   public static final int MODE_STARTS_WITH = 2;
   public static final int MODE_ENDS_WITH = 3;

// --- construction ---

   public DendronSearchSelector() {

      searchField = FIELD_NONE;
      searchMode = MODE_EQUALS;
      searchValue = "";

      // dates remain null
   }

// --- implementation of Selector ---

   /**
    * Decide whether an object belongs in the subset.
    */
   public boolean select(Object o) {
      OrderStub order = (OrderStub) o;

      if (fromDate != null &&   order.recmodDate.before(fromDate)) return false;
      if (toDate   != null && ! order.recmodDate.before(toDate  )) return false;

      if (searchField == FIELD_NONE) return true;

      if (searchField != FIELD_ORDER_ID && ! (order instanceof Order)) return false;

      String field = null;

      switch (searchField) {
      case FIELD_ORDER_ID:  field =          order .getFullID  ();  break;
      case FIELD_NAME:      field = ((Order) order).getFullName();  break;
      case FIELD_EMAIL:     field = ((Order) order).email;  break;
      case FIELD_PHONE:     field = ((Order) order).phone;  break;
      }

      // make all comparisons case-insensitive
      String fieldLC = field      .toLowerCase();
      String valueLC = searchValue.toLowerCase();

      switch (searchMode) {
      case MODE_EQUALS:
         if ( ! fieldLC.equals(valueLC) ) return false;
         break;
      case MODE_CONTAINS:
         if (fieldLC.indexOf(valueLC) == -1) return false;
         break;
      case MODE_STARTS_WITH:
         if ( ! fieldLC.startsWith(valueLC) ) return false;
         break;
      case MODE_ENDS_WITH:
         if ( ! fieldLC.endsWith(valueLC) ) return false;
         break;
      }

      return true;
   }

// --- utility functions ---

   public Object clone() throws CloneNotSupportedException { return super.clone(); }
   public DendronSearchSelector copy() { return (DendronSearchSelector) CopyUtil.copy(this); }

   public void validate() throws ValidationException {

      if (      fromDate != null
           &&   toDate   != null
           && ! fromDate.before(toDate) ) throw new ValidationException(Text.get(this,"e1"));
   }

}

