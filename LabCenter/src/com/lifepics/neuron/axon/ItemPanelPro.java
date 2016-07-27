/*
 * ItemPanelPro.java
 */

package com.lifepics.neuron.axon;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Pop;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.gui.DragDropUtil;
import com.lifepics.neuron.gui.Field;
import com.lifepics.neuron.gui.Graphic;
import com.lifepics.neuron.gui.GridBagHelper;
import com.lifepics.neuron.gui.Rotation;
import com.lifepics.neuron.gui.ThumbnailCache;
import com.lifepics.neuron.gui.ThumbnailGrid;
import com.lifepics.neuron.gui.ThumbnailUtil;
import com.lifepics.neuron.misc.FileMapper;
import com.lifepics.neuron.table.ListView;

import java.io.File;
import java.util.Comparator;
import java.util.LinkedList;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;

/**
 * Another panel for editing roll item information
 */

public class ItemPanelPro extends ItemPanel implements Runnable {

// --- fields ---

   private File rollDir;
   private File currentDir;

   // general
   private ThumbnailCache cache;
   private JTextField count;
   private Component lastComponent;

   // system side
   private JTree systemTree;
   private ListView systemView;
   private LinkedList systemDisplayedList;
   private ThumbnailGrid systemGrid;

   // upload side
   private DefaultMutableTreeNode root;
   private DefaultTreeModel model;
   private JTree tree;
   private ListView view;
   private LinkedList displayedList;
   private ThumbnailGrid grid;

// --- constants ---

   private static Comparator comparator = RollUtil.orderItemOriginalFilename;

// --- construction ---

