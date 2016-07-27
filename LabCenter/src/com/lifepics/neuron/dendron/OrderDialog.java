/*
 * OrderDialog.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Log;
import com.lifepics.neuron.core.Nullable;
import com.lifepics.neuron.core.Pop;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.gui.Blob;
import com.lifepics.neuron.gui.ButtonHelper;
import com.lifepics.neuron.gui.EditDialog;
import com.lifepics.neuron.gui.EditSKUInterface;
import com.lifepics.neuron.gui.Field;
import com.lifepics.neuron.gui.GridBagHelper;
import com.lifepics.neuron.gui.GridColumn;
import com.lifepics.neuron.gui.InitialFocus;
import com.lifepics.neuron.gui.Thumbnail;
import com.lifepics.neuron.gui.ThumbnailUtil;
import com.lifepics.neuron.gui.ViewHelper;
import com.lifepics.neuron.meta.CompoundComparator;
import com.lifepics.neuron.table.ListView;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.logging.Level;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A dialog for viewing order information.
 */

public class OrderDialog extends EditDialog {

// --- fields ---

   private Order order;
   private OrderManager orderManager;
   private JobManager jobManager;

   private FormatArrays fa;

   private JTextField orderID;
   private JTextField orderDir;
   private JComboBox  format;
   private JTextField name;
   private JTextField email;
   private JTextField phone;
   private JTextField count;

   private Thumbnail thumbnail;

   private JComboBox  carrier;
   private JTextField trackingNumber;
   private JTextField trackingNumberConfirm;

   private boolean trackingResult;

// --- static config ---

   private static EditSKUInterface editSKUInterface;
   public static void setEditSKUInterface(EditSKUInterface editSKUInterface) { OrderDialog.editSKUInterface = editSKUInterface; }

   private static CarrierList carrierList;
   public static void setCarrierList(CarrierList carrierList) { OrderDialog.carrierList = carrierList; }

// --- combo boxes ---

   // these are now in order alphabetically by display string, except for FUJI3

   private static Object[] allNames = new Object[] { Text.get(OrderDialog.class,"f8"),
                                                     Text.get(OrderDialog.class,"f12"),
                                                     Text.get(OrderDialog.class,"f20"),
                                                     Text.get(OrderDialog.class,"f21"),
                                                     Text.get(OrderDialog.class,"f13"),
                                                     Text.get(OrderDialog.class,"f24"),
                                                     Text.get(OrderDialog.class,"f14"),
                                                     Text.get(OrderDialog.class,"f19"),
                                                     Text.get(OrderDialog.class,"f22"),
                                                     Text.get(OrderDialog.class,"f10"),
                                                     Text.get(OrderDialog.class,"f4"),
                                                     Text.get(OrderDialog.class,"f5"),
                                                     Text.get(OrderDialog.class,"f16"),
                                                     Text.get(OrderDialog.class,"f17"),
                                                     Text.get(OrderDialog.class,"f6"),
                                                     Text.get(OrderDialog.class,"f11"),
                                                     Text.get(OrderDialog.class,"f7"),
                                                     Text.get(OrderDialog.class,"f3"),
                                                     Text.get(OrderDialog.class,"f9"),
                                                     Text.get(OrderDialog.class,"f0"),
                                                     Text.get(OrderDialog.class,"f1"),
                                                     Text.get(OrderDialog.class,"f2"),
                                                     Text.get(OrderDialog.class,"f23"),
                                                     Text.get(OrderDialog.class,"f18"),
                                                     Text.get(OrderDialog.class,"f15") };

