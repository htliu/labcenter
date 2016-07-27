/*
 * FujiConfig.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.object.XML;
import com.lifepics.neuron.struct.*;

import java.io.File;
import java.util.LinkedList;

import org.w3c.dom.Node;

/**
 * An object that holds configuration information for the Fuji format.
 */

public class FujiConfig extends Structure implements MappingConfig {

// --- fields ---

   public File requestDir;
   public File imageDir;      // nullable (null if old config)
   public File mapRequestDir; // nullable (but usually not)
   public File mapImageDir;   // nullable (but usually not)
   public boolean limitEnable;
   public int limitLength;
   public LinkedList mappings;

// --- n-digit values ---

   // these aren't used here, but this is a good place for them
   public static final int NDIGIT_ORDER_ID  = 4;
   public static final int NDIGIT_SKU_COUNT = 1;

// --- structure ---

   // the loadSpecial overrides sometimes use different default values
   // than the built-in ones.  this is intentional ... it allows
   // new installations to have new values from FujiConfig.loadDefault,
   // but gives old installations different values that preserve their
   // existing behavior.

   public static final StructureDefinition sd = new StructureDefinition(

      FujiConfig.class,
      0,4,
      new AbstractField[] {

         new FileField("requestDir","RequestDir",2,null) {
            protected void loadSpecial(Node node, Object o, int version) throws ValidationException {
               tset(o,Convert.toFile(XML.getElementText(node,"DataDir")));
            }
         },
         new NullableFileField("imageDir","ImageDir",2,new File("C:\\LifePics Orders\\Fuji")) {
            protected void loadSpecial(Node node, Object o, int version) throws ValidationException {
               tset(o,null);
            }
         },
         new NullableFileField("mapRequestDir","MapRequestDir",2,new File("\\\\pic\\request")) {
            protected void loadSpecial(Node node, Object o, int version) throws ValidationException {
               if (version >= 1) {
                  tset(o,Convert.toNullableFile(getNullableText(node,"MapDataDirTo")));
               } else {
                  loadDefault(o);
               }
            }
         },
         new NullableFileField("mapImageDir","MapImageDir",2,new File("\\\\machine name\\LifePics Orders\\Fuji")) {
            protected void loadSpecial(Node node, Object o, int version) throws ValidationException {
               if (version >= 1) {
                  tset(o,Convert.toNullableFile(getNullableText(node,"MapOrderDirTo")));
               } else {
                  tset(o,null);
               }
            }
         },
         new BooleanField("limitEnable","LimitEnable",3,true),
         new IntegerField("limitLength","LimitLength",3,35),
         new StructureListField("mappings","Mapping",FujiMapping.sd,Merge.IDENTITY,4,0) {
            protected void loadSpecial(Node node, Object o, int version) throws ValidationException {
               MappingUtil.migrate(tget(o),node,FujiMapping.sd);
            }
         }.with(MappingUtil.skuAccessor,MappingUtil.skuComparator)
      });

   protected StructureDefinition sd() { return sd; }

// --- validation ---

   public void validate() throws ValidationException {

      if (limitLength < 8) throw new ValidationException(Text.get(this,"e5"));

      MappingUtil.validate(mappings);
   }

// --- implementation of MappingConfig ---

   public LinkedList getMappings() { return mappings; }
   public void putMappings(LinkedList mappings) { this.mappings = mappings; }

   public boolean mapsSpecialSKU() { return true; }

}