   public ItemPanelPro(Dialog owner, String album, LinkedList items, File rollDir, File currentDir, boolean readonly) {

      this.rollDir = rollDir;
      this.currentDir = currentDir;

   // general

      int gridRows    = Text.getInt(this,"n1");
      int gridColumns = Text.getInt(this,"n2");

      int thumbWidth  = Text.getInt(this,"d4");
      int thumbHeight = Text.getInt(this,"d5");

      cache = new ThumbnailCache(thumbWidth,thumbHeight,/* sources = */ 2,/* capacity = */ 10,/* scale = */ 2);

      count = new JTextField(Text.getInt(this,"w5"));

      // lastComponent filled in once we know it

   // system side - tree

      JScrollPane systemScrollTree = null;
      Tree.DriveCombo driveCombo = null;
      if ( ! readonly ) {

         File drive = FileMapper.getRoot(currentDir);

         Tree.FileSystemModel systemModel = Tree.newModel(drive);
         systemTree = Tree.construct(systemModel);

         systemScrollTree = new JScrollPane(systemTree); // both scrollbars as needed

         // this is slightly awkward, doing all this manually,
         // but we can't auto-update because the grid doesn't
         // even exist yet.

         TreePath path = systemModel.traversePath(currentDir);
         systemTree.setSelectionPath(path);
         systemTree.scrollPathToVisible(path); // must be in scroll pane for this to work

         systemTree.addTreeSelectionListener(new TreeSelectionListener() { public void valueChanged(TreeSelectionEvent e) { doSystemSelect(); } });

         systemDisplayedList = ((Tree.FileSystemNode) path.getLastPathComponent()).getTargetFiles();
         // note, traversePath never returns an empty path

         driveCombo = new Tree.DriveCombo(systemTree,drive);
      }

   // system side - grid

      JScrollPane systemScrollGrid = null;
      if ( ! readonly ) {

         systemView = new ListView(systemDisplayedList,null);

         ThumbnailUtil.Adapter systemAdapter = new ThumbnailUtil.Adapter() {
            public File   getFile(Object o) { return (File) o; }
            public String getName(Object o) { return ((File) o).getName(); }
            public int getRotation(Object o) { return 0; }
         };

         systemGrid = new ThumbnailGrid(systemView,systemAdapter,cache.getSource(0),gridRows,gridColumns,thumbWidth,thumbHeight);

         systemScrollGrid = new JScrollPane(systemGrid,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                                       JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
         systemScrollGrid.getViewport().setBackground(systemGrid.getBackground());
      }

   // upload side - tree

      root = Tree.load(album,items);
      model = new Tree.FolderModel(root);

      tree = Tree.construct(model);
      if ( ! readonly ) tree.setEditable(true);
      tree.setSelectionRow(0); // do this <i>before</i> adding listener
      tree.addTreeSelectionListener(new TreeSelectionListener() { public void valueChanged(TreeSelectionEvent e) { doSelect(); } });

      JScrollPane scrollTree = new JScrollPane(tree); // both scrollbars as needed

   // upload side - grid

      view = new ListView(displayedList = Tree.folder(root).items,comparator);
      // root is selected, so show root items

      ThumbnailUtil.Adapter adapter = new ThumbnailUtil.Adapter() {
         public File   getFile(Object o) { return getFileOf((Roll.Item) o); }
         public String getName(Object o) { return ((Roll.Item) o).getOriginalFilename(); }
         public int getRotation(Object o) { return ((Roll.Item) o).rotation; }
      };

      grid = new ThumbnailGrid(view,adapter,cache.getSource(1),gridRows,gridColumns,thumbWidth,thumbHeight);

      JScrollPane scrollGrid = new JScrollPane(grid,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                                    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
      scrollGrid.getViewport().setBackground(grid.getBackground());

   // layout

      int d1 = Text.getInt(this,"d1");
      int d2 = Text.getInt(this,"d2");

      GridBagHelper helper;
      GridBagConstraints constraints;

   // layout - system side

      JPanel panelSystem = null;
      if ( ! readonly ) {

         panelSystem = new JPanel();
         panelSystem.setBorder(BorderFactory.createTitledBorder(Text.get(this,"s9")));
         helper = new GridBagHelper(panelSystem);

         JButton buttonRefresh = new JButton(Text.get(this,"s37"));
         buttonRefresh.addActionListener(driveCombo.listener(/* refresh = */ true));

         helper.addSpan(0,0,3,driveCombo);
         helper.add(0,1,Box.createVerticalStrut(d2));

         constraints = new GridBagConstraints();
         constraints.fill = GridBagConstraints.BOTH;
         constraints.gridwidth = 3;
         helper.add(0,2,systemScrollTree,constraints);

         helper.add(0,3,Box.createVerticalStrut(d2));
         helper.addFill(1,4,buttonRefresh);

         helper.add(3,0,Box.createHorizontalStrut(d1));

         constraints = new GridBagConstraints();
         constraints.fill = GridBagConstraints.VERTICAL;
         constraints.gridheight = 5;
         helper.add(4,0,systemScrollGrid,constraints);

         helper.setRowWeight(2,1);
         helper.setColumnWeight(0,1);
         helper.setColumnWeight(2,1);
      }

   // layout - upload side - tree

      JPanel panelUploadTree = new JPanel();
      helper = new GridBagHelper(panelUploadTree);

      constraints = new GridBagConstraints();
      constraints.fill = GridBagConstraints.BOTH;
      constraints.gridwidth = 3;
      helper.add(0,0,scrollTree,constraints);

      helper.add(0,1,Box.createHorizontalStrut(d1));
      helper.add(2,1,Box.createHorizontalStrut(d1));

      helper.add(1,1,Box.createVerticalStrut(d2));
      helper.addCenter(1,2,new JLabel(Text.get(this,"s33")));
      helper.add(1,3,Box.createVerticalStrut(d2));

      JButton buttonCreate = new JButton(Text.get(this,"s27"));
      buttonCreate.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doCreate(); } });
      helper.addFill(1,4,buttonCreate);

      helper.add(1,5,Box.createVerticalStrut(d2));

