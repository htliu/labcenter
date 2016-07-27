/*
 * PatternDialog.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Pop;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.gui.EditDialog;
import com.lifepics.neuron.gui.Field;
import com.lifepics.neuron.gui.GridBagHelper;
import com.lifepics.neuron.meta.NoCaseComparator;
import com.lifepics.neuron.struct.NewSKU;
import com.lifepics.neuron.struct.Pattern;
import com.lifepics.neuron.struct.PSKU;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A dialog for creating patterns.
 */

public class PatternDialog extends EditDialog {

// --- fields ---

   private String product;
   public Pattern result;

   private int n;
   private JCheckBox[] enable;
   private String[] name;
   private JComboBox[] value;

// --- construction ---

   public static Pattern pick(Dialog owner, ProductConfig productConfig, PSKU psku, boolean auto) throws ValidationException {

   // check the input PSKU

      String product;
      NewSKU template;

      if (psku instanceof NewSKU) {
         product = ((NewSKU) psku).getProduct();
         template = (NewSKU) psku;
      } else if (psku instanceof Pattern) {
         product = ((Pattern) psku).getProduct();
         template = null;
         // could template on whatever attributes the pattern has, but that
         // would be bad, since usually the new pattern should be different
      } else {
         throw new ValidationException(Text.get(PatternDialog.class,"e1"));
      }

   // collect attributes

      // since product codes aren't unique any more, we can't just search for
      // the right product and use its attributes.  so, instead, take a union.

      HashMap map = new HashMap(); // maps attribute name to set of values
      build(map,productConfig,product);

   // handle special cases

      if (map.size() == 0) { // inactive product, only general rule is doable

         String s = Text.get(PatternDialog.class,(auto ? "s8" : "s9"),new Object[] { product });
         boolean confirmed = Pop.confirm(owner,s,Text.get(PatternDialog.class,"s7"));
         if ( ! confirmed ) return null;
         auto = true;
      }

      if (auto) return new Pattern(product,new HashMap()); // general rule
      //
      // it seems like you might want more validation on this, but actually it's fine.
      // the user has checked a checkbox, therefore the row was a NewSKU
      // and not a Pattern, and since the row was visible there's no general rule yet.
      // and, even if it does already exist (possible with add rule on an inactive product),
      // the caller will detect it and jump to the row, not try to create a duplicate.

   // show a dialog

      PatternDialog d = new PatternDialog(owner,product,template,map);
      return d.run() ? d.result : null;
   }

   private PatternDialog(Dialog owner, String product, NewSKU template, HashMap map) throws ValidationException {
      super(owner,Text.get(PatternDialog.class,"s1"));
      this.product = product;
      construct(constructFields(map,template),/* readonly = */ false);
   }

   /**
    * Confirm that an inactive product should really be totally removed.
    * This has nothing to do with the dialog, I just wanted the
    * code in here because the error message is parallel to the ones above.
    */
   public static boolean confirmRemoveInactive(Dialog owner, String product) {

      String s = Text.get(PatternDialog.class,"s11",new Object[] { product });
      return Pop.confirm(owner,s,Text.get(PatternDialog.class,"s10"));
   }

   public static boolean confirmRemoveCovered(Dialog owner, PSKU psku) {

      String s = Text.get(PatternDialog.class,"s13",new Object[] { psku.toString() });
      return Pop.confirm(owner,s,Text.get(PatternDialog.class,"s12"));
   }

   public static boolean confirmAddInconsistent(Dialog owner, Collection diffs) {

      StringBuffer buffer = new StringBuffer();
      int n = Text.getInt(PatternDialog.class,"n1");

      Iterator i = diffs.iterator();
      while (i.hasNext()) {
         buffer.append("\n");
         if (n-- <= 0) {
            buffer.append(Text.get(PatternDialog.class,"s16"));
            break;
         } else {
            buffer.append(i.next().toString()); // not that it matters, but it's a PSKU, actually a SKU
         }
      }

      String s = Text.get(PatternDialog.class,"s15",new Object[] { buffer.toString() });
      return Pop.confirm(owner,s,Text.get(PatternDialog.class,"s14"));
   }

// --- build sequence ---

   private static void build(HashMap map, ProductConfig productConfig, String product) {
      Iterator i = productConfig.productsNew.iterator();
      while (i.hasNext()) {
         NewProduct p = (NewProduct) i.next();
         if (p.productCode.equals(product)) build(map,p);
      }
   }

