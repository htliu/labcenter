/*
 * ConfigDNP.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Pop;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.dendron.DNP;
import com.lifepics.neuron.dendron.DNPConfig;
import com.lifepics.neuron.dendron.DNPPicker;
import com.lifepics.neuron.gui.EditDialog;
import com.lifepics.neuron.gui.Field;
import com.lifepics.neuron.gui.GridBagHelper;
import com.lifepics.neuron.gui.GridColumn;
import com.lifepics.neuron.gui.GridUtil;
import com.lifepics.neuron.gui.ViewHelper;
import com.lifepics.neuron.meta.Accessor;
import com.lifepics.neuron.table.ListView;

import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A panel class for editing DNP direct settings.
 */

public class ConfigDNP extends ConfigFormat {

// --- fields ---

   private DNPConfig config;

   private JComboBox printerType;
   private JComboBox printerIDType;
   private JTextField printerID;
   private JRadioButton rotateCW;
   private JRadioButton rotateCCW;

// --- combo boxes ---

   private static Object[] printerTypeNames = new Object[] { Text.get(ConfigDNP.class,"pt0"),
                                                             Text.get(ConfigDNP.class,"pt1"),
                                                             Text.get(ConfigDNP.class,"pt2")  };
   private static int[] printerTypeValues = new int[] { DNP.PRINTER_DS40,
                                                        DNP.PRINTER_DS80,
                                                        DNP.PRINTER_RX1   };

   private static Object[] printerIDTypeNames = new Object[] { Text.get(ConfigDNP.class,"it0"),
                                                               Text.get(ConfigDNP.class,"it1"),
                                                               Text.get(ConfigDNP.class,"it2"),
                                                               Text.get(ConfigDNP.class,"it3")  };
   private static int[] printerIDTypeValues = new int[] { DNPConfig.ID_TYPE_SINGLE,
                                                          DNPConfig.ID_TYPE_ID,
                                                          DNPConfig.ID_TYPE_SERIAL,
                                                          DNPConfig.ID_TYPE_MEDIA   };

// --- construction ---

   public ConfigDNP(Dialog owner, DNPConfig config) {
      super(owner);
      this.config = config;

   // fields

      printerType = new JComboBox(printerTypeNames);
      printerIDType = new JComboBox(printerIDTypeNames);
      printerID = new JTextField(Text.getInt(this,"w1"));
      rotateCW = new JRadioButton(Text.get(this,"s4"));
      rotateCCW = new JRadioButton(Text.get(this,"s5"));

      ButtonGroup group = new ButtonGroup();
      group.add(rotateCW);
      group.add(rotateCCW);

   // layout

      GridBagHelper helper = new GridBagHelper(this);

      helper.addSpan(0,0,2,new JLabel(Text.get(this,"s1") + ' '));
      helper.add(2,0,printerType);

      helper.add(0,1,printerIDType);
      helper.add(1,1,new JLabel(' ' + Text.get(this,"s2") + ' '));
      helper.add(2,1,printerID);

      helper.add(0,2,Box.createVerticalStrut(Text.getInt(this,"d1")));

      JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
      panel.add(new JLabel(Text.get(this,"s3")));
      panel.add(rotateCW);
      panel.add(rotateCCW);
      helper.addSpan(0,3,6,panel);

   // button feature

      JPanel bracket = new JPanel();
      bracket.setPreferredSize(new Dimension(Text.getInt(this,"d3"),0));
      bracket.setBorder(BorderFactory.createMatteBorder(1,0,1,1,Color.black));

      JButton button = new JButton(Text.get(this,"s6"));
      button.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doPick(); }});

      GridBagConstraints constraints;

      constraints = new GridBagConstraints();
      constraints.fill = GridBagConstraints.VERTICAL;
      constraints.gridheight = 3;
      helper.add(3,0,bracket,constraints);

      helper.add(4,0,Box.createHorizontalStrut(Text.getInt(this,"d4")));

      constraints = new GridBagConstraints();
      constraints.anchor = GridBagConstraints.WEST;
      constraints.gridheight = 3;
      helper.add(5,0,button,constraints);
   }

// --- implementation of ConfigFormat ---

   public Object getFormatConfig() { return config; }

   public void put() {

      Field.put(printerType,printerTypeValues,config.printerType);
      Field.put(printerIDType,printerIDTypeValues,config.printerIDType);
      Field.put(printerID,config.printerID);

      if (config.rotateCW) rotateCW .setSelected(true);
      else                 rotateCCW.setSelected(true);
   }

   public void get() throws ValidationException {

      config.printerType = Field.get(printerType,printerTypeValues);
      config.printerIDType = Field.get(printerIDType,printerIDTypeValues);
      config.printerID = Field.get(printerID);

      config.rotateCW = rotateCW.isSelected();
   }

