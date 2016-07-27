/*
 * ThreadDefinition.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.meta.Accessor;
import com.lifepics.neuron.meta.FieldComparator;
import com.lifepics.neuron.meta.NoCaseComparator;
import com.lifepics.neuron.struct.*;

import java.util.Comparator;

/**
 * An object that holds configuration information for FormatThread multithreading.
 */

public class ThreadDefinition extends Structure {

// --- fields ---

   public String threadID; // actually a number
   public String threadName;

   public FormatSubsystem formatSubsystem;
   // extra field so that we can reuse ThreadDefinition objects to hold
   // all the information needed in the Global object (at the top level)

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      ThreadDefinition.class,
      // no version
      new AbstractField[] {

         new NullableStringField("threadID","ThreadID"),
         new NullableStringField("threadName","ThreadName")
      });

   static { sd.setAttributed(); }
   protected StructureDefinition sd() { return sd; }

// --- sort functions ---

   public static Accessor idAccessor = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return ((ThreadDefinition) o).threadID; }
   };

   public static Accessor nameAccessor = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return ((ThreadDefinition) o).threadName; }
   };

   public static Comparator nameComparator = new FieldComparator(nameAccessor,new NoCaseComparator());

// --- methods ---

   public static String DEFAULT_THREAD_NAME = Text.get(ThreadDefinition.class,"s1");
   public String toString() { return threadName; } // useful in thread list combo

// --- validation ---

   public static void validateThreadName(String threadName) throws ValidationException {
      if (threadName == null || threadName.length() == 0) throw new ValidationException(Text.get(ThreadDefinition.class,"e2"));
   }

   public void validate() throws ValidationException {

      if (threadID == null || threadID.length() == 0) throw new ValidationException(Text.get(this,"e1"));
      // only null for the default thread, which isn't stored in the config

      validateThreadName(threadName);
      // thread name will show up in GUI,
      // but no need to limit the length, if it's too long it just gets cropped
   }

}

