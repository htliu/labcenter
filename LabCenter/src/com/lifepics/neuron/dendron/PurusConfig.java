/*
 * PurusConfig.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.*;

import java.io.File;
import java.util.LinkedList;

/**
 * An object that holds configuration information for the Purus format.
 */

public class PurusConfig extends Structure implements MappingConfig {

// --- fields ---

   public File dataDir;
   public File statusDir;
   public String mandator;
   public String dealer;
   public String prefix;
   public int orderDigits;
   public int reprintDigits;
   public boolean swap;
   public LinkedList mappings;

// --- n-digit values ---

   public static final int NDIGIT_ORDER_DEFAULT = 6;
   public static final int NDIGIT_REPRINT_DEFAULT = 2;

   public static final int NDIGIT_ORDER_MIN = 3;
   public static final int NDIGIT_REPRINT_MIN = 1;

   public static final int NDIGIT_TOTAL_MAX = 12;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      PurusConfig.class,
      0,0,
      new AbstractField[] {

         new FileField("dataDir","DataDir",0,null),
         new FileField("statusDir","StatusDir",0,null),
         new StringField("mandator","Mandator",0,""),
         new StringField("dealer","Dealer",0,""),
         new StringField("prefix","Prefix",0,""),
         new IntegerField("orderDigits","OrderDigits",0,NDIGIT_ORDER_DEFAULT),
         new IntegerField("reprintDigits","ReprintDigits",0,NDIGIT_REPRINT_DEFAULT),
         new BooleanField("swap","Swap",0,false),
         new StructureListField("mappings","Mapping",PurusMapping.sd,Merge.IDENTITY,0,0).with(MappingUtil.skuAccessor,MappingUtil.skuComparator)
      });

   protected StructureDefinition sd() { return sd; }

// --- validation ---

   public void validate() throws ValidationException {

      // the Purus spec suggests that the prefix should be digits,
      // but the folks at Canon say to allow anything, so do that.

      if (orderDigits < NDIGIT_ORDER_MIN) throw new ValidationException(Text.get(this,"e1",new Object[] { Convert.fromInt(NDIGIT_ORDER_MIN) }));
      if (reprintDigits < NDIGIT_REPRINT_MIN) throw new ValidationException(Text.get(this,"e2",new Object[] { Convert.fromInt(NDIGIT_REPRINT_MIN) }));

      if (prefix.length() + orderDigits + reprintDigits > NDIGIT_TOTAL_MAX) throw new ValidationException(Text.get(this,"e3",new Object[] { Convert.fromInt(NDIGIT_TOTAL_MAX) }));

      MappingUtil.validate(mappings);
   }

// --- implementation of MappingConfig ---

   public LinkedList getMappings() { return mappings; }
   public void putMappings(LinkedList mappings) { this.mappings = mappings; }

   public boolean mapsSpecialSKU() { return true; }

}

