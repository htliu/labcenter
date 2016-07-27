/*
 * FujiPoller.java
 */

package com.lifepics.neuron.axon;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;

/**
 * A poller that accepts files and directories in Fuji format.
 */

public class FujiPoller extends FixedPrefixPoller {

   private static final String SUFFIX_TXT = ".txt";

   public boolean accept(File file) {
      return super.accept(file) && endsWithIgnoreCase(file.getName(),SUFFIX_TXT);
   }

// --- main function ---

   private static String getNotNull(HashMap map, String key) throws Exception {
      String value = (String) map.get(key);
      if (value == null) throw new Exception(Text.get(FujiPoller.class,"e3",new Object[] { key }));
      return value;
   }

   protected int processRenamed(File original, File file, PollCallback callback) throws Exception {

      HashMap map = load(file);

   // make sure all relevant fields exist (they may still be empty)

      String fieldOrderID  = getNotNull(map,"OrderID");
      String fieldRollID   = getNotNull(map,"RollID");
      String fieldClaimNo  = getNotNull(map,"ClaimNo");
      String fieldName     = getNotNull(map,"Name");
      String fieldEmail    = getNotNull(map,"Email");

   // validate some things

      // we know original file name ends with SUFFIX_TXT,
      // now make sure the rest matches the order ID.
      // the comparison is not as direct as one might like,
      // to deal with the possibilities of different cases
      // in the suffix text.

      if (    !  original.getName().startsWith(fieldOrderID)
           || ! (original.getName().length() == fieldOrderID.length() + SUFFIX_TXT.length()) ) throw new Exception(Text.get(this,"e4"));

      File dir = new File(file.getParentFile(),fieldOrderID);
      if ( ! dir.isDirectory() ) throw new Exception(Text.get(this,"e5",new Object[] { Convert.fromFile(dir) }));

      String first = null;
      String last = null;

      int i1 = fieldName.indexOf(',');
      if (i1 != -1) {
         int i2 = fieldName.indexOf(',',i1+1);
         if (i2 == -1) {
            last  = fieldName.substring(0,i1); // fieldName is last,first
            first = fieldName.substring(i1+1);
         }
      }
      // if we can't parse it, whether too few or too many commas,
      // just ignore it and leave the names null, so that we don't
      // get uploads conking out on unmonitored systems.

   // create roll

      Roll roll = new Roll();

      roll.source = Roll.SOURCE_HOT_FUJI;
      roll.email = fieldEmail;

      roll.nameFirst = first;
      roll.nameLast  = last;
      roll.album = (fieldRollID.length() == 0) ? null : Roll.getValidAlbum(fieldRollID);
      roll.claimNumber = (fieldClaimNo.length() == 0) ? null : fieldClaimNo;

      // note, album and claim number are nullable, email is not ...
      // that's why length 0 is handled differently in each case.

      roll.rollDir = dir;

      File[] files = dir.listFiles();
      // if there are subdirectories, they will be listed, and the poll will error out.
      // this is the desired behavior ... the user might not notice if we ignored them.

      roll.extraFiles.add(new File("..",getFinalName(original)).getPath());
      // this is fairly awkward, but it produces the right result

      // this is almost perfect, but not quite ... there's a small window
      // between when the roll is created (containing the done form of the
      // text file name) and when the text file is renamed to that name.

      return callback.submitInPlace(roll,files); // this logs on success
   }

// --- load function ---

   private static HashMap load(File file) throws Exception {

      // there's some similar code in java.util.Properties,
      // but I wanted the parsing to be fairly strict.
      // in particular, I'm worried there could be an end-of-line backslash
      // in a Fuji file, which the properties would read as a continuation.

      HashMap map = new HashMap();

      FileReader fileReader = new FileReader(file);
      try {

         BufferedReader reader = new BufferedReader(fileReader);
         while (true) {
            String line = reader.readLine();
            if (line == null) break;

            // don't trim, we don't expect or allow extra whitespace here
            if (line.length() == 0) continue; // do allow blank lines though

            int i = line.indexOf('=');
            if (i == -1) throw new Exception(Text.get(FujiPoller.class,"e1"));

            String key   = line.substring(0,i);
            String value = line.substring(i+1);

            if (map.put(key,value) != null) throw new Exception(Text.get(FujiPoller.class,"e2",new Object[] { key }));
         }

      } finally {
         fileReader.close();
         // why close this?  closing the BufferedReader would also close the FileReader,
         // <i>unless</i> we somehow got an error while constructing the BufferedReader.
      }

      return map;
   }

}

