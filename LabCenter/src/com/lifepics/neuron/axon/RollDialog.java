/*
 * RollDialog.java
 */

package com.lifepics.neuron.axon;

import com.lifepics.neuron.core.ChainedException;
import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Pop;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.gui.Field;
import com.lifepics.neuron.gui.GridBagHelper;
import com.lifepics.neuron.gui.GridColumn;
import com.lifepics.neuron.gui.ViewHelper;
import com.lifepics.neuron.meta.Accessor;
import com.lifepics.neuron.meta.NoCaseComparator;
import com.lifepics.neuron.net.Email;
import com.lifepics.neuron.table.ListView;

import java.io.File;
import java.util.LinkedList;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A dialog for editing roll information.
 */

public class RollDialog extends AbstractRollDialog {

// --- fields ---

   private ItemPanel itemPanel;

   private Roll roll;
   private LinkedList addressList;

   private boolean isWholesaleDialog;

   private JTextField rollID;
   private JTextField email;
   private JTextField emailConfirm;

   // fields for pro mode
   private JTextField eventCode;
   private JComboBox priceList;
   private TransformPanel transformPanel;

   // fields for wholesale mode
   private JComboBox dealer;

   // null unless email panel constructed
   private ListView view;
   private Component emailPanelFirst;
   private Component emailPanelLast;

// --- accessors ---

   public File getCurrentDir() { return itemPanel.getCurrentDir(); }
   public LinkedList getAddressList() { return addressList; }

// --- view parameters ---

   // null in the sense of null modem, it does nothing
   private static Accessor nullAccessor = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return o; }
   };

   private static GridColumn colAddress = new GridColumn(Text.get   (RollDialog.class,"s19"),
                                                         Text.getInt(RollDialog.class,"w7"),
                                                         nullAccessor,null);

// --- construction ---

   /**
    * @param roll An object that will be modified by the dialog.
    */
   public RollDialog(Frame owner, Roll roll, int mode, File currentDir, LinkedList addressList) {
      super(owner,getTitleText(mode));

      boolean readonly = (mode == MODE_VIEW);

      itemPanel = new ItemPanelTree(this,roll.album,roll.items,roll.rollDir,currentDir,readonly);

      this.roll = roll;
      this.addressList = addressList;

      construct(constructFields(mode),readonly);

      // for some reason, the text areas don't figure out the right size
      // the first time around, you have to call pack again for it to work.
      // pack was already called once inside construct, so here's another:

      pack();
   }

   /**
    * @param roll An object that will be modified by the dialog.
    */
   public RollDialog(Dialog owner, Roll roll, int mode, File currentDir, LinkedList addressList) {
      super(owner,getTitleText(mode));

      boolean readonly = (mode == MODE_VIEW);

      itemPanel = new ItemPanelTree(this,roll.album,roll.items,roll.rollDir,currentDir,readonly);

      this.roll = roll;
      this.addressList = addressList;

      construct(constructFields(mode),readonly);

      pack(); // see note above
   }

// --- override ---

   public boolean run() {
      boolean result = super.run();
      itemPanel.stop();
      return result;
   }

