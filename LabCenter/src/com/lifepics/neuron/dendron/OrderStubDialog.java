/*
 * OrderStubDialog.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.gui.Blob;
import com.lifepics.neuron.gui.EditDialog;
import com.lifepics.neuron.gui.Field;
import com.lifepics.neuron.gui.GridBagHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A dialog for viewing order stub information.
 */

public class OrderStubDialog extends EditDialog {

// --- construction ---

   private static String getTitleText() {
      return Text.get(OrderStubDialog.class,"s1");
   }

   /**
    * A pseudo-constructor to deal with the annoying fact that
    * the superclass constructor distinguishes between Frame and Dialog.
    */
   public static OrderStubDialog create(Window owner, OrderStub order) {
      return (owner instanceof Frame) ? new OrderStubDialog((Frame ) owner,order)
                                      : new OrderStubDialog((Dialog) owner,order);
   }

   public OrderStubDialog(Frame owner, OrderStub order) {
      super(owner,getTitleText());

      construct(constructFields(order),/* readonly = */ true);

      // for some reason, the text areas don't figure out the right size
      // the first time around, you have to call pack again for it to work.
      // pack was already called once inside construct, so here's another:

      pack();
   }

   public OrderStubDialog(Dialog owner, OrderStub order) {
      super(owner,getTitleText());

      construct(constructFields(order),/* readonly = */ true);

      pack(); // see note above
   }

// --- helpers ---

   public static String describeID(OrderStub order) {
      String result = order.getFullID();
      if (order.wholesale != null) {
         result = Text.get(OrderStubDialog.class,"s5",new Object[] { result, order.wholesale.merchantName, Convert.fromInt(order.wholesale.merchantOrderID) });
      }
      // note, by validation we never have both orderSeq and wholesale information
      return result;
   }

// --- methods ---

   private JPanel constructFields(OrderStub order) {

   // fields

      JTextField orderID = new JTextField(Text.getInt(this,(order.wholesale == null) ? "w1a" : "w1b"));
      JTextField orderDir = new JTextField(Text.getInt(this,"w2"));

   // data

      // do this now, so we don't have to keep track of things

      Field.put(orderID,describeID(order));
      Field.put(orderDir,Convert.fromFile(order.orderDir));

   // enables

      orderID.setEnabled(false);
      orderDir.setEnabled(false);

   // overall

      JPanel fields = new JPanel();
      GridBagHelper helper = new GridBagHelper(fields);

      helper.add(0,0,new JLabel(Text.get(this,"s2") + ' '));
      helper.add(1,0,orderID);

      helper.add(0,1,new JLabel(Text.get(this,"s3") + ' '));
      helper.add(1,1,orderDir);

      if (order.lastError != null) {
         JComponent blob = Blob.makeBlob(order.lastError,
                                         Text.get(this,"s4"),
                                         Text.getInt(this,"d1"),
                                         Text.getInt(this,"w3"));
         helper.addSpanFill(0,2,2,blob);
      }

      return fields;
   }

   protected void put() {}
   protected void getAndValidate() {}

}

