/*
 * BandwidthConfig.java
 */

package com.lifepics.neuron.net;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Nullable;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.meta.Accessor;
import com.lifepics.neuron.meta.FieldComparator;
import com.lifepics.neuron.meta.NaturalComparator;
import com.lifepics.neuron.object.CopyUtil;
import com.lifepics.neuron.struct.*;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import org.w3c.dom.Node;

/**
 * An object that holds configuration information for bandwidth limiting.
 */

public class BandwidthConfig extends Structure {

// --- fields ---

   // fields that are not real
   public boolean trivial;
   public boolean trivialRestrict;

   public LinkedList schedules; // ok, technically schedule entries
   public int delayCap;

// --- sort helpers ---

   // these have to come first to avoid forward reference problem

   public static Accessor entryNumberAccessor = new Accessor() {
      public Class getFieldClass() { return Integer.class; }
      public Object get(Object o) { return new Integer(((Schedule) o).entryNumber); }
   };

   public static Comparator entryNumberComparator = new FieldComparator(entryNumberAccessor,new NaturalComparator());

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      BandwidthConfig.class,
      0,0,
      new AbstractField[] {

         new StructureListField("schedules","Schedule",Schedule.sd,Merge.IDENTITY,0,0) { // custom loadDefault
            public void loadDefault(Object o) {
               LinkedList list = tget(o);
               list.add(new Schedule().loadDefault(100,"Day",   8*60)); // 8 AM
               list.add(new Schedule().loadDefault(200,"Night",20*60)); // 8 PM
            }
         }.with(entryNumberAccessor,entryNumberComparator),
         //
         // interesting merge case.  the name is kind of a key, but it might
         // be editable someday, and it doesn't determine the entry order.
         // the time is a key, but it's already editable, and only determines the
         // entry order up to rotation.  position in the list is what determines
         // the entry order, but the merge has no way to get a handle on that.
         // and, if we add a field that's basically "position in the list",
         // then merging inserts into different positions would cause trouble.
         // so, I added entryNumber just to give us reasonable merge behavior.
         // still not perfect because it's limited to int granularity, but it
         // isn't too bad.  in practice we'll probably always have two entries.

         new IntegerField("delayCap","DelayCap-Millis",0,60000) // 1 minute
      });

   protected StructureDefinition sd() { return sd; }

// --- copy function ---

   public BandwidthConfig copy() { return (BandwidthConfig) CopyUtil.copy(this); }

// --- validation ---

   public void validate() throws ValidationException {

      if (schedules.size() < 2) throw new ValidationException(Text.get(this,"e3"));
      // maybe one could work too, but it complicates the logic, don't bother

      HashSet names = new HashSet();

      Schedule first = null;
      Schedule prev  = null;
      int timeDistance = 0;
      boolean nonzero = false;
      boolean uncheck = false;

      Iterator i = schedules.iterator();
      while (i.hasNext()) {
         Schedule schedule = (Schedule) i.next();

         // single-entry validation
         schedule.validate();

         // names must be unique
         if ( ! names.add(schedule.entryName) ) throw new ValidationException(Text.get(this,"e4",new Object[] { schedule.entryName }));

         // ordered pair validation
         if (first == null) {
            first = schedule;
         } else {
            timeDistance += validatePair(prev,schedule,/* wrap = */ false);
         }

         // nonzero validation
         if (schedule.bandwidthPercent != 0) nonzero = true;
         if (schedule.bandwidthPercent != 0 && ! schedule.isRestricted()) uncheck = true;

         prev = schedule;
      }

      // final ordered pair validation
      timeDistance += validatePair(prev,first,/* wrap = */ true);

      if (timeDistance != Convert.MINUTES_PER_DAY) throw new ValidationException(Text.get(this,"e5"));
      // it has to be a multiple of 1440, we just want to be sure it's only one.
      // there's no way to get this error unless you have three or more entries!

      if ( ! nonzero ) throw new ValidationException(Text.get(this,"e9"));
      if ( ! uncheck ) throw new ValidationException(Text.get(this,"e10"));
      // kind of technical, but if they're all zero the inactivity wait is undefined

      if (delayCap < 0) throw new ValidationException(Text.get(this,"e6"));
      // might as well allow zero ... we have to handle the zero wait case anyway,
      // since a high percentage and fast transfer rate could also produce it.
   }

   /**
    * @return The time distance increment.
    */
   private static int validatePair(Schedule s1, Schedule s2, boolean wrap) throws ValidationException {

      // numbers must be strictly increasing (except at the wrap point), therefore unique
      if (s2.entryNumber <= s1.entryNumber && ! wrap) throw new ValidationException(Text.get(BandwidthConfig.class,"e7",new Object[] { Convert.fromInt(s1.entryNumber), Convert.fromInt(s2.entryNumber) }));

      // adjacent times must not be the same (same for nonadjacent, but we detect that with timeDistance)
      // this is the error users will actually see for just two entries
      if (s2.startTime == s1.startTime) throw new ValidationException(Text.get(BandwidthConfig.class,"e8",new Object[] { Convert.fromTime(s2.startTime) }));

      // figure out time distance increment
      return Convert.deltaTime(s2.startTime,s1.startTime);
   }

