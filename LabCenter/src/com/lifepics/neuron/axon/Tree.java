/*
 * Tree.java
 */

package com.lifepics.neuron.axon;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Nullable;
import com.lifepics.neuron.core.Pop;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.misc.FileUtil;

import java.io.File;
import java.io.FileFilter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JComboBox;
import javax.swing.JTree;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.*;
import java.awt.Component;

/**
 * Utility classes and functions related to trees.
 */

public class Tree {

// --- folder class ---

   public static class Folder {

      public String name;
      public LinkedList items;

      public Folder(String name) {
         this.name = name;
         items = new LinkedList();
      }

      public String toString() { return name; }
   }

   public static Folder folder(DefaultMutableTreeNode node) { return (Folder) node.getUserObject(); }

// --- model class ---

   public static class FolderModel extends DefaultTreeModel {

      public FolderModel(TreeNode root) { super(root); }

      public void valueForPathChanged(TreePath path, Object newValue) {
         DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
         folder(node).name = ((String) newValue).trim();
         nodeChanged(node);
         // code adapted from DefaultTreeModel.valueForPathChanged;
         // by default it replaces the user object with the string.
      }
   }

// --- load sequence ---

   public static final String blankAlbumName = Text.get(Tree.class,"s30");
   //
   // at first I thought I'd make this depend on the roll number, "Roll {0}",
   // to match how the rolls show up on the server if sent with a blank album name,
   // but there's a catch ... when we're creating a new roll, we don't know the ID.
   // I guess we could say "Roll NEW", to match the "NEW" in the roll ID field,
   // but that's not pretty enough to be worth the amount of effort it would take.
   // also it would be less clear that you're supposed to rename it;
   // also it would make LC depend on the server code in a way that it doesn't now.

   /**
    * Transfer the roll's album name and items into a tree structure.
    * While in the tree, the items' subalbum field is null / ignored.<p>
    *
    * Also, note that the order of items and the order of subfolders
    * is determined by the order of appearance in the roll item list.
    * Any order information beyond that will be lost.
    */
   public static DefaultMutableTreeNode load(String album, LinkedList items) {

      if (album == null) album = blankAlbumName;
      DefaultMutableTreeNode root = new DefaultMutableTreeNode(new Folder(album));

      Iterator i = items.iterator();
      while (i.hasNext()) {
         Roll.Item item = (Roll.Item) i.next(); // n.b., no preitems here

         DefaultMutableTreeNode node;
         if (item.subalbum == null) {
            node = root;
         } else {
            node = traversePath(root,item.subalbum);
         }
         folder(node).items.add(item);
      }

      return root;
   }

   /**
    * @param path The path, not null.
    */
   private static DefaultMutableTreeNode traversePath(DefaultMutableTreeNode node, String path) {

      // expand path into name list
      LinkedList names = new LinkedList();
      File file = new File(path);
      while (file != null) {
         names.addFirst(file.getName());
         file = file.getParentFile();
      }

      // step down one level per name
      Iterator i = names.iterator();
      while (i.hasNext()) {
         String name = (String) i.next();
         node = traverseName(node,name);
      }

      return node;
   }

   private static DefaultMutableTreeNode traverseName(DefaultMutableTreeNode node, String name) {
      DefaultMutableTreeNode child;

      // indexed access probably inefficient, but tree should be small
      for (int i=0; i<node.getChildCount(); i++) {
         child = (DefaultMutableTreeNode) node.getChildAt(i);
         String childName = folder(child).name;
         if (childName.equals(name)) return child;
      }

      // not found, insert new at end
      child = new DefaultMutableTreeNode(new Folder(name));
      node.add(child);
      return child;
   }

// --- store sequence ---

   /**
    * Transfer the tree structure into an item list.
    */
   public static LinkedList store(DefaultMutableTreeNode root) throws ValidationException {
      LinkedList list = new LinkedList();
      storeRecursive(list,root,null);
      return list;
   }

