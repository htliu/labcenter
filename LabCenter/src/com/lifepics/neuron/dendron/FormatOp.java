/*
 * FormatOp.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.misc.Op;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

/**
 * Relic code from when we used to format whole orders instead of jobs.
 * The only reason it's still here is that I need it for existing orders.
 */

public class FormatOp {

// --- basic operations ---

   // combining the object and file operations saves recalculation

   // to look at it another way, the path fields and formatFiles list
   // are a persistent representation of the ops that made the format

   public static class MoveOrder extends Op.Move {
      protected Order order;
      public MoveOrder(Order order, File dest) {
         super( /* dest = */ dest,
                /* src  = */ order.orderDir );
         this.order = order;
      }
      public void finish() { order.orderDir = dest; }
   }

   // this is redundant with MoveOrder, but it's not worth finding the nice way
   //
   public static class TreeMoveOrder extends Op.TreeMove {
      protected Order order;
      public TreeMoveOrder(Order order, File dest) {
         super( /* dest = */ dest,
                /* src  = */ order.orderDir );
         this.order = order;
      }
      public void finish() { order.orderDir = dest; }
   }

   public static class AddPath extends Op.Move {
      protected Order.OrderFile file;
      protected String path;
      public AddPath(File orderDir, Order.OrderFile file, String path) {
         super( /* dest = */ new File(new File(orderDir,path),file.filename),
                /* src  = */ new File(orderDir,file.filename) );
         this.file = file;
         this.path = path;
      }
      public void finish() { file.path = path; }
   }

   public static class RemovePath extends Op.Move { // don't use if path is null
      protected Order.OrderFile file;
      public RemovePath(File orderDir, Order.OrderFile file) {
         super( /* dest = */ new File(orderDir,file.filename),
                /* src  = */ new File(new File(orderDir,file.path),file.filename) );
         this.file = file;
      }
      public void finish() { file.path = null; }
   }

   // in the following operations, we add to the front of the list ...
   // that's because the finish functions are executed in reverse order.

   public static class FormatMkdir extends Op.Mkdir {
      protected LinkedList list;
      protected String dir;
      public FormatMkdir(File orderDir, LinkedList list, String dir) {
         super(new File(orderDir,dir));
         this.list = list;
         this.dir  = dir;
      }
      public void finish() { list.addFirst(dir); }
   }

   public static class FormatCopy extends Op.Copy {
      protected LinkedList list;
      protected String dir;
      protected String file;
      public FormatCopy(File orderDir, LinkedList list, String dir, String file) {
         super( /* dest = */ new File(new File(orderDir,dir),file),
                /* src  = */ new File(orderDir,file) );
         this.list = list;
         this.dir  = dir;
         this.file = file;
      }
      public void finish() { list.addFirst(new File(dir,file).getPath()); }
   }

   public static abstract class FormatGenerate extends Op.Generate {
      protected LinkedList list;
      protected String dir;
      protected String file;
      public FormatGenerate(File orderDir, LinkedList list, String dir, String file) {
         super( /* dest = */ new File(new File(orderDir,dir),file) );
         this.list = list;
         this.dir = dir;
         this.file = file;
      }
      public void finish() { list.addFirst(new File(dir,file).getPath()); }
   }

// --- deploy operations ---

   public static class Deployment { // not an operation

      private File orderDir;
      private File deployDir;
      private String deployPrefix;

      public Deployment(File orderDir, File deployDir, String deployPrefix) {
         this.orderDir = orderDir;
         this.deployDir = deployDir;
         this.deployPrefix = deployPrefix;
      }

      public void undeploy(Order order) { order.deployDir = null; order.deployPrefix = null; }
      public void deploy  (Order order) { order.deployDir = deployDir; order.deployPrefix = deployPrefix; }

      public File getUndeployed(String dir) { return new File(orderDir,dir); }
      public File getDeployed  (String dir) { return new File(deployDir,deployPrefix + dir); }
   }

   public static class DeployOrder extends Op {
      protected Deployment d;
      protected Order order;
      public DeployOrder(Deployment d, Order order) {
         this.d = d;
         this.order = order;
      }
      public void dodo() {}
      public void undo() {}
      public void finish() { d.deploy(order); }
   }

   public static class UndeployOrder extends Op {
      protected Deployment d;
      protected Order order;
      public UndeployOrder(Deployment d, Order order) {
         this.d = d;
         this.order = order;
      }
      public void dodo() {}
      public void undo() {}
      public void finish() { d.undeploy(order); }
   }

   public static class DeployFile extends Op.TreeMove {
      protected LinkedList list;
      protected String dir;
      public DeployFile(Deployment d, LinkedList list, String dir) {
         super( /* dest = */ d.getDeployed  (dir),
                /* src  = */ d.getUndeployed(dir)  );
         this.list = list;
         this.dir = dir;
      }
      public void finish() { list.addFirst(dir); }
   }

   public static class UndeployFile extends Op.TreeMove {
      public UndeployFile(Deployment d, String dir) {
         super( /* dest = */ d.getUndeployed(dir),
                /* src  = */ d.getDeployed  (dir)  );
      }
   }

   public static class RedeployFile extends Op.Move {
      public RedeployFile(File deployDir, String prefixOld, String prefixNew, String dir) {
         super( /* dest = */ new File(deployDir,prefixNew + dir),
                /* src  = */ new File(deployDir,prefixOld + dir)  );
      }
   }

// --- completion operation ---

   // this is an odd one.
   //
   // the point is to handle the case where the lab has already renamed the directories.
   // so, we want to skip the rename step under certain conditions, but we still need
   // to perform the rest of the operation, because it will affect the state of the order.
   //
   // however ... we need the MoveOrder operation in both completed and uncompleted form,
   // and we also need a different operation (RedeployFile) in completed form.
   // so, there's no class hierarchy that lets me put the code in just one place;
   // instead use this wrapper object that can go around any subclass of Op.Move.

   public static class CompletionMove extends Op {

      protected Op.Move op;
      protected boolean done;
      public CompletionMove(Op.Move op) { this.op = op; done = false; }

      public void dodo() throws IOException {
         if (op.dest.exists() && ! op.src.exists()) return;
         op.dodo();
         done = true;
      }

      public void undo() { if (done) op.undo(); } // don't undo it if the lab did it
      public void finish() { op.finish(); }
   }

}

