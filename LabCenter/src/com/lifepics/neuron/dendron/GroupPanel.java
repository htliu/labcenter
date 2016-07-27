/*
 * GroupPanel.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.ChainedException;
import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Log;
import com.lifepics.neuron.core.Pop;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.gui.Counter;
import com.lifepics.neuron.gui.Field;
import com.lifepics.neuron.gui.GridBagHelper;
import com.lifepics.neuron.gui.GridColumn;
import com.lifepics.neuron.gui.MiniLight;
import com.lifepics.neuron.gui.Print;
import com.lifepics.neuron.gui.Style;
import com.lifepics.neuron.gui.TabControl;
import com.lifepics.neuron.gui.ViewHelper;
import com.lifepics.neuron.meta.Accessor;
import com.lifepics.neuron.meta.CompoundComparator;
import com.lifepics.neuron.meta.FieldComparator;
import com.lifepics.neuron.meta.Selector;
import com.lifepics.neuron.meta.Sortable;
import com.lifepics.neuron.struct.SKU;
import com.lifepics.neuron.table.DerivedTable;
import com.lifepics.neuron.table.ListView;
import com.lifepics.neuron.table.View;
import com.lifepics.neuron.table.ViewListener;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Level;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A panel for viewing and manipulating groups -- title is "Queue Management".
 */

public class GroupPanel extends JPanel implements Sortable, ViewListener {

// --- fields ---

   private Frame owner;
   private TabControl tabControl;

   private DerivedTable groupTable;
   private QueueList queueList;
   private JobManager jobManager;
   private OrderManager orderManager;

   private GroupSelector selector;
   private Comparator comparator;
   private ViewHelper viewHelper;
   private GridColumn[] colsNormal;
   private GridColumn[] colsPlus;
   private boolean showGrandTotal;
   private GridColumn columnQueue;

   private JComboBox comboOrderStatus;
   private JComboBox comboSKU;
   private JComboBox comboGroupStatus;
   private QueueCombo comboQueue;

   private HashSet skuSet;

   private JLabel labelTotalGroups;
   private JLabel labelTotalQuantity;

   private ViewListener readyListener;
   private View   viewReady;
   private JLabel labelReadyQuantity;
   private JButton buttonTally;

   private FlowPanel saveFlowPanel;

// --- combo boxes ---

   private static final int STATUS_ANY = -100; // must be disjoint from both order and group statuses
   private static final int STATUS_ALT = -101;

   private static Object[] orderStatusNames = new Object[] { Text.get(GroupPanel.class,"s5"),
                                                             Text.get(GroupPanel.class,"s25"),
                                                             OrderEnum.fromOrderStatus(Order.STATUS_ORDER_INVOICED),
                                                             OrderEnum.fromOrderStatus(Order.STATUS_ORDER_PRINTING),
                                                             OrderEnum.fromOrderStatus(Order.STATUS_ORDER_PRINTED),
                                                             OrderEnum.fromOrderStatus(Order.STATUS_ORDER_COMPLETED) };

   private static int[] orderStatusValues = new int[] { STATUS_ALT,
                                                        STATUS_ANY,
                                                        Order.STATUS_ORDER_INVOICED,
                                                        Order.STATUS_ORDER_PRINTING,
                                                        Order.STATUS_ORDER_PRINTED,
                                                        Order.STATUS_ORDER_COMPLETED };

   private static Object[] groupStatusNames = new Object[] { Text.get(GroupPanel.class,"s6"),
                                                             GroupEnum.fromGroupStatus(Group.STATUS_GROUP_RECEIVED),
                                                             GroupEnum.fromGroupStatus(Group.STATUS_GROUP_PRINTING),
                                                             GroupEnum.fromGroupStatus(Group.STATUS_GROUP_PRINTED)  };

   private static int[] groupStatusValues = new int[] { STATUS_ANY,
                                                        Group.STATUS_GROUP_RECEIVED,
                                                        Group.STATUS_GROUP_PRINTING,
                                                        Group.STATUS_GROUP_PRINTED };

   private static String stringAnySKU = Text.get(GroupPanel.class,"s4");
   private static Object objectAnySKU = new Object() {
      public String toString() { return stringAnySKU; } // see below for the point of this
   };

// --- construction ---

