/*
 * RollUtil.java
 */

package com.lifepics.neuron.axon;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.gui.GridColumn;
import com.lifepics.neuron.meta.Accessor;
import com.lifepics.neuron.meta.CompoundComparator;
import com.lifepics.neuron.meta.EditAccessor;
import com.lifepics.neuron.meta.EnumComparator;
import com.lifepics.neuron.meta.FieldComparator;
import com.lifepics.neuron.meta.NaturalComparator;
import com.lifepics.neuron.meta.NoCaseComparator;
import com.lifepics.neuron.misc.PurgeConfig;

import java.util.Comparator;
import java.util.Date;

/**
 * A utility class containing accessors, comparators, and things.
 */

public class RollUtil {

// --- accessors ---

   public static Accessor rollIDRaw = new Accessor() {
      public Class getFieldClass() { return Integer.class; }
      public Object get(Object o) { return new Integer(((Roll) o).rollID); }
   };

   public static Accessor rollID = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return Convert.fromInt(((Roll) o).rollID); }
   };

   public static Accessor sourceRaw = new Accessor() {
      public Class getFieldClass() { return Integer.class; }
      public Object get(Object o) { return new Integer(((Roll) o).source); }
   };

   public static Accessor source = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return RollEnum.fromSource(((Roll) o).source); }
   };

   public static Accessor email = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return ((Roll) o).email; }
   };

   public static Accessor rollDir = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return Convert.fromFile(((Roll) o).rollDir); }
   };

   public static Accessor statusRaw = new Accessor() {
      public Class getFieldClass() { return Integer.class; }
      public Object get(Object o) { return new Integer(((Roll) o).status); }
   };

   public static Accessor holdRaw = new Accessor() {
      public Class getFieldClass() { return Integer.class; }
      public Object get(Object o) { return new Integer(((Roll) o).hold); }
   };

   public static Accessor statusHold = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) {
         Roll roll = (Roll) o;
         if (roll.hold != Roll.HOLD_ROLL_NONE) {
            String hold = RollEnum.fromRollHold(roll.hold);
            return Text.get(RollUtil.class,"s1",new Object[] { hold });
         } else {
            return RollEnum.fromRollStatus(roll.status);
         }
      }
   };

   public static Accessor receivedDateRaw = new Accessor() {
      public Class getFieldClass() { return Date.class; }
      public Object get(Object o) { return ((Roll) o).receivedDate; }
   };

   public static Accessor receivedDate = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return Convert.fromDateExternal(((Roll) o).receivedDate); }
   };

   public static Accessor recmodDateRaw = new Accessor() {
      public Class getFieldClass() { return Date.class; }
      public Object get(Object o) { return ((Roll) o).recmodDate; }
   };

   public static Accessor recmodDate = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return Convert.fromDateExternal(((Roll) o).recmodDate); }
   };

   private static String denull(String s) { return (s == null) ? "" : s; }

   public static Accessor album = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return denull(((Roll) o).album); }
   };

   public static Accessor eventCode = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return denull(((Roll) o).eventCode); }
   };

   public static Accessor priceList = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) {
         PriceList pl = ((Roll) o).priceList;
         return (pl == null) ? "" : pl.name;
      }
   };

// --- purge date ---

   // this doesn't allow for the purge config changing while the column is visible,
   // but it is all we need for now, and much quicker than the alternatives

   private static PurgeConfig purgeConfig = null;

   public static void setPurgeConfig(PurgeConfig a_purgeConfig) {
      purgeConfig = a_purgeConfig;
   }

   /**
    * @return The purge date, or null if the roll will not be purged.
    */
   public static Date getPurgeDate(Roll roll) {
      if (purgeConfig == null) return null; // just in case

   // check status

      if ( ! (roll.isPurgeableStatus() && roll.hold == Roll.HOLD_ROLL_NONE) ) return null;

   // check whether auto-purge applies

      boolean autoPurge = false;

      switch (roll.source) {
      case Roll.SOURCE_MANUAL:  autoPurge = purgeConfig.autoPurgeManual;   break;
      case Roll.SOURCE_PAKON:   autoPurge = purgeConfig.autoPurgePakon;    break;
      case Roll.SOURCE_HOT_SIMPLE:  autoPurge = purgeConfig.autoPurgeHot;  break;
      case Roll.SOURCE_HOT_XML:     autoPurge = purgeConfig.autoPurgeHot;  break;
      case Roll.SOURCE_DLS:         autoPurge = purgeConfig.autoPurgeDLS;  break;
      case Roll.SOURCE_HOT_FUJI:    autoPurge = purgeConfig.autoPurgeHot;  break;
      case Roll.SOURCE_HOT_ORDER:   autoPurge = purgeConfig.autoPurgeHot;  break;
      case Roll.SOURCE_HOT_EMAIL:   autoPurge = purgeConfig.autoPurgeHot;  break;
      case Roll.SOURCE_HOT_IMAGE:   autoPurge = purgeConfig.autoPurgeHot;  break;
      case Roll.SOURCE_LOCAL:       autoPurge = purgeConfig.autoPurgeHot;  break;
      }

      if ( ! autoPurge ) return null;

   // compute purge date

      Date date = PurgeConfig.increment(roll.recmodDate,purgeConfig.rollPurgeInterval);

      // in practice this only happens for local images, but no reason to restrict it
      if (roll.receivedDate != null && purgeConfig.rollReceivedPurgeInterval != null) {
         Date temp = PurgeConfig.increment(roll.receivedDate,purgeConfig.rollReceivedPurgeInterval.longValue());
         if (temp.after(date)) date = temp; // don't purge unless it's OK according to both limits
      }

      return date;
   }

