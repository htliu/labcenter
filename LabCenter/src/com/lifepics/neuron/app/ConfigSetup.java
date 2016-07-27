/*
 * ConfigSetup.java
 */

package com.lifepics.neuron.app;

import com.lifepics.neuron.core.Convert;
import com.lifepics.neuron.core.Pop;
import com.lifepics.neuron.core.Text;
import com.lifepics.neuron.core.ValidationException;
import com.lifepics.neuron.gui.Field;
import com.lifepics.neuron.gui.GridBagHelper;
import com.lifepics.neuron.gui.PageSetup;
import com.lifepics.neuron.gui.Print;
import com.lifepics.neuron.gui.PrintConfig;

import java.util.Arrays;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.PageFormat;
import java.awt.print.PrinterJob;
import javax.print.PrintService;

/**
 * A helper class for editing PageSetup and PrintConfig.Setup objects.
 */

public class ConfigSetup {

// --- fields ---

   private Dialog owner;
   private PageSetup setupTarget;

   private JCheckBox enableForHome;
   private JCheckBox enableForAccount;
   private JComboBox printer;
   private JRadioButton sizeDefault;
   private JRadioButton sizeSpecify;
   private JTextField width;
   private JTextField height;
   private JTextField marginT;
   private JTextField marginB;
   private JTextField marginL;
   private JTextField marginR;
   private JComboBox orientation;

   private JPanel[] panelArray;

   public  static final int PANEL_TOP    = 0;
   public  static final int PANEL_BOTTOM = 1;
   private static final int PANEL_MAX    = 2;

   public JPanel getPanel(int i) { return panelArray[i]; }

// --- combo boxes ---

   private static final String defaultPrinterString = Text.get(ConfigSetup.class,"s14");
   private static final Object defaultPrinterObject = new Object() {
      public String toString() { return defaultPrinterString; }
   };
   // since combos compare objects by identity, this object
   // can't be mistaken even for a printer with the same name.

   public static Object[] orientationNames = new Object[] { Text.get(ConfigSetup.class,"or0"),
                                                            Text.get(ConfigSetup.class,"or1"),
                                                            Text.get(ConfigSetup.class,"or2")  };

   public static int[] orientationValues = new int[] { PageFormat.PORTRAIT,
                                                       PageFormat.LANDSCAPE,
                                                       PageFormat.REVERSE_LANDSCAPE };

// --- printer combo ---

   public static JComboBox constructPrinterCombo() {

      JComboBox printer = new JComboBox();
      printer.addItem(defaultPrinterObject); // ResolveDialog relies on this being first in list
      PrintService[] so = PrinterJob.lookupPrintServices();
      String[] ss = new String[so.length];
      for (int i=0; i<so.length; i++) { ss[i] = so[i].getName(); }
      Arrays.sort(ss);
      for (int i=0; i<ss.length; i++) { printer.addItem(ss[i]); }
      printer.setEditable(true);

      return printer;
   }

   public static void putPrinterCombo(JComboBox printer, String s) {
      printer.setSelectedItem( (s == null) ? defaultPrinterObject : s );
   }

   public static String getPrinterCombo(JComboBox printer) {
      Object selected = printer.getSelectedItem();
      return (selected == defaultPrinterObject) ? null : (String) selected;
   }

   public JComboBox accessPrinterCombo() { return printer; }

// --- construction ---

