/*
 * DragDropUtil.java
 */

package com.lifepics.neuron.gui;

import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.JComponent;
import javax.swing.TransferHandler;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;

/**
 * A utility class for implementing drag-drop behavior.
 */

public class DragDropUtil {

// --- DataFlavor ---

   /**
    * Get a flavor for transferring data of a specific class within the local JVM.
    * The following identity will hold:
    * c == getClassFlavor(c).getRepresentationClass().
    */
   private static DataFlavor getClassFlavor(Class c) {
      try {
         return new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=\"" + c.getName() + "\"",
                               null,c.getClassLoader());

         // annoyingly, by default DataFlavor tries to resolve the class
         // via the system class loader, and of course that doesn't work
         // when you run LC through the launcher.  so, specify a loader ...
         // DataFlavor will still try the system class loader first, which
         // is stupid, but at least it works this way.
         // the API doesn't mention that null is valid for the second arg,
         // but that's what the one-argument constructor uses internally.

      } catch (ClassNotFoundException e) {
         throw new Error(e);
         // this shouldn't happen ... the problem is that the class name
         // passed to the DataFlavor constructor might be ill-formed,
         // and not specify a class, but here we know it's a real class.
      }
   }

   private static DataFlavor workaroundFlavor = getClassFlavor(Integer.class);
   //
   // this is a workaround for a Windows-specific bug (fixed in 1.4.2).
   // basically, transfer won't work if there's no serializable flavor;
   // work around by telling a lie and reporting a fake flavor that we
   // don't actually support.  I use Integer instead of String so that
   // the text fields won't show as potential drop locations.
   //
   // it also seems to be possible to work around by making the transfer
   // classes serializable ... apparently they never get serialized,
   // but it's kind of worrisome to think that maybe sometime they could.
   //
   // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4702735
   //
   // The cause is that IDropTarget::DragEnter() is implemented so that an
   // upcall to java that invokes DropTargetListener.dragEnter is made only
   // if the drag source exports some native data formats.
   //
   // If the Transferable exports DataFlavor with javaJVMLocalObjectMimeType
   // and Serializable representation class, the Data Transfer subsystem
   // maps this flavor to a synthesized native format. If the representation
   // class is not Serializable, this flavor is not mapped to any native
   // format and the java upcall is not made.

// --- Transferable ---

   /**
    * An implementation of Transferable that transfers a single object by exact class.<p>
    *
    * It would be fun to have isDataFlavorSupported call Class.isAssignableFrom,
    * but then for consistency I think getTransferDataFlavors would have to
    * walk through all parent classes and interfaces and return them in the array.
    */
   private static class ClassTransferable implements Transferable {

      private Object data;
      private DataFlavor flavor;

   // --- construction ---

      public ClassTransferable(Object data) {
         this.data = data;
         flavor = getClassFlavor(data.getClass());
      }

   // --- implementation of Transferable ---

      public boolean isDataFlavorSupported(DataFlavor flavor) {
         return flavor.equals(this.flavor);
      }

      public DataFlavor[] getTransferDataFlavors() {
         return new DataFlavor[] { flavor, workaroundFlavor };
         // can't construct array in advance, we'd need to clone it anyway
      }

      public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
         if ( ! flavor.equals(this.flavor) ) throw new UnsupportedFlavorException(flavor);
         return data;
      }
   }

// --- module interfaces ---

   public interface ExportModule {
      Object prepare();
   }

   public interface ImportModule {
      boolean execute(Object data);
   }

// --- TransferHandler ---

   /**
    * An implementation of TransferHandler that allows
    * independent specification of an export module (or not)
    * and any number of import modules, labeled by class.<p>
    *
    * This is a fairly specialized implementation ...
    * it assumes that you want to perform the whole drag-drop
    * as a single operation, and that the transferred data
    * contains all the information needed to make that happen.
    * And, since the operation is executed during import,
    * there's no action flag available, so for simplicity
    * I just set everything to be MOVE.<p>
    *
    * Clipboard transfer is intentionally disabled.<p>
    *
    * Also, the JComponent arguments in TransferHandler are
    * only there to allow one handler to be attached to
    * multiple components.  I don't want to do that, so I just
    * ignore those arguments.
    */
   public static class ClassTransferHandler extends TransferHandler {

      private ExportModule exportModule;
      private LinkedList   importTable;

      private static class ImportEntry {
         public DataFlavor flavor;
         public ImportModule importModule;
      }

   // --- construction ---

      public ClassTransferHandler() {
         this(null);
      }
      public ClassTransferHandler(ExportModule exportModule) {
         this.exportModule = exportModule;
         importTable = new LinkedList();
      }

      public void addImport(Class c, ImportModule importModule) {
         ImportEntry entry = new ImportEntry();
         entry.flavor = getClassFlavor(c);
         entry.importModule = importModule;
         importTable.add(entry);
      }

   // --- override ---

      public void exportToClipboard(JComponent comp, Clipboard clip, int action) {
         // don't allow this, I don't want it
         exportDone(comp,null,NONE);
         // this is what parent does if source action doesn't match requested action
      }

   // --- implementation of TransferHandler (export) ---

      public int getSourceActions(JComponent src) {
         return (exportModule == null) ? NONE : MOVE;
      }

      protected Transferable createTransferable(JComponent src) {
         return (exportModule == null) ? null : new ClassTransferable(exportModule.prepare());
      }

      protected void exportDone(JComponent src, Transferable t, int action) {
         // do nothing, since the whole operation has already been executed
      }

   // --- implementation of TransferHandler (import) ---

      /**
       * @return The entry for the flavor, or null if there is none.
       */
      private ImportEntry getEntry(DataFlavor flavor) {
         Iterator i = importTable.iterator();
         while (i.hasNext()) {
            ImportEntry entry = (ImportEntry) i.next();
            if (entry.flavor.equals(flavor)) return entry;
         }
         return null;
      }

      /**
       * @return The entry for the best matching flavor, or null if there is none.
       */
      private ImportEntry getEntry(DataFlavor[] flavors) {

         // since the flavors are ordered so that the best ones are first,
         // check all the imports against the first, then all the imports
         // against the second, and so on.

         for (int i=0; i<flavors.length; i++) {
            ImportEntry entry = getEntry(flavors[i]);
            if (entry != null) return entry;
         }
         return null;
      }

      public boolean canImport(JComponent dest, DataFlavor[] flavors) {
         return (getEntry(flavors) != null);
      }

      public boolean importData(JComponent dest, Transferable t) {

         ImportEntry entry = getEntry(t.getTransferDataFlavors());
         if (entry == null) return false;

         Object data;
         try {
            data = t.getTransferData(entry.flavor);
         } catch (Exception e) { // UnsupportedFlavorException or IOException
            return false;
            // shouldn't happen, report it as a failure
         }

         return entry.importModule.execute(data);
      }

   }

}

