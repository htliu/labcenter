/*
 * AbstractRollDialog.java
 */

package com.lifepics.neuron.axon;

import com.lifepics.neuron.core.ChainedException;
import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Pop;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.gui.Blob;
import com.lifepics.neuron.gui.EditDialog;
import com.lifepics.neuron.gui.Field;
import com.lifepics.neuron.gui.GridBagHelper;
import com.lifepics.neuron.net.Email;
import com.lifepics.neuron.object.CopyUtil;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * An abstract parent class for dialogs that edit roll information.
 */

public abstract class AbstractRollDialog extends EditDialog {

// --- mode enumeration ---

   public static final int MODE_CREATE = 0;
   public static final int MODE_EDIT = 1;
   public static final int MODE_VIEW = 2;

// --- construction ---

   protected AbstractRollDialog(Frame owner, String title) {
      super(owner,title);
   }

   protected AbstractRollDialog(Dialog owner, String title) {
      super(owner,title);
   }

   /**
    * A factory function that creates some kind of roll dialog;
    * this is where we decide whether to create a normal or pro dialog.
    * Also, it hides from the caller the annoying fact that
    * the dialog constructors distinguish between Frame and Dialog.
    */
   public static AbstractRollDialog create(Window owner, Roll roll, int mode, File currentDir, LinkedList addressList) {

      if (ProMode.isProOld() && mode == MODE_CREATE) {

         // check email address, since we'll be filling it in
         // and the user won't be able to do anything with it

         String s = null;

         if (s == null && proEmail.length() == 0) {
            s = Text.get(AbstractRollDialog.class,"e1");
         }

         if (s == null) try {
            Email.validate(proEmail);
         } catch (ValidationException e) {
            s = Text.get(AbstractRollDialog.class,"e2",new Object[] { ChainedException.format(e) });
         }

         if (s != null) {
            Pop.error(owner,s,Text.get(AbstractRollDialog.class,"s1"));
            return null;
         }

         // email address is valid, go ahead and fill it in here
         roll.email = proEmail;
      }

      if (ProMode.isProOld()) {
         return (owner instanceof Frame) ? new RollDialogPro((Frame ) owner,roll,mode,currentDir,addressList)
                                         : new RollDialogPro((Dialog) owner,roll,mode,currentDir,addressList);
      } else {
         return (owner instanceof Frame) ? new RollDialog((Frame ) owner,roll,mode,currentDir,addressList)
                                         : new RollDialog((Dialog) owner,roll,mode,currentDir,addressList);
      }
   }

// --- subclass interface ---

   public abstract File getCurrentDir();
   public abstract LinkedList getAddressList();

// --- config variables ---

   // in principle, proMode should be here, too, but ...
   //     (a) it doesn't change,
   //     (b) it's in ProMode.java,
   // and (c) it's only used to decide which kind of roll dialog to create
   //
   private static String proEmail;
   private static TransformConfig transformConfig;
   private static boolean claimEnabled;
   private static String claimEmail;
   private static LinkedList priceLists;
   private static LinkedList dealers;

   public static void setConfig(String proEmail, TransformConfig transformConfig, boolean claimEnabled, String claimEmail, LinkedList priceLists, LinkedList dealers) {
      // copy probably not necessary, but I don't want to think about it
      AbstractRollDialog.proEmail = proEmail;
      AbstractRollDialog.transformConfig = transformConfig.copy();
      AbstractRollDialog.claimEnabled = claimEnabled;
      AbstractRollDialog.claimEmail = claimEmail;
      AbstractRollDialog.priceLists = CopyUtil.copyList(priceLists);
      AbstractRollDialog.dealers = CopyUtil.copyList(dealers);
   }

// --- general helpers ---

   protected static String getTitleText(int mode) {
      return Text.get(AbstractRollDialog.class, (mode == MODE_CREATE) ? "s9" : "s10" );
   }
   // with this factored out, RollDialog should now never distinguish
   // between MODE_CREATE and MODE_EDIT.  so, the mode argument could
   // be replaced with a boolean, but I think it's not worth doing.

   protected static String formatRollID(int rollID) {
      return (rollID == -1) ? Text.get(AbstractRollDialog.class,"s11") : Convert.fromInt(rollID);
   }

   protected static boolean isClaimEnabled() { return claimEnabled; }
   protected static String getClaimEmail() { return claimEmail; }

