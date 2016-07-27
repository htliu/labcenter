/*
 * PollConfig.java
 */

package com.lifepics.neuron.axon;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.object.CopyUtil;
import com.lifepics.neuron.struct.*;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * An object that holds configuration information for the poll subsystem.
 */

public class PollConfig extends Structure {

// --- fields ---

   public long pollInterval; // millis
   public LinkedList targets;
   public int throttleCount;
   public long throttleInterval; // millis
   public String defaultEmail; // nullable
   public boolean ignoreMode;
   public long imageDelayInterval; // millis

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      PollConfig.class,
      0,3,
      new AbstractField[] {

         new LongField("pollInterval","PollInterval",0,60000),
         new StructureListField("targets","Target",Target.sd,Merge.EQUALITY,0,0),
         new IntegerField("throttleCount","ThrottleCount",1,10),
         new LongField("throttleInterval","ThrottleInterval",1,1000),
         new NullableStringField("defaultEmail","DefaultEmail",0,null),
         new BooleanField("ignoreMode","IgnoreMode",2,false),
         new LongField("imageDelayInterval","ImageDelayInterval",3,ImagePoller.DEFAULT_DELAY_INTERVAL)
      });

   protected StructureDefinition sd() { return sd; }

// --- copy function ---

   public PollConfig copy() { return (PollConfig) CopyUtil.copy(this); }

// --- validation ---

   public void validate() throws ValidationException {

      if (pollInterval < 1) throw new ValidationException(Text.get(this,"e1"));

      Iterator i = targets.iterator();
      while (i.hasNext()) {
         ((Target) i.next()).validate();
      }

      if (throttleCount < 1) throw new ValidationException(Text.get(this,"e2"));
      if (throttleInterval < 1) throw new ValidationException(Text.get(this,"e3"));

      // can't validate defaultEmail here, since it uses mail.jar
      // which in a fresh install may not have been extracted yet.
      // so, if an invalid one is entered, the rolls will just pause.
      // we could add something to ConfigDialog.weakValidate,
      // but iterating is a pain, and it's not worth that much effort.

      if (imageDelayInterval < 0) throw new ValidationException(Text.get(this,"e4"));
      // this delay isn't used in a wait call, so zero is fine
   }

// --- target class ---

   public static class Target extends Structure {

      public int source; // from roll source enumeration
      public File directory;
      //
      // eventually we might need more fields,
      // even source-dependent fields,
      // but this is sufficient for now.
      //
      // if you do add any more, you probably
      // also need to change the merge policy!

      public static final StructureDefinition sd = new StructureDefinition(

         Target.class,
         // no version
         new AbstractField[] {

            new EnumeratedField("source","Source",RollEnum.sourceType),
            new FileField("directory","Directory")
         });

      protected StructureDefinition sd() { return sd; }

      public void validate() throws ValidationException {
         Roll.validateSource(source);
      }
   }

}