   private static void storeRecursive(LinkedList items, DefaultMutableTreeNode node, String subalbum) throws ValidationException {

   // validation to prevent folders from being lost / destroyed

      // a folder would be destroyed if it and its children contained
      // no items.  but, in that case, then somewhere there would be
      // a node that had no items and no children; throw the exception
      // on that node rather than counting at every step.

      // if the root is empty, the folder won't be destroyed,
      // so this error is inappropriate.
      // the user will still get an empty roll warning later.

      // the folder name of this folder has already been validated,
      // so it's not blank, so therefore the error will make sense.

      Folder nodeFolder = folder(node);
      if (    subalbum != null               // node != root, but we're in a static context
           && nodeFolder.items.size() == 0
           && node   .getChildCount() == 0 ) throw new ValidationException(Text.get(Tree.class,"e13",new Object[] { nodeFolder.name }));

   // items in this folder come first

      Iterator i = nodeFolder.items.iterator();
      while (i.hasNext()) {
         Roll.Item item = (Roll.Item) i.next(); // may be PreItem, no matter

         // quick validation
         if (item.originalFilename != null) validateFileName(item.originalFilename);
         // this isn't perfect, because if there's disambiguation later,
         // the original file name will be validated on the <i>next</i> user edit.
         // and, that's just the original name of the file, not even user entered.

         item.subalbum = subalbum; // fill these in only at end, since user may have rearranged
         items.add(item);
      }

   // items in subfolders come second

      for (int j=0; j<node.getChildCount(); j++) {
         DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(j);

         String childName = folder(child).name;
         validateFolderName(childName,/* isSubalbum = */ true);

         String childSubalbum = new File(subalbum,childName).getPath();
            // note file constructor handles null subalbum correctly
         storeRecursive(items,child,childSubalbum);
         // this part is just like ItemUtil.makeItemsInto
      }
   }

   // we validate folder names here, because we have them on hand,
   // and it would be a pain to dig them out of the paths later.
   // then it makes some sense to validate the file names here too,
   // even though that could equally well be done in the object.

   /**
    * @param name The folder name under consideration ... already trimmed, not null.
    */
   private static void validateFolderName(String name, boolean isSubalbum) throws ValidationException {
      if (name.length() == 0) throw new ValidationException(Text.get(Tree.class,"e12"));
      if (isSubalbum) Roll.validateSubalbum(name);
      // else we'll do the same validation later on as part of Roll.validate
   }

   /**
    * @param name The file name under consideration ... already trimmed, not null.
    */
   private static void validateFileName(String name) throws ValidationException {
      if (name.length() == 0) throw new ValidationException(Text.get(Tree.class,"e14"));
   }

// --- utilities ---

   public static int countItems(DefaultMutableTreeNode node) {

      int count = folder(node).items.size();

      for (int i=0; i<node.getChildCount(); i++) {
         DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
         count += countItems(child);
      }

      return count;
   }

   /**
    * A version of ItemUtil.makeItemsInto that works with tree nodes.
    */
   public static void makeItemsInto(DefaultMutableTreeNode node, File dir) {

      File[] files = dir.listFiles();
      if (files == null) return;
      FileUtil.sortByName(files);

      LinkedList items = folder(node).items;

      for (int i=0; i<files.length; i++) {
         if (files[i].isDirectory()) {
            DefaultMutableTreeNode child = traverseName(node,files[i].getName());
            makeItemsInto(child,files[i]);
         } else {
            if (ItemUtil.imageFileFilter.accept(files[i])) items.add(ItemUtil.makeItem(files[i]));
            // no subalbum argument here, the field stays null until store
         }
      }
   }

// --- UI utilities ---

   public static JTree construct(TreeModel model) {

      JTree tree = new JTree(model);
      tree.setVisibleRowCount(Text.getInt(Tree.class,"n3"));
      tree.setInvokesStopCellEditing(true); // save edits if user selects different node
      tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

      DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
      renderer.setLeafIcon(renderer.getClosedIcon());
      tree.setCellRenderer(renderer);
      // n.b., the default renderer is com.sun.java.swing.plaf.windows.WindowsTreeUI$WindowsTreeCellRenderer.
      // it has slightly different behavior;
      // in particular, it shows an open folder for selected node only.

      return tree;
   }

