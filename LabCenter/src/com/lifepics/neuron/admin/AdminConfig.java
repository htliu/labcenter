
/*
 * AdminConfig.java
 */

package com.lifepics.neuron.admin;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.net.DiagnoseConfig;
import com.lifepics.neuron.struct.*;

import java.util.logging.Level;

/**
 * An object that holds config information for the admin application.
 */

public class AdminConfig extends Structure {

// --- fields ---

   public String baseURL;
   public String autoConfigURL;

   public int logCount;
   public int logSize; // bytes
   public Level logLevel;

   public DiagnoseConfig diagnoseConfig;
   public int defaultTimeoutInterval;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      AdminConfig.class,
      0,0,
      new AbstractField[] {

         new StringField("baseURL","BaseURL"),
         new StringField("autoConfigURL","AutoConfigURL"),

         new IntegerField("logCount","LogCount"),
         new IntegerField("logSize","LogSize-Bytes"),
         new LevelField("logLevel","LogLevel"),

         new StructureField("diagnoseConfig","DiagnoseConfig",DiagnoseConfig.sd),
         new IntegerField("defaultTimeoutInterval","DefaultTimeoutInterval-Millis")
      });

   protected StructureDefinition sd() { return sd; }

// --- validation ---

   public void validate() throws ValidationException {

      if (logCount < 1) throw new ValidationException(Text.get(this,"e2"));
      if (logSize < 0) throw new ValidationException(Text.get(this,"e3"));

      diagnoseConfig.validate();

      if (defaultTimeoutInterval < 1) throw new ValidationException(Text.get(this,"e4"));
   }

}

