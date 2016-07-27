/*
 * AlternatingFileAutoNumber.java
 */

package com.lifepics.neuron.table;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Log;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.util.logging.Level;

/**
 * An integer auto-number stored in an alternating file.
 */

public class AlternatingFileAutoNumber implements AutoNumber {

// --- fields ---

   private File file;
   private int next;

// --- construction ---

   public AlternatingFileAutoNumber(File file) throws Exception {
      this.file = file;
      next = load();
   }

   /**
    * Special constructor that creates the file if not present.
    * You should only use this if you're trying to auto-update.
    */
   public AlternatingFileAutoNumber(File file, int next) throws Exception {
      this.file = file;
      if (new AlternatingFile(file).exists()) {
         this.next = load();
      } else {
         this.next = next;
         store(next);
      }
   }

// --- implementation of AutoNumber ---

   /**
    * Get the next key, without advancing.
    */
   public String getKey() {
      return Convert.fromInt(next);
   }

   public int getNextInteger() {
      return next;
   }

   /**
    * Advance to the next key.
    */
   public void advance() {
      next++;
      tryStore();
   }

   public void advanceBy(int n) {
      next += n;
      tryStore();
   }

   public void advanceTo(int next) {
      this.next = next;
      tryStore();
   }

   private void tryStore() {
      try {
         store(next);
      } catch (Exception e) {
         Log.log(Level.WARNING,this,"e2",e);
      }
   }

// --- helpers ---

   public static String readAll(Reader reader, int max) throws IOException {

      char[] c = new char[max];
      int off = 0;
      int len = max;

      // read until we reach EOF, which is good,
      // or the buffer is full, which is an error

      while (true) {
         int count = reader.read(c,off,len);
         if (count == -1) break;
         off += count;
         len -= count;
         if (len == 0) throw new IOException(Text.get(AlternatingFileAutoNumber.class,"e1"));
      }

      return new String(c,0,off);
   }

   private int load() throws Exception {
      AlternatingFile af = new AlternatingFile(file);
      try {
         InputStream inputStream = af.beginRead();
         InputStreamReader reader = new InputStreamReader(inputStream);
         return Convert.toInt(readAll(reader,30));
         // the length 30 is arbitrary, but more than enough for any 32-bit integer
         // strange but true: the finally clause runs even if we return
      } catch (Exception e) {
         throw new Exception(Text.get(this,"e3",new Object[] { file.getName() }),e);
      } finally {
         af.endRead();
      }
   }

   private void store(int i) throws Exception {
      AlternatingFile af = new AlternatingFile(file);
      try {
         OutputStream outputStream = af.beginWrite();
         OutputStreamWriter writer = new OutputStreamWriter(outputStream);
         writer.write(Convert.fromInt(i));
         writer.flush();
         af.commitWrite();
      } catch (Exception e) {
         throw new Exception(Text.get(this,"e4",new Object[] { file.getName() }),e);
      } finally {
         af.endWrite();
      }
   }

}