   public static void setAlbum(DefaultTreeModel model, DefaultMutableTreeNode root, String album) throws ValidationException {

      // null not supported here, no conversion to blankAlbumName needed
      album = album.trim();
      if (album.length() == 0) throw new ValidationException(Text.get(Tree.class,"e15"));
      Roll.validateAlbum(album,/* claimNumber = */ true);
      // validate up front so that we don't spring any mysterious errors about albums
      // or subalbums on the user later on

      folder(root).name = album;
      model.nodeChanged(root);
      // much like FolderModel.valueForPathChanged
   }

   public static String getAlbum(DefaultMutableTreeNode root) throws ValidationException {

      String album = folder(root).name;
      if (album.equals(blankAlbumName)) {
         return null;
      } else {
         validateFolderName(album,/* isSubalbum = */ false);
         return album;
      }
   }

   public static boolean createSubalbum(DefaultTreeModel model, JTree tree) {

      DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
      if (node == null) return false; // shouldn't happen

      boolean childOnly = (node.getParent() == null); // (node == root)

      SubalbumDialog sd = new SubalbumDialog((Dialog) tree.getTopLevelAncestor(),childOnly);
      if ( ! sd.run() ) return false; // canceled

      DefaultMutableTreeNode subalbum = new DefaultMutableTreeNode(new Folder(sd.r_name));

      // now that the model exists, all modifications must go through it
      if (sd.r_createChild) {
         model.insertNodeInto(subalbum,node,node.getChildCount());
      } else { // create sibling
         DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
         model.insertNodeInto(subalbum,parent,parent.getIndex(node)+1);
      }

      TreePath path = new TreePath(subalbum.getPath());
      tree.scrollPathToVisible(path);
      tree.setSelectionPath(path); // triggers change to image list

      return true;
   }

   public static boolean deleteSubalbum(DefaultTreeModel model, JTree tree, Component parent, DefaultMutableTreeNode root) {

      DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
      if (node == null) return false; // shouldn't happen

      if (node == root) {
         Pop.error(parent,Text.get(Tree.class,"e10"),Text.get(Tree.class,"s31"));
         return false;
      }

      int count = countItems(node);
      if (count > 0) {
         String s = Text.get(Tree.class,"e11",new Object[] { new Integer(count), Convert.fromInt(count) });
         boolean confirmed = Pop.confirm(parent,s,Text.get(Tree.class,"s32"));
         if ( ! confirmed ) return false;
      }

      int row = tree.getMinSelectionRow(); // not zero because not root

      // now that the model exists, all modifications must go through it
      model.removeNodeFromParent(node);

      tree.setSelectionRow(row-1); // triggers change to image list
      return true;
   }

// --- file system node ---

   // we could use files as nodes, but this is way better since it caches the results.

   // the point of scanning for children (subdirectories) and target files
   // at the same time is to avoid reading the same directory twice.
   // not a big deal, unless the directory errors out with a message to the
   // user -- like you get if you try to read from A:\ with no disk present.

   public static class FileSystemNode {

      private File file; // null if root
      private FileFilter filter;

      private FileSystemNode[] children; // null until used
      private LinkedList targetFiles;    //

      /**
       * @param filter The file filter to be used for target files.
       *               It must accept directories or the tree won't work.
       */
      public FileSystemNode(File file, FileFilter filter) {
         this.file = file;
         this.filter = filter;
      }

      public File getFile() { return file; }

      // we don't construct redundant nodes, so equality by identity is fine

      public boolean isLoaded() { return (children != null); }

