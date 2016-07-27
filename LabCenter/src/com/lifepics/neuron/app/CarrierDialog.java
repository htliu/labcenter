/*
 * CarrierDialog.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.dendron.Carrier;
import com.lifepics.neuron.dendron.CarrierList;
import com.lifepics.neuron.dendron.CarrierTransaction;
import com.lifepics.neuron.gui.EditDialog;
import com.lifepics.neuron.gui.GridBagHelper;
import com.lifepics.neuron.gui.GridColumn;
import com.lifepics.neuron.gui.GridUtil;
import com.lifepics.neuron.gui.ViewHelper;
import com.lifepics.neuron.meta.Accessor;
import com.lifepics.neuron.object.CopyUtil;
import com.lifepics.neuron.table.ListView;

import java.util.Iterator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A subdialog for editing carrier settings.
 */

public class CarrierDialog extends EditDialog {

// --- fields ---

   private CarrierList carrierList;
   private String carrierURL;

   private JComboBox defaultCarrier;
   private ListView view;
   private ViewHelper viewHelper;

   public CarrierList result() { return carrierList; }

// --- construction ---

   public CarrierDialog(Dialog owner, CarrierList carrierList, String carrierURL) {
      super(owner,Text.get(CarrierDialog.class,"s1"));

      this.carrierList = (CarrierList) CopyUtil.copy(carrierList);
      this.carrierURL = carrierURL;
      // since the object changes on refresh, the caller has to ask for the
      // final object in any case, so we might as well do the copy here too.
      // has to be a copy since the show flags are edited in place!

      construct(constructFields(),/* readonly = */ false,/* resizable = */ true);
   }

// --- helpers ---

   private static Object nullObject = new Object() { public String toString() { return ""; } };

   private String getDefaultCarrier() {
      Object o = defaultCarrier.getSelectedItem();
      return (o == nullObject) ? null : ((Carrier) o).primaryName;
   }

   private void setDefaultCarrier(String s) {
      Object o = (s == null) ? nullObject : carrierList.findCarrierByPrimaryName(s);
      // the find call will always succeed
      // because we know the default carrier comes from a valid carrier list
      defaultCarrier.setSelectedItem(o);
   }

   private void reloadDefaultCarrier() {
      defaultCarrier.removeAllItems();
      defaultCarrier.addItem(nullObject);
      Iterator i = carrierList.carriers.iterator();
      while (i.hasNext()) {
         defaultCarrier.addItem(i.next());
      }
   }

   private static GridColumn makeColumn(String suffix, Accessor accessor, boolean editable) {
      GridColumn col = new GridColumn(Text.get   (CarrierDialog.class,"n" + suffix),
                                      Text.getInt(CarrierDialog.class,"w" + suffix),accessor,null);
      if (editable) col.editable = true;
      return col;
   }

// --- methods ---

   private JPanel constructFields() {

      defaultCarrier = new JComboBox(); // values added in put function

      JButton buttonRefresh = new JButton(Text.get(this,"s3"));
      buttonRefresh.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doRefresh(); } });

      GridColumn[] cols = new GridColumn[] {
            makeColumn("1",Carrier.showAccessor,true),
            makeColumn("2",Carrier.displayNameAccessor,false)
         };

      view = new ListView(carrierList.carriers,null);
      viewHelper = new ViewHelper(view,Text.getInt(this,"c1"),cols,null);

      GridUtil.setShowGrid(viewHelper.getTable()); // different look for editable data

      JPanel fields = new JPanel();
      GridBagHelper helper = new GridBagHelper(fields);

      helper.add(0,0,new JLabel(Text.get(this,"s2") + ' '));
      helper.add(1,0,defaultCarrier);

      int d1 = Text.getInt(this,"d1");
      helper.add(0,1,Box.createVerticalStrut(d1));
      helper.addSpan(0,2,2,buttonRefresh);
      helper.add(0,3,Box.createVerticalStrut(d1));

      GridBagConstraints constraints = new GridBagConstraints();
      constraints.fill = GridBagConstraints.BOTH;
      constraints.gridwidth = 2;
      helper.add(0,4,viewHelper.getScrollPane(),constraints);

      helper.setRowWeight(4,1);
      helper.setColumnWeight(1,1);

      return fields;
   }

   protected void put() {

      reloadDefaultCarrier();
      setDefaultCarrier(carrierList.defaultCarrier);

      // show flags are edited in place
   }

   protected void getAndValidate() throws ValidationException {

      viewHelper.stopEditing(); // not necessary, since check boxes
      // don't go into editing mode, but it's definitely good form

      carrierList.defaultCarrier = getDefaultCarrier();

      // show flags are edited in place

      carrierList.validate();
   }

   private void doRefresh() {

      viewHelper.stopEditing(); // not necessary, since check boxes
      // don't go into editing mode, but it's definitely good form

      carrierList.defaultCarrier = getDefaultCarrier();
      // store this in the carrier list object
      // so the refresh function can remove it if it becomes invalid

      CarrierList temp = CarrierTransaction.refresh(this,carrierURL,carrierList);
      if (temp == carrierList) return; // refresh didn't do anything
      carrierList = temp;

      reloadDefaultCarrier();
      setDefaultCarrier(carrierList.defaultCarrier);

      view.repoint(carrierList.carriers);
   }

}