   private static void build(HashMap map, NewProduct p) {
      Iterator i = p.groups.iterator();
      while (i.hasNext()) {
         NewProduct.Group g = (NewProduct.Group) i.next();
         build(map,g);
      }
   }

   private static void build(HashMap map, NewProduct.Group g) {

      if (g.attributes.size() == 0) return; // don't create map entry if no attributes

      HashSet set = (HashSet) map.get(g.groupName);
      if (set == null) {
         set = new HashSet();
         map.put(g.groupName,set);
      }

      Iterator i = g.attributes.iterator();
      while (i.hasNext()) {
         NewProduct.Attribute a = (NewProduct.Attribute) i.next();
         set.add(a.value);
         // this is the one place in the whole system where attribute values
         // aren't passed through NewSKU.describe.  it's unfortunate because
         // it means the rule values don't exactly match what you see on the
         // Edit SKU screen, but it would be more unfortunate if we had some
         // products with overlapping definitions, some "True", some "Border",
         // then you wouldn't be able to tell what's what.
         // also "True" is pretty much phased out at this point, don't worry
         // too much about it
      }
   }

// --- methods ---

   private JPanel constructFields(HashMap map, NewSKU template) {

      n = map.size();
      enable = new JCheckBox[n];
      name = new String[n];
      value = new JComboBox[n];

      JPanel fields = new JPanel();
      GridBagHelper helper = new GridBagHelper(fields);
      int y = 0;

   // product and attributes

      helper.addSpan(2,y,3,new JLabel(product));
      y++;

      NoCaseComparator comparator = new NoCaseComparator();
      int d1 = Text.getInt(this,"d1");

      Iterator i = map.entrySet().iterator(); // sorted by hash order is fine, same as toString
      int j = 0;
      while (i.hasNext()) {
         Map.Entry entry = (Map.Entry) i.next();

         String key = (String) entry.getKey();
         HashSet set = (HashSet) entry.getValue();

         Object[] array = set.toArray();
         Arrays.sort(array,comparator);

         enable[j] = new JCheckBox();
         name[j] = key;
         value[j] = new JComboBox(array);

         if (template != null) {
            String select = template.getAttribute(key);
            if (select != null) {
               value[j].setSelectedItem(select);
            }
            // if the template has nonstandard attributes, I'll just ignore them
         }

         helper.add(0,y,Box.createVerticalStrut(d1));
         y++;

         helper.add(1,y,enable[j]);
         helper.add(2,y,new JLabel(name[j] + ' '));
         helper.addFill(3,y,value[j]);
         y++;

         j++;
      }

   // instructions

      helper.add(0,y,Box.createVerticalStrut(Text.getInt(this,"d2")));
      y++;

      helper.addSpan(0,y,5,new JLabel(Text.get(this,"s2")));
      y++;
      helper.addSpan(0,y,5,new JLabel(Text.get(this,"s3")));
      y++;

      helper.add(0,y,Box.createVerticalStrut(Text.getInt(this,"d3")));
      y++;

      helper.addSpan(0,y,5,new JLabel(Text.get(this,"s4")));
      y++;
      helper.addSpan(0,y,5,new JLabel(Text.get(this,"s5")));
      y++;
      helper.addSpan(0,y,5,new JLabel(Text.get(this,"s6")));
      y++;

   // finish up

      helper.setColumnWeight(0,1);
      helper.setColumnWeight(4,1);
      //
      // center the combo stuff.  I'm amazed, I thought column weight only applied
      // when the layout had to expand beyond its natural size, but this does work

      return fields;
   }

   protected void put() {
      // there's no put phase for this dialog
   }

   protected void getAndValidate() throws ValidationException {

      HashMap attributes = new HashMap();
      for (int j=0; j<n; j++) {
         if (Field.get(enable[j])) {

            // construct a value set for a single value
            // for now, more general patterns can only be created manually
            //
            Pattern.ValueSet vs = new Pattern.ValueSet();
            vs.inverted = false;
            vs.values = new HashSet();
            vs.values.add((String) value[j].getSelectedItem());

            attributes.put(name[j],vs);
         }
      }

      result = new Pattern(product,attributes);
   }

}

