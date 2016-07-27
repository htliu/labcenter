/*
 * XeroxConfig.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.*;

import java.io.File;
import java.util.LinkedList;
import java.util.regex.Pattern;

/**
 * An object that holds configuration information for the Xerox format.
 */

public class XeroxConfig extends Structure implements MappingConfig {

// --- fields ---

   public File requestDir;
   public File imageDir;
   public File mapImageDir; // nullable

   public String prefix; // alpha only
   public int useDigits;
   public boolean completeImmediately;

   public LinkedList mappings;

// --- constants ---

   private static final int NDIGIT_ORDER_MIN =  2; // my arbitrary decision
   private static final int NDIGIT_TOTAL_MAX = 12;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      XeroxConfig.class,
      0,0,
      new AbstractField[] {

         new FileField("requestDir","RequestDir",0,null),
         new FileField("imageDir","ImageDir",0,null),
         new NullableFileField("mapImageDir","MapImageDir",0,null),

         new StringField("prefix","Prefix",0,"LP"),
         new IntegerField("useDigits","UseDigits",0,6),
         new BooleanField("completeImmediately","CompleteImmediately",0,false),

         new StructureListField("mappings","Mapping",XeroxMapping.sd,Merge.IDENTITY,0,0).with(MappingUtil.skuAccessor,MappingUtil.skuComparator)
      });

   protected StructureDefinition sd() { return sd; }

// --- validation ---

   // sets of special characters to exclude, ordered by strictness, descending
   public static Pattern ruleAlpha  = Pattern.compile("[^a-zA-Z]");
   public static Pattern ruleStrict = Pattern.compile("[^a-zA-Z0-9\\x2d_]");
   public static Pattern ruleLoose  = Pattern.compile("[,|$<\\t]");
   // there's no way to enter a tab in the UI, but we might as well exclude it

   public void validate() throws ValidationException {

      // unmapped request and image dirs are only used by LC, no validation needed

   // mapped image dir validation

      File useDir = (mapImageDir != null) ? mapImageDir : imageDir;

      // expand file into ascending list of files
      LinkedList list = new LinkedList();
      while (useDir != null) {
         list.addFirst(useDir);
         useDir = useDir.getParentFile();
      }

      // the first must be the UNC marker; cf. FileMapper.getRoot
      if ( ! ((File) list.removeFirst()).getPath().equals("\\\\") ) throw new ValidationException(Text.get(this,"e6"));

      // the second is the UNC share name
      if (list.isEmpty()) throw new ValidationException(Text.get(this,"e7"));
      list.removeFirst();
      // no validation here; it can contain at least IP dots, possibly more

      // the rest must satisfy the strict rule
      while ( ! list.isEmpty() ) {
         File file = (File) list.removeFirst();
         if (ruleStrict.matcher(file.getName()).find()) throw new ValidationException(Text.get(this,"e8"));
      }

   // other validations

      if (ruleAlpha.matcher(prefix).find()) throw new ValidationException(Text.get(this,"e3"));

      if (useDigits < NDIGIT_ORDER_MIN) throw new ValidationException(Text.get(this,"e4",new Object[] { Convert.fromInt(NDIGIT_ORDER_MIN) }));

      if (prefix.length() + useDigits > NDIGIT_TOTAL_MAX) throw new ValidationException(Text.get(this,"e5",new Object[] { Convert.fromInt(NDIGIT_TOTAL_MAX) }));

      MappingUtil.validate(mappings);
   }

// --- implementation of MappingConfig ---

   public LinkedList getMappings() { return mappings; }
   public void putMappings(LinkedList mappings) { this.mappings = mappings; }

   public boolean mapsSpecialSKU() { return true; }

}