   public GroupPanel(Frame owner, TabControl tabControl, DerivedTable groupTable, JobManager jobManager, OrderManager orderManager, boolean showGrandTotal, LinkedList formatSubsystems) {
      this.owner = owner;
      this.tabControl = tabControl;

      this.groupTable = groupTable;
      this.queueList = jobManager.getQueueList();
      this.jobManager = jobManager;
      this.orderManager = orderManager;

   // build table

      selector = new GroupSelector(queueList); // shows all

      comparator = new CompoundComparator(new FieldComparator(GroupUtil.order,OrderUtil.orderOrderID),GroupUtil.orderSKU);

      View view = groupTable.select(selector,comparator,true,true);

      colsNormal = new GridColumn[] {
            GroupUtil.colOrder(OrderUtil.colInvoiceDate),
            GroupUtil.colOrder(OrderUtil.colOrderID),
            GroupUtil.colOrder(OrderUtil.colStatusHold,Text.get(this,"s2")),
            GroupUtil.colOrder(OrderUtil.colName),
            GroupUtil.colSKU,
            GroupUtil.colTotalQuantity,
            GroupUtil.colStatus,
            columnQueue = GroupUtil.colQueue(queueList)
         };

      colsPlus = new GridColumn[colsNormal.length+1];
      int i = 0;
      int j = 0;
      while (i < colsPlus.length) {
         GridColumn col = colsNormal[j++];
         colsPlus[i++] = col;
         if (col == GroupUtil.colTotalQuantity) colsPlus[i++] = GroupUtil.colOrder(OrderUtil.colGrandTotal);
      }
      // BTW, the two arrays must hold the exact same GridColumn objects,
      // because otherwise the columnQueue behavior won't work.
      // mostly that's no problem, but colOrder and colQueue are dynamic.

      this.showGrandTotal = showGrandTotal;
      GridColumn[] cols = showGrandTotal ? colsPlus : colsNormal;

      ViewHelper.DoubleClick doubleClick = openOrder();
      viewHelper = new ViewHelper(view,Text.getInt(this,"n1"),cols,doubleClick);
      viewHelper.makeSortable(this);
      viewHelper.colorize(new DueColorizer(GroupUtil.order));

   // constants

      int d1 = Text.getInt(this,"d1");
      int d5 = Text.getInt(this,"d5");

   // build query controls

      JPanel controls = new JPanel();
      controls.setBorder(BorderFactory.createEmptyBorder(0,0,d1,0));
      GridBagHelper helper = new GridBagHelper(controls);

      helper.add(0,1,Box.createVerticalStrut(d5));
      helper.add(1,0,new JLabel());
      helper.add(3,0,new JLabel());
      helper.add(5,0,new JLabel());
      helper.add(7,0,new JLabel());

      helper.addCenter(2,0,Style.style.adjustControl(new JLabel(Text.get(this,"s21"))));
      helper.addCenter(4,0,Style.style.adjustControl(new JLabel(Text.get(this,"s22"))));
      helper.addCenter(6,0,Style.style.adjustControl(new JLabel(Text.get(this,"s23"))));
      helper.addCenter(8,0,Style.style.adjustControl(new JLabel(Text.get(this,"s24"))));

      helper.add(0,2,Style.style.adjustControl(new JLabel(Text.get(this,"s3"))));

      comboOrderStatus = new JComboBox(orderStatusNames);
      comboOrderStatus.setSelectedIndex(1); // value at index must be STATUS_ANY, otherwise initial selector won't match combo value
      comboOrderStatus.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doQueryOrderStatus(); } });
      helper.addCenter(2,2,comboOrderStatus);

      comboSKU = new JComboBox(new Object[] { objectAnySKU });
      comboSKU.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doQuerySKU(); } });
      helper.addCenter(4,2,comboSKU);

      skuSet = new HashSet();

      comboGroupStatus = new JComboBox(groupStatusNames);
      comboGroupStatus.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doQueryGroupStatus(); } });
      helper.addCenter(6,2,comboGroupStatus);

      comboQueue = new QueueCombo(queueList,Text.get(this,"s7"),null);
      comboQueue.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doQueryQueue(); } });
      helper.addCenter(8,2,comboQueue);

      helper.setColumnWeight(1,1);
      helper.setColumnWeight(3,1);
      helper.setColumnWeight(5,1);
      helper.setColumnWeight(7,1);

   // build totals area

      JPanel totals = new JPanel();
      totals.setBorder(Style.style.createTotalsBorder());
      totals.setLayout(new BoxLayout(totals,BoxLayout.X_AXIS));

      labelTotalGroups = new JLabel();
      labelTotalQuantity = new JLabel();

      totals.add(Style.style.adjustPlain(new JLabel(Text.get(this,"s11") + ' ')));
      totals.add(Style.style.adjustPlain(labelTotalGroups));
      totals.add(Box.createHorizontalGlue());

      totals.add(Style.style.adjustPlain(new JLabel(Text.get(this,"s12") + ' ')));
      totals.add(Style.style.adjustPlain(labelTotalQuantity));
      totals.add(Box.createHorizontalGlue());

      viewHelper.viewAddListener(this);
      reportChange(); // otherwise no initial update

   // build buttons

      JButton button;

      JPanel buttons = new JPanel();
      buttons.setBorder(Style.style.createButtonBorder());

      helper = new GridBagHelper(buttons);

      helper.add(0,1,Box.createVerticalStrut(d5));
      helper.add(2,3,Box.createVerticalStrut(d5)); // scooted over to make room for fixer
      helper.add(0,5,Box.createVerticalStrut(d5));
      helper.add(2,0,new JLabel());
      helper.add(4,0,new JLabel());
      helper.add(6,0,new JLabel());

      View viewHold = orderManager.getTable().select(OrderSelectors.orderOpenHold,OrderUtil.orderOrderID,true,true);
      View viewGo   = orderManager.getTable().select(OrderSelectors.orderGo,      OrderUtil.orderOrderID,true,true);
      helper.addSpanFill(0,0,2,new FlowPanel(viewHold,"s33",viewGo,TabControl.TARGET_DOWNLOAD,/* formatSubsystems = */ null,/* subsystemTarget = */ 0));
      // light is different from summary tab light, stays on even when not downloading

      viewReady = orderManager.getTable().select(OrderManager.selectorInvoice,OrderUtil.orderOrderID,true,true);
      labelReadyQuantity = new JLabel();

      JPanel fixer = new JPanel();
      GridBagHelper helper2 = new GridBagHelper(fixer);
      //
      helper2.add(0,0,new JLabel());
      helper2.add(0,1,Style.style.adjustPlain(new JLabel(Text.get(this,"s13") + ' ')));
      helper2.add(1,1,Style.style.adjustPlain(new Counter(viewReady,/* colorAlert = */ null,/* large = */ false)),GridBagHelper.alignRight);
      helper2.add(0,2,Box.createVerticalStrut(Text.getInt(this,"d9")));
      helper2.add(0,3,Style.style.adjustPlain(new JLabel(Text.get(this,"s26") + ' ')));
      helper2.add(1,3,Style.style.adjustPlain(labelReadyQuantity),GridBagHelper.alignRight);
      helper2.add(0,4,new JLabel());
      //
      helper2.setRowWeight(0,1);
      helper2.setRowWeight(4,1);
      helper2.setColumnWeight(1,1);
      //
      GridBagConstraints constraints = new GridBagConstraints();
      constraints.fill = GridBagConstraints.BOTH;
      constraints.gridwidth  = 2;
      constraints.gridheight = 3;
      helper.add(0,2,fixer,constraints);

      readyListener = new ReadyListener(); // can't be local variable, listener reference is weak
      viewReady.addListener(readyListener);
      readyListener.reportChange(); // otherwise no initial update

      JPanel multi = new JPanel();
      multi.setLayout(new BoxLayout(multi,BoxLayout.X_AXIS));
      //
      buttonTally = new JButton(Text.get(this,"s27"));
      buttonTally.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doTally(); } });
      multi.add(Style.style.adjustButton(buttonTally));
      //
      multi.add(Box.createHorizontalStrut(Text.getInt(this,"d3")));
      //
      button = new JButton(Text.get(this,"s14"));
      button.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doReleaseInvoice(); } });
      multi.add(Style.style.adjustButton(button));
      //
      helper.addSpanCenter(0,6,2,multi);

      button = new JButton(Text.get(this,"s8"));
      button.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doSelectAll(); } });
      helper.addFill(3,2,Style.style.adjustButton(button));

      button = new JButton(Text.get(this,"s30"));
      button.addActionListener(viewHelper.getAdapter(reprintInvoices()));
      helper.addFill(3,4,Style.style.adjustButton(button));

      button = new JButton(Text.get(this,"s35"));
      button.addActionListener(viewHelper.getAdapter(reprintLabels()));
      helper.addFill(3,6,Style.style.adjustButton(button));

      View viewJobHold = jobManager.getJobTable().select(JobSelectors.jobHold,JobUtil.orderJobID,true,true);
      View viewJobOpen = jobManager.getJobTable().select(JobSelectors.jobOpen,JobUtil.orderJobID,true,true);
      helper.addFill(5,0,saveFlowPanel = new FlowPanel(viewJobHold,"s19",viewJobOpen,TabControl.TARGET_JOB,formatSubsystems,TabControl.TARGET_OTHER));

      button = new JButton(Text.get(this,"s9"));
      button.addActionListener(viewHelper.getAdapter(printGroups()));
      helper.addFill(5,2,Style.style.adjustButton(button));

      button = new JButton(Text.get(this,"s17"));
      button.addActionListener(viewHelper.getAdapter(markGroups()));
      helper.addFill(5,4,Style.style.adjustButton(button));

      helper.addSpanCenter(7,0,3,Style.style.adjustHeader(new JLabel(Text.get(this,"s20"))));

      button = new JButton(Text.get(this,"s15"));
      button.addActionListener(viewHelper.getAdapter(completeOrders()));
      helper.addFill(7,2,Style.style.adjustButton(button));

      button = new JButton(Text.get(this,"s16"));
      button.addActionListener(viewHelper.getAdapter(abortOrders()));
      helper.addFill(7,4,Style.style.adjustButton(button));

      button = new JButton(Text.get(this,"s31"));
      button.addActionListener(viewHelper.getAdapter(cancelOrders()));
      helper.addFill(7,6,Style.style.adjustButton(button));

      helper.add(8,2,Box.createHorizontalStrut(Text.getInt(this,"d8")));

      button = new JButton(Text.get(this,"s32"));
      button.addActionListener(viewHelper.getAdapter(releaseOrders()));
      helper.addFill(9,2,Style.style.adjustButton(button));

      helper.setColumnWeight(2,1);
      helper.setColumnWeight(4,1);
      helper.setColumnWeight(6,1);

   // combine lower panels

      JPanel combined = new JPanel();
      combined.setLayout(new BoxLayout(combined,BoxLayout.Y_AXIS));

      combined.add(totals);
      combined.add(Box.createVerticalStrut(d1));
      combined.add(buttons);

   // make unified panel

      JPanel panelGroup = new JPanel();
      panelGroup.setLayout(new BorderLayout());

      JScrollPane scroll = viewHelper.getScrollPane();
      Style.style.adjustScroll_Grid(scroll);

      panelGroup.add(controls,BorderLayout.NORTH);
      panelGroup.add(scroll,  BorderLayout.CENTER);
      panelGroup.add(combined,BorderLayout.SOUTH);

   // finish up

      // use grid layout just to be consistent with other tabs
      // int dp = Style.style.getPanelGap(); // same as d1 in modern case

      helper = new GridBagHelper(this);

      helper.add(1,1,Style.style.adjustPanel_Group(panelGroup),GridBagHelper.fillBoth);

      helper.add(0,0,Box.createRigidArea(new Dimension(d1,d1)));
      helper.add(2,2,Box.createRigidArea(new Dimension(d1,d1)));

      helper.setRowWeight(1,1);
      helper.setColumnWeight(1,1);
   }

   public void rebuildFormatSubsystems(LinkedList formatSubsystems) {
      saveFlowPanel.cluster.rebuild(formatSubsystems);
      // FlowPanel.cluster is nullable in general,
      // but we know it's not null on saveFlowPanel
   }