// --- methods ---

   private void doPick() {

      LinkedList printers;
      try {
         printers = DNPPicker.getPrinters();

      // fill in printerTypeName, filter out bad rows

         Iterator i = printers.iterator();
         while (i.hasNext()) {
            DNPPicker.Printer p = (DNPPicker.Printer) i.next();

            int index = Field.lookupSafe(printerTypeValues,p.printerType);
            if (index != -1) {
               p.printerTypeName = (String) printerTypeNames[index];
            } else {
               i.remove(); // not a type we recognize, so just remove it
            }
         }

      // make sure we have something to display

         if (printers.size() == 0) throw new ValidationException(Text.get(this,"e1"));

      } catch (Exception e) {
         Pop.error(this,e,Text.get(this,"s7"));
         return;
      }

      PickerDialog d = new PickerDialog(printers);
      if (d.run()) {

         Field.put(printerType,printerTypeValues,d.pickedType);
         Field.put(printerIDType,printerIDTypeValues,d.pickedIDType);
         Field.put(printerID,d.pickedID);
      }
   }

// --- subdialog ---

   private static GridColumn makeColumn(String suffix, Accessor accessor) {
      return new GridColumn(Text.get   (ConfigDNP.class,"n" + suffix),
                            Text.getInt(ConfigDNP.class,"w" + suffix),accessor,null);
   }

   private static Accessor printerTypeNameAccessor = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return ((DNPPicker.Printer) o).printerTypeName; }
   };

   private static Accessor printerIDAccessor = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return Convert.fromInt(((DNPPicker.Printer) o).printerID); }
   };

   private static Accessor serialNumberAccessor = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return ((DNPPicker.Printer) o).serialNumber; }
   };

   private static Accessor mediaLoadedAccessor = new Accessor() {
      public Class getFieldClass() { return String.class; }
      public Object get(Object o) { return ((DNPPicker.Printer) o).mediaLoaded; }
   };

   private class PickerDialog extends EditDialog {

   // --- fields ---

      private ViewHelper viewHelper;

      public int pickedType;
      public int pickedIDType;
      public String pickedID;

   // --- construction ---

      public PickerDialog(LinkedList printers) {
         super(ConfigDNP.this.getOwner(),Text.get(ConfigDNP.class,"s8"));
         construct(constructFields(printers),/* readonly = */ false);
      }

   // --- methods ---

      private JPanel constructFields(LinkedList printers) {

         // do not rearrange, indexes match printerIDTypeValues
         GridColumn[] cols = new GridColumn[] {
               makeColumn("1",printerTypeNameAccessor),
               makeColumn("2",printerIDAccessor),
               makeColumn("3",serialNumberAccessor),
               makeColumn("4",mediaLoadedAccessor)
            };

         ListView view = new ListView(printers,null);
         viewHelper = new ViewHelper(view,Text.getInt(ConfigDNP.class,"c1"),cols,null);

         GridUtil.setShowGrid(viewHelper.getTable()); // different look for editable data
         // we don't have editable data, but we do have cell selection, use the same look

         JPanel fields = new JPanel();
         GridBagHelper helper = new GridBagHelper(fields);

         int y = 0;

         helper.add(0,y,new JLabel(Text.get(ConfigDNP.class,"s9")));
         y++;

         helper.add(0,y,Box.createVerticalStrut(Text.getInt(ConfigDNP.class,"d5")));
         y++;

         helper.add(0,y,viewHelper.getScrollPane());
         y++;

         return fields;
      }

      protected void put() {
      }

      protected void getAndValidate() throws ValidationException {

         int row = viewHelper.tableGetSelectedRow();
         int col = viewHelper.tableGetSelectedColumn();

         if (row == -1 || col == -1) throw new ValidationException(Text.get(ConfigDNP.class,"e2"));

         DNPPicker.Printer p = (DNPPicker.Printer) viewHelper.viewGet(row);

         pickedType = p.printerType;
         pickedIDType = printerIDTypeValues[col]; // equals col, but I don't want to rely on that

         // could easily push this into DNPPicker.Printer
         switch (pickedIDType) {
         case DNPConfig.ID_TYPE_SINGLE:
            pickedID = ""; // this is the only case where pickedID != cell contents,
            // so a kluge seems possible, but I don't want to rely on that property
            break;
         case DNPConfig.ID_TYPE_ID:
            pickedID = Convert.fromInt(p.printerID);
            break;
         case DNPConfig.ID_TYPE_SERIAL:
            pickedID = p.serialNumber;
            break;
         case DNPConfig.ID_TYPE_MEDIA:
            pickedID = p.mediaLoaded;
            break;
         default:
            throw new IllegalArgumentException();
         }
      }
   }

}

