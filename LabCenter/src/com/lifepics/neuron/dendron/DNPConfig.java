/*
 * DNPConfig.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.*;

import java.util.LinkedList;

import org.w3c.dom.Node;

/**
 * An object that holds configuration information for the DNP direct format.
 */

public class DNPConfig extends Structure implements MappingConfig {

// --- fields ---

   public int printerType; // enum PRINTER_X
   public int printerIDType;
   public String printerID;
   public long maxWaitInterval; // millis
   public boolean rotateCW;
   public boolean completeImmediately;

   public LinkedList mappings;

// --- method enumeration ---

   // this has to come before the structure definition
   // since it's all static

   private static final int ID_TYPE_MIN = 0;

   public static final int ID_TYPE_SINGLE = 0;
   public static final int ID_TYPE_ID     = 1;
   public static final int ID_TYPE_SERIAL = 2;
   public static final int ID_TYPE_MEDIA  = 3;

   private static final int ID_TYPE_MAX = 3;

   private static String[] idTypeTable = {
         Text.get(DNPConfig.class,"id0"),
         Text.get(DNPConfig.class,"id1"),
         Text.get(DNPConfig.class,"id2"),
         Text.get(DNPConfig.class,"id3")
      };

   private static int toIDType(String s) throws ValidationException {
      for (int i=0; i<idTypeTable.length; i++) {
         if (s.equals(idTypeTable[i])) return i;
      }
      throw new ValidationException(Text.get(DNPConfig.class,"e2",new Object[] { s }));
   }

   private static String fromIDType(int idType) {
      return idTypeTable[idType];
   }

   private static EnumeratedType idTypeType = new EnumeratedType() {
      public int toIntForm(String s) throws ValidationException { return toIDType(s); }
      public String fromIntForm(int i) { return fromIDType(i); }
   };

   private static void validateIDType(int idType) throws ValidationException {
      if (idType < ID_TYPE_MIN || idType > ID_TYPE_MAX) throw new ValidationException(Text.get(DNPConfig.class,"e3",new Object[] { Convert.fromInt(idType) }));
   }

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      DNPConfig.class,
      0,new History(new int[] {0,741,1,758,2}),
      new AbstractField[] {

         new IntegerField("printerType","PrinterType",0,DNP.PRINTER_DS40),
         new EnumeratedField("printerIDType","PrinterIDType",idTypeType,2,ID_TYPE_ID) {
            protected void loadSpecial(Node node, Object o, int version) {
               tset(o,ID_TYPE_ID);
            }
            public void loadDefault(Object o) {
               tset(o,ID_TYPE_SINGLE);
            }
            // an unusual case.  ID_TYPE_ID is the correct value for rolling forward / backward,
            // but for brand new objects we want ID_TYPE_SINGLE.  the double override does that.
         },
         new StringField("printerID","PrinterID",0,""),
         new LongField("maxWaitInterval","MaxWaitInterval-Millis",1,60000),
         new BooleanField("rotateCW","RotateCW",1,false),
         new BooleanField("completeImmediately","CompleteImmediately",0,false),

         new StructureListField("mappings","Mapping",DNPMapping.sd,Merge.IDENTITY,0,0).with(MappingUtil.skuAccessor,MappingUtil.skuComparator)
      });

   protected StructureDefinition sd() { return sd; }

// --- validation ---

   public void validate() throws ValidationException {

      validateIDType(printerIDType);

      // now validate printerID in different ways depending on the printerIDType
      switch (printerIDType) {
      case ID_TYPE_SINGLE:
         if (printerID.length() > 0) throw new ValidationException(Text.get(this,"e4"));
         break;
      case ID_TYPE_ID:
         Convert.toInt(printerID); // validation
         break;
      case ID_TYPE_SERIAL:
         // no validation
         break;
      case ID_TYPE_MEDIA:
         DNPMedia m = DNPMedia.findMediaByDescription(printerID); // validation
         if (m.scode == null) throw new ValidationException(Text.get(this,"e5",new Object[] { printerID }));
         // null scode is fairly harmless, just means we'd never find the printer .. but, easy to catch
         break;
      default:
         throw new IllegalArgumentException();
      }

      if (maxWaitInterval < 0) throw new ValidationException(Text.get(this,"e1"));

      MappingUtil.validate(mappings);
   }

// --- implementation of MappingConfig ---

   public LinkedList getMappings() { return mappings; }
   public void putMappings(LinkedList mappings) { this.mappings = mappings; }

   public boolean mapsSpecialSKU() { return true; }

}