// --- helpers ---

   // FlowPanel can't be static because it uses tabControl

   private class FlowPanel extends JPanel implements ViewListener {

      private View viewWorking;
      private int target;
      private int subsystemTarget;

      private JLabel labelTitle;
      private JLabel labelWorking;

      public LightCluster cluster;

      public FlowPanel(View viewError, String keyTitle, View viewWorking, int target, LinkedList formatSubsystems, int subsystemTarget) {
         this.viewWorking = viewWorking;
         this.target = target;
         this.subsystemTarget = subsystemTarget;

         addMouseListener(new MouseAdapter() { public void mouseClicked(MouseEvent e) { handleClick(e); } });

         labelTitle = new JLabel(Text.get(GroupPanel.class,keyTitle));
         labelWorking = new JLabel(Text.get(GroupPanel.class,"s34"));

         // I was using BoxLayout here, but when there was extra width to allocate,
         // it gave some to the LightCluster even though there were two glue areas.

         GridBagHelper helper = new GridBagHelper(this);
         int x = 0;

         if (formatSubsystems != null) {
            helper.add(x++,0,cluster = new LightCluster(formatSubsystems,Style.LIGHT_SUBSYSTEM_VIEW));
            helper.add(x++,0,Box.createHorizontalStrut(Text.getInt(GroupPanel.class,"d10")));
            // on the summary panel I was careful to let modern style and classic style
            // have different layouts, but here one layout for the lights is fine
         }

         helper.add(x++,0,new MiniLight(viewError,null,Style.LIGHT_VIEW_SMALL).getComponent());
         helper.add(x++,0,Box.createHorizontalStrut(Text.getInt(GroupPanel.class,"d11"))); // keep light separate from text

         helper.setColumnWeight(x,1); // be glue
         helper.add(x++,0,new JLabel());

         helper.add(x++,0,Style.style.adjustHeader(labelTitle));

         helper.setColumnWeight(x,1); // be glue
         helper.add(x++,0,new JLabel());

         helper.add(x++,0,Style.style.adjustHeader(labelWorking));

         viewWorking.addListener(this);
         reportChange(); // otherwise no initial update
      }

      public void reportInsert(int j, Object o) { reportChange(); }
      public void reportUpdate(int i, int j, Object o) {} // doesn't change count
      public void reportDelete(int i) { reportChange(); }
      public void reportChange() {
         boolean working = (viewWorking.size() > 0);
         labelTitle  .setVisible( ! working );
         labelWorking.setVisible(   working );
      }

      public void handleClick(MouseEvent e) {

         boolean b = cluster.getBounds().contains(e.getPoint());
         // point and bounds are both in FlowPanel coord sys

         tabControl.setSelectedTab(b ? subsystemTarget : target);

         // new plan:
         // don't try and guess where we should send the user,
         // instead have the result depend on what they click.

         // old plan:
         // discussion of click behavior.  the real logic I wanted is,
         //
         // 1. if subsystem aborted, show subsystem
         // 2. if any jobs are errored, show jobs
         // 3. if subsystem has any other problem, show subsystem
         // 4. else show jobs
         //
         // it wouldn't be too hard to write that, just add isAborted
         // and check viewError.size, but it's not worth it because
         // case 3 there doesn't really happen with the job formatter.
         // it's not wired up for network notifications, doesn't stop,
         // and doesn't post errors much.  so, this way is reasonable.
         //
         // another point is, should the view light turn off when the
         // subsystem light is off?  in other cases we do that, but
         // here it's not so clear-cut because there are other subsystems
         // that operate on jobs, like auto-complete.  also, the light
         // really shouldn't be off, so don't worry about it.
      }
   }

