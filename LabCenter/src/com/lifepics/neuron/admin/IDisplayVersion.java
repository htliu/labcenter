/*
 * IDisplayVersion.java
 */

package com.lifepics.neuron.admin;

/**
 * An instance object has two fields that describe the
 * LC version, labCenterVersionID and useDefaultVersion,
 * but we display and edit them as a single column with
 * type IDisplayVersion.  Possible values are a fixed
 * version (represented by some LabCenterVersion object),
 * the default version, and invalid.
 */

public interface IDisplayVersion {

   /**
    * Object.toString is used to display the version in combo boxes.
    */
   String toString();

   /**
    * Whether the version is valid.  I don't allow making the version invalid.
    */
   boolean isValid();

   /**
    * Get the version object.
    * The invalid version will throw a runtime exception.
    */
   LabCenterVersion getVersionObject(LabCenterVersion defaultLabCenterVersion);

   /**
    * Get the version ID.
    * The invalid version will throw a runtime exception.
    */
   int getVersionID(LabCenterVersion defaultLabCenterVersion);

   /**
    * Get a version string that can be passed to the AppVersion
    * functions.  A fixed version will return its name,
    * the default version will return the snapshot default that's
    * passed in, and invalid will return null.
    */
   String getVersionName(LabCenterVersion defaultLabCenterVersion);

   /**
    * Get the default flag.
    * The invalid version will throw a runtime exception.
    */
   boolean getUseDefault();

}