   private static int[] allValues = new int[] { Order.FORMAT_AGFA,
                                                Order.FORMAT_BEAUFORT,
                                                Order.FORMAT_BURN,
                                                Order.FORMAT_HOT_FOLDER,
                                                Order.FORMAT_DKS3,
                                                Order.FORMAT_RAW_JPEG,
                                                Order.FORMAT_DIRECT_PDF,
                                                Order.FORMAT_DIRECT_JPEG,
                                                Order.FORMAT_DNP,   // DNP DIRECT
                                                Order.FORMAT_PIXEL, // DNP
                                                Order.FORMAT_FUJI,
                                                Order.FORMAT_FUJI_NEW,
                                                Order.FORMAT_FUJI3,
                                                Order.FORMAT_HP,
                                                Order.FORMAT_DLS,
                                                Order.FORMAT_DP2,
                                                Order.FORMAT_KODAK,
                                                Order.FORMAT_KONICA,
                                                Order.FORMAT_LUCIDIOM,
                                                Order.FORMAT_FLAT,
                                                Order.FORMAT_TREE,
                                                Order.FORMAT_NORITSU,
                                                Order.FORMAT_PURUS,
                                                Order.FORMAT_XEROX,
                                                Order.FORMAT_ZBE };

   public static class FormatArrays {
      public Object[] formatNames;
      public int[]    formatValues;
   }

   public static void addPublicFormats(HashSet formats) {

      // the original plan was that most formats would be locked,
      // the dealers would pay extra per lab integration,
      // and then the server would tell us which ones to unlock.
      // but, that's not how things turned out.

      for (int format=Order.FORMAT_MIN; format<=Order.FORMAT_MAX; format++) {

         // enumerate the private formats, since that's a much shorter list
         if (    format == Order.FORMAT_LUCIDIOM
              || format == Order.FORMAT_BURN
              || format == Order.FORMAT_PURUS ) continue;

         formats.add(new Integer(format));
      }
   }

   public static FormatArrays getFormatArrays(HashSet formats) {

   // count up front, since variable-size containers can't hold int ...
   // but unless I messed up somewhere, count should equal formats.size()

      int count = 0;

      for (int i=0; i<allValues.length; i++) {
         if (formats.contains(new Integer(allValues[i]))) {
            count++;
         }
      }

   // (re)generate the arrays

      FormatArrays fa = new FormatArrays();

      fa.formatNames  = new Object[count];
      fa.formatValues = new int   [count];

      int j = 0;

      for (int i=0; i<allValues.length; i++) {
         if (formats.contains(new Integer(allValues[i]))) {
            fa.formatNames [j] = allNames [i];
            fa.formatValues[j] = allValues[i];
            j++;
         }
      }

      return fa;
   }

// --- view parameters ---

   private static CompoundComparator comparator = new CompoundComparator(OrderUtil.orderItemFilename,OrderUtil.orderItemSKU);
      // filename and SKU are unique key, see OrderParser
      // also the users will probably expect SKU ordering, not quantity

// --- construction ---

   private static String getTitleText(boolean tracking) {
      return Text.get(OrderDialog.class,tracking ? "s29" : "s1");
   }

   /**
    * A pseudo-constructor to deal with the annoying fact that
    * the superclass constructor distinguishes between Frame and Dialog.
    */
   public static OrderDialog create(Window owner, Order order, OrderManager orderManager, JobManager jobManager, boolean readonly, boolean tracking) {
      return (owner instanceof Frame) ? new OrderDialog((Frame ) owner,order,orderManager,jobManager,readonly,tracking)
                                      : new OrderDialog((Dialog) owner,order,orderManager,jobManager,readonly,tracking);
   }

   public OrderDialog(Frame owner, Order order, OrderManager orderManager, JobManager jobManager, boolean readonly, boolean tracking) {
      super(owner,getTitleText(tracking));

      this.order = order;
      this.orderManager = orderManager;
      this.jobManager = jobManager;
      constructFields(readonly,tracking); // nonstandard, call construct from inside constructFields

      // for some reason, the text areas don't figure out the right size
      // the first time around, you have to call pack again for it to work.
      // pack was already called once inside construct, so here's another:

      pack();
   }

   public OrderDialog(Dialog owner, Order order, OrderManager orderManager, JobManager jobManager, boolean readonly, boolean tracking) {
      super(owner,getTitleText(tracking));

      this.order = order;
      this.orderManager = orderManager;
      this.jobManager = jobManager;
      constructFields(readonly,tracking); // nonstandard, call construct from inside constructFields

      pack(); // see note above
   }

// --- override ---

   public boolean run() {
      boolean result = super.run();
      thumbnail.clear(); // try to stop image processing
      return result;
   }

// --- methods ---

