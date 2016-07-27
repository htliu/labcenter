/*
 * PrefixPoller.java
 */

package com.lifepics.neuron.axon;

import com.lifepics.neuron.core.Convert;

import java.io.File;
import java.io.FileFilter;
import java.util.regex.Pattern;

/**
 * A subclass of {@link RenamePoller} that renames target files in a particular way.
 */

public abstract class PrefixPoller extends RenamePoller implements FileFilter {

// --- fields ---

   protected String prefixTemp;
   protected String prefixDone;
   private boolean wantDirectories;

   private Pattern patternDone;

// --- constants ---

   protected static final char MARK = '_';

// --- construction ---

   protected PrefixPoller(boolean wantDirectories) {
      this("temp","roll",wantDirectories);
   }

   protected PrefixPoller(String prefixTemp, String prefixDone, boolean wantDirectories) {

      this.prefixTemp = prefixTemp + MARK;
      this.prefixDone = prefixDone;
      this.wantDirectories = wantDirectories;

      patternDone = Pattern.compile(prefixDone + "[0-9]*" + MARK + ".*");
      //
      // this is intentionally a bit wider than what is actually produced
      // it matches things with no digits and things with leading zeros
   }

// --- implementation of FileFilter ---

   public boolean accept(File file) {

      if (file.isDirectory() != wantDirectories) return false;

      // note, we create the prefixes, no need to ignore case there
      String name = file.getName();
      if (    name.startsWith(prefixTemp)
           || patternDone.matcher(name).matches() ) return false;

      return true;
   }

// --- implementation of hooks ---

   public FileFilter getFileFilter() { return this; }

   protected String getCore(String name) { return name; }

   protected String getName(String core, boolean done, int rollID) {
      String prefix = done ? (prefixDone + Convert.fromInt(rollID) + MARK) : prefixTemp;
      return prefix + core;
   }

   // processRenamed is left for further subclasses

}

