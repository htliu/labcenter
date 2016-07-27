/*
 * Op.java
 */

package com.lifepics.neuron.misc;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.object.XML;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.LinkedList;
import java.util.ListIterator;

import org.w3c.dom.Document;

/**
 * Classes and functions that make it possible to perform
 * operations on bunches of files as a single transaction.<p>
 *
 * It's kind of a strange construction, but the class (Op)
 * acts both as an abstract superclass and as a namespace.
 */

public abstract class Op {

// --- abstract superclass ---

   public abstract void dodo() throws IOException;
   public          void finish() {}
   public abstract void undo();
   public          void undoPartial() {}

// --- atomic operations ---

   public static class Mkdir extends Op {
      protected File dir;
      public Mkdir(File dir) { this.dir = dir; }

      public void dodo() throws IOException {
         if ( ! dir.mkdir() ) {
            throw new IOException(Text.get(Op.class,"e3",new Object[] { Convert.fromFile(dir) }));
         }
      }
      public void undo() {
         dir.delete(); // ignore result
      }
   }

   public static class Rmdir extends Op { // the inverse of Mkdir
      protected File dir;
      public Rmdir(File dir) { this.dir = dir; }

      public void dodo() throws IOException {
         if ( ! dir.delete() ) {
            throw new IOException(Text.get(Op.class,"e4",new Object[] { Convert.fromFile(dir) }));
         }
      }
      public void undo() {
         dir.mkdir(); // ignore result
      }
   }

   public static class Move extends Op {
      public File dest;
      public File src;
      public Move(File dest, File src) { this.dest = dest; this.src = src; }

      public void dodo() throws IOException {
         if ( ! src.renameTo(dest) ) {
            throw new IOException(Text.get(Op.class,"e5",new Object[] { Convert.fromFile(src) }));
         }
      }
      public void undo() {
         dest.renameTo(src); // ignore result
      }
   }

   public static class Copy extends Op {
      protected File dest;
      protected File src;
      public Copy(File dest, File src) { this.dest = dest; this.src = src; }

      public void dodo() throws IOException {
         FileUtil.copy(dest,src);
      }
      public void undo() {
         dest.delete(); // ignore result
      }
      public void undoPartial() { undo(); }
   }

   public static class MakeEmpty extends Op { // cf. InstallFile.METHOD_MKEMPTY
      protected File dest;
      public MakeEmpty(File dest) { this.dest = dest; }

      public void dodo() throws IOException {
         if ( ! dest.createNewFile() ) { // can return false or throw exception
            throw new IOException(Text.get(Op.class,"e1",new Object[] { Convert.fromFile(dest) }));
         }
      }
      public void undo() {
         dest.delete(); // ignore result
      }
   }

   /**
    * Superclass for file generation as part of a transaction.
    */
   public static abstract class Generate extends Op {
      protected File dest;
      public Generate(File dest) { this.dest = dest; }

      // this is how a PrintWriter gets the correct separator
      // (we don't use PrintWriter because it hides exceptions)
      //
      protected static final String line = System.getProperty("line.separator");

      public void dodo() throws IOException {
         FileOutputStream outputStream = null;

         // caller is responsible for checking nonexistence.
         // it's no good calling checkNotExists here,
         // because during undo we'd delete the existing file.
         // usually we'll be writing into a new directory,
         // so it's not an issue ... but it matters for Fuji.

         try {
            outputStream = new FileOutputStream(dest);
            OutputStreamWriter writer = new OutputStreamWriter(outputStream);

            subdo(writer);

            writer.close(); // don't ignore exceptions on this one

         } finally {
            try {
               if (outputStream != null) outputStream.close();
            } catch (IOException e) {
               // ignore
            }
         }
      }

      public abstract void subdo(Writer writer) throws IOException;

      public void undo() {
         dest.delete(); // ignore result
      }
      public void undoPartial() { undo(); }
   }

   /**
    * Superclass for XML file generation as part of a transaction.
    */
   public static abstract class GenerateXML extends Op {

      protected File dest;
      private String doctype;

      public GenerateXML(File dest) { this(dest,null); }
      public GenerateXML(File dest, String doctype) { this.dest = dest; this.doctype = doctype; }

      public void dodo() throws IOException {

         Document doc = XML.createDocument();
         subdo(doc);
         XML.writeFile(dest,doc,null,null,doctype);
      }

      public abstract void subdo(Document doc) throws IOException;

      public void undo() {
         dest.delete(); // ignore result
      }
      public void undoPartial() { undo(); }
   }

   /**
    * Write an XML document (that already exists) into a file.
    * This is <i>not</i> a superclass, you just use it as is.
    */
   public static class WriteXML extends Op {
      protected File dest;
      protected Document doc;
      public WriteXML(File dest, Document doc) { this.dest = dest; this.doc = doc; }

      public void dodo() throws IOException {
         XML.writeFile(dest,doc);
      }

      public void undo() {
         dest.delete(); // ignore result
      }
      public void undoPartial() { undo(); }
   }