   protected static JComponent makeErrorBlob(String s) {
      return Blob.makeBlob(s,Text.get   (AbstractRollDialog.class,"s14"),
                             Text.getInt(AbstractRollDialog.class,"d7"),
                             Text.getInt(AbstractRollDialog.class,"w6"));
   }

   protected static boolean weakValidateSizeZero(Component parent, int size) {

      if (size != 0) return true;

      return Pop.confirm(parent,Text.get(AbstractRollDialog.class,"e3"),
                                Text.get(AbstractRollDialog.class,"s23"));
   }

   protected static boolean weakValidateNames(Component parent, Roll roll) {

      LinkedList errorList = roll.checkNames();
      if (errorList.isEmpty()) return true;

      StringBuffer errorText = new StringBuffer();
      Iterator i = errorList.iterator();
      while (i.hasNext()) {
         errorText.append(Text.get(AbstractRollDialog.class,"e4b",new Object[] { (String) i.next() }));
      }

      return Pop.confirm(parent,Text.get(AbstractRollDialog.class,"e4a", new Object[] { errorText.toString() }),
                                Text.get(AbstractRollDialog.class,"s24"));
   }

// --- transform config helpers ---

   protected static class TransformPanel extends JPanel {

      private JRadioButton regular;
      private JRadioButton fast;
      private JRadioButton fastest;
      private JRadioButton custom;
      private JCheckBox enableLimit;
      private JTextField xInches;
      private JTextField yInches;
      private JTextField dpi;
      private JCheckBox alwaysCompress;
      private JTextField compression;

      private static String getText(String key) { return Text.get   (AbstractRollDialog.class,key); }
      private static int    getInt (String key) { return Text.getInt(AbstractRollDialog.class,key); }

      public String getLabel() { return getText("s2"); }

      public TransformPanel() {

         regular = new JRadioButton(getText("s3a"));
         fast    = new JRadioButton(getText("s3b"));
         fastest = new JRadioButton(getText("s3c"));
         custom = new JRadioButton(getText("s4"));
         int w1 = getInt("w1");
         enableLimit = new JCheckBox();
         xInches = new JTextField(w1);
         yInches = new JTextField(w1);
         dpi = new JTextField(getInt("w2"));
         alwaysCompress = new JCheckBox();
         compression = new JTextField(getInt("w3"));

         ButtonGroup group = new ButtonGroup();
         group.add(regular);
         group.add(fast);
         group.add(fastest);
         group.add(custom);

         GridBagHelper helper = new GridBagHelper(this);

         helper.add(0,0,regular);
         helper.add(0,1,fast);
         helper.add(0,2,fastest);
         helper.add(1,1,custom);

         helper.add(2,0,enableLimit);
         helper.add(3,0,new JLabel(getText("s25") + ' '));
         helper.add(4,0,xInches);
         helper.add(5,0,new JLabel(' ' + getText("s5") + ' '));
         helper.add(6,0,yInches);
         helper.add(7,0,new JLabel(' ' + getText("s6") + ' '));
         helper.add(8,0,dpi);
         helper.add(9,0,new JLabel(' ' + getText("s7")));
         helper.add(2,1,alwaysCompress);
         helper.addSpan(3,1,7,new JLabel(getText("s26")));

         JPanel subpanel = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
         subpanel.add(new JLabel(getText("s8") + ' '));
         subpanel.add(compression);
         helper.addSpan(2,2,8,subpanel);
      }

      public void setEnabled(boolean b) {
         regular.setEnabled(b);
         fast.setEnabled(b);
         fastest.setEnabled(b);
         custom.setEnabled(b);
         enableLimit.setEnabled(b);
         xInches.setEnabled(b);
         yInches.setEnabled(b);
         dpi.setEnabled(b);
         alwaysCompress.setEnabled(b);
         compression.setEnabled(b);
      }

      // the type and config together have five allowed states.
      //
      //   null     null      default (only for non-pro and older rolls)
      //   REGULAR  null      regular
      //   FAST     null      fast
      //   FASTEST  null      fastest
      //   null     not null  custom
      //
      // for the default case, which should be rare, we just leave
      // all four radio buttons unselected.
      // it's a little weird having putType/putConfig and getType/getConfig
      // be separate functions, but they don't interact that much,
      // and I like having the transform panel not be tied to Roll.
      //
      // also, there's not much difference between default and regular.
      // regular = default.derive(REGULAR), but when we talk to the
      // server in pro mode, we get the DPI and compression and then set
      // the default to that.derive(REGULAR).  derive is idempotent,
      // so as soon as we've talked to the server, they're the same.

