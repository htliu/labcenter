/*
 * AutoUpdateConfig.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.object.CopyUtil;
import com.lifepics.neuron.struct.*;

import java.util.LinkedList;

import org.w3c.dom.Node;

/**
 * An object that holds configuration information for the auto-update subsystem.
 */

public class AutoUpdateConfig extends Structure {

// --- fields ---

   public String autoUpdateURL;

   public VersionInfo lc;
   public VersionInfo invoice;
   public VersionInfo burner;

   public boolean enableRestart;
   public int restartHour1;
   public int restartHour2;
   public long restartWaitInterval;

   public String autoConfigURL; // just a prefix really
   public Integer instanceID;
   public Integer passcode;
   public Integer revisionNumber; // last synced revision

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      AutoUpdateConfig.class,
      0,new History(new int[] {5,772,6}),
      new AbstractField[] {

         new StringField("autoUpdateURL","AutoUpdateURL",4,"https://services.lifepics.com/autoupdate.asp"),

         new StructureField("lc","LabCenter",VersionInfo.sd,5) {
            protected void loadSpecial(Node node, Object o, int version) throws ValidationException {

               // tset(o,new VersionInfo().load(node));
               //
               // this is the idea, loadNormal except migrate in from the parent node, but we want to
               // adjust the backup version too.

               VersionInfo info = new VersionInfo();
               info.load(node);

               // in the old structure, current and backup versions weren't nullable, but VersionInfo
               // in general allows nulls.  how do we reconcile the two?
               // current version isn't nullable in the new structure, we'll catch that in validation.
               // backup version used empty string to represent null, so null -> error, empty -> null.
               //
               if (info.backupVersion == null) throw new ValidationException(Text.get(AutoUpdateConfig.class,"e6"));
               if (info.backupVersion.length() == 0) info.backupVersion = null;

               tset(o,info);
            }
         },
         new StructureField("invoice","Invoice",VersionInfo.sd,5),
         new StructureField("burner","Burner",VersionInfo.sd,6),

         new BooleanField("enableRestart","EnableRestart",2,true),
         new IntegerField("restartHour1","RestartHour1",2,0),
         new IntegerField("restartHour2","RestartHour2",2,3),
         new LongField("restartWaitInterval","RestartWaitInterval-Millis",2,60000),

         new StringField("autoConfigURL","AutoConfigURL",3,"https://services.lifepics.com/LabCenter/"),
         new NullableIntegerField("instanceID","InstanceID",3,null),
         new NullableIntegerField("passcode","PassCode",3,null),
         new NullableIntegerField("revisionNumber","RevisionNumber",3,null)
      });

   protected StructureDefinition sd() { return sd; }

// --- copy function ---

   public AutoUpdateConfig copy() { return (AutoUpdateConfig) CopyUtil.copy(this); }

// --- validation ---

   private void validateHour(int hour) throws ValidationException {
      if (hour < 0 || hour > 23) throw new ValidationException(Text.get(this,"e1",new Object[] { Convert.fromInt(hour) }));
   }

   public void validate() throws ValidationException {

      lc.validate();
      invoice.validate();
      burner.validate();

      if (lc.currentVersion == null) throw new ValidationException(Text.get(this,"e5"));

      validateHour(restartHour1);
      validateHour(restartHour2);
      if (restartHour1 == restartHour2) throw new ValidationException(Text.get(this,"e2"));

      if (restartWaitInterval <= 0) throw new ValidationException(Text.get(this,"e3"));

      if (restartWaitInterval != (int) restartWaitInterval) throw new ValidationException(Text.get(this,"e4"));
      // the point is to use this in a timer, and they take int.
      // the field has to be a long because that's how IntervalField works.
   }

// --- version info class ---

   public static class VersionInfo extends Structure {

      public String currentVersion; // these are file names, not version numbers
      public String backupVersion;
      public LinkedList staleVersions;

      public static final StructureDefinition sd = new StructureDefinition(

         VersionInfo.class,
         // no version
         new AbstractField[] {

            new NullableStringField("currentVersion","CurrentVersion",0,null),
            new NullableStringField("backupVersion","BackupVersion",0,null),
            new InlineListField("staleVersions","StaleVersion",0)
         });

      protected StructureDefinition sd() { return sd; }

      public void validate() throws ValidationException {
      }
   }

}