      private void load() {

         File[] files;
         if (file == null) { // root node
            files = File.listRoots();
         } else { // normal node
            files = file.listFiles(filter);
         }

         if (files == null) files = new File[0]; // I/O error

         FileUtil.sortByName(files);

         LinkedList childFiles = new LinkedList();
         targetFiles = new LinkedList();

         for (int i=0; i<files.length; i++) {
            if (file == null || files[i].isDirectory()) {
               childFiles.add(files[i]);
            } else {
               targetFiles.add(files[i]);
            }
            // some roots (e.g., A:\) claim they're not directories
            // when they don't have a disk.
            // so, override that and poke in the correct value.
         }

         children = new FileSystemNode[childFiles.size()];
         int i = 0;

         Iterator j = childFiles.iterator();
         while (j.hasNext()) {
            children[i++] = new FileSystemNode((File) j.next(),filter);
         }

         // instead of putting the child files in a list,
         // we could just scan the files array twice,
         // but then we'd allocate children as files.length - targetFiles.size(),
         // so if an isDirectory value changed at the wrong time,
         // we'd get an array out of bound exception ... naughty!
      }

      public FileSystemNode[] getChildren() {
         if (children == null) load();
         return children;
      }

      public LinkedList getTargetFiles() {
         if (children == null) load();
         return targetFiles;
      }

      public String toString() {
         String name;
         if (file == null) {
            name = "";
         } else {
            name = file.getName();
            if (name == null || name.length() == 0) name = file.toString(); // roots do this
         }
         return name;
      }
   }

// --- helpers ---

   private static class FileSystemTreePath extends TreePath {
      public FileSystemTreePath(Object singlePath) {
         super(singlePath);
      }
      public FileSystemTreePath(FileSystemTreePath parent, Object lastElement) {
         super(parent,lastElement);
      }
      // the point is to gain access to the protected constructor.
      // the single-argument constructor is just for parallelism.
   }

// --- file system model ---

   // it was tempting to use a DefaultTreeModel with user objects
   // of class File, until I realized File.toString didn't work.
   // actually the TreeModel approach is much cleaner, and doesn't
   // require monitoring open and close events.

   public static class FileSystemModel implements TreeModel {

      public FileSystemNode root;

      /**
       * @param root The directory to use as the root of this model,
       *             or null for a node that contains File.getRoots.
       */
      public FileSystemModel(File root, FileFilter filter) { this.root = new FileSystemNode(root,filter); }

   // --- implementation of TreeModel ---

      public Object getRoot() {
         return root;
      }

      public boolean isLeaf(Object node) {

         if ( ! ((FileSystemNode) node).isLoaded() ) return false;
         // this is the only function called on unexpanded nodes,
         // so by overriding it, we avoid scanning one level too deep.
         // this produces the "click and see [+] go away" behavior.

         FileSystemNode[] children = ((FileSystemNode) node).getChildren();
         return (children.length == 0);
         // in general, length == 0 doesn't imply leaf, but here it does
      }

      public int getChildCount(Object node) {
         FileSystemNode[] children = ((FileSystemNode) node).getChildren();
         return children.length;
      }

      public Object getChild(Object node, int index) {
         FileSystemNode[] children = ((FileSystemNode) node).getChildren();
         return children[index];
      }

      public int getIndexOfChild(Object node, Object child) {
         FileSystemNode[] children = ((FileSystemNode) node).getChildren();
         for (int i=0; i<children.length; i++) {
            if (children[i] == child) return i;
         }
         return -1;
      }

      // the model doesn't change, we can totally ignore listeners
      public void addTreeModelListener(TreeModelListener l) {}
      public void removeTreeModelListener(TreeModelListener l) {}

      // this doesn't happen either
      public void valueForPathChanged(TreePath path, Object newValue) {}

   // --- find functions ---

      /**
       * @return The path to the given file (directory),
       *         or at least the closest match.
       */

