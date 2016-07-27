/*
 * BarcodeDialog.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Pop;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.gui.EditDialog;
import com.lifepics.neuron.gui.Field;
import com.lifepics.neuron.gui.Graphic;
import com.lifepics.neuron.gui.GridBagHelper;
import com.lifepics.neuron.gui.PrintConfig;

import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A subdialog for editing barcode settings,
 * cf. the advanced dialogs for Fuji and Agfa.
 */

public class BarcodeDialog extends EditDialog {

// --- fields ---

   private PrintConfig pc;

   private JCheckBox barcodeEnable;
   private JTextField barcodeWidthPixels;
   private JTextField barcodeOverlapPixels;
   private JTextField barcodePrefix;
   private JCheckBox barcodePrefixReplace;
   private JComboBox barcodeIncludeTax;

   private JTextField modulePixels;
   private JTextField heightPixels;
   private JTextField textPixels;
   private JTextField marginPixelH;
   private JTextField marginPixelV;

// --- construction ---

   public BarcodeDialog(Dialog owner, PrintConfig pc) {
      super(owner,Text.get(BarcodeDialog.class,"s1"));
      this.pc = pc;
      construct(constructFields(),/* readonly = */ false);
   }

// --- methods ---

   private JPanel constructFields() {

      int w1 = Text.getInt(this,"w1");

      barcodeEnable = new JCheckBox(Text.get(this,"s3"));
      barcodeWidthPixels = new JTextField(w1);
      barcodeOverlapPixels = new JTextField(w1);
      barcodePrefix = new JTextField(Text.getInt(this,"w2"));
      barcodePrefixReplace = new JCheckBox(Text.get(this,"s22"));
      barcodeIncludeTax = new JComboBox(ConfigDialog.includeTaxNames);

      modulePixels = new JTextField(w1);
      heightPixels = new JTextField(w1);
      textPixels   = new JTextField(w1);
      marginPixelH = new JTextField(w1);
      marginPixelV = new JTextField(w1);

      JPanel fields = new JPanel();
      GridBagHelper helper = new GridBagHelper(fields);

      int y = 0;

      int d1 = Text.getInt(this,"d1");
      int d2 = Text.getInt(this,"d2");

      int yDiagramStart = y;

      helper.addSpan(0,y,4,barcodeEnable);
      y++;

      helper.add(0,y,Box.createVerticalStrut(d1));
      y++;

      helper.addSpan(0,y,4,new JLabel(Text.get(this,"s4")));
      y++;

      helper.add(0,y,Box.createVerticalStrut(d1));
      y++;

      // extra space added to second label so that 'W' doesn't get cut off
      helper.add(0,y,new JLabel(Text.get(this,"s5") + ' '));
      helper.add(1,y,new JLabel(' ' + Text.get(this,"s6") + ' '));
      helper.add(2,y,modulePixels);
      y++;

      helper.add(0,y,new JLabel(Text.get(this,"s7") + ' '));
      helper.add(1,y,new JLabel(' ' + Text.get(this,"s8") + ' '));
      helper.add(2,y,heightPixels);
      y++;

      helper.add(0,y,new JLabel(Text.get(this,"s9") + ' '));
      helper.add(1,y,new JLabel(' ' + Text.get(this,"s10") + ' '));
      helper.add(2,y,textPixels);
      y++;

      helper.add(0,y,new JLabel(Text.get(this,"s11") + ' '));
      helper.add(1,y,new JLabel(' ' + Text.get(this,"s12") + ' '));
      helper.add(2,y,marginPixelH);
      y++;

      helper.add(0,y,new JLabel(Text.get(this,"s13") + ' '));
      helper.add(1,y,new JLabel(' ' + Text.get(this,"s14") + ' '));
      helper.add(2,y,marginPixelV);
      y++;

      helper.add(0,y,Box.createVerticalStrut(d1));
      y++;

      helper.add(0,y,new JLabel(Text.get(this,"s15") + ' '));
      helper.add(1,y,new JLabel(' ' + Text.get(this,"s16") + ' '));
      helper.addSpan(2,y,2,barcodePrefix);
      helper.add(4,y,Box.createHorizontalStrut(d2));
      y++;

      int yDiagramEnd = y;

      helper.addSpan(0,y,6,barcodePrefixReplace);
      y++;

      helper.add(0,y,Box.createVerticalStrut(d1));
      y++;

      helper.addSpan(0,y,2,new JLabel(Text.get(this,"s17") + ' '));
      helper.add(2,y,barcodeWidthPixels);
      helper.addSpan(3,y,3,new JLabel(' ' + Text.get(this,"s18")));
      y++;

      helper.addSpan(0,y,2,new JLabel(Text.get(this,"s19") + ' '));
      helper.add(2,y,barcodeOverlapPixels);
      helper.addSpan(3,y,3,new JLabel(' ' + Text.get(this,"s20")));
      y++;

      helper.add(0,y,Box.createVerticalStrut(d1));
      y++;

      helper.addSpan(0,y,2,new JLabel(Text.get(this,"s21") + ' '));
      helper.addSpan(2,y,4,barcodeIncludeTax);
      y++;

      GridBagConstraints constraints = new GridBagConstraints();
      constraints.gridheight = yDiagramEnd - yDiagramStart;
      constraints.anchor = GridBagConstraints.SOUTH;
      helper.add(5,yDiagramStart,new JLabel(Graphic.getIcon("barcode.gif")),constraints);

      return fields;
   }

