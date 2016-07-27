/*
 * DP2Config.java
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
 * An object that holds configuration information for the DP2 format.
 */

public class DP2Config extends Structure implements MappingConfig {

// --- fields ---

   public File requestDir;
   public File imageDir;
   public File mapImageDir; // nullable

   public String includeFile;
   public String prefix;
   public String customerID;
   public String status;
   public String rollName;

   public boolean enableRealID;
   public String  prefixRealID;

   public boolean customIntegration;
   public String customerType;
   public boolean includeShipCompany;

   public boolean completeImmediately;

   public LinkedList mappings;

// --- constants ---

   public static final String[] includeSet = new String[] { "Cmds", "~<$App.ShareDirectory>\\Scripts\\Cmds.txt~" };
   public static final String[] statusSet  = new String[] { "Ready", "Hold" };

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      DP2Config.class,
      0,3,
      new AbstractField[] {

         new FileField("requestDir","RequestDir",0,null),
         new FileField("imageDir","ImageDir",0,null),
         new NullableFileField("mapImageDir","MapImageDir",0,null),

         new StringField("includeFile","IncludeFile",1,includeSet[0]),
         new StringField("prefix","Prefix",0,"LP"),
         new StringField("customerID","CustomerID",0,""),
         new StringField("status","Status",0,statusSet[0]),
         new StringField("rollName","RollName",0,"001"),

         new BooleanField("enableRealID","EnableRealID",2,false),
         new StringField ("prefixRealID","PrefixRealID",2,""),

         new BooleanField("customIntegration","CustomIntegration",1,false),
         new StringField("customerType","CustomerType",1,""),
         new BooleanField("includeShipCompany","IncludeShipCompany",2,false),

         new BooleanField("completeImmediately","CompleteImmediately",0,false),

         new StructureListField("mappings","Mapping",DP2Mapping.sd,Merge.IDENTITY,3,0) {
            protected void loadSpecial(Node node, Object o, int version) throws ValidationException {
               MappingUtil.migrate(tget(o),node,DP2Mapping.sd);
            }
         }.with(MappingUtil.skuAccessor,MappingUtil.skuComparator)
      });

   protected StructureDefinition sd() { return sd; }

// --- validation ---

   private static void notilde(String s, String key) throws ValidationException {
      if (s.indexOf('~') != -1) throw new ValidationException(Text.get(DP2Config.class,key));
   }

   public void validate() throws ValidationException {

      if (includeFile.length() == 0) throw new ValidationException(Text.get(this,"e14"));
      // empty prefix isn't recommended, but it's possible
      if (customerID.length() == 0) throw new ValidationException(Text.get(this,"e1"));
      if (status    .length() == 0) throw new ValidationException(Text.get(this,"e2"));
      if (rollName  .length() == 0) throw new ValidationException(Text.get(this,"e3"));
      // could do a conditional validation on customer type being filled in, but no need

      // the validation on the first two folders isn't really necessary
      notilde(Convert.fromFile(requestDir),"e4");
      notilde(Convert.fromFile(imageDir),"e5");
      if (mapImageDir != null) notilde(Convert.fromFile(mapImageDir),"e6");

      // include file needs to be able to have tildes, so user just has to be careful
      notilde(prefix,"e7");
      notilde(customerID,"e8");
      notilde(status,"e9");
      notilde(rollName,"e10");
      notilde(prefixRealID,"e18");
      notilde(customerType,"e15");

      MappingUtil.validate(mappings);
   }

// --- implementation of MappingConfig ---

   public LinkedList getMappings() { return mappings; }
   public void putMappings(LinkedList mappings) { this.mappings = mappings; }

   public boolean mapsSpecialSKU() { return true; }

}

