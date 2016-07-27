/*
 * Unformat.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.misc.Op;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.ListIterator;

/**
 * Relic code from when we used to format whole orders instead of jobs.
 * The only reason it's still here is that I need it for existing orders.
 */

public class Unformat extends FormatOp {

   // this function converts from <i>any</i> format
   // to the intermediate format, which is ... flat.

   private static final String TEMP_DIR = "temp";

   /**
    * Put an order into flat format.
    * This does not update the table, it only modifies the object!
    *
    * @param dataDir The normal data directory for orders.
    */
   public static void unformat(Order order, File dataDir) throws IOException {

   // (0) move everything back to the order directory, if necessary

      LinkedList ops = new LinkedList();

      File useDir;
      String stringID = order.getFullID();

      if ( ! order.orderDir.getName().equals(stringID) ) { // (*)
         useDir = new File(dataDir,stringID);
         ops.add(new TreeMoveOrder(order,useDir)); // do this first
      } else {
         useDir = order.orderDir;
      }

      // (*) with local orders, it's no longer true that the order directory name
      // should equal the order / full ID, but there's also no way to get a local
      // order into a formatted state where unformat could then be called.

   // (0') undeploy, if necessary

      if (order.deployDir != null) {

         Deployment d = new Deployment(useDir,order.deployDir,order.deployPrefix);

         ListIterator li = order.deployFiles.listIterator();
         while (li.hasNext()) {
            ops.add(new UndeployFile(d,(String) li.next()));
         }

         ops.add(new UndeployOrder(d,order));
      }

   // (1) plan how to alter files
   // (1a) move main files back to top

      ListIterator li = order.files.listIterator();
      while (li.hasNext()) {
         Order.OrderFile file = (Order.OrderFile) li.next();
         if (file.path != null) {
            ops.add(new RemovePath(useDir,file));
         }
      }

   // (1b) clean up format files and directories
   //      this has to go second because main files may have been in directories

      File tempDir = new File(useDir,TEMP_DIR);
      ops.add(new Op.TempMkdir(tempDir));

      // go backwards so that we delete files before directories
      // use numbers for temp files because names may duplicate

      int n = 0;

      li = order.formatFiles.listIterator(order.formatFiles.size());
      while (li.hasPrevious()) {
         File src = new File(useDir,(String) li.previous());
         if (src.isDirectory()) {
            ops.add(new Op.Rmdir(src));
         } else {
            File dest = new File(tempDir,Convert.fromInt(n++));
            ops.add(new Op.TempMove(dest,src)); // this deletes at commit
         }
      }

   // (2) alter files

      Op.transact(ops);

   // (3) alter object

      // paths are set to null by transaction
      order.formatFiles.clear();
      order.deployFiles.clear();
   }

}

