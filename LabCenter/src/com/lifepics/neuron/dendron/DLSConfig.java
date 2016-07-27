/*
 * DLSConfig.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.struct.*;

import java.util.LinkedList;

import org.w3c.dom.Node;

/**
 * An object that holds configuration information for the DLS format.
 */

public class DLSConfig extends Structure implements MappingConfig {

// --- fields ---

   public String host;
   public int    port; // probably not in UI, but avoid hardcoding
   public String path; //
   public String userName;
   public String password;

   public boolean completeImmediately;
   // this flag means the job should complete immediately, so that the
   // order items are marked printed.  the order might auto-complete,
   // producing a message to the server, but that's not the same thing.

   public LinkedList backprints;
   public LinkedList mappings;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      DLSConfig.class,
      0,2,
      new AbstractField[] {

         new StringField("host","Host",0,null),
         new IntegerField("port","Port",0,8080),
         new StringField("path","Path",0,"soap/servlet/rpcrouter"),
         new StringField("userName","UserName",0,"pns"),
         new StringField("password","Password",0,"pns123"),

         new BooleanField("completeImmediately","CompleteImmediately",1,false),

         new StructureListField("backprints","Backprint",Backprint.sd,Merge.POSITION,0,2),
         new StructureListField("mappings","Mapping",DLSMapping.sd,Merge.IDENTITY,2,0) {
            protected void loadSpecial(Node node, Object o, int version) throws ValidationException {
               MappingUtil.migrate(tget(o),node,DLSMapping.sd);
            }
         }.with(MappingUtil.skuAccessor,MappingUtil.skuComparator)
      });

   protected StructureDefinition sd() { return sd; }

// --- validation ---

   public void validate() throws ValidationException {
      Backprint.validate(backprints);
      MappingUtil.validate(mappings);
   }

// --- implementation of MappingConfig ---

   public LinkedList getMappings() { return mappings; }
   public void putMappings(LinkedList mappings) { this.mappings = mappings; }

   public boolean mapsSpecialSKU() { return true; }

}

