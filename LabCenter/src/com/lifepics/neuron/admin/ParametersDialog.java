/*
 * ParametersDialog.java
 */

package com.lifepics.neuron.admin;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.gui.EditDialog;
import com.lifepics.neuron.gui.Field;
import com.lifepics.neuron.gui.GridBagHelper;

import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A dialog for editing the parameters for a snapshot.
 */

public class ParametersDialog extends EditDialog {

// --- fields ---

   private Parameters p;

   private JTextField merchants;
   private JTextField locations;
   private JTextField wholesalers;
   private JTextField instances;
   private JCheckBox includeDeletedLocations;

// --- construction ---

   public ParametersDialog(Frame owner, Parameters p) {
      super(owner,Text.get(ParametersDialog.class,"s1"));
      this.p = p;
      construct(constructFields(),/* readonly = */ false);
   }

// --- methods ---

   private JPanel constructFields() {

      int w1 = Text.getInt(this,"w1");

      merchants = new JTextField(w1);
      locations = new JTextField(w1);
      wholesalers = new JTextField(w1);
      instances = new JTextField(w1);
      includeDeletedLocations = new JCheckBox(Text.get(this,"s6"));

      JPanel fields = new JPanel();
      GridBagHelper helper = new GridBagHelper(fields);

      int y = 0;

      helper.add(0,y,new JLabel(Text.get(this,"s2") + ' '));
      helper.add(1,y,merchants);
      helper.add(2,y,Box.createHorizontalStrut(Text.getInt(this,"d1")));
      helper.add(3,y,includeDeletedLocations);
      y++;

      helper.add(0,y,new JLabel(Text.get(this,"s3") + ' '));
      helper.add(1,y,locations);
      y++;

      helper.add(0,y,new JLabel(Text.get(this,"s4") + ' '));
      helper.add(1,y,wholesalers);
      y++;

      helper.add(0,y,new JLabel(Text.get(this,"s5") + ' '));
      helper.add(1,y,instances);
      y++;

      helper.add(0,y,Box.createVerticalStrut(Text.getInt(this,"d2")));
      y++;

      helper.addSpan(1,y,3,new JLabel(Text.get(this,"s7")));
      y++;

      return fields;
   }

   protected void put() {

      Field.put(merchants,putCommaList(p.merchants));
      Field.put(locations,putCommaList(p.locations));
      Field.put(wholesalers,putCommaList(p.wholesalers));
      Field.put(instances,putCommaList(p.instances));
      Field.put(includeDeletedLocations,p.includeDeletedLocations);
   }

   protected void getAndValidate() throws ValidationException {

      p.merchants = getCommaList(Field.get(merchants));
      p.locations = getCommaList(Field.get(locations));
      p.wholesalers = getCommaList(Field.get(wholesalers));
      p.instances = getCommaList(Field.get(instances));
      p.includeDeletedLocations = Field.get(includeDeletedLocations);

      p.validate();
   }

   private static String COMMA = ",";

   private String putCommaList(LinkedList list) {
      StringBuffer b = new StringBuffer();
      Iterator i = list.iterator();
      while (i.hasNext()) {
         b.append(Convert.fromInt(((Integer) i.next()).intValue()));
         if (i.hasNext()) b.append(COMMA);
      }
      return b.toString();
   }

   private LinkedList getCommaList(String s) throws ValidationException {
      LinkedList list = new LinkedList();
      if (s.length() == 0) return list;
      String[] item = s.split(COMMA,-1); // -1 to stop weird default behavior
      for (int i=0; i<item.length; i++) {
         list.add(new Integer(Convert.toInt(item[i])));
      }
      return list;
   }

}

