/*
 * ScanConfigDLS.java
 */

package com.lifepics.neuron.axon;

import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.object.CopyUtil;
import com.lifepics.neuron.struct.*;

import java.util.Date;

/**
 * An object that holds configuration information for the DLS scanner.
 */

public class ScanConfigDLS extends Structure {

// --- fields ---

   public String host;
   public int    port; // probably not in UI, but avoid hardcoding
   public String path; //
   public String userName;
   public String password;

   public Date effectiveDate; // nullable, but validated not null when enabled

   public boolean excludeByID;
   public boolean holdConfirm;
   // this one is similar to holdInvoice, so you might think it should go
   // up there at the top level, but I can easily imagine that when more
   // scanners are available, the users might want to confirm on some and
   // not on others.

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      ScanConfigDLS.class,
      0,1,
      new AbstractField[] {

         new StringField("host","Host",0,""),
         new IntegerField("port","Port",0,8080),
         new StringField("path","Path",0,"soap/servlet/rpcrouter"),
         new StringField("userName","UserName",0,"kiassu"),
         new StringField("password","Password",0,"kimyaxyz"),

         new NullableDateField("effectiveDate","EffectiveDate",0),

         new BooleanField("excludeByID","ExcludeByID",1,false),
         new BooleanField("holdConfirm","HoldConfirm",0,false)
      });

   protected StructureDefinition sd() { return sd; }

   // the port and path defaults are the same as in DLSConfig -- bad style!
   // however, the user name and password are different,
   // because you have to be super-user to call GetOrdersAdmin
   // (technically, the user account has to have some privilege enabled,
   // but we couldn't find any way to manage the user accounts on Mike's DLS)

// --- copy function ---

   public ScanConfigDLS copy() { return (ScanConfigDLS) CopyUtil.copy(this); }

// --- validation ---

   public void validate() throws ValidationException {
   }

}