// --- methods ---

   /**
    * A bandwidth config is trivial if it says to run at full speed all the time.
    */
   public void precomputeTrivial() {

      trivial = true;
      trivialRestrict = true;

      Iterator i = schedules.iterator();
      while (i.hasNext()) {
         Schedule schedule = (Schedule) i.next();

         if (schedule.bandwidthPercent != 100) trivial = false;

         if (schedule.bandwidthPercent != 0 && schedule.isRestricted()) trivialRestrict = false;
         // from validation we know that there's a nonrestricted one,
         // so if there's a restricted one too, then the schedule is nontrivial.
      }
   }

// --- schedule class ---

   // if we ever have something besides bandwidth limiting that runs on
   // a schedule, we could make a superclass with just the number, name,
   // and time plus some common utility functions.

   public static class Schedule extends Structure {

      public int entryNumber;
      public String entryName;
      public int startTime;
      public int bandwidthPercent;
      public Boolean restrict; // upload only, but not worth subclassing

      public static final StructureDefinition sd = new StructureDefinition(

         Schedule.class,
         // no version
         new AbstractField[] {

            new IntegerField("entryNumber","EntryNumber",0,0), // custom loadDefault
            new StringField("entryName","EntryName",0,null),   //
            new IntegerField("startTime","StartTime",0,0) {    //
               protected void loadNormal(Node node, Object o) throws ValidationException {
                  tset(o,Convert.toTime(getElementText(node,xmlName)));
               }
               public void store(Node node, Object o) {
                  createElementText(node,xmlName,Convert.fromTime(tget(o)));
               }
               // this could be another generic field type (TimeField),
               // but I'm tired of setting them up for now.
            },
            new IntegerField("bandwidthPercent","BandwidthPercent",0,100),
            new NullableBooleanField("restrict","Restrict",0,null)
         });

      protected StructureDefinition sd() { return sd; }

      public void validate() throws ValidationException {

         Convert.validateTime(startTime);

         if (bandwidthPercent < 0 || bandwidthPercent > 100) {
            throw new ValidationException(Text.get(BandwidthConfig.class,"e2",new Object[] { Convert.fromInt(bandwidthPercent) }));
         }
      }

      public boolean isRestricted() {
         return Nullable.nbToB(restrict);
      }

      public void setRestricted(boolean b) {
         restrict = Nullable.bToNb(b);
      }

      public Object loadDefault(int entryNumber, String entryName, int startTime) {
         loadDefault();

         this.entryNumber = entryNumber;
         this.entryName = entryName;
         this.startTime = startTime;

         return this; // convenience
      }
   }

}