// --- methods ---

   public void sort(Comparator comparator) {
      this.comparator = comparator;
      viewHelper.viewSort(comparator);

      // this indirection serves two purposes:
      // first, it lets us keep track of the current comparator,
      // so that we can use it when we requery;
      // second, it allows the view object to be changed (on requery).
   }

   public void reinit(QueueList queueList, boolean showGrandTotal) {

      if (showGrandTotal != this.showGrandTotal) {

         this.showGrandTotal = showGrandTotal;
         GridColumn[] cols = showGrandTotal ? colsPlus : colsNormal;

         viewHelper.setColumns(cols);
      }

      if (queueList.equals(this.queueList)) return;
      this.queueList = queueList;

      selector.reinit(queueList);
      GroupUtil.reinit(columnQueue,queueList);

      comboQueue.reinit(queueList);

      // the first two reinit calls require us to requery and redraw the grid.
      // but, the combo reinit restores the current value, which makes the
      // combo go off, which makes doQueryQueue run ... and that causes a requery.

      // in the delete case, the combo reinit clears the combo and causes requery.
      // the selector queueID continues to point to the deleted queue until then.
   }

// --- implementation of ViewListener ---

   public void reportInsert(int j, Object o) { reportChange(); }
   public void reportUpdate(int i, int j, Object o) {} // no change, quantity and SKU not mutable
   public void reportDelete(int i) { reportChange(); }

   public void reportChange() {

      int totalGroups = viewHelper.viewSize();
      int totalQuantity = 0;

      for (int i=0; i<totalGroups; i++) {
         Group group = (Group) viewHelper.viewGet(i);

         totalQuantity += group.totalQuantity;

         if (skuSet.add(group.sku)) insertComboSKU(group.sku);
      }

      labelTotalGroups.setText(Convert.fromInt(totalGroups));
      labelTotalQuantity.setText(Convert.fromInt(totalQuantity));
   }

