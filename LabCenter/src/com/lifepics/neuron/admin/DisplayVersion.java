/*
 * DisplayVersion.java
 */

package com.lifepics.neuron.admin;

import com.lifepics.neuron.core.Text;

import java.util.LinkedList;
import java.util.ListIterator;

/**
 * Utility class associated with IDisplayVersion.
 */

public class DisplayVersion {

   private static String stringDefault = Text.get(DisplayVersion.class,"s1");
   private static String stringInvalid = Text.get(DisplayVersion.class,"s2");

   private static class Default implements IDisplayVersion {
      public String toString() { return stringDefault; }
      public boolean isValid() { return true; }
      public LabCenterVersion getVersionObject(LabCenterVersion defaultLabCenterVersion) { return defaultLabCenterVersion; }
      public int getVersionID(LabCenterVersion defaultLabCenterVersion) { return defaultLabCenterVersion.versionID; }
      public String getVersionName(LabCenterVersion defaultLabCenterVersion) { return defaultLabCenterVersion.versionName; }
      public boolean getUseDefault() { return true; }
   }

   private static class Invalid implements IDisplayVersion {
      public String toString() { return stringInvalid; }
      public boolean isValid() { return false; }
      public LabCenterVersion getVersionObject(LabCenterVersion defaultLabCenterVersion) { throw new RuntimeException(Text.get(DisplayVersion.class,"e1")); }
      public int getVersionID(LabCenterVersion defaultLabCenterVersion) { throw new RuntimeException(Text.get(DisplayVersion.class,"e2")); }
      public String getVersionName(LabCenterVersion defaultLabCenterVersion) { return null; }
      public boolean getUseDefault() { throw new RuntimeException(Text.get(DisplayVersion.class,"e3")); }
   }

   private static IDisplayVersion versionDefault = new Default();
   private static IDisplayVersion versionInvalid = new Invalid();

   public static IDisplayVersion get(Instance instance) {
      if (instance.labCenterVersionID == null || instance.useDefaultVersion  == null) {
         return versionInvalid;
      } else if (instance.useDefaultVersion.booleanValue()) {
         return versionDefault;
         // we don't check that instance.labCenterVersionID is the default version ID,
         // but it should be.
      } else {
         return instance.labCenterVersion;
      }
   }

   public static Object[] getAll(LinkedList labCenterVersions) {
      Object[] o = new Object[labCenterVersions.size() + 2];

      int i = 0;
      o[i++] = versionDefault;

      ListIterator j = labCenterVersions.listIterator(labCenterVersions.size());
      while (j.hasPrevious()) o[i++] = j.previous();

      o[i++] = versionInvalid;
      return o;
   }

}