   public ConfigSetup(Dialog owner, PageSetup setupTarget) {
      this.owner = owner;
      this.setupTarget = setupTarget;

   // fields

      enableForHome    = new JCheckBox(Text.get(this,"s2"));
      enableForAccount = new JCheckBox(Text.get(this,"s3"));

      printer = constructPrinterCombo();

      sizeDefault = new JRadioButton(Text.get(this,"s6"));
      sizeSpecify = new JRadioButton();

      ButtonGroup group = new ButtonGroup();
      group.add(sizeDefault);
      group.add(sizeSpecify);

      int w1 = Text.getInt(this,"w1");
      width   = new JTextField(w1);
      height  = new JTextField(w1);
      marginT = new JTextField(w1);
      marginB = new JTextField(w1);
      marginL = new JTextField(w1);
      marginR = new JTextField(w1);

      orientation = new JComboBox(orientationNames);

   // top panel

      JPanel panel = new JPanel();
      GridBagHelper helper = new GridBagHelper(panel);

      helper.add    (0,1,  new JLabel(Text.get(this,"s1") + ' '));
      helper.add    (1,1,  enableForHome);
      helper.add    (1,2,  enableForAccount);

      // careful, this structure is modified in ConfigDialog

      helper.setColumnWeight(1,1);

      panelArray = new JPanel[PANEL_MAX];
      panelArray[PANEL_TOP] = panel;

   // subpanel to avoid column insanity

      JPanel panelSpecify = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));

      panelSpecify.add(sizeSpecify);
      panelSpecify.add(width);
      panelSpecify.add(new JLabel(' ' + Text.get(this,"s7") + ' '));
      panelSpecify.add(height);

   // bottom panel

      panel = new JPanel();
      helper = new GridBagHelper(panel);

      helper.add    (0,0,  new JLabel(Text.get(this,"s4") + ' '));
      helper.addSpan(1,0,8,printer);

      helper.add    (0,1,  new JLabel(Text.get(this,"s5") + ' '));
      helper.addSpan(1,1,5,sizeDefault);
      helper.addSpan(1,2,5,panelSpecify);

      helper.add    (0,3,  new JLabel(Text.get(this,"s8") + ' '));
      helper.add    (1,3,  new JLabel(Text.get(this,"s9") + ' '));
      helper.add    (2,3,  marginL);
      helper.add    (3,3,  Box.createHorizontalStrut(Text.getInt(this,"d2")));
      helper.add    (4,3,  new JLabel(Text.get(this,"s10") + ' '));
      helper.add    (5,3,  marginR);
      helper.add    (1,4,  new JLabel(Text.get(this,"s11") + ' '));
      helper.add    (2,4,  marginT);
      helper.add    (4,4,  new JLabel(Text.get(this,"s12") + ' '));
      helper.add    (5,4,  marginB);

      helper.add    (0,5,  new JLabel(Text.get(this,"s13") + ' '));
      helper.addSpan(1,5,5,orientation);

   // button feature

      JPanel bracket = new JPanel();
      bracket.setPreferredSize(new Dimension(Text.getInt(this,"d3"),0));
      bracket.setBorder(BorderFactory.createMatteBorder(1,0,1,1,Color.black));

      JButton button = new JButton(Text.get(this,"s15"));
      button.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doPick(); }});

      GridBagConstraints constraints;

      constraints = new GridBagConstraints();
      constraints.fill = GridBagConstraints.VERTICAL;
      constraints.gridheight = 4;
      helper.add(6,2,bracket,constraints);

      helper.add(7,2,Box.createHorizontalStrut(Text.getInt(this,"d4")));

      constraints = new GridBagConstraints();
      constraints.anchor = GridBagConstraints.WEST;
      constraints.gridheight = 4;
      helper.add(8,2,button,constraints);

      helper.setColumnWeight(8,1);

      panelArray[PANEL_BOTTOM] = panel;
   }

   public void put() {
      if (setupTarget instanceof PrintConfig.Setup) putTop((PrintConfig.Setup) setupTarget);
      putBottom(setupTarget);
   }

   private void putTop(PrintConfig.Setup setup) {
      Field.put(enableForHome,   setup.enableForHome   );
      Field.put(enableForAccount,setup.enableForAccount);
   }

   private void putBottom(PageSetup setup) {

      putPrinterCombo(printer,setup.printer);

      if (setup.defaultSize) sizeDefault.setSelected(true);
      else                   sizeSpecify.setSelected(true);

      Field.put(width,  Convert.fromDouble(setup.width  ));
      Field.put(height, Convert.fromDouble(setup.height ));
      Field.put(marginT,Convert.fromDouble(setup.marginT));
      Field.put(marginB,Convert.fromDouble(setup.marginB));
      Field.put(marginL,Convert.fromDouble(setup.marginL));
      Field.put(marginR,Convert.fromDouble(setup.marginR));

      Field.put(orientation,orientationValues,setup.orientation);
   }

   public void get() throws ValidationException {
      if (setupTarget instanceof PrintConfig.Setup) getTop((PrintConfig.Setup) setupTarget);
      getBottom(setupTarget);
   }

   private void getTop(PrintConfig.Setup setup) throws ValidationException {
      setup.enableForHome    = Field.get(enableForHome   );
      setup.enableForAccount = Field.get(enableForAccount);
   }

   private void getBottom(PageSetup setup) throws ValidationException {

      setup.printer = getPrinterCombo(printer);

      setup.defaultSize = sizeDefault.isSelected();

      setup.width   = Convert.toDouble(Field.get(width  ));
      setup.height  = Convert.toDouble(Field.get(height ));
      setup.marginT = Convert.toDouble(Field.get(marginT));
      setup.marginB = Convert.toDouble(Field.get(marginB));
      setup.marginL = Convert.toDouble(Field.get(marginL));
      setup.marginR = Convert.toDouble(Field.get(marginR));

      setup.orientation = Field.get(orientation,orientationValues);
   }

   private void doPick() {

      PrinterJob job;

      // normally you'd want to call loadDefault, but here we're going to
      // write into every field
      PageSetup setup = new PageSetup();
      try {

         getBottom(setup);
         setup.validate();

         job = Print.getJob(setup.printer);

      } catch (Exception e) {
         Pop.error(owner,e,Text.get(this,"s16"));
         return;
      }

      // explain that only paper settings will be remembered
      Pop.info(owner,Text.get(this,"i1"),Text.get(this,"s17"));
      //
      // Java 4 exhibits some poor behavior here too, also fixed by Java 6.
      // the trouble is, there's isn't any repaint while the pageDialog is
      // open, and also it's off-center, so the pop-up window seems to stay on screen.

      PageFormat pfBefore = Print.getPageFormat(job,setup);

      PageFormat pfAfter = job.pageDialog(pfBefore);
      if (pfAfter == pfBefore) return; // same object means canceled

      Print.putPageFormat(setup,pfAfter);
      putBottom(setup);

      // Java 4 exhibits some poor behavior here, but it's fixed by Java 6,
      // let's not worry about it.  to see the symptoms, just bring up the
      // dialog, hit OK, and observe that the values in LC aren't stable.
      //
      // (1) the margins that show in the dialog are page-relative, not paper-relative,
      // so in landscape mode they shuffle around relative to LC.  that's fine, but
      // the inverse shuffle is bugged so that left swaps with right & top with bottom.
      //
      // (2) like LC, the dialog only shows three decimal places, but the rounding for
      // bottom margin truncates instead of rounding, so that 0.4 becomes 0.399 and
      // so on.  if you keep cycling, the value keeps going down.  you can fix this by
      // adding some epsilon to all the margins.
   }

}