// --- another implementation of ViewListener ---

   private class ReadyListener implements ViewListener {

      public void reportInsert(int j, Object o) { reportChange(); }
      public void reportUpdate(int i, int j, Object o) {} // no change, quantity not mutable
      public void reportDelete(int i) { reportChange(); }

      public void reportChange() {

         int readyQuantity = 0;

         for (int i=0; i<viewReady.size(); i++) {
            Order order = (Order) viewReady.get(i);

            Iterator oi = order.items.iterator();
            while (oi.hasNext()) {
               Order.Item item = (Order.Item) oi.next();
               readyQuantity += item.quantity;
            }
         }

         labelReadyQuantity.setText(Convert.fromInt(readyQuantity));
      }
   }

// --- ready quantity by SKU ---

   private static class Entry implements Comparable {
      public SKU sku;
      public int quantity;

      public Entry(SKU sku) { this.sku = sku; this.quantity = 0; }

      public int compareTo(Object o) { return SKUComparator.compareForDisplay(sku,((Entry) o).sku); }
   }
   // we could use Map.Entry to get the SKU, but we'd need to hash twice,
   // ... once to get the current value, and once to store the new value.

   private LinkedList tally() {
      // no need to suspend -- we're in the UI thread, and notifications are transferred

   // accumulate results

      HashMap map = new HashMap();

      for (int i=0; i<viewReady.size(); i++) {
         Order order = (Order) viewReady.get(i);

         Iterator oi = order.items.iterator();
         while (oi.hasNext()) {
            Order.Item item = (Order.Item) oi.next();

            Entry entry = (Entry) map.get(item.sku);
            if (entry == null) {
               entry = new Entry(item.sku);
               map.put(item.sku,entry);
            }

            entry.quantity += item.quantity;
         }
      }

   // put in SKU-sorted list

      LinkedList list = new LinkedList(map.values());

      Collections.sort(list);
      return list;
   }