// --- methods ---

   private JPanel constructFields(int mode) {

      isWholesaleDialog = Wholesale.isWholesale();
      // retrieve once and cache, so that in the rare event
      // that we do an auto-config while the dialog is up,
      // we don't try to e.g. access unconstructed controls.
      // (pro mode is fine, it can't change without restart)

   // fields

      rollID = new JTextField(Text.getInt(this,"w1"));
      email = new JTextField(Text.getInt(this,"w3"));
      if (mode != MODE_VIEW) {
         emailConfirm = new JTextField(Text.getInt(this,"w4"));
      }

      if (ProMode.isPro() || ProMode.isMerged()) {
         eventCode = new JTextField(Text.getInt(this,"w8"));
      }
      if (ProMode.isProOld()) {
         priceList = makePriceListCombo();
         transformPanel = new TransformPanel();
      }

      if (isWholesaleDialog) {
         dealer = makeDealerCombo();
      }

   // enables

      rollID.setEnabled(false);

      if (mode == MODE_VIEW) {

         email.setEnabled(false);
         if (emailConfirm != null) { // actually it's always null
            emailConfirm.setEnabled(false);
         }
         // itemPanel read-only behavior handled at construction

         if (ProMode.isPro() || ProMode.isMerged()) {
            eventCode.setEnabled(false);
         }
         if (ProMode.isProOld()) {
            priceList.setEnabled(false);
            transformPanel.setEnabled(false);
         }

         if (isWholesaleDialog) {
            dealer.setEnabled(false);
         }
      }

   // overall

      JPanel fields = new JPanel();
      GridBagHelper helper = new GridBagHelper(fields);

      int layoutWidth = 2;
      int y = 0;

      int yTop = y; // email panel starts with roll ID so that buttons line up with email address

      helper.add(0,y,new JLabel(Text.get(this,"s3") + ' '));
      helper.add(1,y,rollID);
      y++;

      // the email/emailConfirm logic is already convoluted enough,
      // so don't change it for pro mode, just don't show the fields.
      //
      // why do we even check pro mode in here?  because for a while
      // we were using the basic roll dialog for both normal and pro.
      // the code is getting stale, we should remove it some time.
      //
      if ( ! ProMode.isProOld() ) {

         helper.add(0,y,new JLabel(Text.get(this,"s5") + ' '));
         helper.add(1,y,email);

         if (mode != MODE_VIEW && isClaimEnabled()) { // same conditions as address book, plus claimEnabled

            JButton buttonSetClaim = new JButton(Text.get(this,"s28"));
            buttonSetClaim.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doSetClaim(); } });

            GridBagConstraints constraints = new GridBagConstraints();
            constraints.anchor = GridBagConstraints.NORTH;
            constraints.gridheight = 2; // emailConfirm will be there
            helper.add(2,y,buttonSetClaim,constraints);
            if (layoutWidth < 3) layoutWidth = 3;
         }

         y++;

         if (emailConfirm != null) {
            helper.add(0,y,new JLabel(Text.get(this,"s6") + ' '));
            helper.add(1,y,emailConfirm);
            y++;
         }
      }

      if (ProMode.isPro() || ProMode.isMerged()) {

         helper.add(0,y,new JLabel(Text.get(this,"s24") + ' '));
         helper.add(1,y,eventCode);
         y++;
      }
      if (ProMode.isProOld()) {

         helper.add(0,y,new JLabel(Text.get(this,"s25") + ' '));
         helper.add(1,y,priceList);
         y++;

         helper.add(0,y,new JLabel(transformPanel.getLabel() + ' '));
         helper.add(1,y,transformPanel);
         y++;
      }

      if (isWholesaleDialog) {

         helper.add(0,y,new JLabel(Text.get(this,"s26") + ' '));
         helper.add(1,y,dealer);
         y++;
      }

      y = itemPanel.addFields(helper,y);

      helper.addSpanFill(0,y,layoutWidth,itemPanel);
      y++;

      if (mode == MODE_VIEW || ProMode.isProOld()) { // no address book in pro mode
         // no change to layoutWidth
      } else {

         GridBagConstraints constraints = new GridBagConstraints();
         constraints.fill = GridBagConstraints.VERTICAL;
         constraints.gridheight = y - yTop;
         helper.add(layoutWidth,yTop,constructEmailPanel(),constraints);
         layoutWidth++;

         // splice the address book down to the end of the focus cycle
         setFocusTraversalPolicy(new SplicePolicy(emailPanelFirst,emailPanelLast,itemPanel.getLastComponent()));
      }

      if (roll.lastError != null) {
         helper.addSpanFill(0,y,layoutWidth,makeErrorBlob(roll.lastError));
         y++;
      }

      return fields;
   }

   private JPanel constructEmailPanel() {

      // if this isn't called, the view variable will be null,
      // but then that's only used in the email panel buttons.

   // email table

      view = new ListView(addressList,new NoCaseComparator());

      GridColumn[] cols = new GridColumn[] { colAddress };

      ViewHelper viewHelper = new ViewHelper(view,Text.getInt(this,"n2"),cols,
         new ViewHelper.DoubleClick() { public void run(Object o) { doEmailSelect(o); } });

   // email panel

      JPanel panelEmail = new JPanel();
      panelEmail.setBorder(BorderFactory.createTitledBorder(Text.get(this,"s16")));

      GridBagHelper helper = new GridBagHelper(panelEmail);

      JButton buttonEmailAdd = new JButton(Text.get(this,"s17"));
      buttonEmailAdd.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doEmailAdd(); } });
      helper.addCenter(1,0,buttonEmailAdd);

      helper.add(2,0,Box.createHorizontalStrut(Text.getInt(this,"d13")));

      JButton buttonEmailRemove = new JButton(Text.get(this,"s18"));
      buttonEmailRemove.addActionListener(viewHelper.getAdapter(new ViewHelper.ButtonPress() { public void run(Object[] o) { doEmailRemove(o); } }));
      helper.addFill(3,0,buttonEmailRemove);

      helper.add(0,1,Box.createVerticalStrut(Text.getInt(this,"d11")));

      GridBagConstraints constraints = new GridBagConstraints();
      constraints.fill = GridBagConstraints.BOTH;
      constraints.gridwidth = 5;
      helper.add(0,2,viewHelper.getScrollPane(),constraints);

      helper.add(0,3,Box.createVerticalStrut(Text.getInt(this,"d12")));

      helper.setRowWeight(2,1);
      helper.setColumnWeight(0,1);
      helper.setColumnWeight(4,1);

      emailPanelFirst = buttonEmailAdd;
      emailPanelLast = viewHelper.getTable();

      return panelEmail;
   }

   private void putEmail(String address) {
      Field.put(email,address);
      if (emailConfirm != null) {
         Field.put(emailConfirm,address);
      }
   }

   protected void put() {

      Field.put(rollID,formatRollID(roll.rollID));
      putEmail(roll.email);
      // album and items transferred to itemPanel at construction

      if (ProMode.isPro() || ProMode.isMerged()) {
         Field.putNullable(eventCode,roll.eventCode);
      }
      if (ProMode.isProOld()) {
         putPriceListCombo(priceList,roll.priceList);
         transformPanel.putType(roll.transformType);
         transformPanel.putConfig(roll.transformConfig);
      }

      if (isWholesaleDialog) {
         putDealerCombo(dealer,roll.dealer);
      }
   }

   protected void getAndValidate() throws ValidationException {

      // roll ID not editable
      roll.email = Field.get(email);

      if (emailConfirm != null) { // actually it's always non-null
         String confirm = Field.get(emailConfirm);
         if ( ! confirm.equals(roll.email) ) {
            throw new ValidationException(Text.get(this,"e1"));
         }
      }

      roll.album = itemPanel.getAlbum();
      roll.items = itemPanel.getItems(roll.items);

      if (ProMode.isPro() || ProMode.isMerged()) {
         roll.eventCode = Field.getNullable(eventCode);
      }
      if (ProMode.isProOld()) {
         roll.priceList = getPriceListCombo(priceList);
         roll.transformType = transformPanel.getType();
         roll.transformConfig = transformPanel.getConfig();
      }

      // when we're uploading through web services, which now is always,
      // and when certain conditions apply, we need the album to be set.
      // it's possible to get around this validation by hacking, but we'll
      // double-check in WebServicesMethod.validate.
      //
      if (roll.eventCode != null && roll.album == null) {
         throw new ValidationException(Text.get(this,"e10"));
      }
      if (roll.hasSubalbums() && roll.album == null) {
         throw new ValidationException(Text.get(this,"e11"));
      }

      if (isWholesaleDialog) {
         roll.dealer = getDealerCombo(dealer);
      }

      roll.validate();
   }

   protected boolean weakValidate() {

      // order uploads can't be edited in the GUI, so this code never gets invoked
      // for them, so it doesn't matter how we handle blank email addresses.
      // in practice, if we did allow editing, we'd probably still want to ask about
      // blank addresses, but the message would be slightly different.

      // ditto for local-image uploads

      try {
         Email.validate(roll.email);
      } catch (ValidationException e) {

         String s = Text.get(this,"e4",new Object[] { ChainedException.format(e) });
         boolean confirmed = Pop.confirm(this,s,Text.get(this,"s15"));
         if ( ! confirmed ) return false;
      }

      if (isWholesaleDialog && roll.dealer == null) {

         boolean confirmed = Pop.confirm(this,Text.get(this,"e9"),Text.get(this,"s27"));
         if ( ! confirmed ) return false;
      }

      if ( ! weakValidateSizeZero(this,roll.items.size()) ) return false;
      if ( ! weakValidateNames(this,roll) ) return false;

      return true;
   }

