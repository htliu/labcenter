/*
 * DragDrop.java
 */

package com.lifepics.neuron.axon;

import com.lifepics.neuron.core.Pop;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.gui.DragDropUtil;
import com.lifepics.neuron.table.ListView;

import java.io.File;
import java.util.LinkedList;

import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

/**
 * A drag-drop utility class built on top of {@link DragDropUtil}
 * that provides custom things for the {@link ItemPanel} classes.
 */

public class DragDrop {

// --- selection classes ---

   public static class TreeSelection {
      public DefaultMutableTreeNode node;
   }

   public static class GridSelection {
      public LinkedList list;
      public Object[] o;
      public int[] oIndex; // redundant but sometimes helpful
   }

   // the cached list is necessary because the view responds
   // to selection changes during drag-drop.  how it matters:
   //
   // treeToTree: doesn't matter
   //
   // gridToTree: completely matters ... the view won't be
   // pointing to the original folder any more, so we can't
   // remove the items by removing them from the view.
   //
   // gridToGrid: shouldn't matter, the selection should
   // change back after we move the cursor out of the tree ...
   // but I don't trust that everything will be in sync.
   // there's no magic that makes the tree selection match
   // the displayed list, just the function doSelect, and
   // maybe the call to it is deferred sometimes, who knows.
   //
   // the same concern about things being in sync is why I
   // added the displayedList variable.  is it possible
   // to have a drag-drop start or stop in between when the
   // selected node changes and doSelect is called?
   // probably not, but I'm not sure enough to rely on it.

   public static class SystemTreeSel {
      public Tree.FileSystemNode node;
   }

   public static class SystemGridSel {
      public LinkedList list;
      public Object[] o;
      public int[] oIndex; // redundant but sometimes helpful
   }

   // the system selection classes are similar enough
   // to the non-system ones that we could share them,
   // but it's probably better if I can't accidentally
   // use one in place of the other.

// --- handlers ---

   public static boolean treeToTree(TreeSelection src, TreeSelection dest, DefaultTreeModel model) {
      if (dest.node == null || src.node == null) return false; // no tree selection, shouldn't happen

      // the idea is to add the source node to the end of the dest node child list,
      // but we also have to verify that the change wouldn't make the tree cyclic.
      // by the way, that check prevents the root node from ever moving .. perfect!
      //
      if (dest.node == src.node || dest.node.isNodeAncestor(src.node)) return false;
      //
      // read the second call as "for dest, is src an ancestor?",
      // not the tempting but incorrect "is dest an ancestor of src?"

      model.removeNodeFromParent(src.node);
      model.insertNodeInto(src.node,dest.node,dest.node.getChildCount());
      //
      // insertNodeInto calls MutableTreeNode.insert, which handles parent removal,
      // but it doesn't send a remove message, and the tree doesn't update correctly.

      // no selection work needed, drag leaves a good node selected

      return true;
   }

   public static boolean gridToTree(GridSelection src, TreeSelection dest, ListView view) {
      if (dest.node == null) return false; // no tree selection, shouldn't happen

      LinkedList destList = Tree.folder(dest.node).items;

      for (int i=0; i<src.o.length; i++) {
         Object o = src.o[i];
         src.list.remove(o);
         destList.add   (o);
         // it's possible we're re-adding to the same list ... but that works too
      }

      view.userChange(); // since we added directly to destList instead of to the view
      return true;
   }

   public static boolean gridToGrid(GridSelection src, GridSelection dest, ListView view) {
      LinkedList list = src.list;
      if (dest.list != src.list) return false; // out of sync, shouldn't happen

   // (1) figure out destination index

      int index = (dest.o.length > 0) ? dest.oIndex[0] : dest.list.size()-1;
      //
      // in fileToGrid, below, we allow index to run from 0 to size, inclusive,
      // and just insert at that index.  here, though, we need to subtract 1,
      // because the algorithm is not designed to handle an index equal to size.
      // also, although ThumbnailGrid supports drag-drop with no dest selection,
      // JTable doesn't, so we do still need the whole algorithm.
      //
      // it's unfortunate that gridToGrid and fileToGrid have different behavior,
      // but I don't see any reasonable way to make them the same.

      if (src.o.length == 0) return false; // doesn't happen, and complicates the proof

      // to understand what's going on here, imagine the list has four items,
      // ABCD, and the item C is dragged.  there are four possible destinations,
      // and four possible insertion points in the list ABD .. so match them up!
      // so, drag to A will insert at the start, and drag to D at the end.
      //
      // it seems like that behavior might be unclear to the user, but I think
      // it will make some sense ... drag upward inserts before, and drag down
      // inserts after.  or, to put it another way, dragging one step to transpose
      // always works just like you'd want.
      //
      // then, if the list is ABCDE, with CD selected, the five destinations
      // should map to the four insertion points in ABE as 01223, that's easy.
      // the tough case is if the selection is discontiguous, say CE selected
      // in list ABCDEF.  there, everything is clear except what D means;
      // you just have to pick one of the selected objects as the origin,
      // and that might as well be the first one.  also, discontiguous drag
      // is a weird case, don't worry too much about it.

      // so, the rule is, subtract one for each selected item below index,
      // except don't look at src.oIndex[0], which is the origin object.

      int count = 0;
      for (int i=1; i<src.oIndex.length; i++) {
         if (src.oIndex[i] <= index) count++; // don't modify index yet!
      }

      index -= count;

      // here's a proof that the insertion point will always be valid.
      // let L be the initial length of list, N be number of selected items,
      // and x be the drop selection point.  so, 0 <= x < L.
      // also let C be the count of selected items in [0,x],
      // but excluding the first selected item at src.oIndex[0].
      // what we need to show is that 0 <= x-C <= L-N.
      //
      // we enforce N >= 1, above, which also guarantees L >= 1.
      //
      // for the left inequality, we know there can be at most
      // x+1 selected items in [0,x], and if there are any,
      // one of them has to be the first, so really there can be at most x.
      // (and, if there aren't any, then still C = 0 <= x.)
      // so, C <= x, therefore 0 <= x-C.
      //
      // for the right inequality, if x = L-1 then all selected items
      // are included in [0,x], so excluding the first, we know C = N-1,
      // therefore x-C = (L-1) - (N-1) = L-N, right at the limit.
      // if x = L-2, then C could drop by one, but still C >= N-2,
      // so x-C <= (L-2) - (N-2) = L-N, and so on for other values of x.
      //
      // Q.E.D.

   // (2) move into a temporary list

      LinkedList temp = new LinkedList();

      for (int i=0; i<src.o.length; i++) {
         Object o = src.o[i];
         list.remove(o);
         temp.add   (o);
      }

   // (3) insert at correct location

      list.addAll(index,temp);

      view.userChange(); // since we didn't work through the view object
      return true;
   }