      public FileSystemTreePath traversePath(File file) {

         FileSystemTreePath path = new FileSystemTreePath(root);

         LinkedList stack = new LinkedList();
         // I would have liked to store this on the call stack,
         // but that makes it hard to exit if there's no match.

         while ( ! Nullable.equals(root.getFile(),file) ) {
            if (file == null) return path; // totally outside tree, return root
            stack.addFirst(file);
            file = file.getParentFile();
         }
         // note that the null returned by file.getParentFile will match
         // the null that I use as the file value of the above-root node.

         while ( ! stack.isEmpty() ) {
            FileSystemNode node = traverseName(path.getLastPathComponent(),(File) stack.removeFirst());
            if (node == null) return path; // structure changed, return partial match
            path = new FileSystemTreePath(path,node);
         }

         return path; // complete match
      }

      private FileSystemNode traverseName(Object node, File file) {
         FileSystemNode[] children = ((FileSystemNode) node).getChildren();
         for (int i=0; i<children.length; i++) {
            if (Nullable.equals(children[i].getFile(),file)) return children[i];
         }
         return null;

         // traverseName compares by File equality, not by name,
         // because root nodes have weird names, cf. FileSystemNode.toString.

         // we don't really need to use Nullable.equals in traverseName,
         // since the file of a child node is never null,
         // but I like the parallel structure, it seems nicer this way.
      }
   }

// --- drive combo ---

   public static FileSystemModel newModel(File drive) {
      return new FileSystemModel(drive,ItemUtil.imageFileFilter);
   }

   private static class DriveComboEntry {

      private FileSystemModel model;
      private File selection;
   }

   public static class DriveCombo extends JComboBox {

      private JTree tree;

      private File drive;
      private HashMap map;

      /**
       * Construct a drive combo for the given tree.
       *
       * @param tree The tree that the combo should be linked to.
       * @param drive The initial drive value, must match the tree selection.
       *              Could be derived from the tree, but the caller has it.
       */
      public DriveCombo(JTree tree, File drive) {

         this.tree = tree;

         this.drive = drive;
         map = new HashMap(); // stays empty until drive changes

      // the actual combo

         File[] files = File.listRoots();
         if (files != null) {
            FileUtil.sortByName(files);
            for (int i=0; i<files.length; i++) {
               addItem(files[i].toString());
            }
         }
         // else no roots in the list, oh well

         setEditable(true);
         setSelectedItem(drive.toString());

         addActionListener(listener(/* refresh = */ false));
      }

      public ActionListener listener(final boolean refresh) {
         return new ActionListener() { public void actionPerformed(ActionEvent e) { update(refresh); } };
      }
      // note, we can't make the combo box be an action listener, because
      // it already is one, and already uses it for some internal purpose.  grr!

      private void update(boolean refresh) {

         Object o = getSelectedItem();
         if (o == null) return; // shouldn't happen

      // handle drive variable

         File driveOld = drive;
         File driveNew = new File( ((String) o).trim() );

         if (driveNew.equals(driveOld) && ! refresh) return;

         drive = driveNew;

         // when you type in a string by hand,
         // the combo sends two action events; this removes the second,
         // and also removes edits / selections that aren't really new.

      // archive old drive

         // entry should have been pulled, but if it exists, overwrite.
         // the idea is, the map holds the trees that are *not* in use.

         DriveComboEntry entry = new DriveComboEntry();

         entry.model = (FileSystemModel) tree.getModel();

         FileSystemNode node = (FileSystemNode) tree.getLastSelectedPathComponent();
         entry.selection = (node != null) ? node.getFile() : null;
         // null shouldn't happen, since we always set a selection, but handle it

         map.put(driveOld,entry);

      // restore new drive (same as old, if refresh)

         entry = (DriveComboEntry) map.remove(driveNew); // pull out of map!

         FileSystemModel model = (entry != null && ! refresh) ? entry.model : newModel(driveNew);
         tree.setModel(model);
         // if refreshing, existing model is discarded

         if (entry != null && entry.selection != null) {
            TreePath path = model.traversePath(entry.selection);
            tree.setSelectionPath(path);
            tree.scrollPathToVisible(path);
         } else {
            tree.setSelectionRow(0);
         }
      }
   }

}

