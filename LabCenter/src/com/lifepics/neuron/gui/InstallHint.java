/*
 * InstallHint.java
 */

package com.lifepics.neuron.gui;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.meta.StringSelector;
import com.lifepics.neuron.struct.*;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * An install hint, used to help resolve printer names.
 */

public class InstallHint extends Structure {

   public static String PREFIX = "todo:";

// --- fields ---

   public String printer; // including prefix
   public String pattern;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      InstallHint.class,
      // no version
      new AbstractField[] {

         new StringField("printer","Printer"),
         new StringField("pattern","Pattern")
      });

   static { sd.setAttributed(); }
   protected StructureDefinition sd() { return sd; }

// --- validation ---

   public void validate() throws ValidationException {
      try {
         StringSelector.construct(pattern);
      } catch (Exception e) {
         throw new ValidationException(Text.get(this,"e1",new Object[] { printer }),e); // yes printer not pattern
      }
   }

   public static void validate(LinkedList installHints) throws ValidationException {

      HashSet set = new HashSet();

      Iterator i = installHints.iterator();
      while (i.hasNext()) {
         InstallHint h = (InstallHint) i.next();

         h.validate();

         if ( ! set.add(h.printer) ) throw new ValidationException(Text.get(InstallHint.class,"e2",new Object[] { h.printer }));
      }
   }

   public static String getPattern(LinkedList installHints, String printer) {
      Iterator i = installHints.iterator();
      while (i.hasNext()) {
         InstallHint h = (InstallHint) i.next();
         if (h.printer.equals(printer)) return h.pattern; // case sensitive since we have full control of these
      }
      return null;
   }

}