      JButton buttonDelete = new JButton(Text.get(this,"s28"));
      buttonDelete.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doDelete(); } });
      helper.addFill(1,6,buttonDelete);

      helper.setRowWeight(0,1);
      helper.setColumnWeight(0,1);
      helper.setColumnWeight(2,1);

   // layout - upload side - grid controls

      JPanel panelUploadGridControls = new JPanel();
      helper = new GridBagHelper(panelUploadGridControls);

      JButton buttonEditName = new JButton(Text.get(this,"s34"));
      buttonEditName.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doEditName(grid.getSelectedObjects()); } });
      helper.add(0,0,buttonEditName);

      int d7 = Text.getInt(this,"d7");
      helper.add(1,0,Box.createHorizontalStrut(d7));

      JButton buttonRotateCW = new JButton(Graphic.getIcon("rotate-cw.gif"));
      buttonRotateCW.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doRotate(grid.getSelectedObjects(),true); } });
      helper.add(2,0,buttonRotateCW);

      helper.add(3,0,Box.createHorizontalStrut(d1));

      JButton buttonRotateCCW = new JButton(Graphic.getIcon("rotate-ccw.gif"));
      buttonRotateCCW.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doRotate(grid.getSelectedObjects(),false); } });
      helper.add(4,0,buttonRotateCCW);

      helper.add(5,0,Box.createHorizontalStrut(d7));

      JButton buttonRemove = new JButton(Text.get(this,"s10"));
      buttonRemove.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doRemove(grid.getSelectedObjects()); } });
      helper.add(6,0,buttonRemove);

   // layout - upload side

      JPanel panelUpload = new JPanel();
      panelUpload.setBorder(BorderFactory.createTitledBorder(Text.get(this,"s8")));
      helper = new GridBagHelper(panelUpload);

      constraints = new GridBagConstraints();
      constraints.fill = GridBagConstraints.BOTH;
      constraints.gridheight = 3; // not 4
      helper.add(0,0,panelUploadTree,constraints);

      helper.add(1,0,Box.createHorizontalStrut(d1));

      helper.add(2,0,scrollGrid,GridBagHelper.fillVertical);
      helper.add(2,1,Box.createVerticalStrut(d2));
      helper.addCenter(2,2,panelUploadGridControls);
      helper.add(2,3,Box.createVerticalStrut(Text.getInt(this,"d6")));

      helper.setRowWeight(0,1);
      helper.setColumnWeight(0,1);

   // layout - overall

      helper = new GridBagHelper(this);

      if ( ! readonly ) {
         helper.add(0,0,panelSystem,GridBagHelper.fillBoth);
         helper.setRowWeight(0,1);
      }

      helper.add(0,1,panelUpload,GridBagHelper.fillBoth);
      helper.setRowWeight(1,1);

      helper.setColumnWeight(0,1);

   // read-only mode

      if (readonly) {
         buttonCreate.setEnabled(false);
         buttonDelete.setEnabled(false);
         buttonEditName.setEnabled(false);
         buttonRotateCW.setEnabled(false);
         buttonRotateCCW.setEnabled(false);
         buttonRemove.setEnabled(false);
      }

   // other setup

      count.setEnabled(false);
      updateCount();

      if ( ! readonly ) configureDragDrop();

      lastComponent = buttonRemove;
   }

// --- GUI utilities ---

   private void stopEditing() {
      tree.stopEditing();
   }

// --- implementation of ItemPanel ---

   /**
    * Add simple fields into the parent layout,
    * with the label in column 0 and the field in column 1.
    *
    * @param y Input value of y.
    * @return Output value of y.
    */
   public int addFields(GridBagHelper helper, int y) {

      helper.add(0,y,new JLabel(Text.get(this,"s7") + ' '));
      helper.add(1,y,count);
      y++;

      return y;
   }

   public Component getLastComponent() { return lastComponent; }

   public void setAlbum(String album) throws ValidationException {
      stopEditing();
      Tree.setAlbum(model,root,album);
   }

   public String getAlbum() throws ValidationException {
      stopEditing();
      return Tree.getAlbum(root);
   }

   public LinkedList getItems(LinkedList items) throws ValidationException {
      stopEditing();
      return Tree.store(root);
   }

   public void stop() { cache.stop(); } // try to stop image processing

   public File getCurrentDir() {

      if (systemTree == null) return currentDir; // if not editable

      Tree.FileSystemNode node = (Tree.FileSystemNode) systemTree.getLastSelectedPathComponent();
      if (node == null) return currentDir; // shouldn't happen

      return node.getFile();
   }

