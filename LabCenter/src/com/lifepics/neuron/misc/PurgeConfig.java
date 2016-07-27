/*
 * PurgeConfig.java
 */

package com.lifepics.neuron.misc;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.object.XML;
import com.lifepics.neuron.struct.*;

import java.util.Calendar;
import java.util.Date;

import org.w3c.dom.Node;

/**
 * An object that holds configuration information for the purge subsystems.
 */

public class PurgeConfig extends Structure {

// --- fields ---

   public long rollPurgeInterval;   // millis
   public long orderPurgeInterval;  //
   public long jobPurgeInterval;    //

   public long orderStalePurgeInterval;
   public long jobStalePurgeInterval;

   public Long rollReceivedPurgeInterval;

   public boolean autoPurgeManual;  // rolls
   public boolean autoPurgePakon;   //
   public boolean autoPurgeHot;     //
   public boolean autoPurgeDLS;     //
   public boolean autoPurgeOrders;
   public boolean autoPurgeJobs;

   public boolean autoPurgeStale;
   public boolean autoPurgeStaleLocal;

   public String tryPurgeLocalImageURL;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      PurgeConfig.class,
      0,new History(new int[] {7,720,8,722,9}),
      new AbstractField[] {

         // this is a little clunky .. keep the name PurgeInterval for the
         // roll purge interval to avoid complicating the defaulting logic.

         new LongField("rollPurgeInterval","PurgeInterval-Millis",5,0) {
            protected void loadSpecial(Node node, Object o, int version) throws ValidationException {
               int days = Convert.toInt(XML.getElementText(node,"PurgeInterval-Days"));
               tset(o,days*millisPerDay);
            }
         },
         new LongField("orderPurgeInterval","OrderPurgeInterval-Millis",6,0) {
            protected void loadSpecial(Node node, Object o, int version) throws ValidationException {
               PurgeConfig c = (PurgeConfig) o;
               c.orderPurgeInterval = c.rollPurgeInterval;
            }
         },
         new LongField("jobPurgeInterval","JobPurgeInterval-Millis",5,0) {
            protected void loadSpecial(Node node, Object o, int version) throws ValidationException {
               int days;
               if (version >= 2) {
                  days = Convert.toInt(XML.getElementText(node,"JobPurgeInterval-Days"));
               } else {
                  days = 7;
               }
               tset(o,days*millisPerDay);
            }
         },
         // note, the intervals were integers before, not longs

         new LongField("orderStalePurgeInterval","OrderStalePurgeInterval-Millis",7,2592000000L), // 30 days
         new LongField(  "jobStalePurgeInterval",  "JobStalePurgeInterval-Millis",7,2592000000L),

         new NullableLongField("rollReceivedPurgeInterval","ReceivedPurgeInterval-Millis",8,null),

         new BooleanField("autoPurgeManual","AutoPurgeManual"),
         new BooleanField("autoPurgePakon","AutoPurgeScanner"), // relic name
         new BooleanField("autoPurgeHot","AutoPurgeHot",3,false),
         new BooleanField("autoPurgeDLS","AutoPurgeDLS",4,false),
         new BooleanField("autoPurgeOrders","AutoPurgeOrders",1,false),
         new BooleanField("autoPurgeJobs","AutoPurgeJobs",2,true),

         new BooleanField("autoPurgeStale",     "AutoPurgeStale",     7,false),
         new BooleanField("autoPurgeStaleLocal","AutoPurgeStaleLocal",7,false),

         new StringField("tryPurgeLocalImageURL","TryPurgeLocalImageURL",9,"https://api.lifepics.com/closed/LCService.asmx/TryPurgeLocalImage")
      });

   protected StructureDefinition sd() { return sd; }

// --- constants ---

   // three-state enumeration that describes the kind of purge that applies to an order or job
   //
   public static final int MODE_NONE   = 0;
   public static final int MODE_NORMAL = 1;
   public static final int MODE_STALE  = 2;

// --- helper functions ---

   public static Date increment(Date date, long interval) {

      // this is probably overkill, but ... originally the purge intervals
      // were stored as numbers of days, and were computed with date arithmetic,
      // so that the time would be correct even over daylight savings changes.
      // I like that behavior a lot, so preserve it ... split the interval into
      // a days part and a millis part, then add them using different methods.

      long days   = interval/millisPerDay;
      long millis = interval - days*millisPerDay;

      Calendar calendar = Calendar.getInstance();
      calendar.setTime(date);
      calendar.add(Calendar.DATE,(int) days);
      return new Date(calendar.getTimeInMillis() + millis);

      // could also use calendar.add(Calendar.MILLISECOND,millis),
      // but I like this better.  note, additions don't commute!
   }

   private static final long millisPerDay = 86400000;

// --- validation ---

   public void validate() throws ValidationException {

      if ( rollPurgeInterval < 0) throw new ValidationException(Text.get(this,"e1"));
      if (orderPurgeInterval < 0) throw new ValidationException(Text.get(this,"e2"));
      if (  jobPurgeInterval < 0) throw new ValidationException(Text.get(this,"e3"));

      if (orderStalePurgeInterval <= 0) throw new ValidationException(Text.get(this,"e4"));
      if (  jobStalePurgeInterval <= 0) throw new ValidationException(Text.get(this,"e5"));
      // zero would mean instant purge of all entries, don't allow that.
      // rely on interval field UI to prevent other unreasonably small values.

      if (rollReceivedPurgeInterval != null && rollReceivedPurgeInterval.longValue() < 0) throw new ValidationException(Text.get(this,"e6"));
   }

}

