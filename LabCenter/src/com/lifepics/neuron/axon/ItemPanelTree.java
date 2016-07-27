/*
 * ItemPanelTree.java
 */

package com.lifepics.neuron.axon;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.gui.DragDropUtil;
import com.lifepics.neuron.gui.Field;
import com.lifepics.neuron.gui.Graphic;
import com.lifepics.neuron.gui.GridBagHelper;
import com.lifepics.neuron.gui.GridColumn;
import com.lifepics.neuron.gui.Rotation;
import com.lifepics.neuron.gui.Thumbnail;
import com.lifepics.neuron.gui.ThumbnailUtil;
import com.lifepics.neuron.gui.ViewHelper;
import com.lifepics.neuron.misc.ExtensionFileFilter;
import com.lifepics.neuron.table.ListView;

import java.io.File;
import java.util.Comparator;
import java.util.LinkedList;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.*;

/**
 * A panel for editing roll item information; only works within {@link RollDialog}.
 * To make clear what parts of the roll are edited here, the roll object
 * is not passed in, instead the individual pieces are handled individually.
 * So, you can see that the items and album name are transferred in and out
 * and that the roll directory is used as a constant.
 */

public class ItemPanelTree extends ItemPanel implements Runnable {

// --- fields ---

   private File rollDir;
   private File currentDir;

   private JTextField count;
   private DefaultMutableTreeNode root;
   private DefaultTreeModel model;
   private JTree tree;
   private ListView view;
   private ViewHelper viewHelper;
   private Thumbnail thumbnail;
   private Runnable thumbnailRefresh;

   private LinkedList displayedList;
   private Component lastComponent;

// --- constants ---

   private static Comparator comparator = RollUtil.orderItemOriginalFilename;

// --- construction ---

