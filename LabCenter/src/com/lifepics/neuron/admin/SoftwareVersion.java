/*
 * SoftwareVersion.java
 */

package com.lifepics.neuron.admin;

import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.*;

/**
 * An object that holds information about a software version.
 */

public class SoftwareVersion extends Structure {

// --- fields ---

   public int softwareID; // for refresh
   public String softwareName; // since this is what LC binds to
   public int versionID; // PK
   public String versionName;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      SoftwareVersion.class,
      0,0,
      new AbstractField[] {

         new IntegerField("softwareID","SoftwareID"),
         new StringField("softwareName","SoftwareName"),
         new IntegerField("versionID","VersionID"),
         new StringField("versionName","VersionName")
      });

   protected StructureDefinition sd() { return sd; }

// --- validation ---

   public void validate() throws ValidationException {
   }

}

