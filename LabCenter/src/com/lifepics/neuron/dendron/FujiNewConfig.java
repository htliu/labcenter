/*
 * FujiNewConfig.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.*;

import java.io.File;
import java.util.LinkedList;

import org.w3c.dom.Node;

/**
 * An object that holds configuration information for the Fuji PIC 2.6 format.
 */

public class FujiNewConfig extends Structure implements MappingConfig {

// --- fields ---

   public File requestDir;
   public File imageDir;
   public File mapImageDir;
   public String prefix;
   public boolean useOrderSource;
   public boolean useOrderTypeIn;
   public String orderSource;
   public String orderTypeIn;
   public boolean proMode;
   public long proModeWaitInterval; // millis
   public boolean limitEnable;
   public int limitLength;

   // job completion fields
   public boolean enableCompletion;
   public int completionType;
   public FujiCompletion completion;
   public File listDirectory;
   public String databaseClass;
   public String databaseURL;
   public String databaseUser;
   public String databasePassword;

   public LinkedList mappings;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      FujiNewConfig.class,
      0,7,
      new AbstractField[] {

         new FileField("requestDir","RequestDir",0,null),
         new FileField("imageDir","ImageDir",0,new File("C:\\LifePics Orders\\Fuji")),
         new FileField("mapImageDir","MapImageDir",0,new File("\\\\machine name\\LifePics Orders\\Fuji")),
         new StringField("prefix","Prefix",0,"LP"),
         new BooleanField("useOrderSource","UseOrderSource",1,true),
         new BooleanField("useOrderTypeIn","UseOrderTypeIn",1,false),
         new StringField("orderSource","OrderSource",1,"Kiosk"),
         new StringField("orderTypeIn","OrderTypeIn",1,"Network"),
         new BooleanField("proMode","ProMode",2,false),
         new LongField("proModeWaitInterval","ProModeWaitInterval-Millis",3,15000),
         new BooleanField("limitEnable","LimitEnable",0,true),
         new IntegerField("limitLength","LimitLength",0,35),

         new BooleanField("enableCompletion","EnableCompletion",4,false),
         new EnumeratedField("completionType","CompletionType",FormatFujiNew.completionTypeType,7,FormatFujiNew.TYPE_LIST_TXT),
         new StructureField("completion","Completion",FujiCompletion.sd,7),
         new FileField("listDirectory","ListDirectory",5,new File("C:\\Queues\\Process")),
         new StringField("databaseClass","DatabaseClass",4,"sun.jdbc.odbc.JdbcOdbcDriver"),
         new StringField("databaseURL","DatabaseURL",4,"jdbc:odbc:FDIA_DB"),
         new StringField("databaseUser","DatabaseUser",4,"fdiaclient"),
         new StringField("databasePassword","DatabasePassword",4,"radiogaga"),

         new StructureListField("mappings","Mapping",FujiNewMapping.sd,Merge.IDENTITY,6,0) {
            protected void loadSpecial(Node node, Object o, int version) throws ValidationException {
               MappingUtil.migrate(tget(o),node,FujiNewMapping.sd);
            }
         }.with(MappingUtil.skuAccessor,MappingUtil.skuComparator)
      });

   protected StructureDefinition sd() { return sd; }

// --- validation ---

   public void validate() throws ValidationException {

      if (   prefix.length() < 1 || prefix.length() > 3 ) throw new ValidationException(Text.get(this,"e2"));
      if ( ! prefix.matches("[a-zA-Z]*") ) throw new ValidationException(Text.get(this,"e3"));

      if (proModeWaitInterval < 0) throw new ValidationException(Text.get(this,"e4"));

      if (limitLength < 8) throw new ValidationException(Text.get(this,"e1"));

      FormatFujiNew.validateCompletionType(completionType);
      completion.validate();

      MappingUtil.validate(mappings);
   }

// --- implementation of MappingConfig ---

   public LinkedList getMappings() { return mappings; }
   public void putMappings(LinkedList mappings) { this.mappings = mappings; }

   public boolean mapsSpecialSKU() { return false; }

}