// --- drag-drop ---

   private DragDrop.TreeSelection getTreeSelection() {
      DragDrop.TreeSelection sel = new DragDrop.TreeSelection();
      sel.node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
      return sel;
   }

   private DragDrop.GridSelection getGridSelection() {
      DragDrop.GridSelection sel = new DragDrop.GridSelection();
      sel.list = displayedList;
      sel.o = grid.getSelectedObjects();
      sel.oIndex = grid.getSelectedIndices();
      return sel;
   }

   private DragDrop.SystemTreeSel getSystemTreeSel() {
      DragDrop.SystemTreeSel sel = new DragDrop.SystemTreeSel();
      sel.node = (Tree.FileSystemNode) systemTree.getLastSelectedPathComponent();
      return sel;
   }

   private DragDrop.SystemGridSel getSystemGridSel() {
      DragDrop.SystemGridSel sel = new DragDrop.SystemGridSel();
      sel.list = systemDisplayedList;
      sel.o = systemGrid.getSelectedObjects();
      sel.oIndex = systemGrid.getSelectedIndices();
      return sel;
   }

   private void configureDragDrop() {

      tree.setDragEnabled(true);
      grid.setDragEnabled(true);
      systemTree.setDragEnabled(true);
      systemGrid.setDragEnabled(true);

      DragDropUtil.ClassTransferHandler handler;

      handler = new DragDropUtil.ClassTransferHandler(new DragDropUtil.ExportModule() { public Object prepare() { return getTreeSelection(); } });
      handler.addImport(DragDrop.TreeSelection.class, new DragDropUtil.ImportModule() { public boolean execute(Object data) { return DragDrop.treeToTree((DragDrop.TreeSelection) data,getTreeSelection(),model); } });
      handler.addImport(DragDrop.GridSelection.class, new DragDropUtil.ImportModule() { public boolean execute(Object data) { return DragDrop.gridToTree((DragDrop.GridSelection) data,getTreeSelection(),view); } });
      handler.addImport(DragDrop.SystemTreeSel.class, new DragDropUtil.ImportModule() { public boolean execute(Object data) { return DragDrop.folderTree((DragDrop.SystemTreeSel) data,getTreeSelection(),root,model,tree,ItemPanelPro.this,ItemPanelPro.this); } });
      handler.addImport(DragDrop.SystemGridSel.class, new DragDropUtil.ImportModule() { public boolean execute(Object data) { return DragDrop.fileToTree((DragDrop.SystemGridSel) data,getTreeSelection(),view,ItemPanelPro.this); } });
      tree.setTransferHandler(handler);

      handler = new DragDropUtil.ClassTransferHandler(new DragDropUtil.ExportModule() { public Object prepare() { return getGridSelection(); } });
      handler.addImport(DragDrop.GridSelection.class, new DragDropUtil.ImportModule() { public boolean execute(Object data) { return DragDrop.gridToGrid((DragDrop.GridSelection) data,getGridSelection(),view); } });
      handler.addImport(DragDrop.SystemGridSel.class, new DragDropUtil.ImportModule() { public boolean execute(Object data) { return DragDrop.fileToGrid((DragDrop.SystemGridSel) data,getGridSelection(),view,ItemPanelPro.this); } });
      grid.setTransferHandler(handler);

      handler = new DragDropUtil.ClassTransferHandler(new DragDropUtil.ExportModule() { public Object prepare() { return getSystemTreeSel(); } });
      systemTree.setTransferHandler(handler);

      handler = new DragDropUtil.ClassTransferHandler(new DragDropUtil.ExportModule() { public Object prepare() { return getSystemGridSel(); } });
      systemGrid.setTransferHandler(handler);

      grid.enableDragResponse();
      systemGrid.enableDragResponse();
   }

// --- commands ---

   private void doSystemSelect() {

      Tree.FileSystemNode node = (Tree.FileSystemNode) systemTree.getLastSelectedPathComponent();
      if (node == null) return; // shouldn't happen

      systemView.repoint(systemDisplayedList = node.getTargetFiles());
   }

   private void doSelect() {

      DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
      if (node == null) return; // shouldn't happen

      view.repoint(displayedList = Tree.folder(node).items);
   }

   private void doCreate() {
      stopEditing();
      if (Tree.createSubalbum(model,tree)) {
         // item count doesn't change until you put something in the folder
      }
   }

   private void doDelete() {
      stopEditing();
      if (Tree.deleteSubalbum(model,tree,this,root)) {
         updateCount();
      }
   }

   private void doEditName(Object[] o) {
      stopEditing();

      if (o.length == 0) return;
      Roll.Item item = (Roll.Item) o[0];

      String name = Pop.inputString(this,Text.get(this,"s36",new Object[] { item.getOriginalFilename() }),Text.get(this,"s35"));
      if (name != null) {
         item.setOriginalFilename(name.trim());
         view.editNoSort(item);
      }
   }

   private void doRemove(Object[] o) {
      stopEditing();

      for (int i=0; i<o.length; i++) {
         view.remove(o[i]);
      }

      updateCount();
   }

   private void doRotate(Object[] o, boolean clockwise) {
      stopEditing(); // not necessary here, but pleasant

      for (int i=0; i<o.length; i++) {
         Roll.Item item = (Roll.Item) o[i]; // could be preitem, no matter
         item.rotation = Rotation.rotate(item.rotation,clockwise);
         view.editNoSort(item);
      }
   }

// --- utility functions ---

   private File getFileOf(Roll.Item item) {
      return (item instanceof Roll.PreItem) ? ((Roll.PreItem) item).file
                                            : new File(rollDir,item.filename);
   }

   private void updateCount() {
      Field.put(count,Convert.fromInt(Tree.countItems(root)));
   }

   public void run() { updateCount(); }

}

