/*
 * LabCenterVersion.java
 */

package com.lifepics.neuron.admin;

import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.*;

/**
 * An object that holds information about a LabCenter version.
 */

public class LabCenterVersion extends Structure implements IDisplayVersion {

// --- fields ---

   public int versionID;
   public String versionName;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      LabCenterVersion.class,
      // no version
      new AbstractField[] {

         new IntegerField("versionID","VersionID"),
         new StringField("versionName","VersionName")
      });

   static { sd.setAttributed(); }
   protected StructureDefinition sd() { return sd; }

// --- validation ---

   public void validate() throws ValidationException {
   }

// --- implementation of IDisplayVersion ---

   public String toString() { return versionName; }
   public boolean isValid() { return true; }
   public LabCenterVersion getVersionObject(LabCenterVersion defaultLabCenterVersion) { return this; }
   public int getVersionID(LabCenterVersion defaultLabCenterVersion) { return versionID; }
   public String getVersionName(LabCenterVersion defaultLabCenterVersion) { return versionName; }
   public boolean getUseDefault() { return false; }

}