// --- entry grid columns (EntryUtil) ---

   private static Accessor entrySKU = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return ((Entry) o).sku.toString(); }
   };

   private static Accessor entryQuantity = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return Convert.fromInt(((Entry) o).quantity); }
   };

   private static GridColumn col(int n, Accessor accessor, Comparator comparator) {
      String suffix = Convert.fromInt(n);
      String name = Text.get(GroupPanel.class,"n" + suffix);
      int width;
      try {
         width = Convert.toInt(Text.get(GroupPanel.class,"w" + suffix));
      } catch (ValidationException e) {
         width = 1;
         // nothing we can do in a static context
      }
      return new GridColumn(name,width,accessor,comparator);
   }

   private static class Disposer implements ActionListener {
      private JDialog dialog;
      public Disposer(JDialog dialog) { this.dialog = dialog; }
      public void actionPerformed(ActionEvent e) { dialog.dispose(); }
   }

// --- SKU combo ---

   // like the queue combo, this is a nonstandard one.
   // in particular, it maps to objects, not integers,
   // so we can't make use of Field.get(JComboBox).

   // we wrap the "any SKU" item in an object so that we can distinguish it
   // from a SKU string that happens to have the same value.
   // (combo boxes don't actually track the selected index, just the value.)
   // this isn't necessary now that we use OldSKU, but let's keep it anyway

   // we never remove SKUs.  partly that's because it would be a hassle,
   // but there are good reasons, too ... we don't want to remove a SKU
   // just because there temporarily aren't any of it in the system,
   // that would be confusing; and we certainly don't want to remove all
   // the SKUs when the user filters to a small subset (by SKU, even).

   private void insertComboSKU(SKU sku) {
      int i = 1; // the "any SKU" item is position zero
      for ( ; i<comboSKU.getItemCount(); i++) {
         if (SKUComparator.compareForDisplay(sku,comboSKU.getItemAt(i)) < 0) break;
      }
      comboSKU.insertItemAt(sku,i);
   }

   /**
    * @return A SKU value, or null meaning any SKU.
    */
   private SKU getComboSKU() {
      Object o = comboSKU.getSelectedItem();
      return (o == objectAnySKU) ? null : (SKU) o;
   }