   public ItemPanelTree(Dialog owner, String album, LinkedList items, File rollDir, File currentDir, boolean readonly) {

      this.rollDir = rollDir;
      this.currentDir = currentDir;

      count = new JTextField(Text.getInt(this,"w5"));

      int d1 = Text.getInt(this,"d1");
      int d2 = Text.getInt(this,"d2");
      int d3 = Text.getInt(this,"d3");

   // tree

      root = Tree.load(album,items);
      model = new Tree.FolderModel(root);

      tree = Tree.construct(model);
      if ( ! readonly ) tree.setEditable(true);
      tree.setSelectionRow(0); // do this <i>before</i> adding listener
      tree.addTreeSelectionListener(new TreeSelectionListener() { public void valueChanged(TreeSelectionEvent e) { doSelect(); } });

      JScrollPane scrollTree = new JScrollPane(tree); // both scrollbars as needed

      // this doesn't come out exactly like the table header, but close enough
      JLabel header = new JLabel(Text.get(this,"s29"),JLabel.CENTER);
      header.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createRaisedBevelBorder(),
                                                          BorderFactory.createEmptyBorder(0,0,1,0)));
      scrollTree.setColumnHeaderView(header);

      JPanel panelTree = new JPanel();
      GridBagHelper helper = new GridBagHelper(panelTree);

      GridBagConstraints constraints = new GridBagConstraints();
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
      helper.setColumnWeight(0,1); // in case tree is wider than buttons (it's not)
      helper.setColumnWeight(2,1);

   // table

      view = new ListView(displayedList = Tree.folder(root).items,comparator);
      // root is selected, so show root items

      GridColumn colFilename = readonly ? RollUtil.colItemOriginalFilename : RollUtil.colItemEditableFilename;
      GridColumn[] cols = new GridColumn[] { colFilename, RollUtil.colItemStatus };

      viewHelper = new ViewHelper(view,Text.getInt(this,"n1"),cols,null);

   // rotate buttons

      // fairly easy to put directly in layout below,
      // but hard to get the spacing to be balanced

      JPanel panelRotate = new JPanel();

      helper = new GridBagHelper(panelRotate);

      JButton buttonRotateCW = new JButton(Graphic.getIcon("rotate-cw.gif"));
      buttonRotateCW.addActionListener(viewHelper.getAdapter(new ViewHelper.ButtonPress() { public void run(Object[] o) { doRotate(o,true); } }));
      helper.addFill(0,0,adjustMargin(buttonRotateCW));

      helper.add(1,0,Box.createHorizontalStrut(d1));

      JButton buttonRotateCCW = new JButton(Graphic.getIcon("rotate-ccw.gif"));
      buttonRotateCCW.addActionListener(viewHelper.getAdapter(new ViewHelper.ButtonPress() { public void run(Object[] o) { doRotate(o,false); } }));
      helper.addFill(2,0,adjustMargin(buttonRotateCCW));

      helper.setColumnWeight(0,1);
      helper.setColumnWeight(2,1);

   // panel

      setBorder(BorderFactory.createTitledBorder(Text.get(this,"s8")));

      helper = new GridBagHelper(this);

      constraints = new GridBagConstraints();
      constraints.fill = GridBagConstraints.BOTH;
      constraints.gridheight = 9;

      helper.add(0,0,panelTree,constraints);

      helper.add(1,0,Box.createHorizontalStrut(d1));

      helper.add(2,0,viewHelper.getScrollPane(),constraints);

      helper.add(3,0,Box.createHorizontalStrut(d1));

      JButton buttonAdd = new JButton(Text.get(this,"s9"));
      buttonAdd.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doAdd(); } });
      helper.addFill(4,0,buttonAdd);

      helper.add(4,1,Box.createVerticalStrut(d2));

      JButton buttonAddFolder = new JButton(Text.get(this,"s24"));
      buttonAddFolder.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doAddFolder(); } });
      helper.addFill(4,2,buttonAddFolder);

      helper.add(4,3,Box.createVerticalStrut(d2));

      JButton buttonRemove = new JButton(Text.get(this,"s10"));
      buttonRemove.addActionListener(viewHelper.getAdapter(new ViewHelper.ButtonPress() { public void run(Object[] o) { doRemove(o); } }));
      helper.addFill(4,4,buttonRemove);

      helper.add(4,5,Box.createVerticalStrut(d3));

      thumbnail = new Thumbnail(Text.getInt(this,"d4"),Text.getInt(this,"d5"));
      thumbnailRefresh = thumbnail.bindTo(owner,viewHelper.getTable(),view,new ThumbnailUtil.Adapter() {
         public File   getFile(Object o) { return getFileOf((Roll.Item) o); }
         public String getName(Object o) { return ((Roll.Item) o).getOriginalFilename(); }
         public int getRotation(Object o) { return ((Roll.Item) o).rotation; }
      });
      helper.addCenter(4,6,thumbnail);

      helper.add(4,7,Box.createVerticalStrut(d3));

      helper.addFill(4,8,panelRotate);

      helper.add(4,9,Box.createVerticalStrut(Text.getInt(this,"d6")));

      helper.setRowWeight(6,1);
      helper.setColumnWeight(2,1);

   // read-only mode

      if (readonly) {
         buttonCreate.setEnabled(false);
         buttonDelete.setEnabled(false);
         buttonAdd.setEnabled(false);
         buttonAddFolder.setEnabled(false);
         buttonRemove.setEnabled(false);
         buttonRotateCW.setEnabled(false);
         buttonRotateCCW.setEnabled(false);
      }

   // other setup

      count.setEnabled(false);
      updateCount();

      if ( ! readonly ) configureDragDrop();

      lastComponent = buttonRotateCCW;
   }

