/*
 * XMLPollerLarge.java
 */

package com.lifepics.neuron.axon;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.table.AlternatingFileAutoNumber;
import com.lifepics.neuron.table.AutoNumber;

import java.io.File;
import java.io.FileFilter;
import java.util.Iterator;

import org.w3c.dom.Node;

/**
 * A variant of {@link XMLPoller} that can resume processing in mid-file.
 */

public class XMLPollerLarge extends RenamePoller implements FileFilter {

   // there's a lot of overlap with PrefixPoller,
   // but I can't see any nice way to factor it.

// --- constants ---

   private static final String PREFIX_TEMP = "temp_";
   private static final String PREFIX_DONE = "done_";

   private static final String SUFFIX_XML = ".xml";
   private static final String SUFFIX_POS = ".pos";

// --- implementation of FileFilter ---

   public boolean accept(File file) {

      if (file.isDirectory()) return false;

      String name = file.getName();

      // note, we create the prefixes, no need to ignore case there
      if (    name.startsWith(PREFIX_TEMP)
           || name.startsWith(PREFIX_DONE) ) return false;

      if ( ! endsWithIgnoreCase(name,SUFFIX_XML) ) return false;
      // this excludes SUFFIX_POS, no need to mention it explicitly

      return true;
   }

// --- implementation of hooks ---

   public FileFilter getFileFilter() { return this; }

   protected String getCore(String name) { return name; }

   protected String getName(String core, boolean done, int rollID) {
      return (done ? PREFIX_DONE : PREFIX_TEMP) + core;
   }

// --- utilities ---

   private File getPosFile(File file) {
      // file guaranteed to end with SUFFIX_XML, just replace it
      String name = file.getName();
      name = name.substring(0,name.length()-SUFFIX_XML.length()) + SUFFIX_POS;
      return new File(file.getParentFile(),name);
   }

// --- main functions ---

   protected int processRenamed(File original, File file, PollCallback callback) throws Exception {

      File base = file.getParentFile();
      AutoNumber autoNumber = new AlternatingFileAutoNumber(getPosFile(file),0);
      //
      // the constructor above reads the pos file if present,
      // or creates it if not, with initial index value of 0.
      //
      // also note the pos file will have a temp prefix.
      // I think that's right ... if the user ever sees these files,
      // the xml file will have a temp prefix too,
      // and it's best if the two of them sort close together.

      Iterator i = XMLPoller.getRolls(file);

      // in XMLPoller I had to catch the no-roll case,
      // because I needed a roll ID for the file name,
      // but here it's fine, no harm done.

      // skip to current pos
      int pos = Convert.toInt(autoNumber.getKey());
      if (pos < 0) throw new Exception(Text.get(this,"e2")); // for completeness
      for (int j=0; j<pos; j++) {
         if (i.hasNext()) i.next();
         else throw new Exception(Text.get(this,"e1"));
         // this isn't entirely cosmetic ...
         // the iterator returned by XML.getElements
         // doesn't work unless you call hasNext too
      }

      while (i.hasNext()) {
         Node node = (Node) i.next();
         callback.throttle();
         callback.submitWithItems(XMLPoller.parse(node,base,callback),RollManager.METHOD_COPY,null); // ignore ID
         autoNumber.advance();
         // no need to look at the number, just advance
      }

      return 0; // not used except it comes back as rollID in getName
   }

   protected void finish(File original, File file) {
      getPosFile(file).delete(); // ignore result

      // note, this is slightly non-transactional, in that we mark
      // the main file done before we delete the pos file.
      // but how else could we do it?  if we delete the pos file first,
      // the intermediate state would have a temp file with no pos,
      // and then we'd be in danger of re-processing the whole thing.
   }

}