      public void putType(Integer type) {
         if (type != null) switch (type.intValue()) {
         case Roll.TRANSFORM_TYPE_REGULAR:  regular.setSelected(true);  break;
         case Roll.TRANSFORM_TYPE_FAST:     fast   .setSelected(true);  break;
         case Roll.TRANSFORM_TYPE_FASTEST:  fastest.setSelected(true);  break;
         }
      }

      public void putConfig(TransformConfig tc) {

         if (tc != null) custom.setSelected(true);

         if (tc == null) tc = transformConfig; // fill custom fields with defaults

         Field.put(enableLimit,tc.enableLimit);
         Field.put(xInches,Convert.fromDouble(tc.xInches));
         Field.put(yInches,Convert.fromDouble(tc.yInches));
         Field.put(dpi,Convert.fromInt(tc.dpi));
         Field.put(alwaysCompress,tc.alwaysCompress);
         Field.put(compression,Convert.fromInt(tc.compression));
      }

      public Integer getType() {
         if      (regular.isSelected()) return new Integer(Roll.TRANSFORM_TYPE_REGULAR);
         else if (fast   .isSelected()) return new Integer(Roll.TRANSFORM_TYPE_FAST);
         else if (fastest.isSelected()) return new Integer(Roll.TRANSFORM_TYPE_FASTEST);
         else                           return null; // default or custom
      }

      public TransformConfig getConfig() throws ValidationException {

         if ( ! custom.isSelected() ) return null;

         TransformConfig tc = new TransformConfig();

         tc.enableLimit = Field.get(enableLimit);
         tc.xInches = Convert.toDouble(Field.get(xInches));
         tc.yInches = Convert.toDouble(Field.get(yInches));
         tc.dpi = Convert.toInt(Field.get(dpi));
         tc.alwaysCompress = Field.get(alwaysCompress);
         tc.compression = Convert.toInt(Field.get(compression));

         return tc;
      }
   }

// --- price list helpers ---

   protected static JComboBox makeCombo(LinkedList list) {
      JComboBox combo = new JComboBox();

      combo.addItem("");

      Iterator i = list.iterator();
      while (i.hasNext()) {
         combo.addItem(i.next());
      }
      // list elements must have a toString that returns name

      return combo;
   }

   protected static JComboBox makePriceListCombo() { return makeCombo(priceLists); }

   protected static void putPriceListCombo(JComboBox combo, PriceList pl) {

      // select an item with the same ID, or add a new item
      // this relies on the fact that priceLists doesn't change while dialog is active

      if (pl == null) { combo.setSelectedIndex(0); return; }

      int index = 1;

      Iterator i = priceLists.iterator();
      while (i.hasNext()) {
         if ( ((PriceList) i.next()).id.equals(pl.id) ) { combo.setSelectedIndex(index); return; }
         index++;
      }

      combo.insertItemAt(pl,1);
      combo.setSelectedIndex(1);
      //
      // this breaks the alphabetical ordering, but it should be rare;
      // also I like the way the front placement acts as a small hint
      // that something strange is going on.
   }

   protected static PriceList getPriceListCombo(JComboBox combo) {
      Object o = combo.getSelectedItem();
      return (o instanceof String) ? null : (PriceList) o;
   }

// --- dealer helpers ---

   // cloned from price list helpers, not worth unifying right now

   protected static JComboBox makeDealerCombo() { return makeCombo(dealers); }

   protected static void putDealerCombo(JComboBox combo, Dealer d) {

      if (d == null) { combo.setSelectedIndex(0); return; }

      int index = 1;

      Iterator i = dealers.iterator();
      while (i.hasNext()) {
         if ( ((Dealer) i.next()).id.equals(d.id) ) { combo.setSelectedIndex(index); return; }
         index++;
      }

      combo.insertItemAt(d,1);
      combo.setSelectedIndex(1);
   }

   protected static Dealer getDealerCombo(JComboBox combo) {
      Object o = combo.getSelectedItem();
      return (o instanceof String) ? null : (Dealer) o;
   }

}

