/*
 * JobEnum.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.EnumeratedType;

/**
 * A utility class containing conversion functions for enumerated types.
 */

public class JobEnum {

// --- local utilities ---

   private static String get(String key) {
      return Text.get(JobEnum.class,key);
   }

   private static int find(String[] table, String s) {
      for (int i=0; i<table.length; i++) {
         if (s.equals(table[i])) return i;
      }
      return -1;
   }

// --- conversion functions (cf. Convert) ---

   // these depend upon the *actual numerical values* of the following enumerations
   //
   //    Job.STATUS_JOB_X   (two tables)

// --- job status (internal) ---

   private static String[] jobStatusTableInternal = {
         get("js0"),
         get("js1"),
         get("js2"),
         get("js3"),
         get("js4")
      };

   public static int toJobStatusInternal(String s) throws ValidationException {
      int i = find(jobStatusTableInternal,s);
      if (i == -1) throw new ValidationException(Text.get(JobEnum.class,"e1",new Object[] { s }));
      return i;
   }

   public static String fromJobStatusInternal(int status) {
      return jobStatusTableInternal[status];
   }

   public static EnumeratedType jobStatusInternalType = new EnumeratedType() {
      public int toIntForm(String s) throws ValidationException { return toJobStatusInternal(s); }
      public String fromIntForm(int i) { return fromJobStatusInternal(i); }
   };

// --- job status (external) ---

   private static String[] jobStatusTableExternal = {
         get("jx0"),
         get("jx1"),
         get("jx2"),
         get("jx3"),
         get("jx4")
      };

   public static String fromJobStatusExternal(int status) {
      return jobStatusTableExternal[status];
   }

}