// --- computed fields ---

   public static Accessor itemCountRaw = new Accessor() {
      public Class getFieldClass() { return Integer.class; }
      public Object get(Object o) { return new Integer(((Roll) o).items.size()); }
   };

   public static Accessor itemCount = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return Convert.fromInt(((Roll) o).items.size()); }
   };

   public static Accessor purgeDateRaw = new Accessor() {
      public Class getFieldClass() { return Date.class; }
      public Object get(Object o) { return getPurgeDate((Roll) o); }
   };

   public static Accessor purgeDate = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return Convert.fromDateExternal(getPurgeDate((Roll) o)); }
   };

   public static Accessor localImageID = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return ((Roll) o).getLocalImageID(); }
   };
   // needs to be nullable for LocalImageImageThread use.  I thought that
   // would cause trouble for using it as a grid column, but no, it seems
   // to be fine.

// --- item fields ---

   public static Accessor itemFilename = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return ((Roll.Item) o).filename; }
   };

   public static Accessor itemStatusRaw = new Accessor() {
      public Class getFieldClass() { return Integer.class; }
      public Object get(Object o) { return new Integer(((Roll.Item) o).status); }
   };

   public static Accessor itemStatus = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return RollEnum.fromFileStatus(((Roll.Item) o).status); }
   };

   public static Accessor itemOriginalFilename = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return ((Roll.Item) o).getOriginalFilename(); }
   };

   public static Accessor itemEditableFilename = new EditAccessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return ((Roll.Item) o).getOriginalFilename(); }
      public void put(Object o, Object value) { ((Roll.Item) o).setOriginalFilename(((String) value).trim()); }
   };

   public static Accessor itemSubalbum = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return noNull( ((Roll.Item) o).subalbum ); }
   };
   private static String noNull(String s) { return (s != null) ? s : ""; }

// --- orders for enums ---

   // these depend upon the *actual numerical values* of the following enumerations
   //
   //    Roll.SOURCE_X
   //    Roll.STATUS_ROLL_X
   //    Roll.HOLD_ROLL_X
   //    Roll.STATUS_FILE_X

   private static int[] sourceOrder = { // alphabetical order
         8, // manual
         9, // Pakon
         5, // hot - simple
         6, // hot - xml
         0, // DLS
         2, // hot - fuji
         4, // hot - order
         1, // hot - email
         3, // hot - image
         7  // local
      };

   private static int[] statusOrder = { // logical order
         0, // scanned
         1, // copying
         2, // pending
         3, // sending
         5, // completed
         4  // deleted
      };

   private static int[] holdOrder = { // order by need for attention
         6, // none
         2, // email
         0, // error
         5, // user
         1, // retry
         4, // confirm
         3  // dealer
      };

   private static int[] itemStatusOrder = { // logical order
         0, // pending
         1, // sending
         2  // sent
      };

// --- columns ---

   private static GridColumn col(int n, Accessor accessor, Comparator comparator) {
      String suffix = Convert.fromInt(n);
      String name = Text.get(RollUtil.class,"n" + suffix);
      int width;
      try {
         width = Convert.toInt(Text.get(RollUtil.class,"w" + suffix));
      } catch (ValidationException e) {
         width = 1;
         // nothing we can do in a static context
      }
      return new GridColumn(name,width,accessor,comparator);
   }

   private static Comparator nc(Accessor accessor) {
      return new FieldComparator(accessor,new NaturalComparator());
   }

   private static Comparator sc(Accessor accessor) {
      return new FieldComparator(accessor,new NoCaseComparator());
   }

   private static Comparator ec(Accessor accessor, int[] order) {
      return new FieldComparator(accessor,new EnumComparator(0,order));
   }

   public static Comparator orderRollID = nc(rollIDRaw);

   public static Comparator orderStatusHold = new CompoundComparator(ec(holdRaw,holdOrder),ec(statusRaw,statusOrder));

   public static Comparator orderItemFilename = sc(itemFilename);
   public static Comparator orderItemOriginalFilename = sc(itemOriginalFilename);

   public static GridColumn colRollID     = col(1,rollID,orderRollID);
   public static GridColumn colSource     = col(4,source,ec(sourceRaw,sourceOrder));
   public static GridColumn colEmail      = col(5,email,sc(email));
   public static GridColumn colRollDir    = col(6,rollDir,sc(rollDir));
   public static GridColumn colStatusHold = col(7,statusHold,orderStatusHold);
   public static GridColumn colReceivedDate = col(18,receivedDate,nc(receivedDateRaw));
   public static GridColumn colRecmodDate = col(8,recmodDate,nc(recmodDateRaw));
   public static GridColumn colItemCount  = col(9,itemCount,nc(itemCountRaw));
   public static GridColumn colPurgeDate  = col(10,purgeDate,nc(purgeDateRaw));
   public static GridColumn colAlbum      = col(15,album,sc(album));
   public static GridColumn colEventCode  = col(16,eventCode,sc(eventCode));
   public static GridColumn colPriceList  = col(17,priceList,sc(priceList));
   public static GridColumn colLocalImageID = col(19,localImageID,sc(localImageID));

   public static GridColumn colItemFilename = col(11,itemFilename,orderItemFilename);
   public static GridColumn colItemStatus   = col(12,itemStatus,ec(itemStatusRaw,itemStatusOrder));
   public static GridColumn colItemOriginalFilename = col(13,itemOriginalFilename,orderItemOriginalFilename);
   public static GridColumn colItemEditableFilename = col(13,itemEditableFilename,orderItemOriginalFilename);
   static { colItemEditableFilename.editable = true; }
   public static GridColumn colItemSubalbum = col(14,itemSubalbum,sc(itemSubalbum));

}