// --- bindings ---

   public ViewHelper.DoubleClick openOrder() {
      return new ViewHelper.DoubleClick() { public void run(Object o) { orderManager.doOpen(owner,((Group) o).order); } };
   }

   public ViewHelper.ButtonPress reprintInvoices() {
      return new ViewHelper.ButtonPress() { public void run(Object[] o) { orderManager.doReprint(owner,getUniqueOrders(o),Print.MODE_INVOICE); } };
   }

   public ViewHelper.ButtonPress reprintLabels() {
      return new ViewHelper.ButtonPress() { public void run(Object[] o) { orderManager.doReprint(owner,getUniqueOrders(o),Print.MODE_LABEL); } };
   }

   public ViewHelper.ButtonPress printGroups() {
      return new ViewHelper.ButtonPress() { public void run(Object[] o) { doPrint(o); } };
   }

   public ViewHelper.ButtonPress markGroups() {
      return new ViewHelper.ButtonPress() { public void run(Object[] o) { doMark(o); } };
   }

   public ViewHelper.ButtonPress completeOrders() {
      return new ViewHelper.ButtonPress() { public void run(Object[] o) { orderManager.doComplete(owner,getUniqueOrders(o)); } };
   }

   public ViewHelper.ButtonPress abortOrders() {
      return new ViewHelper.ButtonPress() { public void run(Object[] o) { orderManager.doAbort(owner,getUniqueOrders(o)); } };
   }

   public ViewHelper.ButtonPress cancelOrders() {
      return new ViewHelper.ButtonPress() { public void run(Object[] o) { orderManager.doCancel(owner,getUniqueOrders(o)); } };
   }

   // holdOrders not needed

   public ViewHelper.ButtonPress releaseOrders() {
      return new ViewHelper.ButtonPress() { public void run(Object[] o) { orderManager.doRelease(owner,getUniqueOrders(o)); } };
   }