   private void constructFields(boolean readonly, boolean tracking) {

   // format arrays

      // the format is a relic, and we only allow changing it to FORMAT_FLAT
      // (see validation in OrderManager.open).  so, instead of creating
      // a dependency on the config and the set of unlocked formats, let's just
      // show a two-value combo.

      HashSet formats = new HashSet();
      formats.add(new Integer(order.format     ));
      formats.add(new Integer(Order.FORMAT_FLAT));

      fa = getFormatArrays(formats);

   // fields

      orderID = new JTextField(Text.getInt(this,(order.wholesale == null) ? "w1a" : "w1b"));
      orderDir = new JTextField(Text.getInt(this,"w2"));
      format = new JComboBox(fa.formatNames);

      name = new JTextField(Text.getInt(this,"w4"));
      email = new JTextField(Text.getInt(this,"w5"));
      phone = new JTextField(Text.getInt(this,"w6"));

      count = new JTextField(Text.getInt(this,"w7"));

   // enables

      orderID.setEnabled(false);
      orderDir.setEnabled(false);
      format.setEnabled( ! readonly );

      name.setEnabled(false);
      email.setEnabled(false);
      phone.setEnabled(false);

      count.setEnabled(false);

   // table

      // the comparator argument to ListView doesn't sort, only maintains,
      // so if we want the items sorted, here, we have to do it ourselves.
      //
      LinkedList list = new LinkedList(order.items);
      Collections.sort(list,comparator);

      ListView view = new ListView(list,comparator);

      GridColumn[] cols = new GridColumn[] {
            OrderUtil.colItemFilename,
            OrderUtil.colItemQuantity,
            OrderUtil.colItemSKU,
            OrderUtil.colItemStatus
         };

      ViewHelper viewHelper = new ViewHelper(view,Text.getInt(this,"n1"),cols,null);

   // panel

      JPanel panel = new JPanel();
      panel.setBorder(BorderFactory.createTitledBorder(Text.get(this,"s9")));

      GridBagHelper helper = new GridBagHelper(panel);

      GridBagConstraints constraints = new GridBagConstraints();
      constraints.fill = GridBagConstraints.BOTH;
      constraints.gridwidth = 2;
      helper.add(0,0,viewHelper.getScrollPane(),constraints);

      int d2 = Text.getInt(this,"d2");
      helper.add(0,1,Box.createVerticalStrut(d2));

      helper.add(2,0,Box.createHorizontalStrut(Text.getInt(this,"d3")));

      thumbnail = new Thumbnail(Text.getInt(this,"d4"),Text.getInt(this,"d5"));
      thumbnail.bindTo(this,viewHelper.getTable(),view,new ThumbnailUtil.Adapter() {
         public File getFile(Object o) {
            Order.Item item = (Order.Item) o;
            String filename = item.isMultiImage() ? (String) item.filenames.getFirst() : item.filename;
            Order.OrderFile file = order.findFileByFilename(filename);
            return (file.status < Order.STATUS_ITEM_RECEIVED) ? null : order.getPath(file);
         }
         public String getName(Object o) { return ((Order.Item) o).filename; }
         public int getRotation(Object o) { return 0; }
      });
      helper.addCenter(3,0,thumbnail);

      helper.add(4,0,Box.createHorizontalStrut(Text.getInt(this,"d6")));

      JButton button = new JButton(Text.get(this,"s15"));
      button.addActionListener(viewHelper.getAdapter(new ViewHelper.ButtonPress() { public void run(Object[] o) { if (o.length > 0) doEditSKU(o[0]); } }));
      //
      button.setEnabled( ! tracking );
      //
      helper.addCenter(0,2,button);

      helper.add(1,2,new JLabel(Text.get(this,"s12")),GridBagHelper.alignRight);

      button = new JButton(Text.get(this,"s13"));
      button.addActionListener(viewHelper.getAdapter(new ViewHelper.ButtonPress() { public void run(Object[] o) { doPrint(o); } }));
      //
      button.setEnabled(readonly && ! tracking && order.status >= Order.STATUS_ORDER_INVOICED); // no printing from first column!
      // else order is locked now, but might not be saved,
      // which would be problematic for sending to queues.
      // keep the button though so they known it's disabled and not just missing.
      //
      helper.addCenter(3,2,button);

      helper.add(0,3,Box.createVerticalStrut(d2));

      helper.setRowWeight(0,1);
      helper.setColumnWeight(0,1);

   // overall

      JPanel fields = new JPanel();
      helper = new GridBagHelper(fields);

      int y = 0;

      helper.add(0,y,new JLabel(Text.get(this,"s2") + ' '));
      helper.add(1,y,orderID);
      y++;

      helper.add(0,y,new JLabel(Text.get(this,"s3") + ' '));
      helper.add(1,y,orderDir);
      y++;

      // if format is flat, as it will be from now on,
      // don't display the format combo.  if not,
      // only allowed change is to flat, but that's not enforced here.
      //
      if (order.format != Order.FORMAT_FLAT) {
         helper.add(0,y,new JLabel(Text.get(this,"s10") + ' '));
         helper.add(1,y,format);
         y++;
      }

      helper.add(0,y,new JLabel(Text.get(this,"s5") + ' '));
      helper.add(1,y,name);
      y++;

      helper.add(0,y,new JLabel(Text.get(this,"s6") + ' '));
      helper.add(1,y,email);
      y++;

      helper.add(0,y,new JLabel(Text.get(this,"s7") + ' '));
      helper.add(1,y,phone);
      y++;

      helper.add(0,y,new JLabel(Text.get(this,"s8") + ' '));
      helper.add(1,y,count);
      y++;

      helper.addSpanFill(0,y,2,panel);
      y++;

      int d1 = Text.getInt(this,"d1");
      int w3 = Text.getInt(this,"w3");

      if (order.specialInstructions != null) {
         JComponent blob = Blob.makeBlob(order.specialInstructions,
                                         Text.get(this,"s11"),d1,w3);
         helper.addSpanFill(0,y,2,blob);
         y++;
      }

      if (order.lastError != null) {
         JComponent blob = Blob.makeBlob(order.lastError,
                                         Text.get(this,"s4"),d1,w3);
         helper.addSpanFill(0,y,2,blob);
         y++;
      }

   // variations for tracking numbers

      // if this looks confusing, that's because it is.  there are six factors.
      //
      // * is tracking enabled?   if not, definitely no changes
      // * is the order shipped?  if not, definitely no changes
      //
      // * what's the order status?  (not ready yet / ready / done) ; if not ready yet, no changes (*)
      // * are we opening normally or in tracking mode?
      // * do we have a carrier subset so we can show things?
      // * are we in readonly mode?  (in practice, yes)
      //
      // if you plow through all sixteen combinations of the last four factors
      // and think about what should happen, you get the slices below.
      // I don't think you can reason them out, you just have to plow through.
      // note that the eight tracking mode cases actually reduce to two
      // because there we only open in readonly mode and only for orders that are ready.
      //
      // (*) the reason for not showing the tracking info before ready is,
      // at that time the automatic processes are still likely to want to
      // lock the order, and we'd get occasionaly "unable to lock" errors
      // and confused users.  and, there's really no need to enter tracking info so early.

      JPanel trackingPanel = null;
      ButtonInfo bi = null;

      if (    carrierList.enableTracking
           && order.status >= Order.STATUS_ORDER_INVOICED // check first, faster than shipToHome calculation
           && order.isAnyShipToHome() ) {

         // once those three conditions are met, we'll definitely show something

         if ( ! carrierList.hasCarrierSubset() ) { // slice out easiest case

            trackingPanel = makeTrackingPanel(TRACKING_ERROR,/* green = */ tracking);
            // custom button in tracking mode since it needs to say Do Not Complete, not OK

            if (tracking) {

               JButton b1 = new JButton(Text.get(this,"s23"));
               b1.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
                  doCancel();
               } });

               bi = new ButtonInfo();
               bi.buttons = ButtonHelper.makeButtons(b1);
               bi.buttonDefault = b1;
            }

         } else if ( order.status >= Order.STATUS_ORDER_COMPLETED || ! readonly ) {

            trackingPanel = makeTrackingPanel(TRACKING_VIEW,/* green = */ tracking); // tracking is false
            // standard buttons

         } else {

            trackingPanel = makeTrackingPanel(TRACKING_EDIT,/* green = */ tracking);
            // custom buttons that depend on tracking mode

            if (tracking) {

               // three buttons so it's totally clear what you're doing

               JButton b1 = new JButton(Text.get(this,"s24"));
               b1.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
                  doTrackingOK(/* require = */ true, /* requiredState = */ true);
               } });

               JButton b2 = new JButton(Text.get(this,"s25"));
               b2.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
                  doTrackingOK(/* require = */ true, /* requiredState = */ false);
               } });

               JButton b3 = new JButton(Text.get(this,"s26"));
               b3.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
                  doCancel();
               } });

               bi = new ButtonInfo();
               bi.buttons = ButtonHelper.makeButtons(b1,b2,b3);
               // no default button here

               setFocusTraversalPolicy(new InitialFocus(trackingNumber));

            } else {

               // two buttons, OK and Cancel, but behavior of OK button is different than in EditDialog

               JButton b1 = new JButton(Text.get(this,"s27"));
               b1.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
                  doTrackingOK(/* require = */ false, /* requiredState = */ false);
               } });

               JButton b2 = new JButton(Text.get(this,"s28"));
               b2.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
                  doCancel();
               } });

               bi = new ButtonInfo();
               bi.buttons = ButtonHelper.makeButtons(b1,b2);
               bi.buttonDefault = b1;
            }
         }
      }

   // finish up

      if (trackingPanel != null) {
         helper.addSpanFill(0,y,2,trackingPanel);
         y++;
      }

      if (bi != null) construct(fields,bi,readonly);
      else            construct(fields,   readonly);
   }

   // the mode also determines which GUI controls are created
   // let c = carrier, t = tracking number, u = confirm field
   //
   private static final int TRACKING_EDIT  = 0; // ctu
   private static final int TRACKING_VIEW  = 1; // ct-
   private static final int TRACKING_ERROR = 2; // ---
   //
   protected JPanel makeTrackingPanel(int mode, boolean green) {

      JPanel panel = new JPanel();
      panel.setBorder(BorderFactory.createTitledBorder(Text.get(this,"s16")));

      if (green) panel.setBackground(Color.green);

      GridBagHelper helper = new GridBagHelper(panel);

      if (mode == TRACKING_ERROR) {

         helper.add(0,0,new JLabel(Text.get(this,"s20")));
         helper.add(0,1,new JLabel(Text.get(this,"s21")));

         // no column weight is OK, looks good centered

      } else {

      // gather carrier info

         LinkedList subset = carrierList.getCarrierSubset(); // not empty because not TRACKING_ERROR
         Carrier selected = null;

         if (order.carrier != null) {

            // there are three possible cases:
            // 1. it's in the subset
            // 2. it's in the list but not in the subset
            // 3. it's not in the list

            selected = carrierList.findCarrierByPrimaryName(order.carrier);
            // gets us the right display name in case 2

            if (selected == null) { // case 3
               selected = new Carrier();
               selected.primaryName = order.carrier;
               selected.displayName = order.carrier; // best we can do
               // show flag is academic at this point
            }

            if ( ! subset.contains(selected) ) subset.addFirst(selected);

         } else if (carrierList.defaultCarrier != null) {

            selected = carrierList.findCarrierByPrimaryName(carrierList.defaultCarrier);
            // always in subset, by validation
         }
         // else leave first item selected

      // do the layout

         carrier = new JComboBox(subset.toArray());
         if (selected != null) carrier.setSelectedItem(selected);
         if (mode == TRACKING_VIEW) carrier.setEnabled(false);
         //
         helper.add(0,0,new JLabel(Text.get(this,"s17") + ' '));
         helper.add(1,0,carrier);

         int w8 = Text.getInt(this,"w8");

         trackingNumber = new JTextField(w8);
         Field.putNullable(trackingNumber,order.trackingNumber);
         if (mode == TRACKING_VIEW) trackingNumber.setEnabled(false);
         //
         helper.add(0,1,new JLabel(Text.get(this,"s18") + ' '));
         helper.add(1,1,trackingNumber);

         if (mode != TRACKING_VIEW) {

            trackingNumberConfirm = new JTextField(w8);
            Field.putNullable(trackingNumberConfirm,order.trackingNumber);
            // no need for setEnabled call here
            //
            helper.add(0,2,new JLabel(Text.get(this,"s19") + ' '));
            helper.add(1,2,trackingNumberConfirm);
         }

         helper.setColumnWeight(1,1);
      }

      return panel;
   }

   protected void put() {

      Field.put(orderID,OrderStubDialog.describeID(order));
      Field.put(orderDir,Convert.fromFile(order.orderDir));
      Field.put(format,fa.formatValues,order.format);

      Field.put(name,order.getFullName());
      Field.put(email,order.email);
      Field.put(phone,order.phone);

      Field.put(count,Convert.fromInt(order.items.size()));
   }

   protected void getAndValidate() throws ValidationException {
      order.format = Field.get(format,fa.formatValues);
      order.validate();
      // maybe one day allowed formats could depend on something else
   }