   protected void put() {

      Field.put(barcodeEnable,pc.barcodeEnable);
      Field.putNullable(barcodeWidthPixels,  Convert.fromNullableInt(pc.barcodeWidthPixels  ));
      Field.putNullable(barcodeOverlapPixels,Convert.fromNullableInt(pc.barcodeOverlapPixels));
      Field.put(barcodePrefix,pc.barcodePrefix);
      Field.put(barcodePrefixReplace,pc.barcodePrefixReplace);

      int includeTax = pc.barcodeIncludeTax ? ConfigDialog.INCLUDE_TAX_YES : ConfigDialog.INCLUDE_TAX_NO;
      Field.put(barcodeIncludeTax,ConfigDialog.includeTaxValues,includeTax);

      Field.putNullable(modulePixels,Convert.fromNullableInt(pc.barcodeConfig.modulePixels));
      Field.putNullable(heightPixels,Convert.fromNullableInt(pc.barcodeConfig.heightPixels));
      Field.putNullable(textPixels,  Convert.fromNullableInt(pc.barcodeConfig.textPixels  ));
      Field.putNullable(marginPixelH,Convert.fromNullableInt(pc.barcodeConfig.marginPixelH));
      Field.putNullable(marginPixelV,Convert.fromNullableInt(pc.barcodeConfig.marginPixelV));
   }

   public static void recopy(PrintConfig dest, PrintConfig src) {

      // this is stupid, but there's no time for a better way right now.
      // see comment in caller for details.

      dest.barcodeEnable = src.barcodeEnable;
      dest.barcodeWidthPixels   = src.barcodeWidthPixels;
      dest.barcodeOverlapPixels = src.barcodeOverlapPixels;
      dest.barcodePrefix = src.barcodePrefix;
      dest.barcodePrefixReplace = src.barcodePrefixReplace;
      dest.barcodeIncludeTax = src.barcodeIncludeTax;

      dest.barcodeConfig.modulePixels = src.barcodeConfig.modulePixels;
      dest.barcodeConfig.heightPixels = src.barcodeConfig.heightPixels;
      dest.barcodeConfig.textPixels   = src.barcodeConfig.textPixels;
      dest.barcodeConfig.marginPixelH = src.barcodeConfig.marginPixelH;
      dest.barcodeConfig.marginPixelV = src.barcodeConfig.marginPixelV;
   }

   protected void getAndValidate() throws ValidationException {

      // in the Fuji and Agfa advanced dialogs, we wrote directly
      // into the outer config, and so had to be very careful
      // not to perform partial writes.  here, because of the weak
      // validation step, we just can't do that;
      // instead the caller has to only accept the config on OK.

      pc.barcodeEnable = Field.get(barcodeEnable);
      pc.barcodeWidthPixels   = Convert.toNullableInt(Field.getNullable(barcodeWidthPixels  ));
      pc.barcodeOverlapPixels = Convert.toNullableInt(Field.getNullable(barcodeOverlapPixels));
      pc.barcodePrefix = Field.get(barcodePrefix);
      pc.barcodePrefixReplace = Field.get(barcodePrefixReplace);

      int includeTax = Field.get(barcodeIncludeTax,ConfigDialog.includeTaxValues);
      pc.barcodeIncludeTax = (includeTax == ConfigDialog.INCLUDE_TAX_YES);

      pc.barcodeConfig.modulePixels = Convert.toNullableInt(Field.getNullable(modulePixels));
      pc.barcodeConfig.heightPixels = Convert.toNullableInt(Field.getNullable(heightPixels));
      pc.barcodeConfig.textPixels   = Convert.toNullableInt(Field.getNullable(textPixels  ));
      pc.barcodeConfig.marginPixelH = Convert.toNullableInt(Field.getNullable(marginPixelH));
      pc.barcodeConfig.marginPixelV = Convert.toNullableInt(Field.getNullable(marginPixelV));

      pc.validateBarcode(); // and not all the other fields
   }

   protected boolean weakValidate() {
      LinkedList list = new LinkedList();

      pc.validateSoft(list);

      if ( ! list.isEmpty() ) {

         StringBuffer buf = new StringBuffer();
         buf.append(Text.get(this,"e1a"));
         buf.append("\n");

         Iterator i = list.iterator();
         while (i.hasNext()) {
            buf.append("\n");
            buf.append((String) i.next());
         }

         buf.append("\n\n");
         buf.append(Text.get(this,"e1b"));

         boolean confirmed = Pop.confirm(this,buf.toString(),Text.get(this,"s2"));
         if ( ! confirmed ) return false;
      }

      return true;
   }

}

