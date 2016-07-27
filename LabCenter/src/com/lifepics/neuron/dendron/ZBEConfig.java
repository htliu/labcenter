/*
 * ZBEConfig.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.*;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * An object that holds configuration information for the ZBE format.
 */

public class ZBEConfig extends Structure implements MappingConfig {

// --- fields ---

   public File requestDir;
   public File imageDir;
   public File mapImageDir; // nullable
   public File productRoot; //

   public String prefix;
   public boolean includeCustomer;
   public Boolean submitOnHold;
   public Boolean submitForReview;
   public int units;
   public boolean enableCompletion;

   public LinkedList backprints;
   public LinkedList mappings;

// --- constants ---

   public static final int UNITS_MIN    = 0;
   public static final int UNITS_INCH   = 0;
   public static final int UNITS_CM     = 1;
   public static final int UNITS_300DPI = 2;
   public static final int UNITS_MAX    = 2;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      ZBEConfig.class,
      0,0,
      new AbstractField[] {

         new FileField("requestDir","RequestDir",0,null),
         new FileField("imageDir","ImageDir",0,null),
         new NullableFileField("mapImageDir","MapImageDir",0,null),
         new NullableFileField("productRoot","ProductRoot",0,null),

         new StringField("prefix","Prefix",0,"LP"),
         new BooleanField("includeCustomer","IncludeCustomer",0,false),
         new NullableBooleanField("submitOnHold","SubmitOnHold",0,null),
         new NullableBooleanField("submitForReview","SubmitForReview",0,null),
         new IntegerField("units","Units",0,UNITS_INCH),
         new BooleanField("enableCompletion","EnableCompletion",0,false),

         new StructureListField("backprints","Backprint",Backprint.sd,Merge.POSITION,0,2), // 3 allowed
         new StructureListField("mappings","Mapping",ZBEMapping.sd,Merge.IDENTITY,0,0).with(MappingUtil.skuAccessor,MappingUtil.skuComparator)
      });

   protected StructureDefinition sd() { return sd; }

// --- validation ---

   public void validate() throws ValidationException {

      if (units < UNITS_MIN || units > UNITS_MAX) throw new ValidationException(Text.get(this,"e1",new Object[] { Convert.fromInt(units) }));

      // includeCustomer makes it go into the database,
      // so if you submit for review only, and don't set that, nothing happens.
      // you could get the same problem if submitForReview is set to default
      // and the default is to review, but apparently it's hardcoded no review.
      if (    submitForReview != null
           && submitForReview.booleanValue()
           && ! includeCustomer ) throw new ValidationException(Text.get(this,"e2"));

      if (Backprint.hasSequence(backprints)) throw new ValidationException(Text.get(this,"e3"));
         // don't do this, we'd need one copy of the image file for each sequence number

      Backprint.validate(backprints);
      MappingUtil.validate(mappings);

      if (hasProductPath(mappings) && productRoot == null) throw new ValidationException(Text.get(this,"e4"));
   }

   private static boolean hasProductPath(LinkedList mappings) {
      Iterator i = mappings.iterator();
      while (i.hasNext()) {
         ZBEMapping m = (ZBEMapping) i.next();
         if (m.productPath != null) return true;
      }
      return false;
   }

// --- implementation of MappingConfig ---

   public LinkedList getMappings() { return mappings; }
   public void putMappings(LinkedList mappings) { this.mappings = mappings; }

   public boolean mapsSpecialSKU() { return true; }

}