// --- tracking buttons ---

   // I guess this is as good a place as any to explain all the weird things
   // that are going on here.  so ...
   //
   // the order dialog has a mode where the order object is locked,
   // but it's a relic, used only when the format isn't flat.
   // normally the dialog is read-only, and the Send to Queues button
   // relies on this because creating the jobs has to lock and commit
   // the order while the dialog is still open.  so, we handle the
   // tracking number fields the same way -- wait until we get to the
   // end and then hope we can lock and commit.
   //
   // so, since the EditDialog is in read-only mode all the time,
   // run always returns false.  but, for the tracking number we
   // do need to get back an OK-cancel flag, so there needs to be
   // some other channel for that.  that's what trackingResult is.
   //
   // I did think about making Send to Queues operate on a locked object,
   // but there's a real conceptual issue with doing an immediate commit
   // to an object while there's an active edit session.

   /**
    * Only bind this to buttons in TRACKING_EDIT mode!
    */
   private void doTrackingOK(boolean require, boolean requiredState) {
      try {

         Carrier c = (Carrier) carrier.getSelectedItem();
         String tn = Field.getNullable(trackingNumber);
         String tc = Field.getNullable(trackingNumberConfirm);

         if ( ! Nullable.equals(tn,tc) ) throw new ValidationException(Text.get(this,"e1"));

         if (require) {
            boolean actualState = (tn != null);
            if (actualState != requiredState) throw new ValidationException(Text.get(this,requiredState ? "e2" : "e3"));
         }

         String cs = (tn != null) ? c.primaryName : null;
         // combo is always set to something, just ignore it when tracking is null

         if (    ! Nullable.equals(cs,order.carrier)
              || ! Nullable.equals(tn,order.trackingNumber) ) {

            orderManager.updateTracking(order.getFullID(),cs,tn);
         }
         // else nothing changed, no need to update

      } catch (Exception e) {
         Pop.error(this,e,Text.get(this,"s22"));
         return;
      }

      trackingResult = true;
      doCancel();
   }

   public boolean getTrackingResult() { return trackingResult; }

// --- commands ---

   private void doEditSKU(Object o) {
      Order.Item item = (Order.Item) o;
      editSKUInterface.editSKU(item.sku);
   }

   private void doPrint(Object[] o) {
      if (o.length == 0) return;

      OverrideDialog d = new OverrideDialog(this);
      if ( ! d.run() ) return;

      Log.log(Level.INFO,this,"i1",new Object[] { order.getFullID(), Convert.fromInt(o.length) });

      try {
         // even if locked, we can't use createWithLock -- see comment in constructFields
         jobManager.create(order.getFullID(),null,null,Arrays.asList(o),d.overrideQuantity,/* adjustIfPartial = */ false);
      } catch (Exception e) {
         Pop.error(this,e,Text.get(this,"s14"));
      }
   }

}