// --- GUI utilities ---

   /**
    * Reduce the wide left and right margins to match the smaller top and bottom ones.
    */
   private static JButton adjustMargin(JButton button) {
      Insets insets = button.getMargin();
      insets.left   = insets.top;
      insets.right  = insets.top;
      insets.bottom = insets.top; // in practice already equal
      button.setMargin(insets);
      return button; // convenience
   }

   private void stopEditing() {
      tree.stopEditing();
      viewHelper.stopEditing();
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

   public void stop() { thumbnail.clear(); } // try to stop image processing
   public File getCurrentDir() { return currentDir; }

// --- drag-drop ---

   private DragDrop.TreeSelection getTreeSelection() {
      DragDrop.TreeSelection sel = new DragDrop.TreeSelection();
      sel.node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
      return sel;
   }

   private DragDrop.GridSelection getGridSelection() {
      DragDrop.GridSelection sel = new DragDrop.GridSelection();
      sel.list = displayedList;
      sel.o = viewHelper.getSelectedObjects(); // exactly as in button adapter
      sel.oIndex = viewHelper.getTable().getSelectedRows();
      return sel;
   }

   private void configureDragDrop() {
      JTable table = viewHelper.getTable();

      tree .setDragEnabled(true);
      table.setDragEnabled(true);

      DragDropUtil.ClassTransferHandler handler;

      handler = new DragDropUtil.ClassTransferHandler(new DragDropUtil.ExportModule() { public Object prepare() { return getTreeSelection(); } });
      handler.addImport(DragDrop.TreeSelection.class, new DragDropUtil.ImportModule() { public boolean execute(Object data) { return DragDrop.treeToTree((DragDrop.TreeSelection) data,getTreeSelection(),model); } });
      handler.addImport(DragDrop.GridSelection.class, new DragDropUtil.ImportModule() { public boolean execute(Object data) { return DragDrop.gridToTree((DragDrop.GridSelection) data,getTreeSelection(),view); } });
      tree.setTransferHandler(handler);

      handler = new DragDropUtil.ClassTransferHandler(new DragDropUtil.ExportModule() { public Object prepare() { return getGridSelection(); } });
      handler.addImport(DragDrop.GridSelection.class, new DragDropUtil.ImportModule() { public boolean execute(Object data) { return DragDrop.gridToGrid((DragDrop.GridSelection) data,getGridSelection(),view); } });
      table.setTransferHandler(handler);
   }

// --- commands ---

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

   private void doAdd() {
      stopEditing();

      JFileChooser chooser = new JFileChooser(currentDir);
      chooser.setMultiSelectionEnabled(true);

      ExtensionFileFilter.clearFilters(chooser);
      chooser.addChoosableFileFilter(ItemUtil.imageFileFilter);

      Thumbnail thumbnail2 = new Thumbnail(Text.getInt(this,"d8"),Text.getInt(this,"d9"));
      thumbnail2.addTo(chooser,Text.getInt(this,"d10"));

      int result = chooser.showDialog(this,Text.get(this,"s12"));

      thumbnail2.clear(); // try to stop image processing

      if (result != JFileChooser.APPROVE_OPTION) return;

      // only save directory if files were chosen from it
      currentDir = chooser.getCurrentDirectory();

      File[] files = chooser.getSelectedFiles();
      // FileUtil.sortByName, not needed because view.add uses comparator order
      for (int i=0; i<files.length; i++) {
         view.add(ItemUtil.makeItem(files[i]));
      }

      viewHelper.getTable().clearSelection();
      // otherwise, if the new item is just before a selected item,
      // it gets selected too ... which looks weird, especially if
      // there's only one item in the list.
      // another option would be to select the new items, but that
      // would be a lot of work for a feature nobody has requested.

      updateCount();
   }

   private void doAddFolder() {
      stopEditing();

      if ( ! DragDrop.validateFolderAdd(this,root) ) return;

      JFileChooser chooser = new JFileChooser(currentDir);
      chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

      int result = chooser.showDialog(this,Text.get(this,"s25"));
      if (result != JFileChooser.APPROVE_OPTION) return;

      // only save directory if files were chosen from it
      currentDir = chooser.getCurrentDirectory();

      File dir = chooser.getSelectedFile();

      DragDrop.executeFolderAdd(dir,root,model,tree,this);
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
      }

      thumbnailRefresh.run();
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

