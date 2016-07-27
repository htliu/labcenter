/*
 * InstallConfig.java
 */

package com.lifepics.neuron.install;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.net.DiagnoseConfig;
import com.lifepics.neuron.struct.*;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * An object that holds configuration information
 * for the install (and maybe setup) applications.
 */

public class InstallConfig extends Structure {

// --- fields ---

   public DiagnoseConfig diagnoseConfig;
   public int defaultTimeoutInterval;

   public Context context;
   public LinkedList files;

// --- structure ---

   public static final StructureDefinition sd = new StructureDefinition(

      InstallConfig.class,
      0,1,
      new AbstractField[] {

         new StructureField("diagnoseConfig","DiagnoseConfig",DiagnoseConfig.sd),
         new IntegerField("defaultTimeoutInterval","DefaultTimeoutInterval-Millis"),

         new StructureField("context","Context",Context.sd,1),
         new StructureListField("files","File",InstallFile.sd,Merge.NO_MERGE)
      });

   protected StructureDefinition sd() { return sd; }

// --- helpers ---

   /**
    * Get all files of the specified type.
    */
   public LinkedList getFiles(String type, boolean allowZero) throws Exception {
      LinkedList result = new LinkedList();

      Iterator i = files.iterator();
      while (i.hasNext()) {
         InstallFile file = (InstallFile) i.next();
         if (file.type != null && file.type.equals(type)) result.add(file);
      }

      if (result.size() == 0 && ! allowZero) throw new Exception(Text.get(this,"e1",new Object[] { type }));
      return result;
   }

   /**
    * Get the unique file of the specified type.
    */
   public InstallFile getFile(String type) throws Exception {

      // catch the zero case there so that we can give a better
      // error message here
      LinkedList result = getFiles(type,/* allowZero = */ false);

      if (result.size() > 1) throw new Exception(Text.get(this,"e2",new Object[] { type }));

      return (InstallFile) result.getFirst();
   }

// --- validation ---

   public void validate() throws ValidationException {

      diagnoseConfig.validate();

      if (defaultTimeoutInterval < 1) throw new ValidationException(Text.get(this,"e3"));

      context.validate();

      HashSet labels = new HashSet();
      boolean before = true;

      Iterator i = files.iterator();
      while (i.hasNext()) {
         InstallFile file = (InstallFile) i.next();

         file.validate();

         Iterator j = file.labels.iterator();
         while (j.hasNext()) {
            String label = (String) j.next();
            if ( ! labels.add(label) ) throw new ValidationException(Text.get(this,"e4",new Object[] { label }));
         }
         // have to add one at a time to detect collisions correctly.
         // duplicate labels aren't actually a big problem,
         // I'm just worried that one day I'll branch on the same thing twice,
         // mis-type one of the labels, and end up jumping to the wrong block.

         // require the done flags to make sense by blocking false-true transitions
         boolean after = file.getDone();
         if ( (!before) && after ) throw new ValidationException(Text.get(this,"e5"));
         before = after;
      }
   }

}