   public static boolean folderTree(SystemTreeSel src, TreeSelection dest,
                                    DefaultMutableTreeNode root, DefaultTreeModel model, JTree tree, JPanel owner, Runnable updateCount) {
      if (src.node == null) return false; // no tree selection, shouldn't happen

      // dest selection is totally ignored
      //
      // we could probably reduce the argument set, but why bother, it works as is

      if ( ! validateFolderAdd(owner,root) ) return false;

      File dir = src.node.getFile();
      if (dir == null) return false; // don't allow dragging entire file system!
      // the current scheme doesn't use a null node,
      // so I figure it's sufficient to test for it and fail silently.

      executeFolderAdd(dir,root,model,tree,updateCount);
      return true;
   }

   public static boolean fileToTree(SystemGridSel src, TreeSelection dest, ListView view, Runnable updateCount) {
      if (dest.node == null) return false; // no tree selection, shouldn't happen

      // same as gridToTree except we make items and don't remove from source

      LinkedList destList = Tree.folder(dest.node).items;

      for (int i=0; i<src.o.length; i++) {
         destList.add(ItemUtil.makeItem((File) src.o[i]));
      }

      view.userChange(); // since we added directly to destList instead of to the view
      updateCount.run();
      return true;
   }

   public static boolean fileToGrid(SystemGridSel src, GridSelection dest, ListView view, Runnable updateCount) {

      // much easier than gridToGrid because the lists are guaranteed different.

      // the temporary list is convenient for several reasons:
      // * if we added individual elements, we'd have to traverse the list each time.
      // * we'd also have to adjust the index after each addition.
      // * I thought there was another reason, but I forgot what it was.

      int index = (dest.o.length > 0) ? dest.oIndex[0] : dest.list.size();

      LinkedList temp = new LinkedList();
      for (int i=0; i<src.o.length; i++) {
         temp.add(ItemUtil.makeItem((File) src.o[i]));
      }

      dest.list.addAll(index,temp);

      view.userChange(); // since we didn't work through the view object
      updateCount.run();
      return true;
   }

// --- folder add functions ---

   public static boolean validateFolderAdd(JPanel owner, DefaultMutableTreeNode root) {

      Tree.Folder rootFolder = Tree.folder(root);

      // make sure the user hasn't done anything except maybe set the album name
      if (    root.getChildCount() != 0
           || rootFolder.items.size() != 0 ) {
         Pop.info(owner,Text.get(DragDrop.class,"e9"),Text.get(DragDrop.class,"s26"));
         return false;
      }
      // it would be so easy to relax this condition, and allow adding the folder
      // as a subfolder of the selected node.  however, it's a bad idea.
      // the users are supposed to organize things on disk first, then add here,
      // so 99% of the time, the function they want is the root add with album
      // name replacement, not the subfolder add.  so, we can't remove root add.
      // and, if we allow both, then either we clutter the UI with a switch,
      // or we confuse the user by having the program decide by arbitrary rules.
      // it's just not worth messing with for a 1% case.

      return true;
   }

   public static void executeFolderAdd(File dir, DefaultMutableTreeNode root, DefaultTreeModel model, JTree tree, Runnable updateCount) {

      Tree.makeItemsInto(root,dir);

      Tree.Folder rootFolder = Tree.folder(root);

      // bonus feature, fill in the album name if it's blank
      if (rootFolder.name.equals(Tree.blankAlbumName)) {
         rootFolder.name = dir.getName();
         // no notification needed, we notify for everything below
      }

      model.reload(); // exactly same as reload(root) or nodeStructureChanged(root)
      tree.setSelectionRow(0); // else tree has no selection, and that's not right
      updateCount.run();
      // no need for viewHelper notification, we know it was empty

      // n.b., the root node automatically shows all its children,
      // just as it does at construction.  i don't know the mechanism ...
      // even if I double-click it before adding the folder,
      // the children still show, so it's not just that the node is
      // marked as expanded.
   }

}