// --- focus splice ---

   private static class SplicePolicy extends LayoutFocusTraversalPolicy {

      private Component first;
      private Component last;
      private Component dest;

      private boolean inited;
      private Component beforeFirst;
      private Component afterLast;
      private Component afterDest;

      /**
       * Construct a splice policy that relocates the range [first,last] to after dest.
       */
      public SplicePolicy(Component first, Component last, Component dest) {
         this.first = first;
         this.last = last;
         this.dest = dest;
         inited = false;
         // rest filled in at init
      }

      private void init(Container root) {
         beforeFirst = super.getComponentBefore(root,first);
         afterLast = super.getComponentAfter(root,last);
         afterDest = super.getComponentAfter(root,dest);
         inited = true;
      }

      public Component getComponentAfter(Container root, Component c) {
         if ( ! inited ) init(root);
         if (c == beforeFirst) return afterLast;
         if (c == dest       ) return first;
         if (c == last       ) return afterDest;
         return super.getComponentAfter(root,c);
      }

      public Component getComponentBefore(Container root, Component c) {
         if ( ! inited ) init(root);
         if (c == afterLast) return beforeFirst;
         if (c == first    ) return dest;
         if (c == afterDest) return last;
         return super.getComponentBefore(root,c);
      }
   }

// --- email commands ---

   private void doSetClaim() {

      String claimNumber = Pop.inputString(this,Text.get(this,"s30",new Object[] { Roll.getIllegalAlbumInfo() }),Text.get(this,"s29"));
      if (claimNumber == null) return; // canceled

      try {
         itemPanel.setAlbum(claimNumber);
      } catch (ValidationException e) {
         Pop.error(this,e,Text.get(this,"s31"));
         return;
      }
      putEmail(getClaimEmail());

      // already noted elsewhere, but worth repeating: Roll.claimNumber is a real field
      // that LC keeps track of and sends to the server, but the server ignores it and
      // uses the album name instead.  this is apparently correct -- FujiPoller produces
      // different values for album name and claim number, and it's all working as is.
   }

   private String getConfirmedAddress() throws ValidationException {

      // check lots of different cases to give helpful errors

      String address = Field.get(email);
      if (address.length() == 0) throw new ValidationException(Text.get(this,"e5"));

      if (emailConfirm != null) { // always non-null here

         String confirm = Field.get(emailConfirm);
         if (confirm.length() == 0) throw new ValidationException(Text.get(this,"e6"));

         if ( ! confirm.equals(address) ) throw new ValidationException(Text.get(this,"e7"));
      }

      return address;
   }

   private void doEmailAdd() {

      String address;
      try {
         address = getConfirmedAddress();
      } catch (Exception e) {
         Pop.error(this,e,Text.get(this,"s20"));
         return;
      }

      if ( ! addressList.contains(address) ) {
         view.add(address);
      }
      // else just ignore
   }

   private void doEmailRemove(Object[] o) {

      if (o.length == 0) return;

      String s = Text.get(this,"e8",new Object[] { new Integer(o.length), Convert.fromInt(o.length) });
      boolean confirmed = Pop.confirm(this,s,Text.get(this,"s21"));
      if ( ! confirmed ) return;

      for (int i=0; i<o.length; i++) {
         view.remove(o[i]);
      }
   }

   private void doEmailSelect(Object o) {
      putEmail((String) o);
   }

}

