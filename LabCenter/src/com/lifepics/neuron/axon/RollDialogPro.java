/*
 * RollDialogPro.java
 */

package com.lifepics.neuron.axon;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.gui.Field;
import com.lifepics.neuron.gui.GridBagHelper;

import java.io.File;
import java.util.LinkedList;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Another dialog for editing roll information.
 */

public class RollDialogPro extends AbstractRollDialog implements ActionListener {

// --- fields ---

   private ItemPanel itemPanel;

   private Roll roll;
   private LinkedList addressList; // not used, but ARD requires us to remember

   private JTextField rollID;
   private JRadioButton albumPublic;
   private JRadioButton albumPrivate;
   private JTextField eventCode;
   private JComboBox priceList;
   private TransformPanel transformPanel;

// --- accessors ---

   public File getCurrentDir() { return itemPanel.getCurrentDir(); }
   public LinkedList getAddressList() { return addressList; }

// --- construction ---

   /**
    * @param roll An object that will be modified by the dialog.
    */
   public RollDialogPro(Frame owner, Roll roll, int mode, File currentDir, LinkedList addressList) {
      super(owner,getTitleText(mode));

      boolean readonly = (mode == MODE_VIEW);

      itemPanel = new ItemPanelPro(this,roll.album,roll.items,roll.rollDir,currentDir,readonly);

      this.roll = roll;
      this.addressList = addressList;

      construct(constructFields(mode),readonly,/* resizable = */ true);

      activatePublicPrivate(readonly);
      pack(); // see note in RollDialog
   }

   /**
    * @param roll An object that will be modified by the dialog.
    */
   public RollDialogPro(Dialog owner, Roll roll, int mode, File currentDir, LinkedList addressList) {
      super(owner,getTitleText(mode));

      boolean readonly = (mode == MODE_VIEW);

      itemPanel = new ItemPanelPro(this,roll.album,roll.items,roll.rollDir,currentDir,readonly);

      this.roll = roll;
      this.addressList = addressList;

      construct(constructFields(mode),readonly,/* resizable = */ true);

      activatePublicPrivate(readonly);
      pack(); // see note in RollDialog
   }

// --- override ---

   public boolean run() {
      boolean result = super.run();
      itemPanel.stop();
      return result;
   }

// --- methods ---

   private JPanel constructFields(int mode) {

   // fields

      rollID = new JTextField(Text.getInt(this,"w1"));
      albumPublic = new JRadioButton(Text.get(this,"s21"));
      albumPrivate = new JRadioButton(Text.get(this,"s22"));
      eventCode = new JTextField(Text.getInt(this,"w8"));
      priceList = makePriceListCombo();
      transformPanel = new TransformPanel();

      ButtonGroup group = new ButtonGroup();
      group.add(albumPublic);
      group.add(albumPrivate);

   // enables

      rollID.setEnabled(false);

      if (mode == MODE_VIEW) {
         albumPublic.setEnabled(false);
         albumPrivate.setEnabled(false);
         eventCode.setEnabled(false);
         priceList.setEnabled(false);
         transformPanel.setEnabled(false);
      }

      // itemPanel read-only behavior handled at construction

   // top panel

      int d1 = Text.getInt(this,"d1");
      int d2 = Text.getInt(this,"d2");

      JPanel panelTop = new JPanel();
      panelTop.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(),
                                                            BorderFactory.createEmptyBorder(d1,d1,d1,d1)));

      GridBagHelper helper = new GridBagHelper(panelTop);

      helper.add(0,1,new JLabel(Text.get(this,"s3") + ' '));
      helper.add(1,1,rollID);

      itemPanel.addFields(helper,3); // happens to work in this layout too

      helper.add(2,1,Box.createHorizontalStrut(d2));

      helper.add(3,1,new JLabel(Text.get(this,"s25") + ' '));
      helper.addSpan(4,1,3,priceList);

      helper.addSpan(3,2,2,albumPublic);
      helper.addSpan(3,3,2,albumPrivate);

      helper.add(5,3,new JLabel(' ' + Text.get(this,"s24") + ' '));
      helper.add(6,3,eventCode);

      helper.add(7,1,Box.createHorizontalStrut(d2));

      GridBagConstraints constraints = new GridBagConstraints();
      constraints.gridheight = 5;
      helper.add(8,0,new JLabel(transformPanel.getLabel() + ' '),constraints);
      helper.add(9,0,transformPanel,constraints);

      helper.setRowWeight(0,1);
      helper.setRowWeight(4,1);
      helper.setColumnWeight(2,1);
      helper.setColumnWeight(7,1);

   // overall

      JPanel fields = new JPanel();
      helper = new GridBagHelper(fields);

      helper.addFill(0,0,panelTop);
      helper.add    (0,1,itemPanel,GridBagHelper.fillBoth);

      if (roll.lastError != null) {
         helper.addFill(0,2,makeErrorBlob(roll.lastError));
      }

      helper.setRowWeight(1,1);
      helper.setColumnWeight(0,1);

      return fields;
   }

   protected void put() {

      Field.put(rollID,formatRollID(roll.rollID));

      // the public-private radio buttons have no independent existence
      // as roll properties, they're just a device for showing what the
      // event code means.
      if (roll.eventCode == null) albumPublic .setSelected(true);
      else                        albumPrivate.setSelected(true);
      Field.putNullable(eventCode,roll.eventCode);
      // even if the event code is null, we still need to clear the field

      putPriceListCombo(priceList,roll.priceList);
      transformPanel.putType(roll.transformType);
      transformPanel.putConfig(roll.transformConfig);

      // album and items transferred to itemPanel at construction
   }

   protected void getAndValidate() throws ValidationException {

      // roll ID not editable

      if (albumPublic.isSelected()) {
         roll.eventCode = null;
         // allow filled-in event code in this case, but ignore the value
      } else {
         roll.eventCode = Field.getNullable(eventCode);
         if (roll.eventCode == null) throw new ValidationException(Text.get(this,"e1"));
      }

      roll.priceList = getPriceListCombo(priceList);
      roll.transformType = transformPanel.getType();
      roll.transformConfig = transformPanel.getConfig();

      roll.album = itemPanel.getAlbum();
      roll.items = itemPanel.getItems(roll.items);

      roll.validate();
   }

   protected boolean weakValidate() {

      if ( ! weakValidateSizeZero(this,roll.items.size()) ) return false;
      if ( ! weakValidateNames(this,roll) ) return false;

      return true;
   }

   private void activatePublicPrivate(boolean readonly) {

      if (readonly) return; // just don't do anything, that's safest.
      // in particular, we want to avoid enabling the field in view mode.

      albumPublic .addActionListener(this);
      albumPrivate.addActionListener(this);
      //
      // there are various ways to listen to radio buttons; why this way?
      // putting a ChangeListener on albumPrivate makes the most sense,
      // but it gets notified five times during a false-true transition;
      // ridiculous.  ActionListener isn't perfect, either, because it
      // fires again if you click on the already-selected item, but it's
      // a bit better.  also, ActionListener doesn't fire when you call
      // setSelected in put(), so the timing isn't critical; that's nice.
      // this gets called after put(), though, so it doesn't matter.

      actionPerformed(null); // get into correct initial state
   }

   public void actionPerformed(ActionEvent e) {
      eventCode.setEnabled(albumPrivate.isSelected());
   }

}