   /**
    * Make a temp directory for rollback, but delete it in the end.
    */
   public static class TempMkdir extends Mkdir {
      public TempMkdir(File dir) { super(dir); }
      public void finish() {
         dir.delete(); // ignore result
      }
   }

   /**
    * Move a file to a temp directory for rollback, but delete it in the end.
    */
   public static class TempMove extends Move {
      public TempMove(File dest, File src) { super(dest,src); }
      public void finish() {
         dest.delete(); // ignore result
      }
   }

   /**
    * Copy a file, then delete the source in the end.
    *
    * We had trouble on some versions of Windows trying to move folders
    * to/from network drives.  I think moving files is OK, but it might
    * be useful to have this op available as a substitute, just in case.
    *
    * Here we don't move the source file to a temp directory,
    * but if we get into the finish stage, we managed to copy it,
    * so we do have some confidence that it exists and is a file.
    *
    * Also, in the actual cases where we use this, we don't know
    * that the files are all in the same place, or even on the
    * same drive, so there's no natural place for a temp directory,
    * and in fact we'd just be asking for network drive trouble.
    */
   public static class CopyAndDelete extends Copy {
      public CopyAndDelete(File dest, File src) { super(dest,src); }
      public void finish() {
         src.delete(); // ignore result
      }
   }

// --- execution of atomic operations ---

   public static void transact(LinkedList ops) throws IOException {
      ListIterator li = ops.listIterator();
      try {

         while (li.hasNext()) {
            Op op = (Op) li.next();
            op.dodo();
         }

         // ops succeeded, now finish up

         // reverse order is both convenient and important
         while (li.hasPrevious()) {
            Op op = (Op) li.previous();
            op.finish();
         }

      } catch (IOException e) {

         if (li.hasPrevious()) { // always
            Op op = (Op) li.previous();
            op.undoPartial();
         }

         while (li.hasPrevious()) {
            Op op = (Op) li.previous();
            op.undo();
         }

         throw e;
      }
   }

// --- tree move ---

   // this replaces the plain move operation when a directory is being moved.
   // the plain move usually works, even in that case, but sometimes it doesn't ...
   // best guess, if you're moving across drives (so that a copy is required)
   // and one or both of the drives is on a computer with a Windows before XP.
   //
   // since this operation uses the OS to move/copy the files, it's reasonably
   // efficient, no need to try the plain move before attempting the tree move.

   // instead of maintaining detailed undo information, we just check that the
   // dest directory doesn't exist before doing anything,
   // then anything that's there represents something that needs to be undone.

   // note that empty (sub)directories are handled correctly.

   // the only reason this is a subclass of Move is so FormatOp.CompletionMove can apply

   public static class TreeMove extends Move {

      private boolean started;
      public TreeMove(File dest, File src) { super(dest,src); started = false; }

      public void dodo() throws IOException {

         if (dest.exists()) {
            throw new IOException(Text.get(Op.class,"e6",new Object[] { Convert.fromFile(dest) }));
         }

         started = true;

         treeMove(src,dest);
      }

      public void undo() {

         // this may be a partial undo, so don't require that src not exist

         if ( ! started ) return; // only happens with undoPartial, but check here anyway
         // we have to check this, or else an initial "dest exists" error
         // would lead to us undoing something that we didn't actually do.

         try {
            treeMove(dest,src);
         } catch (IOException e) {
            // ignore result
         }
      }
      public void undoPartial() { undo(); }
   }

   // move a tree onto a destination which may or may not exist.
   // the code responds well to all possible cases
   // of whether src and dest exist and are directories.
   // (the destination will only exist if we're undoing.)
   //
   private static void treeMove(File src, File dest) throws IOException {

      // the idea is "if ( ! src.isDirectory() )", but if we did that,
      // we'd still have to check below for the file array being null.
      // do this before creating dest, to avoid unnecessary activity.
      //
      String[] file = src.list();
      if (file == null) {
         throw new IOException(Text.get(Op.class,"e10",new Object[] { Convert.fromFile(src) }));
      }

      if (dest.exists()) {
         if ( ! dest.isDirectory() ) {
            throw new IOException(Text.get(Op.class,"e11",new Object[] { Convert.fromFile(dest) }));
         }
      } else {
         if ( ! dest.mkdir() ) {
            throw new IOException(Text.get(Op.class,"e7",new Object[] { Convert.fromFile(dest) }));
         }
      }

      for (int i=0; i<file.length; i++) {
         File srcChild  = new File(src, file[i]);
         File destChild = new File(dest,file[i]);

         if (srcChild.isDirectory()) {
            treeMove(srcChild,destChild);
         } else {
            if ( ! srcChild.renameTo(destChild) ) {
               throw new IOException(Text.get(Op.class,"e8",new Object[] { Convert.fromFile(srcChild) }));
            }
         }
      }

      if ( ! src.delete() ) {
         throw new IOException(Text.get(Op.class,"e9",new Object[] { Convert.fromFile(src) }));
      }
   }

}

