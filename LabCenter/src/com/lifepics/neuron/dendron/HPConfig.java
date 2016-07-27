/*
 * HPConfig.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.*;

import java.io.File;
import java.util.LinkedList;

/**
 * An object that holds configuration information for the HP format.
 */

public class HPConfig extends Structure implements MappingConfig {

// --- fields ---

   public File dataDir;

   public int priority;
   public String prefix; // digits, not alpha like other prefixes
   public int useDigits;
   public String dealerID;
   public String dealerName;

   public LinkedList backprints;
   public LinkedList mappings;

// --- constants ---

   public static final int PRIORITY_MIN     = 0;
   public static final int PRIORITY_DEFAULT = 0;
   public static final int PRIORITY_FIRST   = 1;
   public static final int PRIORITY_HIGH    = 2;
   public static final int PRIORITY_NORMAL  = 3;
   public static final int PRIORITY_LOW     = 4;
   public static final int PRIORITY_HOLD    = 5;
   public static final int PRIORITY_MAX     = 5;

   private static final int NDIGIT_ORDER_MIN =  2; // my arbitrary decision
   private static final int NDIGIT_TOTAL_MAX = 12;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      HPConfig.class,
      0,0,
      new AbstractField[] {

         new FileField("dataDir","DataDir",0,new File("C:\\OMHotfolder")),

         new IntegerField("priority","Priority",0,0),
         new StringField("prefix","Prefix",0,""),
         new IntegerField("useDigits","UseDigits",0,8),
         new StringField("dealerID","DealerID",0,""),
         new StringField("dealerName","DealerName",0,""),

         new StructureListField("backprints","Backprint",Backprint.sd,Merge.POSITION,0,1),
         new StructureListField("mappings","Mapping",HPMapping.sd,Merge.IDENTITY,0,0).with(MappingUtil.skuAccessor,MappingUtil.skuComparator)
      });

   protected StructureDefinition sd() { return sd; }

// --- validation ---

   public static boolean isDigits(String s) {

      for (int i=0; i<s.length(); i++) {
         char c = s.charAt(i);
         if (c < '0' || c > '9') return false;
      }

      return true;
   }

   public void validate() throws ValidationException {

      if (priority < PRIORITY_MIN || priority > PRIORITY_MAX) throw new ValidationException(Text.get(this,"e2",new Object[] { Convert.fromInt(priority) }));

      if ( ! isDigits(prefix) ) throw new ValidationException(Text.get(this,"e3"));

      if (useDigits < NDIGIT_ORDER_MIN) throw new ValidationException(Text.get(this,"e4",new Object[] { Convert.fromInt(NDIGIT_ORDER_MIN) }));

      if (prefix.length() + useDigits > NDIGIT_TOTAL_MAX) throw new ValidationException(Text.get(this,"e5",new Object[] { Convert.fromInt(NDIGIT_TOTAL_MAX) }));

      if (dealerID  .length() == 0) throw new ValidationException(Text.get(this,"e6"));
      if (dealerName.length() == 0) throw new ValidationException(Text.get(this,"e7"));

      Backprint.validate(backprints);
      MappingUtil.validate(mappings);
   }

// --- implementation of MappingConfig ---

   public LinkedList getMappings() { return mappings; }
   public void putMappings(LinkedList mappings) { this.mappings = mappings; }

   public boolean mapsSpecialSKU() { return false; }

}

