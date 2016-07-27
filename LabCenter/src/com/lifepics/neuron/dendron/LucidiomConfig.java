/*
 * LucidiomConfig.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.*;

import java.io.File;
import java.util.LinkedList;

import org.w3c.dom.Node;

/**
 * An object that holds configuration information for the Lucidiom format.
 */

public class LucidiomConfig extends Structure implements MappingConfig {

// --- fields ---

   public File dataDir;
   public String apmID;
   public String brand;
   public boolean glossy;
   public boolean completeImmediately;

   public LinkedList mappings;

// --- n-digit values ---

   // these aren't used here, but this is a good place for them
   public static final int NDIGIT_ORDER_ID = 5;
   public static final int NDIGIT_IMAGE_ID = 3;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      LucidiomConfig.class,
      0,2,
      new AbstractField[] {

         new FileField("dataDir","DataDir",0,null),
         new StringField("apmID","ApmID",0,null),
         new StringField("brand","Brand",0,null),
         new BooleanField("glossy","Glossy",0,true),
         new BooleanField("completeImmediately","CompleteImmediately",1,false),

         new StructureListField("mappings","Mapping",LucidiomMapping.sd,Merge.IDENTITY,2,0) {
            protected void loadSpecial(Node node, Object o, int version) throws ValidationException {
               MappingUtil.migrate(tget(o),node,LucidiomMapping.sd);
            }
         }.with(MappingUtil.skuAccessor,MappingUtil.skuComparator)
      });

   protected StructureDefinition sd() { return sd; }

// --- validation ---

   private static void validateDigits(String s, int min, int max, String key) throws ValidationException {
      int n = s.length();

      boolean ok = true;
      if (n < min || n > max) {
         ok = false;
      } else {
         for (int i=0; i<n; i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') { ok = false; break; }
         }
      }

      if ( ! ok ) {
         String field = Text.get(LucidiomConfig.class,key);
         String form = (max != min) ? "e2" : "e1";
         throw new ValidationException(Text.get(LucidiomConfig.class,form,new Object[] { field, s, Convert.fromInt(min), Convert.fromInt(max) }));
         // argument 3 not used in short form, but no harm in sending it
      }
   }

   public void validate() throws ValidationException {
      validateDigits(apmID,4,5,"f1");
      validateDigits(brand,4,4,"f2");
      MappingUtil.validate(mappings);
   }

   public static void validateProduct(String product) throws ValidationException {
      validateDigits(product,4,7,"f3");
      // the point is to keep this here with the other digit validations
   }

// --- implementation of MappingConfig ---

   public LinkedList getMappings() { return mappings; }
   public void putMappings(LinkedList mappings) { this.mappings = mappings; }

   public boolean mapsSpecialSKU() { return true; }

}

