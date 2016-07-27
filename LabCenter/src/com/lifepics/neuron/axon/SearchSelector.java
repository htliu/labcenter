/*
 * SearchSelector.java
 */

package com.lifepics.neuron.axon;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.meta.Selector;
import com.lifepics.neuron.object.Copyable;
import com.lifepics.neuron.object.CopyUtil;

import java.util.Date;

/**
 * A selector for searching for rolls with various parameters.
 */

public class SearchSelector implements Selector, Copyable {

// --- fields ---

   public int searchField;
   public int searchMode;
   public String searchValue;

   public Date fromDate; // inclusive
   public Date toDate;   // exclusive

// --- constants ---

   public static final int FIELD_NONE = 0;
   public static final int FIELD_ROLL_ID = 1;
   public static final int FIELD_EMAIL = 2;
   public static final int FIELD_ALBUM = 3;
   public static final int FIELD_EVENT_CODE = 4;
   public static final int FIELD_PRICE_LIST = 5;
   public static final int FIELD_IMAGE_ID = 6;

   public static final int MODE_EQUALS = 0;
   public static final int MODE_CONTAINS = 1;
   public static final int MODE_STARTS_WITH = 2;
   public static final int MODE_ENDS_WITH = 3;

// --- construction ---

   public SearchSelector() {

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
      Roll roll = (Roll) o;

      if (fromDate != null &&   roll.recmodDate.before(fromDate)) return false;
      if (toDate   != null && ! roll.recmodDate.before(toDate  )) return false;

      if (searchField == FIELD_NONE) return true;

      String field = null;

      switch (searchField) {
      case FIELD_ROLL_ID:  field = Convert.fromInt(roll.rollID);  break;
      case FIELD_EMAIL:    field = roll.email;  break;
      case FIELD_ALBUM:       field = (roll.album == null) ? "" : roll.album;  break;
      case FIELD_EVENT_CODE:  field = (roll.eventCode == null) ? "" : roll.eventCode;  break;
      case FIELD_PRICE_LIST:  field = (roll.priceList == null) ? "" : roll.priceList.name;  break;
      case FIELD_IMAGE_ID:    field = roll.getLocalImageID();  if (field == null) field = "";  break;
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
   public SearchSelector copy() { return (SearchSelector) CopyUtil.copy(this); }

   public void validate() throws ValidationException {

      if (      fromDate != null
           &&   toDate   != null
           && ! fromDate.before(toDate) ) throw new ValidationException(Text.get(this,"e1"));
   }

}