// --- user commands ---

   public static void pleaseSelect(Window owner, boolean multi) {
      Pop.info(owner,Text.get(GroupPanel.class,multi ? "e3a" : "e3b"),Text.get(GroupPanel.class,"s36"));
   }

   private void requery(Selector selector) {

      viewHelper.viewRemoveListener(this);

      View view = groupTable.select(selector,comparator,true,true);
      viewHelper.setView(view);

      viewHelper.viewAddListener(this);
      reportChange(); // otherwise no initial update
   }

   private void doQueryOrderStatus() {
      int i = Field.get(comboOrderStatus,orderStatusValues);
      selector.orderStatus = (i == STATUS_ANY) ? null : new Integer(i);
      requery(selector);
   }

   private void doQuerySKU() {
      selector.sku = getComboSKU();
      requery(selector);
   }

   private void doQueryGroupStatus() {
      int i = Field.get(comboGroupStatus,groupStatusValues);
      selector.groupStatus = (i == STATUS_ANY) ? null : new Integer(i);
      requery(selector);
   }

   private void doQueryQueue() {
      selector.queueID = comboQueue.get();
      requery(selector);
   }

   private void doTally() {

   // table

      LinkedList list = tally();
      ListView view = new ListView(list,null);

      GridColumn[] cols = new GridColumn[] {
            col(2,entrySKU,null),
            col(3,entryQuantity,null)
         };

      ViewHelper viewHelper = new ViewHelper(view,list.size(),cols,null);

   // panel

      int d6 = Text.getInt(this,"d6");

      JPanel panel = new JPanel();
      panel.setBorder(BorderFactory.createEmptyBorder(d6,d6,d6,d6));
      GridBagHelper helper = new GridBagHelper(panel);

      helper.add(0,0,viewHelper.getScrollPane());
      helper.add(0,1,Box.createVerticalStrut(Text.getInt(this,"d7")));

      JButton button = new JButton(Text.get(this,"s29"));
      helper.addCenter(0,2,button);

   // dialog

      JDialog dialog = new JDialog(owner,Text.get(this,"s28"),/* modal = */ true);

      button.addActionListener(new Disposer(dialog));

      dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
      dialog.setResizable(false);

      dialog.getContentPane().add(panel);

      dialog.pack();
      dialog.setLocationRelativeTo(buttonTally);
      dialog.setVisible(true);
   }

   private void doReleaseInvoice() {
      orderManager.doReleaseInvoice(owner);
   }

   private void doSelectAll() {
      viewHelper.tableSelectAll();
   }

   /**
    * @return A map from order IDs (fullID) to linked lists of SKUs.
    */
   private static HashMap mapOrderSKU(Object[] o) {

      HashMap map = new HashMap();

      for (int j=0; j<o.length; j++) {
         Group group = (Group) o[j];
         Object key = group.order.getFullID();

         LinkedList skus = (LinkedList) map.get(key);
         if (skus == null) {
            skus = new LinkedList();
            map.put(key,skus);
         }

         skus.add(group.sku);
      }

      return map;
   }

   private void doPrint(Object[] o) {
      if (o.length == 0) { pleaseSelect(owner,true); return; }

   // compile list of SKUs per order

      HashMap map = mapOrderSKU(o);

   // create jobs per order

      // this is an interesting situation for error handling.
      // what if the user carefully selects a bunch of stuff,
      // and hits print, and we fail partway through?
      // how does the user recover and finish printing?
      //
      // first, note that errors here will be rare.
      // I checked the JobManager code ...
      // here are the ones we ought to think about.
      //
      // (a) the order is already locked,
      //     say by a completion update for a previous job.
      //     this would occur for isolated orders,
      //     before any jobs were created for those orders.
      //
      // (b) the job insert or order update failed,
      //     say because the disk was full.
      //     this would occur for all orders after a certain point.
      //     the first such order might have some jobs created.
      //
      // also note that if you're doing initial printing,
      // not reprinting, it's easy to tell which jobs were created,
      // because the group statuses will have changed.
      // (the status might change for reprints, too, depending,
      // but then it also might change back pretty quickly.)
      //
      // but, for reprints you'd mostly be working with individual orders.
      //
      // so, with all that in mind, when an error occurs,
      // is it better to continue with other orders, or to abort?
      //
      // well, in case (b) we always want to abort.
      // in case (a), it depends ... if we're doing initial printing,
      // it would be a tiny convenience to the user to continue,
      // but it's not difficult to filter on READY status and hit print.
      // if we're reprinting one order, it doesn't matter;
      // and if we're reprinting in bulk (rare), it would be a nuisance
      // to continue, since the user couldn't tell what was going on.
      //
      // so, it looks to me like there's no big difference,
      // but that aborting has less potential for disaster.

      int printed = 0;

      Iterator i = map.entrySet().iterator();
      while (i.hasNext()) {
         Map.Entry entry = (Map.Entry) i.next();

         String fullID = (String) entry.getKey();
         LinkedList skus = (LinkedList) entry.getValue();

         Log.log(Level.INFO,this,"i1",new Object[] { fullID, concat(skus) });

         try {
            jobManager.create(fullID,null,skus,null,null,/* adjustIfPartial = */ false);
         } catch (Exception e) {

            int unprinted = map.size() - (printed+1);
            String message = Text.get(this,"e1",new Object[] { fullID, new Integer(printed), Convert.fromInt(printed), new Integer(unprinted), Convert.fromInt(unprinted) });

            Pop.error(owner,ChainedException.format(message,e),Text.get(this,"s10"));
            //
            // using this variant of format usually means that
            // you ought to be re-throwing a wrapped exception,
            // and catching it somewhere else.

            break;
         }

         printed++;
      }
   }

   private static String concat(LinkedList skus) {
      StringBuffer buffer = new StringBuffer();

      boolean first = true;

      Iterator i = skus.iterator();
      while (i.hasNext()) {
         SKU sku = (SKU) i.next();

         if (first) {
            first = false;
         } else {
            buffer.append(", ");
         }
         buffer.append(sku.toString());
      }

      return buffer.toString();
   }

   private static Object[] getUniqueOrders(Object[] o) {

   // compile set of orders

      // we need a map because we want a set of order objects,
      // but we can't just throw them in a HashSet
      // because they don't implement Object.equals or hashCode.
      // so, use the order IDs as keys, the objects as values.

      // this is kind of stupid, because the returned objects
      // are just going to be turned into keys in the end,
      // but it's kind of stupid the other way, too, because
      // I can't find a nice way to turn an Object array
      // whose contents happen to be strings into a String[].
      // casting to String[] produces a ClassCastException.
      //
      // n.b., I could have used toArray(Object[]) instead.

      HashMap map = new HashMap();

      for (int j=0; j<o.length; j++) {
         Group group = (Group) o[j];
         Object key = group.order.getFullID();

         map.put(key,group.order);
         //
         // "if (get(k) == null) put(k,v)" is more what I had in mind,
         // but that takes two lookups, while this way takes only one.
      }

   // get map values as array

      return map.values().toArray();
   }

   private void doMark(Object[] o) {
      if (o.length == 0) { pleaseSelect(owner,true); return; }

   // compile list of SKUs per order

      HashMap map = mapOrderSKU(o);

   // mark items per order

      Iterator i = map.entrySet().iterator();
      while (i.hasNext()) {
         Map.Entry entry = (Map.Entry) i.next();

         String fullID = (String) entry.getKey();
         LinkedList skus = (LinkedList) entry.getValue();

         try {
            jobManager.mark(fullID,skus,Order.STATUS_ITEM_PRINTED);
         } catch (Exception e) {
            String message = Text.get(this,"e2",new Object[] { fullID });
            Pop.error(owner,ChainedException.format(message,e),Text.get(this,"s18"));
         }
      }
   }

}

